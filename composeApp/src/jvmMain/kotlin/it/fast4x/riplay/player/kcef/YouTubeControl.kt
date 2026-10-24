package it.fast4x.riplay.player.kcef

/**
 * Contratto del motore YouTube per desktop: specchio dell'interfaccia
 * `YouTubeControl` di Android (services/playback/HybridPlayer.kt),
 * con l'aggiunta di load/cue che su desktop non passano da un player esterno.
 *
 * Implementato da [KcefYouTubePlayer] via browser CEF (KCEF) e
 * YouTube IFrame Player API.
 */
interface YouTubeControl {
    fun loadVideoById(videoId: String, startSeconds: Float = 0f)
    fun cueVideoById(videoId: String, startSeconds: Float = 0f)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun getCurrentPositionMs(): Long
    fun getDurationMs(): Long
    fun isPlaying(): Boolean
    fun getVolume(): Float
    fun setVolume(volume: Float)
    fun setPlaybackRate(rate: Float)
}
