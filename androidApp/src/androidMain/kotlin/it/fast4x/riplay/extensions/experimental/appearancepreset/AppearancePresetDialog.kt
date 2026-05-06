package it.fast4x.riplay.extensions.experimental.appearancepreset

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetUiState
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AppearancePresetDialog(
    activePresetId: String?,
    uiState: PresetUiState,
    onDismiss: () -> Unit,
    onSelect: (AppearancePreset) -> Unit,
    onShare: (AppearancePreset) -> Unit = {}
) {
    val presets = (uiState as? PresetUiState.Success)?.presets ?: emptyList()
    val images = presets.map { it.imageRes ?: R.drawable.preset0 }
    val pagerStateAppearance = rememberPagerState(pageCount = { images.size })

    val activeIndex = presets.indexOfFirst { it.id == activePresetId }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) pagerStateAppearance.scrollToPage(activeIndex)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color = colorPalette().background1)
    ) {
        Box {
            AnimatedContent(
                targetState = presets.getOrNull(pagerStateAppearance.currentPage),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.align(Alignment.TopStart)
            ) { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = preset?.name ?: "",
                        style = typography().xxl,
                        color = colorPalette().text,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .background(
                                colorPalette().background1.copy(alpha = 0.3f),
                                getRoundnessShape()
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Text(
                        text = preset?.author?.let { " by $it" } ?: "",
                        style = typography().xs,
                        color = colorPalette().text,
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .background(
                                colorPalette().background1.copy(alpha = 0.3f),
                                getRoundnessShape()
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            HorizontalPager(
                state = pagerStateAppearance,
                pageSize = PageSize.Fill,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                Image(
                    painter = painterResource(images[index]),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(.8f)
                )
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 30.dp)
                    .padding(end = 15.dp)
                    .background(colorPalette().accent, CircleShape)
                    .align(Alignment.BottomEnd),
            ) {
                IconButton(
                    icon = R.drawable.checkmark,
                    color = colorPalette().background0,
                    indication = ripple(false),
                    onClick = {
                        presets.getOrNull(pagerStateAppearance.settledPage)
                            ?.let(onSelect)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                )
            }
            Row(
                Modifier
                    .height(20.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(images.size) { iteration ->
                    val lineWeight = animateFloatAsState(
                        targetValue = if (pagerStateAppearance.currentPage == iteration) 1.5f
                        else if (iteration < pagerStateAppearance.currentPage) 0.5f
                        else 1f,
                        label = "weight",
                        animationSpec = tween(300, easing = EaseInOut)
                    )
                    val color = if (pagerStateAppearance.currentPage == iteration)
                        Color.White else Color.White.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                            .weight(lineWeight.value)
                            .size(5.dp)
                    )
                }
            }
        }
    }

}