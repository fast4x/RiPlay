package it.fast4x.riplay.extensions.rewind.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.rewind.utils.getRewindYear
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber


class RewindViewModelFactory(
    private val y: Int? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewindViewModel::class.java)) {
            return RewindViewModel(y) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class RewindViewModel(y: Int? = null): ViewModel(), ViewModelProvider.Factory  {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewindViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RewindViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val year = y ?: getRewindYear()

    private val _uiState = MutableStateFlow<RewindUiState>(RewindUiState())
    val uiState: StateFlow<RewindUiState> = _uiState.asStateFlow()

    init {
        loadRewindData()
    }

    private fun loadRewindData() {
        viewModelScope.launch {

            combine(

                Database.songMostListenedByYear(year, 10),
                Database.albumMostListenedByYear(year, 10),
                Database.artistMostListenedByYear(year, 10),
                Database.playlistMostListenedByYear(year, 10),


                Database.songsListenedCountByYear(year),
                Database.albumsListenedCountByYear(year),
                Database.artistsListenedCountByYear(year),
                Database.playlistsListenedCountByYear(year)
            ) { results ->

                @Suppress("UNCHECKED_CAST")
                val songs = results[0] as List<SongMostListened?>
                val topSong = songs.firstOrNull()
                val favSong = topSong?.let {
                    AchievementData(
                        title = it.song?.title,
                        subtitle = it.song?.artistsText,
                        thumbnailUrl = it.song?.thumbnailUrl,
                        minutes = it.minutes,
                        levelEnum = calculateSongLevel(it.minutes),
                        songPreview = it.song
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val albums = results[1] as List<AlbumMostListened?>
                val topAlbum = albums.firstOrNull()
                val favAlbum = topAlbum?.let {
                    AchievementData(
                        title = it.album?.title,
                        subtitle = it.album?.authorsText,
                        thumbnailUrl = it.album?.thumbnailUrl,
                        minutes = it.minutes,
                        levelEnum = calculateAlbumLevel(it.minutes),
                        songPreview = null
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val artists = results[2] as List<ArtistMostListened?>
                val topArtist = artists.firstOrNull()
                val favArtist = topArtist?.let {
                    AchievementData(
                        title = it.artist?.name,
                        subtitle = null,
                        thumbnailUrl = it.artist?.thumbnailUrl,
                        minutes = it.minutes,
                        levelEnum = calculateArtistLevel(it.minutes),
                        songPreview = null
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val playlists = results[3] as List<PlaylistMostListened?>
                val topPlaylist = playlists.firstOrNull()
                val favPlaylist = topPlaylist?.let {
                    AchievementData(
                        title = it.playlist?.name,
                        subtitle = "${it.songs} brani",
                        thumbnailUrl = null,
                        minutes = it.minutes,
                        levelEnum = calculatePlaylistLevel(it.minutes),
                        songPreview = null
                    )
                }

                val sCount = results[4] as SongsListenedCount?
                val aCount = results[5] as AlbumsListenedCount?
                val arCount = results[6] as ArtistsListenedCount?
                val pCount = results[7] as PlaylistsListenedCount?
                val totals = TotalsData(
                    songsCount = sCount?.songs ?: 0,
                    songsMinutes = sCount?.minutes ?: 0,
                    albumsCount = aCount?.albums ?: 0,
                    albumsMinutes = aCount?.minutes ?: 0,
                    artistsCount = arCount?.artists ?: 0,
                    artistsMinutes = arCount?.minutes ?: 0,
                    playlistsCount = pCount?.playlists ?: 0,
                    playlistsMinutes = pCount?.minutes ?: 0
                )

                RewindUiState(
                    isLoading = false,
                    topSongs = songs,
                    topAlbums = albums,
                    topArtists = artists,
                    topPlaylists = playlists,
                    favoriteSong = favSong,
                    favoriteAlbum = favAlbum,
                    favoriteArtist = favArtist,
                    favoritePlaylist = favPlaylist,
                    totals = totals,
                    year = year
                )
            }.catch { e ->
                Timber.e(e, "Errore nel caricamento Rewind")
                _uiState.value = RewindUiState(isLoading = false, year = year) // Gestisci errore
            }.collect { state ->
                _uiState.value = state
            }
        }
    }


    private fun calculateSongLevel(minutes: Long): SongLevel {
        return when (minutes) {
            in 0L..200L -> SongLevel.OBSESSION
            in 201L..500L -> SongLevel.ANTHEM
            in 501L..1000L -> SongLevel.SOUNDTRACK
            in 1001L..3000L -> SongLevel.ETERNAL_FLAME
            else -> SongLevel.UNDEFINED
        }
    }

    private fun calculateAlbumLevel(minutes: Long): AlbumLevel {
        return when (minutes) {
            in 0L..1000L -> AlbumLevel.DEEP_DIVE
            in 1001L..2500L -> AlbumLevel.ON_REPEAT
            in 2501L..5000L -> AlbumLevel.RESIDENT
            in 5001L..8000L -> AlbumLevel.SANCTUARY
            else -> AlbumLevel.UNDEFINED
        }
    }

    private fun calculateArtistLevel(minutes: Long): ArtistLevel {
        return when (minutes) {
            in 0L..2000L -> ArtistLevel.NEW_FAVORITE
            in 2001L..5000L -> ArtistLevel.A_LIST_FAN
            in 5001L..10000L -> ArtistLevel.THE_ARCHIVIST
            in 10001L..20000L -> ArtistLevel.THE_DEVOTEE
            else -> ArtistLevel.UNDEFINED
        }
    }

    private fun calculatePlaylistLevel(minutes: Long): PlaylistLevel {
        return when (minutes) {
            in 0L..500L -> PlaylistLevel.CURATOR
            in 501L..1500L -> PlaylistLevel.MASTERMIND
            in 1501L..3000L -> PlaylistLevel.PHENOMENON
            in 3001L..5000L -> PlaylistLevel.OPUS
            else -> PlaylistLevel.UNDEFINED
        }
    }
}