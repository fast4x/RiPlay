package it.fast4x.riplay

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import coil.imageLoader
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.kieronquinn.monetcompat.core.MonetActivityAccessException
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import dev.kdrag0n.monet.theme.ColorScheme
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.playlistPage
import it.fast4x.environment.requests.song
import it.fast4x.environment.utils.LocalePreferenceItem
import it.fast4x.environment.utils.LocalePreferences
import it.fast4x.environment.utils.ProxyPreferenceItem
import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.AnimatedGradient
import it.fast4x.riplay.enums.CheckUpdateState
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.Languages
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.extensions.nsd.discoverNsdServices
import it.fast4x.riplay.extensions.pip.PipModuleContainer
import it.fast4x.riplay.extensions.pip.PipModuleCover
import it.fast4x.riplay.extensions.pip.isInPip
import it.fast4x.riplay.extensions.pip.maybeEnterPip
import it.fast4x.riplay.extensions.pip.maybeExitPip
import it.fast4x.riplay.extensions.preferences.UiTypeKey
import it.fast4x.riplay.extensions.preferences.animatedGradientKey
import it.fast4x.riplay.extensions.preferences.appIsRunningKey
import it.fast4x.riplay.extensions.preferences.applyFontPaddingKey
import it.fast4x.riplay.extensions.preferences.backgroundProgressKey
import it.fast4x.riplay.extensions.preferences.checkUpdateStateKey
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.extensions.preferences.customColorKey
import it.fast4x.riplay.extensions.preferences.customDnsOverHttpsServerKey
import it.fast4x.riplay.extensions.preferences.customThemeDark_Background0Key
import it.fast4x.riplay.extensions.preferences.customThemeDark_Background1Key
import it.fast4x.riplay.extensions.preferences.customThemeDark_Background2Key
import it.fast4x.riplay.extensions.preferences.customThemeDark_Background3Key
import it.fast4x.riplay.extensions.preferences.customThemeDark_Background4Key
import it.fast4x.riplay.extensions.preferences.customThemeDark_TextKey
import it.fast4x.riplay.extensions.preferences.customThemeDark_accentKey
import it.fast4x.riplay.extensions.preferences.customThemeDark_iconButtonPlayerKey
import it.fast4x.riplay.extensions.preferences.customThemeDark_textDisabledKey
import it.fast4x.riplay.extensions.preferences.customThemeDark_textSecondaryKey
import it.fast4x.riplay.extensions.preferences.customThemeLight_Background0Key
import it.fast4x.riplay.extensions.preferences.customThemeLight_Background1Key
import it.fast4x.riplay.extensions.preferences.customThemeLight_Background2Key
import it.fast4x.riplay.extensions.preferences.customThemeLight_Background3Key
import it.fast4x.riplay.extensions.preferences.customThemeLight_Background4Key
import it.fast4x.riplay.extensions.preferences.customThemeLight_TextKey
import it.fast4x.riplay.extensions.preferences.customThemeLight_accentKey
import it.fast4x.riplay.extensions.preferences.customThemeLight_iconButtonPlayerKey
import it.fast4x.riplay.extensions.preferences.customThemeLight_textDisabledKey
import it.fast4x.riplay.extensions.preferences.customThemeLight_textSecondaryKey
import it.fast4x.riplay.extensions.preferences.disableClosingPlayerSwipingDownKey
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.fontTypeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.isEnabledFullscreenKey
import it.fast4x.riplay.extensions.preferences.isKeepScreenOnEnabledKey
import it.fast4x.riplay.extensions.preferences.isProxyEnabledKey
import it.fast4x.riplay.extensions.preferences.languageAppKey
import it.fast4x.riplay.extensions.preferences.loadedDataKey
import it.fast4x.riplay.extensions.preferences.miniPlayerTypeKey
import it.fast4x.riplay.extensions.preferences.navigationBarPositionKey
import it.fast4x.riplay.extensions.preferences.navigationBarTypeKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.pipModuleKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.proxyHostnameKey
import it.fast4x.riplay.extensions.preferences.proxyModeKey
import it.fast4x.riplay.extensions.preferences.proxyPortKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.restartActivityKey
import it.fast4x.riplay.extensions.preferences.shakeEventEnabledKey
import it.fast4x.riplay.extensions.preferences.showSearchTabKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.transitionEffectKey
import it.fast4x.riplay.extensions.preferences.useSystemFontKey
import it.fast4x.riplay.extensions.preferences.ytCookieKey
import it.fast4x.riplay.extensions.preferences.ytDataSyncIdKey
import it.fast4x.riplay.extensions.preferences.ytVisitorDataKey
import it.fast4x.riplay.extensions.rescuecenter.RescueScreen
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.extensions.audiotag.AudioTagViewModel
import it.fast4x.riplay.extensions.preferences.closebackgroundPlayerKey
import it.fast4x.riplay.extensions.preferences.showAutostartPermissionDialogKey
import it.fast4x.riplay.navigation.AppNavigation
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.service.ToolsWorker
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.ui.components.BottomSheet
import it.fast4x.riplay.ui.components.BottomSheetState
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.rememberBottomSheetState
import it.fast4x.riplay.ui.components.themed.CrossfadeContainer
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.player.local.LocalMiniPlayer
import it.fast4x.riplay.ui.screens.player.local.LocalPlayer
import it.fast4x.riplay.ui.screens.player.local.rememberLocalPlayerSheetState
import it.fast4x.riplay.ui.screens.player.online.OnlineMiniPlayer
import it.fast4x.riplay.ui.screens.player.online.OnlinePlayer
import it.fast4x.riplay.ui.screens.player.online.components.core.OnlinePlayerView
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.ui.styling.Appearance
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.styling.applyPitchBlack
import it.fast4x.riplay.ui.styling.colorPaletteOf
import it.fast4x.riplay.ui.styling.customColorPalette
import it.fast4x.riplay.ui.styling.dynamicColorPaletteOf
import it.fast4x.riplay.ui.styling.typographyOf
import it.fast4x.riplay.utils.LocalMonetCompat
import it.fast4x.riplay.utils.OkHttpRequest
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.getDnsOverHttpsType
import it.fast4x.riplay.utils.getKeepPlayerMinimized
import it.fast4x.riplay.utils.getSystemlanguage
import it.fast4x.riplay.utils.invokeOnReady
import it.fast4x.riplay.utils.isAtLeastAndroid13
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.utils.isEnabledFullscreen
import it.fast4x.riplay.utils.isPipModeAutoEnabled
import it.fast4x.riplay.utils.isValidHttpUrl
import it.fast4x.riplay.utils.isValidIP
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.utils.setDefaultPalette
import it.fast4x.riplay.commonutils.thumbnail
import it.fast4x.riplay.extensions.databasebackup.BackupViewModel
import it.fast4x.riplay.extensions.databasebackup.DatabaseBackupManager
import it.fast4x.riplay.extensions.htmlreader.shazamSongInfoExtractor
import it.fast4x.riplay.extensions.ondevice.OnDeviceViewModel
import it.fast4x.riplay.extensions.preferences.resumeOrPausePlaybackWhenDeviceKey
import it.fast4x.riplay.extensions.preferences.showSnowfallEffectKey
import it.fast4x.riplay.extensions.ritune.toRiTuneDevice
import it.fast4x.riplay.service.PlayerServiceQueueViewModel
import it.fast4x.riplay.ui.components.SheetBody
import it.fast4x.riplay.ui.components.Snowfall
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.screens.player.local.RiTunePlayer
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.GlobalSharedData.riTuneDevices
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isAtLeastAndroid12
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.Proxy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt


@UnstableApi
class MainActivity :
    MonetCompatActivity(),
    //AppCompatActivity()
    MonetColorsChangedListener
{
    //lateinit var internetConnectivityObserver: InternetConnectivityObserver

    var client = OkHttpClient()
    var request = OkHttpRequest(client)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) {
                this@MainActivity.binder = service
                //this@MainActivity.onlinePlayer.value = service.onlinePlayer
                this@MainActivity.onlinePlayerPlayingState = service.onlinePlayerPlayingState
//                this@MainActivity.currentDuration.value = service.onlinePlayerCurrentDuration
//                this@MainActivity.currentSecond.value = service.onlinePlayerCurrentSecond
                this@MainActivity.onlinePlayerView = service.onlinePlayerView
            }

//            if (service is ToolsService.LocalBinder) {
//                this@MainActivity.toolsService = service.serviceInstance.LocalBinder()
//            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            //toolsService = null
        }

    }

    private var binder by mutableStateOf<PlayerService.Binder?>(null)
    private var intentUriData by mutableStateOf<Uri?>(null)

    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeCounter = 0

    private var _monet: MonetCompat? by mutableStateOf(null)
    val localMonet get() = _monet ?: throw MonetActivityAccessException()

    private val pipState: MutableState<Boolean> = mutableStateOf(false)

    var cookie: MutableState<String> =
        mutableStateOf("")
    var visitorData: MutableState<String> =
        mutableStateOf("")

    //var riTuneDevices: MutableState<List<NsdServiceInfo>> = mutableStateOf(emptyList())

    var onlinePlayerPlayingState by mutableStateOf(false)
    var localPlayerPlayingState: MutableState<Boolean> = mutableStateOf(false)

    var selectedQueue: MutableState<Queues> = mutableStateOf(defaultQueue())

    //var toolsService by mutableStateOf<ToolsService.LocalBinder?>(null)

    private var onlinePlayerView: YouTubePlayerView? = null

    private var isclosebackgroundPlayerEnabled = false

    private var showAutostartPermissionDialog = false


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Timber.d("MainActivity all permissions are granted.")
            // After standard permissions check autostart permission
            checkAndRequestAutostartPermission()
        } else {
            Timber.w("MainActivity Some permissions are not granted.")
            permissions.entries.forEach { (permission, isGranted) ->
                if (!isGranted) {
                    Timber.w("MainActivity Permission Not GRANTED: $permission")
                }
            }
        }
    }

    private val audioTaggerViewModel: AudioTagViewModel by viewModels {
        AudioTagViewModel()
    }

    private val backupManagerViewModel: BackupViewModel by viewModels {
        BackupViewModel(DatabaseBackupManager(this, Database), this)
    }

    private val playerServiceQueue: PlayerServiceQueueViewModel by viewModels {
        PlayerServiceQueueViewModel()
    }

    private val onDeviceViewModel: OnDeviceViewModel by viewModels {
        OnDeviceViewModel(application)
    }

    private fun checkAndRequestStandardPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (isAtLeastAndroid13) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            //In the future for local video
            //permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (isAtLeastAndroid12 && preferences.getBoolean(resumeOrPausePlaybackWhenDeviceKey, false))
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNotGranted.isNotEmpty()) {
            permissionLauncher.launch(permissionsNotGranted.toTypedArray())
        } else {
            Timber.d("MainActivity Standard permissions already granted.")
            if (showAutostartPermissionDialog)
                checkAndRequestAutostartPermission()
        }
    }

    private fun checkAndRequestAutostartPermission() {
        val manufacturer = Build.MANUFACTURER.lowercase()

        // List of vendors with known restrictions on autostart
        val manufacturersWithAutostart = setOf("xiaomi", "huawei", "oppo", "vivo", "oneplus", "samsung", "asus")

        if (manufacturer in manufacturersWithAutostart) {
            Timber.d("MainActivity Found vendor with autostart restrictions: $manufacturer")
            showAutostartDialog()
        } else {
            Timber.d("MainActivity Vendor known already granted.")
        }
    }

    private fun showAutostartDialog() {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.enable_autostart))
            .setMessage(getString(R.string.to_ensure_that_the_app_is_working_properly_in_the_background_e_g_for_notifications_or_music_playback_you_must_enable_autostart_do_you_want_to_open_the_settings_to_activate_it))
            .setPositiveButton(getString(R.string.open_settings_now)) { _, _ ->
                openAutostartSettings()
            }
            .setNegativeButton(getString(R.string.later), null)
            .setCancelable(false)
            .show()

        // Hide autostart permission dialog after showing the dialog
        preferences.edit(commit = true) {
            putBoolean(showAutostartPermissionDialogKey, false)
        }
    }

    private fun openAutostartSettings() {
        try {
            val intent = when (Build.MANUFACTURER.lowercase()) {
                "xiaomi" -> Intent().apply {
                    component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                "huawei" -> Intent().apply {
                    component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
                }
                "oppo" -> Intent().apply {
                    component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
                "vivo" -> Intent().apply {
                    component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                }
                "oneplus" -> Intent().apply {
                    component = ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
                }
                "samsung" -> { // Samsung is more complicated, often going into battery settings
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }
                else -> {
                    // Generic fallback, open app settings
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }
            }
            startActivity(intent)
            SmartMessage( "Search for 'Autostart' or 'Allow Startup' and activate it for this app.", context = this)
        } catch (e: Exception) {
            Timber.e("MainActivity Unable to open autostart settings. $e")
            // Ultimate fallback: O, open app settings
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(fallbackIntent)
            SmartMessage( "Open the app settings and look for battery or autostart options.", context= this)
        }
    }

    override fun onStart() {
        runCatching {
            val intent = Intent(this, PlayerService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            if (isAtLeastAndroid8)
                startForegroundService(intent)
            else
                startService(intent)

        }.onFailure {
            Timber.e("MainActivity.onStart bindService ${it.stackTraceToString()}")
        }

        /*
        runCatching {
            val intent = Intent(this, ToolsService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)
        }.onFailure {
            Timber.e("MainActivity.onStart startService ToolsService ${it.stackTraceToString()}")
        }
         */

        super.onStart()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @ExperimentalMaterialApi
    @ExperimentalTextApi
    @UnstableApi
    @ExperimentalComposeUiApi
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(
//                StrictMode.ThreadPolicy.Builder()
//                    .detectAll()
//                    .build()
//            )
//            StrictMode.setVmPolicy(
//                StrictMode.VmPolicy.Builder()
//                    .detectAll()
//                    .build()
//            )
//        }

        MonetCompat.enablePaletteCompat()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = Color.Transparent.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            )
        )

        enableFullscreenMode()

        MonetCompat.setup(this)
        _monet = MonetCompat.getInstance()
        localMonet.setDefaultPalette()

        localMonet.addMonetColorsChangedListener(
            listener = this,
            notifySelf = false
        )
        localMonet.updateMonetColors()

        Timber.d("MainActivity.onCreate Before localMonet.invokeOnReady")

        localMonet.invokeOnReady {
            Timber.d("MainActivity.onCreate Inside localMonet.invokeOnReady")

            startApp()
        }

        if (preferences.getBoolean(shakeEventEnabledKey, false)) {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            Objects.requireNonNull(sensorManager)
                ?.registerListener(
                    sensorListener,
                    sensorManager!!
                        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
        }

        checkIfAppIsRunningInBackground()

        //registerNsdService()
        discoverNsdServices(
            onServiceFound = {
                //riTuneDevices.value = it
                riTuneDevices = it.map { it.toRiTuneDevice() }.toMutableStateList()
            }
        )

        isclosebackgroundPlayerEnabled = preferences.getBoolean(closebackgroundPlayerKey, false)

        showAutostartPermissionDialog = preferences.getBoolean(showAutostartPermissionDialogKey, true)

        //initializeWorker()

        checkAndRequestStandardPermissions()

    }


    private fun enableFullscreenMode() {

        // Prepare the Activity to go in immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Old method to hide status bar
        // requestWindowFeature(Window.FEATURE_NO_TITLE)
        // this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // New method to hide system bars
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        if (isEnabledFullscreen()) {
            // Configure the behavior of the hidden system bars.
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
//          windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
//          windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        //Other method
//        if (Build.VERSION.SDK_INT < 16) {
//            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        }
//        if (Build.VERSION.SDK_INT > 15) {
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//            actionBar?.hide()
//        }

    }

    private fun checkIfAppIsRunningInBackground() {
        val runningAppProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(runningAppProcessInfo)
        appRunningInBackground =
            runningAppProcessInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        pipState.value = isInPictureInPictureMode

        // todo improve pip
//        if (isAtLeastAndroid8 && isInPictureInPictureMode)
//            setPictureInPictureParams(
//                PictureInPictureParams.Builder()
//                    .setActions(mutableListOf<RemoteAction?>(null))
//                    //.setAutoEnterEnabled(true)
//                    .build()
//            )

        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.d("MainActivity.onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_UI_HIDDEN")
        }
        if (level == TRIM_MEMORY_RUNNING_LOW) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_LOW")
        }
        if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_CRITICAL")
        }
        if (level == TRIM_MEMORY_BACKGROUND) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_BACKGROUND")
        }
        if (level == TRIM_MEMORY_COMPLETE) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_COMPLETE")
        }
        if (level == TRIM_MEMORY_MODERATE) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_MODERATE")
        }
        if (level == TRIM_MEMORY_RUNNING_MODERATE) {
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_MODERATE")
        }
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (
            isPipModeAutoEnabled() && binder?.player?.isPlaying == true
        ) maybeEnterPip()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //if (newConfig.orientation in intArrayOf(Configuration.ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_PORTRAIT))
        Timber.d("MainActivity.onConfigurationChanged newConfig.orientation ${newConfig.orientation}")
    }


    @Composable
    fun ThemeApp(
        isDark: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                (view.context as Activity).window.let { window ->
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        !isDark
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                        !isDark
                }
            }

        }
        content()
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @OptIn(
        ExperimentalTextApi::class,
        ExperimentalFoundationApi::class, ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class, FlowPreview::class
    )
    @ExperimentalPermissionsApi
    fun startApp() {

        // Used in QuickPics for load data from remote instead of last saved in SharedPreferences
        preferences.edit(commit = true) { putBoolean(loadedDataKey, false) }

        // Used for android auto to show notification to invite user launch app
        preferences.edit(commit = true) { putBoolean(appIsRunningKey, true) }

//        if (!preferences.getBoolean(closeWithBackButtonKey, false))
//            if (Build.VERSION.SDK_INT >= 33) {
//                onBackInvokedDispatcher.registerOnBackInvokedCallback(
//                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
//                ) {
//                    //Log.d("onBackPress", "yeah")
//                }
//            }

        val launchedFromNotification: Boolean =
            intent?.extras?.let {
                it.getBoolean("expandPlayerBottomSheet") || it.getBoolean("fromWidget")
            } ?: false

        Timber.d("MainActivity.onCreate launchedFromNotification: $launchedFromNotification intent $intent.action")

        intentUriData = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()

        with(preferences) {
            if (getBoolean(isKeepScreenOnEnabledKey, false)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            if (getBoolean(isProxyEnabledKey, false)) {
                val hostName = getString(proxyHostnameKey, null)
                val proxyPort = getInt(proxyPortKey, 8080)
                val proxyMode = getEnum(proxyModeKey, Proxy.Type.HTTP)
                if (isValidIP(hostName)) {
                    hostName?.let { hName ->
                        ProxyPreferences.preference =
                            ProxyPreferenceItem(hName, proxyPort, proxyMode)
                    }
                } else {
                    SmartMessage(
                        "Your Proxy Hostname is invalid, please check it",
                        PopupType.Warning,
                        context = this@MainActivity
                    )
                }
            }

        }

        setContent {

            val backupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                backupManagerViewModel.performBackup(uri)
            }

            val restoreLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                backupManagerViewModel.performRestore(uri)
            }

//            try {
//                internetConnectivityObserver.unregister()
//            } catch (e: Exception) {
//                // isn't registered, can be registered without issue
//            }
//            internetConnectivityObserver = InternetConnectivityObserver(this@MainActivity)
//            val isInternetAvailable by internetConnectivityObserver.internetNetworkStatus.collectAsState(true)

            // Observe preference so theme mode updates immediately when changed from Settings
            val colorPaletteMode by rememberObservedPreference(
                colorPaletteModeKey,
                ColorPaletteMode.Dark
            )
            val isPicthBlack = colorPaletteMode == ColorPaletteMode.PitchBlack

            if (preferences.getEnum(
                    checkUpdateStateKey,
                    CheckUpdateState.Ask
                ) == CheckUpdateState.Enabled
            ) {
                val urlVersionCode =
                    "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"
                    //"https://raw.githubusercontent.com/fast4x/CentralUpdates/main/updates/VersionCode-Ri.ver"
                request.GET(urlVersionCode, object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val responseData = response.body?.string()
                        runOnUiThread {
                            try {
                                if (responseData != null) {
                                    val file = File(filesDir, "UpdatedVersionCode.ver")
                                    file.writeText(responseData.toString())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                    }

                    override fun onFailure(call: Call, e: IOException) {
                        Log.d("UpdatedVersionCode", "Check failure")
                    }
                })
            }


            val coroutineScope = rememberCoroutineScope()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val navController = rememberNavController()
            var animatedGradient by rememberPreference(
                animatedGradientKey,
                AnimatedGradient.Linear
            )
            var customColor by rememberPreference(customColorKey, Color.Green.hashCode())
            val lightTheme =
                colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))

            val locale = Locale.getDefault()
            val languageTag = locale.toLanguageTag().replace("-Hant", "")
            val languageApp =
                globalContext().preferences.getEnum(languageAppKey, getSystemlanguage())
            LocalePreferences.preference =
                LocalePreferenceItem(
                    hl = languageApp.code.takeIf { it != Languages.System.code }
                        ?: locale.language.takeIf { it != Languages.System.code }
                        ?: languageTag.takeIf { it != Languages.System.code }
                        ?: "en",
                    gl = locale.country
                        ?: "US"
                )

            cookie.value = preferences.getString(ytCookieKey, "").toString()
            visitorData.value = preferences.getString(ytVisitorDataKey, "").toString()



            // If visitorData is empty, get it from the server with or without login
            if (visitorData.value.isEmpty() || visitorData.value == "null" || visitorData.value == "")
                runCatching {
                    Timber.d("MainActivity.setContent visitorData.isEmpty() getInitialVisitorData visitorData ${visitorData.value}")
                    visitorData.value = runBlocking {
                        Environment.getInitialVisitorData().getOrNull()
                    }.takeIf { it != "null" } ?: ""
                    // Save visitorData in SharedPreferences
                    preferences.edit { putString(ytVisitorDataKey, visitorData.value) }
                }.onFailure {
                    Timber.e("MainActivity.setContent visitorData.isEmpty() getInitialVisitorData ${it.stackTraceToString()}")
                    visitorData.value = "" //Environment._uMYwa66ycM
                }

            Environment.visitorData = visitorData.value
            Timber.d("MainActivity.setContent visitorData in use: ${visitorData.value}")

            cookie.let {
                if (isYtLoggedIn())
                    Environment.cookie = it.value
                else {
                    Environment.cookie = ""
                    cookie.value = ""
                    preferences.edit { putString(ytCookieKey, "") }
                }
            }

            Environment.dataSyncId = preferences.getString(ytDataSyncIdKey, "").toString()

            Timber.d("MainActivity.setContent cookie: ${cookie.value}")
            val customDnsOverHttpsServer =
                preferences.getString(customDnsOverHttpsServerKey, "")

            val customDnsIsOk = customDnsOverHttpsServer?.let { isValidHttpUrl(it) }
            if (customDnsIsOk == false && getDnsOverHttpsType() == DnsOverHttpsType.Custom)
                SmartMessage(
                    stringResource(R.string.custom_dns_is_invalid),
                    PopupType.Error,
                    context = this@MainActivity
                )

            val customDnsUrl = if (customDnsIsOk == true) customDnsOverHttpsServer else null
            Environment.customDnsToUse = customDnsUrl
            Environment.dnsToUse = getDnsOverHttpsType().type

            // Recreate appearance whenever theme mode or light/dark flag changes
            var appearance by rememberSaveable(
                colorPaletteMode,
                !lightTheme,
                stateSaver = Appearance
            ) {
                with(preferences) {
                    val colorPaletteName =
                        getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    //val colorPaletteMode = getEnum(colorPaletteModeKey, ColorPaletteMode.Dark)
                    val thumbnailRoundness =
                        getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)
                    val useSystemFont = getBoolean(useSystemFontKey, false)
                    val applyFontPadding = getBoolean(applyFontPaddingKey, false)

                    var colorPalette =
                        colorPaletteOf(colorPaletteName, colorPaletteMode, !lightTheme)

                    val fontType = getEnum(fontTypeKey, FontType.Rubik)

                    //TODO CHECK MATERIALYOU OR MONIT
                    if (colorPaletteName == ColorPaletteName.MaterialYou) {
                        colorPalette = dynamicColorPaletteOf(
                            Color(localMonet.getAccentColor(this@MainActivity)),
                            !lightTheme
                        )
                    }
                    if (colorPaletteName == ColorPaletteName.CustomColor) {
                        Timber.d("MainActivity.startApp SetContent with(preferences) customColor PRE colorPalette: $colorPalette")
                        colorPalette = dynamicColorPaletteOf(
                            Color(customColor),
                            !lightTheme
                        )
                        Timber.d("MainActivity.startApp SetContent with(preferences) customColor POST colorPalette: $colorPalette")
                    }

                    setSystemBarAppearance(colorPalette.isDark)

                    mutableStateOf(
                        Appearance(
                            colorPalette = colorPalette,
                            typography = typographyOf(
                                colorPalette.text,
                                useSystemFont,
                                applyFontPadding,
                                fontType
                            ),
                            thumbnailShape = thumbnailRoundness.shape()
                        )
                    )
                }


            }

            fun setDynamicPalette(url: String) {
                val playerBackgroundColors = preferences.getEnum(
                    playerBackgroundColorsKey,
                    PlayerBackgroundColors.BlurredCoverColor
                )
                val colorPaletteName =
                    preferences.getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                val isDynamicPalette = colorPaletteName == ColorPaletteName.Dynamic
                val isCoverColor =
                    playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient ||
                            playerBackgroundColors == PlayerBackgroundColors.CoverColor ||
                            animatedGradient == AnimatedGradient.FluidCoverColorGradient

                if (!isDynamicPalette) return


                coroutineScope.launch(Dispatchers.IO) {
                    val result = imageLoader.execute(
                        ImageRequest.Builder(this@MainActivity)
                            .data(url)
                            // Required to get work getPixels
                            //.bitmapConfig(if (isAtLeastAndroid8) Bitmap.Config.RGBA_F16 else Bitmap.Config.ARGB_8888)
                            .bitmapConfig(Bitmap.Config.ARGB_8888)
                            .allowHardware(false)
                            .build()
                    )
                    val isPicthBlack = colorPaletteMode == ColorPaletteMode.PitchBlack
                    val isDark =
                        colorPaletteMode == ColorPaletteMode.Dark || isPicthBlack || (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme)

                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette
                            .from(bitmap)
                            .maximumColorCount(8)
                            .addFilter(if (isDark) ({ _, hsl -> hsl[0] !in 36f..100f }) else null)
                            .generate()

                        dynamicColorPaletteOf(bitmap, isDark)?.let {
                            withContext(Dispatchers.Main) {
                                setSystemBarAppearance(it.isDark)
                            }
                            appearance = appearance.copy(
                                colorPalette = if (!isPicthBlack) it else it.copy(
                                    background0 = Color.Black,
                                    background1 = Color.Black,
                                    background2 = Color.Black,
                                    background3 = Color.Black,
                                    background4 = Color.Black,
                                    // text = Color.White
                                ),
                                typography = appearance.typography.copy(it.text)
                            )
                        }

                    }
                }
            }


            // React to theme mode changes without requiring app restart (include palette mode key)
            DisposableEffect(binder, colorPaletteMode, !lightTheme) {

                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {

                            languageAppKey -> {
                                val lang = sharedPreferences.getEnum(
                                    languageAppKey,
                                    Languages.English
                                )


                                val systemLangCode =
                                    AppCompatDelegate.getApplicationLocales().get(0).toString()

                                val sysLocale: LocaleListCompat =
                                    LocaleListCompat.forLanguageTags(systemLangCode)
                                val appLocale: LocaleListCompat =
                                    LocaleListCompat.forLanguageTags(lang.code)
                                AppCompatDelegate.setApplicationLocales(if (lang.code == "") sysLocale else appLocale)
                            }

                            // todo improve enum in live state
                            UiTypeKey,
                            disablePlayerHorizontalSwipeKey,
                            disableClosingPlayerSwipingDownKey,
                            showSearchTabKey,
                            navigationBarPositionKey,
                            navigationBarTypeKey,
                            showTotalTimeQueueKey,
                            backgroundProgressKey,
                            transitionEffectKey,
                            playerBackgroundColorsKey,
                            miniPlayerTypeKey,
                            restartActivityKey
                                -> {
                                this@MainActivity.recreate()
                                Timber.d("MainActivity.recreate()")
                            }

                            colorPaletteNameKey, colorPaletteModeKey,
                            customThemeLight_Background0Key,
                            customThemeLight_Background1Key,
                            customThemeLight_Background2Key,
                            customThemeLight_Background3Key,
                            customThemeLight_Background4Key,
                            customThemeLight_TextKey,
                            customThemeLight_textSecondaryKey,
                            customThemeLight_textDisabledKey,
                            customThemeLight_iconButtonPlayerKey,
                            customThemeLight_accentKey,
                            customThemeDark_Background0Key,
                            customThemeDark_Background1Key,
                            customThemeDark_Background2Key,
                            customThemeDark_Background3Key,
                            customThemeDark_Background4Key,
                            customThemeDark_TextKey,
                            customThemeDark_textSecondaryKey,
                            customThemeDark_textDisabledKey,
                            customThemeDark_iconButtonPlayerKey,
                            customThemeDark_accentKey,
                            customColorKey
                                -> {
                                val colorPaletteName =
                                    sharedPreferences.getEnum(
                                        colorPaletteNameKey,
                                        ColorPaletteName.Dynamic
                                    )

                                val newColorPaletteMode = sharedPreferences.getEnum(
                                    colorPaletteModeKey,
                                    ColorPaletteMode.Dark
                                )
                                val isNewPitchBlack = newColorPaletteMode == ColorPaletteMode.PitchBlack
                                val isNewDark =
                                    newColorPaletteMode == ColorPaletteMode.Dark || isNewPitchBlack || (newColorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme)
                                val newLightTheme = !isNewDark

                                var colorPalette = colorPaletteOf(
                                    colorPaletteName,
                                    newColorPaletteMode,
                                    newLightTheme.not()
                                )

                                if (colorPaletteName == ColorPaletteName.Dynamic) {
                                    val artworkUri =
                                        (binder?.player?.currentMediaItem?.mediaMetadata?.artworkUri.toString().thumbnail(
                                            1200
                                        )
                                            ?: "").toString()
                                    artworkUri.let {
                                        if (it.isNotEmpty())
                                            setDynamicPalette(it)
                                        else {

                                            setSystemBarAppearance(colorPalette.isDark)
                                            appearance = appearance.copy(
                                                colorPalette = if (!isNewPitchBlack) colorPalette else colorPalette.copy(
                                                    background0 = Color.Black,
                                                    background1 = Color.Black,
                                                    background2 = Color.Black,
                                                    background3 = Color.Black,
                                                    background4 = Color.Black,
                                                    // text = Color.White
                                                ),
                                                typography = appearance.typography.copy(
                                                    colorPalette.text
                                                ),
                                            )
                                        }

                                    }

                                } else {

                                    if (colorPaletteName == ColorPaletteName.MaterialYou) {
                                        colorPalette = dynamicColorPaletteOf(
                                            Color(localMonet.getAccentColor(this@MainActivity)),
                                            newLightTheme.not()
                                        )
                                    }

                                    if (colorPaletteName == ColorPaletteName.Customized) {
                                        colorPalette = customColorPalette(
                                            colorPalette,
                                            this@MainActivity,
                                            isSystemInDarkTheme
                                        )
                                    }
                                    if (colorPaletteName == ColorPaletteName.CustomColor) {
                                        Timber.d("MainActivity.startApp SetContent DisposableEffect customColor PRE colorPalette: $colorPalette")
                                        colorPalette = dynamicColorPaletteOf(
                                            Color(customColor),
                                            newLightTheme.not()
                                        )
                                        Timber.d("MainActivity.startApp SetContent DisposableEffect customColor POST colorPalette: $colorPalette")
                                    }

                                    setSystemBarAppearance(colorPalette.isDark)

                                    appearance = appearance.copy(
                                        colorPalette = if (!isNewPitchBlack) colorPalette else colorPalette.copy(
                                            background0 = Color.Black,
                                            background1 = Color.Black,
                                            background2 = Color.Black,
                                            background3 = Color.Black,
                                            background4 = Color.Black,
                                            text = Color.White
                                        ),
                                        typography = appearance.typography.copy(if (!isNewPitchBlack) colorPalette.text else Color.White),
                                    )
                                }
                            }

                            thumbnailRoundnessKey -> {
                                val thumbnailRoundness =
                                    sharedPreferences.getEnum(key, ThumbnailRoundness.Heavy)

                                appearance = appearance.copy(
                                    thumbnailShape = thumbnailRoundness.shape()
                                )
                            }

                            useSystemFontKey, applyFontPaddingKey, fontTypeKey -> {
                                val useSystemFont =
                                    sharedPreferences.getBoolean(useSystemFontKey, false)
                                val applyFontPadding =
                                    sharedPreferences.getBoolean(applyFontPaddingKey, false)
                                val fontType =
                                    sharedPreferences.getEnum(fontTypeKey, FontType.Rubik)

                                appearance = appearance.copy(
                                    typography = typographyOf(
                                        appearance.colorPalette.text,
                                        useSystemFont,
                                        applyFontPadding,
                                        fontType
                                    ),
                                )
                            }

                            ytCookieKey -> cookie.value =
                                sharedPreferences.getString(ytCookieKey, "").toString()

                            ytVisitorDataKey -> {
                                if (visitorData.value.isEmpty())
                                    visitorData.value =
                                        sharedPreferences.getString(ytVisitorDataKey, "").toString()
                            }
                            isEnabledFullscreenKey -> enableFullscreenMode()
                            closebackgroundPlayerKey -> isclosebackgroundPlayerEnabled =
                                sharedPreferences.getBoolean(
                                    closebackgroundPlayerKey,
                                    false
                                )

                        }
                    }

                with(preferences) {
                    registerOnSharedPreferenceChangeListener(listener)

                    val colorPaletteName =
                        getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    if (colorPaletteName == ColorPaletteName.Dynamic) {
                        setDynamicPalette(
                            (binder?.player?.currentMediaItem?.mediaMetadata?.artworkUri.toString().thumbnail(
                                1200
                            )
                                ?: "").toString()
                        )
                    }

                    onDispose {
                        unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }
            }

            val rippleConfiguration =
                remember(appearance.colorPalette.text, appearance.colorPalette.isDark) {
                    RippleConfiguration(color = appearance.colorPalette.text)
                }

            val shimmerTheme = remember {
                defaultShimmerTheme.copy(
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            easing = LinearEasing,
                            delayMillis = 250,
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    shaderColors = listOf(
                        Color.Unspecified.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.50f),
                        Color.Unspecified.copy(alpha = 0.25f),
                    ),
                )
            }

            LaunchedEffect(Unit) {
                val colorPaletteName =
                    preferences.getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                if (colorPaletteName == ColorPaletteName.Customized) {
                    appearance = appearance.copy(
                        colorPalette = customColorPalette(
                            appearance.colorPalette,
                            this@MainActivity,
                            isSystemInDarkTheme
                        )
                    )
                }
            }


            if (colorPaletteMode == ColorPaletteMode.PitchBlack)
                appearance = appearance.copy(
                    colorPalette = appearance.colorPalette.applyPitchBlack,
                    typography = appearance.typography.copy(appearance.colorPalette.text)
                )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appearance.colorPalette.background0)
            ) {


                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars
                val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }

                val localPlayerSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = 5.dp, //Dimensions.collapsedPlayer,
                    expandedBound = maxHeight
                )

                // TODO remove in the future
                val playerSheetState = rememberLocalPlayerSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = Dimensions.collapsedPlayer + bottomDp,
                    expandedBound = maxHeight,
                )


                val playerAwareWindowInsets by remember(
                    bottomDp,
                    playerSheetState.value
                ) {
                    derivedStateOf {
                        val bottom = playerSheetState.value.coerceIn(
                            bottomDp,
                            playerSheetState.collapsedBound
                        )

                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(bottom = bottom))
                    }
                }


                var openTabFromShortcut = remember { -1 }
                if (intent.action in arrayOf(
                        action_songs,
                        action_albums,
                        action_library,
                        action_search,
                        action_musicidentifier
                    )
                ) {
                    openTabFromShortcut =
                        when (intent?.action) {
                            action_songs -> HomeScreenTabs.Songs.index
                            action_albums -> HomeScreenTabs.Albums.index
                            action_library -> HomeScreenTabs.Playlists.index
                            action_search -> -2
                            action_musicidentifier -> -3
                            else -> -1
                        }
                    intent.action = null
                }

                onlinePlayerPlayingState = binder?.onlinePlayerPlayingState == true
                onlinePlayerView = binder?.onlinePlayerView

                val pip = isInPip(
                    onChange = {
                        if (!it || (binder?.player?.isPlaying != true && !onlinePlayerPlayingState))
                            return@isInPip

                        localPlayerSheetState.expandSoft()
                    }
                )

                CrossfadeContainer(
                    state = pip
                ) { isCurrentInPip ->
                    //Timber.d("MainActivity pipState ${pipState.value} CrossfadeContainer isCurrentInPip $isCurrentInPip ")
                    val pipModule by rememberPreference(pipModuleKey, PipModule.Cover)
                    if (isCurrentInPip) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                        ) {
                            when (pipModule) {
                                PipModule.Cover -> {
                                    PipModuleContainer {
                                        // Implement pip mode with video
                                        //if (mediaItemIsLocal.value) {
                                            PipModuleCover(
                                                url = binder?.player?.currentMediaItem?.mediaMetadata?.artworkUri.toString()
                                                    .resize(1200, 1200)
                                            )
//                                        } else {
//                                            PipModuleCore(
//                                                onlineCore = onlineCore
//                                            )
//                                        }
                                    }
                                }
                            }

                        }

                    } else
                        CompositionLocalProvider(
                            LocalAppearance provides appearance,
                            LocalIndication provides ripple(bounded = true),
                            LocalRippleConfiguration provides rippleConfiguration,
                            LocalShimmerTheme provides shimmerTheme,
                            LocalPlayerServiceBinder provides binder,
                            LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                            LocalLayoutDirection provides LayoutDirection.Ltr,
                            LocalPlayerSheetState provides localPlayerSheetState,
                            LocalMonetCompat provides localMonet,
                            //LocalRiTuneDevices provides riTuneDevices.value,
                            LocalOnlinePlayerPlayingState provides onlinePlayerPlayingState,
                            LocalSelectedQueue provides selectedQueue.value,
                            LocalAudioTagger provides audioTaggerViewModel,
                            LocalBackupManager provides backupManagerViewModel,
                            LocalPlayerServiceQueue provides playerServiceQueue,
                            LocalOnDeviceViewModel provides onDeviceViewModel
                            //LocalInternetAvailable provides isInternetAvailable
                        ) {

                            if (intent.action == action_rescuecenter) {
                                RescueScreen(
                                    onBackup = {
                                        @SuppressLint("SimpleDateFormat")
                                        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                                        backupLauncher.launch("riplay_${dateFormat.format(Date())}.db")
                                    },
                                    onRestore = {
                                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                                    }
                                )
                            } else {
                                AppNavigation(
                                    navController = navController,
                                    miniPlayer = {

                                        if (binder?.currentMediaItemAsSong?.isLocal == true)
                                            LocalMiniPlayer(
                                                showPlayer = { localPlayerSheetState.expandSoft() },
                                                hidePlayer = { localPlayerSheetState.collapseSoft() },
                                                navController = navController,
                                            )
                                        else {
                                            OnlineMiniPlayer(
                                                showPlayer = { localPlayerSheetState.expandSoft() },
                                                hidePlayer = { localPlayerSheetState.collapseSoft() },
                                                navController = navController,
                                                //player = onlinePlayer,
                                                //playerState = onlinePlayerState,
                                                //currentDuration = currentDuration.value,
                                                //currentSecond = currentSecond.value,
                                            )
                                        }
                                    },
                                    //player = onlinePlayer,
                                    //playerState = onlinePlayerState,
                                    openTabFromShortcut = openTabFromShortcut
                                )

                                val isSnowEffectEnabled by rememberObservedPreference(showSnowfallEffectKey, false)
                                if (isSnowEffectEnabled)
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Snowfall()
                                    }

                                checkIfAppIsRunningInBackground()
                                if (appRunningInBackground) localPlayerSheetState.collapseSoft()

                                val thumbnailRoundness by rememberPreference(
                                    thumbnailRoundnessKey,
                                    ThumbnailRoundness.Heavy
                                )

                                val localPlayer: @Composable () -> Unit = {
                                    LocalPlayer(
                                        navController = navController,
                                        onDismiss = {
                                            localPlayerSheetState.collapseSoft()
                                        }
                                    )
                                }




                                val onlinePlayer: @Composable () -> Unit = {
                                    OnlinePlayer(
                                        navController = navController,
                                        //playFromSecond = currentSecond.value,
                                        onlineCore = {
                                            binder?.player?.currentMediaItem?.let{
                                                OnlinePlayerView(
                                                    onlinePlayerView = onlinePlayerView,
                                                    mediaItem = it,
                                                )
                                            }
                                        },
                                        playerSheetState = localPlayerSheetState,
                                        onDismiss = {
                                            localPlayerSheetState.collapseSoft()
                                        },
                                    )
                                }



                                BottomSheet(
                                    state = localPlayerSheetState,
                                    collapsedContent = {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            //Text(text = "BottomSheet", modifier = Modifier.align(Alignment.Center))
                                        }
                                    },
                                    contentAlwaysAvailable = true
                                ) {
                                    //if (!castToRiTuneDeviceActive) {
                                        if (binder?.currentMediaItemAsSong?.isLocal == true)
                                            localPlayer()
                                        else
                                            onlinePlayer()
//                                    } else
//                                        RiTunePlayer(
//                                            navController = navController,
//                                            onDismiss = {}
//                                        )
                                }

                                val menuState = LocalGlobalSheetState.current
                                CustomModalBottomSheet(
                                    showSheet = menuState.isDisplayed,
                                    onDismissRequest = menuState::hide,
                                    containerColor = Color.Transparent,
                                    sheetState = rememberModalBottomSheetState(
                                        skipPartiallyExpanded = true
                                    ),
                                    dragHandle = {
                                        Surface(
                                            modifier = Modifier.padding(vertical = 0.dp),
                                            color = Color.Transparent,
                                            //shape = thumbnailShape
                                        ) {}
                                    },
                                    shape = thumbnailRoundness.shape()
                                ) {
                                    menuState.content()
                                }

                                /*
                                if (showSelectorRiTuneDevices) {
                                    menuState.display {
                                        SheetBody {
                                            Box(
                                                modifier = Modifier
                                                    .background(colorPalette().background0)
                                                    .fillMaxSize()
                                            ) {

                                                LazyColumn(
                                                    state = rememberLazyListState(),
                                                    contentPadding = PaddingValues(all = 10.dp),
                                                    modifier = Modifier
                                                        .background(
                                                            colorPalette().background0
                                                        )
                                                        .height(400.dp)
                                                ) {
                                                    item {
                                                        Text(
                                                            text = "Available RiTune Devices",
                                                            color = colorPalette().text,
                                                            modifier = Modifier.padding(bottom = 10.dp)
                                                        )


                                                    }
                                                    items(
                                                        items = riTuneDevices.distinctBy { it.host },
                                                        //key = { it.host }
                                                    ) { device ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(36.dp)
                                                                .clickable {
                                                                    var dev = riTuneDevices[ riTuneDevices.indexOf(device) ]
                                                                    dev.selected = !dev.selected
                                                                    menuState.hide()
                                                                    showSelectorRiTuneDevices =
                                                                        false
                                                                },
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(
                                                                icon = if (device.selected) R.drawable.cast_connected else R.drawable.cast_disconnected,
                                                                color = colorPalette().text,
                                                                enabled = true,
                                                                onClick = {},
                                                                modifier = Modifier
                                                                    .size(32.dp),
                                                            )
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Text(
                                                                text = device.name,
                                                                color = colorPalette().text,
                                                                modifier = Modifier.border(BorderStroke(1.dp, Color.Red))
                                                            )
                                                        }
                                                    }
                                                }

                                                LinearProgressIndicator(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .align(Alignment.BottomCenter),
                                                )

                                            }
                                        }
                                    }
                                }

                                 */

                            }
                        }

                }
                DisposableEffect(binder?.player) {
                    val player = binder?.player ?: return@DisposableEffect onDispose { }

                    //Timber.d("MainActivity DisposableEffecty mediaItemAsSong ${binder!!.currentMediaItemAsSong}")

                    if (player.currentMediaItem == null) {
                        if (localPlayerSheetState.isExpanded) {
                            localPlayerSheetState.collapseSoft()
                        }
                    } else {
                        if (launchedFromNotification) {
                            intent.replaceExtras(Bundle())
                            if (getKeepPlayerMinimized())
                                localPlayerSheetState.collapseSoft()
                            else localPlayerSheetState.expandSoft()
                        } else {
                            localPlayerSheetState.collapseSoft()
                        }

                    }

                    val listener = object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Timber.d("MainActivity Player.Listener onIsPlayingChanged isPlaying $isPlaying")
                            localPlayerPlayingState.value = isPlaying
                        }
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            Timber.d("MainActivity Player.Listener onMediaItemTransition mediaItem ${mediaItem?.mediaId} reason $reason foreground $appRunningInBackground")

                            if (mediaItem == null) {
                                maybeExitPip()
                                localPlayerSheetState.collapseSoft()
                                return
                            }

                            mediaItem.let {
                                //currentSecond.value = 0F

                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                                    if (it.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                        if (getKeepPlayerMinimized())
                                            localPlayerSheetState.collapseSoft()
                                        else localPlayerSheetState.expandSoft()
                                    } else {
                                        localPlayerSheetState.collapseSoft()
                                    }
                                }

                                setDynamicPalette(
                                    it.mediaMetadata.artworkUri.toString().thumbnail(
                                        1200
                                    ).toString()
                                )

                            }

                        }

                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }


            }

            LaunchedEffect(intentUriData) {
                var uri = intentUriData ?: return@LaunchedEffect
                if (uri.host == "www.shazam.com") {
                    uri = "${"https://"}${
                        uri.toString().substringAfter("https://").substringBeforeLast("\"")
                    }".toUri()
                }

                Timber.d("MainActivity LaunchedEffect intentUriData $uri path ${uri.pathSegments.firstOrNull()} host ${uri.host}")
                Timber.d("MainActivity LaunchedEffect intentUriData scheme ${"https://"}${uri.toString().substringAfter("https://").substringBeforeLast("\"")}")

                SmartMessage(
                    message = "${"RiPlay "}${getString(R.string.opening_url)}",
                    durationLong = true,
                    context = this@MainActivity
                )

                lifecycleScope.launch(Dispatchers.Main) {
                    when (val path = uri.pathSegments.firstOrNull()) {
                        "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                            val browseId = "VL$playlistId"

                            if (playlistId.startsWith("OLAK5uy_")) {
                                Environment.playlistPage(BrowseBody(browseId = browseId))
                                    ?.getOrNull()?.let {
                                        it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                            navController.navigate(route = "${NavRoutes.album.name}/$browseId")

                                        }
                                    }
                            } else {
                                navController.navigate(route = "${NavRoutes.playlist.name}/$browseId")
                            }
                        }

                        "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                            try {
                                navController.navigate(route = "${NavRoutes.artist.name}/$channelId")
                            } catch (e: Exception) {
                                Timber.e("MainActivity.setContent intentUriData ${e.stackTraceToString()}")
                            }
                        }

                        "search" -> uri.getQueryParameter("q")?.let { query ->
                            navController.navigate(route = "${NavRoutes.searchResults.name}/$query")
                        }

                        else -> when {
                            path == "watch" -> uri.getQueryParameter("v")
                            uri.host == "youtu.be" -> path
                            path != "watch" && uri.host == null -> {
                                path.let { query ->
                                    navController.navigate(route = "${NavRoutes.searchResults.name}/$query")
                                }
                                null
                            }
                            uri.host == "www.shazam.com" && (path == "track" || path == "song") -> {
                                shazamSongInfoExtractor(uri.toString(), { artist, title, error ->
                                    Timber.d("MainActivity shazamSongInfoExtractor result $artist $title $error")
                                    if (title.isNotEmpty())
                                        navController.navigate(route = "${NavRoutes.searchResults.name}/${title} ${artist}")

                                })
                                null
                            }

                            else -> null
                        }?.let { videoId ->
                            Environment.song(videoId)?.getOrNull()?.let { song ->
                                val binder = snapshotFlow { binder }.filterNotNull().first()
                                withContext(Dispatchers.Main) {
                                    if (!song.explicit && !preferences.getBoolean(
                                            parentalControlEnabledKey,
                                            false
                                        )
                                    )
                                        binder.player.forcePlay(song.asMediaItem)
                                        //fastPlay(song.asMediaItem, binder)
                                    else
                                        SmartMessage(
                                            "Parental control is enabled",
                                            PopupType.Warning,
                                            context = this@MainActivity
                                        )
                                }
                            }
                        }
                    }
                }
                intentUriData = null
            }

        }

    }

    fun updateSelectedQueue() {
        Database.asyncTransaction {
            selectedQueue.value = Database.selectedQueue() ?: defaultQueue()
        }
    }

    private fun initializeWorker() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val keepAliveRequest = PeriodicWorkRequest.Builder(
            ToolsWorker::class.java,
            5,
            TimeUnit.SECONDS
        ).setConstraints(constraints)
            .addTag("RiPlayKaIdWorker")
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "RiPlayKaIdWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                keepAliveRequest
            )

        //WorkManager.getInstance(this).cancelAllWorkByTag("RiPlayKaIdWorker")

    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            if (preferences.getBoolean(shakeEventEnabledKey, false)) {
                // Fetching x,y,z values
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = currentAcceleration

                // Getting current accelerations
                // with the help of fetched x,y,z values
                currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta: Float = currentAcceleration - lastAcceleration
                acceleration = acceleration * 0.9f + delta

                // Display a Toast message if
                // acceleration value is over 12
                if (acceleration > 12) {
                    shakeCounter++
                    //Toast.makeText(applicationContext, "Shake event detected", Toast.LENGTH_SHORT).show()
                }
                if (shakeCounter >= 1) {
                    //Toast.makeText(applicationContext, "Shaked $shakeCounter times", Toast.LENGTH_SHORT).show()
                    shakeCounter = 0
                    binder?.player?.playNext()
                }

            }

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onResume() {
        super.onResume()

        preferences.edit(commit = true) { putBoolean(appIsRunningKey, true) }

        runCatching {
            sensorManager?.registerListener(
                sensorListener, sensorManager!!.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER
                ), SensorManager.SENSOR_DELAY_NORMAL
            )
        }.onFailure {
            Timber.e("MainActivity.onResume registerListener sensorManager ${it.stackTraceToString()}")
        }
        appRunningInBackground = false

        Timber.d("MainActivity.onResume $appRunningInBackground")
    }

    override fun onPause() {
        super.onPause()

        preferences.edit(commit = true) { putBoolean(appIsRunningKey, false) }

        runCatching {
            sensorListener.let { sensorManager?.unregisterListener(it) }
        }.onFailure {
            Timber.e("MainActivity.onPause unregisterListener sensorListener ${it.stackTraceToString()}")
        }
        appRunningInBackground = true

        Timber.d("MainActivity.onPause $appRunningInBackground")
    }

    @UnstableApi
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentUriData = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()

    }

    override fun onStop() {
        Timber.d("MainActivity.onStop")
        runCatching {
            unbindService(serviceConnection)
        }.onFailure {
            Timber.e("MainActivity.onStop unbindService ${it.stackTraceToString()}")
        }

        if (!isclosebackgroundPlayerEnabled)
            onStart() // some device require white listed



        super.onStop()
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()

        Timber.d("MainActivity.onDestroy")
        preferences.edit(commit = true) { putBoolean(appIsRunningKey, false) }

        runCatching {
            localMonet.removeMonetColorsChangedListener(this)
            _monet = null
        }.onFailure {
            Timber.e("MainActivity.onDestroy removeMonetColorsChangedListener ${it.stackTraceToString()}")
        }

    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        if (!isAtLeastAndroid6) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }

        if (!isAtLeastAndroid8) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val action_search = "it.fast4x.riplay.action.search"
        const val action_songs = "it.fast4x.riplay.action.songs"
        const val action_albums = "it.fast4x.riplay.action.albums"
        const val action_library = "it.fast4x.riplay.action.library"
        const val action_rescuecenter = "it.fast4x.riplay.action.rescuecenter"
        const val action_musicidentifier = "it.fast4x.riplay.action.musicidentifier"
    }


    override fun onMonetColorsChanged(
        monet: MonetCompat,
        monetColors: ColorScheme,
        isInitialChange: Boolean
    ) {
        super<MonetCompatActivity>.onMonetColorsChanged(monet, monetColors, isInitialChange)
        val colorPaletteName =
            preferences.getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
        if (!isInitialChange && colorPaletteName == ColorPaletteName.MaterialYou) {
            /*
            monet.updateMonetColors()
            monet.invokeOnReady {
                startApp()
            }
             */
            this@MainActivity.recreate()
        }
    }

}

var appRunningInBackground: Boolean = false

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { TODO() }

@OptIn(ExperimentalMaterial3Api::class)
val LocalPlayerSheetState =
    staticCompositionLocalOf<BottomSheetState> { error("No sheet state provided") }

val LocalOnlinePlayerPlayingState =
    staticCompositionLocalOf<Boolean> { error("No player sheet state provided") }

//val LocalRiTuneDevices =
//    staticCompositionLocalOf<List<NsdServiceInfo>> { error("No RiTune devices provided") }

val LocalSelectedQueue = staticCompositionLocalOf<Queues?> { error("No selected queue provided") }

val LocalAudioTagger = staticCompositionLocalOf<AudioTagViewModel> { error("No audio tagger provided") }

val LocalBackupManager = staticCompositionLocalOf<BackupViewModel> { error("No backup manager provided") }

val LocalPlayerServiceQueue = staticCompositionLocalOf<PlayerServiceQueueViewModel> { error("No player service queue provided") }

val LocalOnDeviceViewModel = staticCompositionLocalOf<OnDeviceViewModel> { error("No on device view model provided") }



