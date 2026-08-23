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
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getFileNameFromUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportSongsFromSpotifyCSV private constructor(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
): Descriptive, MenuIcon {

    // Struttura dati per svuotare il CSV in modo sincrono e sicuro
    private data class CsvRowData(
        val rawRowMap: Map<String, String>
    )

    companion object {
        private fun openFile(
            uri: Uri,
            externalScope: CoroutineScope, // Sfrutta lo scope persistente a livello applicazione
            beforeTransaction: (Int, Map<String, String>, String?) -> Unit = { _,_,_ -> },
            afterTransaction: ( Int, Song, Album, List<Artist> ) -> Unit = { _,_,_,_ -> }
        ) {
            externalScope.launch(Dispatchers.IO) {
                val context = appContext()
                val fileName = context.getFileNameFromUri(uri)

                val parsedRows = mutableListOf<CsvRowData>()

                // 1. Fase di parsing sincrona e rapida: chiude l'inputStream immediatamente
                try {
                    context.applicationContext
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

                // 2. Fase asincrona e sequenziale sul pool di thread IO globale
                parsedRows.forEachIndexed { index, rowData ->
                    val row = rowData.rawRowMap

                    beforeTransaction(index, row, fileName)

                    // Rilevamento del formato: controlliamo se esiste "Track URI" (Spotify)
                    val isSpotifyFormat = row.containsKey("Track URI")

                    var song: Song? = null
                    var album: Album? = null
                    var artists: List<Artist> = emptyList()
                    var spotifyTrackId: String? = null

                    if (isSpotifyFormat) {
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""

                        // Usa Track URI come ID, o niente
                        val mediaId = row["Track URI"] ?: return@forEachIndexed
                        val title = row["Track Name"] ?: return@forEachIndexed

                        // Gestione Artisti: Spotify usa "Artist Name(s)"
                        val artistsText = row["Artist Name(s)"] ?: ""

                        // Gestione Durata: Spotify usa "Duration (ms)"
                        val durationText = formatAsDuration(row["Duration (ms)"]?.toLongOrNull() ?: 0L)

                        spotifyTrackId = row["Track URI"]?.split(":")?.last()

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
                        } ?: emptyList()

                    } else {
                        val explicitPrefix = if (row["Explicit"] == "true") "e:" else ""
                        val pseudoMediaId = (row["Track Name"].orEmpty() + row["Artist Name(s)"].orEmpty()).filter { it.isLetterOrDigit() }
                        val mediaId = row["MediaId"] ?: pseudoMediaId
                        val title = row["Title"] ?: row["Track Name"] ?: return@forEachIndexed
                        val artistsText = row["Artists"] ?: row["Artist Name(s)"] ?: ""

                        // Tenta prima la colonna "Duration" (testo), poi "Track Duration (ms)"
                        val durationText = row["Duration"] ?: formatAsDuration(row["Track Duration (ms)"]?.toLongOrNull() ?: 0L)

                        song = Song(
                            id = mediaId,
                            title = explicitPrefix + title,
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
                            for (idx in artistIds.indices) {
                                val artistName = artistNames.getOrNull(idx)
                                val artistId = artistIds.getOrNull(idx)
                                if (artistId != null) {
                                    mutableArtists.add(
                                        Artist(
                                            id = artistId,
                                            name = artistName
                                        )
                                    )
                                }
                            }
                        }
                        artists = mutableArtists
                    }

                    // 3. Chiamata di rete 'suspend' integrata nel flusso lineare (Solo se formato Spotify)
                    if (isSpotifyFormat && spotifyTrackId != null) {
                        val url = Environment.spotifyThumbnail(spotifyTrackId).getOrNull()
                        if (!url.isNullOrEmpty() && song != null) {
                            song = song.copy(thumbnailUrl = url)
                            println("ImportPlaylist Copertina integrata per ${song.title}: $url")
                        }
                    }

                    // Finalizzazione transazione e scrittura sincrona nel DB
                    if (song != null && album != null) {
                        afterTransaction(index, song, album, artists)

                        try {
                            Database.upsert(song)
                            println("ImportSongsFromSpotifyCSV inserito nel DB con successo: ${song.title}")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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
