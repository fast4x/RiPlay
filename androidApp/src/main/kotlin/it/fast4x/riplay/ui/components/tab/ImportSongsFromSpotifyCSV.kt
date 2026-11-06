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
import it.fast4x.riplay.utils.getFileNameFromUri
import it.fast4x.spotifymeta.SpotifyMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImportSongsFromSpotifyCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : Descriptive, MenuIcon {

    // Struttura dati per svuotare il CSV in modo sincrono e sicuro
    private data class CsvRowData(
        val rawRowMap: Map<String, String>
    )

    companion object {

        // Colonne minime utilizzate per riconoscere la struttura del file
        private const val COLUMN_TRACK_URI = "Track URI"
        private const val COLUMN_TRACK_NAME = "Track Name"
        private const val COLUMN_TITLE = "Title"

        /**
         * Eccezione interna usata per interrompere l'importazione riportando
         * l'id della stringa da mostrare all'utente tramite SmartMessage.
         */
        private class CsvImportException(val userMessageResId: Int) : Exception()

        /**
         * Verifica che l'intestazione del CSV contenga le colonne minime di
         * uno dei due formati gestiti da questo importer:
         * - formato Spotify (Exportify): "Track URI" + "Track Name"
         * - formato RiPlay generico:     "Title" oppure "Track Name"
         *
         * Se la struttura non è riconosciuta l'importazione viene interrotta
         * prima di toccare il database e l'utente riceve un messaggio di errore.
         */
        private fun isStructureSupported(header: Map<String, String>): Boolean =
            if (header.containsKey(COLUMN_TRACK_URI))
                header.containsKey(COLUMN_TRACK_NAME)
            else
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
                Timber.e(e, "ImportSongsFromSpotifyCSV: unable to read or parse the csv file")
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

            beforeTransaction(index, row, fileName)

            // Rilevamento del formato: controlliamo se esiste "Track URI" (Spotify)
            val isSpotifyFormat = row.containsKey(COLUMN_TRACK_URI)

            var song: Song? = null
            var album: Album? = null
            var artists: List<Artist> = emptyList()
            var spotifyTrackId: String? = null

            if (isSpotifyFormat) {
                val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""

                val mediaId = row[COLUMN_TRACK_URI]?.trim().orEmpty()
                val title = row[COLUMN_TRACK_NAME]?.trim().orEmpty()

                // Dati essenziali mancanti: la riga viene scartata senza interrompere l'importazione
                if (mediaId.isBlank() || title.isBlank()) return false

                // Gestione Artisti: Spotify usa "Artist Name(s)"
                val artistsText = row["Artist Name(s)"].orEmpty()

                // Gestione Durata: Spotify usa "Duration (ms)"
                val durationText = formatAsDuration(row["Duration (ms)"]?.toLongOrNull() ?: 0L)

                spotifyTrackId = mediaId.split(":").lastOrNull()

                song = Song(
                    id = mediaId,
                    title = explicitPrefix + title,
                    artistsText = artistsText,
                    durationText = durationText,
                    thumbnailUrl = null,
                    totalPlayTimeMs = 1L
                )

                // Album
                album = Album(
                    id = "",
                    title = row["Album Name"]
                )

                // Artisti
                artists = row["Artist Name(s)"]
                    ?.split(",")
                    ?.map { name -> Artist(id = "", name = name.trim()) }
                    ?: emptyList()

            } else {
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

                song = Song(
                    id = mediaId,
                    title = explicitPrefix + title,
                    artistsText = artistsText,
                    durationText = durationText,
                    thumbnailUrl = row["ThumbnailUrl"].orEmpty(),
                    totalPlayTimeMs = 1L
                )

                album = Album(
                    id = row["AlbumId"].orEmpty(),
                    title = row["AlbumTitle"]
                )

                val artistNames = row["Artists"]?.split(",")
                val artistIds = row["ArtistIds"]?.split(",")
                val mutableArtists = mutableListOf<Artist>()
                if (artistIds != null && artistNames != null && artistIds.size == artistNames.size) {
                    for (idx in artistIds.indices) {
                        val artistId = artistIds.getOrNull(idx)?.trim()
                        val artistName = artistNames.getOrNull(idx)?.trim()
                        if (!artistId.isNullOrBlank()) {
                            mutableArtists.add(
                                Artist(
                                    id = artistId,
                                    name = artistName.orEmpty()
                                )
                            )
                        }
                    }
                }
                artists = mutableArtists
            }

            val baseSong = song ?: return false
            val baseAlbum = album ?: return false

            // 3. Chiamata di rete 'suspend' integrata nel flusso lineare (solo formato Spotify).
            //    Protetta: un problema di rete non deve interrompere l'importazione.
            var finalSong = baseSong
            if (isSpotifyFormat && !spotifyTrackId.isNullOrBlank()) {
                val url = try {
                    SpotifyMeta.oEmbedInfo(spotifyTrackId).getOrNull()?.thumbnailUrl
                } catch (e: Exception) {
                    Timber.w(e, "ImportSongsFromSpotifyCSV: cover lookup failed for $spotifyTrackId")
                    null
                }
                if (!url.isNullOrEmpty()) {
                    finalSong = finalSong.copy(thumbnailUrl = url)
                    Timber.d("ImportPlaylist Cover for ${finalSong.title}: $url")
                }
            }

            // 4. Finalizzazione transazione e scrittura sincrona nel DB.
            //    afterTransaction può lanciare eccezioni (es. DB): sono gestite
            //    dal chiamante, che scarta la singola riga senza crashare.
            afterTransaction(index, finalSong, baseAlbum, artists)

            try {
                Database.upsert(finalSong)
                Timber.d("ImportSongsFromSpotifyCSV saved in the db: ${finalSong.title}")
            } catch (e: Exception) {
                Timber.e(e, "ImportSongsFromSpotifyCSV: db write failed for: ${finalSong.title}")
            }

            return true
        }

        private fun openFile(
            uri: Uri,
            externalScope: CoroutineScope, // Sfrutta lo scope persistente a livello applicazione
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _, _, _ -> },
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit = { _, _, _, _ -> }
        ) {
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
                        Timber.w(e, "ImportSongsFromSpotifyCSV: unable to resolve file name")
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

                    // 3. Fase asincrona e sequenziale sul pool di thread IO globale.
                    //    Ogni riga è protetta: un errore su una riga non blocca
                    //    né le altre né l'applicazione.
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
                            Timber.e(e, "ImportSongsFromSpotifyCSV: row ${index + 1} skipped due to an error")
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
                    Timber.w("ImportSongsFromSpotifyCSV: import aborted (${e.userMessageResId})")
                    errorResId = e.userMessageResId
                } catch (e: Throwable) {
                    Timber.e(e, "ImportSongsFromSpotifyCSV: unexpected error during import")
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
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _,_,_ -> },
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) = ImportSongsFromSpotifyCSV(
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult

                // Aggancio diretto allo scope globale condiviso a livello di applicazione
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
            SmartMessage(appContext().resources.getString(R.string.info_not_find_app_open_doc),
                type = PopupType.Warning, context = appContext())
        }
    }
}
