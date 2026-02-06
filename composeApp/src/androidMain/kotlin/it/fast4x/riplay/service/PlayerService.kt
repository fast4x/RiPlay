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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
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
import it.fast4x.riplay.commonutils.durationTextToMillis
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
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.extensions.encryptedpreferences.encryptedPreferences
import it.fast4x.riplay.extensions.lastfm.sendNowPlaying
import it.fast4x.riplay.extensions.lastfm.sendScrobble
import it.fast4x.riplay.extensions.players.getOnlineMetadata
import it.fast4x.riplay.extensions.preferences.castToRiTuneDeviceEnabledKey
import it.fast4x.riplay.extensions.preferences.excludeSongIfIsVideoKey
import it.fast4x.riplay.extensions.preferences.isEnabledLastfmKey
import it.fast4x.riplay.extensions.preferences.lastfmScrobbleTypeKey
import it.fast4x.riplay.extensions.preferences.lastfmSessionTokenKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.ritune.improved.RiTuneClient
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneConnectionStatus
import it.fast4x.riplay.extensions.ritune.improved.models.RiTunePlayerState
import it.fast4x.riplay.extensions.ritune.improved.models.RiTuneRemoteCommand
import it.fast4x.riplay.service.experimental.AppSharedScope
import it.fast4x.riplay.service.experimental.GlobalQueueViewModel
import it.fast4x.riplay.service.helpers.BluetoothConnectReceiver
import it.fast4x.riplay.service.helpers.EqualizerHelper
import it.fast4x.riplay.service.helpers.NoisyAudioReceiver
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Objects
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import android.os.Binder as AndroidBinder


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

    var legacyNotificationActionReceiver: LegacyNotificationActionReceiver? = null

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

    private var onlineListenedDurationMs = 0L
    private var lastOnlineMediaId: String? = null

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

    private lateinit var equalizerHelper: EqualizerHelper

    private val globalQueueViewModel: GlobalQueueViewModel by lazy {
        ViewModelProvider(AppSharedScope)[GlobalQueueViewModel::class.java]
    }

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
        player.pauseAtEndOfMediaItems = true

        audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)

        equalizerHelper = EqualizerHelper(this)
        equalizerHelper.setup(0)

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

                withContext(Dispatchers.Main) {
                    player.loadMasterQueue(
                        onLoaded = {
                            /* todo improve restore position from saved queue
                            val seconds = it.div(1000)
                            playFromSecond = seconds.toFloat()
                            currentSecond.value = seconds.toFloat()
                            Timber.d("PlayerService onCreate loadMasterQueue playFromSecond $playFromSecond currentSecond ${currentSecond.value} currentDuration ${currentDuration.value} durationText ${currentSong.value?.durationText}")

                             */
                        }
                    )

                    resumePlaybackOnStart()
                }

                while (isActive) {
                    delay(2.minutes)
                    player.saveMasterQueue(currentSecond.value.toInt())

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
            val currentMediaId = song?.id

            if (lastOnlineMediaId != currentMediaId) {
                if(onlineListenedDurationMs > 0) incrementOnlineListenedPlaytimeMs()
                delay(200)
                onlineListenedDurationMs = 0L
                lastOnlineMediaId = currentMediaId
            }

            withContext(Dispatchers.Main) {
                updateUnifiedNotification()
            }
            Timber.d("PlayerService onCreate update currentSong $song mediaItemState ${currentMediaItemState.value}")
        }

        initializeLegacyNotificationActionReceiver()
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

        globalQueueViewModel.linkController(binder)

        coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (localMediaItem?.isLocal == false) {
                    if (internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING) {
                        onlineListenedDurationMs += 1000
                    } else {
                        if (onlineListenedDurationMs > 0) {
                            incrementOnlineListenedPlaytimeMs()
                            delay(200)
                            onlineListenedDurationMs = 0L
                        }
                    }
                    // not required for now
//                    if (currentDuration.value > 0) {
//                        if (currentSecond.value >= currentDuration.value - 0.5f) {
//                            if (internalOnlinePlayerState == PlayerConstants.PlayerState.PLAYING) {
//                                Timber.d("PlayerService Watchdog: End of online track detected by time, forcing playNext()")
//                                withContext(Dispatchers.Main) {
//                                    player.playNext()
//                                }
//
//                            }
//                        }
//                    }
                    Timber.d("PlayerService onCreate onlineListenedDurationMs $onlineListenedDurationMs")
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

    private fun startForeground() {
        startForeground(NOTIFICATION_ID,notification())
        //runCatching {
//            notification().let {
//                ServiceCompat.startForeground(
//                    this@PlayerService,
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
    }

    private fun initializeVariables() {
        // todo add here all val that requires first initialize and add references in shared preferences, so is not nededed restart service when change it
        CoroutineScope(Dispatchers.Main).launch {
            currentMediaItemState.value = player.currentMediaItem
        }
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
                        //handleSkipToNext()
                        player.playNext()
                    }
                }
            }
        }
    }

    private fun initializeRiTune() {

        val isRiTuneEnabled = preferences.getBoolean(castToRiTuneDeviceEnabledKey, false)
        if (!isRiTuneEnabled) return
        Timber.d("PlayerService initializeRiTune isRituneEnabled $isRiTuneEnabled")

        riTuneObserverJob?.cancel()

        var isConnecting = false

        riTuneObserverJob = coroutineScope.launch {

            while (isActive) {

                val connectionStatus = riTuneClient.connectionStatus.value
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


                val playerState = riTuneClient.state.value?.state
                val duration = riTuneClient.state.value?.duration
                val second = riTuneClient.state.value?.currentTime

                if (isCastActive) {
                    withContext(Dispatchers.Main) {
                        internalOnlinePlayerState =
                            playerState ?: PlayerConstants.PlayerState.UNSTARTED
                        if (duration != null) {
                            currentDuration.value = duration
                        }

                        if (second != null) {
                            currentSecond.value = second
                        }
                        Timber.d("PlayerService initializeRiTune Loop - CastActive PlayerState $playerState, duration $duration, second $second")
                    }
                }

                //Timber.d("PlayerService initializeRiTune Loop - CastActive: $isCastActive, Status: $connectionStatus, isConnecting: $isConnecting PlayerState $playerState  ")

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
                    //handleSkipToNext()
                    player.playNext()
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

        initializeUnifiedSessionCallback()

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
                    CoroutineScope(Dispatchers.Main).launch {
                        internalOnlinePlayer.value = youTubePlayer
                    }

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
                    CoroutineScope(Dispatchers.Main).launch {
                        currentSecond.value = second
                        //Timber.d("PlayerService onlinePlayerView: onCurrentSecond $second")
                    }
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    super.onVideoDuration(youTubePlayer, duration)
                    CoroutineScope(Dispatchers.Main).launch {
                        currentDuration.value = duration
                    }
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
                            if (!firstTimeStarted) {
                                if (!GlobalSharedData.riTuneCastActive)
                                    youTubePlayer.play()
                                else
                                    coroutineScope.launch {
                                        riTuneClient.sendCommand(
                                            RiTuneRemoteCommand(
                                                "play",
                                                position = playFromSecond
                                            )
                                        )
                                    }
                            }
                        }
                        PlayerConstants.PlayerState.ENDED -> {
                            Timber.d("PlayerService onlinePlayerView: onStateChange ENDED regular playNext()")
                            player.playNext()
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
                        player.saveMasterQueue(currentSecond.value.toInt())


                    if (!GlobalSharedData.riTuneCastActive)
                        youTubePlayer.pause()
                    else
                        coroutineScope.launch {
                            riTuneClient.sendCommand(
                                RiTuneRemoteCommand(
                                    "pause",
                                    position = playFromSecond
                                )
                            )
                        }

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

                        localMediaItem?.let {
                            if (!GlobalSharedData.riTuneCastActive)
                                youTubePlayer.cueVideo(it.mediaId, playFromSecond)
                            else
                                coroutineScope.launch {
                                    riTuneClient.sendCommand(
                                        RiTuneRemoteCommand(
                                            "load",
                                            mediaId = it.mediaId,
                                            position = playFromSecond
                                        )
                                    )
                                }
                        }
                        //if (checkVolumeLevel)
                        youTubePlayer.setVolume(getSystemMediaVolume())
                        return
                    }

                    lastError = error

                    if (!isSkipMediaOnErrorEnabled()) return
                    val prev = binder.player.currentMediaItem ?: return

                    //binder.player.playNext()
                    //handleSkipToNext()
                    player.playNext()

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

    private fun initializeLegacyNotificationActionReceiver() {

        legacyNotificationActionReceiver = LegacyNotificationActionReceiver()

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
            legacyNotificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun updateDiscordPresence() {
        if (!isAtLeastAndroid81) return

        Timber.d("PlayerService UpdateDiscordPresence")
        currentSong.value?.asMediaItem?.let{
            Timber.d("PlayerService UpdateDiscordPresence inside isLocal ${it.isLocal}")
            if (!it.isLocal) {
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
        player.saveMasterQueue(currentSecond.value.toInt())
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

        player.saveMasterQueue(currentSecond.value.toInt())

        try {
            unregisterReceiver(legacyNotificationActionReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver ${e.stackTraceToString()}")
        }

        if (this::unifiedMediaSession.isInitialized) {
            unifiedMediaSession.isActive = false
            unifiedMediaSession.release()
        }

        if(this::equalizerHelper.isInitialized) {
            equalizerHelper.release()
        }

        try {
            player.removeListener(this)
            player.release()
        } catch (e: Exception) {
            Timber.e("PlayerService Error in local player release: ${e.message}")
        }

        try {
            CoroutineScope(Dispatchers.Main).launch {
                internalOnlinePlayer.value = null
            }
            internalOnlinePlayerView.value.release()
        } catch (e: Exception) {
            Timber.e("PlayerService Error in online player release: ${e.message}")
        }


        runCatching {

            preferences.unregisterOnSharedPreferenceChangeListener(this)

            cache.release()
            loudnessEnhancer?.release()
            audioVolumeObserver.unregister()
            noisyReceiver?.unregister()
            bluetoothReceiver?.unregister()

            discordPresenceManager?.onStop()

            coroutineScope.launch { delay(500) }

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

        // if pause listen history is enabled or mediaitem is not local, don't register statistic event
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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @UnstableApi
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

        if (mediaItem == null) return

//        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
//            Timber.d("PlayerService: MediaItem transition ignored (Reason: Playlist Changed)")
//            return
//        }

        val newMediaId = mediaItem.mediaId

        if (lastOnlineMediaId == newMediaId) {
            Timber.d("PlayerService: Transition ignored, same MediaID ($newMediaId) skipped")
            CoroutineScope(Dispatchers.Main).launch {
                binder.player.playNext()
            }
            //return
        }

        //Timber.d("PlayerService: MediaItem transition executed (Reason: $reason)")

        //Timber.d("PlayerService onMediaItemTransition RiTune Devices ${GlobalSharedData.riTuneDevices}")

        startForeground()



        Timber.d("PlayerService onMediaItemTransition mediaItem ${mediaItem.mediaId} reason $reason")

        currentQueuePosition = player.currentMediaItemIndex
        CoroutineScope(Dispatchers.Main).launch {
            currentSecond.value = 0F
        }

        if (parentalControlEnabled && mediaItem.isExplicit) {
            //handleSkipToNext()
            player.playNext()
            SmartMessage(resources.getString(R.string.error_message_parental_control_restricted), context = this@PlayerService)
            return
        }

        if (excludeIfIsVideoEnabled && mediaItem.isVideo) {
            //handleSkipToNext()
            player.playNext()
            SmartMessage(getString(R.string.warning_skipped_video), context = this@PlayerService)
            return
        }

        var blacklisted by mutableStateOf(false)
        runBlocking(Dispatchers.IO) {
            blacklisted = Database.blacklisted(mediaItem.mediaId) > 0
        }
        if (blacklisted) {
            //handleSkipToNext()
            player.playNext()
            SmartMessage(getString(R.string.warning_skipped_blacklisted_song), context = this@PlayerService)
            return
        }

        mediaItem.let {
            CoroutineScope(Dispatchers.Main).launch {
                currentMediaItemState.value = it
            }
            localMediaItem = it

            if (!it.isLocal){

                Timber.d("PlayerService onMediaItemTransition system volume ${getSystemMediaVolume()}")

                if (!GlobalSharedData.riTuneCastActive)
                    internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)
                else
                    coroutineScope.launch {
                        riTuneClient.sendCommand(
                            RiTuneRemoteCommand(
                                "load",
                                mediaId = it.mediaId,
                                position = playFromSecond
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


//        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
//            updateMediaSessionQueue(player.currentTimeline)
//        }

        maybeRecoverPlaybackError()
        initializeNormalizeVolume()
        maybeProcessRadio(reason)

        updateUnifiedNotification()

        updateDiscordPresence()

        player.saveMasterQueue(currentSecond.value.toInt())

        if (preferences.getBoolean(isEnabledLastfmKey, false)) {
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
        val isLowMemory = level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        Timber.d("PlayerService onTrimMemory level $level isLowMemory $isLowMemory")
        if (isLowMemory)
            player.saveMasterQueue(currentSecond.value.toInt())
    }


    fun updateUnifiedNotification() {
        coroutineScope.launch {
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
                .setTitle(mediaItem.mediaMetadata.title)
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
                        if (!GlobalSharedData.riTuneCastActive) {
                            internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)
                            //internalOnlinePlayer.value?.loadVideo(it.mediaId, playFromSecond)

                            internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())
                        } else {
                            coroutineScope.launch {
                                riTuneClient.sendCommand(
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
                            player.addMediaItems(player.currentMediaItemIndex + 1,radio.process())
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
            if (!GlobalSharedData.riTuneCastActive)
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

    inner class LegacyNotificationActionReceiver() : BroadcastReceiver() {

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
                        if (!GlobalSharedData.riTuneCastActive)
                            internalOnlinePlayer.value?.pause()
                        else
                            coroutineScope.launch {
                                riTuneClient.sendCommand(
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
                            if (!GlobalSharedData.riTuneCastActive)
                                internalOnlinePlayer.value?.play()
                            else
                                coroutineScope.launch {
                                    riTuneClient.sendCommand(
                                        RiTuneRemoteCommand(
                                            "play",
                                            position = playFromSecond
                                        )
                                    )
                                }
                        }
                    }
                    Action.next.value -> player.playNext() //handleSkipToNext() //it.player.playNext()
                    Action.previous.value -> player.playPrevious() //handleSkipToPrevious()
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

                            if(!GlobalSharedData.riTuneCastActive)
                                internalOnlinePlayer.value?.play()
                            else
                                coroutineScope.launch {
                                    riTuneClient.sendCommand(
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


    fun notification(): Notification {

        val currentMediaItem = binder.player.currentMediaItem

        //bitmapProvider?.load(currentMediaItem?.mediaMetadata?.artworkUri) {}

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
//        val songTitle = player.mediaMetadata.title.toString()
//        val songArtist = player.mediaMetadata.artist.toString()
        val isPlaying = (isPlayingNow || player.isPlaying)
        coroutineScope.launch {
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

        //Increment playtime of song and add event in the database for online songs

        currentSong.value?.id?.let { mediaId ->
            if (currentSecond.value > 5) {
                Timber.d("PlayerService incrementOnlineListenedPlaytimeMs INCREMENT totalPlayTimeMs $onlineListenedDurationMs mediaItem ${currentSong.value?.id}")
                Database.asyncTransaction {
                    Database.incrementTotalPlayTimeMs(mediaId, onlineListenedDurationMs)
                }
            }

            val minTimeForEvent =
                preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)

            if (currentSecond.value > minTimeForEvent.seconds) {
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

    private fun initializePositionObserver() {
        positionObserverJob?.cancel()
        positionObserverJob = coroutineScope.launch {

            var lastProcessedIndex: Int? = null

            while (isActive) {

                withContext(Dispatchers.Main) {

                    //updateWidgets()

                    //Timber.d("PlayerService initializePositionObserver BEFORE player.playbackState ${player.playbackState} internalOnlinePlayerState ${internalOnlinePlayerState} lastProcessedIndex $lastProcessedIndex player.currentMediaItemIndex ${player.currentMediaItemIndex}")

//                    if (player.currentMediaItem?.isLocal == false)
//                        player.pauseAtEndOfMediaItems = true else player.pauseAtEndOfMediaItems = false

                    if (player.currentMediaItem?.isLocal == false && (player.playbackState == Player.STATE_ENDED || internalOnlinePlayerState == PlayerConstants.PlayerState.ENDED)
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
                                /*
                                val hasNext = binder.player.hasNextMediaItem()
                                Timber.d("PlayerService initializePositionObserver Repeat: Default fired")
                                if (hasNext) {
                                    lastProcessedIndex = player.currentMediaItemIndex
                                    //handleSkipToNext()
                                    player.playNext()
                                    Timber.d("PlayerService initializePositionObserver Repeat: Default fired next")
                                }
                                 */
                            }

                            QueueLoopType.RepeatAll -> {
                                val hasNext = binder.player.hasNextMediaItem()
                                Timber.d("PlayerService initializePositionObserver Repeat: RepeatAll fired")
                                if (!hasNext) {
                                    binder.player.seekTo(0, 0)
                                    if (!GlobalSharedData.riTuneCastActive)
                                        internalOnlinePlayer.value?.play()
                                    else
                                        coroutineScope.launch {
                                            riTuneClient.sendCommand(
                                                RiTuneRemoteCommand(
                                                    "play",
                                                    position = playFromSecond
                                                )
                                            )
                                        }

                                    Timber.d("PlayerService initializePositionObserver Repeat: RepeatAll fired first")
                                }
                                /*
                                else {
                                    lastProcessedIndex = player.currentMediaItemIndex
                                    //handleSkipToNext()
                                    player.playNext()
                                    Timber.d("PlayerService initializePositionObserver Repeat: RepeatAll fired next")
                                }
                                 */
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

        val riTuneClient: RiTuneClient
            @Synchronized
            get() = this@PlayerService.riTuneClient

        val equalizer: EqualizerHelper
            get() = this@PlayerService.equalizerHelper

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

//        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
//            bitmapProvider?.listener = listener
//        }

        val bitmap: Bitmap?
            get() = this@PlayerService.bitmapProvider?.bitmap

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

                player.saveMasterQueue(currentSecond.value.toInt())

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
                        player.addMediaItems(player.currentMediaItemIndex + 1, songs.drop(1))
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

    fun initializeUnifiedSessionCallback() {
        Timber.d("PlayerService InitializeUnifiedSessionCallback")
        val currentMediaItem = binder.player.currentMediaItem
        val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
        binder.let {
            unifiedMediaSession.setCallback(
                PlayerMediaSessionCallback(
                    binder = it,
                    onPlayClick = {
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onPlayClick")
                        if (player.currentMediaItem?.isLocal == true)
                            it.player.play()
                        else {
                            if (!GlobalSharedData.riTuneCastActive)
                                internalOnlinePlayer.value?.play()
                            else
                                coroutineScope.launch {
                                    riTuneClient.sendCommand(
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
                        if (!GlobalSharedData.riTuneCastActive) {
                            internalOnlinePlayer.value?.pause()
                        } else {
                            coroutineScope.launch {
                                riTuneClient.sendCommand(
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
                        if (!GlobalSharedData.riTuneCastActive)
                            internalOnlinePlayer.value?.seekTo(newPosition)
                        else
                            coroutineScope.launch {
                                riTuneClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "seek",
                                        //mediaId = item.mediaId,
                                        position = newPosition
                                    )
                                )
                            }
                        CoroutineScope(Dispatchers.Main).launch {
                            currentSecond.value = second.toFloat()
                        }
                    },
                    onPlayNext = {
                        //it.player.playNext()
                        //handleSkipToNext()
                        player.playNext()
                    },
                    onPlayPrevious = {
                        //handleSkipToPrevious()
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

                                    if(!GlobalSharedData.riTuneCastActive)
                                        internalOnlinePlayer.value?.play()
                                    else
                                        coroutineScope.launch {
                                            riTuneClient.sendCommand(
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
                        //updateUnifiedNotification()
                    }
                )
            )
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


