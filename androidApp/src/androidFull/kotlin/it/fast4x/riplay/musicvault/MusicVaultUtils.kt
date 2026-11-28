package it.fast4x.riplay.musicvault

import android.content.Context
import it.fast4x.riplay.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * Music Vault con delega al plugin esterno (protocollo "MusicVault") se installato.
 */
fun initializeMusicVault(coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch(Dispatchers.IO) {
        val settings = (context as? MainApplication)?.appSettingsManager?.activeSettings?.value
        if (settings == null || !settings.musicVaultEnabled) {
            Timber.d("MusicVault disabilitato: skip rilevamento plugin")
            return@launch
        }

        val plugins = VaultPluginClient.discoverPlugins(context)
        if (plugins.isEmpty()) {
            Timber.w("MusicVault: nessuna app MusicVault compatibile installata")
        } else {
            Timber.i("MusicVault plugin disponibili: ${plugins.joinToString { "${it.label} (${it.packageName})" }}")
        }
    }
}

/** true se sul dispositivo esiste un plugin compatibile con il protocollo. */
fun isMusicVaultPluginAvailable(context: Context): Boolean =
    VaultPluginClient.isPluginAvailable(context)
