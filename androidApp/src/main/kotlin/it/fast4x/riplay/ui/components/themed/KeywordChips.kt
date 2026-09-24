package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordChips(
    keywords: List<String>?,
    onKeywordClick: (String) -> Unit = {}
) {
    if (keywords.isNullOrEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        keywords.forEach { keyword ->
            SuggestionChip(
                onClick = { onKeywordClick(keyword) },
                label = {
                    Text(
                        text = keyword.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { it.uppercase() }
                        },
                        style = typography().xxs.semiBold
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = colorPalette().background1,
                    labelColor = colorPalette().text
                ),
                border = BorderStroke(1.dp, colorPalette().text)
            )
        }
    }
}