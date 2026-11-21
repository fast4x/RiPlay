package it.fast4x.riplay.extensions.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceCommandParserTest {

    private fun parse(input: String) = VoiceCommandParser.parse(input)

    @Test
    fun playsSongByName() {
        assertEquals(VoiceCommand.PlaySong("faded"), parse("play faded"))
        assertEquals(VoiceCommand.PlaySong("faded by alan walker"), parse("Play Faded by Alan Walker!"))
    }

    @Test
    fun ignoresWakeWordAndPoliteness() {
        assertEquals(VoiceCommand.PlaySong("faded"), parse("Hey RiPlay, could you play the song Faded"))
        assertEquals(VoiceCommand.PlaySong("faded"), parse("Hello, play Faded"))
        assertEquals(VoiceCommand.Next, parse("hello next"))
        assertEquals(VoiceCommand.Pause, parse("Hello RiPlay, pause"))
    }

    @Test
    fun keepsNumericSongTitles() {
        assertEquals(VoiceCommand.PlaySong("song 2"), parse("play song 2"))
    }

    @Test
    fun transportCommands() {
        assertEquals(VoiceCommand.Pause, parse("pause"))
        assertEquals(VoiceCommand.Pause, parse("stop the music"))
        assertEquals(VoiceCommand.Resume, parse("continue"))
        assertEquals(VoiceCommand.Resume, parse("resume playback"))
        assertEquals(VoiceCommand.Resume, parse("play"))
        assertEquals(VoiceCommand.Resume, parse("play some music"))
    }

    @Test
    fun skipCommands() {
        assertEquals(VoiceCommand.Next, parse("next song"))
        assertEquals(VoiceCommand.Next, parse("skip"))
        assertEquals(VoiceCommand.Next, parse("play the next song"))
        assertEquals(VoiceCommand.Previous, parse("previous song"))
        assertEquals(VoiceCommand.Previous, parse("go back"))
        assertEquals(VoiceCommand.Previous, parse("go to the previous track"))
    }

    @Test
    fun replayCommands() {
        assertEquals(VoiceCommand.Replay, parse("replay"))
        assertEquals(VoiceCommand.Replay, parse("play it again"))
        assertEquals(VoiceCommand.Replay, parse("start over"))
    }

    @Test
    fun loopCommands() {
        assertEquals(VoiceCommand.LoopSong, parse("loop the song"))
        assertEquals(VoiceCommand.LoopSong, parse("repeat this track"))
        assertEquals(VoiceCommand.LoopSong, parse("loop it"))
        assertEquals(VoiceCommand.LoopQueue, parse("loop"))
        assertEquals(VoiceCommand.LoopQueue, parse("repeat the queue"))
        assertEquals(VoiceCommand.LoopOff, parse("stop looping"))
        assertEquals(VoiceCommand.LoopOff, parse("turn off repeat"))
    }

    @Test
    fun shuffleCommands() {
        assertEquals(VoiceCommand.Shuffle, parse("shuffle"))
        assertEquals(VoiceCommand.Shuffle, parse("shuffle the song"))
        assertEquals(VoiceCommand.Shuffle, parse("shuffle the queue"))
    }

    @Test
    fun playlistCommands() {
        assertEquals(VoiceCommand.PlayPlaylist("gym"), parse("play songs from my gym playlist"))
        assertEquals(VoiceCommand.PlayPlaylist("chill"), parse("play my chill playlist"))
        assertEquals(VoiceCommand.PlayPlaylist("road trip"), parse("play playlist road trip"))
        assertEquals(VoiceCommand.PlayPlaylist("workout"), parse("start the workout playlist"))
    }

    @Test
    fun unknownSentencesAreRejected() {
        assertEquals(VoiceCommand.Unknown, parse("what's the weather like"))
        assertEquals(VoiceCommand.Unknown, parse(""))
        assertEquals(VoiceCommand.Unknown, parse("   "))
    }

    @Test
    fun detectsHandsFreeWakeWord() {
        assertTrue(VoiceCommandParser.hasWakeWord("RiPlay, pause"))
        assertTrue(VoiceCommandParser.hasWakeWord("hey ri play next song"))
        assertTrue(VoiceCommandParser.hasWakeWord("OK RiPlay stop the music"))
        assertTrue(VoiceCommandParser.hasWakeWord("Hello, play Faded"))
        assertTrue(VoiceCommandParser.hasWakeWord("hello riplay pause"))
        assertFalse(VoiceCommandParser.hasWakeWord("pause"))
        assertFalse(VoiceCommandParser.hasWakeWord("stop the music"))
        assertFalse(VoiceCommandParser.hasWakeWord("the song says riplay in the chorus"))
        assertFalse(VoiceCommandParser.hasWakeWord("well hello there"))
    }

    @Test
    fun detectsFuzzyAndClippedWakeWords() {
        assertTrue(VoiceCommandParser.hasWakeWord("ryplay next song"))
        assertTrue(VoiceCommandParser.hasWakeWord("riply pause"))
        assertTrue(VoiceCommandParser.hasWakeWord("i play next"))
        assertTrue(VoiceCommandParser.hasWakeWord("helo next"))
        assertTrue(VoiceCommandParser.hasWakeWord("ello pause"))
        assertFalse(VoiceCommandParser.hasWakeWord("replay"))
        assertFalse(VoiceCommandParser.hasWakeWord("replay song"))
        assertFalse(VoiceCommandParser.hasWakeWord("player next"))
    }

    @Test
    fun recognizesWakeWordOnlyUtterances() {
        assertTrue(VoiceCommandParser.isWakeWordOnly("Hello"))
        assertTrue(VoiceCommandParser.isWakeWordOnly("Hello RiPlay"))
        assertTrue(VoiceCommandParser.isWakeWordOnly("hey riplay"))
        assertFalse(VoiceCommandParser.isWakeWordOnly("hello next"))
        assertFalse(VoiceCommandParser.isWakeWordOnly("next"))
    }

    @Test
    fun greetingFillerDoesNotSwallowTheCommand() {
        assertFalse(VoiceCommandParser.hasWakeWord("hey play faded"))
        assertEquals(VoiceCommand.PlaySong("faded"), parse("hey play faded"))
        assertEquals(VoiceCommand.Next, parse("hi next"))
        assertEquals(VoiceCommand.Pause, parse("ok pause"))
    }

    @Test
    fun treatsMisheardHelloRiPlayAsWakeWord() {
        assertTrue(VoiceCommandParser.isWakeWordOnly("hello replay"))
        assertEquals(
            HandsFreeSelection.WakeWordOnly,
            VoiceCommandParser.selectHandsFree(listOf("hello replay"))
        )
        assertEquals(VoiceCommand.Next, parse("hello replay next"))
        assertEquals(VoiceCommand.Replay, parse("replay"))
    }

    @Test
    fun stripsRepeatedWakeWords() {
        assertEquals(VoiceCommand.Next, parse("hello hello next"))
        assertEquals(VoiceCommand.Pause, parse("hello riplay pause"))
    }

    @Test
    fun selectsHandsFreeHypotheses() {
        assertEquals(
            HandsFreeSelection.Command("hello play faded", wakeWord = true),
            VoiceCommandParser.selectHandsFree(listOf("hello play faded"))
        )
        assertEquals(
            HandsFreeSelection.Command("hello next", wakeWord = true),
            VoiceCommandParser.selectHandsFree(listOf("hello next"))
        )
        assertEquals(
            HandsFreeSelection.WakeWordOnly,
            VoiceCommandParser.selectHandsFree(listOf("riplay"))
        )
        assertEquals(
            HandsFreeSelection.WakeWordOnly,
            VoiceCommandParser.selectHandsFree(listOf("hello"))
        )
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(listOf("play faded"))
        )
        assertEquals(
            HandsFreeSelection.Command("play faded", wakeWord = false),
            VoiceCommandParser.selectHandsFree(listOf("hello", "play faded"))
        )
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(listOf("we can leave the christmas lights"))
        )
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(emptyList())
        )
    }

    @Test
    fun handsFreeIgnoresLyricsWithoutWakeWord() {
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(listOf("stop the music"))
        )
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(listOf("pause"))
        )
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(
                listOf("hello darkness my old friend", "stop the music")
            )
        )
    }

    @Test
    fun detectorSessionAcceptsACommandWithAClippedWakeWord() {
        assertEquals(
            HandsFreeSelection.Command("play faded", wakeWord = false),
            VoiceCommandParser.selectHandsFree(
                listOf("play faded"),
                allowCommandWithoutWakeWord = true
            )
        )
        assertEquals(
            HandsFreeSelection.Command("next", wakeWord = false),
            VoiceCommandParser.selectHandsFree(
                listOf("riplay", "next"),
                allowCommandWithoutWakeWord = true
            )
        )
        assertEquals(
            HandsFreeSelection.Ignore,
            VoiceCommandParser.selectHandsFree(
                listOf("we can leave the christmas lights"),
                allowCommandWithoutWakeWord = true
            )
        )
    }

    @Test
    fun matcherFindsPlaylistsDespiteRecognitionNoise() {
        val playlists = listOf("Chill Vibes", "Workout", "Road Trip")
        assertEquals("Chill Vibes", VoiceNameMatcher.bestMatch("chill", playlists))
        assertEquals("Workout", VoiceNameMatcher.bestMatch("wrokout", playlists))
        assertEquals("Road Trip", VoiceNameMatcher.bestMatch("roadtrip", playlists))
        assertNull(VoiceNameMatcher.bestMatch("completely unrelated name", playlists))
    }

    @Test
    fun matcherPrefersExactSongTitles() {
        val titles = listOf("Faded (Remastered)", "Faded Love", "Fade")
        assertEquals(2, VoiceNameMatcher.bestMatchIndex("fade", titles))
        assertEquals(0, VoiceNameMatcher.bestMatchIndex("faded", titles))
    }

    @Test
    fun exactNamesScoreHighest() {
        assertEquals(1.0, VoiceNameMatcher.score("Chill", "chill"))
    }
}
