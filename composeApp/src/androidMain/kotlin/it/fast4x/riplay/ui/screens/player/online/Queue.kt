package it.fast4x.riplay.ui.screens.player.online

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.service.OfflinePlayerService
import it.fast4x.riplay.utils.mediaItems


@OptIn(UnstableApi::class)
fun queue(binder: OfflinePlayerService.Binder?): List<String> {
    return binder?.player?.mediaItems?.map { it.mediaId } ?: emptyList()
}