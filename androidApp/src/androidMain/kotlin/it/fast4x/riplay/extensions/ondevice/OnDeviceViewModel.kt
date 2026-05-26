package it.fast4x.riplay.extensions.ondevice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.enums.OnDeviceSongSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.utils.OnDeviceBlacklist
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isAtLeastAndroid11
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ON_DEVICE_PLAYLIST
import it.fast4x.riplay.utils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.utils.isAtLeastAndroid13
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class OnDeviceViewModel(application: Application) : AndroidViewModel(application),
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnDeviceViewModel::class.java)) {
            val application = getApplication<Application>()
            @Suppress("UNCHECKED_CAST")
            return OnDeviceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    var sortOrder: SortOrder = SortOrder.Descending
    var sortBy: OnDeviceSongSortBy = OnDeviceSongSortBy.DateAdded

    private var _audioFiles = MutableStateFlow<List<Song>>(emptyList())
    val audioFiles: StateFlow<List<Song>> = _audioFiles.asStateFlow()

    private var _audioFolders = MutableStateFlow<List<String>>(emptyList())
    val audioFolders: StateFlow<List<String>> = _audioFolders.asStateFlow()

    private val contentResolver: ContentResolver = application.applicationContext.contentResolver

    private val contentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Called when change some data in device storage, example of uri. Must be checked if exists to understand if removed or added
            // example of uri content://media/external/audio/media/1000037024
            Timber.d("OnDeviceViewModel onChange called with uri $uri and selfChange $selfChange")
            removeObsoleteOndeviceMusic(application.applicationContext)
            loadAudioFiles()
        }
    }

    init {
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
        loadAudioFiles()
    }

    override fun onCleared() {
        super.onCleared()
        contentResolver.unregisterContentObserver(contentObserver)
    }

    fun loadAudioFiles() {
        val showOndevice = application.applicationContext.preferences
            .getBoolean(SHOW_ON_DEVICE_PLAYLIST.key, true)

        Timber.d("OnDeviceViewModel loadAudioFiles called showOndevice $showOndevice")
        if (!showOndevice) return

        val hasPermission = ContextCompat.checkSelfPermission(
            application.applicationContext,
            if (isAtLeastAndroid13) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                runCatching {
                    loadAudioFilesInternal()
                }.onFailure {
                    Timber.e("OnDeviceViewModel loadAudioFiles Error: ${it.stackTraceToString()}")
                }.getOrElse { emptyList() }
            }

            _audioFiles.value = songs
            _audioFolders = MutableStateFlow(
                songs.mapNotNull { it.folder }.distinct()
            )
            Timber.d("OnDeviceViewModel audioList size: ${songs.size}")
        }
    }

    @SuppressLint("Range")
    private fun loadAudioFilesInternal(): List<Song> {
        val collection = if (isAtLeastAndroid10)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        var projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            if (isAtLeastAndroid10) MediaStore.Audio.Media.RELATIVE_PATH
            else MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE
        )
        if (isAtLeastAndroid11) projection += MediaStore.Audio.Media.BITRATE

        val sortBySQL = buildSortSQL()
        val albumUriBase = "content://media/external/audio/albumart".toUri()
        val blacklist = OnDeviceBlacklist() // ← istanziata UNA volta sola
        val audioFiles = mutableListOf<Song>()

        contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            sortBySQL
        )?.use { cursor ->

            // ← tutti i getColumnIndex FUORI dal loop
            val idIdx          = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val nameIdx        = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val artistIdx      = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumIdIdx     = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val albumIdx       = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val relativePathIdx = if (isAtLeastAndroid10)
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            else
                cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val titleIdx       = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val mimeTypeIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val bitrateIdx     = if (isAtLeastAndroid11)
                cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE) else -1
            val fileSizeIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dateModifiedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val duration = cursor.getInt(durationIdx)
                if (duration == 0) continue

                val id   = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)?.substringBeforeLast(".") ?: "Unknown"
                val mediaId = name.substringAfterLast('[', "")
                    .substringBeforeLast(']', "").takeIf { !it.contains(" ") }

                val relativePath = if (isAtLeastAndroid10)
                    cursor.getString(relativePathIdx) ?: ""
                else
                    cursor.getString(relativePathIdx)?.substringBeforeLast("/") ?: ""

                val exclude = blacklist.checkIf(relativePath)
                        || blacklist.checkIf("/$relativePath")
                        || blacklist.checkIf(mediaId ?: "")
                        || relativePath.contains("WhatsApp")

                if (exclude) continue

                val trackName    = cursor.getString(titleIdx)
                val artist       = cursor.getString(artistIdx)
                val albumId      = cursor.getLong(albumIdIdx)
                val album        = cursor.getString(albumIdx)
                val mimeType     = cursor.getString(mimeTypeIdx)
                val bitrate      = if (isAtLeastAndroid11) cursor.getInt(bitrateIdx) else 0
                val fileSize     = cursor.getInt(fileSizeIdx)
                val dateModified = cursor.getLong(dateModifiedIdx)
                val songId       = "$LOCAL_KEY_PREFIX$id"
                val albumUri     = ContentUris.withAppendedId(albumUriBase, albumId)
                val durationText = duration.milliseconds.toComponents { minutes, seconds, _ ->
                    "$minutes:${seconds.toString().padStart(2, '0')}"
                }
                val uri = Uri.withAppendedPath(collection, id.toString())

                runCatching {
                    val song = Song(
                        id = songId,
                        mediaId = mediaId,
                        title = trackName ?: name,
                        artistsText = artist,
                        durationText = durationText,
                        thumbnailUrl = albumUri.toString(),
                        folder = relativePath
                    )

                    Database.upsert(song, Format(
                        songId = song.id,
                        itag = 0,
                        mimeType = mimeType,
                        bitrate = bitrate.toLong(),
                        contentLength = fileSize.toLong(),
                        lastModified = dateModified
                    ))
                    Database.upsert(
                        Album(
                            id = "$LOCAL_KEY_PREFIX$albumId",
                            title = album,
                            thumbnailUrl = albumUri.toString(),
                            year = null,
                            authorsText = artist,
                            shareUrl = null,
                            timestamp = dateModified
                        ),
                        SongAlbumMap(songId = song.id, albumId = "$LOCAL_KEY_PREFIX$albumId", position = 0)
                    )
                    Database.upsert(
                        Artist(
                            id = "$LOCAL_KEY_PREFIX$artist",
                            name = artist,
                            thumbnailUrl = albumUri.toString(),
                            timestamp = dateModified
                        ),
                        SongArtistMap(songId = song.id, artistId = "$LOCAL_KEY_PREFIX$artist")
                    )

                    audioFiles.add(song)
                    Timber.d("OnDeviceViewModel added: ${song.title} [${song.id}]")
                }.onFailure {
                    Timber.e("OnDeviceViewModel addSong error: ${it.stackTraceToString()}")
                }
            }
        }

        return audioFiles
    }

    private fun buildSortSQL(): String {
        val order = when (sortOrder) {
            SortOrder.Ascending -> "ASC"
            SortOrder.Descending -> "DESC"
        }
        return when (sortBy) {
            OnDeviceSongSortBy.Title     -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE $order"
            OnDeviceSongSortBy.DateAdded -> "${MediaStore.Audio.Media.DATE_ADDED} $order"
            OnDeviceSongSortBy.Artist    -> "${MediaStore.Audio.Media.ARTIST} COLLATE NOCASE $order"
            OnDeviceSongSortBy.Duration  -> "${MediaStore.Audio.Media.DURATION} $order"
            OnDeviceSongSortBy.Album     -> "${MediaStore.Audio.Media.ALBUM} COLLATE NOCASE $order"
        }
    }

    fun removeObsoleteOndeviceMusic(
        context: Context
    ) {
        try {
            var version: String? = null

            viewModelScope.launch {
                while (currentCoroutineContext().isActive) {

                    val newVersion = runCatching {
                        if (isAtLeastAndroid10)
                            MediaStore.getVersion(context, MediaStore.VOLUME_EXTERNAL)
                        else
                            MediaStore.getVersion(context)
                    }.onFailure {
                        Timber.w("removeObsoleteOndeviceMusic: MediaStore non disponibile, retry tra 5s: ${it.message}")
                    }.getOrNull()

                    // Se il volume non è pronto, skippa e riprova dopo
                    if (newVersion == null) {
                        delay(5.seconds)
                        continue
                    }

                    if (version != newVersion) {
                        version = newVersion

                        val collection =
                            if (isAtLeastAndroid10) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                        val projection = arrayOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.IS_MUSIC
                        )


                        context.contentResolver.query(collection, projection, null, null, null)
                            ?.use { cursor ->
                                val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                                val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                                val albumIdIdx =
                                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                                val isMusicIdx =
                                    cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)

                                Timber.i(" DeviceListSongs SDK ${Build.VERSION.SDK_INT} initialize columns complete in removeObsoleteOndeviceMusic")

                                val ondeviceSongsList = mutableListOf<String>()
                                val ondeviceAlbumsList = mutableListOf<String>()
                                val ondeviceArtistsList = mutableListOf<String>()

                                Timber.i(" DeviceListSongs removeObsoleteOndeviceMusic start")


                                while (cursor.moveToNext()) {
                                    if (cursor.getInt(isMusicIdx) == 0) continue
                                    val songId = cursor.getLong(idIdx)
                                    val albumId = cursor.getLong(albumIdIdx)
                                    val artistId = cursor.getString(artistIdx)

                                    val ondeviceSongId = "$LOCAL_KEY_PREFIX$songId"
                                    val ondeviceAlbumId = "$LOCAL_KEY_PREFIX${albumId}"
                                    val ondeviceArtistId = "$LOCAL_KEY_PREFIX${artistId}"

                                    if (!ondeviceSongsList.contains(ondeviceSongId))
                                        ondeviceSongsList.add(ondeviceSongId)
                                    if (!ondeviceAlbumsList.contains(ondeviceAlbumId))
                                        ondeviceAlbumsList.add(ondeviceAlbumId)
                                    if (!ondeviceArtistsList.contains(ondeviceArtistId))
                                        ondeviceArtistsList.add(ondeviceArtistId)
                                }
                                Timber.i(" DeviceListSongs removeObsoleteOndeviceMusic cursor complete")

                                Timber.d("DeviceListSongs removeObsoleteOndeviceMusic cursor complete ondeviceSongsList ${ondeviceSongsList.size}")
                                Timber.d("DeviceListSongs removeObsoleteOndeviceMusic cursor complete ondeviceAlbumsList ${ondeviceAlbumsList.size}")
                                Timber.d("DeviceListSongs removeObsoleteOndeviceMusic cursor complete ondeviceArtistsList ${ondeviceArtistsList.size}")


                                runCatching {
                                    Database.songsOnDevice().collect { songs ->
                                        songs.forEach {
                                            if (!ondeviceSongsList.contains(it.id)) {
                                                Database.deleteFormat(it.id)
                                                Database.delete(it)
                                            }
                                        }
                                    }
                                    Timber.d("DeviceListSongs removeObsoleteOndeviceMusic deleteSongs complete")
                                }.onFailure {
                                    Timber.e("DeviceListSongs removeObsoleteOndeviceMusic deleteSongs error ${it.stackTraceToString()}")
                                }
                                runCatching {
                                    Database.albumsOnDeviceByRowIdAsc().collect { albums ->
                                        albums.forEach {
                                            if (!ondeviceAlbumsList.contains(it.id)) {
                                                Database.deleteAlbumMap(it.id)
                                                Database.delete(it)
                                            }
                                        }
                                    }
                                    Timber.d("DeviceListSongs removeObsoleteOndeviceMusic deleteAlbums complete")
                                }.onFailure {
                                    Timber.e("DeviceListSongs removeObsoleteOndeviceMusic deleteAlbums error ${it.stackTraceToString()}")
                                }
                                runCatching {
                                    Database.artistsOnDeviceByRowIdAsc().collect { artists ->
                                        artists.forEach {
                                            if (!ondeviceArtistsList.contains(it.id)) {
                                                Database.deleteArtistMap(it.id)
                                                Database.delete(it)
                                            }
                                        }
                                    }
                                    Timber.d("DeviceListSongs removeObsoleteOndeviceMusic deleteArtists complete")
                                }.onFailure {
                                    Timber.e("DeviceListSongs removeObsoleteOndeviceMusic deleteArtists error ${it.stackTraceToString()}")
                                }

                            }

                    }

                    runCatching {
                        delay(5.seconds)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("OnDeviceViewModel removeObsoleteOndeviceMusic error: ${e.message}")
        }
    }

    fun audioFoldersAsPlaylists(): Flow<MutableList<PlaylistPreview>> {
        loadAudioFiles()
        val _playlists = mutableListOf<PlaylistPreview>()
        _audioFolders.value.forEachIndexed { index, folder ->

            val totalPlayTimeMs = mutableStateOf(0L)
            CoroutineScope(Dispatchers.IO).launch {
                Database.getSongsTotalPlaytime(_audioFiles.value.filter { it.folder == folder }.map { it.id }).collect {
                    totalPlayTimeMs.value = it
                    //Timber.d("OnDeviceViewModel audioFoldersAsPlaylists totalPlayTimeMs ${totalPlayTimeMs.value}")
                }
            }

            //Timber.d("OnDeviceViewModel audioFoldersAsPlaylists folder $folder totalPlayTimeMs ${totalPlayTimeMs.value}")
            val playlist = PlaylistPreview(
                playlist = Playlist(
                    id = index.toLong(),
                    name = folder.substringBeforeLast("/"),
                    browseId = null,
                    isYoutubePlaylist = false,
                    isEditable = false
                ),
                songCount = _audioFiles.value.filter { it.folder == folder }.size,
                totalPlayTimeMs = totalPlayTimeMs.value,
                isOnDevice = true,
                folder = folder
            )
            _playlists += playlist
        }
        return flowOf(_playlists)
    }

    fun audioFilesFromFolder(folder: String): Flow<List<SongEntity>> {
        Timber.d("OnDeviceViewModel audioFilesFromFolder folder $folder ${_audioFiles.value.size}")
        return flowOf(_audioFiles.value.filter { it.folder == folder }.map { it.toSongEntity() } )
    }

}
