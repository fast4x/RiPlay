package it.fast4x.riplay.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.items.PlaylistItem
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer
import it.fast4x.riplay.utils.LoadImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.library

/**
 * STEP 3b: pagina PLAYLISTS della navigazione desktop in stile Android
 * (equivalente desktop di HomePlaylists/elenco playlist del db).
 * Le playlist arrivano dal database (Database.playlistPreviews ordinate
 * per nome); la thumbnail e' la prima immagine salvata per la playlist
 * (Database.playlistThumbnailUrls, come fa Android) con fallback icona
 * libreria. Il click passa la PlaylistPreview all'app, che apre la
 * schermata YouTube (se c'e' un browseId) oppure i brani della playlist
 * locale.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsPage(
    onPlaylistClick: (PlaylistPreview) -> Unit
) {
    val playlists = remember { SnapshotStateList<PlaylistPreview>() }

    LaunchedEffect(Unit) {
        Database.playlistPreviews(PlaylistSortBy.Name, SortOrder.Ascending).collect {
            playlists.clear()
            playlists += it
        }
    }

    val lazyListState = rememberLazyListState()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Playlists (${playlists.size})",
                        style = TextStyle(
                            fontSize = typography.titleSmall.fontSize,
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }
            itemsIndexed(
                items = playlists,
                key = { _, preview -> preview.playlist.id }
            ) { index, preview ->
                PlaylistItem(
                    thumbnailContent = {
                        PlaylistThumbnail(playlistId = preview.playlist.id)
                    },
                    songCount = preview.songCount,
                    name = preview.playlist.name,
                    channelName = null,
                    thumbnailSizeDp = 60.dp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onLongClick = {},
                            onClick = {
                                onPlaylistClick(preview)
                            }
                        )
                )
            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}

/**
 * Thumbnail di una playlist dal db (prima immagine salvata), con
 * fallback icona libreria quando la playlist non ha immagini.
 */
@Composable
private fun PlaylistThumbnail(playlistId: Long) {
    val thumbnailUrl by produceState<String?>(initialValue = null, key1 = playlistId) {
        value = withContext(Dispatchers.IO) {
            Database.playlistThumbnailUrls(playlistId).first().firstOrNull()
        }
    }

    if (thumbnailUrl != null) {
        LoadImage(thumbnailUrl!!)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.library),
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            )
        }
    }
}
