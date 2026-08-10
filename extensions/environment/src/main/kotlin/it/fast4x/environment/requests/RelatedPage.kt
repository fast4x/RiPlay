package it.fast4x.environment.requests


import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import it.fast4x.environment.Environment
import it.fast4x.environment.models.BrowseResponse
import it.fast4x.environment.models.MusicCarouselShelfRenderer
import it.fast4x.environment.models.NextResponse
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.utils.findSectionByStrapline
import it.fast4x.environment.utils.findSectionByTitle
import it.fast4x.environment.utils.from
import it.fast4x.environment.utils.runCatchingNonCancellable
import kotlinx.serialization.ExperimentalSerializationApi


@ExperimentalSerializationApi
suspend fun Environment.relatedPage(body: NextBody) = runCatchingNonCancellable {

    val nextResponse = next(
        body.context,
        body.videoId,
        body.playlistId,
        body.playlistSetVideoId,
        body.index,
        body.params,
        body.continuation
    ).body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.getOrNull(3)
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId

    println("Environment relatedPage related browseId $browseId")

    val response = client.post(_3djbhqyLpE) {
        setLogin(setLogin = true)
        setBody(BrowseBody(browseId = browseId))
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    println("Environment relatedPage sectionListRenderer $sectionListRenderer")

    Environment.RelatedPage(
        songs = sectionListRenderer
            ?.findSectionByTitle("You might also like")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Environment.SongItem::from),
        playlists = sectionListRenderer
            ?.findSectionByTitle("Recommended playlists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Environment.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = sectionListRenderer
            ?.findSectionByStrapline("MORE FROM")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Environment.AlbumItem::from),
        artists = sectionListRenderer
            ?.findSectionByTitle("Similar artists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Environment.ArtistItem::from),
    )
}?.onFailure {
    println("ERROR in Innertube Failed relatedPage ${it.stackTraceToString()}")
}
