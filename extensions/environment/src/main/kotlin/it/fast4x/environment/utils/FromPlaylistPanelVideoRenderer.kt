package it.fast4x.environment.utils

import it.fast4x.environment.Environment
import it.fast4x.environment.models.PlaylistPanelVideoRenderer

fun Environment.SongItem.Companion.from(renderer: PlaylistPanelVideoRenderer): Environment.SongItem? {

    val thumbnail = renderer
        .thumbnail
        ?.thumbnails
        ?.getOrNull(0)

    val isVideo = (thumbnail?.width ?: 0) > (thumbnail?.height ?: 0)

    return Environment.SongItem(
        info = Environment.Info(
            name = renderer
                .title
                ?.text,
            endpoint = renderer
                .navigationEndpoint
                ?.watchEndpoint
        ),
        authors = renderer
            .longBylineText
            ?.splitBySeparator()
            ?.getOrNull(0)
            ?.map(Environment::Info),
        album = renderer
            .longBylineText
            ?.splitBySeparator()
            ?.getOrNull(1)
            ?.getOrNull(0)
            ?.let(Environment::Info),
        thumbnail = thumbnail,
        durationText = renderer
            .lengthText
            ?.text,
        isAudioOnly = !isVideo
    ).takeIf { it.info?.endpoint?.videoId != null }
}
