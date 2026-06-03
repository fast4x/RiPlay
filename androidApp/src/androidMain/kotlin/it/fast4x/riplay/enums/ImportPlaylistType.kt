package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.R

enum class ImportPlaylistType {
    Riplay,
    ExportifyNet;

    val titleId: Int
        get() = when(this) {
            Riplay -> R.string.import_playlist_riplay
            ExportifyNet -> R.string.import_playlist_exportify_net
        }

    val iconId: Int
        get() = when(this) {
            Riplay -> R.drawable.app_icon
            ExportifyNet -> R.drawable.resource_import
        }

    val menuItem: GenericMenuItem
        @Composable
        get() = GenericMenuItem( this.ordinal, titleId, iconId )

}