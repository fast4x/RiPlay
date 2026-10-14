package it.fast4x.riplay.extensions.hypnoticcanvasshaderfallback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import it.fast4x.riplay.enums.AnimatedGradient
import kotlin.math.floor
import kotlin.math.sin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

fun fract(x: Float): Float = x - floor(x)
fun hash(n: Float): Float = fract(sin(n * 127.1f) * 43758.547f)

/**
 * Tempo continuo in secondi, aggiornato ad ogni frame.
 * Va letto SOLO nel draw phase (drawBehind): nessuna recomposition per frame.
 */
@Composable
fun rememberShaderClock(loopSeconds: Float = 60f): State<Float> =
    produceState(0f, loopSeconds) {
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0L) start = now
                value = ((now - start) / 1_000_000_000f) % loopSeconds
            }
        }
    }

fun Modifier.shaderBackgroundFallback(
    effect: AnimatedGradient,
    speed: Float,
): Modifier = composed {
    val currentSpeed by rememberUpdatedState(speed) // aggiornato a ogni recomposition, senza restart
    val clock = rememberPausableShaderClock({ currentSpeed })
    drawBehind {
        when (effect) {
            AnimatedGradient.MesmerizingLens    -> paintMesmerizingLens(clock.value)
            AnimatedGradient.GlossyGradients    -> paintGlossyGradients(clock.value)
            AnimatedGradient.GradientFlow       -> paintGradientFlow(clock.value)
            AnimatedGradient.PurpleLiquid       -> paintPurpleLiquid(clock.value)
            AnimatedGradient.InkFlow            -> paintInkFlow(clock.value)
            AnimatedGradient.OilFlow            -> paintOilFlow(clock.value)
            AnimatedGradient.IceReflection      -> paintIceReflection(clock.value)
            AnimatedGradient.Stage              -> paintStage(clock.value)
            AnimatedGradient.GoldenMagma        -> paintGoldenMagma(clock.value)
            AnimatedGradient.BlackCherryCosmos  -> paintBlackCherryCosmos(clock.value)
            AnimatedGradient.SpectralAura       -> paintSpectralAura(clock.value)
            AnimatedGradient.PrismGlass         -> paintPrismGlass(clock.value)
            AnimatedGradient.Heat               -> paintHeat(clock.value)
            AnimatedGradient.Stripy             -> paintStripy(clock.value)
            AnimatedGradient.BubbleRings        -> paintBubbleRings(clock.value)
            AnimatedGradient.FluidCoverColorGradient,
            AnimatedGradient.FluidThemeColorGradient -> paintFluid(clock.value)
            AnimatedGradient.Mesh               -> paintHypnoticSpiral(clock.value)
            AnimatedGradient.Linear             -> paintMesmerizing(clock.value)
            AnimatedGradient.Random             -> {}
        }
    }
}


