package it.fast4x.environment.requests

import it.fast4x.environment.Environment
import it.fast4x.environment.Environment.getBestQuality
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.models.MusicCarouselShelfRenderer
import it.fast4x.environment.models.MusicTwoRowItemRenderer
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.oddElements
import it.fast4x.environment.models.withoutSeparators
import kotlinx.serialization.Serializable

@Serializable
data class HomePage(
    val sections: List<Section>,
    val chips: List<Environment.Chip>?,
    val continuation: String? = null,
) {

    @Serializable
    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<Environment.Item?>,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {

                val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.withoutSeparators()?.firstOrNull()?.text

                val fromTwoRowItemRenderer = renderer.contents
                    .mapNotNull {
                        fromMusicTwoRowItemRenderer(
                            it.musicTwoRowItemRenderer,
                            title
                        )
                    }

                val fromMusicResponsiveListItemRenderer =
                    fromMusicResponsiveListItemRenderer(renderer.contents)


                //println("getHomePage() fromMusicCarouselShelfRenderer sction title: $title items: ${renderer.contents.map { it.musicResponsiveListItemRenderer?.flexColumns?.map { it.musicResponsiveListItemFlexColumnRenderer }}}")
                println("getHomePage() fromMusicCarouselShelfRenderer sction title: $title fromTwoRowItemRenderer: $fromTwoRowItemRenderer fromMusicResponsiveListItemRenderer: $fromMusicResponsiveListItemRenderer")

                return Section(
                    title = title
                        ?: "",
                    label = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.withoutSeparators()?.firstOrNull()?.text,
                    thumbnail = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),

                    endpoint = BrowseEndpoint(
                        browseId = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId
                            ?: "",
                    ),
                    items = (fromTwoRowItemRenderer + fromMusicResponsiveListItemRenderer).filterNotNull()
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer?, sectionTitle: String? = null): Environment.Item? {

                return when {
                    renderer?.isSong == true -> {

                        Environment.SongItem(
                            info = Environment.Info(
                                renderer.title?.runs?.withoutSeparators()?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.watchEndpoint
                            ),
                            authors = renderer.subtitle?.runs?.withoutSeparators()?.map {
                                Environment.Info(
                                    name = it.text,
                                    endpoint = it.navigationEndpoint?.browseEndpoint
                                )
                            }?.filterNot { it.endpoint == null },
                            album = null,
                            durationText = null,
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer?.isAlbum == true -> {

                        Environment.AlbumItem(
                            info = Environment.Info(
                                renderer.title?.runs?.withoutSeparators()?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.browseEndpoint
                            ),
//                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
//                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
//                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
//                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            authors = renderer.subtitle?.runs?.withoutSeparators()?.drop(1)?.map {
                                Environment.Info(
                                    name = it.text,
                                    endpoint = it.navigationEndpoint?.browseEndpoint
                                )
                            }?.filterNot { it.endpoint == null },
                            year = renderer.subtitle?.runs?.lastOrNull()?.text,
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
//                            explicit = renderer.subtitleBadges?.find {
//                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
//                            } != null
                        )
                    }

                    renderer?.isPlaylist == true || renderer?.isPodcast == true  -> {

                        Environment.PlaylistItem(
                            info = Environment.Info(
                                renderer.title?.runs?.withoutSeparators()?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.browseEndpoint
                            ),
                            songCount = null,
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                            channel = null,
                            isEditable = false
                        )
                    }

                    renderer?.isArtist == true -> {

                        Environment.ArtistItem(
                            info = Environment.Info(
                                renderer.title?.runs?.withoutSeparators()?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.browseEndpoint
                            ),
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                            subscribersCountText = null
                        )
                    }

                    renderer?.isVideo == true -> {

                        Environment.VideoItem(
                            info = Environment.Info(
                                renderer.title?.runs?.withoutSeparators()?.firstOrNull()?.text,
                                renderer.navigationEndpoint?.watchEndpoint
                            ),
                            authors = renderer.subtitle?.runs?.withoutSeparators()?.map {
                                Environment.Info(
                                    name = it.text,
                                    endpoint = it.navigationEndpoint?.browseEndpoint
                                )
                            }?.filterNot { it.endpoint == null },
                            durationText = null,
                            thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                            viewsText = null
                        )

                    }

                    else -> {
                        //println("getHomePage() fromMusicTwoRowItemRenderer else renderer: ${renderer}")
                        null
                    }
                }
            }

            private fun fromMusicResponsiveListItemRenderer(contents: List<MusicCarouselShelfRenderer.Content>, sectionTitle: String? = null): List<Environment.Item?> {

                return contents.map { it.musicResponsiveListItemRenderer }
                    .map { renderer ->
                        when {
                            renderer?.isSong == true -> {
                                val content = renderer.flexColumns
                                val artists =
                                    content.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()
                                val album =
                                    content.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()
                                val duration =
                                    renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()?.text
                                val thumbnail =
                                    renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality()
                                val isExplicit =
                                    renderer.badges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null
                                val title =
                                    content.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()?.text

                                Environment.SongItem(
                                    info = Environment.Info(
                                        title,
                                        NavigationEndpoint.Endpoint.Watch(
                                            videoId = renderer.playlistItemData?.videoId
                                        )
                                    ),
                                    authors = artists?.map {
                                        Environment.Info(
                                            name = it.text,
                                            endpoint = it.navigationEndpoint?.browseEndpoint
                                        )
                                    }?.filterNot { it.endpoint == null },
                                    album = Environment.Info(
                                        name = album?.text,
                                        endpoint = album?.navigationEndpoint?.browseEndpoint
                                    ),
                                    durationText = duration,
                                    thumbnail = thumbnail,
                                    explicit = isExplicit
                                )
                            }

                            renderer?.isPlaylist == true || renderer?.isPodcast == true -> {

                                val title =
                                    renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()?.text
                                val channel =
                                    renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()
                                Environment.PlaylistItem(
                                    info = Environment.Info(
                                        title,
                                        renderer.navigationEndpoint?.browseEndpoint
                                    ),
                                    songCount = null,
                                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                                    channel = channel.let {
                                        Environment.Info(
                                            it?.text,
                                            it?.navigationEndpoint?.browseEndpoint
                                        )
                                    },
                                    isEditable = false
                                )
                            }

                            renderer?.isArtist == true -> {

                                val title =
                                    renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()?.text
                                Environment.ArtistItem(
                                    info = Environment.Info(
                                        title,
                                        renderer.navigationEndpoint?.browseEndpoint
                                    ),
                                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                                    subscribersCountText = null
                                )
                            }

                            renderer?.isVideo == true -> {
                                val artists =
                                    renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()
                                val duration =
                                    renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()?.text
                                val thumbnail =
                                    renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality()
                                val title =
                                    renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.withoutSeparators()?.firstOrNull()?.text

                                Environment.VideoItem(
                                    info = Environment.Info(
                                        title,
                                        renderer.navigationEndpoint?.watchEndpoint
                                    ),
                                    authors = artists?.map {
                                        Environment.Info(
                                            name = it.text,
                                            endpoint = it.navigationEndpoint?.browseEndpoint
                                        )
                                    }?.filterNot { it.endpoint == null },
                                    durationText = duration,
                                    thumbnail = thumbnail,
                                    viewsText = null
                                )

                            }

                            else -> {
                                //println("getHomePage() fromMusicTwoRowItemRenderer else renderer: ${renderer}")
                                null
                            }
                        }
                    }

            }

        }
    }
}

