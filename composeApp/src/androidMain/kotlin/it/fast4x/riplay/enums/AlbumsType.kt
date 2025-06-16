package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class AlbumsType {
    Favorites,
    Library;
    //All;

    val textName: String
        @Composable
        get() = when( this ) {
            Favorites -> stringResource(R.string.favorites)
            Library -> stringResource(R.string.library)
            //All -> stringResource(R.string.all)
        }

}