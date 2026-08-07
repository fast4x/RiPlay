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

    @Volatile
    private var isInitialized = false

    suspend fun initialize() {
        try {
            dao.ensureBaselineRowExists()
            Timber.d("AppSettingsManager inizializzazione AppSettings dal DB")
            val json = dao.getSettings().first()
            if (json != "{}") {
                _activeSettings.value = DbSettingsJson.decodeFromString<AppSettings>(json)
            }
        } catch (e: Exception) {
            Timber.e(e, "AppSettingsManager Errore inizializzazione AppSettings dal DB, uso i default")

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
        val settings = DbSettingsJson.encodeToString(newSettings)
        Timber.d("AppSettingsManager updateSettings newSettings: $settings")
        _activeSettings.value = newSettings // Aggiorna la RAM istantaneamente
        dao.updateSettings(settings) // Aggiorna il DB
    }

// Da usare in debug per intercettare la provenienza della chiamata
//    suspend fun updateSettings(newSettings: AppSettings) {
//        val json = DbSettingsJson.encodeToString(newSettings)
//
//        // LOG PARANOICO: Intercetta TUTTI i salvataggi
//        Timber.e("BREAK!!! SALVATAGGIO INTERCETTATO !!! lunghezza json = ${json.length} JSON: $json")
//
//        // L'eccezione finta per avere lo stack trace CHIUDI
//
//        //println("BREAK!!! STACK TRACE DEL SALVATAGGIO !!!")
//        //Exception("BREAK!!! Chi sta salvando?").printStackTrace()
//
//        _activeSettings.value = newSettings
//        dao.updateSettings(json)
//    }


}