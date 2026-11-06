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
import it.fast4x.environment.Environment
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
import it.fast4x.riplay.utils.DEEZER_TRACK_KEY_PREFIX
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getFileNameFromUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImportSongsFromDeezerCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : Descriptive, MenuIcon {

    private data class CsvRowData(
        val rawRowMap: Map<String, String>
    )

    companion object {

        // Colonne minime attese dal formato TuneMyMusic/Deezer
        private const val COLUMN_ISRC = "ISRC"
        private const val COLUMN_TRACK_NAME = "Track name"

        /**
         * Eccezione interna usata per interrompere l'importazione riportando
         * l'id della stringa da mostrare all'utente tramite SmartMessage.
         */
        private class CsvImportException(val userMessageResId: Int) : Exception()

        /**
         * Verifica che l'intestazione del CSV contenga le colonne obbligatorie
         * del formato Deezer: "ISRC" e "Track name".
         *
         * Se la struttura non è riconosciuta l'importazione viene interrotta
         * prima di toccare il database e l'utente riceve un messaggio di errore.
         */
        private fun isStructureSupported(header: Map<String, String>): Boolean =
            header.containsKey(COLUMN_ISRC) && header.containsKey(COLUMN_TRACK_NAME)

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
                Timber.e(e, "ImportSongsFromDeezerCSV: unable to read or parse the csv file")
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
            fileName: String?,
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit,
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit
        ): Boolean {
            val row = rowData.rawRowMap

            val isrc = row[COLUMN_ISRC]?.trim().orEmpty()
            val trackName = row[COLUMN_TRACK_NAME]?.trim().orEmpty()
            val artistName = row["Artist name"].orEmpty()
            val albumName = row["Album"].orEmpty()

            // Dati essenziali mancanti: la riga viene scartata senza interrompere l'importazione
            if (isrc.isBlank() || trackName.isBlank()) return false

            val songId = "$DEEZER_TRACK_KEY_PREFIX$isrc"

            var song = Song(
                id = songId,
                title = trackName,
                artistsText = artistName,
                durationText = "0:00",
                thumbnailUrl = null,
                totalPlayTimeMs = 1L
            )

            val album = Album(id = "", title = albumName)
            val artists = listOf(Artist(id = "", name = artistName))

            beforeTransaction(index, row, fileName)

            // Chiamata di rete Deezer, protetta: un errore non interrompe l'importazione
            val trackInfo = try {
                Environment.deezerTrackInfo(isrc).getOrNull()
            } catch (e: Exception) {
                Timber.w(e, "ImportSongsFromDeezerCSV: track info lookup failed for ISRC $isrc")
                null
            }
            trackInfo?.let { info ->
                song = song.copy(
                    thumbnailUrl = info.album?.coverXl?.toString(),
                    durationText = formatAsDuration(info.duration * 1000L)
                )
            }

            // afterTransaction può lanciare eccezioni (es. DB): sono gestite
            // dal chiamante, che scarta la singola riga senza crashare.
            afterTransaction(index, song, album, artists)

            try {
                Database.upsert(song)
                Timber.d("ImportPlaylist in background inserito nel DB: $trackName")
            } catch (e: Exception) {
                Timber.e(e, "ImportSongsFromDeezerCSV: db write failed for: $trackName")
            }

            return true
        }

        private fun openFile(
            uri: Uri,
            externalScope: CoroutineScope, // Riceve lo scope persistente
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _, _, _ -> },
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit = { _, _, _, _ -> }
        ) {
            // Avviamo il lavoro sullo scope dell'applicazione
            externalScope.launch(Dispatchers.IO) {
                val context = appContext().applicationContext

                var errorResId: Int? = null
                var importedCount = 0 // righe importate con successo
                var skippedCount = 0  // righe scartate perché prive dei dati essenziali
                var failedCount = 0   // righe interrotte da errori imprevisti

                try {
                    // Nome file: solo informativo, un eventuale errore non deve bloccare l'import
                    val fileName = try {
                        context.getFileNameFromUri(uri)
                    } catch (e: Exception) {
                        Timber.w(e, "ImportSongsFromDeezerCSV: unable to resolve file name")
                        null
                    }

                    // 1. Fase di parsing sincrona e rapida: chiude l'inputStream immediatamente
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

                    // 3. Ciclo di elaborazione protetto: ogni riga è isolata,
                    //    un errore su una riga non blocca né le altre né l'applicazione.
                    parsedRows.forEachIndexed { index, rowData ->
                        try {
                            if (processRow(index, rowData, fileName, beforeTransaction, afterTransaction))
                                importedCount++
                            else
                                skippedCount++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            failedCount++
                            Timber.e(e, "ImportSongsFromDeezerCSV: row ${index + 1} skipped due to an error")
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
                    Timber.w("ImportSongsFromDeezerCSV: import aborted (${e.userMessageResId})")
                    errorResId = e.userMessageResId
                } catch (e: Throwable) {
                    Timber.e(e, "ImportSongsFromDeezerCSV: unexpected error during import")
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
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _,_,_ ->},
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ): ImportSongsFromDeezerCSV {

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult

                val appScopeIO = Dependencies.application.appScopeIO // Eseguo su scope globale per evitare interruzioni
                openFile(uri, appScopeIO, beforeTransaction, afterTransaction)
            }
            return ImportSongsFromDeezerCSV(launcher)
        }
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
