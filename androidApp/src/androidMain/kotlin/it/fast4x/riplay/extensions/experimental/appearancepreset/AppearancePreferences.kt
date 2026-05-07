package it.fast4x.riplay.extensions.experimental.appearancepreset

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.preferences.actionExpandedKey
import it.fast4x.riplay.extensions.preferences.actionspacedevenlyKey
import it.fast4x.riplay.extensions.preferences.activeAppearancePresetIdKey
import it.fast4x.riplay.extensions.preferences.albumCoverRotationKey
import it.fast4x.riplay.extensions.preferences.animatedGradientKey
import it.fast4x.riplay.extensions.preferences.blackgradientKey
import it.fast4x.riplay.extensions.preferences.blurStrengthKey
import it.fast4x.riplay.extensions.preferences.bottomgradientKey
import it.fast4x.riplay.extensions.preferences.buttonzoomoutKey
import it.fast4x.riplay.extensions.preferences.carouselKey
import it.fast4x.riplay.extensions.preferences.carouselSizeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.extensions.preferences.controlsExpandedKey
import it.fast4x.riplay.extensions.preferences.coverThumbnailAnimationKey
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.expandedplayertoggleKey
import it.fast4x.riplay.extensions.preferences.fadingedgeKey
import it.fast4x.riplay.extensions.preferences.iconLikeTypeKey
import it.fast4x.riplay.extensions.preferences.keepPlayerMinimizedKey
import it.fast4x.riplay.extensions.preferences.miniQueueExpandedKey
import it.fast4x.riplay.extensions.preferences.noblurKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerControlsTypeKey
import it.fast4x.riplay.extensions.preferences.playerInfoShowIconsKey
import it.fast4x.riplay.extensions.preferences.playerInfoTypeKey
import it.fast4x.riplay.extensions.preferences.playerPlayButtonTypeKey
import it.fast4x.riplay.extensions.preferences.playerSwapControlsWithTimelineKey
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeKey
import it.fast4x.riplay.extensions.preferences.playerTimelineSizeKey
import it.fast4x.riplay.extensions.preferences.playerTimelineTypeKey
import it.fast4x.riplay.extensions.preferences.playerTypeKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.prevNextSongsKey
import it.fast4x.riplay.extensions.preferences.queueDurationExpandedKey
import it.fast4x.riplay.extensions.preferences.queueTypeKey
import it.fast4x.riplay.extensions.preferences.showBackgroundLyricsKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerAddToPlaylistKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerArrowKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerDiscoverKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerLoopKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerLyricsKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerMenuKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerShuffleKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerSleepTimerKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerStartRadioKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerSystemEqualizerKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerVideoKey
import it.fast4x.riplay.extensions.preferences.showCoverThumbnailAnimationKey
import it.fast4x.riplay.extensions.preferences.showLikeButtonBackgroundPlayerKey
import it.fast4x.riplay.extensions.preferences.showNextSongsInPlayerKey
import it.fast4x.riplay.extensions.preferences.showPlayerActionsBarKey
import it.fast4x.riplay.extensions.preferences.showRemainingSongTimeKey
import it.fast4x.riplay.extensions.preferences.showTopActionsBarKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.extensions.preferences.showalbumcoverKey
import it.fast4x.riplay.extensions.preferences.showlyricsthumbnailKey
import it.fast4x.riplay.extensions.preferences.showsongsKey
import it.fast4x.riplay.extensions.preferences.showthumbnailKey
import it.fast4x.riplay.extensions.preferences.showvisthumbnailKey
import it.fast4x.riplay.extensions.preferences.statsExpandedKey
import it.fast4x.riplay.extensions.preferences.statsfornerdsKey
import it.fast4x.riplay.extensions.preferences.swipeUpQueueKey
import it.fast4x.riplay.extensions.preferences.tapqueueKey
import it.fast4x.riplay.extensions.preferences.textoutlineKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeExKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.thumbnailSpacingKey
import it.fast4x.riplay.extensions.preferences.thumbnailTypeKey
import it.fast4x.riplay.extensions.preferences.thumbnailpauseKey
import it.fast4x.riplay.extensions.preferences.timelineExpandedKey
import it.fast4x.riplay.extensions.preferences.titleExpandedKey
import it.fast4x.riplay.extensions.preferences.topPaddingKey
import it.fast4x.riplay.extensions.preferences.transparentBackgroundPlayerActionBarKey
import it.fast4x.riplay.extensions.preferences.transparentbarKey
import it.fast4x.riplay.extensions.preferences.visualizerEnabledKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AppearancePreferences(context: Context) {
    private val prefs = context.preferences

    fun activePresetIdFlow(): Flow<String?> = callbackFlow {
        trySend(prefs.getString(activeAppearancePresetIdKey, null))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == activeAppearancePresetIdKey) {
                trySend(prefs.getString("activePresetId", null))
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun applyFrom(settings: AppearanceSettings, presetId: String? = null) {

        prefs.edit {
            with(settings) {
                putString(activeAppearancePresetIdKey, presetId)

                // PLAYER
                putBoolean(showTopActionsBarKey,               showTopActionsBar)
                putBoolean(showthumbnailKey,                   showThumbnail)
                putString (playerBackgroundColorsKey,          playerBackgroundColors.name)
                putFloat  (blurStrengthKey,                    blurStrength)
                putString (thumbnailRoundnessKey,              thumbnailRoundness.name)
                putString (thumbnailTypeKey,                   thumbnailType.name)
                putString (playerThumbnailSizeKey,             playerThumbnailSize.name)
                putBoolean(transparentbarKey,                  transparentBar)
                putBoolean(bottomgradientKey,                  bottomGradient)
                putBoolean(expandedplayertoggleKey,                  expandedPlayer)
                putBoolean(showlyricsthumbnailKey,             showLyricsThumbnail)
                putString (playerInfoTypeKey,                  playerInfoType.name)
                putString (playerTypeKey,                      playerType.name)
                putString (playerTimelineTypeKey,              playerTimelineType.name)
                putString (playerTimelineSizeKey,              playerTimelineSize.name)
                putString (playerControlsTypeKey,              playerControlsType.name)
                putString (playerPlayButtonTypeKey,            playerPlayButtonType.name)
                putBoolean(showTotalTimeQueueKey,              showTotalTimeQueue)
                putBoolean(showRemainingSongTimeKey,           showRemainingSongTime)
                putBoolean(showNextSongsInPlayerKey,           showNextSongsInPlayer)
                putString (colorPaletteNameKey,                colorPaletteName.name)
                putString (colorPaletteModeKey,                colorPaletteMode.name)
                putBoolean(transparentBackgroundPlayerActionBarKey,  transparentBackgroundActionBarPlayer)
                putBoolean(actionspacedevenlyKey,             actionsSpacedEvenly)
                putBoolean(showButtonPlayerVideoKey,           showButtonPlayerVideo)
                putBoolean(showButtonPlayerDiscoverKey,        showButtonPlayerDiscover)
                putBoolean(showButtonPlayerAddToPlaylistKey,   showButtonPlayerAddToPlaylist)
                putBoolean(showButtonPlayerLoopKey,            showButtonPlayerLoop)
                putBoolean(showButtonPlayerShuffleKey,         showButtonPlayerShuffle)
                putBoolean(showButtonPlayerLyricsKey,          showButtonPlayerLyrics)
                putBoolean(expandedplayertoggleKey,            expandedPlayerToggle)
                putBoolean(showButtonPlayerSleepTimerKey,      showButtonPlayerSleepTimer)
                putBoolean(visualizerEnabledKey,               visualizerEnabled)
                putBoolean(showButtonPlayerArrowKey,           showButtonPlayerArrow)
                putBoolean(showButtonPlayerStartRadioKey,      showButtonPlayerStartRadio)
                putBoolean(showButtonPlayerMenuKey,            showButtonPlayerMenu)
                putBoolean(showButtonPlayerSystemEqualizerKey, showButtonPlayerSystemEqualizer)
                putBoolean(showBackgroundLyricsKey, showBackgroundLyrics)
                putBoolean(showPlayerActionsBarKey, showPlayerActionsBar)
                putString(iconLikeTypeKey, iconLikeType.name)
                putBoolean(playerSwapControlsWithTimelineKey, playerSwapControlsWithTimeline)
                putBoolean(showvisthumbnailKey, showvisthumbnail)
                putBoolean(buttonzoomoutKey, buttonzoomout)
                putBoolean(thumbnailpauseKey, thumbnailpause)
                putString(showsongsKey, showsongs.name)
                putBoolean(showalbumcoverKey, showalbumcover)
                putString(prevNextSongsKey, prevNextSongs.name)
                putBoolean(tapqueueKey, tapqueue)
                putBoolean(swipeUpQueueKey, swipeUpQueue)
                putBoolean(statsfornerdsKey, statsfornerds)
                putString(queueTypeKey, queueType.name)
                putBoolean(noblurKey, noblur)
                putBoolean(fadingedgeKey, fadingedge)
                putBoolean(carouselKey, carousel)
                putString(carouselSizeKey, carouselSize.name)
                putBoolean(keepPlayerMinimizedKey, keepPlayerMinimized)
                putBoolean(playerInfoShowIconsKey, playerInfoShowIcons)
                putBoolean(queueDurationExpandedKey, queueDurationExpanded)
                putBoolean(titleExpandedKey, titleExpanded)
                putBoolean(timelineExpandedKey, timelineExpanded)
                putBoolean(controlsExpandedKey, controlsExpanded)
                putBoolean(miniQueueExpandedKey, miniQueueExpanded)
                putBoolean(statsExpandedKey, statsExpanded)
                putBoolean(actionExpandedKey, actionExpanded)
                putBoolean(showCoverThumbnailAnimationKey, showCoverThumbnailAnimation)
                putString(coverThumbnailAnimationKey, coverThumbnailAnimation.name)
                putBoolean(topPaddingKey, topPadding)
                putString(animatedGradientKey, animatedGradient.name)
                putBoolean(albumCoverRotationKey, albumCoverRotation)
                putFloat(thumbnailFadeExKey, thumbnailFadeEx)
                putFloat(thumbnailFadeKey, thumbnailFade)
                putFloat(thumbnailSpacingKey, thumbnailSpacing)
                putBoolean(blackgradientKey, blackgradient)
                putBoolean(textoutlineKey, textoutline)
                putBoolean(disablePlayerHorizontalSwipeKey, disablePlayerHorizontalSwipe)
                putBoolean(showLikeButtonBackgroundPlayerKey, showLikeButtonBackgroundPlayer)

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