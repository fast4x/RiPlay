package it.fast4x.riplay.extensions.rewind.data

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.getCalculatedMonths
import kotlinx.coroutines.Dispatchers
import timber.log.Timber


val slideTitleFontSize = 20.sp

sealed class RewindSlide(val id: Int, val backgroundBrush: Brush) {
    abstract val title: String
    abstract val year: Int


    data class IntroSlide(
        override val title: String,
        override val year: Int,
        val brush: Brush,
    ) : RewindSlide(0, brush)

    data class SongAchievement(
        override val title: String,
        override val year: Int,
        val songTitle: String,
        val artistName: String,
        val albumArtUri: Uri,
        val level: SongLevel,
        val brush: Brush,
        val minutesListened: Long,
        val song: Song?
    ) : RewindSlide(1, brush)


    data class AlbumAchievement(
        override val title: String,
        override val year: Int,
        val albumTitle: String,
        val artistName: String,
        val albumArtUri: Uri,
        val level: AlbumLevel,
        val brush: Brush,
        val minutesListened: Long,
    ) : RewindSlide(2, brush)


    data class PlaylistAchievement(
        override val title: String,
        override val year: Int,
        val playlist: Playlist?,
        val playlistName: String,
        val songCount: Int,
        val totalMinutes: Long,
        val level: PlaylistLevel,
        val brush: Brush,
    ) : RewindSlide(3, brush)

    data class ArtistAchievement(
        override val title: String,
        override val year: Int,
        val artistName: String,
        val artistImageUri: Uri,
        val minutesListened: Long,
        val level: ArtistLevel,
        val brush: Brush,
    ) : RewindSlide(4, brush)

    data class TopSongs(
        override val title: String,
        override val year: Int,
        val songs: List<SongMostListened?>,
        val brush: Brush,
    ) : RewindSlide(5, brush)

    data class TopAlbums(
        override val title: String,
        override val year: Int,
        val albums: List<AlbumMostListened?>,
        val brush: Brush,
    ) : RewindSlide(6, brush)

    data class TopArtists(
        override val title: String,
        override val year: Int,
        val artists: List<ArtistMostListened?>,
        val brush: Brush,
    ) : RewindSlide(7, brush)

    data class TopPlaylists(
        override val title: String,
        override val year: Int,
        val playlists: List<PlaylistMostListened?>,
        val brush: Brush,
    ) : RewindSlide(8, brush)

    data class Intermediate(
        override val title: String,
        override val year: Int,
        val message: String,
        val subMessage: String,
        val brush: Brush
    ) : RewindSlide(98, brush)

    data class OutroSlide(
        override val title: String,
        override val year: Int,
        val brush: Brush
    ) : RewindSlide(99, brush)
}


enum class SongLevel(val title: String, val goal: String, val description: String) {
    OBSESSION(
        title = "You have an Obsession",
        goal = "Listened to %s minutes",
        description = "This song was the soundtrack to your year. \n An obsession you couldn't stop listening to.",
    ),
    ANTHEM(
        title = "It's your Anthem",
        goal = "Listened to %s minutes",
        description = "It's not just a song, it's your anthem. \n It defined your summer, your winter, your life.",

    ),
    SOUNDTRACK(
        title = "It's your Soundtrack",
        goal = "Listened to %s minutes",
        description = "This song isn't just an anthem, it's the soundtrack to your life. \n The rhythm that accompanies your days.",
    ),
    ETERNAL_FLAME(
        title = "You are an Eternal Flame",
        goal = "Listened to %s minutes",
        description = "You and this song are one and the same. \n An eternal flame burning in your musical heart. \n A legend.",
    ),
    UNDEFINED(
        title = "Ops",
        goal = "It seems like you haven't listened to any songs",
        description = "Nothing to see here",
    )
}


enum class AlbumLevel(val title: String, val goal: String, val description: String) {
    DEEP_DIVE(
        title = "You love conducting a Deep Dive",
        goal = "Listened to %s minutes",
        description = "You didn't stop at the singles. \n You dove deep into this masterpiece, note by note.",
    ),
    ON_REPEAT(
        title = "You listen to it On Repeat",
        goal = "Listened to %s minutes",
        description = "This album was on repeat for weeks. \n You know every word, every pause, every beat.",
    ),
    RESIDENT(
        title = "You are a Resident",
        goal = "Listened to %s minutes",
        description = "You didn't just listen to this album, you lived in it. \n You're not just a listener, you're a resident.",
    ),
    SANCTUARY(
        title = "It's your Sanctuary",
        goal = "Listened to %s minutes",
        description = "This album is more than music, it's your sanctuary. \n A sacred place to return to for peace and inspiration.",
    ),
    UNDEFINED(
        title = "Ops",
        goal = "It seems like you haven't listened to any albums",
        description = "Nothing to see here",
    )
}


