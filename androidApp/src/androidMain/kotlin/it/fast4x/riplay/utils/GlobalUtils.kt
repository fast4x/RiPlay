package it.fast4x.riplay.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.extensions.preferences.PreferenceKey.UI_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BASSBOOST_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DNS_OVER_HTTPS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_PICTURE_IN_PICTURE_AUTO
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.PreferenceKey.HANDLE_AUDIO_FOCUS_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_ENABLED_FULLSCREEN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_ENABLED_LASTFM
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_KEEP_SCREEN_ON_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LASTFM_SESSION_TOKEN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PARENTAL_CONTROL_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PERSISTENT_QUEUE
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ALL_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FAVORITES_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_GRID_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_IN_LIBRARY_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MONTHLY_PLAYLISTS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ON_DEVICE_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PINNED_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PODCAST_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SEARCH_TAB
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_STATS_IN_NAVBAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SHUFFLE_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SKIP_MEDIA_ON_ERROR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VIEW_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_ACCOUNT_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.YT_ACCOUNT_THUMBNAIL
import it.fast4x.riplay.cast.ritune.models.RiTuneDevice
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_CONNECTION_METERED_ENABLED

@Composable
fun typography() = LocalAppearance.current.typography

@Composable
@ReadOnlyComposable
fun colorPalette() = LocalAppearance.current.colorPalette

@Composable
fun thumbnailShape() = LocalAppearance.current.thumbnailShape

@Composable
fun showSearchIconInNav() = rememberPreference( SHOW_SEARCH_TAB.key, false ).value

@Composable
fun showStatsIconInNav() = rememberPreference( SHOW_STATS_IN_NAVBAR.key, false ).value

@Composable
fun binder() = LocalPlayerServiceBinder.current

fun appContext(): Context = Dependencies.application.applicationContext
fun globalContext(): Context = Dependencies.application

fun getColorTheme() = appContext().preferences.getEnum(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
fun getViewType() = appContext().preferences.getEnum(VIEW_TYPE.key, ViewType.Grid)
fun getDnsOverHttpsType() = appContext().preferences.getEnum(DNS_OVER_HTTPS_TYPE.key, DnsOverHttpsType.None)
fun getUiType() = appContext().preferences.getEnum(UI_TYPE.key, UiType.RiPlay)
fun getKeepPlayerMinimized() = appContext().preferences.getBoolean(KEEP_PLAYER_MINIMIZED.key, false)
fun getlastFmSessionKey() = appContext().preferences.getString(LASTFM_SESSION_TOKEN.key, "")
fun getRoundnessShape() = appContext().preferences.getEnum(THUMBNAIL_ROUNDNESS.key,ThumbnailRoundness.Light).shape()


fun ytAccountName() = appContext().preferences.getString(YT_ACCOUNT_NAME.key, "")
fun ytAccountThumbnail() = appContext().preferences.getString(YT_ACCOUNT_THUMBNAIL.key, "")
fun isHandleAudioFocusEnabled() = appContext().preferences.getBoolean(HANDLE_AUDIO_FOCUS_ENABLED.key, true)
fun isBassBoostEnabled() = appContext().preferences.getBoolean(BASSBOOST_ENABLED.key, false)
fun isParentalControlEnabled() = appContext().preferences.getBoolean(PARENTAL_CONTROL_ENABLED.key, false)
fun isPersistentQueueEnabled() = appContext().preferences.getBoolean(PERSISTENT_QUEUE.key, true)
fun isPipModeAutoEnabled() = appContext().preferences.getBoolean(ENABLE_PICTURE_IN_PICTURE_AUTO.key, false)
fun isEnabledFullscreen() = appContext().preferences.getBoolean(IS_ENABLED_FULLSCREEN.key, false)
fun isSkipMediaOnErrorEnabled() = appContext().preferences.getBoolean(SKIP_MEDIA_ON_ERROR.key, false)
fun isKeepScreenOnEnabled() = appContext().preferences.getBoolean(IS_KEEP_SCREEN_ON_ENABLED.key, false)
fun isEnabledLastFm() = appContext().preferences.getBoolean(IS_ENABLED_LASTFM.key, false)
        && getlastFmSessionKey()?.isNotEmpty() == true

fun isConnectionMetered() = appContext().isConnectionMetered()
fun isConnectionMeteredEnabled() = appContext().preferences.getBoolean(IS_CONNECTION_METERED_ENABLED.key, true)



fun shuffleSongsAAEnabled() = appContext().preferences.getBoolean(SHOW_SHUFFLE_SONGS_AA.key, true)
fun showMonthlyPlaylistsAA() = appContext().preferences.getBoolean(SHOW_MONTHLY_PLAYLISTS_AA.key, true)
fun showOnDeviceAA() = appContext().preferences.getBoolean(SHOW_ON_DEVICE_AA.key, true)
fun showInLibraryAA() = appContext().preferences.getBoolean(SHOW_IN_LIBRARY_AA.key, true)
fun showFavoritesSongsAA() = appContext().preferences.getBoolean(SHOW_FAVORITES_SONGS_AA.key, true)
fun showTopSongstAA() = appContext().preferences.getBoolean(SHOW_TOP_SONGS_AA.key, true)
fun showAllSongstAA() = appContext().preferences.getBoolean(SHOW_ALL_SONGS_AA.key, true)
fun showPodcastAA() = appContext().preferences.getBoolean(SHOW_PODCAST_AA.key, true)
fun showPinnedAA() = appContext().preferences.getBoolean(SHOW_PINNED_AA.key, true)
fun showGridAA() = appContext().preferences.getBoolean(SHOW_GRID_AA.key, true)


object GlobalSharedData {
    var riTuneDevices: MutableState<List<RiTuneDevice>> = mutableStateOf(emptyList())
    var riTuneConnected = mutableStateOf(false)
    var riTuneError: MutableState<String?> = mutableStateOf(null)
    val riTuneCastActive: Boolean
        get() = riTuneDevices.value.any { it.selected }

    var chromecastConnected = mutableStateOf(false)

    var androidAutoConnected = mutableStateOf(false)
}


