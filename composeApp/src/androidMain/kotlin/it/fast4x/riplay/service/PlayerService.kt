package it.fast4x.riplay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.database.SQLException
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
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
import androidx.media3.datasource.DataSpec
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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
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
import it.fast4x.riplay.extensions.preferences.currentQueuePositionKey
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
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
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
import it.fast4x.riplay.utils.isAtLeastAndroid11
import it.fast4x.riplay.utils.isHandleAudioFocusEnabled
import it.fast4x.riplay.utils.isKeepScreenOnEnabled
import it.fast4x.riplay.utils.isOfficialContent
import it.fast4x.riplay.utils.isSkipMediaOnErrorEnabled
import it.fast4x.riplay.utils.isUserGeneratedContent
import it.fast4x.riplay.utils.loadMasterQueue
import it.fast4x.riplay.utils.principalCache
import it.fast4x.riplay.utils.saveMasterQueue
import it.fast4x.riplay.utils.seamlessQueue
import it.fast4x.riplay.commonutils.setLikeState
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.extensions.lastfm.sendNowPlaying
import it.fast4x.riplay.extensions.lastfm.sendScrobble
import it.fast4x.riplay.extensions.preferences.excludeSongIfIsVideoKey
import it.fast4x.riplay.extensions.preferences.isEnabledLastfmKey
import it.fast4x.riplay.extensions.preferences.lastfmScrobbleTypeKey
import it.fast4x.riplay.extensions.preferences.lastfmSessionTokenKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.ritune.improved.RiTuneClient
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneConnectionStatus
import it.fast4x.riplay.extensions.ritune.improved.models.RiTunePlayerState
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneRemoteCommand
import it.fast4x.riplay.service.helpers.BluetoothConnectReceiver
import it.fast4x.riplay.service.helpers.NoisyAudioReceiver
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.toggleRepeatMode
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Objects
import kotlin.collections.map
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import android.os.Binder as AndroidBinder

const val LOCAL_KEY_PREFIX = "local:"
const val LOCAL_AUDIO_URI_PATH = "content://media/external/audio/media/"

@get:OptIn(UnstableApi::class)
val DataSpec.isLocal get() = key?.startsWith(LOCAL_KEY_PREFIX) == true
@get:OptIn(UnstableApi::class)
val DataSpec.isLocalUri get() = uri.toString().startsWith("content://")

val MediaItem.isLocal get() = mediaId.startsWith(LOCAL_KEY_PREFIX)
val Song.isLocal get() = id.startsWith(LOCAL_KEY_PREFIX)

