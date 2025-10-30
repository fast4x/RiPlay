package it.fast4x.riplay.extensions.ondevice

// OnDeviceViewModel.kt
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.models.OnDeviceSong
import it.fast4x.riplay.enums.OnDeviceSongSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.utils.OnDeviceBlacklist
import it.fast4x.riplay.utils.globalContext
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
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.service.LOCAL_KEY_PREFIX
import it.fast4x.riplay.utils.removeObsoleteOndeviceMusic
import kotlin.time.Duration.Companion.milliseconds

class OnDeviceViewModel(application: Application) : AndroidViewModel(application)  {
    private val context = getApplication<Application>().applicationContext

    var sortOrder: SortOrder = SortOrder.Descending
    var sortBy: OnDeviceSongSortBy = OnDeviceSongSortBy.DateAdded

    private var _audioFiles = MutableStateFlow<List<OnDeviceSong>>(emptyList())
    val audioFiles: StateFlow<List<OnDeviceSong>> = _audioFiles.asStateFlow()

    private val contentResolver: ContentResolver = context.contentResolver

    private val contentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Called when change some data in device storage, example of uri. Must be checked if exists to understand if removed or added
            // example of uri content://media/external/audio/media/1000037024
            Timber.d("OnDeviceViewModel onChange called with uri $uri and selfChange $selfChange")
            removeObsoleteOndeviceMusic(context)
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

