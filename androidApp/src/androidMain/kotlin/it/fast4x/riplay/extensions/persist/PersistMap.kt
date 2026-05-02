package it.fast4x.riplay.extensions.persist

import android.util.Log
import androidx.compose.runtime.compositionLocalOf

typealias PersistMap = HashMap<String, Any?>

val LocalPersistMap = compositionLocalOf<PersistMap?> {
    Log.e("PersistMap", "Tried to reference uninitialized PersistMap, stacktrace:")
    runCatching { error("Stack:") }.exceptionOrNull()?.printStackTrace()
    null
}