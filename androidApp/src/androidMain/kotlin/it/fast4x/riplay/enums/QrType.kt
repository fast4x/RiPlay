package it.fast4x.riplay.enums

enum class QrType {
    play,
    artist,
    album,
    playlist,
    localPlaylist,
    unknown;

    val content: String
        get() = when(this) {
            play -> "riplay:play:"
            artist -> "riplay:artist:"
            album -> "riplay:album:"
            playlist -> "riplay:playlist:"
            localPlaylist -> "riplay:localPlaylist:"
            else -> ""
        }
}