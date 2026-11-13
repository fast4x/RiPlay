package it.fast4x.riplay.extensions.listenbrainz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.listenbrainz.ListenBrainzService
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class ListenBrainzAuthState {
    object Idle : ListenBrainzAuthState()
    object Validating : ListenBrainzAuthState()
    object Authenticated : ListenBrainzAuthState()
    data class Error(val message: String) : ListenBrainzAuthState()
}

/**
 * Schermata di connessione a ListenBrainz.
 *
 * A differenza di last.fm (che usa un WebView con token temporaneo), ListenBrainz
 * richiede il token utente personale che si trova su https://listenbrainz.org/settings/
 * Il token viene verificato con l'endpoint /1/validate-token prima di essere salvato.
 *
 * E' possibile indicare anche l'indirizzo di un server ListenBrainz self-hosted
 * (ListenBrainz e' open source e puo' essere installato su un proprio server);
 * se il campo server e' vuoto viene usato il server ufficiale. Il token viene
 * validato proprio contro il server indicato, poi vengono salvati insieme.
 */
@Composable
fun ListenBrainzAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val appSettingsManager = (appContext() as MainApplication).appSettingsManager
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val colorPalette = colorPalette()

    var token by rememberSaveable { mutableStateOf("") }
    // Valore corrente del server impostato: vuoto = server ufficiale
    var serverUrl by rememberSaveable {
        mutableStateOf(appSettingsManager.activeSettings.value.listenBrainzServerUrl)
    }
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }
    var authState by remember { mutableStateOf<ListenBrainzAuthState>(ListenBrainzAuthState.Idle) }

    // Colori espliciti per i campi di testo: la modale e' aperta su uno sfondo
    // scuro/chiaro gestito dal tema RiPlay, mentre il colorScheme di Material
    // di default usa colori di sistema che rendono il testo quasi invisibile.
    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = colorPalette.background1,
        unfocusedContainerColor = colorPalette.background1,
        focusedTextColor = colorPalette.text,
        unfocusedTextColor = colorPalette.text,
        cursorColor = colorPalette.accent,
        focusedIndicatorColor = colorPalette.accent,
        unfocusedIndicatorColor = colorPalette.textDisabled,
        focusedLabelColor = colorPalette.text,
        unfocusedLabelColor = colorPalette.textSecondary,
        focusedPlaceholderColor = colorPalette.textDisabled,
        unfocusedPlaceholderColor = colorPalette.textDisabled
    )

    fun connect() {
        val trimmedToken = token.trim()
        if (trimmedToken.isEmpty()) return

        // Server da usare per la validazione e per le scrobble successive.
        // Il valore salvato nelle impostazioni e' normalizzato; vuoto = ufficiale.
        val serverToSave =
            if (serverUrl.trim().isEmpty()) ""
            else ListenBrainzService.normalizeServerUrl(serverUrl)

        authState = ListenBrainzAuthState.Validating

        coroutineScope.launch {
            ListenBrainzClient.service(serverToSave).validateToken(token = trimmedToken)
                .onSuccess { response ->
                    val username = response.userName ?: ""

                    appSettingsManager.updateSettings(
                        appSettingsManager.activeSettings.value.copy(
                            listenBrainzToken = trimmedToken,
                            listenBrainzUsername = username,
                            listenBrainzServerUrl = serverToSave
                        )
                    )

                    Timber.d("ListenBrainzAuthScreen: Save Token -> $trimmedToken user $username server ${ListenBrainzService.normalizeServerUrl(serverToSave)}")

                    // Il ramo Authenticated del when chiama onAuthSuccess()
                    authState = ListenBrainzAuthState.Authenticated
                }
                .onFailure { error ->
                    Timber.d("ListenBrainzAuthScreen: Authentication error ${error.message}")
                    authState = ListenBrainzAuthState.Error(error.message ?: "Authentication error")
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.listenbrainz_token_instructions),
            style = MaterialTheme.typography.bodyMedium,
            color = colorPalette.text
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { uriHandler.openUri("https://listenbrainz.org/settings/") }
        ) {
            Text(text = "listenbrainz.org/settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(text = stringResource(R.string.listenbrainz_token_hint)) },
            singleLine = true,
            colors = textFieldColors,
            visualTransformation = if (isTokenVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(
                    onClick = { isTokenVisible = !isTokenVisible }
                ) {
                    Text(
                        text = stringResource(
                            if (isTokenVisible) R.string.hide else R.string.show
                        ),
                        color = colorPalette.accent,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text(text = stringResource(R.string.listenbrainz_custom_server_hint)) },
            placeholder = { Text(text = ListenBrainzService.DEFAULT_SERVER_URL) },
            singleLine = true,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.listenbrainz_custom_server_instructions),
            style = MaterialTheme.typography.bodySmall,
            color = colorPalette.textSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = authState) {
            is ListenBrainzAuthState.Idle -> {
                Button(
                    onClick = { connect() },
                    enabled = token.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.listenbrainz_connect))
                }
            }

            is ListenBrainzAuthState.Validating -> {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.listenbrainz_validating),
                    color = colorPalette.text
                )
            }

            is ListenBrainzAuthState.Authenticated -> {
                onAuthSuccess()
            }

            is ListenBrainzAuthState.Error -> {
                Text(
                    text = stringResource(R.string.listenbrainz_token_invalid, state.message),
                    color = colorPalette.red,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { connect() },
                    enabled = token.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.listenbrainz_connect))
                }
            }
        }
    }
}
