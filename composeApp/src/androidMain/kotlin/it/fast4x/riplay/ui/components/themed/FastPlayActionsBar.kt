package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.globalContext

@Composable
fun FastPlayActionsBar(
    modifier: Modifier = Modifier,
    onPlayNowClick: (() -> Unit)? = null,
    onShufflePlayClick: (() -> Unit)? = null,
    onSmartRecommendationClick: (() -> Unit)? = null,
    isRecommendationEnabled: Boolean = false,
    iconSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        onPlayNowClick?.let { onPlayNowClick ->
            HeaderIconButton(
                icon = R.drawable.play_now,
                color = colorPalette().text,
                onClick = {},
                modifier = Modifier
                    .combinedClickable(
                        onClick = onPlayNowClick,
                        onLongClick = {
                            SmartMessage(
                                globalContext().resources.getString(R.string.play_now),
                                context = globalContext()
                            )
                        }
                    ),
                iconSize = iconSize
            )
        }
        onShufflePlayClick?.let { onShufflePlayClick ->
            HeaderIconButton(
                icon = R.drawable.play_shuffle,
                color = colorPalette().text,
                onClick = {},
                modifier = Modifier
                    .combinedClickable(
                        onClick = onShufflePlayClick,
                        onLongClick = {
                            SmartMessage(
                                globalContext().resources.getString(R.string.shuffle_play),
                                context = globalContext()
                            )
                        }
                    ),
                iconSize = iconSize
            )
        }
        onSmartRecommendationClick?.let { onSmartRecommendationClick ->
            HeaderIconButton(
                icon = R.drawable.smart_shuffle,
                color = if (isRecommendationEnabled) colorPalette().text else colorPalette().textDisabled,
                onClick = {},
                modifier = Modifier
                    .combinedClickable(
                        onClick = onSmartRecommendationClick,
                        onLongClick = {
                            SmartMessage(
                                globalContext().resources.getString(R.string.info_smart_recommendation),
                                context = globalContext()
                            )
                        }
                    ),
                iconSize = 36.dp
            )
        }
    }
}