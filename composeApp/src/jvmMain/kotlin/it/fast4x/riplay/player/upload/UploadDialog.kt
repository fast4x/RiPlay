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
