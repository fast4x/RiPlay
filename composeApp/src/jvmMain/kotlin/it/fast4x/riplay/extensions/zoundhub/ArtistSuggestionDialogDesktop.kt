package it.fast4x.riplay.extensions.zoundhub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.spotifymeta.SpotifyArtistMeta
import it.fast4x.spotifymeta.SpotifyArtistResolver
import it.fast4x.spotifymeta.SpotifyMeta
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * DIALOG "RISOLVI SPOTIFY ARTIST" (DESKTOP — specchio di
 * androidApp .../extensions/zoundhub/ArtistSuggestionDialog.kt).
 *
 * Stessa logica a due livelli (vedi SpotifyArtistResolver):
 *  1. AUTOMATICO: Wikidata P1902 -> conferma -> persiste nel db -> apre
 *     il form GitHub suggestion pre-compilato.
 *  2. MANUALE (fallback conforme): apre nel browser la ricerca Spotify
 *     dell'artista, l'utente copia l'URL dell'artista giusto e lo incolla;
 *     l'ID viene validato e il nome verificato via SpotifyMeta.
 */

/**
 * Stati UI del dialog di risoluzione. TOP-LEVEL e privato al file:
 * Kotlin vieta interfacce (ed enum) locali dentro una funzione
 * ("Interface 'State' cannot be local, try to use an anonymous object").
 */
private sealed interface ResolverState {
    data object AutoResolving : ResolverState
    data class Confirmed(
        val spotifyId: String,
        val spotifyName: String?,
        val thumbnailUrl: String?,
        val source: ResolverSource
    ) : ResolverState
    data class Manual(val searchOpened: Boolean) : ResolverState
}

/** Provenienza dello spotifyId confermato nel dialog. */
private enum class ResolverSource { WIKIDATA, MANUAL }

@Composable
fun ZoundHubSpotifyResolverDialogDesktop(
    artistName: String,
    onDismiss: () -> Unit,
    onResolved: (spotifyId: String, spotifyArtistName: String?) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
) {

    var state by remember { mutableStateOf<ResolverState>(ResolverState.AutoResolving) }
    var pastedUrl by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    // LIVELLO 1: Wikidata al primo avvio del dialog
    LaunchedEffect(Unit) {
        val match = SpotifyArtistResolver.resolveViaWikidata(artistName).getOrNull()
        state = if (match != null) {
            ResolverState.Confirmed(match.spotifyId, match.entityLabel, null, ResolverSource.WIKIDATA)
        } else {
            // LIVELLO 2: apre SUBITO la ricerca Spotify nel browser
            openSpotifySearchDesktop(artistName)
            ResolverState.Manual(searchOpened = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xFF17171E), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .width(430.dp)
        ) {
            BasicText(
                text = ds("zoundhub.resolver.title"),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            BasicText(
                text = ds("zoundhub.resolver.artist", "name" to artistName),
                style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            when (val s = state) {
                is ResolverState.AutoResolving -> {
                    BasicText(
                        text = ds("zoundhub.resolver.auto_searching"),
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    )
                }

                is ResolverState.Confirmed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        s.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = s.spotifyName ?: s.spotifyId,
                                style = TextStyle(
                                    color = RiPlayAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            BasicText(
                                text = s.spotifyId,
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    BasicText(
                        text = ds(
                            if (s.source == ResolverSource.WIKIDATA) "zoundhub.resolver.found.wikidata"
                            else "zoundhub.resolver.found.manual"
                        ),
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    DialogActionText(
                        text = ds("zoundhub.resolver.confirm"),
                        onClick = {
                            onResolved(s.spotifyId, s.spotifyName)
                            onDismiss()
                        }
                    )
                }

                is ResolverState.Manual -> {
                    BasicText(
                        text = ds("zoundhub.resolver.not_found"),
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    BasicTextField(
                        value = pastedUrl,
                        onValueChange = {
                            pastedUrl = it
                            inputError = false
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner ->
                            Column(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = if (inputError) 0.12f else 0.06f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .fillMaxWidth()
                            ) {
                                if (pastedUrl.isEmpty())
                                    BasicText(
                                        text = ds("zoundhub.resolver.paste_hint"),
                                        style = TextStyle(
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (inputError) {
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            text = ds("zoundhub.resolver.invalid_url"),
                            style = TextStyle(color = Color(0xFFFFC107), fontSize = 11.sp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    BasicText(
                        text = ds("zoundhub.resolver.search_opened"),
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    DialogActionText(
                        text = ds("zoundhub.resolver.confirm_paste"),
                        onClick = {
                            val id = SpotifyArtistResolver.parseArtistId(pastedUrl)
                            if (id == null) {
                                inputError = true
                            } else {
                                coroutineScope.launch {
                                    val meta = runCatching {
                                        SpotifyMeta.fetchAs<SpotifyArtistMeta>(
                                            "https://open.spotify.com/artist/$id"
                                        )
                                    }.getOrNull()
                                    state = ResolverState.Confirmed(
                                        spotifyId = id,
                                        spotifyName = meta?.name,
                                        thumbnailUrl = meta?.thumbnailUrl,
                                        source = ResolverSource.MANUAL
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Bottone testuale del dialog (stesso stile di "Open ZoundHub page"). */
@Composable
private fun ColumnScope.DialogActionText(text: String, onClick: () -> Unit) {
    BasicText(
        text = text,
        style = TextStyle(
            color = RiPlayAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    )
}

/** Apre la ricerca testuale Spotify nel browser di sistema (livello 2). */
fun openSpotifySearchDesktop(artistName: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(SpotifyArtistResolver.searchUrl(artistName)))
        }
    } catch (t: Throwable) {
        // nessun browser: silenzioso, l'utente puo' cercare da se'
    }
}
