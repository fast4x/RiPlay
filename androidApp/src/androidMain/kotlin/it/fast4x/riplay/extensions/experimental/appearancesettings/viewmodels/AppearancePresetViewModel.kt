package it.fast4x.riplay.extensions.experimental.appearancesettings.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appearancesettings.repository.AppearancePresetRepository
import it.fast4x.riplay.extensions.experimental.appearancesettings.repository.AppearancePresetRepositoryImpl
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.AppearanceSettings
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetEvent
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetSource
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetUiState
import it.fast4x.riplay.extensions.experimental.appearancesettings.utils.toDomain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearancePresetViewModel(
    private val repository: AppearancePresetRepository
) : ViewModel() {

    val dao = Database.appearancePresetDao()

    private val _uiState = MutableStateFlow<PresetUiState>(PresetUiState.Loading)
    val uiState: StateFlow<PresetUiState> = _uiState.asStateFlow()

    val presetList: StateFlow<List<AppearancePreset>> = repository.getAllPresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _events = Channel<PresetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val activePresetId: StateFlow<String?> = repository.getActivePreset()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "aura"
        )

    init {
        viewModelScope.launch {
            repository.ensurePresetsMigrated() // Migra da sharedPreferences
            repository.syncRemotePresets().collect() // Aggiorna i preset remoti
            loadPresetsIntoUiState() // Carica i preset nel UiState
        }
    }

    private fun loadPresetsIntoUiState() {
        viewModelScope.launch {
            _uiState.value = PresetUiState.Success(presetList.value)
        }
    }

    fun sharePreset(preset: AppearancePreset) {
        viewModelScope.launch {
            repository.sharePreset(preset)
                .onSuccess { url -> _events.send(PresetEvent.Shared(url)) }
                .onFailure { _events.send(PresetEvent.Error(it.message ?: "Errore condivisione")) }
        }
    }
    suspend fun getCurrentActivePreset(): AppearancePreset {
        val id = activePresetId.value
        return dao.getPresetById(id.toString())?.toDomain() ?: AppearancePreset(
            id = "aura_fallback",
            name = "Aura (Default)",
            author = "Fast4x",
            source = PresetSource.BUILTIN,
            settings = AppearanceSettings.Aura
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppearancePresetViewModel(
                        repository  = AppearancePresetRepositoryImpl(context)
                    ) as T
            }
    }

}