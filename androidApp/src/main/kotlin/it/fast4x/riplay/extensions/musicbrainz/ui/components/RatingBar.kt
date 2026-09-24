package it.fast4x.riplay.extensions.musicbrainz.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RatingBar(
    rating: Float?,
    votes: Int?,
    modifier: Modifier = Modifier
) {
    if (rating == null) {
        Text(
            text = "Nessun rating disponibile",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val normalizedRating = (rating / 5f).coerceIn(0f, 1f)
    val stars = (normalizedRating * 5).roundToInt()

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Stelle piene
            repeat(stars) {
                Text("★", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
            // Stelle vuote
            repeat(5 - stars) {
                Text("☆", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$rating/5",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (votes != null && votes > 0) {
            Text(
                text = "$votes voti su MusicBrainz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}