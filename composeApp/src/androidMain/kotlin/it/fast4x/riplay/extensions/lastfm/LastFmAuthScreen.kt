package it.fast4x.riplay.extensions.lastfm

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.lastfmSessionTokenKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.globalContext
import timber.log.Timber

@Composable
fun LastFmAuthScreen(
    navController: NavController,
    onAuthSuccess: () -> Unit,
    viewModel: LastFmAuthViewModel = viewModel(
        factory = LastFmAuthViewModelFactory(
            lastFmService = LastFmClient.service,
            onSaveSessionKey = { sessionKey ->
                appContext().preferences.edit(commit = true) {
                    putString(lastfmSessionTokenKey, sessionKey)
                }
                Timber.d("LastFmAuthScreen: Save Session Key -> $sessionKey")
            }
        )
    )
) {
    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = authState) {
            is AuthState.Idle -> {
                viewModel.startAuthentication()
            }
            is AuthState.LoadingToken -> {
                CircularProgressIndicator()
                Text("Generate Token...")
            }
            is AuthState.WebViewReady -> {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                ) {
                    Title(
                        globalContext().resources.getString(it.fast4x.riplay.R.string.lastfm_connect),
                        icon = R.drawable.chevron_down,
                        onClick = { navController.navigateUp() }
                    )
                    LastFmAuthWebView(
                        authUrl = state.authUrl,
                        scope = viewModel.viewModelScope,
                        onAuthApproved = {
                            viewModel.onUserApproved(state.token)
                        },
                        onAuthError = { error ->
                            Timber.d("LastFmAuthScreen: Error $error")
                        }
                    )
                }

            }
            is AuthState.FetchingSession -> {
                CircularProgressIndicator()
                Text("Authentication in progress...")
            }
            is AuthState.Authenticated -> {
                onAuthSuccess()
            }
            is AuthState.Error -> {
                Text("Authentication Error: ${state.message}")
            }
        }
    }
}