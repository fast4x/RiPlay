package it.fast4x.riplay.musicvault

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getSafeDefaultDir
import kotlinx.coroutines.launch
import timber.log.Timber

const val DEFAULT_MUSICVAULT_DIRECTORY = "MusicVault"

@Composable
fun MusicVaultFolderSetting() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    // Otteniamo il percorso di default in modo sicuro
    val defaultDir = remember { getSafeDefaultDir(context, DEFAULT_MUSICVAULT_DIRECTORY) }

    //var musicVaultPath by rememberPreference(MUSIC_VAULT_PATH.key, "")
    val musicVaultPath = appSettings.musicVaultPath

    // Logica di visualizzazione unificata
    val displayPath = when {
        musicVaultPath.isEmpty() -> {
            // Se non ha scelto nulla, mostriamo il percorso di default dell'app
            defaultDir.absolutePath
        }
        musicVaultPath.startsWith("content://") -> {
            // Se ha scelto una cartella SAF, mostriamo il nome leggibile
            DocumentFile.fromTreeUri(context, musicVaultPath.toUri())
                ?.uri?.lastPathSegment ?: musicVaultPath
        }
        else -> musicVaultPath // Fallback per ogni altro caso
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Prendiamo i permessi solo per gli URI content:// (SAF)
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // Salviamo l'URI content:// come stringa

            coroutineScope.launch {
                appSettingsManager.updateSettings(
                    appSettingsManager.activeSettings.value.copy(musicVaultPath = it.toString())
                )
            }
            Timber.d("MusicVaultFolderSetting folderPicke.r musicVaultPath: $musicVaultPath")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { folderPicker.launch(null) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_music_vault_folder_music_vault),
            color = colorPalette().text
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = displayPath,
            color = colorPalette().textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Reset al path di default
        if (musicVaultPath.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_music_vault_restore_default_folder),
                color = colorPalette().text,
                modifier = Modifier.clickable {
                    // Ripristiniamo la stringa vuota, che nel Repository
                    // verrà interpretata come "usa la cartella di default"
                    coroutineScope.launch {
                        appSettingsManager.updateSettings(
                            appSettingsManager.activeSettings.value.copy(musicVaultPath = "")
                        )
                    }
                }
            )
        }
    }
}