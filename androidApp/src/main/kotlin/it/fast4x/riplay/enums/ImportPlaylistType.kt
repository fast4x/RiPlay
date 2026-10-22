package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.R

enum class ImportPlaylistType {
    Riplay,
    //ExportifyNet,
    TuneMyMusicDeezer,
    SpotifyUrl;

    val titleId: Int
        get() = when(this) {
            Riplay -> R.string.import_playlist_riplay
            //ExportifyNet -> R.string.import_playlist_exportify_net_spotify
            TuneMyMusicDeezer -> R.string.import_playlist_tune_my_music_deezer
            SpotifyUrl -> R.string.import_playlist_spotify_url
        }

    val iconId: Int
        get() = when(this) {
            Riplay -> R.drawable.app_icon
            //ExportifyNet -> R.drawable.logo_spotify
            TuneMyMusicDeezer -> R.drawable.logo_deezer
            SpotifyUrl -> R.drawable.logo_spotify
        }

    val menuItem: GenericMenuItem
        @Composable
        get() = GenericMenuItem( this.ordinal, titleId, iconId )

}