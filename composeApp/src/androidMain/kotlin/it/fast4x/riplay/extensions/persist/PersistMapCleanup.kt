package it.fast4x.riplay.extensions.persist

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun PersistMapCleanup(tagPrefix: String) {
    val context = LocalContext.current
    val persistMap = LocalPersistMap.current

    DisposableEffect(context) {
        onDispose {
            if (context.findOwner<Activity>()?.isChangingConfigurations == false) {
                context.persistMap?.keys?.removeAll { it.startsWith(tagPrefix) }
            }
        }
    }
    DisposableEffect(persistMap) {
        onDispose {
            if (context.findActivityNullable()?.isChangingConfigurations == false)
                persistMap?.keys?.removeAll { it.startsWith(tagPrefix) }
        }
    }
}

fun Context.findActivityNullable(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}