package upload

import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog

/**
 * STEP 3: sostituisce il vecchio composable UploadDialog (che apriva il
 * dialog DURANTE la composizione e rilanciava il percorso anche come
 * valore di ritorno: a ogni ricomposizione il dialog si riapriva).
 *
 * Ora e' una funzione normale ONE-SHOT da chiamare dentro una coroutine:
 * il Main dispatcher del desktop gira sull'EDT, quindi il dialog modale
 * e' lecito. Restituisce il percorso COMPLETO del file scelto, oppure
 * null se l'utente annulla.
 */
fun pickLocalMediaPath(): String? {
    val parent = ComposeWindow()
    return try {
        val dialog = FileDialog(parent, "Scegli un file multimediale", FileDialog.LOAD)
        dialog.isVisible = true // blocca finché non si chiude il dialog
        val dir = dialog.directory ?: ""
        val file = dialog.file
        if (file.isNullOrBlank()) null else "$dir$file"
    } catch (e: Exception) {
        println("pickLocalMediaPath: ${e.localizedMessage}")
        null
    } finally {
        runCatching { parent.dispose() }
    }
}

/**
 * STEP 3m (richiesta utente n.2 "caricamento dei files locali presenti
 * nel pc che poi vengono riprodotti con il motore vlcj"): picker di una
 * CARTELLA per l'importazione in blocco dei file musicali (pagina
 * "Locali"). Restituisce il percorso della cartella scelta o null.
 * One-shot come [pickLocalMediaPath]: da chiamare dentro una coroutine.
 * JFileChooser (e non FileDialog) perche' FileDialog non seleziona le
 * directory in modo affidabile su Windows.
 */
fun pickLocalFolder(): String? {
    return try {
        val chooser = javax.swing.JFileChooser().apply {
            dialogTitle = "Scegli la cartella con i tuoi file musicali"
            fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
            isMultiSelectionEnabled = false
        }
        val result = chooser.showOpenDialog(null)
        if (result == javax.swing.JFileChooser.APPROVE_OPTION)
            chooser.selectedFile.absolutePath
        else null
    } catch (e: Exception) {
        println("pickLocalFolder: ${e.localizedMessage}")
        null
    }
}

/** Estensioni audio riconosciute dall'importazione locale (desktop). */
val LOCAL_AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma", "mp4", "m4b"
)

/**
 * STEP 3m: picker MULTI-FILE per importare singoli file musicali (pagina
 * "Locali"). Restituisce la lista dei percorsi scelti (vuota se annullato).
 */
fun pickLocalAudioFiles(): List<String> {
    return try {
        val chooser = javax.swing.JFileChooser().apply {
            dialogTitle = "Scegli i file musicali da importare"
            fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = true
        }
        val result = chooser.showOpenDialog(null)
        if (result == javax.swing.JFileChooser.APPROVE_OPTION)
            chooser.selectedFiles.map { it.absolutePath }
        else emptyList()
    } catch (e: Exception) {
        println("pickLocalAudioFiles: ${e.localizedMessage}")
        emptyList()
    }
}
