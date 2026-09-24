package it.fast4x.riplay.extensions.musicbrainz.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.musicbrainz.models.ArtistDetailUiState
import it.fast4x.riplay.extensions.musicbrainz.models.ArtistRelationInfo
import it.fast4x.riplay.extensions.musicbrainz.models.ArtistStats
import it.fast4x.riplay.extensions.ondevice.OnDeviceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistInsightsViewModel(application: Application) : AndroidViewModel(application),
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtistInsightsViewModel::class.java)) {
            val application = getApplication<Application>()
            @Suppress("UNCHECKED_CAST")
            return ArtistInsightsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val artistDao = Database.artistDao()
    private val albumDao = Database.albumDao()
    private val songDao = Database.songDao()
    private val songArtistRefDao = Database.songArtistCrossRefDao()
    private val eventDao = Database.eventDao()
    private val relationDao = Database.relationDao()

    private val _state = MutableStateFlow(ArtistDetailUiState())
    val state: StateFlow<ArtistDetailUiState> = _state.asStateFlow()

    fun loadArtist(artistId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val artist = withContext(Dispatchers.IO) { artistDao.getById(artistId) }
            if (artist == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            val albums = withContext(Dispatchers.IO) {
                albumDao.getAlbumsByArtist(artistId)
            }
            val topTracks = withContext(Dispatchers.IO) {
                songArtistRefDao.getSongsByArtist(artistId, limit = 5)
            }
            val externalLinks = artist.links ?: emptyList()

            // Relazioni MB
            val relations = withContext(Dispatchers.IO) {
                if (artist.mbId != null) {
                    val rels = relationDao.getBidirectional(artist.mbId)
                    rels.mapNotNull { rel ->
                        val otherMbId = if (rel.fromArtistId == artist.mbId) rel.toArtistId else rel.fromArtistId
                        val otherArtist = artistDao.getByMbId(otherMbId) ?: return@mapNotNull null
                        ArtistRelationInfo(otherArtist, rel.relationType, rel.direction)
                    }
                } else emptyList()
            }

            // Stats
            val stats = withContext(Dispatchers.IO) {
                val songs = songArtistRefDao.getSongsByArtist(artistId, limit = 1000)
                val totalPlayTime = songs.sumOf { it.totalPlayTimeMs }
                val playCount = eventDao.getPlayCountByArtist(artistId) // query
                val likedCount = songs.count { it.isLiked }
                ArtistStats(totalPlayTime, playCount, likedCount, albums.size)
            }

            _state.value = ArtistDetailUiState(
                isLoading = false,
                artist = artist,
                albums = albums,
                topTracks = topTracks,
                relations = relations,
                externalLinks = externalLinks,
                stats = stats
            )
        }
    }
}