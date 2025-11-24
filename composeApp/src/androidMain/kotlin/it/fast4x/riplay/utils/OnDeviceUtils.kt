package it.fast4x.riplay.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import it.fast4x.riplay.commonutils.durationToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.OnDeviceFolderSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.data.models.Folder
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.OnDeviceBlacklistPath
import it.fast4x.riplay.data.models.OnDeviceSong
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.enums.OnDeviceSongSortBy
import it.fast4x.riplay.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.collections.plus
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class OnDeviceOrganize {
    companion object {
        fun sortSongs(sortOrder: SortOrder, sortBy: OnDeviceFolderSortBy, songs: List<SongEntity>): List<SongEntity> {
            return when (sortBy) {
                OnDeviceFolderSortBy.Title -> {
                    if (sortOrder == SortOrder.Ascending)
                        songs.sortedBy { it.song.title }
                    else
                        songs.sortedByDescending { it.song.title }
                }
                OnDeviceFolderSortBy.Artist -> {
                    if (sortOrder == SortOrder.Ascending)
                        songs.sortedBy { it.song.artistsText }
                    else
                        songs.sortedByDescending { it.song.artistsText }
                }
                OnDeviceFolderSortBy.Duration -> {
                    if (sortOrder == SortOrder.Ascending)
                        songs.sortedBy { durationToMillis(it.song.durationText ?: "0:00") }
                    else
                        songs.sortedByDescending {
                            durationToMillis(
                                it.song.durationText ?: "0:00"
                            )
                        }
                }
            }
        }
        fun organizeSongsIntoFolders(songs: List<OnDeviceSong>): Folder {
            val rootFolder = Folder("/", fullPath = "/")

            for (song in songs) {
                if (song.relativePath == "/") {
                    rootFolder.addSong(song)
                }
                else {
                    val pathSegments = song.relativePath.split('/')
                    var currentFolder = rootFolder
                    var currentFullPath = ""

                    for (segment in pathSegments) {
                        if (segment.isNotBlank()) {
                            currentFullPath += "/$segment"
                            val existingFolder = currentFolder.subFolders.find { it.name == segment }
                            currentFolder = if (existingFolder != null) {
                                existingFolder
                            } else {
                                val newFolder = Folder(name = segment, parent = currentFolder, fullPath = currentFullPath + "/")
                                currentFolder.addSubFolder(newFolder)
                                newFolder
                            }
                        }
                    }

                    currentFolder.addSong(song)
                }
            }

            return rootFolder
        }

        fun getFolderByPath(rootFolder: Folder, path: String): Folder? {
            if (path == "/") {
                return rootFolder;
            }

            val pathSegments = path.trim('/').split('/')

            var currentFolder = rootFolder

            for (segment in pathSegments) {
                val folder = currentFolder.subFolders.find { it.name == segment }

                if (folder != null) {
                    currentFolder = folder
                } else {
                    return null
                }
            }

            return currentFolder
        }
        fun logFolderStructure(folder: Folder, indent: String = "") {
            Log.d("FolderStructure", "$indent Folder: ${folder.name}")

            // Log songs in the current folder
            for (song in folder.songs) {
                Log.d("FolderStructure", "$indent - Song: ${song.title} - Relative Path: ${song.relativePath}")
            }

            // Recursively log subfolders
            for (subFolder in folder.subFolders) {
                logFolderStructure(subFolder, "$indent    ")
            }
        }
    }
}

class OnDeviceBlacklist(context: Context) {
    var paths: List<OnDeviceBlacklistPath> = emptyList()

    init {
        val file = File(context.filesDir, "Blacklisted_paths.txt")
        paths = if (file.exists()) {
            file.readLines().map { OnDeviceBlacklistPath(path = it) }
        } else {
            emptyList()
        }
    }

    fun contains(path: String): Boolean {
        return paths.any { it.test(path) }
    }
}



