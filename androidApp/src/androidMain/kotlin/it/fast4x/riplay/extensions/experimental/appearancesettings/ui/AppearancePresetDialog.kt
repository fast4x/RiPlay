package it.fast4x.riplay.extensions.experimental.appearancesettings.ui

import androidx.annotation.OptIn
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetSource
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetUiState
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography

@OptIn(UnstableApi::class)
@Composable
fun AppearancePresetDialog(
    activePresetId: String?,
    uiState: PresetUiState,
    onDismiss: () -> Unit,
    onSelect: (AppearancePreset) -> Unit,
    onShare: (AppearancePreset) -> Unit = {},
    onExport: (String) -> Unit,
    onImport: () -> Unit,
    onDelete: (String) -> Unit = {}
) {
    val presets = (uiState as? PresetUiState.Success)?.presets ?: emptyList()
    val images = presets.map { it.imageRes ?: R.drawable.preset0 }
    val pagerStateAppearance = rememberPagerState(pageCount = { images.size })

    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value


    val activeIndex = presets.indexOfFirst { it.id == activePresetId }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) pagerStateAppearance.scrollToPage(activeIndex)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color = Color.Black).fillMaxSize()
    ) {
        Box {
            HorizontalPager(
                state = pagerStateAppearance,
                pageSize = PageSize.Fill,
                beyondViewportPageCount = 0,
                modifier = Modifier.fillMaxWidth()
            ) { index ->
                Image(
                    painter = painterResource(
                        presets[index].imageRes ?: R.drawable.presetx),
//                        if(presets[index].id == "user_custom_legacy_preset")
//                            R.drawable.image
//                        else presets[index].imageRes ?: R.drawable.image),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                    colorFilter = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
            AnimatedContent(
                targetState = presets.getOrNull(pagerStateAppearance.currentPage),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp)
            ) { preset ->
                if (preset?.id == activePresetId)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.checkmark),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette().text),
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column() {
                        Text(
                            text = preset?.name ?: "",
                            style = typography().xxl,
                            color = colorPalette().text,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        Text(
                            text = preset?.source?.name ?: "",
                            style = typography().xxxs,
                            color = colorPalette().text,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = preset?.author?.let { " by $it" } ?: "",
                        style = typography().xs,
                        color = colorPalette().text,
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                }
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 30.dp)
                    .padding(horizontal = 15.dp)
                    .background(
                        colorPalette().accent.copy(alpha = .2f),
                        appearanceSettings.thumbnailRoundness.shape()
                    )
                    .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        icon = R.drawable.chevron_down,
                        color = colorPalette().text,
                        indication = ripple(false),
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    if (presets.getOrNull(pagerStateAppearance.settledPage)?.source != PresetSource.BUILTIN)
                        IconButton(
                            icon = R.drawable.close,
                            color = colorPalette().text,
                            indication = ripple(false),
                            onClick = {
                                val presetId = presets.getOrNull(pagerStateAppearance.settledPage)?.id
                                if (presetId != null) onDelete(presetId)
                            },
                            modifier = Modifier
                                .size(24.dp)
                        )

                    IconButton(
                        icon = R.drawable.export,
                        color = colorPalette().text,
                        indication = ripple(false),
                        onClick = {
                            val presetName = presets.getOrNull(pagerStateAppearance.settledPage)?.name
                            if (presetName != null) onExport(presetName)
                        },
                        modifier = Modifier
                            .size(24.dp)
                    )
                    IconButton(
                        icon = R.drawable.resource_import,
                        color = colorPalette().text,
                        indication = ripple(false),
                        onClick = { onImport() },
                        modifier = Modifier
                            .size(24.dp)
                    )

                    if (presets.getOrNull(pagerStateAppearance.settledPage)?.id != activePresetId)
                        IconButton(
                            icon = R.drawable.checkmark,
                            color = colorPalette().text,
                            indication = ripple(false),
                            onClick = {
                                val preset = presets.getOrNull(pagerStateAppearance.settledPage)
                                if (preset != null) onSelect(preset)
                            },
                            modifier = Modifier
                                .size(24.dp)
                        )
                }
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