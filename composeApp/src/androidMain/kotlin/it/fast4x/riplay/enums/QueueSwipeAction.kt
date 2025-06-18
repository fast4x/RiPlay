package it.fast4x.riplay.enums

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext

enum class QueueSwipeAction {
    NoAction,
    PlayNext,
    Favourite,
    RemoveFromQueue,
    Enqueue;

    val displayName: String
        get() = when (this) {
            NoAction -> appContext().resources.getString(R.string.none)
            PlayNext -> appContext().resources.getString(R.string.play_next)
            Favourite -> appContext().resources.getString(R.string.favorites)
            RemoveFromQueue  -> appContext().resources.getString(R.string.remove_from_queue)
            Enqueue -> appContext().resources.getString(R.string.enqueue)
        }

    val icon: Int?
        get() = when (this) {
            NoAction -> null
            PlayNext -> R.drawable.play_skip_forward
            Favourite -> R.drawable.heart_outline
            RemoveFromQueue -> R.drawable.trash
            Enqueue -> R.drawable.enqueue
        }

}
