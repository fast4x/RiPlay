package it.fast4x.riplay.extensions.experimental.appearancepreset

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTIONS_SPACED_EVENLY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTIVE_APPEARANCE_PRESET_ID
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ALBUM_COVER_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ANIMATED_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLACK_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLUR_SCALE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BOTTOM_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BUTTON_ZOOM_OUT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CONTROLS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_PLAYER_HORIZONTAL_SWIPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER_TOGGLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FADING_EDGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ICON_LIKE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINI_QUEUE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NO_BLUR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_BACKGROUND_COLORS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_CONTROLS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_SHOW_ICONS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_SWAP_CONTROLS_WITH_TIMELINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_THUMBNAIL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TYPE
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PREV_NEXT_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_DURATION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BACKGROUND_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ARROW
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LOOP
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_MENU
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SHUFFLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SLEEP_TIMER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_START_RADIO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_VIDEO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LIKE_BUTTON_BACKGROUND_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEXT_SONGS_IN_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PLAYER_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_REMAINING_SONG_TIME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOTAL_TIME_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ALBUM_COVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LYRICS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_VIS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_FOR_NERDS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_UP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TAP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TEXT_OUTLINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE_EX
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_SPACING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_PAUSE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TIMELINE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TITLE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TOP_PADDING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VISUALIZER_ENABLED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AppearancePreferences(context: Context) {
    private val prefs = context.preferences

    fun activePresetIdFlow(): Flow<String?> = callbackFlow {
        trySend(prefs.getString(ACTIVE_APPEARANCE_PRESET_ID.key, null))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ACTIVE_APPEARANCE_PRESET_ID.key) {
                trySend(prefs.getString("activePresetId", null))
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun applyFrom(settings: AppearanceSettings, presetId: String? = null) {

        prefs.edit {
            with(settings) {
                putString(ACTIVE_APPEARANCE_PRESET_ID.key, presetId)

                // PLAYER
                putBoolean(SHOW_TOP_ACTIONS_BAR.key,               showTopActionsBar)
                putBoolean(SHOW_THUMBNAIL.key,                   showThumbnail)
                putString (PLAYER_BACKGROUND_COLORS.key,          playerBackgroundColors.name)
                putFloat  (BLUR_SCALE.key,                    blurStrength)
                putString (THUMBNAIL_ROUNDNESS.key,              thumbnailRoundness.name)
                putString (THUMBNAIL_TYPE.key,                   thumbnailType.name)
                putString (PLAYER_THUMBNAIL_SIZE.key,             playerThumbnailSize.name)
                putBoolean(TRANSPARENT_BAR.key,                  transparentBar)
                putBoolean(BOTTOM_GRADIENT.key,                  bottomGradient)
                putBoolean(EXPANDED_PLAYER_TOGGLE.key,                  expandedPlayer)
                putBoolean(SHOW_LYRICS_THUMBNAIL.key,             showLyricsThumbnail)
                putString (PLAYER_INFO_TYPE.key,                  playerInfoType.name)
                putString (PLAYER_TYPE.key,                      playerType.name)
                putString (PLAYER_TIMELINE_TYPE.key,              playerTimelineType.name)
                putString (PLAYER_TIMELINE_SIZE.key,              playerTimelineSize.name)
                putString (PLAYER_CONTROLS_TYPE.key,              playerControlsType.name)
                putString (PLAYER_PLAY_BUTTON_TYPE.key,            playerPlayButtonType.name)
                putBoolean(SHOW_TOTAL_TIME_QUEUE.key,              showTotalTimeQueue)
                putBoolean(SHOW_REMAINING_SONG_TIME.key,           showRemainingSongTime)
                putBoolean(SHOW_NEXT_SONGS_IN_PLAYER.key,           showNextSongsInPlayer)
                putString (COLOR_PALETTE_NAME.key,                colorPaletteName.name)
                putString (COLOR_PALETTE_MODE.key,                colorPaletteMode.name)
                putBoolean(TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,  transparentBackgroundActionBarPlayer)
                putBoolean(ACTIONS_SPACED_EVENLY.key,             actionsSpacedEvenly)
                putBoolean(SHOW_BUTTON_PLAYER_VIDEO.key,           showButtonPlayerVideo)
                putBoolean(SHOW_BUTTON_PLAYER_DISCOVER.key,        showButtonPlayerDiscover)
                putBoolean(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key,   showButtonPlayerAddToPlaylist)
                putBoolean(SHOW_BUTTON_PLAYER_LOOP.key,            showButtonPlayerLoop)
                putBoolean(SHOW_BUTTON_PLAYER_SHUFFLE.key,         showButtonPlayerShuffle)
                putBoolean(SHOW_BUTTON_PLAYER_LYRICS.key,          showButtonPlayerLyrics)
                putBoolean(EXPANDED_PLAYER_TOGGLE.key,            expandedPlayerToggle)
                putBoolean(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key,      showButtonPlayerSleepTimer)
                putBoolean(VISUALIZER_ENABLED.key,               visualizerEnabled)
                putBoolean(SHOW_BUTTON_PLAYER_ARROW.key,           showButtonPlayerArrow)
                putBoolean(SHOW_BUTTON_PLAYER_START_RADIO.key,      showButtonPlayerStartRadio)
                putBoolean(SHOW_BUTTON_PLAYER_MENU.key,            showButtonPlayerMenu)
                putBoolean(SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key, showButtonPlayerSystemEqualizer)
                putBoolean(SHOW_BACKGROUND_LYRICS.key, showBackgroundLyrics)
                putBoolean(SHOW_PLAYER_ACTIONS_BAR.key, showPlayerActionsBar)
                putString(ICON_LIKE_TYPE.key, iconLikeType.name)
                putBoolean(PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key, playerSwapControlsWithTimeline)
                putBoolean(SHOW_VIS_THUMBNAIL.key, showvisthumbnail)
                putBoolean(BUTTON_ZOOM_OUT.key, buttonzoomout)
                putBoolean(THUMBNAIL_PAUSE.key, thumbnailpause)
                putString(SHOW_SONGS.key, showsongs.name)
                putBoolean(SHOW_ALBUM_COVER.key, showalbumcover)
                putString(PREV_NEXT_SONGS.key, prevNextSongs.name)
                putBoolean(TAP_QUEUE.key, tapqueue)
                putBoolean(SWIPE_UP_QUEUE.key, swipeUpQueue)
                putBoolean(STATS_FOR_NERDS.key, statsfornerds)
                putString(QUEUE_TYPE.key, queueType.name)
                putBoolean(NO_BLUR.key, noblur)
                putBoolean(FADING_EDGE.key, fadingedge)
                putBoolean(CAROUSEL.key, carousel)
                putString(CAROUSEL_SIZE.key, carouselSize.name)
                putBoolean(KEEP_PLAYER_MINIMIZED.key, keepPlayerMinimized)
                putBoolean(PLAYER_INFO_SHOW_ICONS.key, playerInfoShowIcons)
                putBoolean(QUEUE_DURATION_EXPANDED.key, queueDurationExpanded)
                putBoolean(TITLE_EXPANDED.key, titleExpanded)
                putBoolean(TIMELINE_EXPANDED.key, timelineExpanded)
                putBoolean(CONTROLS_EXPANDED.key, controlsExpanded)
                putBoolean(MINI_QUEUE_EXPANDED.key, miniQueueExpanded)
                putBoolean(STATS_EXPANDED.key, statsExpanded)
                putBoolean(ACTION_EXPANDED.key, actionExpanded)
                putBoolean(SHOW_COVER_THUMBNAIL_ANIMATION.key, showCoverThumbnailAnimation)
                putString(COVER_THUMBNAIL_ANIMATION.key, coverThumbnailAnimation.name)
                putBoolean(TOP_PADDING.key, topPadding)
                putString(ANIMATED_GRADIENT.key, animatedGradient.name)
                putBoolean(ALBUM_COVER_ROTATION.key, albumCoverRotation)
                putFloat(THUMBNAIL_FADE_EX.key, thumbnailFadeEx)
                putFloat(THUMBNAIL_FADE.key, thumbnailFade)
                putFloat(THUMBNAIL_SPACING.key, thumbnailSpacing)
                putBoolean(BLACK_GRADIENT.key, blackgradient)
                putBoolean(TEXT_OUTLINE.key, textoutline)
                putBoolean(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, disablePlayerHorizontalSwipe)
                putBoolean(SHOW_LIKE_BUTTON_BACKGROUND_PLAYER.key, showLikeButtonBackgroundPlayer)

                //APP

            }
        }
    }

    companion object {
        @Volatile private var instance: AppearancePreferences? = null

        fun getInstance(context: Context): AppearancePreferences =
            instance ?: synchronized(this) {
                instance ?: AppearancePreferences(context.applicationContext).also { instance = it }
            }
    }
}