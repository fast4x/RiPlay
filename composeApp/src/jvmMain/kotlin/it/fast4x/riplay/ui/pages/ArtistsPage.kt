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
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.items.ArtistItem
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer

/**
 * STEP 3b: pagina ARTISTS della navigazione desktop in stile Android
 * (equivalente desktop di HomeArtists/elenco artisti del db).
 * Gli artisti arrivano dal database (Database.artists ordinati per nome);
 * il click apre la schermata artista (ArtistScreen con browseId).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsPage(
    onArtistClick: (Artist) -> Unit
) {
    val artists = remember { SnapshotStateList<Artist>() }

    LaunchedEffect(Unit) {
        Database.artists(ArtistSortBy.Name, SortOrder.Ascending).collect {
            artists.clear()
            artists += it
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
                        text = "Artists (${artists.size})",
                        style = TextStyle(
                            fontSize = typography.titleSmall.fontSize,
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }
            itemsIndexed(
                items = artists,
                key = { _, artist -> artist.id }
            ) { index, artist ->
                ArtistItem(
                    thumbnailUrl = artist.thumbnailUrl,
                    name = artist.name,
                    subscribersCount = null,
                    thumbnailSizeDp = 60.dp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onLongClick = {},
                            onClick = {
                                onArtistClick(artist)
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
