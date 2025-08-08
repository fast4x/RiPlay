package it.fast4x.riplay.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


fun getScreenOrientation(context: Context): Int {
    return when (context.resources.configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> Configuration.ORIENTATION_PORTRAIT
        Configuration.ORIENTATION_LANDSCAPE -> Configuration.ORIENTATION_LANDSCAPE
        else -> 0
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
)

fun getScreenDimensions(): ScreenDimensions {
    return ScreenDimensions(
        Resources.getSystem().displayMetrics.widthPixels,
        Resources.getSystem().displayMetrics.heightPixels,
        Resources.getSystem().displayMetrics.density
    )


}