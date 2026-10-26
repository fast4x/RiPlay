package player

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import upload.pickLocalMediaPath

/**
 * STEP 3: input manuale unico per ENTRAMBI i motori (niente piu' tab):
 *  - URL/id YouTube -> routing verso il motore KCEF;
 *  - file locale (pulsante cartella o percorso) / URL stream -> VLCJ.
 * Il routing avviene in [DesktopPlayerManager.route] in base al contenuto.
 *
 * Il file picker (prima UploadDialog) ora e' one-shot dentro una
 * coroutine: niente dialog riaperti a ogni ricomposizione e niente
 * scritture di stato durante la composizione.
 */
@Composable
fun PlayerInput(modifier: Modifier = Modifier, onUrl: (String) -> Unit) {

    val (url, setUrl) = remember { mutableStateOf("") }
    var uploadTick by remember { mutableStateOf(0) }

    // File picker locale: one-shot (chiamato dalla coroutine, non durante
    // la composizione); un tick = un'apertura del dialog.
    LaunchedEffect(uploadTick) {
        if (uploadTick > 0) {
            val path = pickLocalMediaPath()
            if (!path.isNullOrBlank()) setUrl(path)
        }
    }

    // Già presente prima: debounce di 500 ms prima di chiamare onUrl
    LaunchedEffect(url) {
        delay(500L)
        url.takeIf { it.isNotBlank() }?.let(onUrl)
    }

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(url,
                setUrl,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(ds("local.input_hint")) },
                trailingIcon = {
                    IconButton(onClick = { setUrl("") }, enabled = url.isNotBlank()) {
                        Icon(Icons.Rounded.Clear, ds("local.clear_url"))
                    }
                })
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { setUrl(url) }, enabled = url.isNotBlank()) {
                    Icon(Icons.Rounded.Done, "done")
                }
                IconButton(onClick = { uploadTick++ }) {
                    Icon(Icons.Rounded.FileUpload, ds("local.open_file"))
                }
            }
        }
    }
}
