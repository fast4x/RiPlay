package it.fast4x.riplay.ui.components.themed

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ScrollText (
    text: String,
    style: TextStyle,
    onClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var shouldAnimated by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(shouldAnimated) {
        scrollState.animateScrollTo(
            scrollState.maxValue,
            animationSpec = tween(10000, 300, easing = CubicBezierEasing(0f, 0f, 0f, 0f))
        )

        scrollState.animateScrollTo(
            0,
            animationSpec = tween(10000, 300, easing = CubicBezierEasing(0f, 0f, 0f, 0f))
        )

        shouldAnimated = !shouldAnimated
    }
    BasicText(
        text = AnnotatedString(text),
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier.horizontalScroll(scrollState, true)
            .clickable { onClick() }
            .fillMaxWidth()
    )

}