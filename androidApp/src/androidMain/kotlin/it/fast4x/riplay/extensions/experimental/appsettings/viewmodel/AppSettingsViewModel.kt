package it.fast4x.riplay.extensions.experimental.appsettings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appsettings.AppSettingsManager
import it.fast4x.riplay.extensions.experimental.appsettings.models.AppSettings
import it.fast4x.riplay.utils.DbSettingsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private var isInitialized = false

    private val _activeSettings = MutableStateFlow(AppSettings())
    val activeSettings: StateFlow<AppSettings> = _activeSettings.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = daoApp.getSettings().first()
                Timber.d("AppSettingsViewModel init: Loading settings from DB $settings")
                if (settings.isNotEmpty()) {
                    _activeSettings.value = DbSettingsJson.decodeFromString<AppSettings>(settings)
                    Timber.d("AppSettingsViewModel init: Successfully loaded settings")
                } else {
                    Timber.w("AppSettingsViewModel init: No settings found in DB, initialized with defaults falling back")
                }
            } catch (e: Exception) {
                Timber.e(e, "AppSettingsViewModel init: Error loading app settings from DB")
            } finally {
                isInitialized = true
            }
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isInitialized) {
                Timber.w("AppSettingsViewModel updateSettings: chiamato prima che l'init finisse. Ignoro la scrittura per non corrompere il DB.")
                return@launch
            }
            Timber.d("AppSettingsViewModel updateSettings: Updating settings $settings")

            _activeSettings.value = settings
            daoApp.updateSettings(DbSettingsJson.encodeToString(settings))
            //AppSettingsManager().current = settings
        }
    }
}
