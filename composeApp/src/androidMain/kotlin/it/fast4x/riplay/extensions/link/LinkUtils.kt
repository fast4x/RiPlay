package it.fast4x.riplay.extensions.link

import android.content.Context
import java.io.File

const val LINK_COMMAND_SEPARATOR = "|"
const val LINK_COMMAND_LOAD = "load$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_PLAY = "play$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_PAUSE = "pause$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_STOP = "stop$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_NEXT = "next$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_PREVIOUS = "previous$LINK_COMMAND_SEPARATOR"
const val LINK_COMMAND_SEEK = "seek$LINK_COMMAND_SEPARATOR"


const val LINKWEB_COMMAND_SEPARATOR = "&"
const val LINKWEB_COMMAND_LOAD = "load"
const val LINKWEB_COMMAND_PLAY = "play"
const val LINKWEB_COMMAND_PLAYAT = "playAt"
const val LINKWEB_COMMAND_PAUSE = "pause"

fun String.toCommandLoad(position: Int = 0): String {
    return "command=${LINKWEB_COMMAND_LOAD}${LINKWEB_COMMAND_SEPARATOR}mediaId=$this${LINKWEB_COMMAND_SEPARATOR}position=$position"
}

fun String.toCommandPlay(position: Int = 0): String {
    return "command=${LINKWEB_COMMAND_PLAY}${LINKWEB_COMMAND_SEPARATOR}mediaId=$this${LINKWEB_COMMAND_SEPARATOR}position=$position"
}

fun String.toCommandPlayAt(position: Int = 0): String {
    return "command=${LINKWEB_COMMAND_PLAYAT}${LINKWEB_COMMAND_SEPARATOR}mediaId=$this${LINKWEB_COMMAND_SEPARATOR}position=$position"
}

fun String.toCommand(): String {
    return "command=$this"
}

