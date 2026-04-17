package it.fast4x.riplay.services.playback

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media.VolumeProviderCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import it.fast4x.androidyoutubeplayer.core.player.PlayerConstants
import it.fast4x.androidyoutubeplayer.core.player.YouTubePlayer
import it.fast4x.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import it.fast4x.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import it.fast4x.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.environment.Environment
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.from
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.forceSeekToNext
import it.fast4x.riplay.utils.forceSeekToPrevious
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isAtLeastAndroid13
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.utils.startFadeAnimator
import it.fast4x.riplay.commonutils.thumbnail
import it.fast4x.riplay.utils.timer
import it.fast4x.riplay.R
import it.fast4x.riplay.cast.CastHelper
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.extensions.audiovolume.AudioVolumeObserver
import it.fast4x.riplay.extensions.audiovolume.OnAudioVolumeChangedListener
import it.fast4x.riplay.extensions.discord.DiscordPresenceManager
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOfflinePlayer
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOnlinePlayer
import it.fast4x.riplay.extensions.history.updateOnlineHistory
import it.fast4x.riplay.extensions.preferences.audioReverbPresetKey
import it.fast4x.riplay.extensions.preferences.autoLoadSongsInQueueKey
import it.fast4x.riplay.extensions.preferences.bassboostEnabledKey
import it.fast4x.riplay.extensions.preferences.bassboostLevelKey
import it.fast4x.riplay.extensions.preferences.closePlayerServiceAfterMinutesKey
import it.fast4x.riplay.extensions.preferences.closePlayerServiceWhenPausedAfterMinutesKey
import it.fast4x.riplay.extensions.preferences.discordPersonalAccessTokenKey
import it.fast4x.riplay.extensions.preferences.discoverKey
import it.fast4x.riplay.extensions.preferences.exoPlayerMinTimeForEventKey
import it.fast4x.riplay.extensions.preferences.filterContentTypeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.isDiscordPresenceEnabledKey
import it.fast4x.riplay.extensions.preferences.isPauseOnVolumeZeroEnabledKey
import it.fast4x.riplay.extensions.preferences.isShowingThumbnailInLockscreenKey
import it.fast4x.riplay.extensions.preferences.loudnessBaseGainKey
import it.fast4x.riplay.extensions.preferences.minimumSilenceDurationKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerFirstIconKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerSecondIconKey
import it.fast4x.riplay.extensions.preferences.pauseListenHistoryKey
import it.fast4x.riplay.extensions.preferences.persistentQueueKey
import it.fast4x.riplay.extensions.preferences.playbackDurationKey
import it.fast4x.riplay.extensions.preferences.playbackFadeAudioDurationKey
import it.fast4x.riplay.extensions.preferences.playbackPitchKey
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.putEnum
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.resumeOrPausePlaybackWhenDeviceKey
import it.fast4x.riplay.extensions.preferences.resumePlaybackOnStartKey
import it.fast4x.riplay.extensions.preferences.shakeEventEnabledKey
import it.fast4x.riplay.extensions.preferences.skipSilenceKey
import it.fast4x.riplay.extensions.preferences.useVolumeKeysToChangeSongKey
import it.fast4x.riplay.extensions.preferences.volumeBoostLevelKey
import it.fast4x.riplay.extensions.preferences.volumeNormalizationKey
import it.fast4x.riplay.ui.screens.player.unified.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget
import it.fast4x.riplay.ui.widgets.PlayerVerticalWidget
import it.fast4x.riplay.utils.BitmapProvider
import it.fast4x.riplay.utils.OnlineRadio
import it.fast4x.riplay.utils.SleepTimerListener
import it.fast4x.riplay.utils.TimerJob
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.clearWebViewData
import it.fast4x.riplay.utils.collect
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.isHandleAudioFocusEnabled
import it.fast4x.riplay.utils.isKeepScreenOnEnabled
import it.fast4x.riplay.utils.isOfficialContent
import it.fast4x.riplay.utils.isSkipMediaOnErrorEnabled
import it.fast4x.riplay.utils.isUserGeneratedContent
import it.fast4x.riplay.utils.principalCache
import it.fast4x.riplay.utils.seamlessQueue
import it.fast4x.riplay.commonutils.setLikeState
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.extensions.encryptedpreferences.encryptedPreferences
import it.fast4x.riplay.extensions.lastfm.sendNowPlaying
import it.fast4x.riplay.extensions.lastfm.sendScrobble
import it.fast4x.riplay.extensions.players.getOnlineMetadata
import it.fast4x.riplay.extensions.preferences.disableAudioDRCKey
import it.fast4x.riplay.extensions.preferences.enableWallpaperKey
import it.fast4x.riplay.extensions.preferences.excludeSongIfIsVideoKey
import it.fast4x.riplay.extensions.preferences.isEnabledLastfmKey
import it.fast4x.riplay.extensions.preferences.lastfmScrobbleTypeKey
import it.fast4x.riplay.extensions.preferences.lastfmSessionTokenKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.timerEndTimeKey
import it.fast4x.riplay.extensions.preferences.wallpaperTypeKey
import it.fast4x.riplay.cast.ritune.RiTuneCastClient
import it.fast4x.riplay.cast.ritune.models.RiTuneConnectionStatus
import it.fast4x.riplay.cast.ritune.models.RiTunePlayerState
import it.fast4x.riplay.cast.ritune.models.RiTuneRemoteCommand
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.extensions.preferences.castTypeKey
import it.fast4x.riplay.extensions.preferences.stateDurationKey
import it.fast4x.riplay.extensions.preferences.stateMediaIdKey
import it.fast4x.riplay.services.helpers.AudioDRCHelper
import it.fast4x.riplay.services.helpers.BluetoothConnectHelper
import it.fast4x.riplay.services.helpers.EqualizerHelper
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.isAtLeastAndroid11
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isPersistentQueueEnabled
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.playAtIndex
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.setQueueLoopState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Objects
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import android.os.Binder as AndroidBinder


