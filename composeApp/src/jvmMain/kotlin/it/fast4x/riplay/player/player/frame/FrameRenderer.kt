package player.frame

import kotlinx.coroutines.flow.StateFlow

interface FrameRenderer {
    val size: StateFlow<Pair<Int, Int>>
    val bytes: StateFlow<ByteArray?>
}