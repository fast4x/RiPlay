package it.fast4x.riplay.extensions.experimental.appearancesettings.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.LocalAppearanceSettings
import it.fast4x.riplay.extensions.audiotag.models.UiState
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetEvent
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetUiState
import it.fast4x.riplay.extensions.experimental.appearancesettings.viewmodels.AppearancePresetViewModel
import kotlinx.coroutines.launch
import timber.log.Timber


@Composable
fun AppearancePresetDialogHost(
    context: Context = LocalContext.current,
    onDismiss: () -> Unit
) {
    val viewModel: AppearancePresetViewModel = viewModel(
        factory = AppearancePresetViewModel.factory(context)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activePresetId by viewModel.activePresetId.collectAsStateWithLifecycle()
    val presetList by viewModel.presetList.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appearanceSettings = LocalAppearanceSettings.current

    val uiStateWithPresets = remember(uiState, presetList) { PresetUiState.Success(presetList) }

    Timber.d("AppearancePresetDialogHost: $uiState")

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PresetEvent.Applied -> {
                    Timber.d("AppearancePreset Applied  \"${event.presetName}\" ")
                    scope.launch {
                        snackbarHostState.showSnackbar("Preset \"${event.presetName}\" applicato")
                    }
                    onDismiss()
                }
                is PresetEvent.Shared  -> { /* copia URL, mostra sheet, ecc. */ }
                is PresetEvent.Error   -> scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    AppearancePresetDialog(
        activePresetId = activePresetId,
        uiState   = uiStateWithPresets,
        onDismiss = onDismiss,
        onSelect  = {
            coroutineScope.launch {
                appearanceSettings.applyPreset(it)
            }
        },
        onShare   = {} // ex viewModel::sharePreset
    )

}