package it.fast4x.riplay.items

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import it.fast4x.riplay.extensions.zoundhub.DesktopZoundHub
import it.fast4x.riplay.i18n.ds
import it.fast4x.environment.Environment
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.zoundhub.ZoundHubCatalogState
import it.fast4x.zoundhub.ZoundHubMatch
import it.fast4x.zoundhub.ZoundHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Badge AI (ZoundHub) per la UI DESKTOP: badge nelle liste (Items.kt),
 * dettagli al click (Dialog) e helper di lookup.
 *
 * SEMANTICA (identica alla versione Android): ZoundHub segnala a livello
 * di ARTISTA; il badge su album e brani e' una PROPAGAZIONE del segnale
 * dell'artista, dichiarata nel dialog (ds("zoundhub.dialog.propagated")).
 */

/** Colore condiviso del segnale AI nella UI desktop. */
private val AiBadgeColor = Color(0xFFE91E63)

/** Formatta una similarita' (0..1) come percentuale intera. */
private fun formatSimilarity(similarity: Double): String =
    "${(similarity * 100).toInt()}%"

/** Formatta un epoch millis come data locale breve. */
fun formatZoundHubDateDesktop(epochMillis: Long?): String =
    epochMillis?.let {
        SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Locale.getDefault()).format(Date(it))
    } ?: ""

/** Apre la pagina ZoundHub dell'artista nel browser di sistema. */
private fun openZoundHubPageDesktop(slug: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(ZoundHubService.artistPageUrl(slug)))
        }
    } catch (t: Throwable) {
        // nessun browser: silenzioso, non e' un'azione critica
    }
}

/**
 * Helper composable: restituisce il match per il testo artista corrente
 * (null se toggle OFF / catalogo non pronto / nessun match).
 */
