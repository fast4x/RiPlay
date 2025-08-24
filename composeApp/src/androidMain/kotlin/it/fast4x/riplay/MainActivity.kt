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
import android.media.session.PlaybackState
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.kieronquinn.monetcompat.core.MonetActivityAccessException
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import dev.kdrag0n.monet.theme.ColorScheme
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.requests.playlistPage
import it.fast4x.environment.requests.song
import it.fast4x.environment.utils.LocalePreferenceItem
import it.fast4x.environment.utils.LocalePreferences
import it.fast4x.environment.utils.ProxyPreferenceItem
import it.fast4x.environment.utils.ProxyPreferences
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
import it.fast4x.riplay.service.OfflinePlayerService
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.themed.CrossfadeContainer
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.player.offline.OfflineMiniPlayer
import it.fast4x.riplay.ui.styling.Appearance
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.styling.applyPitchBlack
import it.fast4x.riplay.ui.styling.colorPaletteOf
import it.fast4x.riplay.ui.styling.customColorPalette
import it.fast4x.riplay.ui.styling.dynamicColorPaletteOf
import it.fast4x.riplay.ui.styling.typographyOf
import it.fast4x.riplay.utils.LocalMonetCompat
import it.fast4x.riplay.utils.OkHttpRequest
import it.fast4x.riplay.extensions.rescuecenter.RescueScreen
import it.fast4x.riplay.models.Queues
import it.fast4x.riplay.models.defaultQueue
import it.fast4x.riplay.service.BitmapProvider
import it.fast4x.riplay.service.EndlessService
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.components.BottomSheet
import it.fast4x.riplay.ui.components.rememberBottomSheetState
import it.fast4x.riplay.ui.screens.player.fastPlay
import it.fast4x.riplay.ui.screens.player.offline.OfflinePlayer
import it.fast4x.riplay.ui.screens.player.offline.PlayerSheetState
import it.fast4x.riplay.ui.screens.player.offline.rememberPlayerSheetState
import it.fast4x.riplay.ui.screens.player.online.MediaSessionCallback
import it.fast4x.riplay.ui.screens.player.online.OnlineMiniPlayer
import it.fast4x.riplay.ui.screens.player.online.OnlinePlayer
import it.fast4x.riplay.ui.screens.player.online.components.core.OnlinePlayerCore
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.UiTypeKey
import it.fast4x.riplay.extensions.preferences.animatedGradientKey
import it.fast4x.riplay.extensions.preferences.applyFontPaddingKey
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.backgroundProgressKey
import it.fast4x.riplay.utils.capitalized
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
import it.fast4x.riplay.extensions.preferences.fontTypeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.utils.getSystemlanguage
import it.fast4x.riplay.utils.invokeOnReady
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.extensions.preferences.isKeepScreenOnEnabledKey
import it.fast4x.riplay.extensions.preferences.isProxyEnabledKey
import it.fast4x.riplay.utils.isValidHttpUrl
import it.fast4x.riplay.utils.isValidIP
import it.fast4x.riplay.extensions.preferences.keepPlayerMinimizedKey
import it.fast4x.riplay.extensions.preferences.languageAppKey
import it.fast4x.riplay.extensions.preferences.loadedDataKey
import it.fast4x.riplay.extensions.preferences.miniPlayerTypeKey
import it.fast4x.riplay.extensions.preferences.navigationBarPositionKey
import it.fast4x.riplay.extensions.preferences.navigationBarTypeKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.pipModuleKey
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.proxyHostnameKey
import it.fast4x.riplay.extensions.preferences.proxyModeKey
import it.fast4x.riplay.extensions.preferences.proxyPortKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.extensions.preferences.restartActivityKey
import it.fast4x.riplay.utils.setDefaultPalette
import it.fast4x.riplay.extensions.preferences.shakeEventEnabledKey
import it.fast4x.riplay.extensions.preferences.showSearchTabKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.utils.thumbnail
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.transitionEffectKey
import it.fast4x.riplay.extensions.preferences.useSystemFontKey
import it.fast4x.riplay.extensions.preferences.ytCookieKey
import it.fast4x.riplay.extensions.preferences.ytDataSyncIdKey
import it.fast4x.riplay.extensions.preferences.ytVisitorDataKey
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
import kotlin.math.roundToInt
import kotlin.math.sqrt


