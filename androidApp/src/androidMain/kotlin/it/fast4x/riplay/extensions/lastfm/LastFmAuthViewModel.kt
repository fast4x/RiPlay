package it.fast4x.riplay.extensions.lastfm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.fast4x.lastfm.LastFmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class AuthState {
    object Idle : AuthState()
    object LoadingToken : AuthState()
    data class WebViewReady(val authUrl: String, val token: String) : AuthState()
    object FetchingSession : AuthState()
    data class Authenticated(val username: String, val sessionKey: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class LastFmAuthViewModel(
    private val lastFmService: LastFmService,
    private val onSaveSessionKey: (String) -> Unit
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun startAuthentication() {
        viewModelScope.launch {
            _authState.value = AuthState.LoadingToken

            val tokenResult = lastFmService.getAuthToken()

            tokenResult.onSuccess { token ->
                val authUrl = "https://www.last.fm/api/auth/?api_key=${lastFmService.apiKey}&token=${token}"

                _authState.value = AuthState.WebViewReady(authUrl, token)
            }.onFailure { error ->
                _authState.value = AuthState.Error("LastFmAuthViewModel error token: ${error.message}")
            }
        }
    }

    fun onUserApproved(token: String) {
        viewModelScope.launch {
            _authState.value = AuthState.FetchingSession

            val sessionResult = lastFmService.getSession(token)

            sessionResult.onSuccess { session ->
                onSaveSessionKey(session.key)
                _authState.value = AuthState.Authenticated(session.name, session.key)
            }.onFailure { error ->
                _authState.value = AuthState.Error("LastFmAuthViewModel error session: ${error.message}")
            }
        }
    }

}