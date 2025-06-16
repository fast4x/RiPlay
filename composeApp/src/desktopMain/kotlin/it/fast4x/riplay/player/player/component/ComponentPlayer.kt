package player.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import player.DefaultControls
import player.PlayerController
import java.awt.Component

@Composable
fun ComponentPlayer(
    modifier: Modifier = Modifier,
    url: String,
    component: Component,
    controller: PlayerController,
    showControls: Boolean = true,
    showComponent: Boolean = true
) {
    DisposableEffect(url) {
        controller.load(url)
        onDispose { controller.dispose() }
    }
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (showComponent)
            ComponentContainer(Modifier.requiredHeight(400.dp), component)
        if (showControls)
            DefaultControls(Modifier.fillMaxWidth(), controller)
    }
}