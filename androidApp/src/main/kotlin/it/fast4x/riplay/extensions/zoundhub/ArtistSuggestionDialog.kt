package it.fast4x.riplay.extensions.zoundhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import it.fast4x.spotifymeta.SpotifyArtistResolver
import it.fast4x.spotifymeta.SpotifyMeta
import kotlinx.coroutines.launch

/**
 * DIALOG "RISOLVI SPOTIFY ARTIST" (Android) — livello 1 + 2 del resolver
 * (vedi SpotifyArtistResolver per la strategia completa).
 *
 * Aperto quando l'artista NON ha gia' uno spotifyId nel db (il resolver
 * MusicBrainz non l'ha trovato):
 *
 *  1. AUTOMATICO: ricerca su Wikidata (P1902). Se trovato: mostra nome
 *     + ID e chiede conferma; alla conferma salva nel db (artist.spotifyId,
 *     persistente: la prossima volta non riparte da zero) e apre il form
 *     GitHub suggestion pre-compilato.
 *
 *  2. MANUALE (fallback, la "ricerca testuale" conforme): apre nel browser
 *     la ricerca Spotify dell'artista; l'utente tocca l'artista giusto,
 *     copia l'URL e lo incolla nel campo. Alla conferma l'ID viene
 *     validato, il nome verificato via SpotifyMeta (pagina artista) e
 *     mostrato nella stessa schermata di conferma del punto 1.
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
fun ZoundHubSpotifyResolverDialog(
    artistName: String,
    onDismiss: () -> Unit,
    /** Chiamato alla conferma finale: (spotifyId, nome su Spotify o null). */
    onResolved: (spotifyId: String, spotifyArtistName: String?) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
            openSpotifySearch(context, artistName)
            ResolverState.Manual(searchOpened = true)
        }
    }

    DefaultDialog(onDismiss = onDismiss) {
        BasicText(
            text = stringResource(R.string.zoundhub_resolver_title),
            style = typography().m.semiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = stringResource(R.string.zoundhub_resolver_artist, artistName),
            style = typography().xs.semiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        when (val s = state) {
            is ResolverState.AutoResolving -> {
                BasicText(
                    text = stringResource(R.string.zoundhub_resolver_auto_searching),
                    style = typography().xs.secondary
                )
            }

            is ResolverState.Confirmed -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    s.thumbnailUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            text = s.spotifyName ?: s.spotifyId,
                            style = typography().m.semiBold.copy(color = colorPalette().accent),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        BasicText(
                            text = s.spotifyId,
                            style = typography().xxs.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                BasicText(
                    text = stringResource(
                        if (s.source == ResolverSource.WIKIDATA) R.string.zoundhub_resolver_found_wikidata
                        else R.string.zoundhub_resolver_found_manual
                    ),
                    style = typography().xxs.secondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.zoundhub_resolver_confirm),
                        onClick = {
                            onResolved(s.spotifyId, s.spotifyName)
                            onDismiss()
                        }
                    )
                }
            }

            is ResolverState.Manual -> {
                BasicText(
                    text = stringResource(R.string.zoundhub_resolver_not_found),
                    style = typography().xs.secondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = pastedUrl,
                    onValueChange = {
                        pastedUrl = it
                        inputError = false
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.zoundhub_resolver_paste_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    isError = inputError,
                    colors = TextFieldDefaults.textFieldColors(
                        placeholderColor = colorPalette().textDisabled,
                        cursorColor = colorPalette().text,
                        textColor = colorPalette().text,
                        backgroundColor = if (!inputError) colorPalette().background1 else colorPalette().red,
                        focusedIndicatorColor = colorPalette().accent,
                        unfocusedIndicatorColor = colorPalette().textDisabled
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (inputError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicText(
                        text = stringResource(R.string.zoundhub_resolver_invalid_url),
                        style = typography().xxs.semiBold.copy(color = colorPalette().red)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                BasicText(
                    text = stringResource(R.string.zoundhub_resolver_search_opened),
                    style = typography().xxs.secondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.zoundhub_resolver_confirm_paste),
                        onClick = {
                            val id = SpotifyArtistResolver.parseArtistId(pastedUrl)
                            if (id == null) {
                                inputError = true
                            } else {
                                coroutineScope.launch {
                                    // verifica il match con la pagina artista
                                    // (stesso meccanismo di SpotifyMeta in produzione)
                                    val meta = runCatching {
                                        SpotifyMeta.fetchAs<it.fast4x.spotifymeta.SpotifyArtistMeta>(
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

/** Apre la ricerca testuale Spotify nel browser (livello 2, conforme). */
fun openSpotifySearch(context: android.content.Context, artistName: String) {
    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(SpotifyArtistResolver.searchUrl(artistName))
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // nessun browser: silenzioso, l'utente puo' cercare da se'
    }
}
