package it.fast4x.riplay.utils

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kieronquinn.monetcompat.core.MonetCompat
import it.fast4x.riplay.ui.styling.ColorPalette
import it.fast4x.riplay.ui.styling.DefaultLightColorPalette
import kotlinx.coroutines.launch

val LocalMonetCompat = staticCompositionLocalOf { MonetCompat.getInstance() }

context(LifecycleOwner)
inline fun MonetCompat.invokeOnReady(
    state: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline block: () -> Unit
) = lifecycleScope.launch {
    repeatOnLifecycle(state) {
        awaitMonetReady()
        block()
    }
}

fun MonetCompat.setDefaultPalette(palette: ColorPalette = DefaultLightColorPalette) {
    defaultAccentColor = palette.accent.toArgb()
    defaultBackgroundColor = palette.background0.toArgb()
    defaultPrimaryColor = palette.background1.toArgb()
    defaultSecondaryColor = palette.background2.toArgb()
}
