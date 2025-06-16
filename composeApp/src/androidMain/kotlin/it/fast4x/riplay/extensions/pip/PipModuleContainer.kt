package it.fast4x.riplay.extensions.pip

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
@Composable
fun PipModuleContainer(
    content: @Composable (modifier: Modifier) -> Unit
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        content(
            Modifier
                .align(Alignment.Center)
        )
    }
}