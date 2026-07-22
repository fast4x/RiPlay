package it.fast4x.riplay.extensions.experimental.appearancesettings.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.AppearancePresetDto
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetEvent
import it.fast4x.riplay.extensions.experimental.appearancesettings.models.PresetUiState
import it.fast4x.riplay.extensions.experimental.appearancesettings.utils.toDomain
import it.fast4x.riplay.extensions.experimental.appearancesettings.utils.toDto
import it.fast4x.riplay.extensions.experimental.appearancesettings.viewmodels.AppearancePresetViewModel
import it.fast4x.riplay.utils.DbSettingsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException

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
    val appearanceSettings = LocalAppearanceSettingsManager.current

    val uiStateWithPresets = remember(uiState, presetList) { PresetUiState.Success(presetList) }

    val coroutineScope = rememberCoroutineScope()

    // ---------------------------------------------------------
    // 1. LAUNCHER PER L'EXPORT (Salvataggio su file)
    // ---------------------------------------------------------
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    // Converto preset attuale in DTO/JSON
                    val currentPreset = viewModel.getCurrentActivePreset()
                    val json = DbSettingsJson.encodeToString(currentPreset.toDto())

                    // Scrivo il JSON nel file scelto dall'utente
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                        outputStream.close()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Errore durante l'esportazione del tema")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // 2. LAUNCHER PER L'IMPORT (Apertura file)
    // ---------------------------------------------------------
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    // Leggo il testo dal file scelto
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: throw FileNotFoundException("Impossibile leggere il file")
                    }

                    // Deserializzo e salvo il JSON nel DB
                    val dto = DbSettingsJson.decodeFromString<AppearancePresetDto>(json)

                    appearanceSettings.importAndApplyPreset(dto.toDomain()) // Da creare nel Manager

                } catch (e: Exception) {
                    Timber.e(e, "Errore durante l'importazione del tema (JSON non valido?)")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // GESTIONE EVENTI (LaunchedEffect)
    // ---------------------------------------------------------
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PresetEvent.Applied -> {
                    Timber.d("AppearancePreset Applied  \"${event.presetName}\" ")
                    onDismiss()
                }
                is PresetEvent.Shared  -> { /* Se vuoi mantenere la condivisione URL */ }
                is PresetEvent.Error   -> scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    // ---------------------------------------------------------
    // UI DIALOG
    // ---------------------------------------------------------
    AppearancePresetDialog(
        activePresetId = activePresetId,
        uiState   = uiStateWithPresets,
        onDismiss = onDismiss,
        onSelect  = {
            coroutineScope.launch {
                appearanceSettings.applyPreset(it) // applica
                onDismiss() // chiude
            }
        },
        onShare   = {},
        onExport  = { exportPresetId ->
            exportLauncher.launch("RP_Theme_$exportPresetId.json")
        },
        onImport  = {
            importLauncher.launch("application/json")
        },
        onDelete  = { deletePresetId ->
            coroutineScope.launch {
                appearanceSettings.deletePreset(deletePresetId)
            }
        }
    )
}