package it.fast4x.riplay.ui.components.tab

import android.content.ActivityNotFoundException
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportSongsFromCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
): Descriptive, MenuIcon {

    // Struttura dati di appoggio per estrarre in modo sincrono le righe dal blocco restrittivo CSV
    private data class CsvRowData(
        val rawRowMap: Map<String, String>
    )

    companion object {
        private fun openFile(
            uri: Uri,
            externalScope: CoroutineScope, // Sfrutta lo scope persistente dell'app
            beforeTransaction: (Int, Map<String, String>) -> Unit = { _, _ -> },
            afterTransaction: (Int, Song, Album, List<Artist>) -> Unit = { _, _, _, _ -> }
        ) {
            // Eseguiamo tutto direttamente sotto il pool IO dello scope globale condiviso
            externalScope.launch(Dispatchers.IO) {
                val parsedRows = mutableListOf<CsvRowData>()

                // 1. Fase di parsing sincrona e immediata: svuota il CSV in memoria e chiude il file
                try {
                    appContext().applicationContext
                        .contentResolver
                        .openInputStream(uri)
                        ?.use { inputStream ->
                            csvReader().open(inputStream) {
                                readAllWithHeaderAsSequence().forEach { row ->
                                    parsedRows.add(CsvRowData(rawRowMap = row))
                                }
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }

                // 2. Fase di elaborazione e scrittura sequenziale sul database
                parsedRows.forEachIndexed { index, rowData ->
                    println("mediaItem index song $index")
                    val row = rowData.rawRowMap

                    // Callback pre-transazione eseguito direttamente in IO (niente withContext ridondanti)
                    beforeTransaction(index, row)

                    val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                    val pseudoMediaId = (row["Track Name"].orEmpty() + row["Artist Name(s)"].orEmpty()).filter { it.isLetterOrDigit() }
                    val title = row["Title"] ?: row["Track Name"] ?: return@forEachIndexed
                    val mediaId = row["MediaId"] ?: pseudoMediaId
                    val artistsText = row["Artists"] ?: row["Artist Name(s)"] ?: ""
                    val durationText = row["Duration"] ?: formatAsDuration(row["Track Duration (ms)"]?.toLongOrNull() ?: 0L)

                    val song = Song(
                        id = mediaId,
                        title = explicitPrefix + title,
                        artistsText = artistsText,
                        durationText = durationText,
                        thumbnailUrl = row["ThumbnailUrl"] ?: "",
                        totalPlayTimeMs = 1L
                    )

                    val albumId = row["AlbumId"] ?: ""
                    val albumTitle = row["AlbumTitle"]
                    val album = Album(
                        id = albumId,
                        title = albumTitle
                    )

                    val artistNames = row["Artists"]?.split(",")
                    val artistIds = row["ArtistIds"]?.split(",")
                    val artists = mutableListOf<Artist>()
                    if (artistIds != null && (artistNames?.size == artistIds.size)) {
                        for (idx in artistIds.indices) {
                            val artistName = artistNames.getOrNull(idx)
                            val artistId = artistIds.getOrNull(idx)
                            if (artistId != null) {
                                val artist = Artist(
                                    id = artistId,
                                    name = artistName
                                )
                                artists.add(artist)
                            }
                        }
                    }

                    // Callback post-transazione eseguito sul thread IO
                    afterTransaction(index, song, album, artists)

                    // Scrittura sincrona e atomica sul DB: una riga alla volta nell'ordine corretto
                    try {
                        Database.upsert(song)
                        println("ImportSongsFromCSV inserito nel DB con successo: $title")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
