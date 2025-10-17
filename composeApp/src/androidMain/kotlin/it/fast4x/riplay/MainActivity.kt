package it.fast4x.riplay

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.StrictMode
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import androidx.media3.common.Timeline
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
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.kieronquinn.monetcompat.core.MonetActivityAccessException
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import dev.kdrag0n.monet.theme.ColorScheme
import it.fast4x.environment.Environment
import it.fast4x.environment.models.NavigationEndpoint
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
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.extensions.audiovolume.AudioVolumeObserver
import it.fast4x.riplay.extensions.audiovolume.OnAudioVolumeChangedListener
import it.fast4x.riplay.extensions.discord.DiscordPresenceManager
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOnlinePlayer
import it.fast4x.riplay.extensions.encryptedpreferences.encryptedPreferences
import it.fast4x.riplay.extensions.history.updateOnlineHistory
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
import it.fast4x.riplay.extensions.preferences.bassboostEnabledKey
import it.fast4x.riplay.extensions.preferences.bassboostLevelKey
import it.fast4x.riplay.extensions.preferences.checkUpdateStateKey
import it.fast4x.riplay.extensions.preferences.closeWithBackButtonKey
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
import it.fast4x.riplay.extensions.preferences.discordPersonalAccessTokenKey
import it.fast4x.riplay.extensions.preferences.fontTypeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.isDiscordPresenceEnabledKey
import it.fast4x.riplay.extensions.preferences.isEnabledFullscreenKey
import it.fast4x.riplay.extensions.preferences.isInvincibilityEnabledKey
import it.fast4x.riplay.extensions.preferences.isKeepScreenOnEnabledKey
import it.fast4x.riplay.extensions.preferences.isPauseOnVolumeZeroEnabledKey
import it.fast4x.riplay.extensions.preferences.isProxyEnabledKey
import it.fast4x.riplay.extensions.preferences.languageAppKey
import it.fast4x.riplay.extensions.preferences.lastVideoIdKey
import it.fast4x.riplay.extensions.preferences.loadedDataKey
import it.fast4x.riplay.extensions.preferences.loudnessBaseGainKey
import it.fast4x.riplay.extensions.preferences.miniPlayerTypeKey
import it.fast4x.riplay.extensions.preferences.navigationBarPositionKey
import it.fast4x.riplay.extensions.preferences.navigationBarTypeKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerFirstIconKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerSecondIconKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.pipModuleKey
import it.fast4x.riplay.extensions.preferences.playbackDurationKey
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.proxyHostnameKey
import it.fast4x.riplay.extensions.preferences.proxyModeKey
import it.fast4x.riplay.extensions.preferences.proxyPortKey
import it.fast4x.riplay.extensions.preferences.putEnum
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.restartActivityKey
import it.fast4x.riplay.extensions.preferences.resumeOrPausePlaybackWhenDeviceKey
import it.fast4x.riplay.extensions.preferences.shakeEventEnabledKey
import it.fast4x.riplay.extensions.preferences.showSearchTabKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.transitionEffectKey
import it.fast4x.riplay.extensions.preferences.useSystemFontKey
import it.fast4x.riplay.extensions.preferences.volumeBoostLevelKey
import it.fast4x.riplay.extensions.preferences.volumeNormalizationKey
import it.fast4x.riplay.extensions.preferences.ytCookieKey
import it.fast4x.riplay.extensions.preferences.ytDataSyncIdKey
import it.fast4x.riplay.extensions.preferences.ytVisitorDataKey
import it.fast4x.riplay.extensions.rescuecenter.RescueScreen
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.navigation.AppNavigation
import it.fast4x.riplay.service.AndroidAutoService
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.service.ToolsService
import it.fast4x.riplay.service.ToolsWorker
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.components.BottomSheet
import it.fast4x.riplay.ui.components.BottomSheetState
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.rememberBottomSheetState
import it.fast4x.riplay.ui.components.themed.CrossfadeContainer
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.player.local.LocalMiniPlayer
import it.fast4x.riplay.ui.screens.player.local.LocalPlayer
import it.fast4x.riplay.ui.screens.player.local.rememberLocalPlayerSheetState
import it.fast4x.riplay.ui.screens.player.online.MediaSessionCallback
import it.fast4x.riplay.ui.screens.player.online.OnlineMiniPlayer
import it.fast4x.riplay.ui.screens.player.online.OnlinePlayer
import it.fast4x.riplay.ui.screens.player.online.components.core.ExternalOnlineCore
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.ui.styling.Appearance
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.styling.applyPitchBlack
import it.fast4x.riplay.ui.styling.colorPaletteOf
import it.fast4x.riplay.ui.styling.customColorPalette
import it.fast4x.riplay.ui.styling.dynamicColorPaletteOf
import it.fast4x.riplay.ui.styling.typographyOf
import it.fast4x.riplay.utils.BitmapProvider
import it.fast4x.riplay.utils.DisposableListener
import it.fast4x.riplay.utils.LocalMonetCompat
import it.fast4x.riplay.utils.OkHttpRequest
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.capitalized
import it.fast4x.riplay.utils.clearWebViewData
import it.fast4x.riplay.utils.context
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.getDnsOverHttpsType
import it.fast4x.riplay.utils.getKeepPlayerMinimized
import it.fast4x.riplay.utils.getResumePlaybackOnStart
import it.fast4x.riplay.utils.getSystemlanguage
import it.fast4x.riplay.utils.invokeOnReady
import it.fast4x.riplay.utils.isAtLeastAndroid11
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.utils.isEnabledFullscreen
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isPipModeAutoEnabled
import it.fast4x.riplay.utils.isSkipMediaOnErrorEnabled
import it.fast4x.riplay.utils.isValidHttpUrl
import it.fast4x.riplay.utils.isValidIP
import it.fast4x.riplay.utils.lastMediaItemWasLocal
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.utils.seamlessQueue
import it.fast4x.riplay.utils.setDefaultPalette
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.shuffleQueue
import it.fast4x.riplay.utils.thumbnail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds


const val UNIFIED_NOTIFICATION_CHANNEL = "RiPlay Notifications"
const val NOTIFICATION_ID = 1

