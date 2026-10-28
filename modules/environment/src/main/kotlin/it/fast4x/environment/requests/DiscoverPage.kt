package it.fast4x.environment.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.environment.Environment
import it.fast4x.environment.models.BrowseResponse
import it.fast4x.environment.models.Context.Companion.DefaultWeb
import it.fast4x.environment.models.Context.Companion.DefaultWebWithLocale
import it.fast4x.environment.models.MusicTwoRowItemRenderer
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.models.bodies.BrowseBodyWithLocale
import it.fast4x.environment.models.oddElements
import it.fast4x.environment.models.splitBySeparator

suspend fun Environment.discoverPage(language: String, country: String) = runCatching {

    val response = client.post(_3djbhqyLpE) {
        setBody(
            BrowseBodyWithLocale(
//                context = DefaultWeb.copy(
//                    client = DefaultWeb.client.setLocale(language, country)
//                ),
                // STEP 3ap: contesto con hl/gl LIVE da Environment.locale
                // (DesktopI18n.syncEnvironmentLocale): prima qui arrivava
                // il DefaultWebWithLocale congelato alla class-init (sul
                // desktop senza LocalePreferences = NESSUN hl) -> i moods
                // della home restavano nella lingua dell avvio anche dopo
                // un cambio lingua a runtime (le sezioni invece erano gia
                // live via Environment.browse). Stesso schema del visitorData
                // LIVE introdotto in Environment.browse (step3am).
                context = DefaultWebWithLocale.copy(
                    client = DefaultWebWithLocale.client.setLocale(
                        Environment.locale.hl, Environment.locale.gl
                    )
                ),
                browseId = "FEmusic_explore"
            )
        )
        mask("contents")
    }.body<BrowseResponse>()

    Environment.DiscoverPage(
        newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs
            ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer
                    ?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums"
            }?.musicCarouselShelfRenderer?.contents?.mapNotNull { it.musicTwoRowItemRenderer?.toNewReleaseAlbumPage() }
            .orEmpty(),
        moods = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer
                    ?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_moods_and_genres"
            }?.musicCarouselShelfRenderer?.contents?.mapNotNull { it.musicNavigationButtonRenderer?.toMood() }
            .orEmpty()
    )
}

suspend fun Environment.discoverPageNewAlbums() = runCatching {
    val response = client.post(_3djbhqyLpE) {
        setBody(BrowseBodyWithLocale(browseId = "FEmusic_explore"))
        mask("contents")
    }.body<BrowseResponse>()

    Environment.DiscoverPageAlbums(
        newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs
            ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer
                    ?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums"
            }?.musicCarouselShelfRenderer?.contents?.mapNotNull { it.musicTwoRowItemRenderer?.toNewReleaseAlbumPage() }
            .orEmpty()
    )
}

suspend fun Environment.discoverPageNewAlbumsComplete() = runCatching {
    val response = client.post(_3djbhqyLpE) {
        setBody(BrowseBodyWithLocale(browseId = "FEmusic_new_releases_albums"))
        mask("contents")
    }.body<BrowseResponse>()

    Environment.DiscoverPageAlbums(
        newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs
            ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items?.mapNotNull { it.musicTwoRowItemRenderer?.toNewReleaseAlbumPage() }
            .orEmpty()
    )
}

fun MusicTwoRowItemRenderer.toNewReleaseAlbumPage() = Environment.AlbumItem(
    info = Environment.Info(
        name = title?.text,
        endpoint = navigationEndpoint?.browseEndpoint
    ),
    authors = subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
        Environment.Info(
            name = it.text,
            endpoint = it.navigationEndpoint?.browseEndpoint
        )
    },
    year = subtitle?.runs?.lastOrNull()?.text,
    thumbnail = thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
)