const val NOTIFICATION_CHANNEL = "OnlinePlayer"
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
            if (service is OfflinePlayerService.Binder) {
                this@MainActivity.binder = service
            }
            if (service is EndlessService.LocalBinder) {
                this@MainActivity.endlessService = service.serviceInstance
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            endlessService = null
        }

    }

    private var binder by mutableStateOf<OfflinePlayerService.Binder?>(null)
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
        mutableStateOf("") //mutableStateOf(preferences.getString(ytCookieKey, "").toString())
    var visitorData: MutableState<String> =
        mutableStateOf("") //mutableStateOf(preferences.getString(ytVisitorDataKey, "").toString())

    var linkDevices: MutableState<List<NsdServiceInfo>> = mutableStateOf(emptyList())

    var onlinePlayerPlayingState: MutableState<Boolean> = mutableStateOf(false)

    var selectedQueue: MutableState<Queues> = mutableStateOf(defaultQueue())

    var mediaSession: MediaSessionCompat? = null
    var onlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)
    val actions =
        PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SEEK_TO
    val stateBuilder =
        PlaybackStateCompat.Builder().setActions(actions.let {
            if (isAtLeastAndroid12) it or PlaybackState.ACTION_SET_PLAYBACK_SPEED else it
        })

    var bitmapProvider: BitmapProvider? = null
    var onlinePlayerNotificationActionReceiver: OnlinePlayerNotificationActionReceiver? = null
    var currentPlaybackPosition: MutableState<Long> = mutableStateOf(0)
    var currentPlaybackDuration: MutableState<Long> = mutableStateOf(0)

    var endlessService: EndlessService? = null

    var mediaItemIsLocal: MutableState<Boolean> = mutableStateOf(false)


    override fun onStart() {
        super.onStart()

        runCatching {
            val intent = Intent(this, OfflinePlayerService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)

        }.onFailure {
            Timber.e("MainActivity.onStart bindService ${it.stackTraceToString()}")
        }

        runCatching {
            val intent = Intent(this, EndlessService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)
        }.onFailure {
            Timber.e("MainActivity.onStart startService EndlessService ${it.stackTraceToString()}")
        }

    }

    @ExperimentalMaterialApi
    @ExperimentalTextApi
    @UnstableApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        WindowCompat.setDecorFitsSystemWindows(window, false)

        /***********/
        // TODO() enable fullscreen mode
        // Old method to hide status bar
        // requestWindowFeature(Window.FEATURE_NO_TITLE)
        // this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // New method to hide system bars
