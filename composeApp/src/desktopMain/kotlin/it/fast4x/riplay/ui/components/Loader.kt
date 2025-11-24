package it.fast4x.riplay.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.loader

@Composable
fun Loader(
    modifier: Modifier = Modifier.fillMaxWidth()
) =
    Box(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(Res.drawable.loader),
            colorFilter = ColorFilter.tint(Color.White),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(50.dp)
        )
    }