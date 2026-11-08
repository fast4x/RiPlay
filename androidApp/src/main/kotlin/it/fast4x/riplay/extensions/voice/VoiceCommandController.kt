package it.fast4x.riplay.extensions.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LISTEN_TIMEOUT_MS = 15_000L
private const val RESTART_DELAY_MS = 400L
private const val FAILURE_BACKOFF_MS = 1_200L
private const val MAX_CONSECUTIVE_FAILURES = 4

/**
 * Owns the [SpeechRecognizer] used for playback voice commands.
 *
 * Supports one command at a time ([startListening]) and an optional hands-free
 * loop ([setHandsFreeMode]) that keeps listening while the player is in the
 * foreground, stopping on lifecycle pause and restarting on resume.
 */
class VoiceCommandController internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    internal var onTranscript: suspend (String) -> Unit,
) : RecognitionListener {

    var isListening by mutableStateOf(false)
        private set

    var isHandsFree by mutableStateOf(false)
        private set

    internal var requestPermission: (() -> Unit)? = null
    internal var onHandsFreeAborted: (() -> Unit)? = null
    internal var onRecognitionUnavailable: (() -> Unit)? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var pending: CancellableContinuation<String?>? = null
    private var handsFreeJob: Job? = null
    private var isForeground = true
    private var waitingForPermission = false
    private var lastOutcome = ListenOutcome.Silence

    private enum class ListenOutcome { Result, Silence, Permission, Unavailable, Error }

    fun startListening() {
        if (isListening) return
        val resumeHandsFree = isHandsFree
        if (resumeHandsFree) stopHandsFreeLoop()
        scope.launch {
            runCommand(listenOnce())
            if (resumeHandsFree) startHandsFreeLoop()
        }
    }

    fun setHandsFreeMode(enabled: Boolean) {
        isHandsFree = enabled
        if (enabled) startHandsFreeLoop() else stopHandsFreeLoop()
    }

    fun stopListening() {
        isHandsFree = false
        stopHandsFreeLoop()
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
        cancelPending()
    }

    internal fun onResume() {
        isForeground = true
        if (isHandsFree) startHandsFreeLoop()
    }

    fun destroy() {
        isHandsFree = false
        stopHandsFreeLoop()
        cancelPending()
        speechRecognizer?.let { recognizer ->
            runCatching { recognizer.destroy() }
                .onFailure { Timber.e(it, "VoiceCommandController could not destroy recognizer") }
        }
        speechRecognizer = null
    }

    private suspend fun runCommand(transcript: String?) {
        if (transcript.isNullOrBlank()) return
        runCatching { onTranscript(transcript) }
            .onFailure { Timber.e(it, "VoiceCommandController command failed for: $transcript") }
    }

    private fun startHandsFreeLoop() {
        if (!isHandsFree || !isForeground) return
        if (handsFreeJob?.isActive == true) return
        handsFreeJob = scope.launch {
            var failures = 0
            while (isActive && isHandsFree && isForeground) {
                val transcript = listenOnce()
                if (transcript != null) {
                    failures = 0
                    runCommand(transcript)
                    delay(RESTART_DELAY_MS)
                    continue
                }
                when (lastOutcome) {
                    ListenOutcome.Permission, ListenOutcome.Unavailable -> {
                        Timber.w("VoiceCommandController hands-free stopped: ${lastOutcome.name}")
                        isHandsFree = false
                        onHandsFreeAborted?.invoke()
                        break
                    }
                    ListenOutcome.Error -> {
                        failures++
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
                        failures = 0
                        delay(RESTART_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun stopHandsFreeLoop() {
        handsFreeJob?.cancel()
        handsFreeJob = null
    }

    private suspend fun listenOnce(): String? {
        val result = withTimeoutOrNull(LISTEN_TIMEOUT_MS) { awaitUtterance() }
        if (result == null && isListening) cancelPending()
        return result
    }

    private suspend fun awaitUtterance(): String? = suspendCancellableCoroutine { continuation ->
        lastOutcome = ListenOutcome.Silence

        if (!hasMicrophonePermission()) {
            waitingForPermission = true
            lastOutcome = ListenOutcome.Permission
            requestPermission?.invoke()
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val recognizer = ensureRecognizer()
        if (recognizer == null) {
            lastOutcome = ListenOutcome.Unavailable
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        pending?.let { previous ->
            if (previous.isActive) previous.resume(null)
        }
        pending = continuation
        isListening = true

        continuation.invokeOnCancellation {
            if (pending === continuation) pending = null
            isListening = false
            safeCancel(recognizer)
        }

        safeCancel(recognizer)
        try {
            recognizer.startListening(recognitionIntent())
        } catch (t: Throwable) {
            Timber.e(t, "VoiceCommandController could not start listening")
            finishPending(null)
        }
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.w("VoiceCommandController speech recognition not available")
            onRecognitionUnavailable?.invoke()
            return null
        }
        speechRecognizer?.let { return it }
        return runCatching { SpeechRecognizer.createSpeechRecognizer(context) }
            .onSuccess { it.setRecognitionListener(this) }
            .onFailure { Timber.e(it, "VoiceCommandController could not create recognizer") }
            .getOrNull()
            ?.also { speechRecognizer = it }
    }

    private fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun safeCancel(recognizer: SpeechRecognizer) {
        runCatching { recognizer.cancel() }
    }

    private fun cancelPending() {
        val continuation = pending
        pending = null
        isListening = false
        speechRecognizer?.let(::safeCancel)
        if (continuation != null && continuation.isActive) continuation.resume(null)
    }

    private fun finishPending(transcript: String?) {
        val continuation = pending ?: return
        pending = null
        isListening = false
        if (continuation.isActive) continuation.resume(transcript)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        isListening = true
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() = Unit

    override fun onError(error: Int) {
        lastOutcome = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ListenOutcome.Silence
            else -> ListenOutcome.Error
        }
        when (error) {
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                speechRecognizer?.let { runCatching { it.destroy() } }
                speechRecognizer = null
            }
        }
        finishPending(null)
    }

    override fun onResults(results: Bundle?) {
        lastOutcome = ListenOutcome.Result
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        finishPending(transcript)
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

@Composable
fun rememberVoiceCommandController(onTranscript: suspend (String) -> Unit): VoiceCommandController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember(context) {
        VoiceCommandController(context.applicationContext, scope, onTranscript)
    }

    val currentOnTranscript by rememberUpdatedState(onTranscript)
    SideEffect { controller.onTranscript = currentOnTranscript }

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
