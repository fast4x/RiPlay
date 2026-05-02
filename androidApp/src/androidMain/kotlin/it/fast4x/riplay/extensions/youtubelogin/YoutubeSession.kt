package it.fast4x.riplay.extensions.youtubelogin

data class YoutubeSession(
    val cookie: String = "",
    val visitorData: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = ""
)
