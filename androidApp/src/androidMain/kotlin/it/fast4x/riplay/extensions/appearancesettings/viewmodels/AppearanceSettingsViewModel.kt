package it.fast4x.riplay.extensions.appearancesettings.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.appearancesettings.models.AppearancePreset
import it.fast4x.riplay.extensions.appearancesettings.models.AppearanceSettings
import it.fast4x.riplay.utils.DbSettingsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class AppearanceSettingsViewModel(application: Application) : AndroidViewModel(application),
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppearanceSettingsViewModel::class.java)) {
            val application = getApplication<Application>()
            @Suppress("UNCHECKED_CAST")
            return AppearanceSettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val daoAppearance = Database.appearancePresetDao()

    private val _activeSettings = MutableStateFlow(AppearanceSettings())
    val activeSettings: StateFlow<AppearanceSettings> = _activeSettings.asStateFlow()

    private val _activeId = MutableStateFlow("aura")
    val activeId: StateFlow<String> = _activeId.asStateFlow()



    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Leggo l'ID e ASPETTO il risultato
                val id = daoAppearance.getActivePreset().first() ?: "aura"
                _activeId.value = id
                Timber.d("AppearanceSettingsViewModel init: Loaded active ID -> $id")

                val entity = daoAppearance.getPresetById(id)
                if (entity != null) {
                    val settings = DbSettingsJson.decodeFromString<AppearanceSettings>(entity.settingsJson)
                    Timber.d("AppearanceSettingsViewModel init: Successfully loaded settings for $id")
                    _activeSettings.value = settings
                } else {
                    Timber.w("AppearanceSettingsViewModel init: Preset $id not found in DB, falling back to defaults")
                }
            } catch (e: Exception) {
                Timber.e(e, "AppearanceSettingsViewModel init: Error loading appearance settings from DB")
            }
        }
    }

    fun applyPreset(preset: AppearancePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            daoAppearance.setActivePreset(preset.id)
            _activeSettings.value = preset.settings
            _activeId.value = preset.id
            Timber.d("AppearanceSettingsViewModel applyPreset: Applied preset -> ${preset.id}")
        }
    }

    fun updatePreset(settings: AppearanceSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentId = _activeId.value
            Timber.d("updatePreset: Saving settings for -> $currentId")

            daoAppearance.updatePresetSettings(currentId, DbSettingsJson.encodeToString(settings))
            _activeSettings.value = settings
        }
    }
}