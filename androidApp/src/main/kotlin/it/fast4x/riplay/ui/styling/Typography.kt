package it.fast4x.riplay.ui.styling

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.utils.appContext

@Immutable
data class Typography(
    val xxxs: TextStyle,
    val xxs: TextStyle,
    val xs: TextStyle,
    val s: TextStyle,
    val m: TextStyle,
    val l: TextStyle,
    val xl: TextStyle,
    val xxl: TextStyle,
    val xxxl: TextStyle,
    val xlxl: TextStyle,
) {
    fun copy(color: Color) = Typography(
        xxxs = xxxs.copy(color = color),
        xxs = xxs.copy(color = color),
        xs = xs.copy(color = color),
        s = s.copy(color = color),
        m = m.copy(color = color),
        l = l.copy(color = color),
        xl = xl.copy(color = color),
        xxl = xxl.copy(color = color),
        xxxl = xxxl.copy(color = color),
        xlxl = xlxl.copy(color = color),
    )

    companion object : Saver<Typography, List<Any>> {
        override fun restore(value: List<Any>) = typographyOf(
            color = Color((value[0] as Long).toULong()),
            useSystemFont = false,
            applyFontPadding = false,
            fontType = FontType.Poppins
        )

        override fun SaverScope.save(value: Typography) =
            listOf(
                value.xxs.color.value.toLong(),
                false,
                false,
                FontType.Poppins
            )
    }
}

/* ------------------------------------------------------------------------------------------------
 *  Fuente Encore
 *
 *  Circular / Spotify Mix son propietarias. La alternativa libre más cercana es Figtree
 *  (OFL, Google Fonts). Si copias estos 4 archivos en androidApp/src/main/res/font/:
 *      figtree_regular.ttf  figtree_medium.ttf  figtree_semibold.ttf  figtree_bold.ttf
 *  se usan automáticamente. Si no están, se usa Poppins (ya incluida en el proyecto),
 *  que también es geométrica. No hace falta tocar código en ningún caso.
 * ---------------------------------------------------------------------------------------------- */

private val encoreFontFamily: FontFamily by lazy { figtreeOrNull() ?: poppins() }

@SuppressLint("DiscouragedApi")
private fun figtreeOrNull(): FontFamily? = runCatching {
    val ctx = appContext()
    fun id(name: String) = ctx.resources.getIdentifier(name, "font", ctx.packageName)
    val regular = id("figtree_regular")
    val medium = id("figtree_medium")
    val semiBold = id("figtree_semibold")
    val bold = id("figtree_bold")
    if (listOf(regular, medium, semiBold, bold).any { it == 0 }) return@runCatching null
    FontFamily(
        Font(regular, FontWeight.Light),
        Font(regular, FontWeight.Normal),
        Font(medium, FontWeight.Medium),
        Font(semiBold, FontWeight.SemiBold),
        Font(bold, FontWeight.Bold),
        Font(bold, FontWeight.ExtraBold),
        Font(bold, FontWeight.Black),
    )
}.getOrNull()

private fun poppins() = FontFamily(
    Font(R.font.poppins_w300, FontWeight.Light),
    Font(R.font.poppins_w400, FontWeight.Normal),
    Font(R.font.poppins_w500, FontWeight.Medium),
    Font(R.font.poppins_w600, FontWeight.SemiBold),
    Font(R.font.poppins_w700, FontWeight.Bold),
    Font(R.font.poppins_w700, FontWeight.ExtraBold),
    Font(R.font.poppins_w700, FontWeight.Black),
)

/**
 * Escala compacta tipo Encore, calibrada para un Pixel 9a (1080 × 2424 px, ≈411 dp de ancho con el tamaño de pantalla por defecto).
 * Los tamaños conservan la semántica de RiPlay (xxs…xlxl) para no romper ninguna pantalla:
 *
 *  xxxs  10sp  etiquetas diminutas (badges, "E" de explícito)
 *  xxs   12sp  metadatos, tiempos de la barra de progreso, labels de la barra inferior
 *  xs    14sp  subtítulos / artista (gris B3B3B3 en los componentes)
 *  s     16sp  título de canción en listas, cuerpo
 *  m     18sp  títulos de tarjetas
 *  l     20sp  títulos de sección pequeños
 *  xl    22sp  títulos de sección ("Hecho para ti") – Bold
 *  xxl   24sp  título de canción en el reproductor – Bold
 *  xxxl  32sp  cabeceras de pantalla ("Tu Biblioteca") – Bold
 *  xlxl  28sp  cabeceras de álbum/playlist – Bold
 *
 * Los parámetros se mantienen para compatibilidad con MainActivity, pero se ignoran:
 * la tipografía es fija.
 */
@Suppress("UNUSED_PARAMETER")
fun typographyOf(
    color: Color,
    useSystemFont: Boolean,
    applyFontPadding: Boolean,
    fontType: FontType
): Typography {
    val base = TextStyle(
        fontFamily = encoreFontFamily,
        fontWeight = FontWeight.Normal,
        color = color,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        ),
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    )

    // Títulos: Bold, tracking negativo y interlineado apretado.
    fun title(size: Int, tracking: Float) = base.copy(
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = tracking.sp,
        lineHeight = 1.15.em
    )

    fun body(size: Int, tracking: Float = 0f) = base.copy(
        fontSize = size.sp,
        letterSpacing = tracking.sp,
        lineHeight = 1.3.em
    )

    return Typography(
        xxxs = body(10, 0.1f),
        xxs = body(12, 0.05f),
        xs = body(14),
        s = body(16, -0.1f),
        m = body(18, -0.2f),
        l = title(20, -0.3f),
        xl = title(22, -0.5f),
        xxl = title(24, -0.5f),
        xxxl = title(32, -0.8f),
        xlxl = title(28, -0.6f),
    )
}
