package it.fast4x.riplay.utils

import it.fast4x.riplay.data.models.Song
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.requests.playlistPage
import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.environment.utils.getProxy

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex().matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}

fun getHttpClient() = HttpClient() {
    install(UserAgent) {
        agent = "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0"
    }
    engine {
        ProxyPreferences.preference?.let{
            proxy = getProxy(it)
        }
    }
}

suspend fun Result<Environment.PlaylistOrAlbumPage>.completed(
    maxDepth: Int =  Int.MAX_VALUE
) = runCatching {
    val page = getOrThrow()
    val songs = page.songsPage?.items.orEmpty().toMutableList()
    var continuation = page.songsPage?.continuation

    var depth = 0
    var continuationsList = arrayOf<String>()
    //continuationsList += continuation.orEmpty()

    while (continuation != null && depth++ < maxDepth) {
        val newSongs = Environment
            .playlistPage(
                body = ContinuationBody(continuation = continuation)
            )
            ?.getOrNull()
            ?.takeUnless { it.items.isNullOrEmpty() } ?: break

        newSongs.items?.let { songs += it.filter { it !in songs } }
        continuation = newSongs.continuation

        //println("mediaItem loop $depth continuation founded ${continuationsList.contains(continuation)} $continuation")
        if (continuationsList.contains(continuation)) break

        continuationsList += continuation.orEmpty()
        //println("mediaItem loop continuationList size ${continuationsList.size}")
    }

    page.copy(songsPage = Environment.ItemsPage(items = songs, continuation = null))
}.also { it.exceptionOrNull()?.printStackTrace() }

val Environment.SongItem.asSong: Song
    get() = Song (
        id = key,
        title = info?.name ?: "",
        artistsText = authors?.joinToString(", ") { it.name ?: "" },
        durationText = durationText,
        thumbnailUrl = thumbnail?.url
    )

val Environment.SongItem.asSongEntity: SongEntity
    get() = SongEntity (
        song = Song (
            id = key,
            title = info?.name ?: "",
            artistsText = authors?.joinToString(", ") { it.name ?: "" },
            durationText = durationText,
            thumbnailUrl = thumbnail?.url
            ),
        contentLength = null,
        albumTitle = null
    )

/**
 * STEP 3n (richiesta utente n.7 "la qualita' della cover del player mini e'
 * molto bassa"): la miniatura che arriva da YouTube Music e' di solito il
 * miniaturizzato a 60/120px (=w60-h60-l90-rj oppure default.jpg 120x90).
 * Il pannello player e' largo 300-400px: ingrandire un'immagine cosi'
 * piccola la rende sfocata.
 *
 * Questa funzione chiede alla STESSA URL la risoluzione alta: i server CDN
 * di googleusercontent ridimensionano in URL (basta cambiare il suffisso
 * "=wNNN-hNNN"), mentre per i file i.ytimg.com si sostituisce default.jpg
 * o mqdefault.jpg con hqdefault.jpg (480x360, sempre disponibile; la
 * maxres NON esiste per tutti i video quindi non si usa).
 *
 * URL non riconosciuti = ritornano INVARIATI (nessun rischio per le
 * immagini che arrivano da altre fonti). In caso di regex imprevista
 * ritorna l'originale (runCatching).
 */
fun String.hiResCoverUrl(): String = runCatching {
    when {
        // 1) miniatura video standard di YouTube: default (120px) e mq (320px)
        contains("/default.jpg") -> replace("/default.jpg", "/hqdefault.jpg")
        contains("/mqdefault.jpg") -> replace("/mqdefault.jpg", "/hqdefault.jpg")

        // 2) CDN googleusercontent / ggpht: il suffisso "=..." decide la dimensione
        contains("googleusercontent.com") || contains("ggpht.com") -> {
            val match = Regex("=([ws]\\d+[^=]*)$").find(this)
            when {
                // nessun suffisso di dimensione (e nessuna query): lo si puo'
                // appendere, e' l'API di ridimensionamento del CDN
                match == null && !contains("?") -> "$this=w544-h544-l90-rj"
                match == null -> this
                else -> {
                    val current = match.groupValues[1]
                    val width = Regex("^[ws](\\d+)").find(current)
                        ?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    // gia' abbastanza grande: invariata
                    if (width >= 480) this
                    else replaceRange(match.range, "=w544-h544-l90-rj")
                }
            }
        }

        else -> this
    }
}.getOrDefault(this)