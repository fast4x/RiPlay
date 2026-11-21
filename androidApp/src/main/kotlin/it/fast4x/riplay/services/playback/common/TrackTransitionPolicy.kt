package it.fast4x.riplay.services.playback.common

import it.fast4x.riplay.enums.CrossfadeDuration
import it.fast4x.riplay.services.playback.ActiveEngine

enum class EndOfSongAction {
    IMMEDIATE_PLAY_NEXT,
    EXO_CROSSFADE_OUT,
    WEBVIEW_FADE_OUT,
}

data class WatchdogDecision(
    val shouldTriggerEndOfSong: Boolean,
    val reason: String? = null,
    val nextNearEndStallTicks: Int = 0,
)

/**
 * Determines whether ExoPlayer should pause at the end of each MediaItem.
 *
 * - When [ActiveEngine.YOUTUBE] is active, ExoPlayer only holds a `/dev/null` placeholder item,
 *   so it must pause at the end of media items.
 * - When [ActiveEngine.EXOPLAYER] is active and [CrossfadeDuration.Off] is selected ("Off (Gapless)"),
 *   ExoPlayer must NOT pause at the end of media items so its native sample-accurate gapless
 *   audio pipeline transitions seamlessly to the next track.
 * - When [ActiveEngine.EXOPLAYER] is active and crossfade is enabled, ExoPlayer pauses at the end
 *   so the crossfade watchdog drives the fade-out ramp before advancing.
 */
fun shouldExoPauseAtEndOfMediaItems(
    activeEngine: ActiveEngine,
    crossfadeDuration: CrossfadeDuration,
): Boolean {
    return activeEngine == ActiveEngine.YOUTUBE || crossfadeDuration != CrossfadeDuration.Off
}

/**
 * Determines how `handleEndOfSong` should advance to the next playable track.
 *
 * When a track has already naturally reached `ENDED` (or recovered from a near-end stall),
 * the outgoing stream is already silent; running a 3-12s fade-out loop would only inject dead air.
 */
fun evaluateEndOfSongAction(
    source: String,
    crossfadeDuration: CrossfadeDuration,
    isNextLocal: Boolean,
): EndOfSongAction {
    val alreadySilent = source.contains("ENDED", ignoreCase = true) ||
        source.contains("stall", ignoreCase = true)
    return if (crossfadeDuration != CrossfadeDuration.Off && !alreadySilent) {
        if (isNextLocal) EndOfSongAction.EXO_CROSSFADE_OUT else EndOfSongAction.WEBVIEW_FADE_OUT
    } else {
        EndOfSongAction.IMMEDIATE_PLAY_NEXT
    }
}

/**
 * Evaluates the 200ms playback watchdog tick for track-end crossfades and near-end stall recovery
 * across both [ActiveEngine.EXOPLAYER] and [ActiveEngine.YOUTUBE].
 */
fun evaluateWatchdogTransition(
    activeEngine: ActiveEngine,
    isPlaying: Boolean,
    isFading: Boolean,
    crossfadeDuration: CrossfadeDuration,
    durationMs: Long,
    positionMs: Long,
    lastPositionMs: Long,
    nearEndStallTicks: Int,
): WatchdogDecision {
    if (durationMs <= 0L || positionMs <= 0L || !isPlaying || isFading) {
        return WatchdogDecision(shouldTriggerEndOfSong = false, nextNearEndStallTicks = 0)
    }

    val timeLeft = durationMs - positionMs

    if (crossfadeDuration != CrossfadeDuration.Off) {
        if (positionMs > 3000L && timeLeft in 0L..crossfadeDuration.milliseconds) {
            return WatchdogDecision(
                shouldTriggerEndOfSong = true,
                reason = "cambio con crossfade attivo ($activeEngine)",
                nextNearEndStallTicks = 0,
            )
        }
        return WatchdogDecision(shouldTriggerEndOfSong = false, nextNearEndStallTicks = 0)
    }

    // Crossfade is Off (Gapless):
    // Do NOT cut off advancing tracks 1200ms early; only intervene if the engine stalls
    // within the final 1500ms of the song for 3 consecutive ticks (600ms) without firing ENDED.
    val isFrozenNearEnd = timeLeft in 0L..1500L &&
        positionMs > 5000L &&
        positionMs == lastPositionMs

    if (isFrozenNearEnd) {
        val updatedTicks = nearEndStallTicks + 1
        if (updatedTicks >= 3) {
            return WatchdogDecision(
                shouldTriggerEndOfSong = true,
                reason = "near-end stall recovery ($activeEngine)",
                nextNearEndStallTicks = 0,
            )
        }
        return WatchdogDecision(
            shouldTriggerEndOfSong = false,
            nextNearEndStallTicks = updatedTicks,
        )
    }

    return WatchdogDecision(shouldTriggerEndOfSong = false, nextNearEndStallTicks = 0)
}

/**
 * Determines whether online track fade-in should wait for `YT.PlayerState.PLAYING`
 * rather than starting inside `onMediaItemTransition` while the WebView is still buffering.
 */
fun shouldDeferOnlineFadeInUntilPlaying(
    isLocal: Boolean,
    crossfadeDuration: CrossfadeDuration,
): Boolean {
    return !isLocal && crossfadeDuration != CrossfadeDuration.Off
}

/**
 * Determines whether ExoPlayer's native `DISCONTINUITY_REASON_AUTO_TRANSITION`
 * should be intercepted with a manual `handlePlayNext()` seek.
 */
fun shouldInterceptExoAutoTransitionWithSeek(
    crossfadeDuration: CrossfadeDuration,
): Boolean {
    return false
}