@UnstableApi
class MainActivity :
    MonetCompatActivity(),
    //AppCompatActivity()
    MonetColorsChangedListener
{
    //lateinit var internetConnectivityObserver: InternetConnectivityObserver

    var client = OkHttpClient()
    var request = OkHttpRequest(client)
    lateinit var backupHandler: RoomBackup

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is LocalPlayerService.Binder) {
                this@MainActivity.binder = service
            }
            if (service is ToolsService.LocalBinder) {
                this@MainActivity.toolsService = service.serviceInstance.LocalBinder()
            }
            if (service is AndroidAutoService.LocalBinder) {
                this@MainActivity.androidAutoService = service.serviceInstance.LocalBinder()
                service.mediaSessionInjected = unifiedMediaSession
                service.localPlayerBinderInjected = binder
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            //toolsService = null
        }

    }

    private var binder by mutableStateOf<LocalPlayerService.Binder?>(null)
    private var intentUriData by mutableStateOf<Uri?>(null)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeCounter = 0

    private var _monet: MonetCompat? by mutableStateOf(null)
    val localMonet get() = _monet ?: throw MonetActivityAccessException()

    private val pipState: MutableState<Boolean> = mutableStateOf(false)

    var cookie: MutableState<String> =
        mutableStateOf("") //mutableStateOf(preferences.getString(ytCookieKey, "").toString())
    var visitorData: MutableState<String> =
        mutableStateOf("") //mutableStateOf(preferences.getString(ytVisitorDataKey, "").toString())

    var linkDevices: MutableState<List<NsdServiceInfo>> = mutableStateOf(emptyList())

    var onlinePlayerPlayingState: MutableState<Boolean> = mutableStateOf(false)
    var localPlayerPlayingState: MutableState<Boolean> = mutableStateOf(false)

    var selectedQueue: MutableState<Queues> = mutableStateOf(defaultQueue())

    var unifiedMediaSession: MediaSessionCompat? = null
    var onlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)

    var bitmapProvider: BitmapProvider? = null
    // Needed up to android 11
    var notificationActionReceiverUpAndroid11: NotificationActionReceiverUpAndroid11? = null

    var currentSecond: MutableState<Float> = mutableFloatStateOf(0f)
    var currentDuration: MutableState<Float> = mutableFloatStateOf(0f)

    var toolsService by mutableStateOf<ToolsService.LocalBinder?>(null)
    var androidAutoService by mutableStateOf<AndroidAutoService.LocalBinder?>(null)

    private var volumeNormalizationJob: Job? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    private var discordPresenceManager: DiscordPresenceManager? = null

    private var onlinePlayerState = mutableStateOf(PlayerConstants.PlayerState.UNSTARTED)

    private lateinit var onlinePlayerView: YouTubePlayerView

    private var lastError = mutableStateOf<PlayerConstants.PlayerError?>(null)
    private var onlinePlayerIsInitialized = mutableStateOf(false)



    override fun onStart() {
        super.onStart()

        runCatching {
            val intent = Intent(this, LocalPlayerService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)

        }.onFailure {
            Timber.e("MainActivity.onStart bindService ${it.stackTraceToString()}")
        }

        runCatching {
            val intent = Intent(this, ToolsService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)
        }.onFailure {
            Timber.e("MainActivity.onStart startService ToolsService ${it.stackTraceToString()}")
        }

        runCatching {
            val intent = Intent(this, AndroidAutoService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)
        }.onFailure {
            Timber.e("MainActivity.onStart startService AndroidAutoService ${it.stackTraceToString()}")
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @ExperimentalMaterialApi
    @ExperimentalTextApi
    @UnstableApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .build()
            )
        }

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
        //TODO CHECK IF IT WORKS
        localMonet.addMonetColorsChangedListener(
            listener = this,
            notifySelf = false
        )
        localMonet.updateMonetColors()

        Timber.d("MainActivity.onCreate Before localMonet.invokeOnReady")

        localMonet.invokeOnReady {
            Timber.d("MainActivity.onCreate Inside localMonet.invokeOnReady")

            // Load online player now
            onlinePlayerView = LayoutInflater.from(this)
                .inflate(R.layout.youtube_player, null, false)
                as YouTubePlayerView

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

        backupHandler = RoomBackup(this)

        checkIfAppIsRunningInBackground()

        //TODO Implement link client logic
        //registerNsdService()
        discoverNsdServices(
            onServiceFound = {
                linkDevices.value = it
            }
        )

        initializeBitmapProvider()

        initializeNotificationActionReceiverUpAndroid11()

        initializeUnifiedMediaSession()

        updateOnlineNotification()

        updateSelectedQueue()

        initializeBassBoost()

        resumeOrPausePlaybackWhenDevice()

        initializeAudioVolumeObserver()

        initializeDiscordPresence()

        //initializeWorker()


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

    private fun initializeNotificationActionReceiverUpAndroid11() {
        if (!isAtLeastAndroid11) return

        notificationActionReceiverUpAndroid11 = NotificationActionReceiverUpAndroid11()

        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
            addAction(Action.like.value)
            addAction(Action.playradio.value)
            addAction(Action.shuffle.value)
            addAction(Action.search.value)
            addAction(Action.repeat.value)
        }

        ContextCompat.registerReceiver(
            this@MainActivity,
            notificationActionReceiverUpAndroid11,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun initializeDiscordPresence() {
        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            val token = encryptedPreferences.getString(discordPersonalAccessTokenKey, "")
            if (token?.isNotEmpty() == true) {
                discordPresenceManager = DiscordPresenceManager(
                    context = this,
                    getToken = { token },
                )
            }
        }
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
            isPipModeAutoEnabled() &&
            (binder?.player?.isPlaying == true || onlinePlayerPlayingState.value)
        ) maybeEnterPip()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //if (newConfig.orientation in intArrayOf(Configuration.ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_PORTRAIT))
        //    onlinePlayerIsInitialized.value = false // this reinitialize the online player when screen rotate but maybe not needed
        Timber.d("MainActivity.onConfigurationChanged newConfig.orientation ${newConfig.orientation} onlinePlayerIsInitialized ${onlinePlayerIsInitialized.value}")
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
    fun startApp() {

        // Used in QuickPics for load data from remote instead of last saved in SharedPreferences
        preferences.edit(commit = true) { putBoolean(loadedDataKey, false) }

        // Used for android auto to show notification to invite user launch app
        preferences.edit(commit = true) { putBoolean(appIsRunningKey, true) }

        if (!preferences.getBoolean(closeWithBackButtonKey, false))
            if (Build.VERSION.SDK_INT >= 33) {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
                ) {
                    //Log.d("onBackPress", "yeah")
                }
            }

        /*
            Instead of checking getBoolean() individually, we can use .let() to express condition.
            Or, the whole thing is 'false' if null appears in the process.
         */
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


//            try {
//                internetConnectivityObserver.unregister()
//            } catch (e: Exception) {
//                // isn't registered, can be registered without issue
//            }
//            internetConnectivityObserver = InternetConnectivityObserver(this@MainActivity)
//            val isInternetAvailable by internetConnectivityObserver.internetNetworkStatus.collectAsState(true)

            val colorPaletteMode by rememberPreference(
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
                    "https://raw.githubusercontent.com/fast4x/RiPlay/master/updatedVersion/updatedVersionCode.ver"
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
                context().preferences.getEnum(languageAppKey, getSystemlanguage())
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
                if (isLoggedIn())
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

            var appearance by rememberSaveable(
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


            DisposableEffect(binder, !lightTheme) {

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

                                var colorPalette = colorPaletteOf(
                                    colorPaletteName,
                                    colorPaletteMode,
                                    !lightTheme
                                )

                                if (colorPaletteName == ColorPaletteName.Dynamic) {
                                    val artworkUri =
                                        (binder?.player?.currentMediaItem?.mediaMetadata?.artworkUri.thumbnail(
                                            1200
                                        )
                                            ?: "").toString()
                                    artworkUri.let {
                                        if (it.isNotEmpty())
                                            setDynamicPalette(it)
                                        else {

                                            setSystemBarAppearance(colorPalette.isDark)
                                            appearance = appearance.copy(
                                                colorPalette = if (!isPicthBlack) colorPalette else colorPalette.copy(
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
                                            !lightTheme
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
                                            !lightTheme
                                        )
                                        Timber.d("MainActivity.startApp SetContent DisposableEffect customColor POST colorPalette: $colorPalette")
                                    }

                                    setSystemBarAppearance(colorPalette.isDark)

                                    appearance = appearance.copy(
                                        colorPalette = if (!isPicthBlack) colorPalette else colorPalette.copy(
                                            background0 = Color.Black,
                                            background1 = Color.Black,
                                            background2 = Color.Black,
                                            background3 = Color.Black,
                                            background4 = Color.Black,
                                            text = Color.White
                                        ),
                                        typography = appearance.typography.copy(if (!isPicthBlack) colorPalette.text else Color.White),
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

                            volumeNormalizationKey, loudnessBaseGainKey, volumeBoostLevelKey -> initializeNormalizeVolume()
                            bassboostLevelKey, bassboostEnabledKey -> initializeBassBoost()
                            resumeOrPausePlaybackWhenDeviceKey -> resumeOrPausePlaybackWhenDevice()
                            isPauseOnVolumeZeroEnabledKey -> initializeAudioVolumeObserver()
                            isEnabledFullscreenKey -> enableFullscreenMode()
                            notificationPlayerFirstIconKey, notificationPlayerSecondIconKey -> updateUnifiedMediasessionData()
                            queueLoopTypeKey -> updateOnlineNotification()
                        }
                    }

                with(preferences) {
                    registerOnSharedPreferenceChangeListener(listener)

                    val colorPaletteName =
                        getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    if (colorPaletteName == ColorPaletteName.Dynamic) {
                        setDynamicPalette(
                            (binder?.player?.currentMediaItem?.mediaMetadata?.artworkUri.thumbnail(
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
                        action_search
                    )
                ) {
                    openTabFromShortcut =
                        when (intent?.action) {
                            action_songs -> HomeScreenTabs.Songs.index
                            action_albums -> HomeScreenTabs.Albums.index
                            action_library -> HomeScreenTabs.Playlists.index
                            action_search -> -2
                            else -> -1
                        }
                    intent.action = null
                }

                fun <I, O> ComponentActivity.registerActivityResultLauncher(
                    contract: ActivityResultContract<I, O>,
                    callback: ActivityResultCallback<O>
                ): ActivityResultLauncher<I> {
                    val key = UUID.randomUUID().toString()
                    return activityResultRegistry.register(key, contract, callback)
                }

                var onlinePositionAndDuration by remember { mutableStateOf(0L to 0L) }

                // Val onlineCore outside activity
                val externalOnlineCore: @Composable () -> Unit = {
                    ExternalOnlineCore(
                        onlinePlayerView = onlinePlayerView,
                        player = onlinePlayer,
                        onlinePlayerIsInitialized = onlinePlayerIsInitialized,
                        load = getResumePlaybackOnStart() || lastMediaItemWasLocal(),
                        playFromSecond = currentSecond.value,
                        onPlayerReady = { onlinePlayer.value = it },
                        onSecondChange = {
                            coroutineScope.launch(Dispatchers.IO + SupervisorJob()) {
                                currentSecond.value = it
                            }
                        },
                        onDurationChange = {
                            currentDuration.value = it
                            updateOnlineNotification()

                            val mediaItem = binder?.player?.currentMediaItem
                            if (mediaItem != null)
                                updateDiscordPresenceWithOnlinePlayer(
                                    discordPresenceManager,
                                    mediaItem,
                                    onlinePlayerState,
                                    currentDuration.value,
                                    currentSecond.value
                                )
                        },
                        onPlayerStateChange = {
                            Timber.d("MainActivity.onPlayerStateChange $it")
                            onlinePlayerState.value = it
                            onlinePlayerPlayingState.value =
                                it == PlayerConstants.PlayerState.PLAYING

                            val mediaItem = binder?.player?.currentMediaItem
                            if (mediaItem != null)
                                updateDiscordPresenceWithOnlinePlayer(
                                    discordPresenceManager,
                                    mediaItem,
                                    onlinePlayerState,
                                    currentDuration.value,
                                    currentSecond.value
                                )
                        },
                        onTap = {
                            //showControls = !showControls
                        },
                    )
                }

                val pip = isInPip(
                    onChange = {
                        if (!it || (binder?.player?.isPlaying != true && !onlinePlayerPlayingState.value))
                            return@isInPip

                        localPlayerSheetState.expandSoft()
                    }
                )

                CrossfadeContainer(
                    state = pip
                ) { isCurrentInPip ->
                    Timber.d("MainActivity pipState ${pipState.value} CrossfadeContainer isCurrentInPip $isCurrentInPip ")
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
                            LocalLinkDevices provides linkDevices.value,
                            LocalOnlinePlayerPlayingState provides onlinePlayerPlayingState.value,
                            LocalOnlinePositionAndDuration provides onlinePositionAndDuration,
                            LocalSelectedQueue provides selectedQueue.value,
                            LocalBackupHandler provides backupHandler,
                            //LocalInternetAvailable provides isInternetAvailable
                        ) {

                            if (intent.action == action_rescuecenter) {
                                RescueScreen(
                                    onBackup = {
                                        @SuppressLint("SimpleDateFormat")
                                        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                                        backupHandler.database(Database.getInstance)
                                            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                                            .customBackupFileName(
                                                "RiPlay_RescueBackup_${
                                                    dateFormat.format(
                                                        Date()
                                                    )
                                                }.db"
                                            )
                                            .apply {
                                                onCompleteListener { success, message, exitCode ->
                                                    SmartMessage(
                                                        message = if (success) context.resources.getString(R.string.done)
                                                        else message.capitalized(),
                                                        type = if(success) PopupType.Info else PopupType.Warning,
                                                        context = context,
                                                        durationLong = true
                                                    )
                                                    Timber.d("Rescue backup success: $success, message: $message, exitCode: $exitCode")

                                                }
                                            }
                                            .backup()

                                    },
                                    onRestore = {
                                        backupHandler.database(Database.getInstance)
                                            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                                            .apply {
                                                onCompleteListener { success, message, exitCode ->
                                                    SmartMessage(
                                                        message = if (success) context.resources.getString(R.string.restore_completed)
                                                        else message.capitalized(),
                                                        type = if(success) PopupType.Info else PopupType.Warning,
                                                        context = context,
                                                        durationLong = true
                                                    )
                                                    Timber.d("Rescue restore: success $success, message: $message, exitCode: $exitCode")

                                                }
                                            }
                                            .restore()
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
                                                player = onlinePlayer,
                                                playerState = onlinePlayerState,
                                                currentDuration = currentDuration.value,
                                                currentSecond = currentSecond.value,
                                            )
                                        }
                                    },
                                    player = onlinePlayer,
                                    playerState = onlinePlayerState,
                                    openTabFromShortcut = openTabFromShortcut
                                )

                                checkIfAppIsRunningInBackground()
                                if (appRunningInBackground) localPlayerSheetState.collapseSoft()

                                val thumbnailRoundness by rememberPreference(
                                    thumbnailRoundnessKey,
                                    ThumbnailRoundness.Heavy
                                )

                                val localPlayer: @Composable () -> Unit = {
                                    LocalPlayer(
                                        navController = navController,
                                        playerOnline = onlinePlayer,
                                        playerState = onlinePlayerState,
                                        onDismiss = {
                                            localPlayerSheetState.collapseSoft()
                                        }
                                    )
                                }




                                val onlinePlayer: @Composable () -> Unit = {
                                    OnlinePlayer(
                                        navController = navController,
                                        playFromSecond = currentSecond.value,
                                        onlineCore = {
                                            //OnlineCore()
                                            externalOnlineCore()
                                        },
                                        player = onlinePlayer,
                                        playerState = onlinePlayerState,
                                        currentDuration = currentDuration.value,
                                        currentSecond = currentSecond.value,
                                        playerSheetState = localPlayerSheetState,
                                        onDismiss = {
                                            localPlayerSheetState.collapseSoft()
                                        },
                                    )
                                }

                                //Needed to update time in notification
                                LaunchedEffect(onlinePlayerPlayingState.value) {
                                    if (onlinePlayerState.value == PlayerConstants.PlayerState.PLAYING
                                        || onlinePlayerState.value == PlayerConstants.PlayerState.PAUSED )
                                        updateOnlineNotification()
                                    Timber.d("MainActivity LaunchedEffect initializeMediasession onlinePlayerState ${onlinePlayerState.value}")
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
                                    if (binder?.currentMediaItemAsSong?.isLocal == true)
                                        localPlayer()
                                    else
                                        onlinePlayer()
                                }

                                val menuState = LocalMenuState.current
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
                            updateUnifiedMediasessionData()
                        }
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            Timber.d("MainActivity Player.Listener onMediaItemTransition mediaItem $mediaItem reason $reason foreground $appRunningInBackground")

                            if (mediaItem == null) {
                                maybeExitPip()
                                localPlayerSheetState.collapseSoft()
                            }

                            mediaItem?.let {
                                //currentPlaybackPosition.value = 0L
                                currentSecond.value = 0F

                                //Timber.d("MainActivity Player.Listener onMediaItemTransition mediaItemAsSong ${binder!!.currentMediaItemAsSong}")

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
                                    it.mediaMetadata.artworkUri.thumbnail(
                                        1200
                                    ).toString()
                                )

                                bitmapProvider?.load(it.mediaMetadata.artworkUri) {}

                                updateSelectedQueue()

                                initializeNormalizeVolume()

                                updateOnlineNotification()

                                updateDiscordPresenceWithOnlinePlayer(
                                    discordPresenceManager,
                                    it,
                                    onlinePlayerState,
                                    currentDuration.value,
                                    currentSecond.value
                                )

                            }

                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
                                updateMediaSessionQueue(player.currentTimeline)

                        }

                        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                            if (reason != Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) return
                            updateMediaSessionQueue(timeline)
                        }

                        override fun onRepeatModeChanged(repeatMode: Int) {
                            super.onRepeatModeChanged(repeatMode)
                            updateOnlineNotification()
                        }

                        override fun onIsLoadingChanged(isLoading: Boolean) {
                            super.onIsLoadingChanged(isLoading)
                            updateOnlineNotification()
                        }

                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }


            }

            LaunchedEffect(intentUriData) {
                val uri = intentUriData ?: return@LaunchedEffect

                Timber.d("MainActivity LaunchedEffect intentUriData $uri path ${uri.pathSegments.firstOrNull()} host ${uri.host}")

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


    // Fun OnlineCore inside activity
    @Composable
    fun OnlineCore(){
        Timber.d("OnlinePlayerCore: called")
        val binder = LocalPlayerServiceBinder.current

        var localMediaItem = remember { binder?.player?.currentMediaItem }
        if (localMediaItem?.isLocal == true) return


        //val queueLoopType by rememberObservedPreference(queueLoopTypeKey, QueueLoopType.Default)
        val queueLoopType =preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default)


        val lastVideoId = rememberPreference(lastVideoIdKey, "")

        binder?.player?.DisposableListener {
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let {
                        onlinePlayer.value?.pause()
                        binder.player.pause()

                        localMediaItem = it
                        lastVideoId.value = it.mediaId
                        onlinePlayer.value?.loadVideo(it.mediaId, 0f)
                        updateOnlineHistory(it)

                        Timber.d("OnlinePlayerCore: onMediaItemTransition loaded ${it.mediaId}")
                    }
                }

            }
        }

        //var shouldBePlaying by remember { mutableStateOf(false) }
        //val lifecycleOwner = LocalLifecycleOwner.current
        val isLandscape = isLandscape
//        val playerThumbnailSize by rememberPreference(
//            playerThumbnailSizeKey,
//            PlayerThumbnailSize.Biggest
//        )
        val playerThumbnailSize = preferences.getEnum(
            playerThumbnailSizeKey,
            PlayerThumbnailSize.Biggest
        )

        //MedleyMode for online player
//        val playbackDuration by rememberPreference(playbackDurationKey, 0f)
//        var playbackSpeed by rememberPreference(playbackSpeedKey, 1f)
        val playbackDuration =  preferences.getFloat(playbackDurationKey, 0f)
        val playbackSpeed =  preferences.getFloat(playbackSpeedKey, 1f)

//        val isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)
//        val isKeepScreenOnEnabled by rememberPreference(isKeepScreenOnEnabledKey, false)

        val isInvincibilityEnabled = preferences.getBoolean(isInvincibilityEnabledKey, false)
        val isKeepScreenOnEnabled = preferences.getBoolean(isKeepScreenOnEnabledKey, false)


        //  todo Maybe this is the cause for firing consecutive times playnext
        LaunchedEffect(onlinePlayerState.value) {
            if (onlinePlayerState.value == PlayerConstants.PlayerState.ENDED) {
                when (queueLoopType) {
                    QueueLoopType.RepeatOne -> {
                        onlinePlayer.value?.seekTo(0f)
                        Timber.d("OnlinePlayerCore Repeat: RepeatOne fired")
                    }
                    QueueLoopType.Default -> {
                        val hasNext = binder?.player?.hasNextMediaItem()
                        Timber.d("OnlinePlayerCore Repeat: Default fired")
                        if (hasNext == true) {
                            binder.player.playNext()
                            Timber.d("OnlinePlayerCore Repeat: Default fired next")
                        }
                    }
                    QueueLoopType.RepeatAll -> {
                        val hasNext = binder?.player?.hasNextMediaItem()
                        Timber.d("OnlinePlayerCore Repeat: RepeatAll fired")
                        if (hasNext == false) {
                            binder.player.seekTo(0, 0)
                            onlinePlayer.value?.play()
                            Timber.d("OnlinePlayerCore Repeat: RepeatAll fired first")
                        } else {
                            binder?.player?.playNext()
                            Timber.d("OnlinePlayerCore Repeat: RepeatAll fired next")
                        }
                    }
                }

            }

        }


        LaunchedEffect(playbackDuration) {
            if (playbackDuration > 0f)
                while (isActive) {
                    delay((1.seconds * playbackDuration.roundToInt()) + 2.seconds)
                    withContext(Dispatchers.Main) {
                        Timber.d("MedleyMode: Pre fired next")
                        if (onlinePlayerState.value == PlayerConstants.PlayerState.PLAYING) {
                            onlinePlayer.value?.pause()
                            onlinePlayer.value?.seekTo(0f)
                            binder?.player?.playNext()
                            Timber.d("MedleyMode: next fired")
                        }
                    }
                }
        }


        LaunchedEffect(playbackSpeed) {
            val plabackRate = when {
                (playbackSpeed.toDouble() in 0.0..0.25)     -> PlayerConstants.PlaybackRate.RATE_0_25
                (playbackSpeed.toDouble() in 0.26..0.5)     -> PlayerConstants.PlaybackRate.RATE_0_5
                (playbackSpeed.toDouble() in 0.51..0.75)    -> PlayerConstants.PlaybackRate.RATE_0_75
                (playbackSpeed.toDouble() in 0.76..1.0)     -> PlayerConstants.PlaybackRate.RATE_1
                (playbackSpeed.toDouble() in 1.01..1.25)    -> PlayerConstants.PlaybackRate.RATE_1_25
                (playbackSpeed.toDouble() in 1.26..1.5)     -> PlayerConstants.PlaybackRate.RATE_1_5
                (playbackSpeed.toDouble() in 1.51..1.75)    -> PlayerConstants.PlaybackRate.RATE_1_75
                (playbackSpeed.toDouble() > 1.76) -> PlayerConstants.PlaybackRate.RATE_2
                else -> PlayerConstants.PlaybackRate.RATE_1
            }
            onlinePlayer.value?.setPlaybackRate(plabackRate)
        }

        val load by remember { mutableStateOf(getResumePlaybackOnStart() || lastMediaItemWasLocal()) }
        val actAsMini by remember { mutableStateOf(false) }

        Timber.d("OnlinePlayerCore: before create androidview")

        AndroidView(
            //modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            factory = {

                val iFramePlayerOptions = IFramePlayerOptions.Builder(appContext())
                    .controls(0) // show/hide controls
                    .listType("playlist")
                    .origin(context().resources.getString(R.string.env_fqqhBZd0cf))
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        super.onReady(youTubePlayer)
                        onlinePlayer.value = youTubePlayer

//                        val customPlayerUiController =
//                            CustomBasePlayerUiControllerAsListener(
//                                it,
//                                customPLayerUi,
//                                youTubePlayer,
//                                onlinePlayerView
//                            )
//                        youTubePlayer.addListener(customPlayerUiController)

                        // Used to show default player ui with defaultPlayerUiController as custom view
                        val customUiController =
                            CustomDefaultPlayerUiController(
                                this@MainActivity,
                                onlinePlayerView,
                                youTubePlayer,
                                onTap = {}
                            )
                        customUiController.showUi(false) // disable all default controls and buttons
                        customUiController.showMenuButton(false)
                        customUiController.showVideoTitle(false)
                        customUiController.showPlayPauseButton(false)
                        customUiController.showDuration(false)
                        customUiController.showCurrentTime(false)
                        customUiController.showSeekBar(false)
                        customUiController.showBufferingProgress(false)
                        customUiController.showYouTubeButton(false)
                        customUiController.showFullscreenButton(false)
                        onlinePlayerView.setCustomPlayerUi(customUiController.rootView)

                        //youTubePlayer.loadOrCueVideo(lifecycleOwner.lifecycle, mediaItem.mediaId, lastYTVideoSeconds)
                        //Timber.d("OnlinePlayerCore: onReady shouldBePlaying: $shouldBePlaying")
                        if (localMediaItem != null) {
                            if (!load)
                                youTubePlayer.cueVideo(localMediaItem.mediaId, currentSecond.value)
                            else
                                youTubePlayer.loadVideo(localMediaItem.mediaId, currentSecond.value)
                        }

                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        super.onCurrentSecond(youTubePlayer, second)
                        currentSecond.value = second
                        //onSecondChange(second)
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        super.onVideoDuration(youTubePlayer, duration)
                        currentDuration.value = duration
                        //onDurationChange(duration)
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        super.onStateChange(youTubePlayer, state)

//                    val fadeDisabled = getPlaybackFadeAudioDuration() == DurationInMilliseconds.Disabled
//                    val duration = getPlaybackFadeAudioDuration().milliSeconds
//                    if (!fadeDisabled)
//                        startFadeAnimator(
//                            player = player,
//                            duration = duration,
//                            fadeIn = true
//                        )

                        //playerState = state

                        ///////
                        Timber.d("MainActivity.onPlayerStateChange $it")
                        onlinePlayerState.value = state
                        onlinePlayerPlayingState.value =
                            state == PlayerConstants.PlayerState.PLAYING

                        val mediaItem = binder?.player?.currentMediaItem
                        if (mediaItem != null)
                            updateDiscordPresenceWithOnlinePlayer(
                                discordPresenceManager,
                                mediaItem,
                                onlinePlayerState,
                                currentDuration.value,
                                currentSecond.value
                            )
                        ///////

                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {
                        super.onError(youTubePlayer, error)

                        localMediaItem?.isLocal?.let { if (it) return }

                        youTubePlayer.pause()
                        clearWebViewData()

                        Timber.e("OnlinePlayerCore: onError $error")
                        val errorString = when (error) {
                            PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> "Content not playable, recovery in progress, try to click play but if the error persists try to log in"
                            PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Content not found, perhaps no longer available"
                            else -> null
                        }

                        if (errorString != null && lastError.value != error) {
                            SmartMessage(
                                errorString,
                                PopupType.Error,
                                //durationLong = true,
                                context = context()
                            )
                            if (localMediaItem != null)
                                youTubePlayer.cueVideo(localMediaItem.mediaId, 0f)

                        }

                        lastError.value = error

                        if (!isSkipMediaOnErrorEnabled()) return
                        val prev = binder?.player?.currentMediaItem ?: return

                        binder.player.playNext()

                        SmartMessage(
                            message = context().getString(
                                R.string.skip_media_on_error_message,
                                prev.mediaMetadata.title
                            ),
                            context = context(),
                        )

                    }

                }

                onlinePlayerView.apply {
                    enableAutomaticInitialization = false

                    if (isInvincibilityEnabled)
                        enableBackgroundPlayback(true)
                    else
                        lifecycle.addObserver(this)

                    keepScreenOn = isKeepScreenOnEnabled

                    if (!onlinePlayerIsInitialized.value)
                        initialize(listener, iFramePlayerOptions)

                    onlinePlayerIsInitialized.value = true

                }

            },
            update = { view ->

//            (it.parent as? DialogWindowProvider)
//                ?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                view.enableBackgroundPlayback(isInvincibilityEnabled)
                view.keepScreenOn = isKeepScreenOnEnabled

                when(actAsMini) {
                    true -> {
                        view.layoutParams = ViewGroup.LayoutParams(
                            100,
                            100
                        )
                    }
                    false -> {
                        view.layoutParams = if (!isLandscape) {
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                if (playerThumbnailSize == PlayerThumbnailSize.Expanded)
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                else playerThumbnailSize.height
                            )
                        } else {
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                    }
                }

            }
        )
    }

    fun updateSelectedQueue() {
        Database.asyncTransaction {
            selectedQueue.value = Database.selectedQueue() ?: defaultQueue()
        }
    }

    private fun initializeUnifiedMediaSession() {
        if (unifiedMediaSession == null)
            unifiedMediaSession = MediaSessionCompat(this, "OnlinePlayer")

        val repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        unifiedMediaSession?.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        unifiedMediaSession?.setRepeatMode(repeatMode)
        unifiedMediaSession?.isActive = true

        updateOnlineNotification()
    }

    private fun updateUnifiedMediasessionData() {
        Timber.d("MainActivity initializeMediasession")
        val currentMediaItem = binder?.player?.currentMediaItem
        val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
        binder?.let {
            unifiedMediaSession?.setCallback(
                MediaSessionCallback(
                    binder = it,
                    onPlayClick = {
                        Timber.d("MainActivity MediaSessionCallback onPlayClick")
                        if (currentMediaItem?.isLocal == true)
                            it.player.play()
                        else
                            onlinePlayer.value?.play()
                    },
                    onPauseClick = {
                        Timber.d("MainActivity MediaSessionCallback onPauseClick")
                        it.player.pause()
                        onlinePlayer.value?.pause()
                    },
                    onSeekToPos = { second ->
                        val newPosition = (second / 1000).toFloat()
                        Timber.d("MainActivity MediaSessionCallback onSeekPosTo ${newPosition}")
                        onlinePlayer.value?.seekTo(newPosition)
                        //currentPlaybackPosition.value = second
                        currentSecond.value = second.toFloat()
                    },
                    onPlayNext = {
                        it.player.playNext()
                    },
                    onPlayPrevious = {
                        it.player.playPrevious()
                    },
                    onPlayQueueItem = { id ->
                        it.player.seekToDefaultPosition(id.toInt())
                    },
                    onCustomClick = { customAction ->
                        Timber.d("MainActivity MediaSessionCallback onCustomClick $customAction")
                        when (customAction) {
                            NotificationButtons.Favorites.action -> {
                                if (currentMediaItem != null)
                                    mediaItemToggleLike(currentMediaItem)
                            }
                            NotificationButtons.Repeat.action -> {
                                preferences.edit(commit = true) { putEnum(queueLoopTypeKey, setQueueLoopState(queueLoopType)) }
                            }
                            NotificationButtons.Shuffle.action -> {
                                it.player.shuffleQueue()
                            }
                            NotificationButtons.Radio.action -> {
                                if (currentMediaItem != null) {
                                    it.stopRadio()
                                    it.player.seamlessQueue(currentMediaItem)
                                    onlinePlayer.value?.play()
                                    it.setupRadio(
                                        NavigationEndpoint.Endpoint.Watch(videoId = currentMediaItem.mediaId)
                                    )
                                }
                            }
                            NotificationButtons.Search.action -> {
                                it.actionSearch()
                            }
                        }
                        updateOnlineNotification()
                    }
                )
            )
        }

        unifiedMediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    currentMediaItem?.mediaId
                )
                .putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    bitmapProvider?.bitmap
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    currentMediaItem?.mediaMetadata?.title.toString()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentMediaItem?.mediaMetadata?.artist.toString()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                    currentMediaItem?.mediaMetadata?.albumTitle.toString()
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (currentDuration.value * 1000).toLong())
                .build()
        )

        Timber.d("MainActivity updateMediasessionData onlineplayer playing ${onlinePlayerPlayingState.value} localplayer playing ${binder?.player?.isPlaying}")

        val actions =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO

        // todo Improve custom actions in online player notification
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        val firstCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerFirstIcon }
            .map {
                PlaybackStateCompat.CustomAction.Builder(
                    it.action,
                    it.name,
                    it.getStateIcon(
                        it,
                        binder?.currentMediaItemAsSong?.likedAt,
                        binder?.player?.repeatMode ?: 0,
                        binder?.player?.shuffleModeEnabled ?: false
                    ),
                ).build()
            }.first()


        val secondCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerSecondIcon }
            .map {
                PlaybackStateCompat.CustomAction.Builder(
                    it.action,
                    it.name,
                    it.getStateIcon(
                        it,
                        binder?.currentMediaItemAsSong?.likedAt,
                        binder?.player?.repeatMode ?: 0,
                        binder?.player?.shuffleModeEnabled ?: false
                    ),
                ).build()
            }.first()


        unifiedMediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder().setActions(actions.let {
                if (isAtLeastAndroid12) it or PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED else it
            })
                .apply {
                    addCustomAction(firstCustomAction)
                    addCustomAction(secondCustomAction)
                    setState(
                        if (onlinePlayerPlayingState.value || localPlayerPlayingState.value)
                            PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        (currentSecond.value * 1000).toLong(), //currentPlaybackPosition.value,
                        1f
                    )
                }
            .build()
        )



    }

    private fun updateMediaSessionQueue(timeline: Timeline) {
        if (binder == null) return

        val currentMediaItemIndex = binder!!.player.currentMediaItemIndex
        val lastIndex = timeline.windowCount - 1
        var startIndex = currentMediaItemIndex - 7
        var endIndex = currentMediaItemIndex + 7

        if (startIndex < 0) endIndex -= startIndex

        if (endIndex > lastIndex) {
            startIndex -= (endIndex - lastIndex)
            endIndex = lastIndex
        }

        startIndex = startIndex.coerceAtLeast(0)

        unifiedMediaSession?.setQueue(
            List(endIndex - startIndex + 1) { index ->
                val mediaItem = timeline.getWindow(index + startIndex, Timeline.Window()).mediaItem
                MediaSessionCompat.QueueItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setTitle(mediaItem.mediaMetadata.title)
                        .setSubtitle(mediaItem.mediaMetadata.artist)
                        .setIconUri(mediaItem.mediaMetadata.artworkUri)
                        .build(),
                    (index + startIndex).toLong()
                )
            }
        )
    }

    private fun initializeBitmapProvider() {
        runCatching {
            bitmapProvider = BitmapProvider(
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("Failed init bitmap provider in MainActivity ${it.stackTraceToString()}")
        }
    }

    fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            UNIFIED_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(UNIFIED_NOTIFICATION_CHANNEL)
            .setShowBadge(false)
            .build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    @UnstableApi
    fun updateOnlineNotification() {
        val currentMediaItem = binder?.player?.currentMediaItem
        if (currentMediaItem?.isLocal == true) return

        //if (bitmapProvider?.bitmap == null)
        //    runBlocking {
                bitmapProvider?.load(currentMediaItem?.mediaMetadata?.artworkUri) {}
        //    }


        updateUnifiedMediasessionData()

        createNotificationChannel()

        val forwardAction = NotificationCompat.Action.Builder(
            R.drawable.play_skip_forward,
            "next",
            Action.next.pendingIntent
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            if (onlinePlayerPlayingState.value) R.drawable.pause else R.drawable.play,
            if (onlinePlayerPlayingState.value) "pause" else "play",
            if (onlinePlayerPlayingState.value) Action.pause.pendingIntent
            else Action.play.pendingIntent,
        ).build()

        val previousAction = NotificationCompat.Action.Builder(
            R.drawable.play_skip_back,
            "prev",
            Action.previous.pendingIntent
        ).build()


        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        val firstCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerFirstIcon }
            .map {
                NotificationCompat.Action.Builder(
                    it.getStateIcon(
                        it,
                        binder?.currentMediaItemAsSong?.likedAt,
                        binder?.player?.repeatMode ?: 0,
                        binder?.player?.shuffleModeEnabled ?: false
                    ),
                    it.name,
                    it.pendingIntentOnline,
                ).build()
            }.first()


        val secondCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerSecondIcon }
            .map {
                NotificationCompat.Action.Builder(
                    it.getStateIcon(
                        it,
                        binder?.currentMediaItemAsSong?.likedAt,
                        binder?.player?.repeatMode ?: 0,
                        binder?.player?.shuffleModeEnabled ?: false
                    ),
                    it.name,
                    it.pendingIntentOnline,
                ).build()
            }.first()


        val notification = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(this, UNIFIED_NOTIFICATION_CHANNEL)
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle(currentMediaItem?.mediaMetadata?.title)
            .setContentText(currentMediaItem?.mediaMetadata?.artist)
            //.setSubText(currentMediaItem?.mediaMetadata?.artist)
            .setContentInfo(currentMediaItem?.mediaMetadata?.albumTitle)
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(bitmapProvider?.bitmap)
            .setShowWhen(false)
            .setSilent(true)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(firstCustomAction)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(forwardAction)
            .addAction(secondCustomAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3)
                    .setMediaSession(unifiedMediaSession?.sessionToken)

            )
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java)
                        .putExtra("expandPlayerBottomSheet", true),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()

        //workaround for android 12+
