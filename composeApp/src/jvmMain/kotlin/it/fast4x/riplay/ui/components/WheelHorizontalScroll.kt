package it.fast4x.riplay.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


fun Modifier.wheelToHorizontalScroll(state: ScrollableState): Modifier = composed {
    // una tacca di rotella (delta ~ 1) = 50px di scorrimento: deciso ma non brusco
    val pxPerUnit = with(LocalDensity.current) { 50.dp.toPx() }
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                // se un componente interno ha gia' consumato lo scroll (es. uno
                // slider sotto il puntatore) non interveniamo
                if (change.isConsumed) continue
                val delta = change.scrollDelta
                val units = delta.y + delta.x
                if (units == 0f) continue
                state.dispatchRawDelta(units * pxPerUnit)
                // consumato = il verticalScroll della pagina padre non scatta
                change.consume()
            }
        }
    }
}
