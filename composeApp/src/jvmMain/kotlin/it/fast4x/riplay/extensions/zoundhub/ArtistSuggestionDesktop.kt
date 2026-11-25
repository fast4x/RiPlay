package it.fast4x.riplay.extensions.zoundhub

import it.fast4x.riplay.data.models.Artist
import it.fast4x.zoundhub.ZoundHubService
import java.awt.Desktop
import java.net.URI

/**
 * "Suggerisci artista a ZoundHub" (DESKTOP — specchio di
 * androidApp .../extensions/zoundhub/ArtistSuggestion.kt).
 *
 * Apre nel browser di sistema il form GitHub "nuova issue" del repo
 * xoundbyte/zoundhub con titolo e corpo pre-compilati, per proporre
 * l'inserimento (o la correzione) dell'artista corrente nel catalogo
 * zoundhub.com.
 *
 * SPOTIFY URL: la fonte preferita e' lo spotifyId matchato dal resolver
 * MusicBrainz ("Ponte ZoundHub" sull'entita' Artist). Su desktop
 * l'arricchimento MusicBrainz gira lato Android app: se lo spotifyId non
 * e' ancora in db il campo resta VUOTO come nel template del repo e
 * l'utente incolla l'URL prima dell'invio. Come fonte secondaria si usa
 * l'eventuale link spotify tra le url-rels MB (artist.links).
 *
 * NOTE: si allegano i link gia' noti dell'artista (YouTube Music dal
 * browseId, MusicBrainz dal mbId, Wikipedia) piu' la provenienza del
 * report, cosi' la issue e' subito utile a chi mantiene il catalogo.
 */

/** Note standard della suggestion con i link noti dell'artista (desktop). */
fun zoundHubArtistSuggestionNotesDesktop(
    artist: Artist?,
    fallbackArtistName: String?,
): List<String> = buildList {
    val name = artist?.name ?: fallbackArtistName
    if (!name.isNullOrBlank()) add("Artist name: $name")
    artist?.shareYTMUrl?.let { add("YouTube Music: $it") }
    artist?.mbId?.let { add("MusicBrainz: https://musicbrainz.org/artist/$it") }
    artist?.wikipediaUrl?.takeIf { it.isNotBlank() }?.let { add("Wikipedia: $it") }
    add("Reported from RiPlay (Desktop): https://github.com/fast4x/RiPlay")
}

/**
 * URL Spotify noto per l'artista: prima lo spotifyId dal resolver
 * MusicBrainz (o confermato dall'utente nel dialog di risoluzione),
 * poi l'eventuale link spotify tra le url-rels MB.
 */
fun zoundHubArtistSpotifyUrlDesktop(artist: Artist?, spotifyIdOverride: String? = null): String? {
    val effectiveId = spotifyIdOverride ?: artist?.spotifyId
    return effectiveId?.let { ZoundHubService.spotifyArtistUrl(it) }
        ?: artist?.links?.firstOrNull { it.platform == "spotify" }?.url
}

/** URL completo del form GitHub issue pre-compilato per l'artista (desktop). */
fun zoundHubArtistSuggestionUrlDesktop(
    artist: Artist?,
    fallbackArtistName: String?,
    spotifyIdOverride: String? = null,
): String = ZoundHubService.newArtistIssueUrl(
    title = "${ZoundHubService.DEFAULT_ISSUE_TITLE}: ${artist?.name ?: fallbackArtistName}",
    spotifyArtistUrl = zoundHubArtistSpotifyUrlDesktop(artist, spotifyIdOverride),
    notes = zoundHubArtistSuggestionNotesDesktop(artist, fallbackArtistName)
)

/** Apre il form di suggestion nel browser di sistema (silenzioso se assente). */
fun openZoundHubArtistSuggestionDesktop(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (t: Throwable) {
        // nessun browser: silenzioso, non e' un'azione critica
    }
}
