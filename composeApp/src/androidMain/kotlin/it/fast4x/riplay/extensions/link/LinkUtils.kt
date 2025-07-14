package it.fast4x.riplay.extensions.link

const val LINK_COMMAND_SEPARATOR = "|"
const val LINK_COMMAND_LOAD = "load$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_PLAY = "play$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_PAUSE = "pause$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_STOP = "stop$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_NEXT = "next$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_PREVIOUS = "previous$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_SEEK = "seek$LINK_COMMAND_SEPARATOR"

fun String.toLoadCommand(position: Int = 0): String {
    return "$LINK_COMMAND_LOAD$this$LINK_COMMAND_SEPARATOR$position"
}

fun String.toPlayCommand(position: Int = 0): String {
    return "$LINK_COMMAND_PLAY$this$LINK_COMMAND_SEPARATOR$position"
}
