package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.enums.ArtistNature

@Composable
fun NatureBadgeLarge(
    nature: ArtistNature?,
    modifier: Modifier = Modifier
) {
    if (nature == null || nature == ArtistNature.UNKNOWN) return

    val (icon, label, color) = when (nature) {
        ArtistNature.AI_GENERATED -> Triple("🤖", "AI Generated", Color(0xFFE91E63))
        ArtistNature.VIRTUAL -> Triple("👤", "Virtual Artist", Color(0xFF9C27B0))
        ArtistNature.COVER_TRIBUTE -> Triple("🎤", "Tribute/Cover", Color(0xFFFF9800))
        ArtistNature.COMPILATION -> Triple("📦", "Compilation", Color(0xFF795548))
        ArtistNature.HUMAN -> Triple("🎸", "Human Artist", Color(0xFF4CAF50))
        ArtistNature.UNKNOWN -> return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NatureBadgeLarge(
    nature: AlbumNature?,
    modifier: Modifier = Modifier
) {
    if (nature == null || nature == AlbumNature.UNKNOWN) return

    val (icon, label, color) = when (nature) {
        AlbumNature.AI_GENERATED -> Triple("🤖", "AI Generated", Color(0xFFE91E63))
        AlbumNature.COMPILATION -> Triple("📦", "Compilation", Color(0xFF795548))
        AlbumNature.LIVE -> Triple("🔴", "Live", Color(0xFFF44336))
        AlbumNature.SOUNDTRACK -> Triple("🎬", "Soundtrack", Color(0xFF2196F3))
        AlbumNature.STUDIO_ALBUM -> Triple("💿", "Studio Album", Color(0xFF607D8B))
        AlbumNature.SINGLE -> Triple("🎵", "Single", Color(0xFF009688))
        AlbumNature.EP -> Triple("唱片", "EP", Color(0xFF8BC34A))
        AlbumNature.UNKNOWN -> return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}