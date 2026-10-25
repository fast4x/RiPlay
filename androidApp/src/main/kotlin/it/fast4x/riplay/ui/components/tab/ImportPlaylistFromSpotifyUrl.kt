package it.fast4x.riplay.ui.components.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.ProgressIndicator
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.SpotifyImportProgress
import it.fast4x.riplay.utils.SpotifyImportResult
import it.fast4x.riplay.utils.SpotifyPlaylistImporter
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import it.fast4x.spotifymeta.SpotifyPlaylist
import kotlinx.coroutines.launch

private enum class SpotifyImportPhase { INPUT, IMPORTING, DONE }

/**
 * Importa una playlist di Spotify a partire dal suo url.
 *
 * L'utente incolla il link, la playlist viene letta da Spotify e per ogni brano viene
 * cercato il corrispondente su YouTube Music; i metadati dei brani non risolti restano utili ai consigli.
 */
@Composable
fun ImportPlaylistFromSpotifyUrlDialog(
    onDismiss: () -> Unit
) {
    var phase by remember { mutableStateOf(SpotifyImportPhase.INPUT) }
    var url by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(SpotifyImportProgress(0, 0)) }
    var result by remember { mutableStateOf<SpotifyImportResult?>(null) }

    val coroutineScope = rememberCoroutineScope()

    when (phase) {
        SpotifyImportPhase.INPUT -> InputTextDialog(
            onDismiss = { if (phase == SpotifyImportPhase.INPUT) onDismiss() },
            title = stringResource(R.string.import_playlist_spotify_url),
            value = url,
            placeholder = stringResource(R.string.spotify_playlist_url_placeholder),
            setValue = { newUrl ->
                if (SpotifyPlaylist.parseId(newUrl) == null) {
                    SmartMessage(
                        appContext().resources.getString(R.string.spotify_playlist_url_invalid),
                        context = appContext()
                    )
                } else {
                    url = newUrl
                    phase = SpotifyImportPhase.IMPORTING
                    coroutineScope.launch {
                        val importResult = SpotifyPlaylistImporter.import(newUrl) { current ->
                            progress = current
                        }
                        result = importResult
                        phase = SpotifyImportPhase.DONE
                    }
                }
            }
        )

        SpotifyImportPhase.IMPORTING -> SpotifyImportProgressDialog(progress)

        SpotifyImportPhase.DONE -> SpotifyImportResultDialog(
            result = result,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SpotifyImportProgressDialog(progress: SpotifyImportProgress) {
    Dialog(onDismissRequest = { }) {
        Surface(
            modifier = Modifier.widthIn(min = 280.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            tonalElevation = 6.dp,
            color = colorPalette().background1
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.importing_spotify_playlist),
                    style = typography().s.semiBold,
                    color = colorPalette().text
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = if (progress.total > 0)
                        progress.current.toFloat() / progress.total.toFloat()
                    else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (progress.total > 0)
                        stringResource(
                            R.string.importing_spotify_playlist_progress,
                            progress.current,
                            progress.total
                        )
                    else stringResource(R.string.importing_spotify_playlist_waiting),
                    style = typography().xs,
                    color = colorPalette().textSecondary
                )

                progress.trackName?.let { trackName ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trackName,
                        style = typography().xs,
                        color = colorPalette().textDisabled,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyImportResultDialog(
    result: SpotifyImportResult?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 280.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            tonalElevation = 6.dp,
            color = colorPalette().background1
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                val error = result?.error
                Text(
                    text = if (error != null)
                        stringResource(R.string.importing_spotify_playlist_failed)
                    else stringResource(R.string.importing_spotify_playlist_done),
                    style = typography().s.semiBold,
                    color = colorPalette().text
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (error != null) {
                    Text(
                        text = error,
                        style = typography().xs,
                        color = colorPalette().red
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.spotify_import_recommendations_summary,
                            result?.imported ?: 0,
                            result?.pending ?: 0
                        ),
                        style = typography().xs,
                        color = colorPalette().textSecondary,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.spotify_import_recommendations_playable_skips,
                            result?.skipped ?: 0,
                            result?.total ?: 0
                        ),
                        style = typography().xs,
                        color = colorPalette().textSecondary
                    )

                    result?.playlistName?.let { playlistName ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = playlistName,
                            style = typography().xs,
                            color = colorPalette().text
                        )
                    }

                    if (result?.partial == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.importing_spotify_playlist_partial),
                            style = typography().xs,
                            color = colorPalette().textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close), color = colorPalette().text)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
