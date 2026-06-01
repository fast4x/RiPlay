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
import it.fast4x.riplay.utils.formatAsDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportSongsFromSpotifyCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
): Descriptive, MenuIcon {

    companion object {
        private fun openFile(
            uri: Uri,
            beforeTransaction: (Int, Map<String, String>) -> Unit = { _,_ -> },
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) {
            appContext().applicationContext
                .contentResolver
                .openInputStream(uri)
                ?.use { inputStream ->
                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().forEachIndexed { index, row: Map<String, String> ->
                            println("mediaItem index song $index")

                            Database.asyncTransaction {
                                beforeTransaction( index, row )

                                // Rilevamento del formato: controlliamo se esiste "Track URI" (Spotify)
                                val isSpotifyFormat = row.containsKey("Track URI")

                                val song: Song
                                val album: Album
                                val artists: List<Artist>

                                if (isSpotifyFormat) {

                                    val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""

                                    // Usa Track URI come ID, o niente
                                    val mediaId = row["Track URI"] ?: return@asyncTransaction

                                    val title = row["Track Name"] ?: return@asyncTransaction

                                    // Gestione Artisti: Spotify usa "Artist Name(s)"
                                    val artistsText = row["Artist Name(s)"] ?: ""

                                    // Gestione Durata: Spotify usa "Duration (ms)"
                                    val durationText = formatAsDuration(row["Duration (ms)"]?.toLong() ?: 0L)

                                    val spotifyTrackId = row["Track URI"]?.split(":")?.last()

                                    song = Song(
                                        id = mediaId,
                                        title = explicitPrefix + title,
                                        artistsText = artistsText,
                                        durationText = durationText,
                                        thumbnailUrl = null,
                                        totalPlayTimeMs = 1L
                                    )

                                    // Album
                                    val albumTitle = row["Album Name"]
                                    album = Album(
                                        id = "",
                                        title = albumTitle
                                    )

                                    // Artisti
                                    val artistNames = row["Artist Name(s)"]?.split(",")
                                    artists = artistNames?.map { name ->
                                        Artist(
                                            id = "",
                                            name = name.trim()
                                        )
                                    } ?: mutableListOf()

                                    afterTransaction( index, song, album, artists )

                                    // 3. Recupero della copertina in parallelo
                                    spotifyTrackId?.let { id ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val url = Environment.spotifyThumbnail(id).getOrNull()

                                            if (!url.isNullOrEmpty()) {
                                                println("ImportPlaylist Copertina trovata per $title: $url. Aggiorno DB con ID: $mediaId")
                                                Database.updateSongThumbnail(mediaId, url)
                                            }
                                        }
                                    }

                                } else {

                                    val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                                    val pseudoMediaId = (row["Track Name"]+row["Artist Name(s)"]).filter { it.isLetterOrDigit() }
                                    val mediaId = row["MediaId"] ?: pseudoMediaId
                                    val title = row["Title"] ?: row["Track Name"] ?: return@asyncTransaction
                                    val artistsText = row["Artists"] ?: row["Artist Name(s)"] ?: ""

                                    // Tenta prima la colonna "Duration" (testo), poi "Track Duration (ms)"
                                    val durationText = row["Duration"] ?: formatAsDuration(row["Track Duration (ms)"]?.toLong() ?: 0L)

                                    song = Song(
                                        id = mediaId,
                                        title = explicitPrefix+title,
                                        artistsText = artistsText,
                                        durationText = durationText,
                                        thumbnailUrl = row["ThumbnailUrl"] ?: "",
                                        totalPlayTimeMs = 1L
                                    )

                                    val albumId = row["AlbumId"] ?: ""
                                    val albumTitle = row["AlbumTitle"]
                                    album = Album(
                                        id = albumId,
                                        title = albumTitle
                                    )

                                    val artistNames = row["Artists"]?.split(",")
                                    val artistIds = row["ArtistIds"]?.split(",")
                                    val mutableArtists = mutableListOf<Artist>()
                                    if (artistIds != null && (artistNames?.size == artistIds.size)) {
                                        for(idx in artistIds.indices){
                                            val artistName = artistNames.getOrNull(idx)
                                            val artistId = artistIds.getOrNull(idx)
                                            if(artistId!=null){
                                                val artist = Artist(
                                                    id = artistId,
                                                    name = artistName
                                                )
                                                mutableArtists.add(artist)
                                            }
                                        }
                                    }
                                    artists = mutableArtists

                                    afterTransaction( index, song, album, artists )
                                }

                            }
                        }
                    }
                }
        }

        @JvmStatic
        @Composable
        fun init(
            beforeTransaction: (Int, Map<String, String>) -> Unit = { _,_ -> },
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) = ImportSongsFromSpotifyCSV(
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