//        val windowInsetsController =
//            WindowCompat.getInsetsController(window, window.decorView)
//        // Configure the behavior of the hidden system bars.
//        windowInsetsController.systemBarsBehavior =
//            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//
//        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
////        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
////        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        /***********/

        MonetCompat.setup(this)
        _monet = MonetCompat.getInstance()
        localMonet.setDefaultPalette()
        //TODO CHECK IF IT WORKS
        localMonet.addMonetColorsChangedListener(
            listener = this,
            notifySelf = false
        )
        localMonet.updateMonetColors()

        Timber.d("MainActivity onCreate Before localMonet.invokeOnReady")

        localMonet.invokeOnReady {
            Timber.d("MainActivity onCreate Inside localMonet.invokeOnReady")
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

        initializeMediasession()

        initializeBitmapProvider()

        onlinePlayerNotificationActionReceiver = OnlinePlayerNotificationActionReceiver()
        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)

        }

        ContextCompat.registerReceiver(
            this@MainActivity,
            onlinePlayerNotificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )


        updateOnlineNotification()

        updateSelectedQueue()

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
        println("MainActivity.onLowMemory")
        Timber.d("MainActivity.onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_UI_HIDDEN")
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_UI_HIDDEN")
        }
        if (level == TRIM_MEMORY_RUNNING_LOW) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_LOW")
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_LOW")
        }
        if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_CRITICAL")
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_CRITICAL")
        }
        if (level == TRIM_MEMORY_BACKGROUND) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_BACKGROUND")
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_BACKGROUND")
        }
        if (level == TRIM_MEMORY_COMPLETE) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_COMPLETE")
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_COMPLETE")
        }
        if (level == TRIM_MEMORY_MODERATE) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_MODERATE")
            Timber.d("MainActivity.onTrimMemory TRIM_MEMORY_MODERATE")
        }
        if (level == TRIM_MEMORY_RUNNING_MODERATE) {
            println("MainActivity.onTrimMemory TRIM_MEMORY_RUNNING_MODERATE")
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
        ExperimentalMaterial3Api::class
    )
    fun startApp() {

        // Used in QuickPics for load data from remote instead of last saved in SharedPreferences
        preferences.edit(commit = true) { putBoolean(loadedDataKey, false) }

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

        println("MainActivity.onCreate launchedFromNotification: $launchedFromNotification intent $intent.action")

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
            //var mediaItemIsLocal = rememberSaveable { mutableStateOf(false) }
            var switchToAudioPlayer by rememberSaveable { mutableStateOf(false) }
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
            if (visitorData.value.isEmpty() || visitorData.value == "null")
                runCatching {
                    println("MainActivity.onCreate visitorData.isEmpty() getInitialVisitorData visitorData ${visitorData.value}")
                    visitorData.value = runBlocking {
                        Environment.getInitialVisitorData().getOrNull()
                    }.takeIf { it != "null" } ?: Environment._uMYwa66ycM
                    // Save visitorData in SharedPreferences
                    preferences.edit { putString(ytVisitorDataKey, visitorData.value) }
                }.onFailure {
                    Timber.e("MainActivity.onCreate visitorData.isEmpty() getInitialVisitorData ${it.stackTraceToString()}")
                    println("MainActivity.onCreate visitorData.isEmpty() getInitialVisitorData ${it.stackTraceToString()}")
                    visitorData.value = Environment._uMYwa66ycM
                }

            Environment.visitorData = visitorData.value
            println("MainActivity.onCreate visitorData in use: ${visitorData.value}")

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

            println("MainActivity.onCreate cookie: ${cookie.value}")
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
                        println("MainActivity.startApp SetContent with(preferences) customColor PRE colorPalette: $colorPalette")
                        colorPalette = dynamicColorPaletteOf(
                            Color(customColor),
                            !lightTheme
                        )
                        println("MainActivity.startApp SetContent with(preferences) customColor POST colorPalette: $colorPalette")
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
                                println("MainActivity.recreate()")
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
                                        println("MainActivity.startApp SetContent DisposableEffect customColor PRE colorPalette: $colorPalette")
                                        colorPalette = dynamicColorPaletteOf(
                                            Color(customColor),
                                            !lightTheme
                                        )
                                        println("MainActivity.startApp SetContent DisposableEffect customColor POST colorPalette: $colorPalette")
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

                val playerSheetState = rememberPlayerSheetState(
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

                var currentSecond by remember { mutableFloatStateOf(0f) }
                val onlinePlayerState =
                    remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
                //var showControls by remember { mutableStateOf(true) }
                var currentDuration by remember { mutableFloatStateOf(0f) }
                val onlineCore: @Composable () -> Unit = {
                    OnlinePlayerCore(
                        load = getResumePlaybackOnStart(),
                        playFromSecond = currentSecond,
                        onPlayerReady = { onlinePlayer.value = it },
                        onSecondChange = {
                            currentSecond = it
                            currentPlaybackPosition.value = (it * 1000).toLong()
                            //println("MainActivity onSecondChange ${currentPlaybackPosition.value}")
                        },
                        onDurationChange = {
                            currentDuration = it
                            currentPlaybackDuration.value = (it * 1000).toLong()
                            println("MainActivity onDurationChange ${currentPlaybackDuration.value}")
                        },
                        onPlayerStateChange = {
                            onlinePlayerState.value = it
                            onlinePlayerPlayingState.value =
                                it == PlayerConstants.PlayerState.PLAYING
                            updateOnlineNotification()

                        },
                        onTap = {
                            //showControls = !showControls
                        }
                    )
                }

                val pip = isInPip(
                    onChange = {
                        if (!it || (binder?.player?.isPlaying != true && !onlinePlayerPlayingState.value))
                            return@isInPip

                        playerSheetState.expandSoft()
                    }
                )

                CrossfadeContainer(
                    state = pip //pipState.value
                ) { isCurrentInPip ->
                    println("MainActivity pipState ${pipState.value} CrossfadeContainer isCurrentInPip $isCurrentInPip ")
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
                            LocalPlayerSheetState provides playerSheetState,
                            LocalMonetCompat provides localMonet,
                            LocalLinkDevices provides linkDevices.value,
                            LocalOnlinePlayerPlayingState provides onlinePlayerPlayingState.value,
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
                                                    println("Rescue backup success: $success, message: $message, exitCode: $exitCode")

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
                                                    println("Rescue restore: success  $success, message: $message, exitCode: $exitCode")

                                                }
                                            }
                                            .restore()
                                    }
                                )
                            } else {


                                val localPlayerSheetState = rememberBottomSheetState(
                                    dismissedBound = 0.dp,
                                    collapsedBound = 5.dp, //Dimensions.collapsedPlayer,
                                    expandedBound = maxHeight
                                )

//                                LaunchedEffect(showControls) {
//                                    if (showControls) {
//                                        delay(5000)
//                                        showControls = false
//                                    }
//                                }

                                AppNavigation(
                                    navController = navController,
                                    miniPlayer = {
                                        //println("MainActivity miniPlayer mediaItemIsLocal ${mediaItemIsLocal.value}")
                                        if (mediaItemIsLocal.value)
                                            OfflineMiniPlayer(
                                                showPlayer = { localPlayerSheetState.expandSoft() },
                                                hidePlayer = { localPlayerSheetState.collapseSoft() },
                                                navController = navController,
                                            )
                                        else {
                                            OnlineMiniPlayer(
                                                showPlayer = {
                                                    localPlayerSheetState.expandSoft()
                                                },
                                                hidePlayer = { localPlayerSheetState.collapseSoft() },
                                                navController = navController,
                                                player = onlinePlayer,
                                                playerState = onlinePlayerState,
                                                currentDuration = currentDuration,
                                                currentSecond = currentSecond,
                                            )
                                        }
                                    },
                                    player = onlinePlayer,
                                    playerState = onlinePlayerState,
                                    openTabFromShortcut = openTabFromShortcut
                                )

                                checkIfAppIsRunningInBackground()


                                val thumbnailRoundness by rememberPreference(
                                    thumbnailRoundnessKey,
                                    ThumbnailRoundness.Heavy
                                )

                                val offlinePlayer: @Composable () -> Unit = {
                                    OfflinePlayer(
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
                                        playFromSecond = currentSecond,
                                        onlineCore = onlineCore,
                                        player = onlinePlayer,
                                        playerState = onlinePlayerState,
                                        currentDuration = currentDuration,
                                        currentSecond = currentSecond,
                                        //showControls = showControls,
                                        playerSheetState = localPlayerSheetState,
                                        onDismiss = {
                                            localPlayerSheetState.collapseSoft()
                                        },
                                    )

                                }

                                //Needed to update time in notification
                                LaunchedEffect(onlinePlayerPlayingState.value) {
                                    updateOnlineNotification()
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
                                    if (mediaItemIsLocal.value)
                                        offlinePlayer()
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
//                                    BottomSheetMenu(
//                                        state = menuState,
//                                        modifier = Modifier.fillMaxSize()
//                                    )

                            }
                        }

                }
                DisposableEffect(binder?.player) {
                    val player = binder?.player ?: return@DisposableEffect onDispose { }

                    if (player.currentMediaItem == null) {
                        if (playerSheetState.isExpanded) {
                            playerSheetState.collapseSoft()
                        }
                    } else {
                        if (launchedFromNotification) {
                            intent.replaceExtras(Bundle())
                            if (preferences.getBoolean(keepPlayerMinimizedKey, false))
                                playerSheetState.collapseSoft()
                            else playerSheetState.expandSoft()
                        } else {
                            playerSheetState.collapseSoft()
                        }
                    }

                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            println("MainActivity Player.Listener onMediaItemTransition mediaItem $mediaItem reason $reason foreground $appRunningInBackground")

                            if (mediaItem == null) {
                                maybeExitPip()
                                playerSheetState.dismissSoft()
                            }

                            mediaItemIsLocal.value = mediaItem?.isLocal == true
                            currentPlaybackPosition.value = 0L

//                            if (!mediaItemIsLocal.value && mediaItem != null) {
//                                onlinePlayer.value?.loadVideo(mediaItem.mediaId, 0f)
//                            }

                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    if (preferences.getBoolean(keepPlayerMinimizedKey, false))
                                        playerSheetState.collapseSoft()
                                    else playerSheetState.expandSoft()
                                }
                            }

                            setDynamicPalette(
                                mediaItem?.mediaMetadata?.artworkUri.thumbnail(
                                    1200
                                ).toString()
                            )

                            bitmapProvider?.load(mediaItem?.mediaMetadata?.artworkUri) {}

                            updateSelectedQueue()

                        }
                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }


            }

            LaunchedEffect(intentUriData) {
                val uri = intentUriData ?: return@LaunchedEffect

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
                                Timber.e("MainActivity.onCreate intentUriData ${e.stackTraceToString()}")
                            }
                        }

                        "search" -> uri.getQueryParameter("q")?.let { query ->
                            navController.navigate(route = "${NavRoutes.searchResults.name}/$query")
                        }

                        else -> when {
                            path == "watch" -> uri.getQueryParameter("v")
                            uri.host == "youtu.be" -> path
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
                                    //binder?.player?.forcePlay(song.asMediaItem)
                                        fastPlay(song.asMediaItem, binder)
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

    fun initializeMediasession() {
        val currentMediaItem = binder?.player?.currentMediaItem
        if (mediaSession == null)
            mediaSession = MediaSessionCompat(this, "OnlinePlayer")

        mediaSession?.setFlags(0)
        mediaSession?.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    currentMediaItem?.mediaMetadata?.title.toString()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentMediaItem?.mediaMetadata?.artist.toString()
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentPlaybackDuration.value)
                .build()
        )
        mediaSession?.setPlaybackState(
            stateBuilder
                .setState(
                    if (onlinePlayerPlayingState.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    currentPlaybackPosition.value,
                    1f
                )
                .build()
        )

        binder?.let {
            mediaSession?.setCallback(
                MediaSessionCallback(
                    it,
                    {
                        println("OnlinePlayer callback play")
                        onlinePlayer.value?.play()
                    },
                    {
                        println("OnlinePlayer callback pause")
                        onlinePlayer.value?.pause()
                    },
                    { second ->
                        val newPosition = (second / 1000).toFloat()
                        println("OnlinePlayer callback seekTo ${newPosition}")
                        onlinePlayer.value?.seekTo(newPosition)
                        currentPlaybackPosition.value = second
                    }
                )
            )
        }


        mediaSession?.setPlaybackState(stateBuilder.build())
        mediaSession?.isActive = true
    }

    fun initializeBitmapProvider() {
        runCatching {
            bitmapProvider = BitmapProvider(
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("Failed init bitmap provider in PlayerService ${it.stackTraceToString()}")
        }
    }

    fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(NOTIFICATION_CHANNEL)
            .setShowBadge(false)
            .build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    @UnstableApi
    fun updateOnlineNotification() {
        val currentMediaItem = binder?.player?.currentMediaItem
        if (currentMediaItem?.isLocal == true) return

        initializeMediasession()

        createNotificationChannel()



        // todo Improve custom actions in online player notification
//        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
//        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)
//
//        val firstActionButton = NotificationButtons.entries
//            .filter { it == notificationPlayerFirstIcon }
//            .map {
//                NotificationCompat.Action.Builder(
//                    it.getStateIcon(
//                        it,
//                        0,
//                        binder?.player?.repeatMode ?: 0,
//                        binder?.player?.shuffleModeEnabled ?: false
//                    ),
//                    it.name,
//                    it.pendingIntent
//                ).build()
//            }
//

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

        val notification = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle(currentMediaItem?.mediaMetadata?.title)
            .setContentText(currentMediaItem?.mediaMetadata?.artist)
            .setSubText(currentMediaItem?.mediaMetadata?.artist)
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(bitmapProvider?.bitmap)
            .setSilent(true)
            .setColorized(false)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(forwardAction)
            //.addAction(firstActionButton as NotificationCompat.Action?)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(false)
                    .setMediaSession(mediaSession?.sessionToken)

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
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    @JvmInline
    value class Action(val value: String) {
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                context(),
                100,
                Intent(context(), OnlinePlayerNotificationActionReceiver::class.java).setAction(
                    value
                ),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {

            val pause = Action("it.fast4x.riplay.onlineplayer.pause")
            val play = Action("it.fast4x.riplay.onlineplayer.play")
            val next = Action("it.fast4x.riplay.onlineplayer.next")
            val previous = Action("it.fast4x.riplay.onlineplayer.previous")

        }
    }

    inner class OnlinePlayerNotificationActionReceiver() : BroadcastReceiver() {

        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            println("OnlinePlayerNotificationActionReceiver onReceive intent.action: ${intent.action}")
            when (intent.action) {
                Action.pause.value -> onlinePlayer.value?.pause()
                Action.play.value -> onlinePlayer.value?.play()
                Action.next.value -> binder?.player?.playNext()
                Action.previous.value -> binder?.player?.playPrevious()
            }
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
        println("MainActivity.onResume $appRunningInBackground")
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            sensorListener.let { sensorManager?.unregisterListener(it) }
        }.onFailure {
            Timber.e("MainActivity.onPause unregisterListener sensorListener ${it.stackTraceToString()}")
        }
        appRunningInBackground = true

        println("MainActivity.onPause $appRunningInBackground")
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

val LocalPlayerServiceBinder = staticCompositionLocalOf<OfflinePlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { TODO() }

@OptIn(ExperimentalMaterial3Api::class)
val LocalPlayerSheetState =
    staticCompositionLocalOf<PlayerSheetState> { error("No player sheet state provided") }

val LocalOnlinePlayerPlayingState =
    staticCompositionLocalOf<Boolean> { error("No player sheet state provided") }
val LocalLinkDevices =
    staticCompositionLocalOf<List<NsdServiceInfo>> { error("No link devices provided") }

val LocalSelectedQueue = staticCompositionLocalOf<Queues?> { error("No selected queue provided") }

val LocalBackupHandler = staticCompositionLocalOf<RoomBackup> { error("No backup handler provided") }

