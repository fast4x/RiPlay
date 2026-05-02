package it.fast4x.riplay.extensions.ondevice

import it.fast4x.riplay.data.models.Song

class Folder(
    val name: String,
    val note: String? = null,
    val parent: Folder? = null,
    val subFolders: MutableList<Folder> = mutableListOf(),
    val songs: MutableList<Song> = mutableListOf(),
    val fullPath: String = ""
) {
    fun addSubFolder(folder: Folder) {
        subFolders.add(folder)
    }

    fun addSong(song: Song) {
        songs.add(song)
    }

    fun getAllSongs(): List<Song> {
        val allSongs = mutableListOf<Song>()
        collectSongsRecursively(allSongs)
        return allSongs
    }

    private fun collectSongsRecursively(allSongs: MutableList<Song>) {
        allSongs.addAll(songs)
        for (subFolder in subFolders) {
            subFolder.collectSongsRecursively(allSongs)
        }
    }
}