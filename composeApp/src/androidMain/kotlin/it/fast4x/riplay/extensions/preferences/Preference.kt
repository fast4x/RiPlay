package it.fast4x.riplay.extensions.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import it.fast4x.riplay.enums.HomeItemSize

object Preference {

    /****  ENUMS  ****/
    val HOME_ARTIST_ITEM_SIZE = Key( "AristItemSizeEnum", HomeItemSize.BIG )
    val HOME_ALBUM_ITEM_SIZE = Key( "AlbumItemSizeEnum", HomeItemSize.BIG )
    val HOME_LIBRARY_ITEM_SIZE = Key( "LibraryItemSizeEnum", HomeItemSize.BIG )

    @Composable
    inline fun <reified T: Enum<T>> remember( key: Key<T>): MutableState<T> =
        rememberPreference( key.key, key.default )

    /**
     * In order to ensure consistent between input key and output value.
     * The provided key must bear a potential return value.
     */
    data class Key<T>( val key: String, val default: T )
}