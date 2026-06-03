package it.fast4x.riplay.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
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
import it.fast4x.riplay.extensions.preferences.rememberPreference
import androidx.core.net.toUri
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.utils.colorPalette

@Composable
fun FolderSetting(
    folderKey: PreferenceKey,
    title: String
) {
    val context = LocalContext.current
    var folderPath by rememberPreference(folderKey.key, "/")

    val displayPath = when {
        folderPath.isEmpty() -> "/"
        else -> folderPath
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
            folderPath = extractPathFromTreeUri(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { folderPicker.launch(null) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
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
        if (folderPath.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_music_vault_restore_default_folder),
                color = colorPalette().text,
                modifier = Modifier.clickable { folderPath = "/" }
            )
        }
    }
}

/**
 * Funzione di utilità per convertire l'URI di SAF in un percorso leggibile.
 * Es: content://com.android.externalstorage.documents/tree/primary%3AMusic -> /Music
 */
private fun extractPathFromTreeUri(uri: Uri): String {
    val docId = DocumentsContract.getTreeDocumentId(uri)

    // Il docId di solito è nella forma "primary:Music" o "primary:" per la root
    val split = docId.split(":")

    return if (split.size > 1 && split[1].isNotEmpty()) {
        // Se c'è una parte dopo i due punti, è il nome della cartella
        "/${split[1]}"
    } else {
        // Se non c'è (es. l'utente ha selezionato la root principale), restituisce "/"
        "/"
    }
}