package it.fast4x.riplay.extensions.audiotagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.extensions.audiotag.AudioTagViewModel
import it.fast4x.riplay.extensions.audiotagger.api.AudioTagApiService
import it.fast4x.riplay.extensions.audiotagger.models.ApiInfoResponse
import it.fast4x.riplay.extensions.audiotagger.models.ApiStatsResponse
import it.fast4x.riplay.extensions.audiotagger.models.GetOfflineStreamResultResponse
import it.fast4x.riplay.extensions.audiotagger.models.GetResultResponse
import it.fast4x.riplay.extensions.audiotagger.models.IdentifyOfflineStreamResponse
import it.fast4x.riplay.extensions.audiotagger.models.IdentifyResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioTaggerViewModel : ViewModel(), ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioTaggerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioTaggerViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val apiService = AudioTagApiService()

    // Stati per le informazioni API
    private val _apiInfoState = MutableStateFlow<ApiState<ApiInfoResponse>>(ApiState.Idle)
    val apiInfoState: StateFlow<ApiState<ApiInfoResponse>> = _apiInfoState.asStateFlow()

    // Stati per le statistiche account
    private val _accountStatsState = MutableStateFlow<ApiState<ApiStatsResponse>>(ApiState.Idle)
    val accountStatsState: StateFlow<ApiState<ApiStatsResponse>> = _accountStatsState.asStateFlow()

    // Stati per l'identificazione file
    private val _identifyState = MutableStateFlow<ApiState<IdentifyResponse>>(ApiState.Idle)
    val identifyState: StateFlow<ApiState<IdentifyResponse>> = _identifyState.asStateFlow()

    // Stati per i risultati di identificazione
    private val _resultState = MutableStateFlow<ApiState<GetResultResponse>>(ApiState.Idle)
    val resultState: StateFlow<ApiState<GetResultResponse>> = _resultState.asStateFlow()

    // Stati per l'identificazione file remoto
    private val _identifyRemoteState = MutableStateFlow<ApiState<IdentifyOfflineStreamResponse>>(ApiState.Idle)
    val identifyRemoteState: StateFlow<ApiState<IdentifyOfflineStreamResponse>> = _identifyRemoteState.asStateFlow()

    // Stati per i risultati di identificazione remota
    val _remoteResultState = MutableStateFlow<ApiState<GetOfflineStreamResultResponse>>(ApiState.Idle)
    val remoteResultState: StateFlow<ApiState<GetOfflineStreamResultResponse>> = _remoteResultState.asStateFlow()

    // Chiamata per ottenere informazioni API
    fun getApiInfo(apiKey: String) {
        viewModelScope.launch {
            _apiInfoState.value = ApiState.Loading
            try {
                val response = apiService.getApiInfo(apiKey)
                _apiInfoState.value = if (response.success) {
                    ApiState.Success(response)
                } else {
                    ApiState.Error(response.error ?: "Errore sconosciuto")
                }
            } catch (e: Exception) {
                _apiInfoState.value = ApiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    // Chiamata per ottenere statistiche account
    fun getAccountStats(apiKey: String) {
        viewModelScope.launch {
            _accountStatsState.value = ApiState.Loading
            try {
                val response = apiService.getAccountStats(apiKey)
                _accountStatsState.value = if (response.success) {
                    ApiState.Success(response)
                } else {
                    ApiState.Error(response.error ?: "Errore sconosciuto")
                }
            } catch (e: Exception) {
                _accountStatsState.value = ApiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    // Chiamata per identificare un file audio
    fun identifyAudioFile(apiKey: String, audioFile: File, startTime: Int = 0, timeLen: Int? = null) {
        viewModelScope.launch {
            _identifyState.value = ApiState.Loading
            try {
                val response = apiService.identifyAudioFile(apiKey, audioFile, startTime, timeLen)
                _identifyState.value = if (response.success) {
                    ApiState.Success(response)
                } else {
                    ApiState.Error(response.error ?: "Errore sconosciuto")
                }
            } catch (e: Exception) {
                _identifyState.value = ApiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    // Chiamata per ottenere risultati di identificazione
    fun getRecognitionResult(apiKey: String, token: String) {
        viewModelScope.launch {
            _resultState.value = ApiState.Loading
            try {
                val response = apiService.getRecognitionResult(apiKey, token)
                _resultState.value = if (response.success) {
                    ApiState.Success(response)
                } else {
                    ApiState.Error(response.error ?: "Errore sconosciuto")
                }
            } catch (e: Exception) {
                _resultState.value = ApiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    // Chiamata per identificare un file audio remoto
    fun identifyRemoteAudioFile(apiKey: String, url: String, baseTime: String = "00:00:00", overwrite: Int = 0) {
        viewModelScope.launch {
            _identifyRemoteState.value = ApiState.Loading
            try {
                val response = apiService.identifyRemoteAudioFile(apiKey, url, baseTime, overwrite)
                _identifyRemoteState.value = if (response.success) {
                    ApiState.Success(response)
                } else {
                    ApiState.Error(response.error ?: "Errore sconosciuto")
                }
            } catch (e: Exception) {
                _identifyRemoteState.value = ApiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    // Chiamata per ottenere risultati di identificazione remota
    fun getRemoteRecognitionResult(apiKey: String, token: String) {
        viewModelScope.launch {
            _remoteResultState.value = ApiState.Loading
            try {
                val response = apiService.getRemoteRecognitionResult(apiKey, token)
                _remoteResultState.value = if (response.success) {
                    ApiState.Success(response)
                } else {
                    ApiState.Error(response.error ?: "Errore sconosciuto")
                }
            } catch (e: Exception) {
                _remoteResultState.value = ApiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }
}

// Stato generico per le chiamate API
sealed class ApiState<out T> { // Aggiungi 'out' qui
    object Idle : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()
    data class Success<T>(val data: T) : ApiState<T>()
    data class Error(val message: String) : ApiState<Nothing>()
}