enum class PlaylistLevel(val title: String, val goal: String, val description: String) {
    CURATOR(
        title = "You are a Curator",
        goal = "Listened to %s minutes",
        description = "You're not just a listener, you're a curator. \n You created the perfect soundtrack for a moment.",
    ),
    MASTERMIND(
        title = "You are a Mastermind",
        goal = "Listened to %s minutes",
        description = "Your playlist is a work of art. \n Maybe you should consider a career as a DJ.",
    ),
    PHENOMENON(
        title = "You are a Phenomenon",
        goal = "Listened to %s minutes",
        description = "This playlist isn't just a list of songs, it's a phenomenon. \n A cultural event in your world.",
    ),
    OPUS(
        title = "You created an Opus",
        goal = "Listened to %s minutes",
        description = "You didn't just create a playlist, you composed a masterpiece.\n This is your magnum opus, your legacy.",
    ),
    UNDEFINED(
        title = "Ops",
        goal = "It seems like you haven't listened to any playlists",
        description = "Nothing to see here",
    )
}


enum class ArtistLevel(val title: String, val goal: String, val description: String) {
    NEW_FAVORITE(
        title = "You discover New Favorite",
        goal = "Listened to %s minutes",
        description = "You discovered a new favorite and can't stop listening. \n The beginning of a beautiful musical story.",
    ),
    A_LIST_FAN(
        title = "You are an A-List Fan",
        goal = "Listened to %s minutes",
        description = "This artist made it to your A-List. \n Their music is a constant and loved presence in your routine.",
    ),
    THE_ARCHIVIST(
        title = "You are The Archivist",
        goal = "Listened to %s minutes",
        description = "You don't just stick to the hit singles. \n You've explored every corner of their discography, becoming a true expert.",
    ),
    THE_DEVOTEE(
        title = "You are The Devoted",
        goal = "Listened to %s minutes",
        description = "This artist's music is more than just sound, it's a part of you. \n A deep and unbreakable bond.",
    ),
    UNDEFINED(
        title = "Ops",
        goal = "It seems like you haven't listened to any artists",
        description = "Nothing to see here",
    )
}

data class RewindState (
    val intro: RewindSlide.IntroSlide,
    val song: RewindSlide.SongAchievement,
    val album: RewindSlide.AlbumAchievement,
    val playlist: RewindSlide.PlaylistAchievement,
    val artist: RewindSlide.ArtistAchievement,
    val topSongs: RewindSlide.TopSongs,
    val topAlbums: RewindSlide.TopAlbums,
    val topArtists: RewindSlide.TopArtists,
    val topPlaylists: RewindSlide.TopPlaylists,
    val outro: RewindSlide.OutroSlide,
    val intermediate1: RewindSlide.Intermediate,
    val intermediate2: RewindSlide.Intermediate?,
    val intermediate3: RewindSlide.Intermediate?,
    val intermediate4: RewindSlide.Intermediate?,
    val intermediate5: RewindSlide.Intermediate?,
    val intermediate6: RewindSlide.Intermediate?,
    val intermediate7: RewindSlide.Intermediate?,
    val intermediate8: RewindSlide.Intermediate?,
    val intermediate9: RewindSlide.Intermediate?,
    val intermediate10: RewindSlide.Intermediate?,
)

