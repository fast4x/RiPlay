package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun RecommendationHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.sparkles),
            contentDescription = null,
            tint = colorPalette().text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Per te dal tuo ascolto",
                style = typography().s
            )
            Text(
                text = "Suggerimenti basati sulla tua libreria",
                style = typography().xs,
                color = colorPalette().text
            )
        }
        IconButton(onClick = onRefresh, enabled = !isRefreshing, colors = IconButtonDefaults.iconButtonColors().copy(containerColor = colorPalette().text)) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = "Aggiorna suggerimenti"
                )
            }
        }
    }
}