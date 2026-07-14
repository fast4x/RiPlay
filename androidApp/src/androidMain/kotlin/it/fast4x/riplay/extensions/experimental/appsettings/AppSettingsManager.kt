package it.fast4x.riplay.extensions.experimental.appsettings

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appsettings.models.AppSettings
import it.fast4x.riplay.utils.DbSettingsJson

class AppSettingsManager {

    val dao = Database.appSettingsDao()

    // Lo stato in RAM
    var current: AppSettings = AppSettings()

    // Inizializzazione all'avvio dell'app
    suspend fun initialize() {
        val json = dao.getSettings()
        if (json != null) {
            current = DbSettingsJson.decodeFromString<AppSettings>(json)
        }
    }

    // Aggiornamento dell'intero oggetto
    suspend fun updateSettings(newSettings: AppSettings) {
        // Aggiorno subito in memoria
        current = newSettings

        // Serializzo in JSON
        val json = DbSettingsJson.encodeToString(newSettings)

        // Salvo nel DB
        dao.updateSettings(json)
    }

}