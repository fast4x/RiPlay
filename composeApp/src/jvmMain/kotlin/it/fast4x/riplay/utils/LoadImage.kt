package it.fast4x.riplay.utils

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import it.fast4x.riplay.enums.ThumbnailRoundness
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.loader

@Composable
fun LoadImage(url: String) = AsyncImage(
    model = url,
    contentDescription = null,
    placeholder = painterResource(Res.drawable.loader),
    error = painterResource(Res.drawable.loader),
    fallback = painterResource(Res.drawable.loader),
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .clip(ThumbnailRoundness.Medium.shape())
        .fillMaxSize()
)