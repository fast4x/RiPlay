package it.fast4x.riplay.enums

import it.fast4x.riplay.R
import it.fast4x.riplay.appContext

enum class AlbumSwipeAction {
    NoAction,
    PlayNext,
    Bookmark,
    Enqueue;

    val displayName: String
        get() = when (this) {
            NoAction -> appContext().resources.getString(R.string.none)
            PlayNext -> appContext().resources.getString(R.string.play_next)
            Bookmark  -> appContext().resources.getString(R.string.bookmark)
            Enqueue  -> appContext().resources.getString(R.string.enqueue)
        }

    val icon: Int?
        get() = when (this) {
            NoAction -> null
            PlayNext -> R.drawable.play_skip_forward
            Bookmark -> R.drawable.bookmark_outline
            Enqueue -> R.drawable.enqueue
        }

        fun getStateIcon(bookmarkedState: Long?): Int? {
            return when (this) {
                NoAction -> null
                PlayNext -> R.drawable.play_skip_forward
                Bookmark -> when(bookmarkedState) {
                    null -> R.drawable.bookmark_outline
                    else -> R.drawable.bookmark
                }
                Enqueue -> R.drawable.enqueue
        }
    }
}
