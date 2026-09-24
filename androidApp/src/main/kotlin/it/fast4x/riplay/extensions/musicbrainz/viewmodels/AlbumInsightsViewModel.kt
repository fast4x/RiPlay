package it.fast4x.riplay.extensions.musicbrainz.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.musicbrainz.models.AlbumDetailUiState
import it.fast4x.riplay.extensions.musicbrainz.models.AlbumStats
import it.fast4x.riplay.extensions.musicbrainz.models.ExternalLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class AlbumInsightsViewModel(application: Application) : AndroidViewModel(application),
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumInsightsViewModel::class.java)) {
            val application = getApplication<Application>()
            @Suppress("UNCHECKED_CAST")
            return AlbumInsightsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }


    private val albumDao = Database.albumDao()
    private val songDao = Database.songDao()
    private val artistDao = Database.artistDao()
    private val songArtistRefDao = Database.songArtistCrossRefDao()
    private val eventDao = Database.eventDao()


    private val _state = MutableStateFlow(AlbumDetailUiState())
    val state: StateFlow<AlbumDetailUiState> = _state.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val album = withContext(Dispatchers.IO) { albumDao.getById(albumId) }
            if (album == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            // Tracklist via Song.albumId
            val tracks = withContext(Dispatchers.IO) {
                songDao.getSongsByAlbum(albumId, limit = 100)
            }

            // Artista (via authorsText match)
            val artist = withContext(Dispatchers.IO) {
                album.authorsText?.let { artistDao.findByNameExactIgnoreCase(it) }
            }

            // Altri album dello stesso artista
            val otherAlbums = withContext(Dispatchers.IO) {
                album.authorsText?.let { name ->
                    albumDao.getAlbumsByArtistName(name)
                        .filter { it.id != albumId }
                        .take(10)
                } ?: emptyList()
            }

            // Link esterni
            val externalLinks = album.links ?: emptyList()

            // Stats
            val stats = withContext(Dispatchers.IO) {
                val totalPlayTime = tracks.sumOf { it.totalPlayTimeMs }
                val playCount = tracks.sumOf { song ->
                    eventDao.getPlayCountBySong(song.id)
                }
                val likedCount = tracks.count { it.isLiked }
                AlbumStats(totalPlayTime, playCount, likedCount, tracks.size)
            }

            _state.value = AlbumDetailUiState(
                isLoading = false,
                album = album,
                tracks = tracks,
                artist = artist,
                otherAlbums = otherAlbums,
                externalLinks = externalLinks,
                stats = stats
            )
        }
    }
}