package it.fast4x.riplay.extensions.rewind.data

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song


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
        val song: Song?,
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
        val song: Song?,
    ) : RewindSlide(3, brush)

    data class ArtistAchievement(
        override val title: String,
        override val year: Int,
        val artistName: String,
        val artistImageUri: Uri,
        val minutesListened: Long,
        val level: ArtistLevel,
        val brush: Brush,
        val song: Song?,
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

    data class AnnualListener(
        override val title: String,
        override val year: Int,
        val brush: Brush
    ) : RewindSlide(8, brush)

    data class Intermediate(
        override val title: String,
        override val year: Int,
        val message: String,
        val subMessage: String,
        val message1: String,
        val subMessage1: String,
        val brush: Brush
    ) : RewindSlide(98, brush)

    data class OutroSlide(
        override val title: String,
        override val year: Int,
        val brush: Brush
    ) : RewindSlide(99, brush)
}


enum class SongLevel(val title: Int, val goal: Int, val description: Int) {
    OBSESSION(
        title = R.string.rw_songlevel_obsession_title_you_have_an_obsession,
        goal = R.string.rw_songlevel_obsession_goal_listened_to_s_minutes,
        description = R.string.rw_songlevel_obsession_desc_this_song_was_the_soundtrack_to_your_year_an_obsession_you_couldn_t_stop_listening_to,
    ),
    ANTHEM(
        title = R.string.rw_songlevel_anthem_title_it_s_your_anthem,
        goal = R.string.rw_songlevel_anthem_goal_listened_to_s_minutes,
        description = R.string.rw_songlevel_anthem_desc_it_s_not_just_a_song_it_s_your_anthem_it_defined_your_summer_your_winter_your_life,

    ),
    SOUNDTRACK(
        title = R.string.rw_songlevel_soundtrack_title_it_s_your_soundtrack,
        goal = R.string.rw_songlevel_soundtrack_goal_listened_to_s_minutes,
        description = R.string.rw_songlevel_soundtrack_desc_this_song_isn_t_just_an_anthem_it_s_the_soundtrack_to_your_life_the_rhythm_that_accompanies_your_days,
    ),
    ETERNAL_FLAME(
        title = R.string.rw_songlevel_eternalflame_title_you_are_an_eternal_flame,
        goal = R.string.rw_songlevel_eternalflame_goal_listened_to_s_minutes,
        description = R.string.rw_songlevel_eternalflame_desc_you_and_this_song_are_one_and_the_same_an_eternal_flame_burning_in_your_musical_heart_a_legend,
    ),
    UNDEFINED(
        title = R.string.rw_songlevel_undefined_title_oops,
        goal = R.string.rw_songlevel_undefined_goal_it_seems_like_you_haven_t_listened_to_any_songs,
        description = R.string.rw_songlevel_undefined_desc_nothing_to_see_here,
    )
}


enum class AlbumLevel(val title: Int, val goal: Int, val description: Int) {
    DEEP_DIVE(
        title = R.string.rw_albumlevel_deepdive_title_you_love_conducting_a_deep_dive,
        goal = R.string.rw_albumlevel_deepdive_goal_listened_to_s_minutes,
        description = R.string.rw_albumlevel_deepdive_desc_you_didn_t_stop_at_the_singles_you_dove_deep_into_this_masterpiece_note_by_note,
    ),
    ON_REPEAT(
        title = R.string.rw_albumlevel_onrepeat_title_you_listen_to_it_on_repeat,
        goal = R.string.rw_albumlevel_onrepeat_goal_listened_to_s_minutes,
        description = R.string.rw_albumlevel_onrepeat_desc_this_album_was_on_repeat_for_weeks_you_know_every_word_every_pause_every_beat,
    ),
    RESIDENT(
        title = R.string.rw_albumlevel_resident_title_you_are_a_resident,
        goal = R.string.rw_albumlevel_resident_goal_listened_to_s_minutes,
        description = R.string.rw_albumlevel_resident_desc_you_didn_t_just_listen_to_this_album_you_lived_in_it_you_re_not_just_a_listener_you_re_a_resident,
    ),
    SANCTUARY(
        title = R.string.rw_albumlevel_sactuary_title_it_s_your_sanctuary,
        goal = R.string.rw_albumlevel_sanctuary_goal_listened_to_s_minutes,
        description = R.string.rw_albumlevel_sanctuary_desc_this_album_is_more_than_music_it_s_your_sanctuary_a_sacred_place_to_return_to_for_peace_and_inspiration,
    ),
    UNDEFINED(
        title = R.string.rw_albumlevel_undefined_title_oops,
        goal = R.string.rw_albumlevel_undefined_goal_it_seems_like_you_haven_t_listened_to_any_albums,
        description = R.string.rw_albumlevel_undefined_desc_nothing_to_see_here,
    )
}


