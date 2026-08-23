package it.fast4x.riplay.ui.components.tab

import android.content.ActivityNotFoundException
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportSongsFromDeezerCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
): Descriptive, MenuIcon {

    private data class CsvRowData(
        val isrc: String,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val rawRowMap: Map<String, String>
    )

    companion object {
        private fun openFile(
            uri: Uri,
            externalScope: CoroutineScope, // Riceve lo scope persistente
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _,_,_ -> },
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) {
            // Avviamo il lavoro sullo scope dell'applicazione
            externalScope.launch(Dispatchers.IO) {
                val context = appContext()
                val fileName = context.getFileNameFromUri(uri)

                val parsedRows = mutableListOf<CsvRowData>()
                try {
                    context.applicationContext
                        .contentResolver
                        .openInputStream(uri)
                        ?.use { inputStream ->
                            csvReader().open(inputStream) {
                                readAllWithHeaderAsSequence().forEach { row ->
                                    val isrc = row["ISRC"]
                                    val trackName = row["Track name"]
                                    if (isrc != null && trackName != null) {
                                        parsedRows.add(
                                            CsvRowData(
                                                isrc = isrc,
                                                trackName = trackName,
                                                artistName = row["Artist name"] ?: "",
                                                albumName = row["Album"] ?: "",
                                                rawRowMap = row
                                            )
                                        )
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }

                // Ciclo di elaborazione protetto
                parsedRows.forEachIndexed { index, rowData ->
                    val songId = "$DEEZER_TRACK_KEY_PREFIX${rowData.isrc}"

                    var song = Song(
                        id = songId,
                        title = rowData.trackName,
                        artistsText = rowData.artistName,
                        durationText = "0:00",
                        thumbnailUrl = null,
                        totalPlayTimeMs = 1L
                    )

                    val album = Album(id = "", title = rowData.albumName)
                    val artists = listOf(Artist(id = "", name = rowData.artistName))

                    beforeTransaction(index, rowData.rawRowMap, fileName)


                    // Chiamata di rete Deezer
                    val trackInfo = Environment.deezerTrackInfo(rowData.isrc).getOrNull()
                    trackInfo?.let { info ->
                        song = song.copy(
                            thumbnailUrl = info.album.coverXl.toString(),
                            durationText = formatAsDuration(info.duration * 1000L)
                        )
                    }

                    try {
                        afterTransaction(index, song, album, artists)

                        Database.upsert(song)
                        println("ImportPlaylist in background inserito nel DB: ${rowData.trackName}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                openFile(uri,  appScopeIO, beforeTransaction, afterTransaction)
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
