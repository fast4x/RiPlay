package it.fast4x.riplay.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

object DesktopSleepTimer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: kotlinx.coroutines.Job? = null

    /** gettone anti-gara: solo l'avvio piu' recente puo' eseguire la fade */
    private val token = AtomicReference<Any?>(null)

    /** Istante di scadenza (epoch ms); null = timer spento. Osservabile in UI. */
    var endsAtMs by mutableStateOf<Long?>(null)
        private set

    /** TRUE durante la dissolvenza finale del volume. */
    var fading by mutableStateOf(false)
        private set

    val isActive: Boolean
        get() = endsAtMs != null

    fun start(minutes: Int) {
        cancel()
        if (minutes <= 0) return
        val myToken = Any()
        token.set(myToken)
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        endsAtMs = endAt
        job = scope.launch {
            // attesa con check al secondo: il cancel() interrompe subito
            while (isActive && System.currentTimeMillis() < endAt) delay(1_000L)
            if (!isActive || token.get() !== myToken) return@launch
            fadeOutAndPause(myToken)
            if (token.get() === myToken) {
                // fine naturale: pulizia (un eventuale start() nel frattempo
                // avrebbe gia' cambiato il token e lo stato)
                endsAtMs = null
                job = null
                token.set(null)
            }
        }
    }

    fun cancel() {
        token.set(null)
        job?.cancel()
        job = null
        endsAtMs = null
        fading = false
    }

    /**
     * Dissolvenza lineare ~10s (40 passi da 250ms) sul controller attivo,
     * poi pausa e ripristino del volume di partenza. Ogni passo controlla
     * il token: un cancel()/start() arrivato nel frattempo blocca subito
     * la fade (il volume resta com'era, nessun effetto a metà).
     */
    private suspend fun fadeOutAndPause(myToken: Any) {
        val controller = DesktopPlaybackHub.activeController
        if (controller == null) {
            fading = false
            return
        }
        fading = true
        val startVolume = controller.state.value.volume.coerceIn(0.05f, 1f)
        val steps = 40
        for (i in 1..steps) {
            if (token.get() !== myToken) return
            controller.setVolume(startVolume * (1f - i.toFloat() / steps))
            delay(250L)
        }
        if (token.get() !== myToken) return
        controller.pause()
        delay(400L)
        if (token.get() !== myToken) return
        controller.setVolume(startVolume)
        fading = false
    }
}
