package it.fast4x.riplay.utils

import android.content.Context
import android.util.Log
import it.fast4x.riplay.commonutils.durationToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.OnDeviceFolderSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.ondevice.Folder
import it.fast4x.riplay.extensions.ondevice.OnDeviceBlacklistPath
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.enums.BlacklistType
import kotlinx.coroutines.runBlocking
import timber.log.Timber

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
        fun organizeSongsIntoFolders(songs: List<Song>): Folder {
            val rootFolder = Folder("/", fullPath = "/")

            for (song in songs) {
                if (song.folder == "/") {
                    rootFolder.addSong(song)
                }
                else {
                    val pathSegments = song.folder?.split('/') ?: emptyList()
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
                Log.d("FolderStructure", "$indent - Song: ${song.title} - Relative Path: ${song.folder}")
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
        paths = runBlocking {
            Database.blacklistedN(listOf(BlacklistType.Song.name, BlacklistType.Video.name, BlacklistType.Folder.name))
                .map { OnDeviceBlacklistPath(path = it.path) }
        }

    }

    fun startWith(path: String): Boolean {
        Timber.d("OnDeviceBlacklist paths ${paths.map { it.path }} contains path $path")
        return paths.any { path.startsWith(it.path) }
    }
}
