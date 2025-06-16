package it.fast4x.riplay.models

class Folder(
    val name: String,
    val parent: Folder? = null,
    val subFolders: MutableList<Folder> = mutableListOf(),
    val songs: MutableList<OnDeviceSong> = mutableListOf(),
    val fullPath: String = ""
) {
    fun addSubFolder(folder: Folder) {
        subFolders.add(folder)
    }

    fun addSong(song: OnDeviceSong) {
        songs.add(song)
    }

    fun getAllSongs(): List<OnDeviceSong> {
        val allSongs = mutableListOf<OnDeviceSong>()
        collectSongsRecursively(allSongs)
        return allSongs
    }

    private fun collectSongsRecursively(allSongs: MutableList<OnDeviceSong>) {
        allSongs.addAll(songs)
        for (subFolder in subFolders) {
            subFolder.collectSongsRecursively(allSongs)
        }
    }
}
