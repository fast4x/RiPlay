package it.fast4x.riplay.extensions.experimental.webdavlibrary

import android.content.Context
import androidx.core.content.edit
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.extensions.preferences.lastDbModified
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.appContext

object SyncMetadataManager {
    private val context = (appContext() as MainApplication)
    private val preferences = context.preferences

    fun getLastDbModified(): Long {
        return preferences.getLong(lastDbModified, 0L)
    }

    fun updateLastDbModified() {
        // Uso apply() perchè è asincrono e non blocca il thread del database
        preferences.edit { putLong(lastDbModified, System.currentTimeMillis()) }
    }
}