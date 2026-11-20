package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.environment.Environment
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.zoundhub.ZoundHubClient
import it.fast4x.zoundhub.ZoundHubCatalogState
import it.fast4x.zoundhub.ZoundHubMatch
import it.fast4x.zoundhub.ZoundHubService
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Badge AI (ZoundHub) per artisti / album / brani.
 *
 * SEMANTICA: ZoundHub segnala a livello di ARTISTA (il campo `releases`
 * e' solo un conteggio). Il badge su album e brani e' quindi una
 * PROPAGAZIONE del segnale dell'artista: il dialog lo dichiara sempre
 * esplicitamente all'utente (stringa zoundhub_dialog_propagated).
 *
 * Uso tipico nelle righe item:
 *   AiArtistBadge(artistText = authors)
 * Il badge calcola da solo il match (rispettando l'impostazione
 * isEnabledZoundHub e lo stato del catalogo) e apre il dialog dettagliato.
 */

/** Colore condiviso del segnale AI (stessa famiglia del NatureBadge AI_GENERATED). */
private val AiBadgeColor = Color(0xFFE91E63)

/**
 * Helper composable: restituisce il match ZoundHub per il testo artista
 * corrente, oppure null se:
 *  - l'impostazione isEnabledZoundHub e' OFF;
 *  - il catalogo non e' pronto (mai scaricato / refresh in corso);
 *  - non esiste match (esatto o fuzzy sopra soglia).
 */
@Composable
fun rememberZoundHubMatch(artistText: String?): ZoundHubMatch? {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    if (!appSettings.isEnabledZoundHub) return null

    val store = ZoundHubClient.store
    val state by store.state.collectAsState()

    if (state.status != ZoundHubCatalogState.Status.Ready) return null

    return remember(artistText, state.catalog) { store.lookup(artistText) }
}

/**
 * Badge AI autonomo: calcola il match e gestisce il dialog dettagliato.
 * Nasce per essere inserito in una Row esistente senza toccare il layout:
 *   Row { AiArtistBadge(authors); BasicText(...) }
 */
@Composable
fun AiArtistBadge(
    artistText: String?,
    modifier: Modifier = Modifier,
) {
    val match = rememberZoundHubMatch(artistText) ?: return

    var showDialog by remember { mutableStateOf(false) }

    AiBadge(
        match = match,
        onClick = { showDialog = true },
        modifier = modifier
    )

    if (showDialog) {
        AiInfoDialog(
            match = match,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Badge visuale. Se il match e' fuzzy (incerto) mostra "AI?" invece di "AI".
 */
@Composable
fun AiBadge(
    match: ZoundHubMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uncertain = !match.exact

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = AiBadgeColor.copy(alpha = 0.18f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            BasicText(
                text = "\uD83E\uDD16", // robot
                style = TextStyle(fontSize = 9.sp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(3.dp))
            BasicText(
                text = if (uncertain) stringResource(R.string.zoundhub_badge_ai_uncertain)
                else stringResource(R.string.zoundhub_badge_ai),
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AiBadgeColor
                ),
                maxLines = 1
            )
        }
    }
}

/** Riga chiave/valore del dialog info. */
@Composable
private fun AiInfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        BasicText(
            text = label,
            style = typography().xs.semiBold.secondary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicText(
            text = value,
            style = typography().xs.semiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Formatta una similarita' (0..1) come percentuale intera. */
private fun formatSimilarity(similarity: Double): String =
    "${(similarity * 100).toInt()}%"

/** Formatta un epoch millis come data locale breve. */
fun formatZoundHubDate(epochMillis: Long?): String =
    epochMillis?.let {
        SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Locale.getDefault()).format(Date(it))
    } ?: ""

/**
 * Dialog dettagliato con le informazioni ZoundHub dell'artista matchato:
 * rating, punteggio SubmitHub, flag Deezer, release, debutto, follower,
 * avviso di match incerto (fuzzy), nota di propagazione e link alla pagina
 * pubblica zoundhub.com/artist/{slug}.
 */
@Composable
fun AiInfoDialog(
    match: ZoundHubMatch,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val artist = match.artist

    DefaultDialog(onDismiss = onDismiss) {
        BasicText(
            text = stringResource(R.string.zoundhub_dialog_title),
            style = typography().m.semiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = "\uD83E\uDD16 ${artist.name}",
            style = typography().l.semiBold.copy(color = AiBadgeColor),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            AiInfoRow(
                label = stringResource(R.string.zoundhub_dialog_rating),
                value = artist.rating?.let { "$it / 5" } ?: "-"
            )
            AiInfoRow(
                label = stringResource(R.string.zoundhub_dialog_shscore),
                value = artist.shScore?.let { "%.1f".format(it) } ?: "-"
            )
            AiInfoRow(
                label = stringResource(R.string.zoundhub_dialog_deezer_yes),
                value = when (artist.deezer) {
                    true -> "\u2713"
                    false -> "\u2715"
                    else -> "-"
                }
            )
            AiInfoRow(
                label = stringResource(R.string.zoundhub_dialog_releases),
                value = artist.releases?.toString() ?: "-"
            )
            artist.debut?.takeIf { it.isNotBlank() }?.let {
                AiInfoRow(label = stringResource(R.string.zoundhub_dialog_debut), value = it)
            }
            artist.followers?.let {
                AiInfoRow(label = stringResource(R.string.zoundhub_dialog_followers), value = it.toString())
            }
            artist.listeners?.let {
                AiInfoRow(label = stringResource(R.string.zoundhub_dialog_listeners), value = it.toString())
            }
        }

        if (!match.exact) {
            Spacer(modifier = Modifier.height(8.dp))
            BasicText(
                text = stringResource(
                    R.string.zoundhub_dialog_fuzzy,
                    formatSimilarity(match.similarity),
                    artist.name
                ),
                style = typography().xxs.secondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = stringResource(R.string.zoundhub_dialog_propagated),
            style = typography().xxs.secondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        BasicText(
            text = stringResource(R.string.zoundhub_dialog_source),
            style = typography().xxs.secondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = { openZoundHubPage(context, artist.slug) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            BasicText(
                text = stringResource(R.string.zoundhub_open_page),
                style = typography().xs.semiBold.copy(color = colorPalette().accent)
            )
        }
    }
}

/** Apre la pagina pubblica ZoundHub dell'artista (browser esterno). */
fun openZoundHubPage(context: android.content.Context, slug: String) {
    try {
        val url = ZoundHubService.artistPageUrl(slug)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // nessun browser disponibile: silenzioso, non e' un'azione critica
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filtro "Nascondi contenuti AI" (impostazione isHiddenZoundHubAi).
//
// ARCHITETTURA REATTIVA v1.3 (fix freeze):
//  1. La DECISIONE "questo testo autori e' AI?" passa dalla cache processuale
//     ZoundHubCatalogStore.isAiHidden(): condivisa da TUTTE le schermate, il
//     costo fuzzy di un nome e' pagato UNA volta per processo (prima ogni
//     schermata si ricreava la cache in composizione e ripagava i miss).
//  2. Il filtraggio delle liste NON gira MAI sul main thread:
//     rememberZoundHubAiFiltered / filterHiddenZoundHubAi restituiscono subito
//     la lista sorgente (la UI compare istantaneamente) e calcolano la lista
//     filtrata su Dispatchers.Default via produceState; il risultato sostituisce
//     la lista appena pronto. Il predicato e' thread-safe (cache concorrente).
//  3. Attivo SOLO quando: badge abilitato + filtro abilitato + catalogo pronto
//     (finche' il catalogo carica in background le liste restano complete,
//     niente falsi vuoti al lancio dell'app). Al refresh del catalogo il
//     predicato cambia istanza e le liste si ri-filtrano da sole.
// ─────────────────────────────────────────────────────────────────────────────

/** Predicato neutro: identita' stabile cosi' le liste non cambiano quando il filtro e' spento. */
private val aiNeverHidden: (String?) -> Boolean = { false }

/** true se il filtro "nascondi contenuti AI" e' effettivamente operativo. */
@Composable
fun rememberZoundHubAiFilterActive(): Boolean {
    val appSettings = LocalAppSettingsManager.current
        .activeSettings
        .collectAsStateWithLifecycle()
        .value

    if (!appSettings.isEnabledZoundHub || !appSettings.isHiddenZoundHubAi) return false

    val state = ZoundHubClient.store.state.collectAsState().value
    return state.status == ZoundHubCatalogState.Status.Ready
}

/**
 * Predicato "questo testo autori e' AI?" attivo solo col filtro operativo.
 *
 * Delega alla cache processuale dello store (ZoundHubCatalogStore.isAiHidden):
 * nessuna cache locale per-composizione, quindi navigare tra schermate NON
 * ripaga piu' i miss fuzzy gia' pagati. L'istanza del predicato e' chiavata
 * all'identita' del catalogo: refresh -> nuova istanza -> produceState e
 * LaunchedEffect chiavati sul predicato si ri-lanciano e le liste si
 * ri-filtrano subito col catalogo aggiornato.
 */
@Composable
fun rememberZoundHubAiHiddenPredicate(): (String?) -> Boolean {
    if (!rememberZoundHubAiFilterActive()) return aiNeverHidden

    val store = ZoundHubClient.store
    val state by store.state.collectAsState()
    return remember(state.catalog) {
        { text: String? -> store.isAiHidden(text) }
    }
}

/**
 * FILTRO REATTIVO "nascondi contenuti AI": NON blocca mai la composizione.
 *
 * Restituisce SUBITO la lista sorgente (la UI esce istantaneamente, come
 * quando il filtro e' spento), poi calcola la lista filtrata su
 * Dispatchers.Default e la pubblica appena pronta. Con la cache processuale
 * calda il passaggio e' di pochi ms; al primo giro di una libreria molto
 * grande la lista appare completa e gli item AI scompaiono a filtraggio
 * concluso (come la home resta completa finche' il catalogo carica).
 *
 * Ri-calcola automaticamente quando cambia la lista sorgente, il predicato
 * (impostazione o refresh catalogo) o lo stato del filtro.
 *
 * Uso tipico nelle schermate:
 *   val visible = rememberZoundHubAiFiltered(songs) { it.artistsText }
 */
@Composable
fun <T> rememberZoundHubAiFiltered(
    source: List<T>,
    artistNames: (T) -> String?,
): List<T> {
    val predicate = rememberZoundHubAiHiddenPredicate()
    if (predicate === aiNeverHidden) return source

    // toList(): lettura osservata -> se la sorgente e' una SnapshotStateList
    // che muta in place, ogni modifica produce un nuovo snapshot e ri-lancia
    // il producer (le emissioni immutabili dei Flow passano intatte).
    val snapshot = source.toList()
    return produceState(initialValue = snapshot, snapshot, predicate) {
        withContext(Dispatchers.Default) {
            value = snapshot.filter { !predicate(artistNames(it)) }
        }
    }.value
}

/**
 * Rimuove dalla lista gli item il cui testo autori matcha il catalogo AI.
 * Versione reattiva e NON-BLOCCANTE (Dispatchers.Default): vedi
 * [rememberZoundHubAiFiltered].
 * Uso tipico nelle schermate:
 *   val visible = songs.filterHiddenZoundHubAi { it.artistsText }
 */
@Composable
fun <T> List<T>.filterHiddenZoundHubAi(artistNames: (T) -> String?): List<T> =
    rememberZoundHubAiFiltered(this, artistNames)

/**
 * Testo autori di un item Environment (nome artista per ArtistItem, join degli
 * autori per brani/album/video): usato dal filtro "nascondi contenuti AI" nelle
 * liste home/ricerca. null = nessun autore valutabile (l'item non viene filtrato).
 */
fun Environment.Item.zoundHubArtistNames(): String? = when (this) {
    is Environment.SongItem -> authors?.joinToString(", ") { it.name ?: "" }
    is Environment.AlbumItem -> authors?.joinToString(", ") { it.name ?: "" }
    is Environment.VideoItem -> authors?.joinToString(", ") { it.name ?: "" }
    is Environment.ArtistItem -> info?.name
    else -> null
}

/**
 * Testo autori di una Song (nome artista per ArtistItem, join degli
 * autori per brani/album/video): usato dal filtro "nascondi contenuti AI" nelle
 * liste home/ricerca. null = nessun autore valutabile (l'item non viene filtrato).
 */
fun Song.zoundHubArtistNames(): String? = this.artistsText
fun Album.zoundHubArtistNames(): String? = this.authorsText


/**
 * Sezione completa per la pagina artista (ArtistOverview, area "read more"):
 * titolo + righe informative + link alla pagina ZoundHub.
 */
@Composable
fun ZoundHubArtistSection(match: ZoundHubMatch, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val artist = match.artist

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        BasicText(
            text = stringResource(R.string.zoundhub_section_title),
            style = typography().xs.semiBold.copy(color = colorPalette().text)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AiBadge(match = match, onClick = {})

            artist.rating?.let {
                BasicText(
                    text = "\u2605 $it / 5",
                    style = typography().xxs.semiBold.copy(color = Color(0xFFFFC107)),
                    maxLines = 1
                )
            }

            artist.shScore?.let {
                BasicText(
                    text = "SH %.0f".format(it),
                    style = typography().xxs.semiBold.copy(color = colorPalette().text),
                    maxLines = 1
                )
            }

            when (artist.deezer) {
                true -> BasicText(
                    text = "Deezer \u2713",
                    style = typography().xxs.semiBold.copy(color = colorPalette().accent),
                    maxLines = 1
                )
                false -> BasicText(
                    text = "Deezer \u2715",
                    style = typography().xxs.semiBold.copy(color = colorPalette().text),
                    maxLines = 1
                )
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        BasicText(
            text = stringResource(R.string.zoundhub_dialog_propagated),
            style = typography().xxs.secondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        BasicText(
            text = "zoundhub.com/artist/${artist.slug}",
            style = typography().xxs.semiBold.copy(color = colorPalette().accent),
            modifier = Modifier.clickable { openZoundHubPage(context, artist.slug) }
        )
    }
}