private val mediaScope = CoroutineScope(Dispatchers.IO + CoroutineName("MediaStore worker"))
fun Context.musicFilesAsFlow(
    sortBy: OnDeviceSongSortBy = OnDeviceSongSortBy.DateAdded,
    order: SortOrder = SortOrder.Descending,
    context: Context
): StateFlow<List<OnDeviceSong>> = flow {
    var version: String? = null

    while (currentCoroutineContext().isActive) {
        val newVersion = MediaStore.getVersion(applicationContext)
        if (version != newVersion) {
            version = newVersion

            val collection =
                if (isAtLeastAndroid10) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

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

            val sortOrderSQL = when (order) {
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

            val albumUriBase = Uri.parse("content://media/external/audio/albumart")

            contentResolver.query(collection, projection, null, null, sortBySQL)
                ?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    Timber.i(" DeviceListSongs colums idIdx $idIdx")
                    val nameIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    Timber.i(" DeviceListSongs colums nameIdx $nameIdx")
                    val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    Timber.i(" DeviceListSongs colums durationIdx $durationIdx")
                    val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    Timber.i(" DeviceListSongs colums artistIdx $artistIdx")
                    //val artistIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                    //Timber.i(" DeviceListSongs colums artistIdIdx $artistIdIdx")
                    val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    Timber.i(" DeviceListSongs colums albumIdIdx $albumIdIdx")
                    val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    Timber.i(" DeviceListSongs colums albumIdx $albumIdx")
                    //val yearIdx = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                    //Timber.i(" DeviceListSongs colums yearIdx $yearIdx")
                    val relativePathIdx = if (isAtLeastAndroid10) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    } else {
                        cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    }
                    Timber.i(" DeviceListSongs colums relativePathIdx $relativePathIdx")
                    val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    Timber.i(" DeviceListSongs colums titleIdx $titleIdx")
                    val isMusicIdx = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                    Timber.i(" DeviceListSongs colums isMusicIdx $isMusicIdx")
                    val mimeTypeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    Timber.i(" DeviceListSongs colums mimeTypeIdx $mimeTypeIdx")
                    val bitrateIdx = if (isAtLeastAndroid11) cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE) else -1
                    Timber.i(" DeviceListSongs colums bitrateIdx $bitrateIdx")
                    val fileSizeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                    Timber.i(" DeviceListSongs colums fileSizeIdx $fileSizeIdx")
                    val dateModifiedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                    Timber.i(" DeviceListSongs colums dateModifiedIdx $dateModifiedIdx")


                    val blacklist = OnDeviceBlacklist(context = context)

                    Timber.i(" DeviceListSongs SDK ${Build.VERSION.SDK_INT} initialize columns complete")

                    buildList {
                        while (cursor.moveToNext()) {
                            if (cursor.getInt(isMusicIdx) == 0) continue
                            val id = cursor.getLong(idIdx)
                            val name = cursor.getString(nameIdx).substringBeforeLast(".")
                            val mediaId = name.substringAfterLast('[',"")
                                .substringBeforeLast(']',"").takeIf { !it.contains(" ") }
                            Timber.i("DeviceListSongs name $name mediaId $mediaId")


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
                            //println("DeviceListSongs trackname $trackName exclude $exclude relativePath ${relativePath}")

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
                                    Database.insert(
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

                                    Database.insert(
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

                                    Database.insert(
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
                                    //println("DeviceListSongs song ${song.id} album $LOCAL_KEY_PREFIX${albumId} artist $LOCAL_KEY_PREFIX${artist}")
                                    add(
                                        song
                                    )
                                }.onFailure {
                                    Timber.e("DeviceListSongs addSong error ${it.stackTraceToString()}")
                                }
                            }
                        }
                    }
                }?.let {
                    runCatching {
                        emit(it)
                    }.onFailure {
                        Timber.e("DeviceListSongs emit error ${it.stackTraceToString()}")
                    }
                }
        }
        runCatching {
            delay(5.seconds)
        }
    }
}.distinctUntilChanged()
    .stateIn(mediaScope, SharingStarted.Eagerly, listOf())


fun removeObsoleteOndeviceMusic(
    context: Context
) {
    var version: String? = null

    mediaScope.launch {
        while (currentCoroutineContext().isActive) {
            val newVersion = MediaStore.getVersion(context)
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
                        val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        val isMusicIdx = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)

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
}