package it.fast4x.riplay.enums

import androidx.media3.common.Player

enum class QueueLoopType {
    Default,
    RepeatOne,
    RepeatAll;

    val type: Int
        get() = when (this) {
        Default -> Player.REPEAT_MODE_OFF
        RepeatOne -> Player.REPEAT_MODE_ONE
        RepeatAll -> Player.REPEAT_MODE_ALL
    }

    companion object {
        @JvmStatic
        fun from(value: Int): QueueLoopType {
            return when (value) {
                Player.REPEAT_MODE_OFF -> Default
                Player.REPEAT_MODE_ONE -> RepeatOne
                Player.REPEAT_MODE_ALL -> RepeatAll
                else -> Default
            }
        }
    }
}