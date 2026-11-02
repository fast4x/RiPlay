package it.fast4x.riplay.musicvault

import android.content.Context
import it.fast4x.chaquopy.ChaquopySupport
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 *  1. Python parte SOLO se Music Vault è stato attivato dall'utente
 *     (risparmia anche memoria/tempo di avvio su tutti gli altri dispositivi);
 *  2. l'avvio è protetto tramite ChaquopySupport.safeStart(): in caso di
 *     fallimento l'app parte comunque, semplicemente senza Music Vault.
 */
fun initializeMusicVault(coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch(Dispatchers.IO) {
        val settings = (context as? MainApplication)?.appSettingsManager?.activeSettings?.value
        if (settings == null || !settings.musicVaultEnabled || !settings.musicVaultDisclaimerAccepted) {
            Timber.d("MusicVault disabilitato: skip avvio runtime Python")
            return@launch
        }

        val error = ChaquopySupport.safeStart(context)
        if (error != null) {
            Timber.e("MusicVault: $error")
        } else {
            Timber.i("MusicVault: runtime Python pronto")
        }
    }
}

fun checkAndStartMusicVault() {
    val context = appContext()

    val appSettingsManager = (context as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value

    if (appSettings.musicVaultEnabled && appSettings.musicVaultDisclaimerAccepted) {
        val (pyVersion, ytdlpVersion, ready) = engine.testAndStartChaquopy(context)
        Timber.d("Chaquopy python=$pyVersion yt-dlp=$ytdlpVersion ready=$ready")

        if (!ready) {
            // Feedback chiaro all'utente invece del crash silenzioso.
            // Su fallimento il secondo elemento della Triple contiene la
            // descrizione del problema (es. "Motore Python non supportato
            // su questo dispositivo (ABI: ...)").
            val reason = when {
                ytdlpVersion.contains("non ") -> ytdlpVersion
                pyVersion.isBlank()           -> "runtime Python non avviabile"
                else                          -> "yt-dlp non installato"
            }
            SmartMessage(
                message = "Music Vault: $reason",
                type = PopupType.Error,
                durationLong = true,
                context = context
            )
        }
    }
}
