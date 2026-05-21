package it.fast4x.riplay.extensions.experimental.musicvalt

import android.content.Intent
import android.os.Environment
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_VAULT_PATH
import it.fast4x.riplay.extensions.preferences.rememberPreference
import androidx.core.net.toUri
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette

@Composable
fun MusicVaultFolderSetting() {
    val context = LocalContext.current
    var musicVaultPath by rememberPreference(MUSIC_VAULT_PATH.key, "")

    val displayPath = when {
        musicVaultPath.isEmpty() ->
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath ?: ""
        musicVaultPath.startsWith("content://") ->
            DocumentFile.fromTreeUri(context, musicVaultPath.toUri())
                ?.uri?.lastPathSegment ?: musicVaultPath
        else -> musicVaultPath
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            musicVaultPath = it.toString()
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
                modifier = Modifier.clickable { musicVaultPath = "" }
            )
        }
    }
}