package it.fast4x.environment.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import it.fast4x.environment.Environment
import it.fast4x.environment.models.ContinuationResponse
import it.fast4x.environment.models.MusicResponsiveListItemRenderer
import it.fast4x.environment.models.MusicShelfRenderer
import it.fast4x.environment.models.SearchResponse
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.utils.runCatchingNonCancellable

suspend fun <T : Environment.Item> Environment.searchPage(
    body: SearchBody,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {


    val responseApi = client.post(_QPmE9fYezr) {
        setLogin(clientType = body.context.client, setLogin = true)
        setBody(body)
        //mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$musicResponsiveListItemRendererMask)")
    }

    println("Environment.searchPage responseApi ${responseApi.bodyAsText()}")

    val response = responseApi.body<SearchResponse>()

    println("Environment.searchPage response ${response.contents}")

    response
        .contents
        ?.tabbedSearchResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.lastOrNull()
        ?.musicShelfRenderer
        ?.toItemsPage(fromMusicShelfRendererContent)
}?.onFailure {
    println("Environment.searchPage error ${it.stackTraceToString()}")
}

suspend fun <T : Environment.Item> Environment.searchPage(
    body: ContinuationBody,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {
    val response = client.post(_QPmE9fYezr) {
        setBody(body)
        mask("continuationContents.musicShelfContinuation(continuations,contents.$musicResponsiveListItemRendererMask)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(fromMusicShelfRendererContent)
}

private fun <T : Environment.Item> MusicShelfRenderer?.toItemsPage(mapper: (MusicShelfRenderer.Content) -> T?) =
    Environment.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(mapper),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )

private fun <T : Environment.Item> MusicResponsiveListItemRenderer?.toItemsPage(mapper: (MusicResponsiveListItemRenderer.FlexColumn) -> T?) =
    Environment.ItemsPage(
        items = this
            ?.flexColumns
            ?.mapNotNull(mapper),
        continuation = null
    )
