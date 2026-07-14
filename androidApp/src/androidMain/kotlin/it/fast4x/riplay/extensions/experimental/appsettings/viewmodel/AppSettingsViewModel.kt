package it.fast4x.riplay.extensions.experimental.appsettings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appsettings.models.AppSettings
import it.fast4x.riplay.utils.DbSettingsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AppSettingsViewModel(application: Application) : AndroidViewModel(application),
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppSettingsViewModel::class.java)) {
            val application = getApplication<Application>()
            @Suppress("UNCHECKED_CAST")
            return AppSettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val daoApp = Database.appSettingsDao()

    private val _activeSettings = MutableStateFlow(AppSettings())
    val activeSettings: StateFlow<AppSettings> = _activeSettings.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = daoApp.getSettings()?.first()
                if (settings != null) {
                    _activeSettings.value = DbSettingsJson.decodeFromString<AppSettings>(settings.toString())
                    Timber.d("AppSettingsViewModel init: Successfully loaded settings")
                } else {
                    Timber.w("AppSettingsViewModel init: Settings not found in DB, falling back to defaults")
                }
            } catch (e: Exception) {
                Timber.e(e, "AppSettingsViewModel init: Error loading app settings from DB")
            }
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            daoApp.updateSettings(DbSettingsJson.encodeToString(settings))
            _activeSettings.value = settings
        }
    }
}
