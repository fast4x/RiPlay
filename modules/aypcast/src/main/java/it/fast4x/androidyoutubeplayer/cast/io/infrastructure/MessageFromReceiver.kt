package it.fast4x.androidyoutubeplayer.cast.io.infrastructure

/**
 * POJO representing a message received from the cast receiver.
 */
internal data class MessageFromReceiver(val type: String, val data: String)