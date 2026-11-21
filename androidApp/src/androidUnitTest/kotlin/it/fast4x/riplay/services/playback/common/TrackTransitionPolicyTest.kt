package it.fast4x.riplay.services.playback.common

import it.fast4x.riplay.enums.CrossfadeDuration
import it.fast4x.riplay.services.playback.ActiveEngine
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackTransitionPolicyTest {

    @Test
    fun exoPlayerAllowsNativeGaplessTransitionWhenCrossfadeIsOff() {
        assertFalse(
            shouldExoPauseAtEndOfMediaItems(
                activeEngine = ActiveEngine.EXOPLAYER,
                crossfadeDuration = CrossfadeDuration.Off,
            ),
            "ExoPlayer must not pause at end of media items when active and crossfade is Off",
        )
        assertTrue(
            shouldExoPauseAtEndOfMediaItems(
                activeEngine = ActiveEngine.EXOPLAYER,
                crossfadeDuration = CrossfadeDuration.`5s`,
            ),
            "ExoPlayer should pause at end of media items when crossfade is active so watchdog controls fade-out",
        )
        assertTrue(
            shouldExoPauseAtEndOfMediaItems(
                activeEngine = ActiveEngine.YOUTUBE,
                crossfadeDuration = CrossfadeDuration.Off,
            ),
            "ExoPlayer placeholder must remain paused at end when YouTube engine is active",
        )
    }

    @Test
    fun naturalEndedSignalNeverRunsPostMortemSilenceFadeOut() {
        assertEquals(
            EndOfSongAction.IMMEDIATE_PLAY_NEXT,
            evaluateEndOfSongAction(
                source = "onStateChange ENDED",
                crossfadeDuration = CrossfadeDuration.`7s`,
                isNextLocal = false,
            ),
            "An already ENDED track must advance immediately instead of fading out silence for 7s",
        )
        assertEquals(
            EndOfSongAction.WEBVIEW_FADE_OUT,
            evaluateEndOfSongAction(
                source = "cambio con crossfade attivo",
                crossfadeDuration = CrossfadeDuration.`7s`,
                isNextLocal = false,
            ),
            "Early crossfade watchdog trigger should fade out active WebView audio",
        )
        assertEquals(
            EndOfSongAction.EXO_CROSSFADE_OUT,
            evaluateEndOfSongAction(
                source = "cambio con crossfade attivo",
                crossfadeDuration = CrossfadeDuration.`5s`,
                isNextLocal = true,
            ),
        )
    }

    @Test
    fun watchdogTriggersCrossfadeEarlyForYoutubeOnlineEngine() {
        val decision = evaluateWatchdogTransition(
            activeEngine = ActiveEngine.YOUTUBE,
            isPlaying = true,
            isFading = false,
            crossfadeDuration = CrossfadeDuration.`5s`,
            durationMs = 180_000L,
            positionMs = 175_200L,
            lastPositionMs = 175_000L,
            nearEndStallTicks = 0,
        )
        assertTrue(
            decision.shouldTriggerEndOfSong,
            "Watchdog must trigger early fade-out for YOUTUBE engine when crossfade is enabled",
        )
    }

    @Test
    fun watchdogDoesNotCutOffAdvancingLocalTrackEarlyWhenCrossfadeIsOff() {
        val decision = evaluateWatchdogTransition(
            activeEngine = ActiveEngine.EXOPLAYER,
            isPlaying = true,
            isFading = false,
            crossfadeDuration = CrossfadeDuration.Off,
            durationMs = 200_000L,
            positionMs = 199_100L, // 900ms left, still advancing (lastPositionMs = 198_900L)
            lastPositionMs = 198_900L,
            nearEndStallTicks = 0,
        )
        assertFalse(
            decision.shouldTriggerEndOfSong,
            "Advancing local track must not be cut off 1200ms early when crossfade is Off",
        )
    }

    @Test
    fun watchdogRecoversNearEndStallOnOnlineTrackAfterThreeFrozenTicks() {
        val tick1 = evaluateWatchdogTransition(
            activeEngine = ActiveEngine.YOUTUBE,
            isPlaying = true,
            isFading = false,
            crossfadeDuration = CrossfadeDuration.Off,
            durationMs = 180_000L,
            positionMs = 179_200L,
            lastPositionMs = 179_200L,
            nearEndStallTicks = 0,
        )
        assertFalse(tick1.shouldTriggerEndOfSong)
        assertEquals(1, tick1.nextNearEndStallTicks)

        val tick2 = evaluateWatchdogTransition(
            activeEngine = ActiveEngine.YOUTUBE,
            isPlaying = true,
            isFading = false,
            crossfadeDuration = CrossfadeDuration.Off,
            durationMs = 180_000L,
            positionMs = 179_200L,
            lastPositionMs = 179_200L,
            nearEndStallTicks = tick1.nextNearEndStallTicks,
        )
        assertFalse(tick2.shouldTriggerEndOfSong)
        assertEquals(2, tick2.nextNearEndStallTicks)

        val tick3 = evaluateWatchdogTransition(
            activeEngine = ActiveEngine.YOUTUBE,
            isPlaying = true,
            isFading = false,
            crossfadeDuration = CrossfadeDuration.Off,
            durationMs = 180_000L,
            positionMs = 179_200L,
            lastPositionMs = 179_200L,
            nearEndStallTicks = tick2.nextNearEndStallTicks,
        )
        assertTrue(
            tick3.shouldTriggerEndOfSong,
            "Frozen online track within 1.5s of end for 3 ticks (600ms) must trigger end-of-song transition",
        )
        assertEquals(0, tick3.nextNearEndStallTicks)
    }

    @Test
    fun onlineFadeInIsDeferredUntilPlayingAndExoAutoTransitionIsNotReSeeked() {
        assertTrue(
            shouldDeferOnlineFadeInUntilPlaying(
                isLocal = false,
                crossfadeDuration = CrossfadeDuration.`5s`,
            ),
            "Online crossfade must wait for PLAYING state before ramping fade-in volume",
        )
        assertFalse(
            shouldInterceptExoAutoTransitionWithSeek(CrossfadeDuration.Off),
            "Native ExoPlayer auto-transition must not trigger a redundant handlePlayNext seek",
        )
    }

    @Test
    fun aypHtmlSupportsDualDeckPreloadAndClearsProgressIntervalProperly() {
        val candidatePaths = listOf(
            File("../modules/ayp/src/main/res/raw/ayp_youtube_player.html"),
            File("modules/ayp/src/main/res/raw/ayp_youtube_player.html"),
        )
        val htmlFile = candidatePaths.firstOrNull { it.exists() }
            ?: error("Could not locate ayp_youtube_player.html")
        val html = htmlFile.readText()

        assertTrue(
            html.contains("function preloadVideo("),
            "ayp_youtube_player.html must expose preloadVideo(videoId, startSeconds) for standby deck pre-buffering",
        )
        assertTrue(
            html.contains("standbyPlayer.loadVideoById(videoId, startSec)"),
            "ayp_youtube_player.html must actively pre-buffer the standby audio stream via loadVideoById",
        )
        assertTrue(
            html.contains("standbyCued.prebuffered = true"),
            "ayp_youtube_player.html must pause and mark the standby deck prebuffered once buffered",
        )
        assertTrue(
            html.contains("function startCrossfade("),
            "ayp_youtube_player.html must expose startCrossfade(durationMs) for overlapping dual-deck crossfades",
        )
        assertTrue(
            html.contains("function startOutgoingDeckFadeOut("),
            "ayp_youtube_player.html must fade out the outgoing deck concurrently while the incoming deck fades in",
        )
        assertTrue(
            html.contains("if (volumePercent > 0 && typeof player.unMute === 'function')"),
            "ayp_youtube_player.html setVolume must unmute the promoted deck whenever volumePercent > 0",
        )
        assertTrue(
            html.contains("youTubePlayerDOMStandby"),
            "ayp_youtube_player.html must define standby deck container youTubePlayerDOMStandby",
        )
        assertTrue(
            html.contains("clearInterval(timerId)"),
            "ayp_youtube_player.html must clear the setInterval timerId with clearInterval(timerId)",
        )
    }
}
