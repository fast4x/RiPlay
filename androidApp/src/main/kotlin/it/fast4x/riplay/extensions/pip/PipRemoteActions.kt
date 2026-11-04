package it.fast4x.riplay.extensions.pip

import android.app.Activity
import android.app.RemoteAction
import android.graphics.drawable.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.utils.ActionReceiver
import it.fast4x.riplay.utils.findActivity

private const val PIP_ACTION_BASE = "it.fast4x.riplay.pip.action"

/**
 * Tasti del player come azioni remote della finestra PiP:
 * precedente, play/pausa e successivo.
 */
class PipActionReceiver(
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) : ActionReceiver(PIP_ACTION_BASE) {

    private var playPauseHandler = onPlayPause
    private var previousHandler = onPrevious
    private var nextHandler = onNext

    /** Allinea i gestori ai lambda correnti della composizione. */
    fun update(onPlayPause: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
        playPauseHandler = onPlayPause
        previousHandler = onPrevious
        nextHandler = onNext
    }

    val previous by action { _, _ -> previousHandler() }
    val playPause by action { _, _ -> playPauseHandler() }
    val next by action { _, _ -> nextHandler() }
}

private fun remoteAction(
    activity: Activity,
    action: ActionReceiver.Action,
    iconRes: Int,
    titleRes: Int
) = RemoteAction(
    Icon.createWithResource(activity, iconRes),
    activity.getString(titleRes),
    activity.getString(titleRes),
    action.pendingIntent
)

/**
 * Mantiene allineati i parametri della finestra PiP allo stato del player:
 * rapporto d'aspetto per modulo, tasti come azioni remote e auto-enter
 * su Android 12+ durante la riproduzione.
 */
@Composable
fun PipRemoteActions(
    module: PipModule,
    isPlaying: Boolean,
    enabled: Boolean,
    autoEnter: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val receiver = remember { PipActionReceiver(onPlayPause, onPrevious, onNext) }
    SideEffect { receiver.update(onPlayPause, onPrevious, onNext) }

    DisposableEffect(activity, enabled) {
        if (enabled) receiver.register()
        onDispose {
            if (enabled) runCatching { context.unregisterReceiver(receiver) }
        }
    }

    if (!enabled) return

    DisposableEffect(activity, module, isPlaying, autoEnter) {
        val (numerator, denominator) = module.pipAspectRatio()
        val actions = listOf(
            remoteAction(activity, receiver.previous, R.drawable.play_skip_back, R.string.pip_action_previous),
            remoteAction(
                activity,
                receiver.playPause,
                if (isPlaying) R.drawable.pause else R.drawable.play,
                if (isPlaying) R.string.pip_action_pause else R.string.pip_action_play
            ),
            remoteAction(activity, receiver.next, R.drawable.play_skip_forward, R.string.pip_action_next)
        )
        activity.setPipConfiguration(
            numerator = numerator,
            denominator = denominator,
            actions = actions,
            autoEnterIfPossible = autoEnter && isPlaying
        )
        onDispose { }
    }
}