enum class PlaylistLevel(val title: Int, val goal: Int, val description: Int) {
    CURATOR(
        title = R.string.rw_playlistlevel_curator_title_you_are_a_curator,
        goal = R.string.rw_playlistlevel_curator_goal_listened_to_s_minutes,
        description = R.string.rw_playlistlevel_curator_desc_you_re_not_just_a_listener_you_re_a_curator_you_created_the_perfect_soundtrack_for_a_moment,
    ),
    MASTERMIND(
        title = R.string.rw_playlistlevel_mastermind_title_you_are_a_mastermind,
        goal = R.string.rw_playlistlevel_mastermind_goal_listened_to_s_minutes,
        description = R.string.rw_playlistlevel_mastermind_desc_your_playlist_is_a_work_of_art_maybe_you_should_consider_a_career_as_a_dj,
    ),
    PHENOMENON(
        title = R.string.rw_playlistlevel_phenomenon_title_you_are_a_phenomenon,
        goal = R.string.rw_playlistlevel_phenomenon_goal_listened_to_s_minutes,
        description = R.string.rw_playlistlevel_phenomenon_desc_this_playlist_isn_t_just_a_list_of_songs_it_s_a_phenomenon_a_cultural_event_in_your_world,
    ),
    OPUS(
        title = R.string.rw_playlistlevel_opus_title_you_created_an_opus,
        goal = R.string.rw_playlistlevel_opus_goal_listened_to_s_minutes,
        description = R.string.rw_playlistlevel_opus_desc_you_didn_t_just_create_a_playlist_you_composed_a_masterpiece_this_is_your_magnum_opus_your_legacy,
    ),
    UNDEFINED(
        title = R.string.rw_playlistlevel_undefined_title_oops,
        goal = R.string.rw_playlistlevel_undefined_goal_it_seems_like_you_haven_t_listened_to_any_playlists,
        description = R.string.rw_playlistlevel_undefined_desc_nothing_to_see_here,
    )
}


