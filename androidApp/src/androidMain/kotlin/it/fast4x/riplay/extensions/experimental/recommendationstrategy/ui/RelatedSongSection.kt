package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.RelatedSong
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.RelatedItemsService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.SongEnricherService
import it.fast4x.riplay.extensions.musicbrainz.ui.components.InfoCard
import it.fast4x.riplay.extensions.musicbrainz.ui.row.SongRow

@Composable
fun RelatedSongsSection(
    songId: String,
    relatedItemsService: RelatedItemsService,
    enricher: SongEnricherService
) {
    val enrichmentState by enricher.enrichmentState.collectAsState()
    var relatedSongs = produceState(initialValue = emptyList<RelatedSong>(), songId) {
        value = relatedItemsService.getRelatedSongs(songId)
    }.value

    // Auto-refresh quando l'enrichment completa
    LaunchedEffect(enrichmentState) {
        if (enrichmentState is SongEnricherService.EnrichmentState.Complete &&
            (enrichmentState as SongEnricherService.EnrichmentState.Complete).songId == songId) {
            // Ricarica i related
            relatedSongs = relatedItemsService.getRelatedSongs(songId)
        }
    }

    if (relatedSongs.isNotEmpty()) {
        InfoCard(title = "Brani simili", icon = Icons.Default.CompareArrows) {
            relatedSongs.forEach { related ->
                SongRow(song = related.song, onClick = { /* play */ })
                Text(
                    text = related.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 60.dp, bottom = 4.dp)
                )
            }
        }
    } else if (enrichmentState is SongEnricherService.EnrichmentState.Loading) {
        // Mostra placeholder durante enrich
        InfoCard(title = "Brani simili", icon = Icons.Default.CompareArrows) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Recupero informazioni su MusicBrainz...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    // Se failed o idle senza risultati, la card non va mostrata
}