@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : Service(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnAudioVolumeChangedListener
{
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var unifiedMediaSession: MediaSessionCompat
    val cache: SimpleCache by lazy {
        principalCache.getInstance(this)
    }
    lateinit var player: ExoPlayer
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    //private lateinit var connectivityManager: ConnectivityManager

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    private val metadataBuilder = MediaMetadataCompat.Builder()

    private var notificationManager: NotificationManager? = null

    private var timerJob: TimerJob? = null

    private var radio: OnlineRadio? = null

    var bitmapProvider: BitmapProvider? = null

    private var volumeNormalizationJob: Job? = null
    private var endedObserverJob: Job? = null

    private var isPersistentQueueEnabled = false
    private var isResumePlaybackOnStart = false

    //private var isclosebackgroundPlayerEnabled = false
    private var closeServiceAfterMinutes by mutableStateOf(DurationInMinutes.Disabled)
    private var closeServiceWhenPlayerPausedAfterMinutes by mutableStateOf(DurationInMinutes.Disabled)

    private var isShowingThumbnailInLockscreen = true
    private var medleyDuration by mutableFloatStateOf(0f)

    private lateinit var audioManager: AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

    var legacyActionReceiver: LegacyActionReceiver? = null

    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    var currentMediaItemState = MutableStateFlow<MediaItem?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSong = currentMediaItemState.flatMapLatest { mediaItem ->
        Database.song(mediaItem?.mediaId)
    }.stateIn(serviceScope, SharingStarted.Lazily, null)

    lateinit var sleepTimerListener: SleepTimerListener

    /**
     * Online configuration
     */

    private val _internalOnlinePlayerView = MutableStateFlow<YouTubePlayerView>(
        LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false)
                as YouTubePlayerView
    )
    val internalOnlinePlayerView: StateFlow<YouTubePlayerView?> = _internalOnlinePlayerView

    private val _internalOnlinePlayer = MutableStateFlow<YouTubePlayer?>(null)
    val internalOnlinePlayer: StateFlow<YouTubePlayer?> = _internalOnlinePlayer

    private val _internalBufferedFraction = MutableStateFlow(0f)
    val internalBufferedFraction: StateFlow<Float> = _internalBufferedFraction

    private var _currentSecond = MutableStateFlow(0f)
    var currentSecond: StateFlow<Float> = _currentSecond

    private var _currentDuration = MutableStateFlow(0f)
    var currentDuration: StateFlow<Float> = _currentDuration

    var load = true
    var playFromSecond by mutableFloatStateOf(0f)
    var lastError: PlayerConstants.PlayerError? = null
    var isPlayingNow by mutableStateOf(false)
    var localMediaItem: MediaItem? = null
    var closingTimerStarted: Boolean? = false

    private var onlineListenedDurationMs = 0L
    private var lastOnlineMediaId: String? = null
    private var whatchDogVolume = 0L

    private var lastPlayNextTime = 0L
    private var debounceDelayMs = 2000L

    /**
     * end online configuration
     */

    private var bassBoost: BassBoost? = null
    private var reverbPreset: PresetReverb? = null

    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeCounter = 0

    private var discordPresenceManager: DiscordPresenceManager? = null

    private var currentQueuePosition: Int = 0

    private var minTimeForEvent: MinTimeForEvent = MinTimeForEvent.`20s`

    private var lastMediaIdInHistory: String = ""

    var excludeIfIsVideoEnabled by mutableStateOf(false)

    var parentalControlEnabled by mutableStateOf(false)

    var firstTimeStarted by mutableStateOf(true)

    private var bluetoothReceiver: BluetoothConnectHelper? = null

    private val riTuneCastClient: RiTuneCastClient = RiTuneCastClient()
    private var riTuneObserverJob: Job? = null
    private var riTunePlayerState: RiTunePlayerState? = null

    private lateinit var equalizerHelper: EqualizerHelper

//    private val globalQueue: GlobalQueueViewModel by lazy {
//        ViewModelProvider(AppSharedScope)[GlobalQueueViewModel::class.java]
//    }

    private var unstartedWatchdogJob: Job? = null

    /*
    private var telephonyManager: TelephonyManager? = null
    private var wasPlayingBeforeCall = false
    // Listener per Android 12 e superiori (Nuova API)
    private val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state)
            }
        }
    } else null
    // Listener per Android 11 e inferiori (Vecchia API, non deprecata per loro)
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallStateChange(state)
        }
    }

     */
    //private var checkVolumeLevel: Boolean = true


    override fun onBind(intent: Intent?): AndroidBinder {
        return binder
    }

    @ExperimentalSerializationApi
    @ExperimentalCoroutinesApi
    @FlowPreview
    @SuppressLint("Range")
    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(loading = true)

        //connectivityManager = getSystemService()

        // INITIALIZATION
        preferences.registerOnSharedPreferenceChangeListener(this)

        initializeLocalPlayer()
        initializeVariables()
        initializeOnlinePlayer()
        initializeUnifiedMediaSession()

        startForeground()

        checkAndRestoreTimer()

        initializeBitmapProvider()
        initializeAudioManager()
        initializeAudioVolumeObserver()
        initializeAudioEqualizer()
        initializeLegacyNotificationActionReceiver()

        initializeBluetoothConnect()
        initializeNormalizeVolume()
        initializeBassBoost()
        initializeReverb()
        initializeSensorListener()
        initializeSongCoverInLockScreen()
        initializeMedleyMode()
        initializePlaybackParameters()
        initializeAudioDRCHelper()

        //initializeTelephonyManager(true)

        initializeRiTune()
        initializeDiscordPresence()

        // INITIALIZATION

        if (isPersistentQueueEnabled) {

            serviceScope.launch {

                withContext(Dispatchers.Main) {
                    loadQueue()
                    resumePlaybackOnStart()
                }

                while (isActive) {
                    delay(60.seconds)
                    if (!_playerState.value.isPlaying) {
                        saveQueue()
                        Timber.d("PlayerService saveQueue periodic when not playing")
                    }

                    if (_currentSecond.value >= minTimeForEvent.seconds && lastMediaIdInHistory != currentSong.value?.id) {
                        currentSong.value?.let {
                            updateOnlineHistory(it.asMediaItem)
                            lastMediaIdInHistory = it.id
                        }
                    }

                }
            }

            serviceScope.launch {
                while (isActive) {
                    delay(10.seconds)
                    if (_playerState.value.isPlaying) {
                        saveQueue()
                        Timber.d("PlayerService saveQueue when playing")
                    }
                }
            }

        }

        currentSong.debounce(1000).collect(serviceScope) { song ->
            if (song == null) return@collect

            Timber.d("PlayerService onCreate update currentSong $song mediaItemState ${currentMediaItemState.value}")

            withContext(Dispatchers.Main) {
                updateUnifiedMediasession()
                updateUnifiedNotification()
            }

            val currentMediaId = if (!song.isLocal) song.id else song.mediaId.toString()

            if (lastOnlineMediaId != currentMediaId) {
                if (onlineListenedDurationMs > 0) incrementOnlineListenedPlaytimeMs()
                delay(200)
                onlineListenedDurationMs = 0L
                lastOnlineMediaId = currentMediaId
            }



            val format = Database.format(currentMediaId.toString()).first()
            Timber.d("PlayerService onCreate update currentSong $currentMediaId format $format")
            if (format == null) {
                getOnlineMetadata(currentMediaId)
                    ?.let {
                        Timber.d("PlayerService onCreate update currentSong onlinemetadata it $it")
                        try {
                            Database.insert(
                                Format(
                                    songId = currentMediaId,
                                    contentLength = it.videoDetails?.lengthSeconds?.toLong(),
                                    loudnessDb = it.playerConfig?.audioConfig?.loudnessDb
                                        ?: it.playerConfig?.audioConfig?.perceptualLoudnessDb?.toFloat(),
                                    playbackUrl = it.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                                )
                            )
                        } catch (e: Exception) {
                            Timber.e("PlayerService onCreate update currentSong exception ${e.stackTraceToString()}")
                        }

                    }
            }

            withContext(Dispatchers.Main) {
                val currentState = _playerState.value
                _playerState.value = currentState.copy(
                    mediaInfo = MediaInfo(
                        mediaItem = song.asMediaItem,
                        queueIndex = player.currentMediaItemIndex,
                        queueSize = player.mediaItems.size
                    ),
                    errorMessage = null,
                )
            }
        }


        //todo in the future
        //globalQueue.linkController(binder)

        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (localMediaItem?.isLocal == false) {
                    if (_playerState.value.isPlaying) {
                        onlineListenedDurationMs += 1000
                    } else {
                        if (onlineListenedDurationMs > 0) {
                            incrementOnlineListenedPlaytimeMs()
                            delay(200)
                            onlineListenedDurationMs = 0L
                        }
                    }
                    //fallback if online player not fire state ended
                    if (_currentDuration.value > 0 && preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default) == QueueLoopType.Default) {
                        if (_currentSecond.value >= _currentDuration.value - 0.5f) {
                            if (_playerState.value.isPlaying) {
                                Timber.d("PlayerService Watchdog: End of online track detected by time, forcing playNext()")
                                handlePlayNext()
                            }
                        }
                    }
                    //Timber.d("PlayerService onCreate onlineListenedDurationMs $onlineListenedDurationMs")

                    //Workaround to fix volume bug in webview in some devices. Same for youtube music app
                    // todo maybe not works
                    whatchDogVolume += 1
                    if (whatchDogVolume > 2) {
                        withContext(Dispatchers.Main) {
                            _internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())
                        }
                        whatchDogVolume = 0
                        //Timber.d("PlayerService onCreate whatchDogVolume fired")
                    }
                }
                delay(1000)
            }
        }

        updateWidgets()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()

        return START_STICKY
    }

    private fun startForeground(loading: Boolean = false) {

            val notification = if (loading) {
                NotificationCompat
                    .Builder(this@PlayerService, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(resources.getString(R.string.loading_please_wait))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .build()
            } else {
                notification()
            }

        //startForeground(NOTIFICATION_ID,notification())

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (isAtLeastAndroid11) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )

    }

    private fun initializeVariables() {

        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, true)
        isResumePlaybackOnStart = preferences.getBoolean(resumePlaybackOnStartKey, false)
        isShowingThumbnailInLockscreen =
            preferences.getBoolean(isShowingThumbnailInLockscreenKey, false)
        medleyDuration = preferences.getFloat(playbackDurationKey, 0f)

        _internalOnlinePlayerView.value = LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false) as YouTubePlayerView

        currentMediaItemState.value = player.currentMediaItem

        //isclosebackgroundPlayerEnabled = preferences.getBoolean(closebackgroundPlayerKey, false)
        closeServiceAfterMinutes =
            preferences.getEnum(closePlayerServiceAfterMinutesKey, DurationInMinutes.Disabled)
        closeServiceWhenPlayerPausedAfterMinutes = preferences.getEnum(
            closePlayerServiceWhenPausedAfterMinutesKey, DurationInMinutes.Disabled
        )
    }

    private fun initializePlaybackParameters() {
        when (localMediaItem?.isLocal) {
            false -> {
                val playbackSpeed = preferences.getFloat(playbackSpeedKey, 1f)
                val onlinePlabackRate = when {
                    (playbackSpeed.toDouble() in 0.0..0.25) -> PlayerConstants.PlaybackRate.RATE_0_25
                    (playbackSpeed.toDouble() in 0.26..0.5) -> PlayerConstants.PlaybackRate.RATE_0_5
                    (playbackSpeed.toDouble() in 0.51..0.75) -> PlayerConstants.PlaybackRate.RATE_0_75
                    (playbackSpeed.toDouble() in 0.76..1.0) -> PlayerConstants.PlaybackRate.RATE_1
                    (playbackSpeed.toDouble() in 1.01..1.25) -> PlayerConstants.PlaybackRate.RATE_1_25
                    (playbackSpeed.toDouble() in 1.26..1.5) -> PlayerConstants.PlaybackRate.RATE_1_5
                    (playbackSpeed.toDouble() in 1.51..1.75) -> PlayerConstants.PlaybackRate.RATE_1_75
                    (playbackSpeed.toDouble() > 1.76) -> PlayerConstants.PlaybackRate.RATE_2
                    else -> PlayerConstants.PlaybackRate.RATE_1
                }
                _internalOnlinePlayer.value?.setPlaybackRate(onlinePlabackRate)
            }

            else -> {
                player.playbackParameters = PlaybackParameters(
                    preferences.getFloat(playbackSpeedKey, 1f),
                    preferences.getFloat(playbackPitchKey, 1f)
                )
            }
        }

    }

    /*
    private fun handleCallStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Sta squillando o sei in chiamata
                Timber.d("PhoneState: Chiamata in corso/Ricevuta -> Pause")
                wasPlayingBeforeCall = isPlayingNow || player.isPlaying
                if (wasPlayingBeforeCall) {
                    pausePlayback()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Chiamata terminata
                Timber.d("PhoneState: Chiamata terminata -> Resume")
                if (wasPlayingBeforeCall) {
                    // Ritardo per sicurezza
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(500) // Ritardo di 500ms
                        playPlayback()
                    }
                }
                wasPlayingBeforeCall = false
            }
        }
    }


    private fun pausePlayback() {
        if (localMediaItem?.isLocal == true)
            player.pause()
        else
            _internalOnlinePlayer.value?.pause()
    }

    private fun playPlayback() {
        if (localMediaItem?.isLocal == true)
            player.play()
        else
            _internalOnlinePlayer.value?.play()
    }

     */

    private fun initializeMedleyMode() {
        serviceScope.launch {
            while (medleyDuration > 0) {
                withContext(Dispatchers.Main) {
                    Timber.d("PlayerService initializeMedleyMode medleyDuration $medleyDuration player.isPlaying ${player.isPlaying} internalOnlinePlayerState ${_playerState.value.isPlaying}")
                    val seconds =
                        if (localMediaItem?.isLocal == true) player.currentPosition.div(1000)
                            .toInt() else _currentSecond.value.toInt()
                    if (medleyDuration.toInt() <= seconds) {
                        handlePlayNext()
                    }
                }
            }
        }
    }

    private fun initializeRiTune() {

        riTuneObserverJob?.cancel()

        val isRiTuneEnabled = preferences.getEnum(castTypeKey, CastType.RITUNECAST) == CastType.RITUNECAST
        if (!isRiTuneEnabled) return
        //if (!isRiTuneEnabled || riTuneClient.connectionStatus.value != RiTuneConnectionStatus.Connected) return
        //Timber.d("PlayerService initializeRiTune isRituneEnabled $isRiTuneEnabled")

        var isConnecting = false

        riTuneObserverJob = serviceScope.launch {

            while (isActive) {

                val connectionStatus = riTuneCastClient.connectionStatus.value
                try {
                    withContext(Dispatchers.Main) {
                        GlobalSharedData.riTuneError.value = when (connectionStatus) {
                            is RiTuneConnectionStatus.Error -> connectionStatus.message
                            else -> null
                        }
                        GlobalSharedData.riTuneConnected.value =
                            connectionStatus == RiTuneConnectionStatus.Connected
                    }
                } catch (e: Exception) {
                    Timber.e("PlayerService initializeRiTune LOOP ERROR: $e")
                }
                val isCastActive = GlobalSharedData.riTuneCastActive


                val playerState = riTuneCastClient.state.value?.state
                val duration = riTuneCastClient.state.value?.duration
                val second = riTuneCastClient.state.value?.currentTime

                if (isCastActive) {
                    withContext(Dispatchers.Main) {
                        when (playerState) {
                            PlayerConstants.PlayerState.PLAYING -> startEndedObserver()
                            else -> stopEndedObserver()
                        }

                        playerState?.let { updatePlayerState(it) }

                        if (duration != null) {
                            _currentDuration.value = duration
                        }

                        if (second != null) {
                            _currentSecond.value = second
                        }
                        //Timber.d("PlayerService initializeRiTune Loop - CastActive PlayerState $playerState, duration $duration, second $second")
                    }
                }

                //Timber.d("PlayerService initializeRiTune Loop - CastActive: $isCastActive, Status: $connectionStatus, isConnecting: $isConnecting PlayerState $playerState  ")

                if (!isCastActive) {
                    if (isConnecting) isConnecting = false
                    //Timber.d("PlayerService initializeRiTune CAST NOT ACTIVE - Status: $connectionStatus, isConnecting: $isConnecting")
                    if (connectionStatus == RiTuneConnectionStatus.Connected) {
                        riTuneCastClient.disconnect()
                        withContext(Dispatchers.Main) {
                            player.pause()
                            _internalOnlinePlayer.value?.pause()
                        }
                        updatePlayerState(PlayerConstants.PlayerState.PAUSED)
                        Timber.d("PlayerService initializeRiTune CAST NOT ACTIVE - Disconnected")
                    }

                } else {

                    if (connectionStatus == RiTuneConnectionStatus.Connected) {
                        if (isConnecting) {
                            isConnecting = false
                            withContext(Dispatchers.Main) {
                                player.pause()
                                _internalOnlinePlayer.value?.pause()
                            }

                            Timber.d("PlayerService initializeRiTune Connection established successfully")
                        }

                    } else if (!isConnecting) {

                        Timber.d("PlayerService initializeRiTune CAST ACTIVE - Trying to connect...")

                        val device = GlobalSharedData.riTuneDevices.value.firstOrNull { it.selected }

                        if (device != null) {
                            isConnecting = true
                            launch {
                                try {
                                    riTuneCastClient.startConnection(
                                        device.host.substringAfter("/"),
                                        device.port
                                    )
                                } catch (e: TimeoutCancellationException) {
                                    isConnecting = false
                                    Timber.e("PlayerService initializeRiTune CAST TIMEOUT: $e")
                                } catch (e: Exception) {
                                    isConnecting = false
                                    Timber.e("PlayerService initializeRiTune CAST ERROR: $e")
                                }
                            }

                        } else {
                            Timber.w("PlayerService initializeRiTune NO DEVICE SELECTED!")
                        }
                    } else {
                        Timber.d("PlayerService initializeRiTune Connection already in progress, waiting...")
                    }
                }
                //Timber.d("PlayerService initializeRiTune Loop Tick - Active: $isActive")
                delay(1000)
            }
            Timber.d("PlayerService initializeRiTune: JOB TERMINATO (end of loop)")
        }
    }

    private fun initializeDiscordPresence() {
        if (!isAtLeastAndroid81) return

        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            val token = encryptedPreferences.getString(discordPersonalAccessTokenKey, "")
            //Timber.d("PlayerService initializeDiscordPresence token $token")
            if (token?.isNotEmpty() == true) {
                discordPresenceManager = DiscordPresenceManager(
                    context = this,
                    getToken = { token },
                )
            }
        }
    }

    private fun initializeSensorListener() {
        if (preferences.getBoolean(shakeEventEnabledKey, false)) {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            Objects.requireNonNull(sensorManager)
                ?.registerListener(
                    sensorListener,
                    sensorManager
                        ?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
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
                    handlePlayNext()
                    //player.playNext()
                }

            }

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun resumePlaybackOnStart() {
        if (!isPersistentQueueEnabled && !isResumePlaybackOnStart) return

        when (player.currentMediaItem?.isLocal) {
            true -> {
                if (!player.isPlaying) player.play()
            }

            else -> {}
        }

    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        val currentState = _playerState.value
        val settings = currentState.settings
        _playerState.value = currentState.copy(settings = settings.copy(repeatMode = QueueLoopType.from(repeatMode)))
        updateUnifiedNotification()
    }

    private fun initializeBitmapProvider() {
        runCatching {
            bitmapProvider = BitmapProvider(
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) Color.BLACK else Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("PlayerService Failed init bitmap provider in MainActivity ${it.stackTraceToString()}")
        }
    }

    private fun initializeUnifiedMediaSession() {

        unifiedMediaSession = MediaSessionCompat(this, "PlayerService")

        val repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        unifiedMediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        unifiedMediaSession.setRepeatMode(repeatMode)

        if (preferences.getBoolean(useVolumeKeysToChangeSongKey, false))
            unifiedMediaSession.setPlaybackToRemote(getVolumeProvider())

        initializeUnifiedSessionCallback()

        unifiedMediaSession.isActive = true

    }

    fun recreateOnlinePlayerView() {
        initializeVariables()
        initializeOnlinePlayer()
    }

    private fun initializeLocalPlayer() {
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRendersFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                isHandleAudioFocusEnabled()
            )
            //.setUsePlatformDiagnostics(false)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
//            .setLoadControl(
//                DefaultLoadControl.Builder()
//                    .setBufferDurationsMs(
//                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, // 50000
//                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS, // 50000
//                        5000,
//                        10000
//                    ).build()
//            )
            .build()
            .apply {
                addListener(this@PlayerService)
                sleepTimerListener = SleepTimerListener(serviceScope, this)
                addListener(sleepTimerListener)
                addAnalyticsListener(PlaybackStatsListener(false, this@PlayerService))
            }

        player.repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.pauseAtEndOfMediaItems = true
    }

    private fun initializeOnlinePlayer() {

        val listener = object : AbstractYouTubePlayerListener() {

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)

                _internalOnlinePlayer.value = youTubePlayer

                val customUiController =
                    CustomDefaultPlayerUiController(
                        this@PlayerService,
                        _internalOnlinePlayerView.value,
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
                _internalOnlinePlayerView.value.setCustomPlayerUi(customUiController.rootView)

                Timber.d("PlayerService onlinePlayer onReady localmediaItem ${localMediaItem?.mediaId} queue index ${binder.player.currentMediaItemIndex}")
                Timber.d("PlayerService onlinePlayer onReady isPersistentQueueEnabled $isPersistentQueueEnabled isResumePlaybackOnStart $isResumePlaybackOnStart")

                youTubePlayer.setVolume(getSystemMediaVolume())

                if (localMediaItem?.isLocal == true) return

                localMediaItem?.let{
                    if (isPersistentQueueEnabled && isResumePlaybackOnStart && firstTimeStarted) {
                        youTubePlayer.loadVideo(it.mediaId, playFromSecond)
                        playFromSecond = 0f
                        Timber.d("PlayerService onlinePlayer onReady loadVideo ${it.mediaId}")
                    }
                }

                firstTimeStarted = false

            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                _currentSecond.value = second
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                super.onVideoDuration(youTubePlayer, duration)

                _currentDuration.value = duration

                preferences.edit { putFloat(stateDurationKey, duration) }
                preferences.edit { putString(stateMediaIdKey, localMediaItem?.mediaId) }

                updateUnifiedNotification()
                updateDiscordPresence()
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                if (localMediaItem?.isLocal == true) return
                Timber.d("PlayerService onlinePlayerView: onStateChange $state")

                unstartedWatchdogJob?.cancel()

                updatePlayerState(state)

                when(state) {
                    PlayerConstants.PlayerState.UNSTARTED -> {
                        if (!firstTimeStarted) {
                            unstartedWatchdogJob = CoroutineScope(Dispatchers.Main).launch {
                                Timber.d("PlayerService onlinePlayerView: onStateChange UNSTARTED watchdog")
                                delay(1000)

                                if (_playerState.value.playbackState == PlaybackState.UNSTARTED) {
                                    Timber.e("PlayerService onlinePlayerView: Persistent UNSTARTED state. Probably webView killed. Force to re-initialize.")

                                    recreateOnlinePlayerView()
                                    delay(500)
                                    val currentPlayer = this@PlayerService._internalOnlinePlayer.value

                                    localMediaItem?.let { item ->
                                        if (currentPlayer != null) {
                                            Timber.d("PlayerService onlinePlayerView: Try reload song/video")
                                            currentPlayer.pause()
                                            currentPlayer.cueVideo(item.mediaId, playFromSecond)
                                        } else {
                                            Timber.w("PlayerService onlinePlayerView: Recovery - _internalOnlinePlayer is not defined, impossible to continue")
                                        }
                                    }

                                }
                            }
                        }
                    }

                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                        Timber.d("PlayerService onlinePlayerView: onStateChange VIDEO_CUED regular play()")
                        playFromSecond = 0f
                        if (!firstTimeStarted) {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                                youTubePlayer.unMute()
                                youTubePlayer.setVolume(getSystemMediaVolume())
                                youTubePlayer.play()
                            }
//                                else
//                                    coroutineScope.launch {
//                                        localMediaItem?.let { item ->
//                                            riTuneClient.sendCommand(
//                                                RiTuneRemoteCommand(
//                                                    "load",
//                                                    mediaId = item.mediaId,
//                                                    position = playFromSecond
//                                                )
//                                            )
//                                        }
//                                    }
                        }

                    }
                    PlayerConstants.PlayerState.PLAYING -> {
                        startEndedObserver()
                        sendOpenExternalEqualizerIntent()
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        stopEndedObserver()
                        sendCloseExternalEqualizerIntent()
                    }
//                        PlayerConstants.PlayerState.ENDED -> {
//                            Timber.d("PlayerService onlinePlayerView: onStateChange ENDED regular playNext()")
//                            player.playNext()
//                        }
                    else -> {}
                }

                if (closeServiceWhenPlayerPausedAfterMinutes != DurationInMinutes.Disabled) {
                    if (state != PlayerConstants.PlayerState.PLAYING && closingTimerStarted == false) {
                        Timber.d("PlayerService closingTimer started")
                        binder.startSleepTimer(closeServiceWhenPlayerPausedAfterMinutes.minutesInMilliSeconds)
                        closingTimerStarted = true
                    }
                    if (state == PlayerConstants.PlayerState.PLAYING && closingTimerStarted == true) {
                        Timber.d("PlayerService closingTimer cancelled")
                        binder.cancelSleepTimer()
                        closingTimerStarted = false
                    }
                }

                isPlayingNow = state == PlayerConstants.PlayerState.PLAYING
                updateUnifiedNotification()
                updateDiscordPresence()

            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {

                val currentState = _playerState.value
                _playerState.value = currentState.copy(
                    playbackState = PlaybackState.ERROR
                )

                if (localMediaItem == null || localMediaItem?.isLocal == true) return

                if (isPersistentQueueEnabled)
                    saveQueue()


                if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                    youTubePlayer.pause()
                else
                    serviceScope.launch {
                        riTuneCastClient.sendCommand(
                            RiTuneRemoteCommand(
                                "pause",
                                position = playFromSecond
                            )
                        )
                    }

                clearWebViewData()

                Timber.e("PlayerService: onError $error")
                val errorString = when (error) {
                    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> when (isYtLoggedIn()) {
                        false -> "Sorry, content unavailable, try to login next time"
                        true -> "Sorry, content unavailable"
                    }

                    PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Sorry, content no longer available"
                    PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST -> "Invalid parameters in request"
                    else -> null
                }

                if (errorString != null && lastError != error) {
                    if (error != PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST)
                        SmartMessage(
                            errorString,
                            PopupType.Warning,
                            context = this@PlayerService
                        )

                    //handlePlayNext()

                    //}

                    if (error == PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST)
                        localMediaItem?.let {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                                youTubePlayer.cueVideo(it.mediaId, playFromSecond)
                            }
                            else serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "load",
                                        mediaId = it.mediaId,
                                        position = playFromSecond
                                    )
                                )
                            }
                        }

                    youTubePlayer.setVolume(getSystemMediaVolume())
                    return
                }

                lastError = error

                if (!isSkipMediaOnErrorEnabled()) return
                val prev = binder.player.currentMediaItem ?: return

                handlePlayNext()

                SmartMessage(
                    message = this@PlayerService.getString(
                        R.string.skip_media_on_error_message,
                        cleanPrefix(prev.mediaMetadata.title.toString())
                    ),
                    context = this@PlayerService,
                )

            }

            override fun onVideoLoadedFraction(
                youTubePlayer: YouTubePlayer,
                loadedFraction: Float
            ) {
                _internalBufferedFraction.value = loadedFraction
            }

        }

        //This initilize chromecast if available (available only on full build variant)
        if (CastHelper.isCastAvailable && preferences.getEnum(castTypeKey, CastType.RITUNECAST) !in listOf(CastType.NONE, CastType.RITUNECAST)) {
            serviceScope.launch {
                CastHelper.initChromecastYouTubePlayerContext(this@PlayerService)
                while (isActive) {
                    delay(1.seconds)
                    CastHelper.let {
                        GlobalSharedData.chromecastConnected.value = it.connected.value
                        if (!it.connected.value) {
                            withContext(Dispatchers.Main) {
                                _internalOnlinePlayer.value?.pause()
                            }
                            val currentState = _playerState.value
                            _playerState.value = currentState.copy(
                                playbackState = PlaybackState.PAUSED
                            )
                            return@let
                        }
                        _internalOnlinePlayer.value = it.internalCastOnlinePlayer.value
                        //Timber.d("PlayerService: CastHelper connected ${it.connected.value}")
                        _internalBufferedFraction.value = it.internalBufferedFraction.value
                        _currentSecond.value = it.currentSecond.value
                        _currentDuration.value = it.currentDuration.value
                        updatePlayerState(it.playerState.value)
                    }
                }
            }
            return
        }

        //This initialize the online player view if chromcast isn't connected
        _internalOnlinePlayerView.value.apply {
            enableAutomaticInitialization = false

            enableBackgroundPlayback(true)

            keepScreenOn = isKeepScreenOnEnabled()

            val iFramePlayerOptions = IFramePlayerOptions.Builder(appContext())
                .controls(0)
                .listType("playlist")
                .origin(resources.getString(R.string.env_fqqhBZd0cf))
                .build()



            initialize(listener, iFramePlayerOptions)

        }

    }

    private fun updatePlayerState(state: PlayerConstants.PlayerState) {
        val currentState = _playerState.value
        _playerState.value = when (state) {
            PlayerConstants.PlayerState.PLAYING -> currentState.copy(playbackState = PlaybackState.PLAYING)
            PlayerConstants.PlayerState.UNSTARTED -> currentState.copy(playbackState = PlaybackState.UNSTARTED)
            PlayerConstants.PlayerState.VIDEO_CUED -> currentState.copy(playbackState = PlaybackState.PLAYING)
            PlayerConstants.PlayerState.ENDED -> currentState.copy(playbackState = PlaybackState.ENDED)
            PlayerConstants.PlayerState.BUFFERING -> currentState.copy(playbackState = PlaybackState.BUFFERING)
            PlayerConstants.PlayerState.PAUSED -> currentState.copy(playbackState = PlaybackState.PAUSED)
            PlayerConstants.PlayerState.UNKNOWN -> currentState.copy(playbackState = PlaybackState.IDLE)
        }
    }

    private fun initializeAudioVolumeObserver() {
        audioVolumeObserver = AudioVolumeObserver(this, audioManager)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)
    }

    private fun initializeAudioEqualizer() {
        equalizerHelper = EqualizerHelper(this)
        equalizerHelper.setup(0)
    }

    private fun initializeLegacyNotificationActionReceiver() {

        legacyActionReceiver = LegacyActionReceiver()

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
            this@PlayerService,
            legacyActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun updateDiscordPresence() {
        if (!isAtLeastAndroid81) return

        currentSong.value?.asMediaItem?.let{

            if (!it.isLocal) {
                updateDiscordPresenceWithOnlinePlayer(
                    discordPresenceManager,
                    it,
                    _playerState.value.isPlaying,
                    _currentDuration.value,
                    _currentSecond.value
                )
            } else {
                updateDiscordPresenceWithOfflinePlayer(
                    discordPresenceManager,
                    binder
                )
            }
        }


    }

    private fun getVolumeProvider(): VolumeProviderCompat {

        val STREAM_TYPE = AudioManager.STREAM_MUSIC
        val currentVolume = audioManager.getStreamVolume(STREAM_TYPE)
        val maxVolume = audioManager.getStreamMaxVolume(STREAM_TYPE)
        val VOLUME_UP = 1
        val VOLUME_DOWN = -1

        return object :
            VolumeProviderCompat(VOLUME_CONTROL_RELATIVE, maxVolume, currentVolume) {

                override fun onAdjustVolume(direction: Int) {
                        val useVolumeKeysToChangeSong = preferences.getBoolean(useVolumeKeysToChangeSongKey, false)
                        // Up = 1, Down = -1, Release = 0
                        if (direction == VOLUME_UP) {
                            if (binder.player.isPlaying && useVolumeKeysToChangeSong) {
                                binder.player.forceSeekToNext()
                            } else {
                                audioManager.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                setCurrentVolume(audioManager.getStreamVolume(STREAM_TYPE))
                            }
                        } else if (direction == VOLUME_DOWN) {
                            if (binder.player.isPlaying && useVolumeKeysToChangeSong) {
                                binder.player.forceSeekToPrevious()
                            } else {
                                audioManager.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                setCurrentVolume(audioManager.getStreamVolume(STREAM_TYPE))
                            }
                        }
                }

        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled) {
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.shuffleOrder = DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis())
        }
        updateUnifiedNotification()
        saveQueue()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("PlayerService onTaskRemoved closeServiceAfterMinutes $closeServiceAfterMinutes")
        if (closeServiceAfterMinutes != DurationInMinutes.Disabled) {
            binder.startSleepTimer(closeServiceAfterMinutes.minutesInMilliSeconds)
        }
    }

    @UnstableApi
    override fun onDestroy() {
        Timber.d("PlayerService onDestroy")

        sendCloseExternalEqualizerIntent()

        serviceScope.launch {
            withContext(Dispatchers.Main) {
                saveQueue()
            }
        }

        //initializeTelephonyManager(false)

        serviceScope.cancel()

        try {
            unregisterReceiver(legacyActionReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver ${e.message}")
        }

        if (this::unifiedMediaSession.isInitialized) {
            unifiedMediaSession.isActive = false
            unifiedMediaSession.release()
        }

        if(this::equalizerHelper.isInitialized) {
            equalizerHelper.release()
        }

        try {
            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    player.removeListener(this@PlayerService)
                    player.release()
                }
            }
        } catch (e: Exception) {
            Timber.e("PlayerService Error in local player release: ${e.message}")
        }

        try {

            _internalOnlinePlayer.value = null

            _internalOnlinePlayerView.value.release()
        } catch (e: Exception) {
            Timber.e("PlayerService Error in online player release: ${e.message}")
        }


        runCatching {

            preferences.unregisterOnSharedPreferenceChangeListener(this)

            cache.release()
            loudnessEnhancer?.release()
            audioVolumeObserver.unregister()
            bluetoothReceiver?.unregister()
            discordPresenceManager?.onStop()

            endedObserverJob?.cancel()
            endedObserverJob = null
            riTuneObserverJob?.cancel()
            riTuneObserverJob = null
            timerJob?.cancel()
            timerJob = null
            unstartedWatchdogJob?.cancel()
            unstartedWatchdogJob = null
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = null

            AudioDRCHelper.restoreDRC()

            notificationManager?.cancelAll()
            //coroutineScope.launch { delay(500) }



        }.onFailure {
            Timber.e("Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }

        super.onDestroy()
    }

    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (preferences.getBoolean(isPauseOnVolumeZeroEnabledKey, false)) {
            if ((player.isPlaying || _playerState.value.isPlaying) && currentVolume < 1) {
                if (player.currentMediaItem?.isLocal == true) {
                    binder.callPause {}
                } else {
                    _internalOnlinePlayer.value?.pause()
                }
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                if (player.currentMediaItem?.isLocal == true) {
                    binder.player.play()
                } else {
                    _internalOnlinePlayer.value?.play()
                }
                pausedByZeroVolume = false
            }
        }

        if (localMediaItem?.isLocal == false) {
            val onlineVolume = getSystemMediaVolume()
            Timber.d("PlayerService onAudioVolumeChanged currentVolume $currentVolume onlineVolume $onlineVolume")
            _internalOnlinePlayer.value?.setVolume(onlineVolume)
        }
    }

    override fun onAudioVolumeDirectionChanged(direction: Int) {
        /*
        if (direction == 0) {
            binder.player.seekToPreviousMediaItem()
        } else {
            binder.player.seekToNextMediaItem()
        }

         */
    }

    @UnstableApi
    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {

        Timber.d("PlayerService onPlaybackStatsReady CALLED eventTime $eventTime playbackStats $playbackStats")

        if (preferences.getBoolean(pauseListenHistoryKey, false)) return

        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (!mediaItem.isLocal) return

        Timber.d("PlayerService onPlaybackStatsReady PROCESS eventTime $eventTime playbackStats $playbackStats")

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            Timber.d("PlayerService onPlaybackStatsReady INCREMENT totalPlayTimeMs $totalPlayTimeMs mediaItem ${mediaItem.mediaId}")
            Database.asyncTransaction {
                Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }


        val minTimeForEvent =
            preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)

        if (totalPlayTimeMs > minTimeForEvent.ms) {
            Timber.d("PlayerService onPlaybackStatsReady INSERT EVENT totalPlayTimeMs $totalPlayTimeMs")
            Database.asyncTransaction {
                try {
                    Database.insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = System.currentTimeMillis(),
                            playTime = totalPlayTimeMs
                        )
                    )
                } catch (e: SQLException) {
                    Timber.e("PlayerService onPlaybackStatsReady SQLException ${e.stackTraceToString()}")
                }
            }

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @UnstableApi
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

        if (mediaItem == null) return

        _currentSecond.value = 0F

