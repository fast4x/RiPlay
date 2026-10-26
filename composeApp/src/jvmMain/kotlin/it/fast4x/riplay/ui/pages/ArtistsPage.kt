package it.fast4x.riplay.ui.pages

import it.fast4x.riplay.i18n.ds

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.items.ArtistItem
import it.fast4x.riplay.ui.components.RiPlayAccent
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer

/**
 * STEP 3b: pagina ARTISTS della navigazione desktop in stile Android
 * (equivalente desktop di HomeArtists/elenco artisti del db).
 * Gli artisti arrivano dal database (Database.artists ordinati per nome);
 * il click apre la schermata artista (ArtistScreen con browseId).
 *
 * STEP 3i - ARTISTI SEGUITI: l'artista visitato viene salvato nel db
 * (ArtistScreen) e con il bookmark (stesso meccanismo degli album) diventa
 * PREFERITO. Qui:
 *  - filtro ds("page.followed") in alto: mostra SOLO gli artisti preferiti;
 *  - gli artisti preferiti hanno la stella accanto al nome.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsPage(
    onArtistClick: (Artist) -> Unit
) {
    val artists = remember { SnapshotStateList<Artist>() }

    // STEP 3i: filtro attivo = mostra SOLO gli artisti seguiti (preferiti)
    var onlyFollowed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Database.artists(ArtistSortBy.Name, SortOrder.Ascending).collect {
            artists.clear()
            artists += it
        }
    }

    val visible = if (onlyFollowed) artists.filter { it.bookmarkedAt != null } else artists

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
                        text = ds("page.artists", "n" to visible.size),
                        style = TextStyle(
                            fontSize = typography.titleSmall.fontSize,
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    )

                    // STEP 3i: toggle ds("page.followed_only") (stella): filtra l'elenco
                    // sugli artisti preferiti (bookmarkedAt != null)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onlyFollowed = !onlyFollowed }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (onlyFollowed) RiPlayAccent.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.07f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (onlyFollowed) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (onlyFollowed) RiPlayAccent
                                   else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = ds("page.followed"),
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = if (onlyFollowed) RiPlayAccent
                                        else Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
            itemsIndexed(
                items = visible,
                key = { _, artist -> artist.id }
            ) { index, artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArtistItem(
                        thumbnailUrl = artist.thumbnailUrl,
                        name = artist.name,
                        subscribersCount = null,
                        thumbnailSizeDp = 60.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onLongClick = {},
                                onClick = {
                                    onArtistClick(artist)
                                }
                            )
                    )

                    // STEP 3i: stella per gli artisti SEGUITI (preferiti)
                    if (artist.bookmarkedAt != null) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = ds("page.followed_artist"),
                            tint = RiPlayAccent,
                            modifier = Modifier
                                .padding(end = 14.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}
