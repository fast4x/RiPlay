package it.fast4x.riplay.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.styling.favoritesOverlay
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.semiBold

@Composable
fun QueueItem(
    title: String,
    isSelected: Boolean,
    acceptSong: Boolean,
    acceptVideo: Boolean,
    acceptPodcast: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {

    val colorPalette = LocalAppearance.current.colorPalette
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val thumbnailSizeDp = Dimensions.thumbnails.song

    ItemContainer(
        alternative = false,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .applyIf(isSelected){
                background(colorPalette.favoritesOverlay)
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )

    ) {

        ItemInfoContainer {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = title,
                    style = typography().xs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                        .weight(1f)
                )

            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                IconButton(
                    onClick = {},
                    icon = R.drawable.musical_notes,
                    color = if (acceptSong) colorPalette().text else colorPalette().textDisabled,
                    modifier = Modifier
                        .size(15.dp)
                )
                IconButton(
                    onClick = {},
                    icon = R.drawable.video,
                    color = if (acceptVideo) colorPalette().text else colorPalette().textDisabled,
                    modifier = Modifier
                        .size(15.dp)
                )
                IconButton(
                    onClick = {},
                    icon = R.drawable.podcast,
                    color = if (acceptPodcast) colorPalette().text else colorPalette().textDisabled,
                    modifier = Modifier
                        .size(15.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {},
                    icon = R.drawable.reorder,
                    color = colorPalette().text,
                    modifier = Modifier
                        .size(20.dp)
                )

            }
        }
    }
}