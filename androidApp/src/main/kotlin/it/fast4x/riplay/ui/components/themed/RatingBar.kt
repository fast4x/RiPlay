package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun RatingBar(rating: Float?, votesCount: Int?) {
    if (rating == null || rating == 0f) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.star),
            contentDescription = "Rating",
            tint = Color(0xFFFFC107), // Giallo stella
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Voto numerico (es. 4.2)
        Text(
            text = String.format("%.1f", rating),
            style = typography().xs.semiBold,
            color = colorPalette().text
        )

        // Votanti (es. (150 voti))
        if (votesCount != null && votesCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "($votesCount voti)",
                style = typography().xxs.semiBold,
                color = colorPalette().text
            )
        }
    }
}