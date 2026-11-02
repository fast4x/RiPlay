package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Cache su disco della copertina mostrata nei widget.
 *
 * In precedenza l'artwork viaggiava come stringa Base64 dentro il DataStore
 * di ogni widget: una JPEG da ~30KB diventava una stringa da ~40KB scritta
 * su disco per ogni istanza di ogni widget a ogni aggiornamento.
 *
 * Ora l'immagine ridimensionata viene salvata una sola volta in
 * `cacheDir/widget_artwork/` (un file per mediaId) e nei widget viaggia
 * solo il percorso. Se il brano non cambia, il file esiste già e non
 * avviene nessuna riscrittura.
 */
object WidgetArtworkCache {

    private const val DIR_NAME = "widget_artwork"
    private const val MAX_SIZE_PX = 256
    private const val JPEG_QUALITY = 80

    /** Numero massimo di copertine conservate (corrente + alcune recenti). */
    private const val MAX_FILES = 3

    /**
     * Salva (o riusa) la copertina per il brano indicato e restituisce il
     * percorso assoluto del file, oppure null se l'artwork non è utilizzabile.
     *
     * @param mediaId id del brano corrente; determina il nome del file
     * @param source  bitmap sorgente fornita dal [it.fast4x.riplay.utils.BitmapProvider]
     */
    suspend fun save(context: Context, mediaId: String?, source: Bitmap?): String? {
        if (source == null || source.isRecycled || source.width < 20 || source.height < 20) {
            return null
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, DIR_NAME).apply { mkdirs() }
                val safeId = (mediaId ?: "unknown")
                    .take(40)
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                // Prefisso "cover2_": invalida i file della versione precedente,
                // che potevano contenere il placeholder tinta unita salvato per errore.
                val target = File(dir, "cover2_$safeId.jpg")

                // Copertina già presente per questo brano: nessuna riscrittura
                if (target.exists() && target.length() > 0) {
                    cleanup(dir, keep = target)
                    return@runCatching target.absolutePath
                }

                val scaled = scaleDown(source)
                val tmp = File(dir, "${target.name}.tmp")
                FileOutputStream(tmp).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                if (!tmp.renameTo(target)) {
                    tmp.delete()
                    Timber.w("WidgetArtworkCache: rename fallito per ${target.name}")
                    return@runCatching null
                }

                cleanup(dir, keep = target)
                target.absolutePath
            }.onFailure {
                Timber.e(it, "WidgetArtworkCache: errore salvataggio copertina")
            }.getOrNull()
        }
    }

    private fun scaleDown(source: Bitmap): Bitmap {
        val ratio = source.width.toFloat() / source.height.toFloat()
        val width = if (source.width >= source.height) MAX_SIZE_PX else (MAX_SIZE_PX * ratio).toInt()
        val height = if (source.height >= source.width) MAX_SIZE_PX else (MAX_SIZE_PX / ratio).toInt()
        val result = Bitmap.createScaledBitmap(source, width.coerceAtLeast(1), height.coerceAtLeast(1), true)
        // createScaledBitmap può restituire la stessa istanza se le dimensioni coincidono:
        // in quel caso non va riciclata, la sorgente è di proprietà del chiamante.
        return result
    }

    /** Mantieni il file corrente e le più recenti tra le altre; elimina il resto. */
    private fun cleanup(dir: File, keep: File) {
        val others = dir.listFiles { file: File -> file.isFile && file.name.endsWith(".jpg") && file != keep }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        others.drop(MAX_FILES - 1).forEach { file ->
            runCatching { file.delete() }
        }
    }
}
