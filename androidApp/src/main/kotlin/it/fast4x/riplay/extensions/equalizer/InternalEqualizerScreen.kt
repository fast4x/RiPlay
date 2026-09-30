package it.fast4x.riplay.extensions.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.services.helpers.BandConfig
import it.fast4x.riplay.services.helpers.EqualizerConfig
import it.fast4x.riplay.services.helpers.EqualizerHelper
import it.fast4x.riplay.ui.components.themed.VerticalSlider
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlin.math.abs
import kotlin.math.roundToInt
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.EqualizerPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InternalEqualizerScreen(equalizerHelper: EqualizerHelper) {
    val context = LocalContext.current
    var config by remember { mutableStateOf<EqualizerConfig?>(null) }
    val bandLevels = remember { mutableStateMapOf<Short, Float>() }

    var isLinked by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(EqualizerPreset.FLAT) }
    var isEqEnabled by remember { mutableStateOf(false) }

    val preampDb by equalizerHelper.preampDb.collectAsState()

    LaunchedEffect(Unit) {
        withTimeoutOrNull(5000.milliseconds) { equalizerHelper.eqReady.first { it } }
        val cfg = equalizerHelper.getEqualizerConfig() ?: return@LaunchedEffect
        config = cfg

        val saved = equalizerHelper.loadSettings()
        if (saved == null) {
            cfg.bands.forEach { bandLevels[it.index] = 0.5f }
            return@LaunchedEffect
        }
        isEqEnabled = saved.first
        saved.second?.let { selectedPreset = EqualizerPreset.fromString(it) }
        saved.third.forEach { (i, p) -> bandLevels[i] = p }

    }

    config?.let { cfg ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(colorPalette().background1, RoundedCornerShape(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.equalizer), style = typography().xl, color = colorPalette().text)
                it.fast4x.riplay.ui.components.themed.IconButton(
                    onClick = { isLinked = !isLinked },
                    icon = R.drawable.link,
                    color = if (isLinked) colorPalette().text else colorPalette().textDisabled
                )
                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = { isChecked ->
                        isEqEnabled = isChecked
                        cfg.let { equalizerHelper.applyAndSave(isChecked, selectedPreset.name, it, bandLevels) }
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = colorPalette().accent,
                        checkedTrackColor = colorPalette().text,
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }
            if (isEqEnabled && preampDb != 0f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.equalizer_pre_amp_title_pre_amp_1f_db).format(preampDb),
                        fontSize = 12.sp,
                        color = colorPalette().textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (isEqEnabled) 1f else 0.3f
                    }
                    .fillMaxWidth()
            ) {

                InternalEqualizerCurve(
                    bandLevels = bandLevels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                PresetSelector(
                    selectedPreset = selectedPreset,
                    onPresetSelected = { preset ->
                        selectedPreset = preset
                        isEqEnabled = true
                        applyPreset(preset, cfg, bandLevels, equalizerHelper)
                    },
                    onReset = {
                        selectedPreset = EqualizerPreset.FLAT
                        equalizerHelper.reset()
                        val centerPercent = levelToPercent(
                            ((cfg.minLevel + cfg.maxLevel) / 2).toShort(),
                            cfg.minLevel,
                            cfg.maxLevel
                        )
                        cfg.bands.forEach { bandLevels[it.index] = centerPercent }
                        equalizerHelper.applyAndSave(isEqEnabled, selectedPreset.name, cfg, bandLevels)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .height(300.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    cfg.bands.forEach { band ->
                        BandColumnLinked(
                            helper = equalizerHelper,
                            bandConfig = band,
                            minLevel = cfg.minLevel,
                            maxLevel = cfg.maxLevel,
                            currentPercent = bandLevels[band.index] ?: 0.5f,
                            isLinked = isLinked,
                            allBandLevels = bandLevels,
                            onPercentChanged = { updatedLevels ->
                                updatedLevels.forEach { (k, v) -> bandLevels[k] = v }
                                equalizerHelper.applyAndSave(isEqEnabled, selectedPreset.name, cfg, bandLevels)
                            },
                            enabled = isEqEnabled
                        )
                    }
                }
            }
        }
    } ?: run {
        Text("EqualizerHelper UI loading...")
    }
}

