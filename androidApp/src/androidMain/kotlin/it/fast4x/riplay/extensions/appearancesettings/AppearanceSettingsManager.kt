package it.fast4x.riplay.extensions.appearancesettings

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset
import it.fast4x.riplay.extensions.appearancesettings.models.AppearanceSettings
import it.fast4x.riplay.extensions.appearancesettings.repository.AppearancePresetRepository
import it.fast4x.riplay.extensions.appearancesettings.repository.AppearancePresetRepositoryImpl
import it.fast4x.riplay.extensions.appearancesettings.utils.toEntity
import it.fast4x.riplay.utils.DbSettingsJson
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID

class AppearanceSettingsManager {

    val daoAppearance = Database.appearancePresetDao()
    val daoApp = Database.appSettingsDao()

    val appearanncePresetRepository: AppearancePresetRepository =
        AppearancePresetRepositoryImpl(
            appContext()
        )

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

            // L'utente ha delle personalizzazioni attive?
            val customAppearanceSettings = daoApp.getActiveAppearanceSettings().first()

            if (customAppearanceSettings != "{}" && !customAppearanceSettings.isEmpty()) {
                _activeSettings.value = DbSettingsJson.decodeFromString<AppearanceSettings>(customAppearanceSettings)
                Timber.d("AppearanceSettingsManager init: Successfully loaded CUSTOM settings for $id")
            } else {
                val entity = daoAppearance.getPresetById(id)
                if (entity != null) {
                    val settings =
                        DbSettingsJson.decodeFromString<AppearanceSettings>(entity.settingsJson)
                    Timber.d("AppearanceSettingsManager init: Successfully loaded settings for $id")
                    _activeSettings.value = settings
                } else {
                    Timber.w("AppearanceSettingsManager init: Preset $id not found in DB, falling back to defaults")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "AppearanceSettingsManager init: Error loading appearance settings from DB")
        }
    }

    suspend fun applyPreset(preset: AppearancePreset) {
        daoAppearance.setActivePreset(preset.id)
        _activeSettings.value = preset.settings
        _activeId.value = preset.id

        // L'utente ha scelto di tornare al template di default, quindi resetto le personalizzazioni
        daoApp.updateActiveAppearanceSettings("{}")

        Timber.d("AppearanceSettingsManager applyPreset: Applied preset -> ${preset.id}")
    }

    suspend fun updatePreset(settings: AppearanceSettings) {
        val currentId = _activeId.value
        Timber.d("AppearanceSettingsManager updatePreset: saved current preset id -> $currentId")

        // Sovrascrive le impostazioni di default del preset
        //daoAppearance.updatePresetSettings(currentId, DbSettingsJson.encodeToString(settings))

        // Aggiorno subito la ram
        _activeSettings.value = settings
        // Salva le impostazioni personalizzate nella tabella app_settings senza sovrascrivere il preset corrente
        daoApp.updateActiveAppearanceSettings(DbSettingsJson.encodeToString(settings))

    }

    suspend fun importAndApplyPreset(preset: AppearancePreset) {

        var presetToImport = preset

        // Prendo il preset corrente, se l'ID importato è un id di default cambio id per non sovrascriverlo,
        if (isBuiltInPreset(preset.id)) {
            presetToImport = preset.copy(
                id = UUID.randomUUID().toString(), // Nuovo ID univoco
                name = "${presetToImport.name} (Imported)"
            )
        }

        daoAppearance.insertPreset(presetToImport.toEntity())
        daoAppearance.setActivePreset(presetToImport.id)
        _activeSettings.value = presetToImport.settings

        // L'utente ha scelto di importare il template, quindi resetto le personalizzazioni
        daoApp.updateActiveAppearanceSettings("{}")
    }

    suspend fun deletePreset(id: String) {
        val currentId = _activeId.value
        if (currentId == id || isBuiltInPreset(id)) return

        daoAppearance.deletePreset(id)
        daoApp.updateActiveAppearanceSettings("{}")

    }

    // Sono i preset di default, al momento li gestisco in modo semplice, in futuro sarà da migliorare
    private fun isBuiltInPreset(id: String): Boolean {
        return listOf("aura", "deck", "zen", "noir", "prism", "groove").contains(id)
    }

}