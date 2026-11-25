package it.fast4x.riplay.extensions.zoundhub

import android.content.Context
import android.content.Intent
import android.net.Uri
import it.fast4x.riplay.data.models.Artist
import it.fast4x.zoundhub.ZoundHubService

/**
 * "Suggerisci artista a ZoundHub" (Android).
 *
 * Apre nel browser il form GitHub "nuova issue" del repo xoundbyte/zoundhub
 * con titolo e corpo pre-compilati, per proporre l'inserimento (o la
 * correzione) dell'artista corrente nel catalogo zoundhub.com.
 *
 * SPOTIFY URL: la fonte preferita e' lo spotifyId matchato dal resolver
 * MusicBrainz (Ponte ZoundHub, popolato da MBMetadataHelper.onArtistViewed
 * e dai backfiller). Se assente si usa l'eventuale link spotify presente
 * nelle url-rels MB (artist.links). Se nessuna delle due fonti e'
 * disponibile il campo resta VUOTO come nel template del repo: l'utente
 * lo incolla manualmente prima dell'invio.
 *
 * NOTE: si allegano i link gia' noti dell'artista (YouTube Music dal
 * browseId, MusicBrainz dal mbId, Wikipedia) piu' la provenienza del
 * report: risparmiano all'utente la copia manuale e rendono la issue
 * subito utile a chi mantiene il catalogo.
 */

/** Note standard della suggestion con i link noti dell'artista. */
fun zoundHubArtistSuggestionNotes(
    artist: Artist?,
    fallbackArtistName: String?,
    platformLabel: String = "RiPlay (Android): https://github.com/fast4x/RiPlay",
): List<String> = buildList {
    val name = artist?.name ?: fallbackArtistName
    if (!name.isNullOrBlank()) add("Artist name: $name")
    artist?.shareYTMUrl?.let { add("YouTube Music: $it") }
    artist?.mbId?.let { add("MusicBrainz: https://musicbrainz.org/artist/$it") }
    artist?.wikipediaUrl?.takeIf { it.isNotBlank() }?.let { add("Wikipedia: $it") }
    add("Reported from $platformLabel")
}

/**
 * URL Spotify noto per l'artista: prima lo spotifyId dal resolver
 * MusicBrainz (o confermato dall'utente nel dialog di risoluzione),
 * poi l'eventuale link spotify tra le url-rels MB.
 */
fun zoundHubArtistSpotifyUrl(artist: Artist?, spotifyIdOverride: String? = null): String? {
    val effectiveId = spotifyIdOverride ?: artist?.spotifyId
    return effectiveId?.let { ZoundHubService.spotifyArtistUrl(it) }
        ?: artist?.links?.firstOrNull { it.platform == "spotify" }?.url
}

/** URL completo del form GitHub issue pre-compilato per l'artista. */
fun zoundHubArtistSuggestionUrl(
    artist: Artist?,
    fallbackArtistName: String?,
    spotifyIdOverride: String? = null,
): String = ZoundHubService.newArtistIssueUrl(
    title = "${ZoundHubService.DEFAULT_ISSUE_TITLE}: ${artist?.name ?: fallbackArtistName}",
    spotifyArtistUrl = zoundHubArtistSpotifyUrl(artist, spotifyIdOverride),
    notes = zoundHubArtistSuggestionNotes(artist, fallbackArtistName)
)

/**
 * Apre il form di suggestion nel browser (silenzioso se non c'e' browser).
 * [spotifyIdOverride]: ID appena risolto col dialog (gia' salvato nel db
 * dal chiamante) — evita la rilettura dell'entity prima dell'apertura.
 */
fun openZoundHubArtistSuggestion(
    context: Context,
    artist: Artist?,
    fallbackArtistName: String?,
    spotifyIdOverride: String? = null,
) {
    try {
        val url = zoundHubArtistSuggestionUrl(artist, fallbackArtistName, spotifyIdOverride)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // nessun browser disponibile: silenzioso, non e' un'azione critica
    }
}
