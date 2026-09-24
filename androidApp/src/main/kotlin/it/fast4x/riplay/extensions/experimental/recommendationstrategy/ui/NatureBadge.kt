package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.enums.ArtistNature


@Composable
fun NatureBadge(
    artistNature: ArtistNature?,
    albumNature: AlbumNature?,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when {
        // Artist natures
        artistNature == ArtistNature.AI_GENERATED -> Triple("🤖", "AI", Color(0xFFE91E63))
        artistNature == ArtistNature.VIRTUAL -> Triple("👤", "Virtual", Color(0xFF9C27B0))
        artistNature == ArtistNature.COVER_TRIBUTE -> Triple("🎤", "Cover", Color(0xFFFF9800))
        artistNature == ArtistNature.COMPILATION -> Triple("📦", "Compilation", Color(0xFF795548))
        artistNature == ArtistNature.HUMAN -> Triple("🎸", "Umano", Color(0xFF4CAF50))  // ← AGGIUNTO

        // Album natures (se l'artista è null o UNKNOWN)
        albumNature == AlbumNature.AI_GENERATED -> Triple("🤖", "AI", Color(0xFFE91E63))
        albumNature == AlbumNature.COMPILATION -> Triple("📦", "Compilation", Color(0xFF795548))
        albumNature == AlbumNature.LIVE -> Triple("🔴", "Live", Color(0xFFF44336))
        albumNature == AlbumNature.SOUNDTRACK -> Triple("🎬", "OST", Color(0xFF2196F3))
        albumNature == AlbumNature.STUDIO_ALBUM -> Triple("💿", "Studio", Color(0xFF607D8B))  // ← AGGIUNTO
        albumNature == AlbumNature.LIVE -> Triple("🔴", "Live", Color(0xFFF44336))
        albumNature == AlbumNature.SINGLE -> Triple("🎵", "Single", Color(0xFF009688))
        albumNature == AlbumNature.EP -> Triple("唱片", "EP", Color(0xFF8BC34A))

        // Default: niente badge per UNKNOWN
        else -> return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}