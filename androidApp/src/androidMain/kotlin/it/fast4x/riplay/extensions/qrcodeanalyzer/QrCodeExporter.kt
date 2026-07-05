package it.fast4x.riplay.extensions.qrcodeanalyzer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object QrCodeExporter {

    suspend fun shareQrCode(
        context: Context, // Assicurati di passare qui l'Activity Context, NON l'Application Context
        bitmap: Bitmap,
        fileName: String = "qrcode.png"
    ) {
        // 1. Creazione del file in background
        val uri = withContext(Dispatchers.IO) {
            Timber.d("DEBUG_CRASH 1. Inizio creazione file")
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            Timber.d("DEBUG_CRASH 2. File creato, genero URI")
            val generatedUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )
            Timber.d("DEBUG_CRASH 3. URI generato con successo: $generatedUri")
            generatedUri // Ritorna l'URI
        }


        // 2. Lancio della condivisione sul Main Thread
        withContext(Dispatchers.Main) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                // Diamo il permesso di lettura alle altre app
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }


            // FONDAMENTALE: Usa SEMPRE createChooser per evitare che l'app chiami se stessa
            val chooserIntent = Intent.createChooser(shareIntent, null)
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)



        }
    }
}