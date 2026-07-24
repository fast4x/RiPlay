package it.fast4x.riplay.extensions.appearancesettings.repository

import it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset
import kotlinx.coroutines.flow.Flow

interface AppearancePresetRepository {

    fun getActivePreset(): Flow<String?>
    fun getAllPresets(): Flow<List<it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset>>

    fun localPresets(): List<it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset>

    /** Preset condivisi dalla community — Flow per aggiornamenti in tempo reale */
    fun syncRemotePresets(): Flow<Unit>

    /** Carica un preset tramite share-URL o codice */
    suspend fun loadSharedPreset(shareUrl: String): Result<it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset>

    /** Pubblica un preset condividendolo con altri utenti */
    suspend fun sharePreset(preset: it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset): Result<String>

    /** Migra il preset esistente da sharedPreferences e lo inserisce nel database */
    suspend fun ensurePresetsMigrated() // Migra il preset esistente da sharedPreferences e lo inserisce nel database
}