package it.fast4x.riplay.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.ondevice.Folder
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.styling.secondary

@UnstableApi
@Composable
fun FolderItem(
    folder: Folder,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    icon: Int = R.drawable.folder,
    disableScrollingText: Boolean
) {
    ItemContainer(
        alternative = false,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(thumbnailSizeDp)
        ) {

            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorPalette().text),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(thumbnailSizeDp - 15.dp)
            )
        }

        ItemInfoContainer {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = folder.name,
                    style = typography().xs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .applyIf(!disableScrollingText){ basicMarquee(iterations = Int.MAX_VALUE) }
                )
            }
            //if (folder.note?.isNotEmpty() == true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = folder.note ?: folder.fullPath,
                        style = typography().xxxs.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .applyIf(!disableScrollingText){ basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
            }
        }
    }
}