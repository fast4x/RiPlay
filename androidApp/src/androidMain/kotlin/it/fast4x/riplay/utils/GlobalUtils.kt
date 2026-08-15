package it.fast4x.riplay.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Shape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.cast.ritune.models.RiTuneDevice
import it.fast4x.riplay.ui.styling.LocalAppearance

@Composable
fun getAppSettingsForComposable() = LocalAppSettingsManager.current.activeSettings.collectAsStateWithLifecycle().value

fun getAppSettings() = (appContext() as MainApplication).appSettingsManager.activeSettings.value

@Composable
fun typography() = LocalAppearance.current.typography

@Composable
@ReadOnlyComposable
fun colorPalette() = LocalAppearance.current.colorPalette

@Composable
fun thumbnailShape() = LocalAppearance.current.thumbnailShape

@Composable
fun showSearchIconInNav() = getAppSettingsForComposable().showSearchTab

@Composable
fun showStatsIconInNav() = getAppSettingsForComposable().showStatsInNavbar

@Composable
fun binder() = LocalPlayerServiceBinder.current

fun appContext(): Context = Dependencies.application.applicationContext
fun globalContext(): Context = Dependencies.application

fun getViewType() = getAppSettings().viewType
fun getDnsOverHttpsType() = getAppSettings().dnsOverHttpsType
fun getKeepPlayerMinimized() = getAppSettings().keepPlayerMinimized
fun getlastFmSessionKey() = getAppSettings().lastFMSessionToken

@Composable
fun getRoundnessShape(): Shape {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    return appearanceSettings.thumbnailRoundness.shape()
}
fun ytAccountName() = getAppSettings().ytAccountName
fun isHandleAudioFocusEnabled() = getAppSettings().handleAudioFocusEnabled
fun isBassBoostEnabled() = getAppSettings().bassBoostEnabled
fun isParentalControlEnabled() = getAppSettings().parentalControlEnabled
fun isPersistentQueueEnabled() = getAppSettings().persistentQueue
fun isPipModeAutoEnabled() = getAppSettings().enablePictureInPictureAuto
fun isEnabledFullscreen() = getAppSettings().isEnabledFullScreen
fun isSkipMediaOnErrorEnabled() = getAppSettings().skipMediaOnError
fun isKeepScreenOnEnabled() = getAppSettings().keepScreenEnabled
fun isEnabledLastFm() = getAppSettings().isEnabledLastFM && getlastFmSessionKey().isNotEmpty()


object GlobalSharedData {
    var riTuneDevices: MutableState<List<RiTuneDevice>> = mutableStateOf(emptyList())
    var riTuneConnected = mutableStateOf(false)
    var riTuneError: MutableState<String?> = mutableStateOf(null)
    val riTuneCastActive: Boolean
        get() = riTuneDevices.value.any { it.selected }

    var chromecastConnected = mutableStateOf(false)

    var androidAutoConnected = mutableStateOf(false)
}


