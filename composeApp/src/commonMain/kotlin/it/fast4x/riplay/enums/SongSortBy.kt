package it.fast4x.riplay.enums

enum class SongSortBy {
    PlayTime,
    RelativePlayTime,
    Title,
    DateAdded,
    DatePlayed,
    DateLiked,
    Artist,
    Duration,
    AlbumName;

    companion object {
        fun fromString(value: String): SongSortBy {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                DateAdded
            }
        }
    }
}
