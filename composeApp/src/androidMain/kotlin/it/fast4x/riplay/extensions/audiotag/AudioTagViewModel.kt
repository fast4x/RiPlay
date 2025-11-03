package it.fast4x.riplay.extensions.audiotag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.audiotaginfo.AudioTagInfo
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.audiotag.AudioRecorder
import it.fast4x.riplay.extensions.audiotag.models.UiState
import it.fast4x.riplay.utils.globalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class AudioTagViewModel(
    //private val repository: AudioTagRepository
) : ViewModel(), ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioTagViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioTagViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder()
    private val apiKey = globalContext().resources.getString(R.string.AudioTagInfo_API_KEY)

    fun info() {
        viewModelScope.launch {
            val response = AudioTagInfo.info(apiKey)
            Timber.d("AudioTag Info: $response")
        }
    }

    fun identifySong() {
        if (_uiState.value is UiState.Recording) return

        viewModelScope.launch {
            _uiState.value = UiState.Recording
            val audioData = audioRecorder.startRecording()

            if (audioData != null) {
                _uiState.value = UiState.Loading
                val result = AudioTagInfo.identifyAudioFile(apiKey, audioData)

                Timber.d("AudioTag Result: $result")

                result?.fold(
                    onSuccess = { response ->
                        response.token ?.let {
                            _uiState.value = UiState.Response(it)
                            Timber.d("AudioTag Token: $it")
                        } ?: run {
                            _uiState.value = UiState.Error("Song not found.")
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "An unknown error occurred.")
                    }
                )
            } else {
                _uiState.value = UiState.Error("Recording failed.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
    }
}