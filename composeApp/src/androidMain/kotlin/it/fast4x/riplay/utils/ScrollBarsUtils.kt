package it.fast4x.riplay.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import io.github.oikvpqya.compose.fastscroller.ScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.ThumbStyle
import io.github.oikvpqya.compose.fastscroller.TrackStyle
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import it.fast4x.riplay.colorPalette


@Composable
fun LazyListContainer(
    state: LazyListState,
    content: @Composable () -> Unit
){
    Box() {
        content()
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = state),
            style = defaultScrollbarStyle(),
            enablePressToScroll = true,
        )
    }
}

@Composable
fun LazyListContainer(
    state: LazyGridState,
    content: @Composable () -> Unit
){
    Box() {
        content()
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = state),
            style = defaultScrollbarStyle(),
            enablePressToScroll = true,
        )
    }
}

@Composable
fun defaultScrollbarStyle() = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 4.dp,
    hoverDurationMillis = 300,
    thumbStyle = ThumbStyle(
        shape = RoundedCornerShape(2.dp),
        unhoverColor = colorPalette().accent,
        hoverColor = colorPalette().accent,
    ),
    trackStyle = TrackStyle(
        shape = RectangleShape,
        unhoverColor = colorPalette().background1,
        hoverColor = colorPalette().background1,
    ),
)