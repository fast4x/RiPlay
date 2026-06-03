package it.fast4x.riplay.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val isLandscape
    @Composable
    @ReadOnlyComposable
    get() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * Controlla se l'app sta girando su una Android TV.
 * Combina il controllo del UiModeManager e la presenza del feature Leanback
 * per coprire anche TV Box non ufficialmente certificati.
 */
@Composable
fun isTvMode(): Boolean {
    val context = LocalContext.current

    // Metodo 1: Controllo tramite UiModeManager (Standard ufficiale Android TV)
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isUiModeTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    // Metodo 2: Controllo tramite PackageManager (Verifica se il dispositivo supporta l'interfaccia Leanback)
    val isLeanbackFeature = context.packageManager?.hasSystemFeature(PackageManager.FEATURE_LEANBACK) == true

    // Se uno dei due è true, siamo su una TV
    return isUiModeTv || isLeanbackFeature
}

fun getScreenOrientation(context: Context): Int {
    return when (context.resources.configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> Configuration.ORIENTATION_PORTRAIT
        Configuration.ORIENTATION_LANDSCAPE -> Configuration.ORIENTATION_LANDSCAPE
        else -> Configuration.ORIENTATION_UNDEFINED
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun getScreenRotation(context: Context): String {
    return when (getRotation(context)) {
        Surface.ROTATION_0 -> "Portrait"
        Surface.ROTATION_90 -> "Landscape Right"
        Surface.ROTATION_180 -> "Upside Down"
        Surface.ROTATION_270 -> "Landscape Left"
        else -> "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun getRotation(context: Context): Int {
    return context.display.rotation
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun getRotation(): Int {
    val context = LocalContext.current
    return context.display.rotation
}

class ScreenDimensions (
    val width: Int,
    val height: Int,
    val density: Float,
    val metrics: DisplayMetrics
)

fun getScreenDimensions(): ScreenDimensions {
    return ScreenDimensions(
        Resources.getSystem().displayMetrics.widthPixels,
        Resources.getSystem().displayMetrics.heightPixels,
        Resources.getSystem().displayMetrics.density,
        Resources.getSystem().displayMetrics
    )


}

@Suppress("DEPRECATION")
fun getScreenRealSize(context: Context): Pair<Int, Int> {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        Pair(bounds.width(), bounds.height())
    } else {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
}