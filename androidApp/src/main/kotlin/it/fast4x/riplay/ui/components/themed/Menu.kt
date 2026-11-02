package it.fast4x.riplay.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.tv.LocalIsTv
import it.fast4x.riplay.ui.components.tv.tvFocusRing
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Controller per il focus automatico della prima voce del menu su TV.
 * La prima MenuEntry composta "rivendica" il requester; il Menu poi
 * sposta il focus lì appena il menu è visibile.
 */
class TvMenuFocusController {
    internal var claimed = false
    internal val requester = FocusRequester()
}

val LocalTvMenuFocusController = staticCompositionLocalOf { TvMenuFocusController() }

@Composable
fun Menu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isTv = LocalIsTv.current
    val tvMenu = remember { TvMenuFocusController() }

    // Su TV: porta il focus sulla prima voce appena il menu è composto,
    // così il D-pad funziona subito senza un "colpo alla cieca".
    LaunchedEffect(Unit) {
        if (isTv) {
            awaitFrame()
            delay(50.milliseconds)
            runCatching { tvMenu.requester.requestFocus() }
        }
    }

    CompositionLocalProvider(LocalTvMenuFocusController provides tvMenu) {
        Column(
            modifier = modifier
                //.padding(top = 12.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .background(colorPalette().background1)
                //.padding(top = 2.dp)
                .padding(vertical = 8.dp)
                .navigationBarsPadding(),
            content = content
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuEntry(
    painter: Painter,
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val isTv = LocalIsTv.current
    val tvMenu = LocalTvMenuFocusController.current
    // La PRIMA voce composta rivendica il focus automatico (solo TV)
    var entryModifier = Modifier
        .tvFocusRing(shape = RoundedCornerShape(10.dp), focusedScale = 1f)
    if (isTv && !tvMenu.claimed && enabled) {
        tvMenu.claimed = true
        entryModifier = entryModifier.focusRequester(tvMenu.requester)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = entryModifier
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 24.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette().text),
            modifier = Modifier
                .size(15.dp)
        )

        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f)
        ) {
            BasicText(
                text = text,
                style = typography().xs.medium
            )

            secondaryText?.let { secondaryText ->
                BasicText(
                    text = secondaryText,
                    style = typography().xxs.medium.secondary
                )
            }
        }

        trailingContent?.invoke()

    }
}

@Composable
fun MenuEntry(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    MenuEntry(
        painterResource( icon ),
        text,
        onClick,
        onLongClick,
        secondaryText,
        enabled,
        trailingContent
    )
}

@Composable
fun SubMenuEntry(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
) {
    MenuEntry(
        painterResource( icon ),
        text,
        onClick,
        onLongClick,
        secondaryText,
        enabled,
        trailingContent = {
            Image(
                painter = painterResource(R.drawable.chevron_forward),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    colorPalette().textSecondary
                ),
                modifier = Modifier
                    .size(16.dp)
            )
        }
    )
}
