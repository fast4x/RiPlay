package it.fast4x.riplay.extensions.experimental.appearancepreset

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetEvent
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetUiState
import it.fast4x.riplay.extensions.preferences.activeAppearancePresetIdKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearancePresetViewModel(
    private val repository: AppearancePresetRepository,
    private val preferences: AppearancePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<PresetUiState>(PresetUiState.Loading)
    val uiState: StateFlow<PresetUiState> = _uiState.asStateFlow()

    private val _events = Channel<PresetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val activePresetId: StateFlow<String?> = preferences.activePresetIdFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appContext().preferences.getString(activeAppearancePresetIdKey, null)
        )

    init { loadPresets() }

    fun applyPreset(preset: AppearancePreset) {
        viewModelScope.launch {
            runCatching { preferences.applyFrom(preset.settings, preset.id) }
                .onSuccess { _events.send(PresetEvent.Applied(preset.name)) }
                .onFailure { _events.send(PresetEvent.Error(it.message ?: "Errore")) }
        }
    }

    fun sharePreset(preset: AppearancePreset) {
        viewModelScope.launch {
            repository.sharePreset(preset)
                .onSuccess { url -> _events.send(PresetEvent.Shared(url)) }
                .onFailure { _events.send(PresetEvent.Error(it.message ?: "Errore condivisione")) }
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            repository.remotePresets()
                .catch { e ->
                    // Fallback ai soli preset locali in caso di errore rete
                    _uiState.value = PresetUiState.Success(repository.localPresets())
                }
                .collect { remote ->
                    val all = (repository.localPresets() + remote)
                        .distinctBy { it.id }
                    _uiState.value = PresetUiState.Success(all)
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppearancePresetViewModel(
                        repository  = AppearancePresetRepositoryImpl(context),
                        preferences = AppearancePreferences.getInstance(context)
                    ) as T
            }
    }

}


