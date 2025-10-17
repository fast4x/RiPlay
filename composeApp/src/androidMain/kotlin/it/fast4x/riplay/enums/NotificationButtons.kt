package it.fast4x.riplay.enums

import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionCommand
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.R
import it.fast4x.riplay.service.MediaSessionConstants.CommandSearch
import it.fast4x.riplay.service.MediaSessionConstants.CommandStartRadio
import it.fast4x.riplay.service.MediaSessionConstants.CommandToggleLike
import it.fast4x.riplay.service.MediaSessionConstants.CommandToggleRepeatMode
import it.fast4x.riplay.service.MediaSessionConstants.CommandToggleShuffle
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.utils.appContext

enum class NotificationButtons {
    Favorites,
    Repeat,
    Shuffle,
    Radio,
    Search;

    val sessionCommand: SessionCommand
    get() = when (this) {
        Favorites -> CommandToggleLike
        Repeat -> CommandToggleRepeatMode
        Shuffle -> CommandToggleShuffle
        Radio -> CommandStartRadio
        Search -> CommandSearch
    }

    val pendingIntent: PendingIntent
    @OptIn(UnstableApi::class)
    get() = when (this) {
        Favorites -> LocalPlayerService.Action.like.pendingIntent
        Repeat -> LocalPlayerService.Action.repeat.pendingIntent
        Shuffle -> LocalPlayerService.Action.shuffle.pendingIntent
        Radio -> LocalPlayerService.Action.playradio.pendingIntent
        Search -> LocalPlayerService.Action.search.pendingIntent
    }

    val pendingIntentOnline: PendingIntent
        @OptIn(UnstableApi::class)
        get() = when (this) {
            Favorites -> MainActivity.Action.like.pendingIntent
            Repeat -> MainActivity.Action.repeat.pendingIntent
            Shuffle -> MainActivity.Action.shuffle.pendingIntent
            Radio -> MainActivity.Action.playradio.pendingIntent
            Search -> MainActivity.Action.search.pendingIntent
        }

    val action: String
        get() = when (this) {
            Favorites -> "FAVORITES"
            Repeat -> "REPEAT"
            Shuffle -> "SHUFFLE"
            Radio -> "RADIO"
            Search -> "SEARCH"
        }

    val displayName: String
    get() = when (this) {
        Favorites -> appContext().resources.getString(R.string.favorites)
        Repeat -> appContext().resources.getString(R.string.repeat)
        Shuffle -> appContext().resources.getString(R.string.shuffle)
        Radio -> appContext().resources.getString(R.string.start_radio)
        Search -> appContext().resources.getString(R.string.search)
    }

    val icon: Int
        get() = when (this) {
            Favorites -> R.drawable.heart_outline
            Repeat -> R.drawable.repeat
            Shuffle -> R.drawable.shuffle
            Radio -> R.drawable.radio
            Search -> R.drawable.search
        }

        @OptIn(UnstableApi::class)
        fun getStateIcon(button: NotificationButtons, likedState: Long?, repeatMode: Int, shuffleMode: Boolean): Int {
            return when (button) {
                Favorites -> when (likedState) {
                    -1L -> R.drawable.heart_dislike
                    null -> R.drawable.heart_outline
                    else -> R.drawable.heart
                }
                Repeat -> when (repeatMode) {
                    REPEAT_MODE_OFF -> R.drawable.repeat
                    REPEAT_MODE_ONE -> R.drawable.repeatone
                    REPEAT_MODE_ALL -> R.drawable.infinite
                    else -> throw IllegalStateException()
                }
                Shuffle -> if (shuffleMode) R.drawable.shuffle_filled else R.drawable.shuffle
                Radio -> R.drawable.radio
                Search -> R.drawable.search
            }

        }

}