package it.fast4x.riplay.extensions.experimental.appearancesettings

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.AppearanceSettings
import it.fast4x.riplay.extensions.experimental.appearancesettings.repository.AppearancePresetRepository
import it.fast4x.riplay.extensions.experimental.appearancesettings.repository.AppearancePresetRepositoryImpl
import it.fast4x.riplay.utils.DbSettingsJson
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

class AppearanceSettingsManager {

    val daoAppearance = Database.appearancePresetDao()

    val appearanncePresetRepository: AppearancePresetRepository =
        AppearancePresetRepositoryImpl(appContext())

    private val _activeSettings = MutableStateFlow(AppearanceSettings())
    val activeSettings: StateFlow<AppearanceSettings> = _activeSettings.asStateFlow()

    private val _activeId = MutableStateFlow("aura")
    val activeId: StateFlow<String> = _activeId.asStateFlow()

    suspend fun initialize() {

        try {
            // Verifico se presente almeno aura il preset di default
            val defaultPreset = daoAppearance.getPresetById("aura")
            if (defaultPreset == null) {
                // Se non lo è lo inserisco nel DB e migro il preset esistente da sharedPreferences
                appearanncePresetRepository.ensurePresetsMigrated()
                Timber.d("AppearanceSettingsManager init: Default presets inserted and migrated user preset")
            } else {
                Timber.d("AppearanceSettingsManager init: Default preset aura already exists")
            }
        } catch (e: Exception) {
            Timber.e(e, "AppearanceSettingsManager init: Error checking default preset aura")
        }

        try {
            // Leggo l'ID e ASPETTO il risultato
            val id = daoAppearance.getActivePreset().first() ?: "aura"
            _activeId.value = id
            Timber.d("AppearanceSettingsManager init: Loaded active ID -> $id")

            val entity = daoAppearance.getPresetById(id)
            if (entity != null) {
                val settings = DbSettingsJson.decodeFromString<AppearanceSettings>(entity.settingsJson)
                Timber.d("AppearanceSettingsManager init: Successfully loaded settings for $id")
                _activeSettings.value = settings
            } else {
                Timber.w("AppearanceSettingsManager init: Preset $id not found in DB, falling back to defaults")
            }
        } catch (e: Exception) {
            Timber.e(e, "AppearanceSettingsManager init: Error loading appearance settings from DB")
        }
    }

    suspend fun applyPreset(preset: AppearancePreset) {
        daoAppearance.setActivePreset(preset.id)
        _activeSettings.value = preset.settings
        _activeId.value = preset.id
        Timber.d("AppearanceSettingsManager applyPreset: Applied preset -> ${preset.id}")
    }

    suspend fun updatePreset(settings: AppearanceSettings) {
        val currentId = _activeId.value
        Timber.d("updatePreset: Saving settings for -> $currentId")

        daoAppearance.updatePresetSettings(currentId, DbSettingsJson.encodeToString(settings))
        _activeSettings.value = settings
    }

}