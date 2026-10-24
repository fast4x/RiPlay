package it.fast4x.riplay.ui.styling

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.palette.graphics.Palette
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.styling.Hsl
import it.fast4x.riplay.styling.hsl

/* ------------------------------------------------------------------------------------------------
 *  Encore Dark – paleta fija. No hay temas claros, dinámicos ni personalizados.
 * ---------------------------------------------------------------------------------------------- */
object Encore {
    val Background      = Color(0xFF121212) // fondo principal
    val Surface         = Color(0xFF181818) // tarjetas, barra inferior
    val SurfaceElevated = Color(0xFF282828) // cajitas de inicio, hover, chips
    val SurfaceHighest  = Color(0xFF3E3E3E) // pressed / separadores fuertes
    val SurfaceBlack    = Color(0xFF000000) // base del degradado de la barra inferior

    val Green           = Color(0xFF1DB954) // acento
    val GreenActive     = Color(0xFF1ED760) // acento presionado / activo
    val OnGreen         = Color(0xFF000000) // texto/icono encima del verde

    val TextPrimary     = Color(0xFFFFFFFF)
    val TextSecondary   = Color(0xFFB3B3B3)
    val TextDisabled    = Color(0xFF535353)

    val Red             = Color(0xFFE91429) // errores / borrar
    val Blue            = Color(0xFF509BF5)

    // Degradado de "Canciones que te gustan"
    val LikedSongsTop    = Color(0xFF450AF5)
    val LikedSongsBottom = Color(0xFFC4EFD9)
}

val youtubeItemTintColor = Color.Red.copy(0.75f).compositeOver(Color.White)

@Immutable
data class ColorPalette(
    val background0: Color,
    val background1: Color,
    val background2: Color,
    val background3: Color,
    val background4: Color,
    val accent: Color,
    val onAccent: Color,
    val red: Color = Encore.Red,
    val blue: Color = Encore.Blue,
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val isDark: Boolean,
    val iconButtonPlayer: Color,
    val accentActive: Color = Encore.GreenActive,
) {
    companion object : Saver<ColorPalette, List<Any>> {
        // Sólo existe una paleta global; el Saver la restaura siempre.
        override fun restore(value: List<Any>) = SpotifyDarkColorPalette

        override fun SaverScope.save(value: ColorPalette) = listOf(0, true)
    }
}

val SpotifyDarkColorPalette = ColorPalette(
    background0 = Encore.Background,
    background1 = Encore.Surface,
    background2 = Encore.SurfaceElevated,
    background3 = Encore.SurfaceHighest,
    background4 = Encore.SurfaceBlack,
    text = Encore.TextPrimary,
    textSecondary = Encore.TextSecondary,
    textDisabled = Encore.TextDisabled,
    iconButtonPlayer = Encore.TextPrimary,
    accent = Encore.Green,
    onAccent = Encore.OnGreen,
    isDark = true
)

/* Alias para no romper las ~100 referencias existentes en el resto del código.
 * Todas apuntan a la misma instancia, así que las comparaciones `===` siguen funcionando. */
val DefaultDarkColorPalette = SpotifyDarkColorPalette
val DefaultLightColorPalette = SpotifyDarkColorPalette
val PureBlackColorPalette = SpotifyDarkColorPalette
val ModernBlackColorPalette = SpotifyDarkColorPalette

/** Ignora nombre/modo/sistema: siempre Encore Dark. */
@Suppress("UNUSED_PARAMETER")
fun colorPaletteOf(
    colorPaletteName: ColorPaletteName,
    colorPaletteMode: ColorPaletteMode,
    isSystemInDarkMode: Boolean
): ColorPalette = SpotifyDarkColorPalette

/* ------------------------------------------------------------------------------------------------
 *  Paletas derivadas de la carátula (sólo para el reproductor a pantalla completa).
 *  El acento y los textos NO cambian; sólo se tiñen background1/background2 con el color
 *  dominante para el degradado "carátula → #121212".
 * ---------------------------------------------------------------------------------------------- */

fun dynamicColorPaletteOf(bitmap: Bitmap, isDark: Boolean): ColorPalette? {
    val palette = Palette
        .from(bitmap)
        .maximumColorCount(16)
        .generate()

    val swatch = palette.dominantSwatch ?: return null
    val hsl = swatch.hsl

    // Carátulas casi grises: buscar el swatch más saturado para que el degradado tenga color.
    val chosen = if (hsl[1] < 0.08f) {
        palette.swatches
            .map(Palette.Swatch::getHsl)
            .sortedByDescending(FloatArray::component2)
            .firstOrNull { it[1] != 0f }
            ?: hsl
    } else hsl

    return dynamicColorPaletteOf(chosen, isDark)
}

@Suppress("UNUSED_PARAMETER")
fun dynamicColorPaletteOf(hsl: FloatArray, isDark: Boolean): ColorPalette =
    tintedPalette(hue = hsl[0], saturation = hsl[1], lightness = hsl[2])

@Suppress("UNUSED_PARAMETER")
fun dynamicColorPaletteOf(hsl: Hsl, isDark: Boolean): ColorPalette =
    tintedPalette(hue = hsl.hue, saturation = hsl.saturation, lightness = hsl.lightness)

fun dynamicColorPaletteOf(
    accentColor: Color,
    isDark: Boolean
) = dynamicColorPaletteOf(
    hsl = accentColor.hsl,
    isDark = isDark
)

/**
 * Tono de la carátula, oscurecido para que el texto blanco siempre tenga contraste
 * (luminosidad 0.24–0.38, igual que la cabecera del reproductor de Spotify).
 */
private fun tintedPalette(hue: Float, saturation: Float, lightness: Float): ColorPalette {
    val sat = saturation.coerceIn(0f, 0.65f)
    val top = lightness.coerceIn(0.24f, 0.38f)
    return SpotifyDarkColorPalette.copy(
        background1 = Color.hsl(hue, sat, top * 0.55f),
        background2 = Color.hsl(hue, sat, top),
    )
}

/* ------------------------------------------------------------------------------------------------
 *  Colores semánticos usados por los componentes existentes
 * ---------------------------------------------------------------------------------------------- */

/** Línea de progreso del MiniPlayer: blanca como en Spotify. */
inline val ColorPalette.collapsedPlayerProgressBar: Color
    get() = text

/** Corazón "Me gusta" activo: verde. */
inline val ColorPalette.favoritesIcon: Color
    get() = accent

inline val ColorPalette.shimmer: Color
    get() = Encore.SurfaceElevated

inline val ColorPalette.primaryButton: Color
    get() = background2

inline val ColorPalette.favoritesOverlay: Color
    get() = accent.copy(alpha = 0.4f)

inline val ColorPalette.overlay: Color
    get() = Encore.SurfaceBlack.copy(alpha = 0.5f)

inline val ColorPalette.onOverlay: Color
    get() = Encore.TextPrimary

inline val ColorPalette.onOverlayShimmer: Color
    get() = Encore.SurfaceElevated

/** Se mantiene por compatibilidad: ya no cambia nada porque el tema es fijo. */
inline val ColorPalette.applyPitchBlack: ColorPalette
    get() = this

inline val ColorPalette.applyTransparency: ColorPalette
    get() = this.copy(
        background0 = Color.Transparent,
        background1 = Color.Transparent,
        background2 = Color.Transparent,
        background3 = Color.Transparent,
        background4 = Color.Transparent,
    )
