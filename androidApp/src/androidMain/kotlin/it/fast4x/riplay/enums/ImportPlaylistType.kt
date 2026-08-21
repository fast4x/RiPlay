package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.R

enum class ImportPlaylistType {
    Riplay,
    ExportifyNet,
    TuneMyMusicDeezer;

    val titleId: Int
        get() = when(this) {
            Riplay -> R.string.import_playlist_riplay
            ExportifyNet -> R.string.import_playlist_exportify_net
            TuneMyMusicDeezer -> R.string.import_playlist_tune_my_music_deezer
        }

    val iconId: Int
        get() = when(this) {
            Riplay -> R.drawable.app_icon
            ExportifyNet -> R.drawable.logo_spotify
            TuneMyMusicDeezer -> R.drawable.logo_deezer
        }

    val menuItem: GenericMenuItem
        @Composable
        get() = GenericMenuItem( this.ordinal, titleId, iconId )

}