    @SuppressLint("Range")
    fun loadAudioFiles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val collection = if (isAtLeastAndroid10) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                var projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.ALBUM,
                    if (isAtLeastAndroid10) {
                        MediaStore.Audio.Media.RELATIVE_PATH
                    } else {
                        MediaStore.Audio.Media.DATA
                    },
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.IS_MUSIC,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.DATE_MODIFIED
                )
                if (isAtLeastAndroid11)
                    projection += MediaStore.Audio.Media.BITRATE

                projection += MediaStore.Audio.Media.SIZE

                val sortOrderSQL = when (sortOrder) {
                    SortOrder.Ascending -> "ASC"
                    SortOrder.Descending -> "DESC"
                }

                val sortBySQL = when (sortBy) {
                    OnDeviceSongSortBy.Title -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE $sortOrderSQL"
                    OnDeviceSongSortBy.DateAdded -> "${MediaStore.Audio.Media.DATE_ADDED} $sortOrderSQL"
                    OnDeviceSongSortBy.Artist -> "${MediaStore.Audio.Media.ARTIST} COLLATE NOCASE $sortOrderSQL"
                    OnDeviceSongSortBy.Duration -> "${MediaStore.Audio.Media.DURATION} COLLATE NOCASE $sortOrderSQL"
                    OnDeviceSongSortBy.Album -> "${MediaStore.Audio.Media.ALBUM} COLLATE NOCASE $sortOrderSQL"
                }


                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val albumUriBase = "content://media/external/audio/albumart".toUri()
                val audioFiles = mutableListOf<OnDeviceSong>()

                contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortBySQL
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val isMusicIdx = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                        //Timber.i(" OnDeviceViewModel colums isMusicIdx $isMusicIdx")

                        val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                        //Timber.i(" OnDeviceViewModel colums idIdx $idIdx")
                        val nameIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                        //Timber.i(" OnDeviceViewModel colums nameIdx $nameIdx")
                        val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                        //Timber.i(" OnDeviceViewModel colums durationIdx $durationIdx")
                        val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                        //Timber.i(" OnDeviceViewModel colums artistIdx $artistIdx")
                        //val artistIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                        //Timber.i(" OnDeviceViewModel colums artistIdIdx $artistIdIdx")
                        val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        //Timber.i(" OnDeviceViewModel colums albumIdIdx $albumIdIdx")
                        val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                        //Timber.i(" OnDeviceViewModel colums albumIdx $albumIdx")
                        //val yearIdx = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                        //Timber.i(" OnDeviceViewModel colums yearIdx $yearIdx")
                        val relativePathIdx = if (isAtLeastAndroid10) {
                            cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                        } else {
                            cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        }
                        //Timber.i(" OnDeviceViewModel colums relativePathIdx $relativePathIdx")
                        val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                        //Timber.i(" OnDeviceViewModel colums titleIdx $titleIdx")
                        val mimeTypeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                        //Timber.i(" OnDeviceViewModel colums mimeTypeIdx $mimeTypeIdx")
                        val bitrateIdx = if (isAtLeastAndroid11) cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE) else -1
                        //Timber.i(" OnDeviceViewModel colums bitrateIdx $bitrateIdx")
                        val fileSizeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                        //Timber.i(" OnDeviceViewModel colums fileSizeIdx $fileSizeIdx")
                        val dateModifiedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                        //Timber.i(" OnDeviceViewModel colums dateModifiedIdx $dateModifiedIdx")


                        val blacklist = OnDeviceBlacklist(context = globalContext())

                        //Timber.i(" OnDeviceViewModel SDK ${Build.VERSION.SDK_INT} initialize columns complete")

                        val uri = Uri.withAppendedPath(collection, idIdx.toString())

                        val id = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx).substringBeforeLast(".")
                        val mediaId = name.substringAfterLast('[',"")
                            .substringBeforeLast(']',"").takeIf { !it.contains(" ") }
                        //Timber.i("OnDeviceViewModel name $name mediaId $mediaId")


                        val trackName = cursor.getString(titleIdx)
                        val duration = cursor.getInt(durationIdx)
                        if (duration == 0) continue
                        val artist = cursor.getString(artistIdx)
                        //val artistId = cursor.getLong(artistIdIdx)
                        val albumId = cursor.getLong(albumIdIdx)
                        val album = cursor.getString(albumIdx)
                        //val year = cursor.getInt(yearIdx)

                        val mimeType = cursor.getString(mimeTypeIdx)
                        val bitrate = if (isAtLeastAndroid11) cursor.getInt(bitrateIdx) else 0
                        val fileSize = cursor.getInt(fileSizeIdx)
                        val dateModified = cursor.getLong(dateModifiedIdx)

                        val relativePath = if (isAtLeastAndroid10) {
                            cursor.getString(relativePathIdx)
                        } else {
                            cursor.getString(relativePathIdx).substringBeforeLast("/")
                        }
                        val exclude = blacklist.contains(relativePath)
                        //Timber.d("OnDeviceViewModel trackname $trackName exclude $exclude relativePath ${relativePath}")

                        if (!exclude) {
                            runCatching {
                                val albumUri = ContentUris.withAppendedId(albumUriBase, albumId)
                                val durationText =
                                    duration.milliseconds.toComponents { minutes, seconds, _ ->
                                        "$minutes:${seconds.toString().padStart(2, '0')}"
                                    }
                                val song = OnDeviceSong(
                                    id = "$LOCAL_KEY_PREFIX$id",
                                    mediaId = mediaId,
                                    title = trackName ?: name,
                                    artistsText = artist,
                                    durationText = durationText,
                                    thumbnailUrl = albumUri.toString(),
                                    relativePath = relativePath
                                )
                                Database.upsert(
                                    song.toSong(),
                                    Format(
                                        songId = song.id,
                                        itag = 0,
                                        mimeType = mimeType,
                                        bitrate = bitrate.toLong(),
                                        contentLength = fileSize.toLong(),
                                        lastModified = dateModified
                                    )
                                )

                                Database.upsert(
                                    Album(
                                        id = "$LOCAL_KEY_PREFIX${albumId}",
                                        title = album,
                                        thumbnailUrl = albumUri.toString(),
                                        year = null,
                                        authorsText = artist,
                                        shareUrl = null,
                                        timestamp = dateModified
                                    ),
                                    SongAlbumMap(
                                        songId = song.id,
                                        albumId = "$LOCAL_KEY_PREFIX${albumId}",
                                        position = 0
                                    )
                                )

                                Database.upsert(
                                    Artist(
                                        id = "$LOCAL_KEY_PREFIX${artist}",
                                        name = artist,
                                        thumbnailUrl = albumUri.toString(),
                                        timestamp = dateModified
                                    ),
                                    SongArtistMap(
                                        songId = song.id,
                                        artistId = "$LOCAL_KEY_PREFIX${artist}"
                                    )
                                )

                                audioFiles.add(
                                    song
                                )
                                Timber.d("OnDeviceViewModel updated and added song ${song.title} and songId ${song.id}")
                            }.onFailure {
                                Timber.e("OnDeviceViewModel addSong error ${it.stackTraceToString()}")
                            }

                        }

                    }
                }
                audioFiles
            }.let {
                _audioFiles.value = it
                Timber.d("OnDeviceViewModel audioList inside size audioFiles ${_audioFiles.value.size} ")
            }

        }
    }
}