package it.fast4x.environment.utils

import it.fast4x.environment.Environment
import it.fast4x.environment.models.PlaylistPanelVideoRenderer
import it.fast4x.environment.models.WatchEndpoint

fun Environment.SongItem.Companion.from(renderer: PlaylistPanelVideoRenderer): Environment.SongItem? {

    val thumbnail = renderer
        .thumbnail
        ?.thumbnails
        ?.getOrNull(0)

    val musicVideoType = renderer
        .navigationEndpoint
        ?.watchEndpoint
        ?.watchEndpointMusicSupportedConfigs
        ?.watchEndpointMusicConfig
        ?.musicVideoType

    val isVideoFromType = when (musicVideoType) {
        WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.MUSIC_VIDEO_TYPE_OMV,
        WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.MUSIC_VIDEO_TYPE_UGC -> true
        else -> false
    }

    // Fallback to thumbnail ratio only when the endpoint does not expose a video type.
    val isVideoFromThumbnail = musicVideoType == null && (thumbnail?.width ?: 0) > (thumbnail?.height ?: 0)
    val isVideo = isVideoFromType || isVideoFromThumbnail

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
