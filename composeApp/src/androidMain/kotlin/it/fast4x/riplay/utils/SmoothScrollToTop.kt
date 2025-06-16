package it.fast4x.riplay.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState

suspend fun LazyGridState.smoothScrollToTop() {
    if (firstVisibleItemIndex > layoutInfo.visibleItemsInfo.size) {
        scrollToItem(layoutInfo.visibleItemsInfo.size)
    }
    animateScrollToItem(0)
}

suspend fun LazyListState.smoothScrollToTop() {
    if (firstVisibleItemIndex > layoutInfo.visibleItemsInfo.size) {
        scrollToItem(layoutInfo.visibleItemsInfo.size)
    }
    animateScrollToItem(0)
}
