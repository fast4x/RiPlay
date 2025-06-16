package player

data class PlayerState(
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = .5f,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
)