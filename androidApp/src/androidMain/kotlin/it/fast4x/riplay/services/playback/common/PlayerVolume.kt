package it.fast4x.riplay.services.playback.common

fun restorePlayerVolume(
    userVolume: Float,
    isFading: Boolean,
    isServiceReady: Boolean,
    setVolume: (Float) -> Unit,
) {
    if (isFading || !isServiceReady) return
    setVolume(userVolume.coerceIn(0f, 1f))
}
