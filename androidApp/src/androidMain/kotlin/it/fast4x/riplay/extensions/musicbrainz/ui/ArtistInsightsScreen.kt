package it.fast4x.riplay.extensions.musicbrainz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.fast4x.riplay.LocalArtistInsights
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.RelatedArtist
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.RelatedItemsService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui.NatureBadgeLarge
import it.fast4x.riplay.extensions.musicbrainz.models.ArtistStats
import it.fast4x.riplay.extensions.musicbrainz.ui.components.InfoCard
import it.fast4x.riplay.extensions.musicbrainz.ui.components.RatingBar
import it.fast4x.riplay.extensions.musicbrainz.ui.components.TagRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.AlbumRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.ExternalLinkRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.RelatedArtistRow
import it.fast4x.riplay.extensions.musicbrainz.ui.row.SongRow
import it.fast4x.riplay.ui.components.themed.KeywordChips
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.toFlagEmoji
import it.fast4x.riplay.utils.typography

@Composable
fun ArtistInsightsScreen(
    artistId: String,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = LocalArtistInsights.current
    val state by viewModel.state.collectAsState()

    val relatedItemsService = remember { RelatedItemsService() }
    var relatedArtists  by remember(artistId) { mutableStateOf<List<RelatedArtist>>(emptyList())}

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)

        relatedArtists = relatedItemsService.getRelatedArtists(artistId)
    }
/*
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.artist?.name ?: "Artista") },
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
            val artist = state.artist ?: return
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(colorPalette().background0),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // === HEADER ===
                item { ArtistHeader(artist) }

                if (relatedArtists.isNotEmpty()) {
                    item {
                        InfoCard(title = "Artisti simili", icon = Icons.Default.CompareArrows) {
                            relatedArtists.forEach { related ->
                                RelatedArtistRow(
                                    artist = related.artist,
                                    relationType = related.reason,
                                    onClick = {
                                        onArtistClick(related.artist.id)
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }

                // === GENERI E TAG ===
                val keywords = artist.keywords
                if (keywords.isNotEmpty()) {
                    item {
                        InfoCard(title = "Tag & Generi", icon = Icons.Default.Info) {
                            //TagRow(tags = keywords)
                            KeywordChips(keywords)
                        }
                    }
                }

                // === POPOLARITÀ ===
                if (artist.rating != null) {
                    item {
                        InfoCard(title = "Popolarità", icon = Icons.Default.Star) {
                            RatingBar(rating = artist.rating, votes = artist.ratingVotes)
                        }
                    }
                }

                // === STATS ASCOLTO ===
                state.stats?.let { stats ->
                    item {
                        InfoCard(title = "I tuoi ascolti", icon = Icons.Default.Equalizer) {
                            ArtistStatsRow(stats)
                        }
                    }
                }

                // === BIOGRAFIA ===
                artist.wikipediaBio?.let { bio ->
                    if (bio.isNotBlank()) {
                        item {
                            InfoCard(title = "Biografia", icon = Icons.Default.Article) {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // === DISCOGRAFIA ===
                if (state.albums.isNotEmpty()) {
                    item {
                        InfoCard(title = "Discografia", icon = Icons.Default.Album) {
                            state.albums.forEach { album ->
                                AlbumRow(album = album, onClick = { onAlbumClick(album.id) })
                            }
                        }
                    }
                }

                // === TOP TRACKS ===
                if (state.topTracks.isNotEmpty()) {
                    item {
                        InfoCard(title = "Brani più ascoltati", icon = Icons.Default.MusicNote) {
                            state.topTracks.forEachIndexed { index, song ->
                                SongRow(song = song, position = index + 1)
                            }
                        }
                    }
                }

                // === RELAZIONI MB ===
                if (state.relations.isNotEmpty()) {
                    item {
                        InfoCard(title = "Membri & Collaborazioni", icon = Icons.Default.Group) {
                            state.relations.forEach { rel ->
                                RelatedArtistRow(
                                    artist = rel.artist,
                                    relationType = rel.relationType,
                                    onClick = {
                                        onArtistClick(rel.artist.id)
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }

                // === LINK ESTERNI ===
                if (state.externalLinks.isNotEmpty() || artist.wikipediaUrl != null) {
                    item {
                        InfoCard(title = "Collegamenti", icon = Icons.Default.Link) {
                            artist.wikipediaUrl?.let {
                                ExternalLinkRow("Wikipedia", it, Icons.Default.Public)
                            }
                            artist.mbId?.let {
                                ExternalLinkRow(
                                    "MusicBrainz",
                                    "https://musicbrainz.org/artist/$it",
                                    Icons.Default.Public
                                )
                            }
                            state.externalLinks.forEach { link ->
                                ExternalLinkRow(link.platform, link.url, Icons.Default.Public)
                            }
                        }
                    }
                }
            }
        }
    //}
}

@Composable
private fun ArtistHeader(artist: Artist) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = artist.name ?: "Unknown",
            style = typography().s.semiBold,
        )

        Spacer(Modifier.height(8.dp))
        NatureBadgeLarge(nature = artist.nature)

        Spacer(Modifier.height(8.dp))

        // Info row: origine, periodo
        val infoText = buildList {
            artist.countryCode?.let {
                add("$it ${it.toFlagEmoji()}")
            }
            artist.beginYear?.let { year ->
                add("$year")
                /*
                val endYear = artist.let { /* todo implementare endyear */ }
                if (endYear != null) {
                    add("$year-$endYear")
                } else {
                    add("$year-presente")
                }

                 */
            }
            artist.artistType?.let { add(it) }
        }.joinToString(" • ")

        if (infoText.isNotBlank()) {
            Text(
                text = infoText,
                style = typography().s,
                color = colorPalette().text
            )
        }
    }
}

@Composable
private fun ArtistStatsRow(stats: ArtistStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Ascolti", value = stats.playCount.toString())
        StatItem(label = "Album", value = stats.distinctAlbumsCount.toString())
        StatItem(label = "Like", value = stats.likedSongsCount.toString())
        StatItem(
            label = "Tempo totale",
            value = formatPlayTime(stats.totalPlayTimeMs)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = typography().xs.semiBold,
        )
        Text(
            text = label,
            style = typography().xs,
            color = colorPalette().text
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