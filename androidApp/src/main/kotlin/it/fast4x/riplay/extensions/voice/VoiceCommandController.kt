package it.fast4x.riplay.extensions.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val SINGLE_SHOT_TIMEOUT_MS = 15_000L
private const val HANDS_FREE_TIMEOUT_MS = 9_000L
private const val FOLLOW_UP_TIMEOUT_MS = 6_000L
private const val RESTART_DELAY_MS = 800L
private const val SILENCE_RESTART_DELAY_MS = 800L
private const val DETECTOR_RETRY_DELAY_MS = 1_000L
private const val DETECTOR_RELEASE_DELAY_MS = 120L
private const val PLAYBACK_WAIT_TIMEOUT_MS = 3_000L
private const val PLAYBACK_SETTLE_DELAY_MS = 1_200L
private const val RESUME_AFTER_COMMAND_DELAY_MS = 150L
private const val FAILURE_BACKOFF_MS = 1_200L
private const val MAX_CONSECUTIVE_FAILURES = 4
private const val MAX_DETECTOR_FAILURES = 5

/**
 * Owns the [SpeechRecognizer] used for playback voice commands.
 *
 * Supports one command at a time ([startListening]) and an optional hands-free
 * loop ([setHandsFreeMode]) that keeps running while the player is in the
 * foreground, stopping on lifecycle pause and restarting on resume.
 *
 * Hands-free listening is gated by [SpeechStartDetector]: the microphone is
 * only handed to the system recognizer once somebody starts speaking. The
 * recognizer itself asks for exclusive audio focus, so Chromium/ExoPlayer
 * pause the song for the duration of a command. Keeping the recognizer open
 * all the time made the song stop again and again; the detector takes no audio
 * focus and lets playback continue while it waits.
 *
 * Every listening session gets its own recognizer. Reusing one recognizer and
 * calling `cancel()` right before the next `startListening()` looks harmless,
 * but the cancel-related callback can arrive after the next session has
 * started. That old callback used to complete the new session immediately,
 * which made hands-free mode open and close the microphone in a tight loop.
 */
