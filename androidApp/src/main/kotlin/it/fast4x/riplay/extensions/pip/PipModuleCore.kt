package it.fast4x.riplay.extensions.pip

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
@OptIn(UnstableApi::class)
fun PipModuleCore(
    modifier: Modifier = Modifier,
    onlineCore: @Composable () -> Unit,
){
    Box(
        modifier = modifier
            //.border(BorderStroke(2.dp, color = Color.White))
            .fillMaxSize()
    ) {
        onlineCore()
    }
}