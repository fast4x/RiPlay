package it.fast4x.riplay.extensions.musicbrainz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.fast4x.riplay.LocalAlbumInsights
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui.NatureBadgeLarge
import it.fast4x.riplay.extensions.musicbrainz.models.AlbumStats
import it.fast4x.riplay.extensions.musicbrainz.ui.components.InfoCard
import it.fast4x.riplay.extensions.musicbrainz.ui.components.RatingBar
import it.fast4x.riplay.extensions.musicbrainz.ui.components.TagRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.AlbumRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.ExternalLinkRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.SongRow

@Composable
fun AlbumInsightsScreen(
    albumId: String,
    onSongClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = LocalAlbumInsights.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    /*
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.album?.title ?: "Album") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        */
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val album = state.album ?: return
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // === HEADER ===
                item { AlbumHeader(album = album, artist = state.artist) }

                // === GENERI E TAG ===
                val keywords = album.genres.orEmpty() + album.tags.orEmpty()
                if (keywords.isNotEmpty()) {
                    item {
                        InfoCard(title = "Tag & Generi", icon = Icons.Default.Label) {
                            TagRow(tags = keywords.distinct())
                        }
                    }
                }

                // === POPOLARITÀ ===
                if (album.rating != null) {
                    item {
                        InfoCard(title = "Popolarità", icon = Icons.Default.Star) {
                            RatingBar(rating = album.rating, votes = album.ratingVotes)
                        }
                    }
                }

                // === STATS ASCOLTO ===
                state.stats?.let { stats ->
                    if (stats.tracksCount > 0) {
                        item {
                            InfoCard(title = "I tuoi ascolti", icon = Icons.Default.Equalizer) {
                                AlbumStatsRow(stats)
                            }
                        }
                    }
                }

                // === RECENSIONE / WIKIPEDIA ===
                album.wikipediaInfo?.let { info ->
                    if (info.isNotBlank()) {
                        item {
                            InfoCard(title = "Recensione", icon = Icons.Default.Article) {
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // === TRACKLIST ===
                if (state.tracks.isNotEmpty()) {
                    item {
                        InfoCard(title = "Tracklist (${state.tracks.size})", icon = Icons.Default.MusicNote) {
                            state.tracks.forEachIndexed { index, song ->
                                SongRow(
                                    song = song,
                                    position = index + 1,
                                    onClick = { onSongClick(song.id) }
                                )
                            }
                        }
                    }
                }

                // === ALTRI ALBUM DELL'ARTISTA ===
                if (state.otherAlbums.isNotEmpty()) {
                    item {
                        InfoCard(title = "Altri album", icon = Icons.Default.Album) {
                            state.otherAlbums.forEach { otherAlbum ->
                                AlbumRow(
                                    album = otherAlbum,
                                    onClick = { onAlbumClick(otherAlbum.id) }
                                )
                            }
                        }
                    }
                }

                // === COLLEGAMENTI ===
                if (state.externalLinks.isNotEmpty() ||
                    album.wikipediaUrl != null ||
                    album.mbId != null) {
                    item {
                        InfoCard(title = "Collegamenti", icon = Icons.Default.Link) {
                            album.wikipediaUrl?.let {
                                ExternalLinkRow("Wikipedia", it, Icons.Default.Public)
                            }
                            album.mbId?.let {
                                ExternalLinkRow(
                                    "MusicBrainz",
                                    "https://musicbrainz.org/release-group/$it",
                                    Icons.Default.Public
                                )
                            }
                            state.externalLinks.forEach { link ->
                                ExternalLinkRow(link.platform, link.url, Icons.Default.Public)
                            }
                        }
                    }
                }

                // Spazio finale per non coprire col bottom bar
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    //}
}

@Composable
private fun AlbumHeader(
    album: Album,
    artist: Artist?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover album (quadrata, non cerchio)
        AsyncImage(
            model = album.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = album.title ?: "Unknown Album",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        // Artista (clickable se presente)
        Text(
            text = artist?.name ?: album.authorsText ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))
        NatureBadgeLarge(nature = album.nature)

        Spacer(Modifier.height(8.dp))

        // Info row: anno, tipo
        val infoText = buildList {
            album.originalYear?.let { add(it.toString()) }
            album.albumType?.let { add(it) }
        }.joinToString(" • ")

        if (infoText.isNotBlank()) {
            Text(
                text = infoText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumStatsRow(stats: AlbumStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Brani", value = stats.tracksCount.toString())
        StatItem(label = "Ascolti", value = stats.playCount.toString())
        StatItem(label = "Like", value = stats.likedSongsCount.toString())
        StatItem(
            label = "Tempo tot.",
            value = formatPlayTime(stats.totalPlayTimeMs)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatPlayTime(ms: Long): String {
    val hours = ms / 3_600_000
    return when {
        hours < 1 -> "${ms / 60_000}min"
        hours < 24 -> "${hours}h"
        else -> "${hours / 24}g"
    }
}