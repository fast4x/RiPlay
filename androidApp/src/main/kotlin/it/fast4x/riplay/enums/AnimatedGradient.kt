package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class AnimatedGradient {
    FluidThemeColorGradient,
    FluidCoverColorGradient,
    Linear,
    Mesh,
    MesmerizingLens,
    GlossyGradients,
    GradientFlow,
    PurpleLiquid,
    InkFlow,
    OilFlow,
    IceReflection,
    Stage,
    GoldenMagma,
    BlackCherryCosmos,
    Random;

    val displayName: String
        @Composable
        get() = when (this) {
            AnimatedGradient.FluidThemeColorGradient -> stringResource(R.string.bg_colors_fluid_gradient_background_from_theme)
            AnimatedGradient.FluidCoverColorGradient -> stringResource(R.string.bg_colors_fluid_gradient_background_from_cover)
            AnimatedGradient.Linear -> stringResource(R.string.linear)
            AnimatedGradient.Mesh -> stringResource(R.string.mesh)
            AnimatedGradient.MesmerizingLens -> stringResource(R.string.mesmerizinglens)
            AnimatedGradient.GlossyGradients -> stringResource(R.string.glossygradient)
            AnimatedGradient.GradientFlow -> stringResource(R.string.gradientflow)
            AnimatedGradient.PurpleLiquid -> stringResource(R.string.purpleliquid)
            AnimatedGradient.Stage -> stringResource(R.string.stage)
            AnimatedGradient.InkFlow -> stringResource(R.string.inkflow)
            AnimatedGradient.GoldenMagma -> stringResource(R.string.goldenmagma)
            AnimatedGradient.OilFlow -> stringResource(R.string.oilflow)
            AnimatedGradient.IceReflection -> stringResource(R.string.icereflection)
            AnimatedGradient.BlackCherryCosmos -> stringResource(R.string.blackcherrycosmos)
            AnimatedGradient.Random -> stringResource(R.string.random)
        }
}