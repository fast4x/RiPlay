package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoredRecommendation
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun RecommendationsBlock(
    service: RecommendationService,
    onPlayItem: (ScoredRecommendation) -> Unit,
    onRejectItem: (ScoredRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldShow by service.shouldShowSection.collectAsState()
    val sections by service.visibleSections.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (!shouldShow) return

    Column(modifier = modifier.fillMaxWidth()) {
        RecommendationHeader(
            isRefreshing = isRefreshing.value,
            onRefresh = {
                scope.launch {
                    isRefreshing.value = true
                    service.refreshAll()
                    isRefreshing.value = false
                }
            }
        )

        sections.forEach { section ->
            Spacer(Modifier.height(8.dp))
            RecommendationSubSection(
                section = section,
                onItemClick = onPlayItem,
                onItemLongPress = onRejectItem
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}