class VoiceCommandController internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    internal var onTranscript: suspend (String, Boolean) -> Unit,
) {

    var isListening by mutableStateOf(false)
        private set

    var isHandsFree by mutableStateOf(false)
        private set

    internal var requestPermission: (() -> Unit)? = null
    internal var onHandsFreeAborted: (() -> Unit)? = null
    internal var onRecognitionUnavailable: (() -> Unit)? = null
    internal var isPlaybackRunning: () -> Boolean = { false }

    /**
     * Reasserts playback after a recognition session. The system recognizer
     * takes exclusive audio focus, so Chromium pauses the song while the mic is
     * open; resuming here keeps "Hello, next" from leaving the player stopped.
     * Explicit pause commands never call it.
     */
    internal var resumePlayback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val speechStartDetector = SpeechStartDetector(context)

    private var session: ListenSession? = null
    private var handsFreeJob: Job? = null
    private var singleShotJob: Job? = null
    private var isForeground = true
    private var waitingForPermission = false

    private enum class ListenOutcome { Result, Silence, Permission, Unavailable, Error }

    private data class ListenResult(
        val hypotheses: List<String>,
        val outcome: ListenOutcome,
    )

    private class ListenSession(
        val recognizer: SpeechRecognizer,
        val continuation: CancellableContinuation<ListenResult>,
    ) {
        var cleaned = false
    }

    fun startListening() {
        if (isListening || singleShotJob?.isActive == true) return
        val resumeHandsFree = isHandsFree
        val previousHandsFreeJob = handsFreeJob
        stopHandsFreeLoop()

        singleShotJob = scope.launch {
            // Let the detector release the microphone before the recognizer
            // asks for it; otherwise the session fails with ERROR_AUDIO.
            previousHandsFreeJob?.join()
            delay(DETECTOR_RELEASE_DELAY_MS)

            // The recognizer pauses the song while it owns the audio focus.
            // Remember whether it was playing so a command can restore it.
            var shouldResume = isPlaybackRunning()
            val result = listenOnce(SINGLE_SHOT_TIMEOUT_MS)
            result.hypotheses.firstOrNull()?.let { transcript ->
                if (VoiceCommandParser.parse(transcript) == VoiceCommand.Pause) {
                    shouldResume = false
                }
                runCommand(transcript, handsFree = false)
            }
            resumePlaybackIfNeeded(shouldResume)

            if (resumeHandsFree && isHandsFree && isForeground) startHandsFreeLoop()
        }
    }

    fun setHandsFreeMode(enabled: Boolean) {
        isHandsFree = enabled
        if (enabled) startHandsFreeLoop() else stopHandsFreeLoop()
    }

    fun stopListening() {
        isHandsFree = false
        stopHandsFreeLoop()
        singleShotJob?.cancel()
        singleShotJob = null
        cancelPending()
    }

    internal fun onMicrophonePermissionResult(granted: Boolean) {
        if (!granted) {
            waitingForPermission = false
            if (isHandsFree) {
                isHandsFree = false
                onHandsFreeAborted?.invoke()
            }
            return
        }
        if (waitingForPermission || isHandsFree) {
            waitingForPermission = false
            if (isHandsFree) startHandsFreeLoop() else startListening()
        }
    }

    internal fun onPause() {
        isForeground = false
        stopHandsFreeLoop()
        singleShotJob?.cancel()
        singleShotJob = null
        cancelPending()
    }

    internal fun onResume() {
        isForeground = true
        if (isHandsFree) startHandsFreeLoop()
    }

    fun destroy() {
        isHandsFree = false
        stopHandsFreeLoop()
        singleShotJob?.cancel()
        singleShotJob = null
        cancelPending()
    }

    private suspend fun runCommand(transcript: String, handsFree: Boolean) {
        if (transcript.isBlank()) return
        Timber.d("VoiceCommandController heard: %s (handsFree=%s)", transcript, handsFree)
        runCatching { onTranscript(transcript, handsFree) }
            .onFailure { Timber.e(it, "VoiceCommandController command failed for: $transcript") }
    }

    private fun startHandsFreeLoop() {
        if (!isHandsFree || !isForeground) return
        if (handsFreeJob?.isActive == true) return

        handsFreeJob = scope.launch {
            var failures = 0
            var detectorFailures = 0

            while (isActive && isHandsFree && isForeground) {
                if (!hasMicrophonePermission()) {
                    if (!waitingForPermission) {
                        waitingForPermission = true
                        requestPermission?.invoke()
                    }
                    break
                }

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Timber.w("VoiceCommandController speech recognition not available")
                    isHandsFree = false
                    onRecognitionUnavailable?.invoke()
                    onHandsFreeAborted?.invoke()
                    break
                }

                // Do not start the detector while the song is still
                // buffering/resuming after a recognition session: the sudden
                // music onset would look like speech. Wait until playback is
                // stable, then give the echo canceler time to converge.
                awaitPlaybackSettled()
                if (!isActive || !isHandsFree || !isForeground) break

                // Wait for a voice without holding audio focus. The song keeps
                // playing during this phase.
                val heardSpeech = withContext(Dispatchers.IO) { speechStartDetector.awaitSpeech() }
                if (!isActive || !isHandsFree || !isForeground) break
                if (!heardSpeech) {
                    detectorFailures++
                    Timber.w("VoiceCommandController voice detector failed (%s)", detectorFailures)
                    if (detectorFailures >= MAX_DETECTOR_FAILURES) {
                        isHandsFree = false
                        onHandsFreeAborted?.invoke()
                        break
                    }
                    delay(DETECTOR_RETRY_DELAY_MS)
                    continue
                }
                detectorFailures = 0

                delay(DETECTOR_RELEASE_DELAY_MS)
                // The detector heard a voice, so the recognizer is about to take
                // the audio focus and Chromium will pause the iframe. Remember
                // whether the song was playing before that happens.
                var shouldResume = isPlaybackRunning()
                val result = listenOnce(HANDS_FREE_TIMEOUT_MS)
                var executed = false

                when (val selection = VoiceCommandParser.selectHandsFree(result.hypotheses)) {
                    is HandsFreeSelection.Command -> {
                        if (VoiceCommandParser.parse(selection.text) == VoiceCommand.Pause) {
                            shouldResume = false
                        }
                        runCommand(selection.text, handsFree = true)
                        executed = true
                    }
                    HandsFreeSelection.WakeWordOnly -> {
                        // The user usually says the command right after the wake
                        // word. Keep a short session open so "Hello ... next"
                        // split across two recognitions still works.
                        val followUp = listenOnce(FOLLOW_UP_TIMEOUT_MS)
                        val followSelection =
                            VoiceCommandParser.selectHandsFree(followUp.hypotheses)
                        if (followSelection is HandsFreeSelection.Command) {
                            if (VoiceCommandParser.parse(followSelection.text) ==
                                VoiceCommand.Pause
                            ) {
                                shouldResume = false
                            }
                            runCommand(followSelection.text, handsFree = true)
                            executed = true
                        }
                    }
                    HandsFreeSelection.Ignore -> Unit
                }

                // Speech without a usable command must not leave the song
                // stopped either: only an explicit "pause"/"stop" may do that.
                val recognizerRan = result.outcome == ListenOutcome.Result ||
                        result.outcome == ListenOutcome.Silence ||
                        result.outcome == ListenOutcome.Error
                resumePlaybackIfNeeded(shouldResume && recognizerRan)

                failures = if (result.outcome == ListenOutcome.Error) failures + 1 else 0
                when (result.outcome) {
                    ListenOutcome.Permission -> {
                        Timber.w("VoiceCommandController hands-free paused: waiting for microphone permission")
                        break
                    }
                    ListenOutcome.Unavailable -> {
                        Timber.w("VoiceCommandController hands-free stopped: recognition unavailable")
                        isHandsFree = false
                        onHandsFreeAborted?.invoke()
                        break
                    }
                    ListenOutcome.Error -> {
                        if (failures >= MAX_CONSECUTIVE_FAILURES) {
                            Timber.w("VoiceCommandController hands-free stopped after $failures failures")
                            isHandsFree = false
                            onHandsFreeAborted?.invoke()
                            break
                        }
                        delay(FAILURE_BACKOFF_MS * failures)
                    }
                    // Silence and timeouts are normal while waiting for a command.
                    ListenOutcome.Result, ListenOutcome.Silence -> {
                        delay(if (executed) RESTART_DELAY_MS else SILENCE_RESTART_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun stopHandsFreeLoop() {
        handsFreeJob?.cancel()
        handsFreeJob = null
    }

    private suspend fun resumePlaybackIfNeeded(enabled: Boolean) {
        if (!enabled) return
        delay(RESUME_AFTER_COMMAND_DELAY_MS)
        if (!coroutineContext.isActive) return
        runCatching { resumePlayback?.invoke() }
            .onFailure { Timber.w(it, "VoiceCommandController could not resume playback") }
    }

    private suspend fun awaitPlaybackSettled() {
        var waitedMs = 0L
        while (coroutineContext.isActive && isHandsFree && isForeground &&
            waitedMs < PLAYBACK_WAIT_TIMEOUT_MS
        ) {
            if (isPlaybackRunning()) {
                delay(PLAYBACK_SETTLE_DELAY_MS)
                return
            }
            delay(100L)
            waitedMs += 100L
        }
    }

    private suspend fun listenOnce(timeoutMs: Long): ListenResult {
        if (!hasMicrophonePermission()) {
            if (!waitingForPermission) {
                waitingForPermission = true
                requestPermission?.invoke()
            }
            return ListenResult(emptyList(), ListenOutcome.Permission)
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.w("VoiceCommandController speech recognition not available")
            onRecognitionUnavailable?.invoke()
            return ListenResult(emptyList(), ListenOutcome.Unavailable)
        }

        val recognizer = createRecognizer()
            ?: return ListenResult(emptyList(), ListenOutcome.Unavailable)

        val result = withTimeoutOrNull(timeoutMs) { awaitUtterance(recognizer) }
        if (result != null) return result

        // The timeout cancelled awaitUtterance. Clean up synchronously so a
        // following session cannot start while the old recognizer still owns
        // the microphone.
        runOnMain { session?.let { cleanupSession(it, cancel = true) } }
        Timber.d("VoiceCommandController listening timed out")
        return ListenResult(emptyList(), ListenOutcome.Silence)
    }

    private suspend fun createRecognizer(): SpeechRecognizer? =
        withContext(Dispatchers.Main.immediate) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }
                .onFailure { Timber.e(it, "VoiceCommandController could not create recognizer") }
                .getOrNull()
        }

    private suspend fun awaitUtterance(recognizer: SpeechRecognizer): ListenResult =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val listenSession = ListenSession(recognizer, continuation)
                recognizer.setRecognitionListener(
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            if (session === listenSession) isListening = true
                        }

                        override fun onBeginningOfSpeech() = Unit

                        override fun onRmsChanged(rmsdB: Float) = Unit

                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() = Unit

                        override fun onError(error: Int) {
                            finishSession(listenSession, emptyList(), errorOutcome(error))
                        }

                        override fun onResults(results: Bundle?) {
                            finishSession(
                                listenSession,
                                recognitionHypotheses(results),
                                ListenOutcome.Result
                            )
                        }

                        override fun onPartialResults(partialResults: Bundle?) = Unit

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    }
                )

                session = listenSession
                isListening = true

                // Cancellation can arrive from the timeout executor or the
                // lifecycle thread. SpeechRecognizer must only be used from the
                // main thread, so hand the cleanup back to it.
                continuation.invokeOnCancellation {
                    runOnMain { cleanupSession(listenSession, cancel = true) }
                }

                runCatching { recognizer.startListening(recognitionIntent()) }
                    .onFailure {
                        Timber.e(it, "VoiceCommandController could not start listening")
                        finishSession(listenSession, emptyList(), ListenOutcome.Error)
                    }
            }
        }

    private fun finishSession(
        target: ListenSession,
        hypotheses: List<String>,
        outcome: ListenOutcome,
    ) {
        runOnMain {
            if (session !== target) return@runOnMain
            cleanupSession(target, cancel = false)
            if (target.continuation.isActive) {
                target.continuation.resume(ListenResult(hypotheses, outcome))
            }
        }
    }

    private fun cleanupSession(target: ListenSession, cancel: Boolean) {
        if (target.cleaned) return
        target.cleaned = true
        if (session === target) {
            session = null
            isListening = false
        }
        if (cancel) {
            runCatching { target.recognizer.cancel() }
                .onFailure { Timber.w(it, "VoiceCommandController could not cancel recognizer") }
        }
        runCatching { target.recognizer.destroy() }
            .onFailure { Timber.w(it, "VoiceCommandController could not destroy recognizer") }
    }

    private fun cancelPending() {
        runOnMain {
            session?.let { cleanupSession(it, cancel = true) }
        }
    }

    private fun recognitionHypotheses(results: Bundle?): List<String> =
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.mapNotNull { hypothesis ->
                hypothesis.trim().takeIf { it.isNotEmpty() }
            }
            ?.distinct()
            ?.take(5)
            ?: emptyList()

    private fun errorOutcome(error: Int): ListenOutcome = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ListenOutcome.Silence
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ListenOutcome.Permission
        else -> ListenOutcome.Error
    }

    private fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putStringArrayListExtra(
            RecognizerIntent.EXTRA_BIASING_STRINGS,
            arrayListOf(
                "RiPlay", "Hello RiPlay", "Hello",
                "next", "previous", "pause", "continue", "replay",
                "shuffle", "loop", "stop"
            )
        )
        // Return the result promptly once the command is finished instead of
        // leaving the song paused for several extra seconds.
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }
}

@Composable
fun rememberVoiceCommandController(
    isPlaybackRunning: () -> Boolean = { false },
    onTranscript: suspend (String, Boolean) -> Unit,
): VoiceCommandController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember(context) {
        VoiceCommandController(context.applicationContext, scope, onTranscript)
    }

    val currentOnTranscript by rememberUpdatedState(onTranscript)
    val currentIsPlaybackRunning by rememberUpdatedState(isPlaybackRunning)
    SideEffect {
        controller.onTranscript = currentOnTranscript
        controller.isPlaybackRunning = { currentIsPlaybackRunning() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        controller.onMicrophonePermissionResult(granted)
        if (!granted) {
            SmartMessage(
                message = context.getString(R.string.voice_microphone_permission_denied),
                type = PopupType.Error,
                context = context
            )
        }
    }

    DisposableEffect(controller) {
        controller.requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        onDispose { controller.requestPermission = null }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.destroy() }
    }

    return controller
}
