package it.fast4x.riplay

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.utils.UiTypeKey
import it.fast4x.riplay.utils.audioQualityFormatKey
import it.fast4x.riplay.utils.autosyncKey
import it.fast4x.riplay.utils.bassboostEnabledKey
import it.fast4x.riplay.utils.colorPaletteModeKey
import it.fast4x.riplay.utils.dnsOverHttpsTypeKey
import it.fast4x.riplay.utils.exoPlayerMinTimeForEventKey
import it.fast4x.riplay.utils.getEnum
import it.fast4x.riplay.utils.handleAudioFocusEnabledKey
import it.fast4x.riplay.utils.lastVideoIdKey
import it.fast4x.riplay.utils.lastVideoSecondsKey
import it.fast4x.riplay.utils.logDebugEnabledKey
import it.fast4x.riplay.utils.parentalControlEnabledKey
import it.fast4x.riplay.utils.pauseListenHistoryKey
import it.fast4x.riplay.utils.preferences
import it.fast4x.riplay.utils.queueLoopTypeKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.showSearchTabKey
import it.fast4x.riplay.utils.showStatsInNavbarKey
import it.fast4x.riplay.utils.viewTypeKey
import it.fast4x.riplay.utils.ytAccountNameKey
import it.fast4x.riplay.utils.ytAccountThumbnailKey

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

fun ytAccountName() = appContext().preferences.getString(ytAccountNameKey, "")
fun ytAccountThumbnail() = appContext().preferences.getString(ytAccountThumbnailKey, "")
fun isAutoSyncEnabled() = appContext().preferences.getBoolean(autosyncKey, false)
fun isHandleAudioFocusEnabled() = appContext().preferences.getBoolean(handleAudioFocusEnabledKey, true)
fun isBassBoostEnabled() = appContext().preferences.getBoolean(bassboostEnabledKey, false)
fun isDebugModeEnabled() = appContext().preferences.getBoolean(logDebugEnabledKey, false)
fun isParentalControlEnabled() = appContext().preferences.getBoolean(parentalControlEnabledKey, false)
