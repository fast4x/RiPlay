package it.fast4x.riplay.extensions.experimental.webdavlibrary.models

import it.fast4x.riplay.data.models.Song

data class WebDavConfig(
    val baseUrl: String, // es. "https://cloud.server.com/remote.php/dav/files/username/"
    val username: String,
    val password: String
)


data class WebDavItem(
    val href: String,           // Il percorso relativo o assoluto del file
    val isDirectory: Boolean,   // True se è una cartella (collection)
    val contentLength: Long,    // Dimensione in byte (0 per le cartelle)
    val lastModified: String?,  // Data ultima modifica
    val contentType: String?    // Mime type (es. audio/mpeg)
)

sealed class WebDavBrowserState {
    object Idle : WebDavBrowserState()
    object Loading : WebDavBrowserState()
    data class Success(val folders: List<WebDavItem>, val songs: List<Song>) : WebDavBrowserState()
    data class Error(val message: String) : WebDavBrowserState()
}

data class WebDavSongMetadata(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String? = null
)