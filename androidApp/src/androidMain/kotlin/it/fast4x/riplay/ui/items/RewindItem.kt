package it.fast4x.riplay.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.commonutils.PIPED_PREFIX
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.onOverlay
import it.fast4x.riplay.ui.styling.overlay
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography

@Composable
fun RewindItem(
    thumbnailContent: @Composable BoxScope.() -> Unit,
    name: String?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    homePage: Boolean = false,
    iconSize: Dp = 0.dp,
    alternative: Boolean = false,
    showName: Boolean = true,
    disableScrollingText: Boolean,
) {
    val localIconSize = remember { if (homePage) 0.2*iconSize else 30.dp }
    ItemContainer(
        alternative = alternative,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(thumbnailShape())
                .background(color = colorPalette().background4)
                .requiredSize(thumbnailSizeDp)
        ) {
            thumbnailContent()

            name?.let {
                Image(
                    painter = painterResource(R.drawable.stat_year),
                    colorFilter = ColorFilter.tint(colorPalette().accent),
                    modifier = Modifier
                        .padding(all = 5.dp)
                        .background(colorPalette().text, CircleShape)
                        .size(localIconSize)
                        .padding(all = 5.dp),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Fit
                )
            }
        }


        ItemInfoContainer(
            horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (showName)
                if (name != null) {
                    BasicText(
                        text = cleanPrefix(name),
                        style = typography().xs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                }

        }
    }
}