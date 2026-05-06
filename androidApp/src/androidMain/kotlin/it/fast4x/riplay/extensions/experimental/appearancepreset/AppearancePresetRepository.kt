package it.fast4x.riplay.extensions.experimental.appearancepreset

import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import kotlinx.coroutines.flow.Flow


// ── Repository  ───────────────────────────────────────

interface AppearancePresetRepository {
    /** Preset built-in dell'app */
    fun localPresets(): List<AppearancePreset>

    /** Preset condivisi dalla community — Flow per aggiornamenti in tempo reale */
    fun remotePresets(): Flow<List<AppearancePreset>>

    /** Carica un preset tramite share-URL o codice */
    suspend fun loadSharedPreset(shareUrl: String): Result<AppearancePreset>

    /** Pubblica un preset condividendolo con altri utenti */
    suspend fun sharePreset(preset: AppearancePreset): Result<String>
}