@Composable
fun BandColumnLinked(
    helper: EqualizerHelper,
    bandConfig: BandConfig,
    minLevel: Short,
    maxLevel: Short,
    currentPercent: Float,
    isLinked: Boolean,
    allBandLevels: Map<Short, Float>,
    onPercentChanged: (Map<Short, Float>) -> Unit,
    enabled: Boolean
) {
    var sliderPosition by remember { mutableStateOf(currentPercent) }

    LaunchedEffect(currentPercent) {
        sliderPosition = currentPercent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Text(
            "${abs(minLevel / 100)}",
            fontSize = typography().xxxs.fontSize,
            color = colorPalette().text
        )

        Spacer(modifier = Modifier.height(12.dp))

        VerticalSlider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue

                val newLevels = allBandLevels.toMutableMap()

                if (isLinked) {
                    val oldPercent = allBandLevels[bandConfig.index] ?: 0.5f
                    val delta = newValue - oldPercent

                    newLevels.forEach { (index, percent) ->
                        newLevels[index] = (percent + delta).coerceIn(0f, 1f)
                    }
                } else {
                    newLevels[bandConfig.index] = newValue
                }

                onPercentChanged(newLevels)
            },
            modifier = Modifier.weight(1f)
        )

        if (!enabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    }
            )
        }

        val currentMb = helper.percentToMb(sliderPosition)
        Text(
            "${currentMb / 100}",
            fontSize = 10.sp,
            color = colorPalette().text,
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "${minLevel / 100}",
            fontSize = typography().xxxs.fontSize,
            color = colorPalette().text
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            formatFrequency(bandConfig.centerFreq),
            fontSize = typography().xxs.fontSize,
            color = colorPalette().text
        )
    }

}

// Convert level to mB (es. -1500) in percent (0.0 - 1.0)
private fun levelToPercent(level: Short, min: Short, max: Short): Float {
    val range = max - min
    val current = level - min
    return if (range > 0) current.toFloat() / range.toFloat() else 0.5f
}

// Convert pencentage to level mB
private fun percentToLevel(percent: Float, min: Short, max: Short): Short {
    val range = max - min
    val level = min + (range * percent)
    return level.roundToInt().toShort()
}

// Format 60000 -> "60Hz", 1000000 -> "1kHz"
private fun formatFrequency(milliHz: Int): String {
    return when {
        milliHz >= 1000000 -> "${milliHz / 1000000}kHz"
        milliHz >= 1000 -> "${milliHz / 1000}Hz"
        else -> "${milliHz}Hz" // Raro per gli EQ
    }
}

@Composable
fun PresetSelector(
    selectedPreset: EqualizerPreset,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onReset: () -> Unit
) {
    val presets = remember { EqualizerPreset.entries }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(presets) { preset ->
            FilterChip(
                selected = selectedPreset == preset,
                onClick = { onPresetSelected(preset) },
                label = { 
                    Text(
                        text = stringResource(id = preset.labelRes), 
                        fontSize = 12.sp
                    ) 
                },
                modifier = Modifier.height(32.dp)
            )
        }
        
        item {
            TextButton(
                onClick = onReset, 
                modifier = Modifier.height(32.dp)
            ) {
                Text(stringResource(R.string.equalizer_reset), fontSize = 12.sp)
            }
        }
    }
}

fun applyPreset(
    preset: EqualizerPreset,
    cfg: EqualizerConfig,
    bandLevels: MutableMap<Short, Float>,
    helper: EqualizerHelper
) {
    val count = cfg.bands.size
    if (count == 0) return

    val shape: List<Float> = when (preset) {
        EqualizerPreset.BASS_BOOST -> listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        EqualizerPreset.ROCK       -> listOf(0.8f, 0.7f, 0.5f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
        EqualizerPreset.JAZZ       -> listOf(0.7f, 0.7f, 0.6f, 0.6f, 0.7f, 0.7f, 0.6f, 0.5f, 0.5f, 0.5f)
        EqualizerPreset.VOCAL      -> listOf(0.4f, 0.5f, 0.6f, 0.8f, 0.9f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f)
        EqualizerPreset.CLASSICAL  -> listOf(0.6f, 0.6f, 0.5f, 0.5f, 0.5f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f)
        EqualizerPreset.POP        -> listOf(0.8f, 0.7f, 0.6f, 0.6f, 0.7f, 0.7f, 0.6f, 0.5f, 0.5f, 0.5f)
        EqualizerPreset.ELECTRONIC -> listOf(1.0f, 0.9f, 0.7f, 0.5f, 0.4f, 0.5f, 0.7f, 0.9f, 1.0f, 1.0f)
        EqualizerPreset.DANCE      -> listOf(0.9f, 0.8f, 0.6f, 0.5f, 0.5f, 0.7f, 0.8f, 0.9f, 1.0f, 1.0f)
        EqualizerPreset.ACOUSTIC   -> listOf(0.6f, 0.6f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.6f, 0.6f, 0.6f)
        EqualizerPreset.FLAT       -> List(count) { 0.5f }
    }

    // Costruisce la mappa
    val newMap: Map<Short, Float> = if (count == 1) {
        mapOf(cfg.bands[0].index to shape[shape.size / 2])
    } else {
        (0 until count).associate { i ->
            val t = i.toFloat() / (count - 1) * (shape.size - 1)
            val i0 = t.toInt().coerceAtMost(shape.size - 2)
            val percent = shape[i0] + (shape[i0 + 1] - shape[i0]) * (t - i0)
            cfg.bands[i].index to percent
        }
    }

    // Aggiorna lo stato UI
    bandLevels.clear()
    bandLevels.putAll(newMap)

    // Applica preset selezionato = scelta intenzionale di attivare l'EQ
    helper.applyAndSave(true, preset.name, cfg, newMap)
}