//        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
//            Timber.d("PlayerService: MediaItem transition ignored (Reason: Playlist Changed)")
//            return
//        }

        val newMediaId = mediaItem.mediaId

        if (lastOnlineMediaId == newMediaId) {
            Timber.d("PlayerService: Transition ignored, same MediaID ($newMediaId) skipped")

            handlePlayNext()
        }

        startForeground()

        Timber.d("PlayerService onMediaItemTransition mediaItem ${mediaItem.mediaId} reason $reason")

        currentQueuePosition = player.currentMediaItemIndex


        if (parentalControlEnabled && mediaItem.isExplicit) {
            handlePlayNext()
            SmartMessage(resources.getString(R.string.error_message_parental_control_restricted), context = this@PlayerService)
            return
        }

        if (excludeIfIsVideoEnabled && mediaItem.isVideo) {
            handlePlayNext()
            SmartMessage(getString(R.string.warning_skipped_video), context = this@PlayerService)
            return
        }

        var blacklisted by mutableStateOf(false)
        runBlocking(Dispatchers.IO) {
            blacklisted = Database.blacklisted(mediaItem.mediaId) > 0
        }
        if (blacklisted) {
            handlePlayNext()
            SmartMessage(getString(R.string.warning_skipped_blacklisted_song), context = this@PlayerService)
            return
        }

        mediaItem.let {

            currentMediaItemState.value = it

            localMediaItem = it

            if (!it.isLocal){

                if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                    _internalOnlinePlayer.value?.pause()
                    _internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)
                }
                else
                    serviceScope.launch {
                        riTuneCastClient.sendCommand(
                            RiTuneRemoteCommand(
                                "load",
                                mediaId = it.mediaId,
                                position = playFromSecond
                            )
                        )
                    }

                _internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())

            }

            bitmapProvider?.load(it.mediaMetadata.artworkUri) { bitmap ->
                serviceScope.launch {
                    setWallpaper(this@PlayerService, bitmap)
                }
            }
        }


