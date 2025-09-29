package it.fast4x.riplay

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.extensions.preferences.UiTypeKey
import it.fast4x.riplay.extensions.preferences.appIsRunningKey
import it.fast4x.riplay.extensions.preferences.audioQualityFormatKey
import it.fast4x.riplay.extensions.preferences.autosyncKey
import it.fast4x.riplay.extensions.preferences.bassboostEnabledKey
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.dnsOverHttpsTypeKey
import it.fast4x.riplay.extensions.preferences.enablePictureInPictureAutoKey
import it.fast4x.riplay.extensions.preferences.exoPlayerMinTimeForEventKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.handleAudioFocusEnabledKey
import it.fast4x.riplay.extensions.preferences.isEnabledFullscreenKey
import it.fast4x.riplay.extensions.preferences.keepPlayerMinimizedKey
import it.fast4x.riplay.extensions.preferences.lastVideoIdKey
import it.fast4x.riplay.extensions.preferences.lastVideoSecondsKey
import it.fast4x.riplay.extensions.preferences.logDebugEnabledKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.pauseListenHistoryKey
import it.fast4x.riplay.extensions.preferences.persistentQueueKey
import it.fast4x.riplay.extensions.preferences.playbackFadeAudioDurationKey
import it.fast4x.riplay.extensions.preferences.playerTimelineTypeKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.resumePlaybackOnStartKey
import it.fast4x.riplay.extensions.preferences.showSearchTabKey
import it.fast4x.riplay.extensions.preferences.showStatsInNavbarKey
import it.fast4x.riplay.extensions.preferences.viewTypeKey
import it.fast4x.riplay.extensions.preferences.ytAccountNameKey
import it.fast4x.riplay.extensions.preferences.ytAccountThumbnailKey

@Composable
fun typography() = LocalAppearance.current.typography

@Composable
@ReadOnlyComposable
fun colorPalette() = LocalAppearance.current.colorPalette

@Composable
fun thumbnailShape() = LocalAppearance.current.thumbnailShape

@Composable
fun showSearchIconInNav() = rememberPreference( showSearchTabKey, false ).value

@Composable
fun showStatsIconInNav() = rememberPreference( showStatsInNavbarKey, false ).value

@Composable
fun binder() = LocalPlayerServiceBinder.current?.service

fun appContext(): Context = Dependencies.application.applicationContext
fun context(): Context = Dependencies.application

fun getColorTheme() = appContext().preferences.getEnum(colorPaletteModeKey, ColorPaletteMode.Dark)
fun getAudioQualityFormat() = appContext().preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)
fun getViewType() = appContext().preferences.getEnum(viewTypeKey, ViewType.Grid)
fun getDnsOverHttpsType() = appContext().preferences.getEnum(dnsOverHttpsTypeKey, DnsOverHttpsType.None)
fun getUiType() = appContext().preferences.getEnum(UiTypeKey, UiType.RiPlay)
fun getQueueLoopType() = appContext().preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default)
fun getPauseListenHistory() = appContext().preferences.getBoolean(pauseListenHistoryKey, false)
fun getMinTimeForEvent() = appContext().preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)
fun getLastYTVideoId() = appContext().preferences.getString(lastVideoIdKey, "")
fun getLastYTVideoSeconds() = appContext().preferences.getFloat(lastVideoSecondsKey, 0f)
fun getResumePlaybackOnStart() = appContext().preferences.getBoolean(resumePlaybackOnStartKey, false)
fun getPlayerTimelineType() = appContext().preferences.getEnum(playerTimelineTypeKey, PlayerTimelineType.Default)
fun getPlaybackFadeAudioDuration() = appContext().preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled)
fun getKeepPlayerMinimized() = appContext().preferences.getBoolean(keepPlayerMinimizedKey, false)

fun ytAccountName() = appContext().preferences.getString(ytAccountNameKey, "")
fun ytAccountThumbnail() = appContext().preferences.getString(ytAccountThumbnailKey, "")
fun isAutoSyncEnabled() = appContext().preferences.getBoolean(autosyncKey, false)
fun isHandleAudioFocusEnabled() = appContext().preferences.getBoolean(handleAudioFocusEnabledKey, true)
fun isBassBoostEnabled() = appContext().preferences.getBoolean(bassboostEnabledKey, false)
fun isDebugModeEnabled() = appContext().preferences.getBoolean(logDebugEnabledKey, false)
fun isParentalControlEnabled() = appContext().preferences.getBoolean(parentalControlEnabledKey, false)
fun isPersistentQueueEnabled() = appContext().preferences.getBoolean(persistentQueueKey, true)
fun isPipModeAutoEnabled() = appContext().preferences.getBoolean(enablePictureInPictureAutoKey, false)
fun isEnabledFullscreen() = appContext().preferences.getBoolean(isEnabledFullscreenKey, false)
fun isAppRunning() = appContext().preferences.getBoolean(appIsRunningKey, false)



