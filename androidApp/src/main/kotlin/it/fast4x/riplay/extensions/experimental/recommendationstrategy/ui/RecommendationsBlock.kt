package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.RecommendationService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.ScoredRecommendation
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun RecommendationsBlock(
    service: RecommendationService,
    onPlayItem: (ScoredRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldShow by service.shouldShowSection.collectAsState()
    val sections by service.visibleSections.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ★ Stato per il dialog "Non mi interessa"
    var showRejectDialog by remember { mutableStateOf<ScoredRecommendation?>(null) }

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
                onItemLongPress = { item ->
                    // ★ Mostra dialog su long-press
                    showRejectDialog = item
                }
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    // ★ Dialog "Non mi interessa"
    showRejectDialog?.let { item ->
        val itemTitle = item.primaryTitle
        val itemId = item.song?.id ?: item.album?.id ?: item.artist?.id ?: ""

        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Non ti interessa?") },
            text = {
                Text("Non ti mostreremo più '$itemTitle' nei suggerimenti per 30 giorni.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        service.markRejected(itemId)
                    }
                    showRejectDialog = null
                }) {
                    Text("Non mi interessa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}