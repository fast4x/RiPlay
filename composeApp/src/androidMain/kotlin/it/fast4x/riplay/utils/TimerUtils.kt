package it.fast4x.riplay.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimerListener(
    private val scope: CoroutineScope,
    val player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    var triggerTime by mutableLongStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    fun start(minute: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minute == -1) {
            pauseWhenSongEnd = true
        } else {
            triggerTime = System.currentTimeMillis() + minute.minutes.inWholeMilliseconds
            sleepTimerJob = scope.launch {
                delay(minute.minutes)
                player.pause()
                triggerTime = -1L
            }
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }
}

interface TimerJob {
    val millisLeft: StateFlow<Long?>
    fun cancel()
}

fun CoroutineScope.timer(delayMillis: Long, onCompletion: () -> Unit): TimerJob {
    val millisLeft = MutableStateFlow<Long?>(delayMillis)
    val job = launch {
        while (isActive && millisLeft.value != null) {
            delay(1000)
            millisLeft.emit(millisLeft.value?.minus(1000)?.takeIf { it > 0 })
        }
    }
    val disposableHandle = job.invokeOnCompletion {
        if (it == null) {
            onCompletion()
        }
    }

    return object : TimerJob {
        override val millisLeft: StateFlow<Long?>
            get() = millisLeft.asStateFlow()

        override fun cancel() {
            millisLeft.value = null
            disposableHandle.dispose()
            job.cancel()
        }
    }
}