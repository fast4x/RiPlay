package it.fast4x.riplay.extensions.experimental.appearancepreset.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.ThemeJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    // Il preset in memoria da usare nella ui
    private val _activeSettings = MutableStateFlow(AppearanceSettings())
    val activeSettings: StateFlow<AppearanceSettings> = _activeSettings.asStateFlow()

    init {
        // Al primo avvio, leggo dal DB qual è il tema attivo e lo carico in memoria
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = daoAppearance.getActivePreset().first() ?: "aura" // Fallback in caso di errore

            val entity = daoAppearance.getPresetById(activeId)
            if (entity != null) {
                // Deserializzo il JSON e lo carico in memoria
                val settings = ThemeJson.decodeFromString<AppearanceSettings>(entity.settingsJson)
                _activeSettings.value = settings
            }
        }
    }

    // Questo viene chiamato quando l'utente clicca su un preset nella lista
    fun applyPreset(preset: AppearancePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            // Aggiorno il preset attivo nel DB
            daoAppearance.setActivePreset(preset.id)

            // Carico il nuovo preset in memoria
            _activeSettings.value = preset.settings
        }
    }
}