//        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
//            updateMediaSessionQueue(player.currentTimeline)
//        }

        maybeRecoverPlaybackError()
        initializeNormalizeVolume()
        maybeProcessRadio(reason)

        updateUnifiedNotification()

        updateDiscordPresence()

        saveQueue()

        if (preferences.getBoolean(isEnabledLastfmKey, false)) {
            preferences.getString(lastfmSessionTokenKey, "")?.let {
                when (preferences.getEnum(lastfmScrobbleTypeKey, LastFmScrobbleType.Simple)) {
                    LastFmScrobbleType.Simple -> {
                        sendScrobble(
                            mediaItem.mediaMetadata.artist.toString(),
                            cleanPrefix(mediaItem.mediaMetadata.title.toString()),
                            mediaItem.mediaMetadata.albumTitle.toString(),
                            it
                        )
                    }

                    LastFmScrobbleType.NowPlaying -> {
                        sendNowPlaying(
                            mediaItem.mediaMetadata.artist.toString(),
                            cleanPrefix(mediaItem.mediaMetadata.title.toString()),
                            mediaItem.mediaMetadata.albumTitle.toString(),
                            it
                        )
                    }
                }

            }
        }
        Timber.d("PlayerService-onMediaItemTransition mediaItem: ${mediaItem.mediaId} currentMediaItemIndex: $currentQueuePosition shuffleModeEnabled ${player.shuffleModeEnabled} repeatMode ${player.repeatMode} reason $reason")

    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            updateMediaSessionQueue(timeline)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        Timber.d("PlayerService onPlayWhenReadyChanged playWhenReady $playWhenReady reason $reason")
    }

    override fun onTrimMemory(level: Int) {
        val isLowMemory = level == TRIM_MEMORY_RUNNING_CRITICAL
        Timber.d("PlayerService onTrimMemory level $level isLowMemory $isLowMemory")
        if (isLowMemory)
            saveQueue()
    }


    fun updateUnifiedNotification() {
        serviceScope.launch {
            withContext(Dispatchers.Main){
                if (player.mediaItemCount <= 0) return@withContext
                updateUnifiedMediasession()
                val notifyInstance = notification()
                notifyInstance.let {
                    @Suppress("MissingPermission")
                    NotificationManagerCompat
                        .from(this@PlayerService)
                        .notify(NOTIFICATION_ID, it)
                }
            }
        }
    }

        private fun updateMediaSessionQueue(timeline: Timeline) {
        if (!this::unifiedMediaSession.isInitialized) return

        val queueItems = mutableListOf<MediaSessionCompat.QueueItem>()
        val window = Timeline.Window()

        for (i in 0 until timeline.windowCount) {
            timeline.getWindow(i, window)

            val mediaItem = window.mediaItem

            val description = MediaDescriptionCompat.Builder()
                .setMediaId(mediaItem.mediaId)
                .setTitle(cleanPrefix(mediaItem.mediaMetadata.title.toString()))
                .setSubtitle(mediaItem.mediaMetadata.artist)

                .setIconUri(mediaItem.mediaMetadata.artworkUri)
                .build()


            val queueItem = MediaSessionCompat.QueueItem(description, i.toLong())
            queueItems.add(queueItem)
        }

        unifiedMediaSession.setQueue(queueItems)
        unifiedMediaSession.setQueueTitle("Playback Queue")
    }

    private fun maybeRecoverPlaybackError() {
        try {
            if (localMediaItem?.isLocal == true) {
                if (player.playerError != null) {
                    Timber.w("PlayerService maybeRecoverPlaybackError: try to recover player error")
                    player.prepare()

                    if (player.isPlaying) {
                        player.play()
                    }
                }
            } else {
                if (lastError != null) {
                    Timber.w("PlayerService maybeRecoverPlaybackError: try to recover player error")
                    localMediaItem?.let {
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                            _internalOnlinePlayer.value?.pause()
                            _internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)

                            //_internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())
                        } else {
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "load",
                                        mediaId = it.mediaId,
                                        position = playFromSecond
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("PlayerService maybeRecoverPlaybackError: recovery error ${e.stackTraceToString()}")
        }
    }

    private fun maybeProcessRadio(reason: Int) {
        if (!preferences.getBoolean(autoLoadSongsInQueueKey, true)
            || preferences.getEnum(
                queueLoopTypeKey,
                defaultValue = QueueLoopType.Default
            ) == QueueLoopType.RepeatAll
        ) return

        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 10
        ) {
            if (radio == null) {
                binder.setupRadio(
                    NavigationEndpoint.Endpoint.Watch(
                        videoId = player.currentMediaItem?.mediaId
                    )
                )
            } else {
                radio?.let { radio ->
                    serviceScope.launch(Dispatchers.Main) {
                        if (player.playbackState != STATE_IDLE)
                            player.addMediaItems(radio.process())
                    }
                }
            }
        }

    }


    @UnstableApi
    private fun initializeNormalizeVolume() {
        if (!preferences.getBoolean(volumeNormalizationKey, false)) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Timber.e("PlayerService initializeNormalizeVolume Errore durante il release di LoudnessEnhancer: ${e.message}")
        }
        loudnessEnhancer = null
        loudnessEnhancer = LoudnessEnhancer(0)

        val baseGain = preferences.getFloat(loudnessBaseGainKey, 5.00f)
        val boostLevel = preferences.getFloat(volumeBoostLevelKey, 0.00f)

        if (currentSong.value?.isLocal == true && currentSong.value?.mediaId?.isEmpty() == true) return

        volumeNormalizationJob?.cancel()
        volumeNormalizationJob = serviceScope.launch(Dispatchers.Main) {

            fun Float?.toMb() = ((this ?: 0f) * 100).toInt()

            Database.loudnessDb((if(currentSong.value?.isLocal == true)
                currentSong.value?.mediaId else currentSong.value?.id).toString())
                .cancellable().collectLatest { loudnessDb ->
                val loudnessMb = loudnessDb.toMb().let {
                    if (it !in -2000..2000) {
                        withContext(Dispatchers.Main) {
                            SmartMessage("Extreme loudness detected", context = this@PlayerService)
                        }
                        0
                    } else it
                }
                try {
                    loudnessEnhancer?.setTargetGain((baseGain.toMb() + boostLevel.toMb()) - loudnessMb)
                    loudnessEnhancer?.enabled = true
                } catch (e: Exception) {
                    Timber.e("PlayerService maybeNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                }
            }
        }
    }

    private fun initializeAudioDRCHelper() {
       val disable = preferences.getBoolean(disableAudioDRCKey, false)

        AudioDRCHelper.init(this)
        if (disable) AudioDRCHelper.disableDRC()
         else AudioDRCHelper.restoreDRC()
    }

    private fun initializeSongCoverInLockScreen() {
        val bitmap =
            if (isAtLeastAndroid13 || isShowingThumbnailInLockscreen) bitmapProvider?.bitmap else null

        val uri = player.mediaMetadata.artworkUri?.toString()?.thumbnail(512)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, uri)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)

        if (isAtLeastAndroid13 && player.currentMediaItemIndex == 0) {
            metadataBuilder.putText(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                "${cleanPrefix(player.mediaMetadata.title.toString())} "
            )
        }

        unifiedMediaSession.setMetadata(metadataBuilder.build())
    }

    /*
    private fun initializeTelephonyManager(enable: Boolean) {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (enable && (!hasPermission || !preferences.getBoolean(resumeOrPausePlaybackWhenCallKey, false))) return

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (enable) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager?.registerTelephonyCallback(
                    ContextCompat.getMainExecutor(this),
                    telephonyCallback
                )
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
            Timber.d("PlayerService: TelephonyManager registered")

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager?.unregisterTelephonyCallback(telephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
            Timber.w("PlayerService: TelephonyManager unregistered")
        }

    }
     */

    private fun initializeAudioManager() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private fun initializeBluetoothConnect() {
        if (!preferences.getBoolean(resumeOrPausePlaybackWhenDeviceKey, false)) return

        bluetoothReceiver = BluetoothConnectHelper(
            context = this,
            onDeviceConnected = {
                if (currentSong.value?.isLocal == true) {
                    player.play()
                } else {
                    if (_internalOnlinePlayer.value == null)
                        initializeOnlinePlayer()

                    _internalOnlinePlayer.value?.play()
                }
                SmartMessage(getString(R.string.music_resumed_headphones_connected), context = this)
            },
            onDeviceDisconnected = {
                player.pause()
                _internalOnlinePlayer.value?.pause()

                SmartMessage(getString(R.string.music_paused_headphones_disconnected), context = this)
            }

        )
        bluetoothReceiver?.register()

    }

    @UnstableApi
    private fun sendOpenExternalEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                    if (localMediaItem?.isLocal == true) player.audioSessionId
                    else 0
                )
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }


    @UnstableApi
    private fun sendCloseExternalEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                    if (localMediaItem?.isLocal == true) player.audioSessionId
                    else 0
                )
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    private fun updateUnifiedMediasession() {

        val currentMediaItem = binder.player.currentMediaItem
        val currentMediaItemDuration = if (currentMediaItem?.isLocal == false) (_currentDuration.value * 1000).toLong() else player.duration
        val currentMediaItemPosition = if(player.currentMediaItem?.isLocal == false) (_currentSecond.value * 1000).toLong() else player.currentPosition

        unifiedMediaSession.setMetadata(
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
                    cleanPrefix(currentMediaItem?.mediaMetadata?.title.toString())
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentMediaItem?.mediaMetadata?.artist.toString()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                    currentMediaItem?.mediaMetadata?.albumTitle.toString()
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentMediaItemDuration)
                .build()
        )

        val actions =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO

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
                        currentSong.value?.likedAt,
                        player.repeatMode,
                        player.shuffleModeEnabled
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
                        currentSong.value?.likedAt,
                        player.repeatMode,
                        player.shuffleModeEnabled
                    ),
                ).build()
            }.first()


        unifiedMediaSession.setPlaybackState(
            PlaybackStateCompat.Builder().setActions(actions.let {
                if (isAtLeastAndroid12) it or PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED else it
            })
                .apply {
                    addCustomAction(firstCustomAction)
                    addCustomAction(secondCustomAction)
                    setActiveQueueItemId(
                        if (player.currentMediaItemIndex >= 0) player.currentMediaItemIndex.toLong()
                        else MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()
                    )
                    setState(
                        if (_playerState.value.isPlaying)
                            PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        currentMediaItemPosition,
                        1f
                    )
                }
                .build()
        )

        Timber.d("PlayerService updateUnifiedMediasessionData onlineplayer playing ${_playerState.value.isPlaying} currentSecond ${_currentSecond.value} localplayer playing ${player.isPlaying}")
    }

    inner class LegacyActionReceiver() : BroadcastReceiver() {

        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("MainActivity onReceive intent.action: ${intent.action}")
            val currentMediaItem = binder.player.currentMediaItem

            binder.let {
                when (intent.action) {
                    Action.pause.value -> {
                        player.pause()
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                            _internalOnlinePlayer.value?.pause()
                        else
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "pause",
                                        position = playFromSecond
                                    )
                                )
                            }
                    }
                    Action.play.value -> {
                        if (player.currentMediaItem?.isLocal == true)
                            it.player.play()
                        else {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                                _internalOnlinePlayer.value?.play()
                            else
                                serviceScope.launch {
                                    riTuneCastClient.sendCommand(
                                        RiTuneRemoteCommand(
                                            "play",
                                            position = playFromSecond
                                        )
                                    )
                                }
                        }
                    }
                    Action.next.value -> handlePlayNext()
                    Action.previous.value -> player.playPrevious()
                    Action.like.value -> {
                        it.toggleLike()
                    }
                    Action.repeat.value -> {
                        it.toggleRepeat()
                    }
                   Action.shuffle.value -> {
                       it.toggleShuffle()
                    }
                    Action.playradio.value -> {
                        if (currentMediaItem != null) {
                            it.stopRadio()
                            it.player.seamlessQueue(currentMediaItem)

                            if(!GlobalSharedData.riTuneCastActive)
                                _internalOnlinePlayer.value?.play()
                            else
                                serviceScope.launch {
                                    riTuneCastClient.sendCommand(
                                        RiTuneRemoteCommand(
                                            "play",
                                            position = playFromSecond
                                        )
                                    )
                                }

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
            updateUnifiedNotification()
        }

    }


    /*
    @RequiresApi(Build.VERSION_CODES.O)
    @ExperimentalCoroutinesApi
    @FlowPreview
    @Suppress("DEPRECATION")
    override fun onEvents(player: Player, events: Player.Events) {
        if (!events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_IS_LOADING_CHANGED,
                Player.EVENT_MEDIA_METADATA_CHANGED
                //Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED
            )
        ) return

        val notification = notification()

        //if (notification == null) {
        isNotificationStarted = false

        runCatching {
            stopForeground(false)
        }.onFailure {
            Timber.e("PlayerService Failed stopForeground onEvents ${it.stackTraceToString()}")
        }
        sendCloseEqualizerIntent()
        //notificationManager?.cancel(NOTIFICATION_ID)
            //return
        //}

        if ((player.isPlaying || isPlayingNow) && !isNotificationStarted) {
            isNotificationStarted = true
            runCatching {
                if (isAtLeastAndroid8)
                    startForegroundService(intent<PlayerService>())
                else
                    startService(intent<PlayerService>())

                startForeground()
            }.onFailure {
                Timber.e("PlayerServiceFailed startForegroundService onEvents ${it.stackTraceToString()}")
            }

            sendOpenEqualizerIntent()
        } else {
            if (player.isPlaying || isPlayingNow) {
                isNotificationStarted = false
                runCatching {
                    stopForeground(false)
                }.onFailure {
                    Timber.e("PlayerService Failed stopForeground onEvents ${it.stackTraceToString()}")
                }

                sendCloseEqualizerIntent()
            }
            runCatching {
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }.onFailure {
                Timber.e("PlayerServiceFailed onEvents notificationManager.notify ${it.stackTraceToString()}")
            }
        }

    }
    */


    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {

        if (localMediaItem?.isLocal == false) return

        val currentState = _playerState.value
        if (isPlaying) {
            startEndedObserver()
            _playerState.value = currentState.copy(playbackState = PlaybackState.PLAYING)
        }
        else {
            stopEndedObserver()
            _playerState.value = currentState.copy(playbackState = PlaybackState.PAUSED)
        }

        if (closeServiceWhenPlayerPausedAfterMinutes != DurationInMinutes.Disabled) {
            if (!isPlaying && closingTimerStarted == false) {
                Timber.d("PlayerService closingTimer started")
                binder.startSleepTimer(closeServiceWhenPlayerPausedAfterMinutes.minutesInMilliSeconds)
                closingTimerStarted = true
            }
            if (isPlaying && closingTimerStarted == true) {
                Timber.d("PlayerService closingTimer cancelled")
                binder.cancelSleepTimer()
                closingTimerStarted = false
            }
        }

        isPlayingNow = isPlaying
        val fadeDisabled = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled) == DurationInMilliseconds.Disabled
        val duration = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled).milliSeconds
        if (isPlayingNow && !fadeDisabled)
            startFadeAnimator(
                player = binder.player,
                duration = duration,
                fadeIn = true
            )

        if (currentMediaItemState.value?.isLocal == true)
            updateUnifiedNotification()

        //notify external equalizer
        if (!isPlaying) sendCloseExternalEqualizerIntent()
        else sendOpenExternalEqualizerIntent()

        updateDiscordPresence()

        super.onIsPlayingChanged(isPlaying)
    }


    @UnstableApi
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return

        when (key) {
            persistentQueueKey -> {
                isPersistentQueueEnabled = sharedPreferences.getBoolean(key, true)
            }
            resumePlaybackOnStartKey  -> {
                    isResumePlaybackOnStart = sharedPreferences.getBoolean(key, false)
            }
            skipSilenceKey -> {
                player.skipSilenceEnabled = sharedPreferences.getBoolean(key, false)
            }
            excludeSongIfIsVideoKey -> {
                excludeIfIsVideoEnabled = sharedPreferences.getBoolean(key, false)
            }
            parentalControlEnabledKey -> {
                parentalControlEnabled = sharedPreferences.getBoolean(key, false)
            }
            queueLoopTypeKey -> {
                player.repeatMode =
                    sharedPreferences.getEnum(key, QueueLoopType.Default).type
            }
//            closebackgroundPlayerKey -> {
//                    isclosebackgroundPlayerEnabled = sharedPreferences.getBoolean(key, false)
//            }
            closePlayerServiceAfterMinutesKey -> {
                closeServiceAfterMinutes =
                    sharedPreferences.getEnum(key,
                        DurationInMinutes.Disabled)
            }
            closePlayerServiceWhenPausedAfterMinutesKey -> {
                closeServiceWhenPlayerPausedAfterMinutes =
                    sharedPreferences.getEnum(key,
                        DurationInMinutes.Disabled)
            }
            isShowingThumbnailInLockscreenKey -> {
                isShowingThumbnailInLockscreen = sharedPreferences.getBoolean(key, true)
                initializeSongCoverInLockScreen()
            }
            playbackDurationKey -> {
                medleyDuration = sharedPreferences.getFloat(key, 0f)
                initializeMedleyMode()
            }
            exoPlayerMinTimeForEventKey -> {
                minTimeForEvent = sharedPreferences.getEnum(key,
                    MinTimeForEvent.`20s`)
            }
            resumeOrPausePlaybackWhenDeviceKey -> initializeBluetoothConnect()
            bassboostLevelKey, bassboostEnabledKey -> initializeBassBoost()
            audioReverbPresetKey -> initializeReverb()
            volumeNormalizationKey, loudnessBaseGainKey, volumeBoostLevelKey -> initializeNormalizeVolume()
            playbackPitchKey, playbackSpeedKey -> initializePlaybackParameters()
            castTypeKey -> initializeRiTune()
            disableAudioDRCKey -> initializeAudioDRCHelper()
        }
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
            Timber.d("PlayerService processBassBoost bassboostLevel $bassboostLevel")
            bassBoost?.enabled = false
            bassBoost?.setStrength(bassboostLevel)
            bassBoost?.enabled = true
        }.onFailure {
            SmartMessage(
                "Can't enable bass boost",
                context = this@PlayerService
            )
        }
    }

    private fun initializeReverb() {
        val presetType = preferences.getEnum(audioReverbPresetKey, PresetsReverb.NONE)
        Timber.d("PlayerService processReverb presetType $presetType")
        if (presetType == PresetsReverb.NONE) {
            runCatching {
                reverbPreset?.enabled = false
                player.clearAuxEffectInfo()
                reverbPreset?.release()
            }
            reverbPreset = null
            return
        }

        runCatching {
            if (reverbPreset == null) reverbPreset = PresetReverb(1,
                //player.audioSessionId
                0
            )

            reverbPreset?.enabled = false
            reverbPreset?.preset = presetType.preset
            reverbPreset?.enabled = true
            reverbPreset?.id?.let { player.setAuxEffectInfo(AuxEffectInfo(it, 1f)) }
        }
    }


    fun notification(): Notification {

        val currentMediaItem = binder.player.currentMediaItem

        createNotificationChannel()

        val forwardAction = NotificationCompat.Action.Builder(
            R.drawable.play_skip_forward,
            "next",
            Action.next.pendingIntent
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlayingNow || player.isPlaying) R.drawable.pause else R.drawable.play,
            if (isPlayingNow || player.isPlaying) "pause" else "play",
            if (isPlayingNow || player.isPlaying) Action.pause.pendingIntent
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
                        currentSong.value?.likedAt,
                        player.repeatMode,
                        player.shuffleModeEnabled
                    ),
                    it.name,
                    it.pendingIntent,
                ).build()
            }.first()


        val secondCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerSecondIcon }
            .map {
                NotificationCompat.Action.Builder(
                    it.getStateIcon(
                        it,
                        currentSong.value?.likedAt,
                        player.repeatMode,
                        player.shuffleModeEnabled
                    ),
                    it.name,
                    it.pendingIntent,
                ).build()
            }.first()


        val notification = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle(cleanPrefix(currentMediaItem?.mediaMetadata?.title.toString()))
            .setContentText(currentMediaItem?.mediaMetadata?.artist)
            .setContentInfo(currentMediaItem?.mediaMetadata?.albumTitle)
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(bitmapProvider?.bitmap)
            .setShowWhen(false)
            .setSilent(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(firstCustomAction)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(forwardAction)
            .addAction(secondCustomAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3)
                    .setMediaSession(unifiedMediaSession.sessionToken)

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

        return notification

    }

    private fun createNotificationChannel() {
        if (!isAtLeastAndroid8) return

        notificationManager = getSystemService(NotificationManager::class.java)

        notificationManager?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }

            if (getNotificationChannel(SLEEPTIMER_NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        SLEEPTIMER_NOTIFICATION_CHANNEL_ID,
                        SLEEPTIMER_NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }
        }
    }



    private fun createMediaSourceFactory() = DefaultMediaSourceFactory(
        createLocalDataSourceFactory(),
        DefaultExtractorsFactory()
    )

    fun createLocalCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(cache)
            // Remove upstream cause issue with local files
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(
                    this,
                    OkHttpDataSource.Factory(
                        OkHttpClient
                            .Builder()
                            .proxy(Environment.proxy)
                            .build(),
                    ),
                ),
            )

    private fun createRendersFactory() = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            val minimumSilenceDuration = preferences.getLong(
                minimumSilenceDurationKey, 2_000_000L
            ).coerceIn(1000L..2_000_000L)

            return DefaultAudioSink.Builder(applicationContext)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioOffloadSupportProvider(
                    DefaultAudioOffloadSupportProvider(applicationContext)
                )
                .setAudioProcessorChain(
                    DefaultAudioProcessorChain(
                        arrayOf(),
                        SilenceSkippingAudioProcessor(
                            /* minimumSilenceDurationUs = */ minimumSilenceDuration,
                            /* silenceRetentionRatio = */ 0.01f,
                            /* maxSilenceToKeepDurationUs = */ minimumSilenceDuration,
                            /* minVolumeToKeepPercentageWhenMuting = */ 0,
                            /* silenceThresholdLevel = */ 256
                        ),
                        SonicAudioProcessor()
                    )
                )
                .build()
                .apply {
                    if (isAtLeastAndroid10) setOffloadMode(AudioSink.OFFLOAD_MODE_DISABLED)
                }
        }
    }.setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER) // prefer extension renderers to opus format

    fun updateWidgets() {
        val isPlaying = (isPlayingNow || player.isPlaying)
        serviceScope.launch {
            playerVerticalWidget.updateInfo(
                context = this@PlayerService,
                isPlaying = isPlaying,
                bitmap = bitmapProvider?.bitmap,
                binder = binder
            )
            playerHorizontalWidget.updateInfo(
                context = this@PlayerService,
                isPlaying = isPlaying,
                bitmap = bitmapProvider?.bitmap,
                binder = binder
            )
        }
    }

    private fun incrementOnlineListenedPlaytimeMs() {
        if (currentSong.value?.isLocal == true
                || preferences.getBoolean(pauseListenHistoryKey, false)
        ) return

        currentSong.value?.id?.let { mediaId ->
            if (_currentSecond.value > 5) {
                Timber.d("PlayerService incrementOnlineListenedPlaytimeMs INCREMENT totalPlayTimeMs $onlineListenedDurationMs mediaItem ${currentSong.value?.id}")
                Database.asyncTransaction {
                    Database.incrementTotalPlayTimeMs(mediaId, onlineListenedDurationMs)
                }
            }

            val minTimeForEvent =
                preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)

            if (_currentSecond.value > minTimeForEvent.seconds) {
                Timber.d("PlayerService incrementOnlineListenedPlaytimeMs INSERT EVENT totalPlayTimeMs $onlineListenedDurationMs")
                Database.asyncTransaction {
                    try {
                        Database.insert(
                            Event(
                                songId = mediaId,
                                timestamp = System.currentTimeMillis(),
                                playTime = onlineListenedDurationMs
                            )
                        )
                    } catch (e: SQLException) {
                        Timber.e("PlayerService incrementOnlineListenedPlaytimeMs SQLException ${e.stackTraceToString()}")
                    }
                }

            }

        }

    }


    private fun startEndedObserver() {
        endedObserverJob?.cancel()

        endedObserverJob = serviceScope.launch(Dispatchers.Main) {

            var lastProcessedIndex: Int? = null

            while (isActive) {

                val isLocal = player.currentMediaItem?.isLocal == true
                val playbackState = player.playbackState

                if (isLocal)
                    _internalBufferedFraction.value = player.bufferedPosition.toFloat()

                player.pauseAtEndOfMediaItems = !isLocal

                if (!isLocal && (playbackState == Player.STATE_ENDED || _playerState.value.playbackState == PlaybackState.ENDED)
                    && lastProcessedIndex != player.currentMediaItemIndex
                ) {

                    val queueLoopType = preferences.getEnum(
                        queueLoopTypeKey,
                        defaultValue = QueueLoopType.Default
                    )

                    when (queueLoopType) {
                        QueueLoopType.RepeatOne -> {
                            _internalOnlinePlayer.value?.seekTo(0f)
                        }
                        QueueLoopType.Default -> {
                            if (binder.player.hasNextMediaItem()) {
                                lastProcessedIndex = binder.player.currentMediaItemIndex
                                handlePlayNext()
                            }
                        }
                        QueueLoopType.RepeatAll -> {
                            if (!binder.player.hasNextMediaItem()) {
                                binder.player.playAtIndex(0)
                            } else {
                                lastProcessedIndex = player.currentMediaItemIndex
                                handlePlayNext()
                            }
                        }
                    }
                }

                delay(200)
            }
        }
    }

    private fun stopEndedObserver() {
        endedObserverJob?.cancel()
        endedObserverJob = null
    }

    private fun getSystemMediaVolume(): Int {
        return 100 // set to max
//        val maxMediaVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
//        val minVolume = maxMediaVolume.div(3)
//        val volumeOnlinePlayer =  (((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: minVolume) * 100) / maxMediaVolume)
//            .coerceIn(0, 100)
//        return volumeOnlinePlayer
    }

    suspend fun setWallpaper(context: Context, bitmap: Bitmap) {
        val enabled = preferences.getBoolean(enableWallpaperKey, false)
        if (!enabled) return
        val wallpaperTarget = preferences.getEnum(wallpaperTypeKey, WallpaperType.Lockscreen)

        CoroutineScope(Dispatchers.IO).launch {
            val wallpaperManager = WallpaperManager.getInstance(context) ?: return@launch

            try {

                when (wallpaperTarget) {
                    WallpaperType.Home -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    }

                    WallpaperType.Lockscreen -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }

                    WallpaperType.Both -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                }

            } catch (e: Exception) {
                Timber.e("PlayerService setWallpaper error ${e.stackTraceToString()}")
            }
        }
    }

    private fun checkAndRestoreTimer() {
        val savedEndTime = preferences.getLong(timerEndTimeKey, 0)

        if (savedEndTime != 0L) {
            val currentTime = System.currentTimeMillis()
            val remainingMillis = savedEndTime - currentTime

            if (remainingMillis > 0) {
                Timber.d("PlayerService Timer restoration detected. Remaining: $remainingMillis ms")

                timerJob = serviceScope.timer(remainingMillis) {
                    binder.executeStopServiceLogic()
                }
            } else {
                Timber.d("PlayerService Timer expired while service was dead. Stopping now.")
                binder.executeStopServiceLogic()
            }
        }
    }


    fun saveQueue() {
        if (!isPersistentQueueEnabled()) return

        CoroutineScope(Dispatchers.Main).launch {
            val mediaItems = player.currentTimeline.mediaItems
            val mediaItemIndex = player.currentMediaItemIndex
            val mediaItemPosition = if (player.currentMediaItem?.isLocal == true) player.currentPosition else (currentSecond.value * 1000).toLong()

            //Timber.d("SaveQueue savePersistentQueue mediaItems ${mediaItems.size} mediaItemIndex $mediaItemIndex mediaItemPosition $mediaItemPosition")

            if (mediaItems.isEmpty()) return@launch

            withContext(Dispatchers.IO) {

                mediaItems.mapIndexed { index, mediaItem ->
                    QueuedMediaItem(
                        mediaItem = mediaItem,
                        mediaId = mediaItem.mediaId,
                        position = if (index == mediaItemIndex) mediaItemPosition else -1,
                        idQueue = mediaItem.mediaMetadata.extras?.getLong("idQueue", defaultQueueId())
                    )
                }.let { queuedMediaItems ->
                    if (queuedMediaItems.isEmpty()) return@let

                    try {
                        Database.asyncTransaction {
                            clearQueuedMediaItems()
                            //insert(queuedMediaItems)
                            //Timber.d("SaveMasterQueue QueuePersistentEnabled Saved mediaItems ${queuedMediaItems.size}")
                            queuedMediaItems.forEach {
                                insert(it)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("SaveQueue QueuePersistentEnabled Error: ${e.message}")
                    }

                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun loadQueue() {
        Timber.d("LoadQueue loadPersistentQueue is enabled, called")
        if (!isPersistentQueueEnabled()) return

        Database.asyncQuery {
            clearOldEmptyQueuedMediaItems()
            val queuedSongs = try { queuedMediaItems() } catch (e: Exception) { emptyList() }

            if (queuedSongs.isEmpty()) return@asyncQuery

            val index = queuedSongs.indexOfFirst { (it.position ?: 0L) >= 0L }.coerceAtLeast(0)
            val queuedSong = queuedSongs[index]
            val position = if (queuedSong.mediaItem.isLocal) {
                queuedSong.position ?: C.TIME_UNSET
            } else {
                (queuedSong.position ?: 0L) / 1000
            }

            Timber.d("LoadQueue loadPersistentQueue is enabled, processing, restored index: $index isLocal ${queuedSong.mediaItem.isLocal} and mediaItemPosition: $position")

            runBlocking(Dispatchers.Main) {
                player.setMediaItems(
                    queuedSongs.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                                mediaMetadata.extras?.putLong("idQueue", mediaItem.idQueue ?: defaultQueueId())
                            }
                    },
                    index,
                    if (queuedSong.mediaItem.isLocal) position else 0
                )
                player.prepare()

                if (!queuedSong.mediaItem.isLocal) {
                    val duration = try {
                        preferences.getFloat(stateDurationKey, 0f)
                    } catch (e: Exception) {
                        0f
                    }
                    val mId = preferences.getString(stateMediaIdKey, null)
                    playFromSecond = position.toFloat()
                    _currentSecond.value = playFromSecond
                    _currentDuration.value = if (queuedSong.mediaId == mId) duration else 0f
                    _internalOnlinePlayer.value?.pause()
                }

            }
        }
    }


    open inner class Binder : AndroidBinder() {
        val player: ExoPlayer
            get() = this@PlayerService.player

        val playerState: StateFlow<PlayerState>
            get() = this@PlayerService.playerState

        val onlinePlayer: YouTubePlayer?
            get() = this@PlayerService.internalOnlinePlayer.value

        val onlinePlayerPlayingState: Boolean
            get() = this@PlayerService.playerState.value.isPlaying

        val onlinePlayerBufferedFraction: StateFlow<Float>
            get() = this@PlayerService.internalBufferedFraction

        val onlinePlayerCurrentDuration: StateFlow<Float>
            get() = this@PlayerService.currentDuration

        val onlinePlayerCurrentSecond: StateFlow<Float>
            get() = this@PlayerService.currentSecond

        val onlinePlayerView: StateFlow<YouTubePlayerView?>
            get() = this@PlayerService.internalOnlinePlayerView

        val cache: Cache
            get() = this@PlayerService.cache

        val mediaSession
            get() = this@PlayerService.unifiedMediaSession

        val currentMediaItemAsSong: Song?
            get() = this@PlayerService.player.currentMediaItem?.asSong

        val riTuneCastClient: RiTuneCastClient
            @Synchronized
            get() = this@PlayerService.riTuneCastClient

        val equalizer: EqualizerHelper
            get() = this@PlayerService.equalizerHelper

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        val bitmap: Bitmap?
            get() = this@PlayerService.bitmapProvider?.bitmap

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()

            val endTime = System.currentTimeMillis() + delayMillis
            preferences.edit { putLong(timerEndTimeKey, endTime) }

            Timber.d("PlayerService startSleepTimer delayMillis $delayMillis, scheduled for $endTime")

            timerJob = serviceScope.timer(delayMillis) {
                Timber.d("PlayerService timer finished naturally")
                executeStopServiceLogic()
            }
        }

        fun executeStopServiceLogic() {
            preferences.edit { putLong(timerEndTimeKey, 0) }

            saveQueue()

            val notification = NotificationCompat
                .Builder(this@PlayerService, SLEEPTIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Self closing timer ended")
                .setSmallIcon(R.drawable.app_icon)
                .build()
            notificationManager?.notify(SLEEPTIMER_NOTIFICATION_ID, notification)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.appTasks?.forEach { it.finishAndRemoveTask() }

            Handler(Looper.getMainLooper()).postDelayed({
                exitProcess(0)
            }, 300L)
        }

        fun cancelSleepTimer() {
            Timber.d("PlayerService cancelSleepTimer")
            timerJob?.cancel()
            timerJob = null
        }

        @UnstableApi
        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = true)

        @UnstableApi
        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = false)


        @UnstableApi
        private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean, filterArtist: String = "") {
            radioJob?.cancel()
            radio = null
            val isDiscoverEnabled = applicationContext.preferences.getBoolean(discoverKey, false)
            val filterContentType = applicationContext.preferences.getEnum(filterContentTypeKey,
                ContentType.All)

            OnlineRadio(
                endpoint?.videoId,
                endpoint?.playlistId,
                endpoint?.playlistSetVideoId,
                endpoint?.params,
                isDiscoverEnabled,
                applicationContext,
                binder,
                serviceScope
            ).let {
                isLoadingRadio = true
                radioJob = serviceScope.launch(Dispatchers.Main) {

                    val songs =
                        (if (filterArtist.isEmpty()) it.process()
                        else it.process().filter { song -> song.mediaMetadata.artist == filterArtist })
                            .filter { song ->
                                when (filterContentType) {
                                    ContentType.All -> true
                                    ContentType.Official -> song.isOfficialContent
                                    ContentType.UserGenerated -> song.isUserGeneratedContent
                                }
                            }

                    songs.forEach {
                        Database.asyncTransaction { insert(it) }
                    }

                    if (justAdd) {
                        player.addMediaItems( songs.drop(1))
                    } else {
                        player.forcePlayFromBeginning(songs)
                    }
                    radio = it
                    isLoadingRadio = false
                }
            }
        }

        fun stopRadio() {
            isLoadingRadio = false
            radioJob?.cancel()
            radio = null
        }

        fun playFromSearch(query: String) {
            serviceScope.launch {
                Environment.searchPage(
                    body = SearchBody(
                        query = query,
                        params = Environment.SearchFilter.Song.value
                    ),
                    fromMusicShelfRendererContent = Environment.SongItem.Companion::from
                )?.getOrNull()?.items?.firstOrNull()?.info?.endpoint?.let { playRadio(it) }
            }
        }

        /**
         * This method should ONLY be called when the application (sc. activity) is in the foreground!
         */
        fun restartForegroundOrStop() {
            player.pause()
            stopSelf()
        }

        @OptIn(FlowPreview::class)
        fun toggleLike() {
            Timber.d("PlayerService toggleLike currentSong ${currentSong.value}")
            Database.asyncTransaction {
                currentSong.value?.let {
                    Timber.d("PlayerService toggleLike currentSong inside ${it.title}")
                    like(
                        it.id,
                        setLikeState(it.likedAt)
                    )
                }.also {
                    currentSong.debounce(1000).collect(serviceScope) { updateUnifiedNotification() }
                }
            }

        }

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        fun toggleShuffle() {
            player.shuffleModeEnabled = !player.shuffleModeEnabled

        }

        fun toggleRepeat() {
            val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
            preferences.edit { putEnum(queueLoopTypeKey, setQueueLoopState(queueLoopType)) }
        }

        fun callPause(onPause: () -> Unit) {
            val fadeDisabled = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled) == DurationInMilliseconds.Disabled
            val duration = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled).milliSeconds
            if (player.isPlaying) {
                if (fadeDisabled) {
                    player.pause()
                    onPause()
                } else {
                    //fadeOut
                    startFadeAnimator(player, duration, false) {
                        player.pause()
                        onPause()
                    }
                }
            }
        }

        fun actionSearch() {
            startActivity(Intent(applicationContext, MainActivity::class.java)
                .setAction(MainActivity.action_search)
                .setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK))
        }

        fun loadQueue() = this@PlayerService.loadQueue()

    }


    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatching {
                context.stopService(context.intent<PlayerService>())
            }.onFailure {
                Timber.e("Failed NotificationDismissReceiver stopService in PlayerService ${it.stackTraceToString()}")
            }
        }
    }

    fun initializeUnifiedSessionCallback() {
        Timber.d("PlayerService InitializeUnifiedSessionCallback")
        val currentMediaItem = binder.player.currentMediaItem

        binder.let {
            unifiedMediaSession.setCallback(
                PlayerMediaSessionCallback(
                    binder = it,
                    onPlayClick = {
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onPlayClick")
                        if (player.currentMediaItem?.isLocal == true)
                            it.player.play()
                        else {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                                _internalOnlinePlayer.value?.play()
                            else
                                serviceScope.launch {
                                    riTuneCastClient.sendCommand(
                                        RiTuneRemoteCommand(
                                            "play",
                                            position = playFromSecond
                                        )
                                    )
                                }
                        }
                    },
                    onPauseClick = {
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onPauseClick")
                        it.player.pause()
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                            _internalOnlinePlayer.value?.pause()
                        } else {
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "pause",
                                    )
                                )
                            }
                        }
                    },
                    onSeekToPos = { second ->
                        val newPosition = (second / 1000).toFloat()
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onSeekPosTo ${newPosition}")
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                            _internalOnlinePlayer.value?.seekTo(newPosition)
                        else
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "seek",
                                        position = newPosition
                                    )
                                )
                            }

                        _currentSecond.value = second.toFloat()

                    },
                    onPlayNext = {
                        handlePlayNext()
                    },
                    onPlayPrevious = {
                        player.playPrevious()
                    },
                    onPlayQueueItem = { queueId ->
                        val timelineIndex = queueId.toInt()
                        if (timelineIndex >= 0 && timelineIndex < player.currentTimeline.windowCount) {
                            player.seekToDefaultPosition(timelineIndex)
                        }
                    },
                    onCustomClick = { customAction ->
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onCustomClick $customAction")
                        when (customAction) {
                            NotificationButtons.Favorites.action -> {
                                it.toggleLike()
                            }
                            NotificationButtons.Repeat.action -> {
                                it.toggleRepeat()
                            }
                            NotificationButtons.Shuffle.action -> {
                                it.toggleShuffle()
                            }
                            NotificationButtons.Radio.action -> {
                                if (currentMediaItem != null) {
                                    it.stopRadio()
                                    it.player.seamlessQueue(currentMediaItem)

                                    if(!GlobalSharedData.riTuneCastActive)
                                        _internalOnlinePlayer.value?.play()
                                    else
                                        serviceScope.launch {
                                            riTuneCastClient.sendCommand(
                                                RiTuneRemoteCommand(
                                                    "play",
                                                    position = playFromSecond
                                                )
                                            )
                                        }

                                    it.setupRadio(
                                        NavigationEndpoint.Endpoint.Watch(videoId = currentMediaItem.mediaId)
                                    )
                                }
                            }
                            NotificationButtons.Search.action -> {
                                it.actionSearch()
                            }
                        }

                    }
                )
            )
        }
    }

    fun handlePlayNext() {
        val now = System.currentTimeMillis()
        if (now - lastPlayNextTime < debounceDelayMs) {
            Timber.d("PlayerService handlePlayNext ignored (too fast)")
            return
        }
        lastPlayNextTime = now
        Timber.d("PlayerService handlePlayNext executed")
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                player.playNext()
            }
        }
    }

    @JvmInline
    value class Action(val value: String) {
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                appContext(),
                100,
                Intent(value).setPackage(appContext().packageName),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {

            val pause = Action("it.fast4x.riplay.pause")
            val play = Action("it.fast4x.riplay.play")
            val next = Action("it.fast4x.riplay.next")
            val previous = Action("it.fast4x.riplay.previous")
            val like = Action("it.fast4x.riplay.like")
            val playradio = Action("it.fast4x.riplay.playradio")
            val shuffle = Action("it.fast4x.riplay.shuffle")
            val search = Action("it.fast4x.riplay.search")
            val repeat = Action("it.fast4x.riplay.repeat")
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        val NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.player_notification_channel_id)

        const val SLEEPTIMER_NOTIFICATION_ID = 1002
        val SLEEPTIMER_NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.sleep_timer_notification_channel_id)

        const val ACTION_UPDATE_PHONE_LISTENER = "it.fast4x.riplay.action.UPDATE_PHONE_LISTENER"
        const val EXTRA_ENABLE_LISTENER = "enable_listener"


    }


}


