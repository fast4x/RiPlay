package it.fast4x.riplay.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer
import it.fast4x.riplay.utils.LoadImage

/**
 * STEP 3b: brani di una playlist LOCALE del db (playlist senza browseId
 * YouTube), aperta dal tab Playlists. Equivale desktop minimale di
 * LocalPlaylistSongs di Android: elenco brani (Database.playlistSongs)
 * con click per la riproduzione.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistSongsPage(
    playlist: Playlist,
    onSongClick: (Song) -> Unit
) {
    val songs = remember { SnapshotStateList<Song>() }

    LaunchedEffect(playlist.id) {
        Database.playlistSongs(playlist.id).collect {
            songs.clear()
            songs += it
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
                        text = "${playlist.name} (${songs.size})",
                        style = TextStyle(
                            fontSize = typography.titleSmall.fontSize,
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    title = song.title,
                    authors = song.artistsText,
                    duration = song.durationText,
                    thumbnailSizeDp = 50.dp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onLongClick = {},
                            onClick = {
                                onSongClick(song)
                            }
                        ),
                    thumbnailContent = {
                        song.thumbnailUrl?.let { LoadImage(it) }
                    },
                    trailingContent = {}
                )
            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}