//        runCatching {
//            notification.let {
//                ServiceCompat.startForeground(
//                    toolsService,
//                    NOTIFICATION_ID,
//                    it,
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
//                    } else {
//                        0
//                    }
//                )
//            }
//        }.onFailure {
//            Timber.e("PlayerService oncreate startForeground ${it.stackTraceToString()}")
//        }

        NotificationManagerCompat.from(this@MainActivity).notify(NOTIFICATION_ID, notification)
    }

    private fun initializeBassBoost() {
        if (!preferences.getBoolean(bassboostEnabledKey, false)) {
            runCatching {
                bassBoost?.enabled = false
                bassBoost?.release()
            }
            bassBoost = null
            initializeNormalizeVolume()
            return
        }

        runCatching {
            if (bassBoost == null) bassBoost = BassBoost(0, 0)
            val bassboostLevel =
                (preferences.getFloat(bassboostLevelKey, 0.5f) * 1000f).toInt().toShort()
            Timber.d("MainActivity processBassBoost bassboostLevel $bassboostLevel")
            bassBoost?.enabled = false
            bassBoost?.setStrength(bassboostLevel)
            bassBoost?.enabled = true
        }.onFailure {
            SmartMessage(
                "Can't enable bass boost",
                context = this
            )
        }
    }

    @UnstableApi
    private fun initializeNormalizeVolume() {
        if (!preferences.getBoolean(volumeNormalizationKey, false)) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            return
        }

        runCatching {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(0)
            }
        }.onFailure {
            Timber.e("MainActivity processNormalizeVolume loudnessEnhancer ${it.stackTraceToString()}")
            return
        }

        val baseGain = preferences.getFloat(loudnessBaseGainKey, 5.00f)
        val volumeBoostLevel = preferences.getFloat(volumeBoostLevelKey, 0f)
        binder?.player?.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.IO) {
                fun Float?.toMb() = ((this ?: 0f) * 100).toInt()
                Database.loudnessDb(songId).cancellable().collectLatest { loudnessDb ->
                    val loudnessMb = loudnessDb.toMb().let {
                        if (it !in -2000..2000) {
                            withContext(Dispatchers.IO) {
                                SmartMessage(
                                    "Extreme loudness detected",
                                    context = this@MainActivity
                                )
                            }

                            0
                        } else it
                    }
                    try {
                        loudnessEnhancer?.setTargetGain(baseGain.toMb() + volumeBoostLevel.toMb() - loudnessMb)
                        loudnessEnhancer?.enabled = true
                    } catch (e: Exception) {
                        Timber.e("MainActivity processNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                    }
                }
            }
        }
    }

    private fun resumeOrPausePlaybackWhenDevice() {
        if (!isAtLeastAndroid6) return

        if (preferences.getBoolean(resumeOrPausePlaybackWhenDeviceKey, false)) {
            if (audioManager == null) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
            }

            audioDeviceCallback = object : AudioDeviceCallback() {
                private fun canPlayMusic(audioDeviceInfo: AudioDeviceInfo): Boolean {
                    if (!audioDeviceInfo.isSink) return false

                    return audioDeviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_REMOTE_SUBMIX
                }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    Timber.d("MainActivity onAudioDevicesAdded addedDevices ${addedDevices.map { it.type }}")
                    if (addedDevices.any(::canPlayMusic)) {
                        Timber.d("MainActivity onAudioDevicesAdded device known ${addedDevices.map { it.productName }}")
                        onlinePlayer.value?.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    Timber.d("MainActivity onAudioDevicesRemoved removedDevices ${removedDevices.map { it.type }}")
                    if (removedDevices.any(::canPlayMusic)) {
                        Timber.d("MainActivity onAudioDevicesRemoved device known ${removedDevices.map { it.productName }}")
                        onlinePlayer.value?.pause()
                    }

                }
            }

            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)

        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }

    private fun initializeAudioVolumeObserver() {
        val onAudioVolumeChangedListener = object : OnAudioVolumeChangedListener {
            private var pausedByZeroVolume = false
            override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
                if (context().preferences.getBoolean(isPauseOnVolumeZeroEnabledKey, false)) {
                    if (onlinePlayerPlayingState.value && currentVolume < 1) {
                        onlinePlayer.value?.pause()
                        pausedByZeroVolume = true
                    } else if (pausedByZeroVolume && currentVolume >= 1) {
                        onlinePlayer.value?.play()
                        pausedByZeroVolume = false
                    }
                }
            }

            override fun onAudioVolumeDirectionChanged(direction: Int) {
                // todo handle volume keys to change media
//            if (!context().preferences.getBoolean(useVolumeKeysToChangeSongKey, false)) return
//
//            if (direction == 0) {
//                binder?.player?.playPrevious()
//            } else {
//                binder?.player?.playNext()
//            }

            }
        }

        audioVolumeObserver = AudioVolumeObserver(context())
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, onAudioVolumeChangedListener)

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

    @JvmInline
    value class Action(val value: String) {
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                appContext(),
                110,
                Intent(value).setPackage(appContext().packageName),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {

            val pause = Action("it.fast4x.riplay.onlineplayer.pause")
            val play = Action("it.fast4x.riplay.onlineplayer.play")
            val next = Action("it.fast4x.riplay.onlineplayer.next")
            val previous = Action("it.fast4x.riplay.onlineplayer.previous")
            val like = Action("it.fast4x.riplay.onlineplayer.like")
            val playradio = Action("it.fast4x.riplay.onlineplayer.playradio")
            val shuffle = Action("it.fast4x.riplay.onlineplayer.shuffle")
            val search = Action("it.fast4x.riplay.onlineplayer.search")
            val repeat = Action("it.fast4x.riplay.onlineplayer.repeat")

        }
    }

    inner class NotificationActionReceiverUpAndroid11() : BroadcastReceiver() {

        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("MainActivity onReceive intent.action: ${intent.action}")
            val currentMediaItem = binder?.player?.currentMediaItem
            val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
            binder?.let {
                when (intent.action) {
                    Action.pause.value -> onlinePlayer.value?.pause()
                    Action.play.value -> onlinePlayer.value?.play()
                    Action.next.value -> it.player.playNext()
                    Action.previous.value -> it.player.playPrevious()
                    Action.like.value -> {
                        if (currentMediaItem != null)
                            mediaItemToggleLike(currentMediaItem)
                    }
                    Action.repeat.value -> {
                        preferences.edit(commit = true) { putEnum(queueLoopTypeKey, setQueueLoopState(queueLoopType)) }
                    }
                    Action.shuffle.value -> {
                        it.player.shuffleQueue()
                    }
                    Action.playradio.value -> {
                        if (currentMediaItem != null) {
                            it.stopRadio()
                            it.player.seamlessQueue(currentMediaItem)
                            onlinePlayer.value?.play()
                            it.setupRadio(
                                NavigationEndpoint.Endpoint.Watch(videoId = currentMediaItem.mediaId)
                            )
                        }
                    }
                    Action.search.value -> {
                        it.actionSearch()
                    }

                }
            }
            updateOnlineNotification()
        }

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

        updateOnlineNotification()

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
        updateOnlineNotification()
        Timber.d("MainActivity.onPause $appRunningInBackground")
    }

    @UnstableApi
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentUriData = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()

    }

    override fun onStop() {
        runCatching {
            unbindService(serviceConnection)
        }.onFailure {
            Timber.e("MainActivity.onStop unbindService ${it.stackTraceToString()}")
        }
        super.onStop()
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()

        Timber.d("MainActivity.onDestroy")


        preferences.edit(commit = true) { putBoolean(appIsRunningKey, false) }

        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            Timber.d("[DiscordPresence] onStop: call the manager (close discord presence)")
            discordPresenceManager?.onStop()
        }

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

val LocalPlayerServiceBinder = staticCompositionLocalOf<LocalPlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { TODO() }

@OptIn(ExperimentalMaterial3Api::class)
val LocalPlayerSheetState =
    staticCompositionLocalOf<BottomSheetState> { error("No sheet state provided") }

val LocalOnlinePlayerPlayingState =
    staticCompositionLocalOf<Boolean> { error("No player sheet state provided") }

val LocalOnlinePositionAndDuration = staticCompositionLocalOf<Pair<Long, Long>> { error("No player sheet state provided") }

val LocalLinkDevices =
    staticCompositionLocalOf<List<NsdServiceInfo>> { error("No link devices provided") }

val LocalSelectedQueue = staticCompositionLocalOf<Queues?> { error("No selected queue provided") }

val LocalBackupHandler = staticCompositionLocalOf<RoomBackup> { error("No backup handler provided") }

