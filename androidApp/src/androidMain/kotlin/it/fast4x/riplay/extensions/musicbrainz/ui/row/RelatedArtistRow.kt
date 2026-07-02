package it.fast4x.riplay.extensions.musicbrainz.ui.row

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui.NatureBadge
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun RelatedArtistRow(
    artist: Artist,
    relationType: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail artista (o placeholder iniziale)
        if (artist.thumbnailUrl != null) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorPalette().background1),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artist.name?.firstOrNull()?.uppercase() ?: "?",
                    style = typography().xs,
                    color = colorPalette().text
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Nome + tipo relazione
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name ?: stringResource(R.string.unknown_artist),
                style = typography().xs,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = relationType.replaceFirstChar { it.uppercase() },
                style = typography().xxs,
                color = colorPalette().text,
                maxLines = 1
            )
        }

        // Nature badge (se AI o compilation, utile evidenziazione)
        if (artist.nature != ArtistNature.HUMAN && artist.nature != ArtistNature.UNKNOWN) {
            NatureBadge(
                artistNature = artist.nature,
                albumNature = null
            )
        }
    }
}