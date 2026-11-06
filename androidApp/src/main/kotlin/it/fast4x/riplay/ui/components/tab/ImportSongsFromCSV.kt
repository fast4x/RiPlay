package it.fast4x.riplay.ui.components.tab

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon
import it.fast4x.riplay.utils.formatAsDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImportSongsFromCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : Descriptive, MenuIcon {

    // Struttura dati di appoggio per estrarre in modo sincrono le righe dal blocco restrittivo CSV
    private data class CsvRowData(
        val rawRowMap: Map<String, String>
    )

    companion object {

        // Colonne minime utilizzate per riconoscere la struttura del file RiPlay/generico
        private const val COLUMN_TITLE = "Title"
        private const val COLUMN_TRACK_NAME = "Track Name"

        /**
         * Eccezione interna usata per interrompere l'importazione riportando
         * l'id della stringa da mostrare all'utente tramite SmartMessage.
         */
        private class CsvImportException(val userMessageResId: Int) : Exception()

        /**
         * Verifica che l'intestazione del CSV contenga almeno una colonna titolo
         * ("Title" oppure "Track Name"): tutte le altre colonne sono opzionali.
         *
         * Se la struttura non è riconosciuta l'importazione viene interrotta
         * prima di toccare il database e l'utente riceve un messaggio di errore.
         */
        private fun isStructureSupported(header: Map<String, String>): Boolean =
            header.containsKey(COLUMN_TITLE) || header.containsKey(COLUMN_TRACK_NAME)

        /**
         * 1. Fase di lettura e parsing sincrona: il file viene letto per intero
         *    e l'inputStream viene chiuso immediatamente.
         *    Qualunque problema di lettura o di interpretazione del CSV viene
         *    convertito in CsvImportException con un messaggio comprensibile.
         */
        private fun readCsvRows(context: Context, uri: Uri): List<CsvRowData> {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw CsvImportException(R.string.error_import_unreadable_file)

                val parsedRows = mutableListOf<CsvRowData>()
                inputStream.use { input ->
                    csvReader().open(input) {
                        readAllWithHeaderAsSequence().forEach { row ->
                            parsedRows.add(CsvRowData(rawRowMap = row))
                        }
                    }
                }
                return parsedRows
            } catch (e: CsvImportException) {
                throw e
            } catch (e: Throwable) {
                // File non leggibile, non testuale o CSV malformato: niente crash,
                // l'errore viene loggato e segnalato all'utente
                Timber.e(e, "ImportSongsFromCSV: unable to read or parse the csv file")
                throw CsvImportException(R.string.error_import_unreadable_file)
            }
        }

        /**
         * 2. Elaborazione di una singola riga del CSV.
         *
         * @return true  -> la riga è stata elaborata e il brano salvato
         *         false -> la riga è stata scartata perché priva dei dati essenziali
         * @throws Throwable eventuali errori imprevisti, gestiti riga per riga dal chiamante
         */
        private suspend fun processRow(
            index: Int,
            rowData: CsvRowData,
            beforeTransaction: (Int, Map<String, String>) -> Unit,
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit
        ): Boolean {
            val row = rowData.rawRowMap

            Timber.d("ImportSongsFromCSV: processing row ${index + 1}")

            // Callback pre-transazione eseguito direttamente in IO (niente withContext ridondanti)
            beforeTransaction(index, row)

            val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
            val pseudoMediaId = (row[COLUMN_TRACK_NAME].orEmpty() + row["Artist Name(s)"].orEmpty())
                .filter { it.isLetterOrDigit() }

            val title = (row[COLUMN_TITLE] ?: row[COLUMN_TRACK_NAME])?.trim().orEmpty()

            // Titolo assente: la riga viene scartata senza interrompere l'importazione
            if (title.isBlank()) return false

            val mediaId = row["MediaId"]?.trim()?.takeIf { it.isNotBlank() } ?: pseudoMediaId
            if (mediaId.isBlank()) return false

            val artistsText = row["Artists"] ?: row["Artist Name(s)"] ?: ""

            // Tenta prima la colonna "Duration" (testo), poi "Track Duration (ms)"
            val durationText = row["Duration"]
                ?: formatAsDuration(row["Track Duration (ms)"]?.toLongOrNull() ?: 0L)

            val song = Song(
                id = mediaId,
                title = explicitPrefix + title,
                artistsText = artistsText,
                durationText = durationText,
                thumbnailUrl = row["ThumbnailUrl"].orEmpty(),
                totalPlayTimeMs = 1L
            )

            val album = Album(
                id = row["AlbumId"].orEmpty(),
                title = row["AlbumTitle"]
            )

            val artistNames = row["Artists"]?.split(",")
            val artistIds = row["ArtistIds"]?.split(",")
            val artists = mutableListOf<Artist>()
            if (artistIds != null && artistNames != null && artistNames.size == artistIds.size) {
                for (idx in artistIds.indices) {
                    val artistName = artistNames.getOrNull(idx)?.trim()
                    val artistId = artistIds.getOrNull(idx)?.trim()
                    if (!artistId.isNullOrBlank()) {
                        val artist = Artist(
                            id = artistId,
                            name = artistName.orEmpty()
                        )
                        artists.add(artist)
                    }
                }
            }

            // Callback post-transazione eseguito sul thread IO.
            // afterTransaction può lanciare eccezioni (es. DB): sono gestite
            // dal chiamante, che scarta la singola riga senza crashare.
            afterTransaction(index, song, album, artists)

            // Scrittura sincrona e atomica sul DB: una riga alla volta nell'ordine corretto
            try {
                Database.upsert(song)
                Timber.d("ImportSongsFromCSV inserted in the db: $title")
            } catch (e: Exception) {
                Timber.e(e, "ImportSongsFromCSV: db write failed for: $title")
            }

            return true
        }

        private fun openFile(
            uri: Uri,
            externalScope: CoroutineScope, // Sfrutta lo scope persistente dell'app
            beforeTransaction: (Int, Map<String, String>) -> Unit = { _, _ -> },
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit = { _, _, _, _ -> }
        ) {
            // Eseguiamo tutto direttamente sotto il pool IO dello scope globale condiviso
            externalScope.launch(Dispatchers.IO) {
                val context = appContext().applicationContext

                var errorResId: Int? = null
                var importedCount = 0 // righe importate con successo
                var skippedCount = 0  // righe scartate perché prive dei dati essenziali
                var failedCount = 0   // righe interrotte da errori imprevisti

                try {
                    // 1. Fase di parsing sincrona e immediata: svuota il CSV in memoria e chiude il file
                    val parsedRows = readCsvRows(context, uri)

                    if (parsedRows.isEmpty()) {
                        // Il file è vuoto o non contiene alcuna riga di dati
                        throw CsvImportException(R.string.error_import_empty_file)
                    }

                    // 2. Verifica della struttura: senza le colonne minime l'operazione
                    //    viene interrotta subito e segnalata all'utente
                    if (!isStructureSupported(parsedRows.first().rawRowMap)) {
                        throw CsvImportException(R.string.error_import_invalid_structure)
                    }

                    // 3. Fase di elaborazione e scrittura sequenziale sul database.
                    //    Ogni riga è protetta: un errore su una riga non blocca
                    //    né le altre né l'applicazione.
                    parsedRows.forEachIndexed { index, rowData ->
                        try {
                            if (processRow(index, rowData, beforeTransaction, afterTransaction))
                                importedCount++
                            else
                                skippedCount++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            failedCount++
                            Timber.e(e, "ImportSongsFromCSV: row ${index + 1} skipped due to an error")
                        }
                    }

                    // Nessun brano importato: la struttura di fatto non è utilizzabile
                    if (importedCount == 0) {
                        errorResId =
                            if (failedCount > 0) R.string.error_import_failed
                            else R.string.error_import_invalid_structure
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: CsvImportException) {
                    Timber.w("ImportSongsFromCSV: import aborted (${e.userMessageResId})")
                    errorResId = e.userMessageResId
                } catch (e: Throwable) {
                    Timber.e(e, "ImportSongsFromCSV: unexpected error during import")
                    errorResId = R.string.error_import_failed
                }

                // 4. Feedback all'utente, sempre sul main thread
                withContext(Dispatchers.Main) {
                    val failureResId = errorResId
                    val message = when {
                        failureResId != null -> context.getString(failureResId)
                        importedCount > 0 && (skippedCount + failedCount) > 0 ->
                            context.getString(
                                R.string.info_import_completed_with_skipped,
                                importedCount,
                                skippedCount + failedCount
                            )
                        importedCount > 0 ->
                            context.getString(R.string.info_import_completed, importedCount)
                        else ->
                            context.getString(R.string.error_import_invalid_structure)
                    }
                    SmartMessage(message, type = if (importedCount > 0) PopupType.Success else PopupType.Warning, context = context)
                }
            }
        }

        @JvmStatic
        @Composable
        fun init(
            beforeTransaction: (Int, Map<String, String>) -> Unit = { _, _ -> },
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit = { _, _, _, _ -> }
        ) = ImportSongsFromCSV(
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult

                // Recuperiamo lo scope globale dell'applicazione prima di invocare openFile
                val appScopeIO = Dependencies.application.appScopeIO
                openFile(uri, appScopeIO, beforeTransaction, afterTransaction)
            }
        )
    }

    override val messageId: Int = R.string.import_playlist
    override val iconId: Int = R.drawable.resource_import
    override val menuIconTitle: String
        @Composable
        get() = stringResource(messageId)

    override fun onShortClick() {
        try {
            launcher.launch(arrayOf("text/csv", "text/comma-separated-values"))
        } catch (_: ActivityNotFoundException) {
            SmartMessage(
                appContext().resources.getString(R.string.info_not_find_app_open_doc),
                type = PopupType.Warning, context = appContext()
            )
        }
    }
}