@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : Service(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnAudioVolumeChangedListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var unifiedMediaSession: MediaSessionCompat
    val cache: SimpleCache by lazy {
        principalCache.getInstance(this)
    }
    lateinit var player: ExoPlayer
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    //private lateinit var connectivityManager: ConnectivityManager

    private val metadataBuilder = MediaMetadataCompat.Builder()

    private var notificationManager: NotificationManager? = null

    private var timerJob: TimerJob? = null

    private var radio: OnlineRadio? = null

    var bitmapProvider: BitmapProvider? = null

    private var volumeNormalizationJob: Job? = null
    private var positionObserverJob: Job? = null

    private var isPersistentQueueEnabled = false
    private var isResumePlaybackOnStart = false
    //private var isclosebackgroundPlayerEnabled = false
    private var closeServiceAfterMinutes by mutableStateOf(DurationInMinutes.Disabled)
    private var closeServiceWhenPlayerPausedAfterMinutes by mutableStateOf(DurationInMinutes.Disabled)

    private var isShowingThumbnailInLockscreen = true
    private var medleyDuration by mutableFloatStateOf(0f)

//    private var audioManager: AudioManager? = null
//    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

    private var isNotificationStarted = false

    var notificationActionReceiverUpAndroid11: NotificationActionReceiverUpAndroid11? = null

    var serviceRestartReceiver: ServiceRestartReceiver? = null

    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    var currentMediaItemState = MutableStateFlow<MediaItem?>(null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private val currentSong = currentMediaItemState.flatMapLatest { mediaItem ->
        Database.song(mediaItem?.mediaId)
    }.stateIn(coroutineScope, SharingStarted.Lazily, null)

    lateinit var sleepTimerListener: SleepTimerListener

    /**
     * Online configuration
     */
    var currentSecond: MutableState<Float> = mutableFloatStateOf(0f)
    var currentDuration: MutableState<Float> = mutableFloatStateOf(0f)
    var internalOnlinePlayerView: MutableState<YouTubePlayerView> = mutableStateOf(
        LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false)
                as YouTubePlayerView
    )
    var internalOnlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)
    var internalOnlinePlayerState by mutableStateOf(PlayerConstants.PlayerState.UNSTARTED)
    var load = true
    var playFromSecond by mutableFloatStateOf(0f)
    var lastError: PlayerConstants.PlayerError? = null
    var isPlayingNow by mutableStateOf(false)
    var localMediaItem: MediaItem? = null
    var closingTimerStarted: Boolean? = false

    /**
     * end online configuration
     */

    private fun playerPositionMonitor(player: ExoPlayer) = flow {
        while (player.isPlaying && player.currentMediaItem?.isLocal == true) {
            emit(player.currentPosition)
            delay(1000)
        }
    }.flowOn(Dispatchers.Main)

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

    private var noisyReceiver: NoisyAudioReceiver? = null
    private var bluetoothReceiver: BluetoothConnectReceiver? = null

    private val riTuneClient: RiTuneClient = RiTuneClient()
    private var riTuneObserverJob: Job? = null
    private var riTunePlayerState: RiTunePlayerState? = null

    //private var checkVolumeLevel: Boolean = true


    override fun onBind(intent: Intent?): AndroidBinder {
        return binder
    }


    @ExperimentalCoroutinesApi
    @FlowPreview
    @SuppressLint("Range")
    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        /**
         * Online initialization
         */
        createNotificationChannel()

        preferences.registerOnSharedPreferenceChangeListener(this)

        //val preferences = preferences
        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, true)
        isResumePlaybackOnStart = preferences.getBoolean(resumePlaybackOnStartKey, false)
        isShowingThumbnailInLockscreen =
            preferences.getBoolean(isShowingThumbnailInLockscreenKey, false)
        medleyDuration = preferences.getFloat(playbackDurationKey, 0f)

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
                sleepTimerListener = SleepTimerListener(coroutineScope, this)
                addListener(sleepTimerListener)
                addAnalyticsListener(PlaybackStatsListener(false, this@PlayerService))
            }

        player.repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        //player.pauseAtEndOfMediaItems = true

        audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)

        //connectivityManager = getSystemService()!!

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                if (localMediaItem?.isLocal == true) {
                    playerPositionMonitor(player).collect {
                        updateUnifiedNotification()
                    }
                }
            }
        }

        if (isPersistentQueueEnabled) {
            coroutineScope.launch {

                loadMasterQueueWithPosition()
                withContext(Dispatchers.Main) {
                    resumePlaybackOnStart()
                }

                while (isActive) {
                    delay(2.minutes)
                    saveMasterQueueWithPosition()

                    if (currentSecond.value >= minTimeForEvent.seconds && lastMediaIdInHistory != currentSong.value?.id) {
                        currentSong.value?.let {
                            updateOnlineHistory(it.asMediaItem)
                            lastMediaIdInHistory = it.id
                        }
                    }

                }
            }
        }

        currentSong.debounce(1000).collect(coroutineScope) { song ->
            Timber.d("PlayerService onCreate update currentSong $song mediaItemState ${currentMediaItemState.value}")
            withContext(Dispatchers.Main) {
                updateUnifiedNotification()
                updateWidgets()

            }
        }

        initializeNotificationActionReceiverUpAndroid11()
        initializeUnifiedMediaSession()
        initializeBitmapProvider()
        initializeOnlinePlayer()

        startForeground()

        initializePositionObserver()
        initializeBluetoothConnect()
        initializeBassBoost()
        initializeReverb()
        initializeSensorListener()
        initializeSongCoverInLockScreen()
        initializeMedleyMode()
        initializeVariables()
        initializePlaybackParameters()
        initializeNoisyReceiver()
        initializeRiTune()
        initializeDiscordPresence()
        //initializeServiceRestartReceiver() not used for now
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_NOT_STICKY
    }

    private fun startForeground() {
        runCatching {
            notification()?.let {
                ServiceCompat.startForeground(
                    this@PlayerService,
                    NOTIFICATION_ID,
                    it,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else {
                        0
                    }
                )
            }
        }.onFailure {
            Timber.e("PlayerService oncreate startForeground ${it.stackTraceToString()}")
        }
    }

    private fun initializeServiceRestartReceiver() {
        serviceRestartReceiver = ServiceRestartReceiver()

        val filter = IntentFilter().apply {
            addAction("restart")
        }

        ContextCompat.registerReceiver(
            this@PlayerService,
            serviceRestartReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun initializeVariables() {
        // todo add here all val that requires first initialize and add references in shared preferences, so is not nededed restart service when change it
        currentMediaItemState.value =  player.currentMediaItem
        //isclosebackgroundPlayerEnabled = preferences.getBoolean(closebackgroundPlayerKey, false)
        closeServiceAfterMinutes = preferences.getEnum(closePlayerServiceAfterMinutesKey, DurationInMinutes.Disabled)
        closeServiceWhenPlayerPausedAfterMinutes = preferences.getEnum(
            closePlayerServiceWhenPausedAfterMinutesKey, DurationInMinutes.Disabled)
    }

    private fun initializePlaybackParameters() {
        when (localMediaItem?.isLocal) {
            false -> {
                val playbackSpeed = preferences.getFloat(playbackSpeedKey, 1f)
                val onlinePlabackRate = when {
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
                internalOnlinePlayer.value?.setPlaybackRate(onlinePlabackRate)
            }
            else -> {
                player.playbackParameters = PlaybackParameters(
                    preferences.getFloat(playbackSpeedKey, 1f),
                    preferences.getFloat(playbackPitchKey, 1f)
                )
            }
        }

    }

    private fun initializeMedleyMode() {
        coroutineScope.launch {
            while (medleyDuration > 0) {
                withContext(Dispatchers.Main) {
                    Timber.d("PlayerService initializeMedleyMode medleyDuration $medleyDuration player.isPlaying ${player.isPlaying} internalOnlinePlayerState ${internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING}")
                    val seconds = if (localMediaItem?.isLocal == true) player.currentPosition.div(1000).toInt() else currentSecond.value.toInt()
                    if (medleyDuration.toInt() <= seconds) {
                        //delay(1.seconds * (medleyDuration.toInt() + 2))
                        handleSkipToNext()
                    }
                }
            }
        }
    }

    private fun initializeRiTune() {

        riTuneObserverJob?.cancel()

        var isConnecting = false

        riTuneObserverJob = coroutineScope.launch {

            while (isActive) {

                val connectionStatus = riTuneClient.connectionStatus.value
                val isCastActive = GlobalSharedData.riTuneCastActive

                val playerState = riTuneClient.state.value?.state
                internalOnlinePlayerState = playerState ?: PlayerConstants.PlayerState.UNSTARTED
                val duration = riTuneClient.state.value?.duration
                if (duration != null) {
                    currentDuration.value = duration
                }
                val second = riTuneClient.state.value?.currentTime
                if (second != null) {
                    currentSecond.value = second
                }




                Timber.d("PlayerService initializeRiTune Loop - CastActive: $isCastActive, Status: $connectionStatus, isConnecting: $isConnecting PlayerState $playerState  ")

                if (!isCastActive) {
                    if (isConnecting) isConnecting = false
                    Timber.d("PlayerService initializeRiTune CAST NOT ACTIVE - Status: $connectionStatus, isConnecting: $isConnecting")
                    if (connectionStatus == RiTuneConnectionStatus.Connected) {
                        riTuneClient.disconnect()
                        Timber.d("PlayerService initializeRiTune CAST NOT ACTIVE - Disconnected")
                    }

                } else {

                    if (connectionStatus == RiTuneConnectionStatus.Connected) {
                        if (isConnecting) {
                            isConnecting = false
                            Timber.d("PlayerService initializeRiTune Connection established successfully")
                        }

                    } else if (!isConnecting) {

                        Timber.d("PlayerService initializeRiTune CAST ACTIVE - Trying to connect...")

                        val device = GlobalSharedData.riTuneDevices.firstOrNull { it.selected }

                        if (device != null) {
                            isConnecting = true
                            launch {
                                try {
                                    riTuneClient.startConnection(
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
                Timber.d("PlayerService initializeRiTune Loop Tick - Active: $isActive")
                delay(1000)
            }
            Timber.d("PlayerService initializeRiTune: JOB TERMINATO (end of loop)")
        }
    }

    private fun initializeDiscordPresence() {
        if (!isAtLeastAndroid81) return

        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            val token = preferences.getString(discordPersonalAccessTokenKey, "")
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
                    sensorManager!!
                        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
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
                    handleSkipToNext()
                }

            }

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun resumePlaybackOnStart() {
        if(!isPersistentQueueEnabled && !isResumePlaybackOnStart) return

        when (player.currentMediaItem?.isLocal) {
            true -> { if (!player.isPlaying) player.play() }
            else -> {}
        }

    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateUnifiedNotification()
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

        unifiedMediaSession.isActive = true

    }

    private fun initializeOnlinePlayer() {

        internalOnlinePlayerView.value.apply {
            enableAutomaticInitialization = false

            enableBackgroundPlayback(true)

            keepScreenOn = isKeepScreenOnEnabled()

            val iFramePlayerOptions = IFramePlayerOptions.Builder(appContext())
                .controls(0) // show/hide controls
                .listType("playlist")
                .origin(resources.getString(R.string.env_fqqhBZd0cf))
                .build()

            val listener = object : AbstractYouTubePlayerListener() {

                override fun onReady(youTubePlayer: YouTubePlayer) {
                    super.onReady(youTubePlayer)
                    internalOnlinePlayer.value = youTubePlayer

                    val customUiController =
                        CustomDefaultPlayerUiController(
                            context,
                            internalOnlinePlayerView.value,
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
                    internalOnlinePlayerView.value.setCustomPlayerUi(customUiController.rootView)

                    Timber.d("PlayerService onlinePlayer onReady localmediaItem ${localMediaItem?.mediaId} queue index ${binder.player.currentMediaItemIndex}")
                    Timber.d("PlayerService onlinePlayer onReady isPersistentQueueEnabled $isPersistentQueueEnabled isResumePlaybackOnStart $isResumePlaybackOnStart")

                    localMediaItem?.let{
                        if (isPersistentQueueEnabled && isResumePlaybackOnStart && firstTimeStarted) {
                            youTubePlayer.loadVideo(it.mediaId, playFromSecond)
                            Timber.d("PlayerService onlinePlayer onReady loadVideo ${it.mediaId}")
                        }
                    }
                    youTubePlayer.setVolume(getSystemMediaVolume())
                    firstTimeStarted = false
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    super.onCurrentSecond(youTubePlayer, second)
                    currentSecond.value = second
                    //Timber.d("PlayerService onlinePlayerView: onCurrentSecond $second")
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    super.onVideoDuration(youTubePlayer, duration)
                    currentDuration.value = duration
                    updateUnifiedNotification()
                    updateDiscordPresence()

                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {

                    Timber.d("PlayerService onlinePlayerView: onStateChange $state")

                    when(state) {
                        PlayerConstants.PlayerState.PLAYING, PlayerConstants.PlayerState.PAUSED -> {
                            youTubePlayer.unMute()
                        }
                        PlayerConstants.PlayerState.VIDEO_CUED, PlayerConstants.PlayerState.UNSTARTED -> {
                            if (!firstTimeStarted)
                                youTubePlayer.play()
                        }
                        else -> { youTubePlayer.mute() }
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

                    internalOnlinePlayerState = state
                    isPlayingNow = state == PlayerConstants.PlayerState.PLAYING
                    updateUnifiedNotification()
                    updateDiscordPresence()

                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    //super.onError(youTubePlayer, error)

                    localMediaItem?.isLocal?.let { if (it) return }
                    if (isPersistentQueueEnabled)
                        saveMasterQueueWithPosition()

                    youTubePlayer.pause()
                    clearWebViewData()

                    Timber.e("PlayerService: onError $error")
                    val errorString = when (error) {
                        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> "Content not playable, recovery in progress, try to click play but if the error persists try to log in"
                        PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Content not found, perhaps no longer available"
                        PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST -> "Invalid parameters in request"
                        else -> null
                    }

                    if (errorString != null && lastError != error) {
                        if (error != PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST)
                            SmartMessage(
                                errorString,
                                PopupType.Error,
                                //durationLong = true,
                                context = this@PlayerService
                            )

                        localMediaItem?.let { youTubePlayer.cueVideo(it.mediaId, playFromSecond) }
                        //if (checkVolumeLevel)
                        youTubePlayer.setVolume(getSystemMediaVolume())
                        return
                    }

                    lastError = error

                    if (!isSkipMediaOnErrorEnabled()) return
                    val prev = binder.player.currentMediaItem ?: return

                    //binder.player.playNext()
                    handleSkipToNext()

                    SmartMessage(
                        message = this@PlayerService.getString(
                            R.string.skip_media_on_error_message,
                            prev.mediaMetadata.title
                        ),
                        context = this@PlayerService,
                    )

                }

            }

            initialize(listener, iFramePlayerOptions)

        }

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
            this@PlayerService,
            notificationActionReceiverUpAndroid11,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun updateDiscordPresence() {
        if (!isAtLeastAndroid81) return

        localMediaItem?.let{
            if (it.isLocal) {
                updateDiscordPresenceWithOnlinePlayer(
                    discordPresenceManager,
                    it,
                    mutableStateOf(internalOnlinePlayerState),
                    currentDuration.value,
                    currentSecond.value
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
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager?

        val STREAM_TYPE = AudioManager.STREAM_MUSIC
        val currentVolume = audio?.getStreamVolume(STREAM_TYPE)
        val maxVolume = audio?.getStreamMaxVolume(STREAM_TYPE)
        val VOLUME_UP = 1
        val VOLUME_DOWN = -1

        return object :
            VolumeProviderCompat(VOLUME_CONTROL_RELATIVE, maxVolume!!, currentVolume!!) {

                override fun onAdjustVolume(direction: Int) {
                        val useVolumeKeysToChangeSong = preferences.getBoolean(useVolumeKeysToChangeSongKey, false)
                        // Up = 1, Down = -1, Release = 0
                        if (direction == VOLUME_UP) {
                            if (binder.player.isPlaying && useVolumeKeysToChangeSong) {
                                binder.player.forceSeekToNext()
                            } else {
                                audio?.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                if (audio != null) {
                                    setCurrentVolume(audio.getStreamVolume(STREAM_TYPE))
                                }
                            }
                        } else if (direction == VOLUME_DOWN) {
                            if (binder.player.isPlaying && useVolumeKeysToChangeSong) {
                                binder.player.forceSeekToPrevious()
                            } else {
                                audio?.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                if (audio != null) {
                                    setCurrentVolume(audio.getStreamVolume(STREAM_TYPE))
                                }
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
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("PlayerService onTaskRemoved closeServiceAfterMinutes $closeServiceAfterMinutes")
        if (closeServiceAfterMinutes != DurationInMinutes.Disabled) {
            binder.startSleepTimer(closeServiceAfterMinutes.minutesInMilliSeconds)
        }
//        if (isclosebackgroundPlayerEnabled) {
//            player.pause()
//            internalOnlinePlayer.value?.pause()
//            broadCastPendingIntent<NotificationDismissReceiver>().send()
//            this.stopService(this.intent<PlayerService>())
//            //stopSelf()
//            onDestroy()
//            super.onTaskRemoved(rootIntent)
//        }
    }

    @UnstableApi
    override fun onDestroy() {
        Timber.d("PlayerService onDestroy")

        try {
            unregisterReceiver(notificationActionReceiverUpAndroid11)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver ${e.stackTraceToString()}")
        }


        runCatching {
            saveMasterQueueWithPosition()

            preferences.unregisterOnSharedPreferenceChangeListener(this)

            coroutineScope.launch {
                withContext(Dispatchers.Main){
                    player.removeListener(this@PlayerService)
                    player.stop()
                    player.release()
                }
            }


            //unregisterReceiver(serviceRestartReceiver)

            unifiedMediaSession.isActive = false
            unifiedMediaSession.release()
            cache.release()
            loudnessEnhancer?.release()
            audioVolumeObserver.unregister()
            noisyReceiver?.unregister()
            bluetoothReceiver?.unregister()

            discordPresenceManager?.onStop()

            coroutineScope.launch { delay(1000) }

            coroutineScope.cancel()

        }.onFailure {
            Timber.e("Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }

        super.onDestroy()
    }

    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (preferences.getBoolean(isPauseOnVolumeZeroEnabledKey, false)) {
            if ((player.isPlaying || internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING) && currentVolume < 1) {
                if (player.currentMediaItem?.isLocal == true) {
                    binder.callPause {}
                } else {
                    internalOnlinePlayer.value?.pause()
                }
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                if (player.currentMediaItem?.isLocal == true) {
                    binder.player.play()
                } else {
                    internalOnlinePlayer.value?.play()
                }
                pausedByZeroVolume = false
            }
        }

        if (localMediaItem?.isLocal == false
            //&& checkVolumeLevel
            ) {
            val onlineVolume = getSystemMediaVolume()
            Timber.d("PlayerService onAudioVolumeChanged currentVolume $currentVolume onlineVolume $onlineVolume")
            internalOnlinePlayer.value?.setVolume(onlineVolume)
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

        // if pause listen history is enabled, don't register statistic event
        if (preferences.getBoolean(pauseListenHistoryKey, false)) return

        Timber.d("PlayerService onPlaybackStatsReady PROCESS eventTime $eventTime playbackStats $playbackStats")

        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @UnstableApi
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

        Timber.d("PlayerService onMediaItemTransition RiTune Devices ${GlobalSharedData.riTuneDevices}")

        startForeground()

        if (mediaItem == null) return

        Timber.d("PlayerService onMediaItemTransition mediaItem ${mediaItem.mediaId} reason $reason")

        currentQueuePosition = player.currentMediaItemIndex
        currentSecond.value = 0F

        if (parentalControlEnabled && mediaItem.isExplicit) {
            handleSkipToNext()
            SmartMessage(resources.getString(R.string.error_message_parental_control_restricted), context = this@PlayerService)
            return
        }

        if (excludeIfIsVideoEnabled && mediaItem.isVideo) {
            handleSkipToNext()
            SmartMessage(getString(R.string.warning_skipped_video), context = this@PlayerService)
            return
        }

        var blacklisted by mutableStateOf(false)
        runBlocking(Dispatchers.IO) {
            blacklisted = Database.blacklisted(mediaItem.mediaId) > 0
        }
        if (blacklisted) {
            handleSkipToNext()
            SmartMessage(getString(R.string.warning_skipped_blacklisted_song), context = this@PlayerService)
            return
        }

        mediaItem.let {
            currentMediaItemState.value = it
            localMediaItem = it

            if (!it.isLocal){

                Timber.d("PlayerService onMediaItemTransition system volume ${getSystemMediaVolume()}")

                //internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)
                coroutineScope.launch {
                    riTuneClient.sendCommand(
                        RiTuneRemoteCommand(
                            "load",
                            mediaId = it.mediaId,
                            position = 0f
                        )
                    )
                }
                //internalOnlinePlayer.value?.loadVideo(it.mediaId, playFromSecond)
                //startFadeAnimator(player = internalOnlinePlayer, volumeDevice = getSystemMediaVolume(), duration = 5, fadeIn = true) {}
                //if (checkVolumeLevel)
                internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())

            }

            bitmapProvider?.load(it.mediaMetadata.artworkUri) {}
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            updateMediaSessionQueue(player.currentTimeline)
        }

        maybeRecoverPlaybackError()
        initializeNormalizeVolume()
        maybeProcessRadio(reason)

        updateUnifiedNotification()
        updateWidgets()
        updateDiscordPresence()

        saveMasterQueueWithPosition()

        if (preferences.getBoolean(isEnabledLastfmKey, false))
            preferences.getString(lastfmSessionTokenKey, "")?.let {
                when (preferences.getEnum(lastfmScrobbleTypeKey, LastFmScrobbleType.Simple)) {
                    LastFmScrobbleType.Simple -> {
                        sendScrobble(
                            mediaItem.mediaMetadata.artist.toString(),
                            mediaItem.mediaMetadata.title.toString(),
                            mediaItem.mediaMetadata.albumTitle.toString(),
                            it
                        )
                    }
                    LastFmScrobbleType.NowPlaying -> {
                        sendNowPlaying(
                            mediaItem.mediaMetadata.artist.toString(),
                            mediaItem.mediaMetadata.title.toString(),
                            mediaItem.mediaMetadata.albumTitle.toString(),
                            it
                        )
                    }
                }

            }

        Timber.d("PlayerService-onMediaItemTransition mediaItem: ${mediaItem.mediaId} currentMediaItemIndex: $currentQueuePosition shuffleModeEnabled ${player.shuffleModeEnabled} repeatMode ${player.repeatMode} reason $reason")

    }


    fun handleSkipToNext() {
        try {
            val hasNext = player.hasNextMediaItem()
            Timber.d("PlayerService handleSkipToNext hasNext before: $hasNext, previous position: $currentQueuePosition")

            if (hasNext) {
                Timber.d("PlayerService handleSkipToNext hasNext inside: Yes, previous position: $currentQueuePosition")
                val previousPosition = currentQueuePosition

                player.forceSeekToNext()

                currentQueuePosition = player.currentMediaItemIndex

                if (currentQueuePosition <= previousPosition) {
                    Timber.w("PlayerService handleSkipToNext: index not increased force to next")
                    player.forceSeekToNext()
                    currentQueuePosition = player.currentMediaItemIndex
                }
                Timber.d("PlayerService handleSkipToNext hasNext inside end: Yes, current position: $currentQueuePosition")

                saveMasterQueueWithPosition()
            } else {
                Timber.d("PlayerService handleSkipToNext: queue ended")
            }
        } catch (e: Exception) {
            Timber.e("PlayerService handleSkipToNext: skip to next in error ${e.stackTraceToString()}")
            maybeRecoverPlaybackError()
        }
    }


    fun handleSkipToPrevious() {
        try {
            val hasPrevious = player.hasPreviousMediaItem()
            Timber.d("PlayerService handleSkipToPrevious hasPrevious before: $hasPrevious, previous position: $currentQueuePosition")

            if (hasPrevious) {
                val previousPosition = currentQueuePosition

                player.forceSeekToPrevious()
                currentQueuePosition = player.currentMediaItemIndex

                if (currentQueuePosition >= previousPosition) {
                    Timber.w("PlayerService handleSkipToPrevious: index not decreased force to previous")
                    player.forceSeekToPrevious()
                    currentQueuePosition = player.currentMediaItemIndex
                }

                Timber.d("PlayerService handleSkipToPrevious end: current position: $currentQueuePosition")
                saveMasterQueueWithPosition()
            } else {
                Timber.d("PlayerService handleSkipToPrevious: already at start of queue")
            }
        } catch (e: Exception) {
            Timber.e("PlayerService handleSkipToPrevious: skip to previous in error ${e.stackTraceToString()}")
            maybeRecoverPlaybackError()
        }
    }

    private fun handlePlayQueueItem(targetIndex: Int) {
        try {
            if (targetIndex < 0 || targetIndex >= player.mediaItemCount) {
                Timber.w("PlayerService handlePlayQueueItem: invalid index=$targetIndex (count=${player.mediaItemCount})")
                return
            }

            val currentIndex = player.currentMediaItemIndex
            Timber.d("PlayerService handlePlayQueueItem targetIndex=$targetIndex currentIndex=$currentIndex")

            // Android Auto may call "play queue item" even for the currently selected item.
            // If we're already on it (especially for the online player), force a re-cue to avoid staying UNSTARTED/VIDEO_CUED.
            if (targetIndex == currentIndex) {
                player.currentMediaItem?.let { item ->
                    if (!item.isLocal) {
                        internalOnlinePlayer.value?.cueVideo(item.mediaId, playFromSecond)
                        internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())
                        //internalOnlinePlayer.value?.play()
                    } else {
                        // For local items, just ensure playback resumes.
                        if (!player.isPlaying) player.play()
                    }
                }
                return
            }

            player.seekToDefaultPosition(targetIndex)
            currentQueuePosition = targetIndex
            saveMasterQueueWithPosition()
        } catch (e: Exception) {
            Timber.e("PlayerService handlePlayQueueItem: error ${e.stackTraceToString()}")
            maybeRecoverPlaybackError()
        }
    }


    private fun saveMasterQueueWithPosition() {
        try {
            player.saveMasterQueue()

            preferences.edit {
                putInt(currentQueuePositionKey, currentQueuePosition)
            }

            Timber.d("PlayerService saveMasterQueueWithPosition: position and queue saved $currentQueuePosition")
        } catch (e: Exception) {
            Timber.e("PlayerService saveMasterQueueWithPosition: error ${e.stackTraceToString()}")
        }
    }

    private suspend fun loadMasterQueueWithPosition() = suspendCancellableCoroutine { continuation ->
        try {
            val listener = object : Player.Listener {
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                        Timber.d("PlayerService: Timeline changed, queue is now populated.")
                        player.removeListener(this)
                        restoreMasterQueuePosition()
                        continuation.resume(Unit, onCancellation = {})
                    }
                }
            }

            player.addListener(listener)

            player.loadMasterQueue()

            continuation.invokeOnCancellation {
                player.removeListener(listener)
            }

        } catch (e: Exception) {
            Timber.e("PlayerService loadMasterQueueWithPosition: error ${e.stackTraceToString()}")
            continuation.resumeWithException(e)
        }
    }

    private fun restoreMasterQueuePosition() {
        val savedPosition = preferences.getInt(currentQueuePositionKey, 0)
        val mediaItemCount = player.mediaItemCount

        if (mediaItemCount > 0 && savedPosition < mediaItemCount) {
            player.seekToDefaultPosition(savedPosition)
            currentQueuePosition = savedPosition
        }

        Timber.d("PlayerService restoreMasterQueuePosition: restored savedPosition $savedPosition, mediaItemCount $mediaItemCount, currentQueuePosition $currentQueuePosition")
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
        val isLowMemory = level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        Timber.d("PlayerService onTrimMemory level $level isLowMemory $isLowMemory")
        if (isLowMemory)
            saveMasterQueueWithPosition()
    }

    fun updateUnifiedNotification() {


        coroutineScope.launch {
            withContext(Dispatchers.Main){
                if (player.mediaItemCount <= 0) return@withContext
                updateUnifiedMediasession()
                val notifyInstance = notification()
                notifyInstance?.let { NotificationManagerCompat.from(this@PlayerService).notify(NOTIFICATION_ID, it) }
            }
        }
    }


    private fun updateMediaSessionQueue(timeline: Timeline) {
        val builder = MediaDescriptionCompat.Builder()

        val currentMediaItemIndex = player.currentMediaItemIndex
        val lastIndex = timeline.windowCount - 1
        var startIndex = currentMediaItemIndex - 7
        var endIndex = currentMediaItemIndex + 7

        if (startIndex < 0) {
            endIndex -= startIndex
        }

        if (endIndex > lastIndex) {
            startIndex -= (endIndex - lastIndex)
            endIndex = lastIndex
        }

        startIndex = startIndex.coerceAtLeast(0)

        unifiedMediaSession.setQueue(
            List(endIndex - startIndex + 1) { index ->
                val mediaItem = timeline.getWindow(index + startIndex, Timeline.Window()).mediaItem
                MediaSessionCompat.QueueItem(
                    builder
                        .setMediaId(mediaItem.mediaId)
                        .setTitle(cleanPrefix(mediaItem.mediaMetadata.title.toString()))
                        .setSubtitle(
                            if (mediaItem.mediaMetadata.albumTitle != null)
                                "${mediaItem.mediaMetadata.artist} | ${mediaItem.mediaMetadata.albumTitle}"
                            else mediaItem.mediaMetadata.artist
                        )
                        .setIconUri(mediaItem.mediaMetadata.artworkUri)
                        .setExtras(mediaItem.mediaMetadata.extras)
                        .build(),
                    (index + startIndex).toLong()
                )
            }
        )
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
                        internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)
                        //internalOnlinePlayer.value?.loadVideo(it.mediaId, playFromSecond)

                        internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("PlayerService maybeRecoverPlaybackError: recovery error ${e.stackTraceToString()}")
        }
    }

    private fun maybeProcessRadio(reason: Int) {
        if (!preferences.getBoolean(autoLoadSongsInQueueKey, true)) return

        // New feature auto start radio in queue
        //val isDiscoverEnabled = applicationContext.preferences.getBoolean(discoverKey, false)
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 10
            //if (isDiscoverEnabled) 10 else 3
        ) {
            if (radio == null) {
                binder.setupRadio(
                    NavigationEndpoint.Endpoint.Watch(
                        videoId = player.currentMediaItem?.mediaId
                    )
                )
            } else {
                radio?.let { radio ->
                    //if (player.mediaItemCount - player.currentMediaItemIndex <= 3) {
                    coroutineScope.launch(Dispatchers.Main) {
                        if (player.playbackState != STATE_IDLE)
                            player.addMediaItems(radio.process())
                    }
                    //}
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

        runCatching {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(0)
            }
        }.onFailure {
            Timber.e("PlayerService maybeNormalizeVolume load loudnessEnhancer ${it.stackTraceToString()}")
            println("PlayerService maybeNormalizeVolume load loudnessEnhancer ${it.stackTraceToString()}")
            return
        }

        val baseGain = preferences.getFloat(loudnessBaseGainKey, 5.00f)
        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.Main) {
                fun Float?.toMb() = ((this ?: 0f) * 100).toInt()
                Database.loudnessDb(songId).cancellable().collectLatest { loudnessDb ->
                    val loudnessMb = loudnessDb.toMb().let {
                        if (it !in -2000..2000) {
                            withContext(Dispatchers.Main) {
                                SmartMessage("Extreme loudness detected", context = this@PlayerService)
                                /*
                                SmartMessage(
                                    getString(
                                        R.string.loudness_normalization_extreme,
                                        getString(R.string.format_db, (it / 100f).toString())
                                    )
                                )
                                 */
                            }

                            0
                        } else it
                    }
                    try {
                        //default
                        //loudnessEnhancer?.setTargetGain(-((loudnessDb ?: 0f) * 100).toInt() + 500)
                        loudnessEnhancer?.setTargetGain(baseGain.toMb() - loudnessMb)
                        loudnessEnhancer?.enabled = true
                    } catch (e: Exception) {
                        Timber.e("PlayerService maybeNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                        println("PlayerService maybeNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                    }
                }
            }
        }
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

    private fun initializeNoisyReceiver() {
        if (!preferences.getBoolean(resumeOrPausePlaybackWhenDeviceKey, false)) return

        noisyReceiver = NoisyAudioReceiver(this) {
            player.pause()
            internalOnlinePlayer.value?.pause()
            SmartMessage(getString(R.string.music_paused_headphones_disconnected), context = this)
        }

        noisyReceiver?.register()
    }

    private fun initializeBluetoothConnect() {
        if (!preferences.getBoolean(resumeOrPausePlaybackWhenDeviceKey, false)) return

        bluetoothReceiver = BluetoothConnectReceiver(this) {
            if (currentSong.value?.isLocal == true) {
                player.play()
            } else {
                internalOnlinePlayer.value?.play()
            }

            SmartMessage(getString(R.string.music_resumed_headphones_connected), context = this)
        }
        bluetoothReceiver?.register()

    }
    /* todo old maybe will be removed in the future
    @SuppressLint("NewApi")
    private fun initializeResumeOrPausePlaybackWhenDevice() {
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
                    Timber.d("PlayerService onAudioDevicesAdded addedDevices ${addedDevices.map { it.type }}")
                    if (addedDevices.any(::canPlayMusic)) {
                        Timber.d("PlayerService onAudioDevicesAdded device known ${addedDevices.map { it.productName }}")
                        if (player.currentMediaItem?.isLocal == true)
                            player.play()
                        else
                            internalOnlinePlayer.value?.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    Timber.d("PlayerService onAudioDevicesRemoved removedDevices ${removedDevices.map { it.type }}")
                    if (removedDevices.any(::canPlayMusic)) {
                        Timber.d("PlayerService onAudioDevicesRemoved device known ${removedDevices.map { it.productName }}")
                        player.pause()
                        internalOnlinePlayer.value?.pause()
                    }
                }
            }

            //audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)

        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }
    */

    @UnstableApi
    private fun sendOpenEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                    //player.audioSessionId
                    0
                )
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }


    @UnstableApi
    private fun sendCloseEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                    //player.audioSessionId
                    0
                )
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    private fun updateUnifiedMediasession() {
        Timber.d("PlayerService updateUnifiedMediasessionData")
        val currentMediaItem = binder.player.currentMediaItem
        val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
        binder.let {
            unifiedMediaSession.setCallback(
                PlayerMediaSessionCallback(
                    binder = it,
                    onPlayClick = {
                        Timber.d("PlayerService MediaSessionCallback onPlayClick")
                        if (player.currentMediaItem?.isLocal == true)
                            it.player.play()
                        else
                            internalOnlinePlayer.value?.play()
                    },
                    onPauseClick = {
                        Timber.d("PlayerService MediaSessionCallback onPauseClick")
                        it.player.pause()
                        internalOnlinePlayer.value?.pause()
                    },
                    onSeekToPos = { second ->
                        val newPosition = (second / 1000).toFloat()
                        Timber.d("PlayerService MediaSessionCallback onSeekPosTo ${newPosition}")
                        internalOnlinePlayer.value?.seekTo(newPosition)
                        currentSecond.value = second.toFloat()
                    },
                    onPlayNext = {
                        //it.player.playNext()
                        handleSkipToNext()
                    },
                    onPlayPrevious = {
                        handleSkipToPrevious()
                    },
                    onPlayQueueItem = { id ->
                        handlePlayQueueItem(id.toInt())
                    },
                    onCustomClick = { customAction ->
                        Timber.d("PlayerService MediaSessionCallback onCustomClick $customAction")
                        when (customAction) {
                            NotificationButtons.Favorites.action -> {
                                it.toggleLike()
                            }
                            NotificationButtons.Repeat.action -> {
                                preferences.edit(commit = true) { putEnum(queueLoopTypeKey, setQueueLoopState(queueLoopType)) }
                            }
                            NotificationButtons.Shuffle.action -> {
                                //it.player.shuffleQueue()
                                it.toggleShuffle() // toggle shuffle mode
                            }
                            NotificationButtons.Radio.action -> {
                                if (currentMediaItem != null) {
                                    it.stopRadio()
                                    it.player.seamlessQueue(currentMediaItem)
                                    internalOnlinePlayer.value?.play()
                                    it.setupRadio(
                                        NavigationEndpoint.Endpoint.Watch(videoId = currentMediaItem.mediaId)
                                    )
                                }
                            }
                            NotificationButtons.Search.action -> {
                                it.actionSearch()
                            }
                        }
                        updateUnifiedNotification()
                    }
                )
            )
        }

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
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, if (currentMediaItem?.isLocal == false) (currentDuration.value * 1000).toLong() else player.duration)
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
                        if (internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING || player.isPlaying)
                            PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        if(player.currentMediaItem?.isLocal == false) (currentSecond.value * 1000).toLong() else player.currentPosition,
                        1f
                    )
                }
                .build()
        )
        Timber.d("PlayerService updateUnifiedMediasessionData onlineplayer playing ${internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING} localplayer playing ${player?.isPlaying}")
    }

    inner class NotificationActionReceiverUpAndroid11() : BroadcastReceiver() {

        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("MainActivity onReceive intent.action: ${intent.action}")
            val currentMediaItem = binder.player.currentMediaItem
            val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
            binder.let {
                when (intent.action) {
                    Action.pause.value -> {
                        player.pause()
                        internalOnlinePlayer.value?.pause()
                    }
                    Action.play.value -> {
                        if (player.currentMediaItem?.isLocal == true)
                            it.player.play()
                        else
                            internalOnlinePlayer.value?.play()
                    }
                    Action.next.value -> handleSkipToNext() //it.player.playNext()
                    Action.previous.value -> handleSkipToPrevious()
                    Action.like.value -> {
                        it.toggleLike()
                    }
                    Action.repeat.value -> {
                        preferences.edit(commit = true) { putEnum(queueLoopTypeKey, setQueueLoopState(queueLoopType)) }
                    }
                   Action.shuffle.value -> {
                        //it.player.shuffleQueue()
                       it.toggleShuffle() // toggle shuffle mode
                    }
                    Action.playradio.value -> {
                        if (currentMediaItem != null) {
                            it.stopRadio()
                            it.player.seamlessQueue(currentMediaItem)
                            internalOnlinePlayer.value?.play()
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


    // legacy behavior may cause inconsistencies, but not available on sdk 24 or lower
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

        if (notification == null) {
            isNotificationStarted = false
            //makeInvincible(false)
            runCatching {
                stopForeground(false)
            }.onFailure {
                Timber.e("PlayerService Failed stopForeground onEvents ${it.stackTraceToString()}")
            }
            sendCloseEqualizerIntent()
            notificationManager?.cancel(NOTIFICATION_ID)
            return
        }

        if ((player.isPlaying || isPlayingNow) && !isNotificationStarted) {
            isNotificationStarted = true
            runCatching {
                startForegroundService( intent<PlayerService>())
            }.onFailure {
                Timber.e("PlayerServiceFailed startForegroundService onEvents ${it.stackTraceToString()}")
            }
            startForeground()
//                runCatching {
//                    notification()?.let {
//                        ServiceCompat.startForeground(
//                            this@PlayerService,
//                            NotificationId,
//                            it,
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
//                            } else {
//                                0
//                            }
//                        )
//                    }
//                }.onFailure {
//                    Timber.e("PlayerService Failed startForeground onEvents ${it.stackTraceToString()}")
//                }
            //makeInvincible(false)
            sendOpenEqualizerIntent()
        } else {
            if (player.isPlaying || isPlayingNow) {
                isNotificationStarted = false
                runCatching {
                    stopForeground(false)
                }.onFailure {
                    Timber.e("PlayerService Failed stopForeground onEvents ${it.stackTraceToString()}")
                }
                //makeInvincible(true)
                sendCloseEqualizerIntent()
            }
            runCatching {
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }.onFailure {
                Timber.e("PlayerServiceFailed onEvents notificationManager.notify ${it.stackTraceToString()}")
            }
        }
        //}
        //updateUnifiedNotification()
    }
//
//    override fun onPlayerError(error: PlaybackException) {
//
//        Timber.e("PlayerService onPlayerError ${error.stackTraceToString()}")
//        currentMediaItemState.value?.isLocal?.let { if (!it) return }
//
//        if (!preferences.getBoolean(skipMediaOnErrorKey, false) || !player.hasNextMediaItem())
//            return
//
//        val prev = player.currentMediaItem ?: return
//
//        player.playNext()
//        player.prepare()
//
//        showSmartMessage(
//            message = getString(
//                R.string.skip_media_on_error_message,
//                prev.mediaMetadata.title
//            )
//        )
//    }

    private fun showSmartMessage(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Main) {
                SmartMessage(
                    message,
                    type = PopupType.Info,
                    durationLong = true,
                    context = this@PlayerService
                )
            }
        }
    }



    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {

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

        updateWidgets()
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
                    sharedPreferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type
            }
//            closebackgroundPlayerKey -> {
//                    isclosebackgroundPlayerEnabled = sharedPreferences.getBoolean(key, false)
//            }
            closePlayerServiceAfterMinutesKey -> {
                closeServiceAfterMinutes =
                    sharedPreferences.getEnum(closePlayerServiceAfterMinutesKey,
                        DurationInMinutes.Disabled)
            }
            closePlayerServiceWhenPausedAfterMinutesKey -> {
                closeServiceWhenPlayerPausedAfterMinutes =
                    sharedPreferences.getEnum(closePlayerServiceWhenPausedAfterMinutesKey,
                        DurationInMinutes.Disabled)
            }
            isShowingThumbnailInLockscreenKey -> {
                isShowingThumbnailInLockscreen = sharedPreferences.getBoolean(key, true)
                initializeSongCoverInLockScreen()
            }
            playbackDurationKey -> {
                medleyDuration = sharedPreferences.getFloat(playbackDurationKey, 0f)
                initializeMedleyMode()
            }
            exoPlayerMinTimeForEventKey -> {
                minTimeForEvent = sharedPreferences.getEnum(exoPlayerMinTimeForEventKey,
                    MinTimeForEvent.`20s`)
            }
//            checkVolumeLevelKey -> {
//                checkVolumeLevel = sharedPreferences.getBoolean(key, false)
//            }
            resumeOrPausePlaybackWhenDeviceKey -> initializeBluetoothConnect()
            bassboostLevelKey, bassboostEnabledKey -> initializeBassBoost()
            audioReverbPresetKey -> initializeReverb()
            volumeNormalizationKey, loudnessBaseGainKey, volumeBoostLevelKey -> initializeNormalizeVolume()
            playbackPitchKey, playbackSpeedKey -> initializePlaybackParameters()

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


    fun notification(): Notification? {

        val currentMediaItem = binder.player.currentMediaItem

        bitmapProvider?.load(currentMediaItem?.mediaMetadata?.artworkUri) {}

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
            .setContentTitle(currentMediaItem?.mediaMetadata?.title)
            .setContentText(currentMediaItem?.mediaMetadata?.artist)
            //.setSubText(currentMediaItem?.mediaMetadata?.artist)
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

        notificationManager = applicationContext.getSystemService<NotificationManager>()

        notificationManager?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_LOW
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
                        NotificationManager.IMPORTANCE_LOW
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
        val songTitle = player.mediaMetadata.title.toString()
        val songArtist = player.mediaMetadata.artist.toString()
        val isPlaying = player.isPlaying
        coroutineScope.launch {
            playerVerticalWidget.updateInfo(
                context = applicationContext,
                songTitle = songTitle,
                songArtist = songArtist,
                isPlaying = isPlaying,
                bitmap = bitmapProvider?.bitmap,
                player = player
            )
            playerHorizontalWidget.updateInfo(
                context = applicationContext,
                songTitle = songTitle,
                songArtist = songArtist,
                isPlaying = isPlaying,
                bitmap = bitmapProvider?.bitmap,
                player = player
            )
        }
    }

    private fun initializePositionObserver() {
        positionObserverJob?.cancel()
        positionObserverJob = coroutineScope.launch {

            var lastProcessedIndex: Int? = null

            while (isActive) {

                withContext(Dispatchers.Main) {

                    //Timber.d("PlayerService initializePositionObserver BEFORE player.playbackState ${player.playbackState} internalOnlinePlayerState ${internalOnlinePlayerState} lastProcessedIndex $lastProcessedIndex player.currentMediaItemIndex ${player.currentMediaItemIndex}")

                    if (player.currentMediaItem?.isLocal == false)
                        player.pauseAtEndOfMediaItems = true else player.pauseAtEndOfMediaItems = false

                    //Increment playtime of song and add event in the database for online songs
                    if (internalOnlinePlayerState == PlayerConstants.PlayerState.ENDED
                        && currentSong.value?.isLocal == false
                        && !preferences.getBoolean(pauseListenHistoryKey, false)
                    ) {
                        currentSong.value?.id?.let { mediaId ->
                            val totalPlayTimeMs = (currentSecond.value * 1000).toLong()
                            if (currentSecond.value > 5) {
                                Timber.d("PlayerService initializePositionObserver INCREMENT totalPlayTimeMs $totalPlayTimeMs mediaItem ${currentSong.value?.id}")
                                Database.asyncTransaction {
                                    Database.incrementTotalPlayTimeMs(mediaId, totalPlayTimeMs)
                                }
                            }

                            val minTimeForEvent =
                                preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)

                            if (currentSecond.value > minTimeForEvent.seconds) {
                                Timber.d("PlayerService initializePositionObserver INSERT EVENT totalPlayTimeMs $totalPlayTimeMs")
                                Database.asyncTransaction {
                                    try {
                                        Database.insert(
                                            Event(
                                                songId = mediaId,
                                                timestamp = System.currentTimeMillis(),
                                                playTime = totalPlayTimeMs
                                            )
                                        )
                                    } catch (e: SQLException) {
                                        Timber.e("PlayerService initializePositionObserver SQLException ${e.stackTraceToString()}")
                                    }
                                }

                            }

                        }

                    }


                    if ((player.playbackState == Player.STATE_ENDED || internalOnlinePlayerState == PlayerConstants.PlayerState.ENDED)
                        && lastProcessedIndex != player.currentMediaItemIndex
                    ) {

                        Timber.d("PlayerService initializePositionObserver INSIDE player.playbackState ${player.playbackState} internalOnlinePlayerState ${internalOnlinePlayerState} lastProcessedIndex $lastProcessedIndex player.currentMediaItemIndex ${player.currentMediaItemIndex}")

                        val queueLoopType = preferences.getEnum(
                            queueLoopTypeKey,
                            defaultValue = QueueLoopType.Default
                        )

                        when (queueLoopType) {
                            QueueLoopType.RepeatOne -> {
                                internalOnlinePlayer.value?.seekTo(0f)
                                Timber.d("PlayerService initializePositionObserver Repeat: RepeatOne fired")
                            }

                            QueueLoopType.Default -> {
                                val hasNext = binder.player.hasNextMediaItem()
                                Timber.d("PlayerService initializePositionObserver Repeat: Default fired")
                                if (hasNext) {
                                    lastProcessedIndex = player.currentMediaItemIndex
                                    handleSkipToNext()
                                    Timber.d("PlayerService initializePositionObserver Repeat: Default fired next")
                                }
                            }

                            QueueLoopType.RepeatAll -> {
                                val hasNext = binder.player.hasNextMediaItem()
                                Timber.d("PlayerService initializePositionObserver Repeat: RepeatAll fired")
                                if (!hasNext) {
                                    binder.player.seekTo(0, 0)
                                    internalOnlinePlayer.value?.play()
                                    Timber.d("PlayerService initializePositionObserver Repeat: RepeatAll fired first")
                                } else {
                                    lastProcessedIndex = player.currentMediaItemIndex
                                    handleSkipToNext()
                                    Timber.d("PlayerService initializePositionObserver Repeat: RepeatAll fired next")
                                }
                            }
                        }
                        delay(500)
                    }

                    delay(200)
                }
            }
        }
    }

    private fun getSystemMediaVolume(): Int {
        return 100 // set to max
//        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
//        val maxMediaVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
//        val minVolume = maxMediaVolume.div(3)
//        val volumeOnlinePlayer =  (((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: minVolume) * 100) / maxMediaVolume)
//            .coerceIn(0, 100)
//        return volumeOnlinePlayer
    }



    open inner class Binder : AndroidBinder() {
        val player: ExoPlayer
            get() = this@PlayerService.player

        val onlinePlayer: YouTubePlayer?
            get() = this@PlayerService.internalOnlinePlayer.value

        val onlinePlayerPlayingState: Boolean
            get() = this@PlayerService.internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING

        val onlinePlayerState: PlayerConstants.PlayerState
            get() = this@PlayerService.internalOnlinePlayerState

        val onlinePlayerCurrentDuration: Float
            get() = this@PlayerService.currentDuration.value

        val onlinePlayerCurrentSecond: Float
            get() = this@PlayerService.currentSecond.value

        val onlinePlayerView: YouTubePlayerView?
            get() = this@PlayerService.internalOnlinePlayerView.value

        val cache: Cache
            get() = this@PlayerService.cache

        val mediaSession
            get() = this@PlayerService.unifiedMediaSession

        val currentMediaItemAsSong: Song?
            get() = this@PlayerService.player.currentMediaItem?.asSong

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider?.listener = listener
        }

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()

            Timber.d("PlayerService startSleepTimer delayMillis $delayMillis")

            timerJob = coroutineScope.timer(delayMillis) {
                Timber.d("PlayerService startSleepTimer stop delay close service")
                val notification = NotificationCompat
                    .Builder(this@PlayerService, SLEEPTIMER_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Self closing timer ended")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .build()

                notificationManager?.notify(SLEEPTIMER_NOTIFICATION_ID, notification)

                stopSelf()
                onDestroy()
                exitProcess(0)
            }
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
                coroutineScope
            ).let {
                isLoadingRadio = true
                radioJob = coroutineScope.launch(Dispatchers.Main) {

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
                        player.addMediaItems(songs.drop(1))
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
            coroutineScope.launch {
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

        @kotlin.OptIn(FlowPreview::class)
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
                    currentSong.debounce(1000).collect(coroutineScope) { updateUnifiedNotification() }
                }
            }

        }

        fun toggleRepeat() {
            player.toggleRepeatMode()
        }

//        fun toggleShuffle() {
//            player.toggleShuffleMode()
//        }

        @kotlin.OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        fun toggleShuffle() {
            player.shuffleModeEnabled = !player.shuffleModeEnabled

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

    class ServiceRestartReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("PlayerService ServiceRestartReceiver onReceive")
            runCatching {
                val intent = context.intent<PlayerService>()
                if (isAtLeastAndroid8)
                    context.startForegroundService(intent)
                else
                    context.startService(intent)
            }.onFailure {
                Timber.e("Failed ServiceRestartReceiver stopService in PlayerService ${it.stackTraceToString()}")
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

    private companion object {
        const val NOTIFICATION_ID = 1001
        val NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.player_notification_channel_id)  //"Player Notification"

        const val SLEEPTIMER_NOTIFICATION_ID = 1002
        val SLEEPTIMER_NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.sleep_timer_notification_channel_id) //"Sleep Timer Notification"


    }


}


