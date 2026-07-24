package it.fast4x.riplay.extensions.appsettings

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.appsettings.models.AppSettings
import it.fast4x.riplay.utils.DbSettingsJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

class AppSettingsManager {

    val dao = Database.appSettingsDao()

    private val _activeSettings = MutableStateFlow(AppSettings())
    val activeSettings: StateFlow<AppSettings> = _activeSettings.asStateFlow()

    private var isInitialized = false

    suspend fun initialize() {
        try {
            val json = dao.getSettings().first()
            if (json.isNotEmpty()) {
                _activeSettings.value = DbSettingsJson.decodeFromString<AppSettings>(json)
            }
        } catch (e: Exception) {
            Timber.e(e, "AppSettingsManager Errore inizializzazione AppSettings dal DB, uso i default")
            dao.updateSettings(DbSettingsJson.encodeToString(AppSettings()))
        } finally {
            isInitialized = true
        }
    }

    suspend fun waitForInitialization(): AppSettings {
        // Se l'Application ha già finito, ritorna subito i dati
        if (isInitialized) {
            return _activeSettings.value
        }

        // Se l'Application è ancora in esecuzione, aspettiamo il primo aggiornamento del Flow
        return _activeSettings.first { isInitialized }
    }

    suspend fun updateSettings(newSettings: AppSettings) {
        _activeSettings.value = newSettings // Aggiorna la RAM istantaneamente
        dao.updateSettings(DbSettingsJson.encodeToString(newSettings)) // Aggiorna il DB
    }


}