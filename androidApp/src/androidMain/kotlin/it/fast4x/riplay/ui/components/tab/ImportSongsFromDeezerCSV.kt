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

    // Intended to import deezer playlist from csv exported by https://www.tunemymusic.com

    companion object {
        private fun openFile(
            uri: Uri,
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _,_,_ -> },
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) {
            val context = appContext()
            val fileName = context.getFileNameFromUri(uri)
            context.applicationContext
                .contentResolver
                .openInputStream(uri)
                ?.use { inputStream ->

                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().forEachIndexed { index, row: Map<String, String> ->
                            println("ImportSongsFromDeezerCSV index song $index rowMap = $row")

                            Database.asyncTransaction {
                                beforeTransaction( index, row, fileName )

                                // Rilevamento del formato: controlliamo se esiste "ISRC" (Deezer)
                                if (!row.containsKey("ISRC")) return@asyncTransaction

                                val song: Song
                                val album: Album
                                val artists: List<Artist>

                                val trackISRC = row["ISRC"]
                                val songId = "$DEEZER_TRACK_KEY_PREFIX$trackISRC"

                                val title = row["Track name"] ?: return@asyncTransaction

                                val artistsText = row["Artist name"] ?: ""

                                val durationText = "0:00"

                                song = Song(
                                    id = songId,
                                    title = title,
                                    artistsText = artistsText,
                                    durationText = durationText,
                                    thumbnailUrl = null,
                                    totalPlayTimeMs = 1L
                                )

                                // Album
                                val albumTitle = row["Album"]
                                album = Album(
                                    id = "",
                                    title = albumTitle
                                )

                                // Artista
                                artists = listOf(Artist(
                                        id = "",
                                        name = artistsText
                                    ))

                                afterTransaction( index, song, album, artists )

                                // 3. Recupero della info in parallelo
                                trackISRC?.let { id ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val trackInfo = Environment.deezerTrackInfo(id).getOrNull()

                                        trackInfo?.let { info ->
                                            val songToUpdate = song.copy(
                                                thumbnailUrl = info.album.coverXl.toString(),
                                                durationText = formatAsDuration(info.duration * 1000L)
                                            )
                                            println("ImportPlaylist info trovate per $title Aggiorno DB con ID: $trackISRC")
                                            Database.upsert(songToUpdate)
                                            //Database.updateSongThumbnail(songId, info.album.coverXl.toString())
                                        }
                                    }
                                }



                            }
                        }
                    }
                }
        }

        @JvmStatic
        @Composable
        fun init(
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _,_,_ ->},
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) = ImportSongsFromDeezerCSV(
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if( uri == null ) return@rememberLauncherForActivityResult

                openFile( uri, beforeTransaction, afterTransaction )
            }
        )
    }

    override val messageId: Int = R.string.import_playlist
    override val iconId: Int = R.drawable.resource_import
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override fun onShortClick() {
        try {
            launcher.launch( arrayOf("text/csv", "text/comma-separated-values") )
        } catch (_: ActivityNotFoundException) {
            SmartMessage(
                appContext().resources.getString( R.string.info_not_find_app_open_doc ),
                type = PopupType.Warning, context = appContext()
            )
        }
    }
}