@Composable
fun rememberZoundHubMatchDesktop(artistText: String?): ZoundHubMatch? {
    if (!DesktopZoundHub.enabled) return null

    val store = DesktopZoundHub.store
    val state by store.state.collectAsState()

    if (state.status != ZoundHubCatalogState.Status.Ready) return null

    return remember(artistText, state.catalog) { store.lookup(artistText) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filtro "Nascondi contenuti AI" (desktop, specchio di AiBadge.kt Android).
//
// ARCHITETTURA REATTIVA v1.3 (fix freeze):
//  1. DECISIONE via cache processuale dello store (isAiHidden): condivisa tra
//     tutte le pagine, il costo fuzzy di un nome e' pagato UNA volta per
//     processo (prima ogni pagina/composizione ripagava i miss fuzzy).
//  2. Il filtraggio NON gira mai sull'EDT: filterHiddenZoundHubAiDesktop e
//     rememberZoundHubAiFilteredDesktop restituiscono subito la lista sorgente
//     (UI istantanea) e calcolano la lista filtrata su Dispatchers.Default via
//     produceState; il risultato sostituisce la lista appena pronto.
//  3. Attivo SOLO con: toggle abilitato + filtro abilitato + catalogo pronto.
//     Refresh catalogo -> nuova istanza del predicato -> le liste si ri-filtrano.
// ─────────────────────────────────────────────────────────────────────────────

/** Predicato neutro: identita' stabile quando il filtro e' spento. */
private val aiNeverHiddenDesktop: (String?) -> Boolean = { false }

/** true se il filtro "nascondi contenuti AI" e' effettivamente operativo. */
@Composable
fun rememberZoundHubAiFilterActiveDesktop(): Boolean {
    if (!DesktopZoundHub.enabled || !DesktopZoundHub.hidden) return false
    val state = DesktopZoundHub.store.state.collectAsState().value
    return state.status == ZoundHubCatalogState.Status.Ready
}

/**
 * Predicato "questo testo autori e' AI?" attivo solo col filtro operativo.
 * Delega alla cache processuale dello store (isAiHidden): niente cache locale
 * per-composizione. Chiavato all'identita' del catalogo: refresh -> nuova
 * istanza -> le liste si ri-filtrano subito col catalogo aggiornato.
 */
@Composable
fun rememberZoundHubAiHiddenPredicateDesktop(): (String?) -> Boolean {
    if (!rememberZoundHubAiFilterActiveDesktop()) return aiNeverHiddenDesktop

    val store = DesktopZoundHub.store
    val state by store.state.collectAsState()
    return remember(state.catalog) {
        { text: String? -> store.isAiHidden(text) }
    }
}

/**
 * FILTRO REATTIVO "nascondi contenuti AI" (desktop): NON blocca mai l'EDT.
 * Restituisce SUBITO la lista sorgente, poi calcola la lista filtrata su
 * Dispatchers.Default e la pubblica appena pronta. Ri-calcola automaticamente
 * quando cambia la lista sorgente o il predicato (impostazione/refresh).
 */
@Composable
fun <T> rememberZoundHubAiFilteredDesktop(
    source: List<T>,
    artistNames: (T) -> String?,
): List<T> {
    val predicate = rememberZoundHubAiHiddenPredicateDesktop()
    if (predicate === aiNeverHiddenDesktop) return source

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
 * Versione reattiva e NON-BLOCCANTE (Dispatchers.Default): la pagina esce
 * subito con la lista completa e gli item AI scompaiono a filtraggio concluso.
 * Da chiamare in scope composable PRIMA dei blocchi Lazy (items() non e'
 * composable).
 */
@Composable
fun <T> List<T>.filterHiddenZoundHubAiDesktop(artistNames: (T) -> String?): List<T> =
    rememberZoundHubAiFilteredDesktop(this, artistNames)

/**
 * Testo autori di un item Environment per il filtro AI desktop
 * (nome artista per ArtistItem, join degli autori per brani/album/video).
 */
fun Environment.Item.zoundHubArtistNamesDesktop(): String? = when (this) {
    is Environment.SongItem -> authors?.joinToString(", ") { it.name ?: "" }
    is Environment.AlbumItem -> authors?.joinToString(", ") { it.name ?: "" }
    is Environment.VideoItem -> authors?.joinToString(", ") { it.name ?: "" }
    is Environment.ArtistItem -> info?.name
    else -> null
}

/**
 * Badge AI autonomo per le liste desktop: calcola il match e gestisce il
 * dialog dettagliato. Inserimento in una Row esistente:
 *   Row { AiArtistBadgeDesktop(authors); BasicText(...) }
 */
@Composable
fun AiArtistBadgeDesktop(
    artistText: String?,
    modifier: Modifier = Modifier,
) {
    val match = rememberZoundHubMatchDesktop(artistText) ?: return

    var showDialog by remember { mutableStateOf(false) }

    AiBadgeDesktop(
        match = match,
        onClick = { showDialog = true },
        modifier = modifier
    )

    if (showDialog) {
        AiInfoDialogDesktop(
            match = match,
            onDismiss = { showDialog = false }
        )
    }
}

/** Badge visuale; "AI?" se il match e' fuzzy (incerto). */
@Composable
fun AiBadgeDesktop(
    match: ZoundHubMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uncertain = !match.exact

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = AiBadgeColor.copy(alpha = 0.22f)
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
                text = if (uncertain) ds("zoundhub.badge.ai.uncertain") else ds("zoundhub.badge.ai"),
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

/** Riga chiave/valore del dialog info desktop. */
@Composable
private fun AiInfoRowDesktop(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicText(
            text = value,
            style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Dialog dettagliato desktop (rating, SubmitHub, Deezer, link zoundhub). */
@Composable
fun AiInfoDialogDesktop(
    match: ZoundHubMatch,
    onDismiss: () -> Unit,
) {
    val artist = match.artist

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xFF17171E), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .width(430.dp)
        ) {
            BasicText(
                text = ds("zoundhub.dialog.title"),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            BasicText(
                text = "\uD83E\uDD16 ${artist.name}",
                style = TextStyle(
                    color = AiBadgeColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AiInfoRowDesktop(
                    label = ds("zoundhub.dialog.rating"),
                    value = artist.rating?.let { "$it / 5" } ?: "-"
                )
                AiInfoRowDesktop(
                    label = ds("zoundhub.dialog.shscore"),
                    value = artist.shScore?.let { "%.1f".format(it) } ?: "-"
                )
                AiInfoRowDesktop(
                    label = ds("zoundhub.dialog.deezer"),
                    value = when (artist.deezer) {
                        true -> "\u2713"
                        false -> "\u2715"
                        else -> "-"
                    }
                )
                AiInfoRowDesktop(
                    label = ds("zoundhub.dialog.releases"),
                    value = artist.releases?.toString() ?: "-"
                )
                artist.debut?.takeIf { it.isNotBlank() }?.let {
                    AiInfoRowDesktop(label = ds("zoundhub.dialog.debut"), value = it)
                }
                artist.followers?.let {
                    AiInfoRowDesktop(label = ds("zoundhub.dialog.followers"), value = it.toString())
                }
                artist.listeners?.let {
                    AiInfoRowDesktop(label = ds("zoundhub.dialog.listeners"), value = it.toString())
                }
            }

            if (!match.exact) {
                Spacer(Modifier.height(10.dp))
                BasicText(
                    text = ds(
                        "zoundhub.dialog.fuzzy",
                        "pct" to formatSimilarity(match.similarity),
                        "name" to artist.name
                    ),
                    style = TextStyle(
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            BasicText(
                text = ds("zoundhub.dialog.propagated"),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            BasicText(
                text = ds("zoundhub.dialog.source"),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )

            Spacer(Modifier.height(14.dp))

            BasicText(
                text = ds("zoundhub.open.page"),
                style = TextStyle(
                    color = RiPlayAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { openZoundHubPageDesktop(artist.slug) }
                    .padding(6.dp)
            )
        }
    }
}
