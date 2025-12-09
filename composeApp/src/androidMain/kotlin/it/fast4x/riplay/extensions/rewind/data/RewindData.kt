package it.fast4x.riplay.extensions.rewind.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import it.fast4x.riplay.R


sealed class RewindSlide(val id: Int, val backgroundBrush: Brush) {

    data class IntroSlide(
        val brush: Brush
    ) : RewindSlide(0, brush)


    data class TopArtist(
        val artistName: String,
        val artistImageRes: Int,
        val minutesListened: Int,
        val artistGenre: String,
        val brush: Brush
    ) : RewindSlide(1, brush)


    data class SongAchievement(
        val songTitle: String,
        val artistName: String,
        val albumArtRes: Int,
        val level: SongLevel,
        val brush: Brush
    ) : RewindSlide(2, brush)


    data class AlbumAchievement(
        val albumTitle: String,
        val artistName: String,
        val albumArtRes: Int,
        val level: AlbumLevel,
        val brush: Brush
    ) : RewindSlide(3, brush)


    data class PlaylistAchievement(
        val playlistName: String,
        val songCount: Int,
        val totalMinutes: Int,
        val level: PlaylistLevel,
        val brush: Brush
    ) : RewindSlide(4, brush)

    data class ArtistAchievement(
        val artistName: String,
        val artistImageRes: Int,
        val minutesListened: Int,
        val level: ArtistLevel,
        val brush: Brush
    ) : RewindSlide(5, brush)

    data class OutroSlide(
        val brush: Brush
    ) : RewindSlide(99, brush)
}


enum class SongLevel(val goal: String, val description: String, range: IntRange) {
    OBSESSION(
        goal = "Listened to more than 200 times",
        description = "This song was the soundtrack to your year. An obsession you couldn't stop listening to.",
        range = 0..200
    ),
    ANTHEM(
        goal = "Listened to more than 500 times",
        description = "It's not just a song, it's your anthem. It defined your summer, your winter, your life.",
        range = 201..500

    ),
    SOUNDTRACK(
        goal = "Listened to more than 1000 times",
        description = "This song isn't just an anthem, it's the soundtrack to your life. The rhythm that accompanies your days.",
        range = 501..1000

    ),
    ETERNAL_FLAME(
        goal = "Listened to more than 3000 times",
        description = "You and this song are one and the same. An eternal flame burning in your musical heart. A legend.",
        range = 1001..3000
    )
}


enum class AlbumLevel(val goal: String, val description: String, range: IntRange) {
    DEEP_DIVE(
        goal = "Listened to for over 1000 minutes",
        description = "You didn't stop at the singles. You dove deep into this masterpiece, note by note.",
        range = 0..1000

    ),
    ON_REPEAT(
        goal = "Listened to for over 2500 minutes",
        description = "This album was on repeat for weeks. You know every word, every pause, every beat.",
        range = 1001..2500

    ),
    RESIDENT(
        goal = "Listened to for over 5000 minutes",
        description = "You didn't just listen to this album, you lived in it. You're not just a listener, you're a resident.",
        range = 2501..5000
    ),
    SANCTUARY(
        goal = "Listened to for over 8000 minutes",
        description = "This album is more than music, it's your sanctuary. A sacred place to return to for peace and inspiration.",
        range = 5001..8000
    )
}


enum class PlaylistLevel(val goal: String, val description: String, range: IntRange) {
    CURATOR(
        goal = "Created and listened to for over 500 minutes",
        description = "You're not just a listener, you're a curator. You created the perfect soundtrack for a moment.",
        range = 0..500
    ),
    MASTERMIND(
        goal = "Created and listened to for over 1500 minutes",
        description = "Your playlist is a work of art. Maybe you should consider a career as a DJ.",
        range = 501..1500
    ),
    PHENOMENON(
        goal = "Created and listened to for over 3000 minutes",
        description = "This playlist isn't just a list of songs, it's a phenomenon. A cultural event in your world.",
        range = 1501..3000
    ),
    OPUS(
        goal = "Created and listened to for over 5000 minutes",
        description = "You didn't just create a playlist, you composed a masterpiece. This is your magnum opus, your legacy.",
        range = 3001..5000
    )
}


enum class ArtistLevel(val goal: String, val description: String, range: IntRange) {
    NEW_FAVORITE(
        goal = "Listened to for over 2,000 minutes",
        description = "You discovered a new favorite and can't stop listening. The beginning of a beautiful musical story.",
        range = 0..2000
    ),
    A_LIST_FAN(
        goal = "Listened to for over 5,000 minutes",
        description = "This artist made it to your A-List. Their music is a constant and loved presence in your routine.",
        range = 2001..5000
    ),
    THE_ARCHIVIST(
        goal = "Listened to for over 10,000 minutes",
        description = "You don't just stick to the hit singles. You've explored every corner of their discography, becoming a true expert.",
        range = 5001..10000
    ),
    THE_DEVOTEE(
        goal = "Listened to for over 20,000 minutes",
        description = "This artist's music is more than just sound, it's a part of you. A deep and unbreakable bond.",
        range = 10001..20000
    )
}


fun getRewindSlides(): List<RewindSlide> {
    return listOf(
        RewindSlide.IntroSlide(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFF))
            )
        ),
        RewindSlide.SongAchievement(
            songTitle = "Blinding Lights",
            artistName = "The Weeknd",
            albumArtRes = R.drawable.music_album,
            level = SongLevel.OBSESSION,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFF191414))
            )
        ),
        RewindSlide.AlbumAchievement(
            albumTitle = "After Hours",
            artistName = "The Weeknd",
            albumArtRes = R.drawable.music_album,
            level = AlbumLevel.DEEP_DIVE,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
            )
        ),
        RewindSlide.PlaylistAchievement(
            playlistName = "Top Hits 2023",
            songCount = 50,
            totalMinutes = 1234,
            level = PlaylistLevel.CURATOR,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
            )
        ),
        RewindSlide.ArtistAchievement(
            artistName = "The Weeknd",
            artistImageRes = R.drawable.artist,
            minutesListened = 4321,
            level = ArtistLevel.NEW_FAVORITE,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFF))
            )
        ),
        RewindSlide.TopArtist(
            artistName = "The Weeknd",
            artistImageRes = R.drawable.artist,
            minutesListened = 1234,
            artistGenre = "R&B",
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF))
            )
        ),
        RewindSlide.OutroSlide(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5))
            )
        )
    )
}