package it.fast4x.riplay.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.GenericMenuItem

enum class OnDeviceSongSortBy(
    @StringRes val textId: Int,
    @DrawableRes val iconId: Int
): MenuTitle, Drawable {

    Title( R.string.sort_title, R.drawable.text ),

    DateAdded( R.string.sort_date_played, R.drawable.calendar ),

    Artist( R.string.sort_artist, R.drawable.artist ),

    Duration( R.string.sort_duration, R.drawable.time ),

    Album( R.string.sort_album, R.drawable.music_album );

    override val titleId: Int
        get() = this.textId

    override val icon: Painter
        @Composable
        get() = painterResource( this.iconId )

    val menuItem: GenericMenuItem
        @Composable
        get() = GenericMenuItem( this.ordinal,textId, iconId )

}