enum class ArtistLevel(val title: Int, val goal: Int, val description: Int) {
    NEW_FAVORITE(
        title = R.string.rw_artistlevel_newfavorite_title_you_discover_new_favorite,
        goal = R.string.rw_artistlevel_newfavorite_goal_listened_to_s_minutes,
        description = R.string.rw_artistlevel_newfavorite_desc_you_discovered_a_new_favorite_and_can_t_stop_listening_the_beginning_of_a_beautiful_musical_story,
    ),
    A_LIST_FAN(
        title = R.string.rw_artistlevel_alistfan_title_you_are_an_a_list_fan,
        goal = R.string.rw_artistlevel_alistfan_goal_listened_to_s_minutes,
        description = R.string.rw_artistlevel_alistfan_desc_this_artist_made_it_to_your_a_list_their_music_is_a_constant_and_loved_presence_in_your_routine,
    ),
    THE_ARCHIVIST(
        title = R.string.rw_artistlevel_thearchivist_title_you_are_the_archivist,
        goal = R.string.rw_artistlevel_thearchivist_goal_listened_to_s_minutes,
        description = R.string.rw_artistlevel_thearchivist_desc_you_don_t_just_stick_to_the_hit_singles_you_ve_explored_every_corner_of_their_discography_becoming_a_true_expert,
    ),
    THE_DEVOTEE(
        title = R.string.rw_artistlevel_thedevotee_title_you_are_the_devoted,
        goal = R.string.rw_artistlevel_thedevotee_goal_listened_to_s_minutes,
        description = R.string.rw_artistlevel_thedevotee_desc_this_artist_s_music_is_more_than_just_sound_it_s_a_part_of_you_a_deep_and_unbreakable_bond,
    ),
    UNDEFINED(
        title = R.string.rw_artistlevel_undefined_title_oops,
        goal = R.string.rw_artistlevel_undefined_goal_it_seems_like_you_haven_t_listened_to_any_artists,
        description = R.string.rw_artistlevel_undefined_desc_nothing_to_see_here,
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
    val annualListener: RewindSlide.AnnualListener,
    val intermediate1: RewindSlide.Intermediate,
    val intermediate2: RewindSlide.Intermediate,
    val intermediate3: RewindSlide.Intermediate,
    val intermediate4: RewindSlide.Intermediate,
    val intermediate5: RewindSlide.Intermediate,
    val intermediate6: RewindSlide.Intermediate,
    val intermediate7: RewindSlide.Intermediate,
    val intermediate8: RewindSlide.Intermediate,
    val intermediate9: RewindSlide.Intermediate?,
    val intermediate10: RewindSlide.Intermediate?,
)


@Composable
fun buildRewindState(state: RewindUiState): RewindState {

    val y = state.year ?: 0

    return RewindState(
        intro = RewindSlide.IntroSlide(
            title = stringResource(R.string.rw_rewind),
            year = y.toInt(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE7D858), Color(0xFF733B81))
            )
        ),
        topSongs = RewindSlide.TopSongs(
            title = stringResource(R.string.rw_top_songs, y.toInt()),
            year = y.toInt(),
            songs = state.topSongs,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFFBBA0A0))
            )
        ),
        topAlbums = RewindSlide.TopAlbums(
            title = stringResource(R.string.rw_top_albums, y.toInt()),
            year = y.toInt(),
            albums = state.topAlbums,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
            )
        ),
        topArtists = RewindSlide.TopArtists(
            title = stringResource(R.string.rw_top_artists, y.toInt()),
            year = y.toInt(),
            artists = state.topArtists,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5A6CD2), Color(0xFF1DB954))
            )
        ),
        topPlaylists = RewindSlide.TopPlaylists(
            title = stringResource(R.string.rw_top_playlists, y.toInt()),
            year = y.toInt(),
            playlists = state.topPlaylists,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
            )
        ),
        song = RewindSlide.SongAchievement(
            title = stringResource(R.string.rw_your_favorite_song, y.toInt()),
            year = y.toInt(),
            songTitle = state.favoriteSong?.title.toString(),
            artistName = state.favoriteSong?.subtitle.toString(),
            albumArtUri = (state.favoriteSong?.thumbnailUrl ?: "" ).toUri(),
            level = when (state.favoriteSong?.minutes) {
                in 0L..200L  -> SongLevel.OBSESSION
                in 201L..500L -> SongLevel.ANTHEM
                in 501L..1000L -> SongLevel.SOUNDTRACK
                in 1001L..3000L -> SongLevel.ETERNAL_FLAME
                else -> SongLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFFBBA0A0))
            ),
            minutesListened = state.favoriteSong?.minutes ?: 0,
            song = state.favoriteSong?.songPreview,
        ),
        album = RewindSlide.AlbumAchievement(
            title = stringResource(R.string.rw_your_favorite_album, y.toInt()),
            year = y.toInt(),
            albumTitle = state.favoriteAlbum?.title.toString(),
            artistName = state.favoriteAlbum?.subtitle.toString(),
            albumArtUri = (state.favoriteAlbum?.thumbnailUrl ?: "").toUri(),
            level = when (state.favoriteAlbum?.minutes) {
                in 0L..1000L -> AlbumLevel.DEEP_DIVE
                in 1001L..2500L -> AlbumLevel.ON_REPEAT
                in 2501L..5000L -> AlbumLevel.RESIDENT
                in 5001L..8000L -> AlbumLevel.SANCTUARY
                else -> AlbumLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
            ),
            minutesListened = state.favoriteAlbum?.minutes ?: 0,
            song = state.favoriteAlbum?.songPreview
        ),
        playlist = RewindSlide.PlaylistAchievement(
            title = stringResource(R.string.rw_your_favorite_playlist, y.toInt()),
            year = y.toInt(),
            playlist = null, // todo
            playlistName = state.favoritePlaylist?.title.toString(),
            songCount = 0, // todo
            totalMinutes = state.favoritePlaylist?.minutes ?: 0,
            level = when (state.favoritePlaylist?.minutes) {
                in  0L..500L -> PlaylistLevel.CURATOR
                in 501L..1500L -> PlaylistLevel.MASTERMIND
                in 1501L..3000L -> PlaylistLevel.PHENOMENON
                in 3001L..5000L -> PlaylistLevel.OPUS
                else -> PlaylistLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
            ),
            song = state.favoritePlaylist?.songPreview
        ),
        artist = RewindSlide.ArtistAchievement(
            title = stringResource(R.string.rw_your_favorite_artist, y.toInt()),
            year = y.toInt(),
            artistName = state.favoriteArtist?.title ?: "",
            artistImageUri = (state.favoriteArtist?.thumbnailUrl ?: "").toUri(),
            minutesListened = state.favoriteArtist?.minutes ?: 0,
            level = when (state.favoriteArtist?.minutes) {
                in 0L..2000L-> ArtistLevel.NEW_FAVORITE
                in 2001L..5000L -> ArtistLevel.A_LIST_FAN
                in 5001L..10000L -> ArtistLevel.THE_ARCHIVIST
                in 10001L..20000L -> ArtistLevel.THE_DEVOTEE
                else -> ArtistLevel.UNDEFINED
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5A6CD2), Color(0xFF1DB954))
            ),
            song = state.favoriteArtist?.songPreview
        ),
        outro = RewindSlide.OutroSlide(
            title = stringResource(R.string.rw_rewind),
            year = y.toInt(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        annualListener = RewindSlide.AnnualListener(
            title = stringResource(R.string.rw_annual_listener_level, y.toInt()),
            year = y.toInt(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate1 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_songs_listened_to, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_songs, state.totals?.songsCount.toString()),
            subMessage = "",
            message1 = stringResource(R.string.rw_minutes, state.totals?.songsMinutes.toString()),
            subMessage1 = "",
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate2 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_albums_listened_to, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_albums, state.totals?.albumsCount.toString()),
            subMessage = "",
            message1 = stringResource(R.string.rw_minutes, state.totals?.albumsMinutes.toString()),
            subMessage1 = "",
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate3 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_artists_listened_to, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_artists, state.totals?.artistsCount.toString()),
            subMessage = "",
            message1 = stringResource(R.string.rw_minutes, state.totals?.artistsMinutes.toString()),
            subMessage1 = "",
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate4 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_playlists_listened_to, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_playlists, state.totals?.playlistsCount.toString()),
            subMessage = "",
            message1 = stringResource(R.string.rw_minutes, state.totals?.playlistsMinutes.toString()),
            subMessage1 = "",
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate5 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_rewind_year, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_let_s_analyze),
            subMessage = stringResource(R.string.rw_your_history),
            message1 = stringResource(R.string.rw_ready_for),
            subMessage1 = stringResource(R.string.rw_your_results),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate6 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_rewind_year, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_let_s_start),
            subMessage = stringResource(R.string.rw_your_numbers),
            message1 = stringResource(R.string.rw_ready_for),
            subMessage1 = stringResource(R.string.rw_your_music_story),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate7 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_rewind_year, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_now),
            subMessage = stringResource(R.string.rw_your_top),
            message1 = stringResource(R.string.rw_ready_for),
            subMessage1 = stringResource(R.string.rw_your_music_preferences),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate8 = RewindSlide.Intermediate(
            title = stringResource(R.string.rw_rewind_year, y.toInt()),
            year = y.toInt(),
            message = stringResource(R.string.rw_and_now),
            subMessage = stringResource(R.string.rw_your_badge),
            message1 = stringResource(R.string.rw_ready_to_know),
            subMessage1 = stringResource(R.string.rw_your_music_level),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        ),
        intermediate9 = null,
        intermediate10 = null,
    )

}


@Composable
fun getRewindSlides(vmState: RewindUiState): List<RewindSlide> {

    val state = buildRewindState(vmState)

    return listOf(
        state.intro,

        //numbers
        state.intermediate6,
        state.intermediate1,
        state.intermediate2,
        state.intermediate3,
        state.intermediate4,

        // top
        state.intermediate7,
        state.topSongs,
        state.topAlbums,
        state.topArtists,
        state.topPlaylists,

        // results
        state.intermediate5,
        state.song,
        state.album,
        state.playlist,
        state.artist,

        // annual badge
        state.intermediate8,
        state.annualListener,

        // end
        state.outro
    )
}
