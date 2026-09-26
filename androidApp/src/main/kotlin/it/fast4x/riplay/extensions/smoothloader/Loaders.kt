package it.fast4x.riplay.extensions.smoothloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.typography
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.utils.applyIf

@Composable
fun Loader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isStartingUp: Boolean = false,
    expandWidth: Boolean = true
) {
    val appSettingsManager = Dependencies.application.appSettingsManager
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .applyIf(expandWidth) {
                fillMaxWidth()
            }
    ) {
        SmoothLoader(
            modifier = Modifier.align(Alignment.Center),
            style = appSettings.loaderStyle,
            size = size,
            isStartingUp = isStartingUp
        )
    }
}

@Composable
fun LoaderScreen(
    show: Boolean = true,
    mini: Boolean = false,
    isStartingUp: Boolean = false,
) {
    if (!show) return

    Column(
        modifier = if (!mini) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Loader(
                isStartingUp = isStartingUp,
                size = if (mini) 48.dp else 96.dp
            )
        }

        if (!mini) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.loading_please_wait),
                style = typography().xs
            )
        }
    }
}


