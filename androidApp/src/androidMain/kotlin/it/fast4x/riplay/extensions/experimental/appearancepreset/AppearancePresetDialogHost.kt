package it.fast4x.riplay.extensions.experimental.appearancepreset

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
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetEvent
import kotlinx.coroutines.launch


@Composable
fun AppearancePresetDialogHost(
    context: Context = LocalContext.current,
    onDismiss: () -> Unit
) {
    val viewModel: AppearancePresetViewModel = viewModel(
        factory = AppearancePresetViewModel.factory(context)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PresetEvent.Applied -> {
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

//    AppearancePresetDialog(
//        uiState   = uiState,
//        onDismiss = onDismiss,
//        onSelect  = viewModel::applyPreset,
//        onShare   = viewModel::sharePreset
//    )

}