@Composable
fun buildRewindState(): RewindState {
    val ym by remember { mutableStateOf(getCalculatedMonths(0)) }
    val y by remember { mutableLongStateOf( ym?.substring(0,4)?.toLong() ?: 0) }
    val m by remember { mutableLongStateOf( ym?.substring(5,7)?.toLong() ?: 0) }

    val songMostListened = remember {
        Database.songMostListenedByYear(y, 10)
    }.collectAsState(initial = null, context = Dispatchers.IO)

    Timber.d("RewindData: songMostListened: $songMostListened")

    val albumMostListened = remember {
        Database.albumMostListenedByYear(y, 10)
    }.collectAsState(initial = null, context = Dispatchers.IO)

    Timber.d("RewindData: albumMostListened: $albumMostListened")


    val playlistMostListened = remember {
        Database.playlistMostListenedByYear(y, 10)
    }.collectAsState(initial = null, context = Dispatchers.IO)

    Timber.d("RewindData: playlistMostListened: $playlistMostListened")


    val artistMostListened = remember {
        Database.artistMostListenedByYear(y, 10)
    }.collectAsState(initial = null, context = Dispatchers.IO)

    Timber.d("RewindData: artistMostListened: $artistMostListened")


    return RewindState(
        intro = RewindSlide.IntroSlide(
            title = "Rewind",
            year = y.toInt(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE7D858), Color(0xFF733B81))
            )
        ),
        topSongs = RewindSlide.TopSongs(
            title = "Top Songs ${y.toInt()}",
            year = y.toInt(),
            songs = songMostListened.value ?: emptyList(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFFBBA0A0))
            )
        ),
        topAlbums = RewindSlide.TopAlbums(
            title = "Top Albums ${y.toInt()}",
            year = y.toInt(),
            albums = albumMostListened.value ?: emptyList(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
            )
        ),
        topArtists = RewindSlide.TopArtists(
            title = "Top Artists ${y.toInt()}",
            year = y.toInt(),
            artists = artistMostListened.value ?: emptyList(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5A6CD2), Color(0xFF1DB954))
            )
        ),
        topPlaylists = RewindSlide.TopPlaylists(
            title = "Top Playlists ${y.toInt()}",
            year = y.toInt(),
            playlists = playlistMostListened.value ?: emptyList(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
            )
        ),
        song = RewindSlide.SongAchievement(
            title = "Your favorite song ${y.toInt()}",
            year = y.toInt(),
            songTitle = songMostListened.value?.firstOrNull()?.song?.title ?: "",
            artistName = songMostListened.value?.firstOrNull()?.song?.artistsText ?: "",
            albumArtUri = (songMostListened.value?.firstOrNull()?.song?.thumbnailUrl ?: "").toUri(),
            level = when (songMostListened.value?.firstOrNull()?.minutes) {
                in 0L..200L  -> SongLevel.OBSESSION
                in 201L..500L -> SongLevel.ANTHEM
                in 501L..1000L -> SongLevel.SOUNDTRACK
                in 1001L..3000L -> SongLevel.ETERNAL_FLAME
                else -> SongLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFFBBA0A0))
            ),
            minutesListened = songMostListened.value?.firstOrNull()?.minutes ?: 0,
            song = songMostListened.value?.firstOrNull()?.song,
        ),
        album = RewindSlide.AlbumAchievement(
            title = "Your favorite album ${y.toInt()}",
            year = y.toInt(),
            albumTitle = albumMostListened.value?.firstOrNull()?.album?.title ?: "",
            artistName = albumMostListened.value?.firstOrNull()?.album?.authorsText ?: "",
            albumArtUri = (albumMostListened.value?.firstOrNull()?.album?.thumbnailUrl ?: "").toUri(),
            level = when (albumMostListened.value?.firstOrNull()?.minutes) {
                in 0L..1000L -> AlbumLevel.DEEP_DIVE
                in 1001L..2500L -> AlbumLevel.ON_REPEAT
                in 2501L..5000L -> AlbumLevel.RESIDENT
                in 5001L..8000L -> AlbumLevel.SANCTUARY
                else -> AlbumLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
            ),
            minutesListened = albumMostListened.value?.firstOrNull()?.minutes ?: 0
        ),
        playlist = RewindSlide.PlaylistAchievement(
            title = "Your favorite playlist ${y.toInt()}",
            year = y.toInt(),
            playlist = playlistMostListened.value?.firstOrNull()?.playlist,
            playlistName = playlistMostListened.value?.firstOrNull()?.playlist?.name ?: "",
            songCount = playlistMostListened.value?.firstOrNull()?.songs ?: 0,
            totalMinutes = playlistMostListened.value?.firstOrNull()?.minutes ?: 0,
            level = when (playlistMostListened.value?.firstOrNull()?.minutes) {
                in  0L..500L -> PlaylistLevel.CURATOR
                in 501L..1500L -> PlaylistLevel.MASTERMIND
                in 1501L..3000L -> PlaylistLevel.PHENOMENON
                in 3001L..5000L -> PlaylistLevel.OPUS
                else -> PlaylistLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
            )
        ),
        artist = RewindSlide.ArtistAchievement(
            title = "Your favorite artist ${y.toInt()}",
            year = y.toInt(),
            artistName = artistMostListened.value?.firstOrNull()?.artist?.name ?: "",
            artistImageUri = (artistMostListened.value?.firstOrNull()?.artist?.thumbnailUrl ?: "").toUri(),
            minutesListened = artistMostListened.value?.firstOrNull()?.minutes ?: 0,
            level = when (artistMostListened.value?.firstOrNull()?.minutes) {
                in 0L..2000L-> ArtistLevel.NEW_FAVORITE
                in 2001L..5000L -> ArtistLevel.A_LIST_FAN
                in 5001L..10000L -> ArtistLevel.THE_ARCHIVIST
                in 10001L..20000L -> ArtistLevel.THE_DEVOTEE
                else -> ArtistLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5A6CD2), Color(0xFF1DB954))
            )
        ),
        outro = RewindSlide.OutroSlide(
            title = "Rewind",
            year = y.toInt(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate1 = RewindSlide.Intermediate(
            title = "Rewind",
            year = y.toInt(),
            message = "One",
            subMessage = "intermediate",
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate2 = null,
        intermediate3 = null,
        intermediate4 = null,
        intermediate5 = null,
        intermediate6 = null,
        intermediate7 = null,
        intermediate8 = null,
        intermediate9 = null,
        intermediate10 = null,
    )

}


@Composable
fun getRewindSlides(): List<RewindSlide> {

    val state = buildRewindState()

    return listOf(
        state.intro,
        state.intermediate1,
        state.topSongs,
        state.topAlbums,
        state.topArtists,
        state.topPlaylists,
        state.song,
        state.album,
        state.playlist,
        state.artist,
        state.outro
    )
}