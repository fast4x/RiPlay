package it.fast4x.riplay.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
fun DelayedControls(
    delayControls: Boolean = false,
    time: Long = 5000,
    onTime: () -> Unit
) {
    LaunchedEffect(delayControls) {
        if (delayControls) {
            delay(time)
            onTime()
        }
    }
}