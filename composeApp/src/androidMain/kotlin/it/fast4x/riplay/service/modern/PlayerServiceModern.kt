package it.fast4x.riplay.service.modern

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionToken
import androidx.media3.ui.DefaultMediaDescriptionAdapter
import androidx.media3.ui.PlayerNotificationManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.from
import it.fast4x.riplay.Database
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.R
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.ExoPlayerMinTimeForEvent
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.NotificationType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.extensions.audiovolume.AudioVolumeObserver
import it.fast4x.riplay.extensions.audiovolume.OnAudioVolumeChangedListener
import it.fast4x.riplay.extensions.discord.sendDiscordPresence
import it.fast4x.riplay.models.Event
import it.fast4x.riplay.models.PersistentQueue
import it.fast4x.riplay.models.PersistentSong
import it.fast4x.riplay.models.QueuedMediaItem
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.models.asMediaItem
import it.fast4x.riplay.service.BitmapProvider
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget
import it.fast4x.riplay.ui.widgets.PlayerVerticalWidget
import it.fast4x.riplay.utils.CoilBitmapLoader
import it.fast4x.riplay.utils.TimerJob
import it.fast4x.riplay.utils.YouTubeRadio
import it.fast4x.riplay.utils.activityPendingIntent
import it.fast4x.riplay.utils.audioQualityFormatKey
import it.fast4x.riplay.utils.autoLoadSongsInQueueKey
import it.fast4x.riplay.utils.broadCastPendingIntent
import it.fast4x.riplay.utils.closebackgroundPlayerKey
import it.fast4x.riplay.utils.collect
import it.fast4x.riplay.utils.discordPersonalAccessTokenKey
import it.fast4x.riplay.utils.discoverKey
import it.fast4x.riplay.utils.enableWallpaperKey
import it.fast4x.riplay.utils.encryptedPreferences
import it.fast4x.riplay.utils.exoPlayerMinTimeForEventKey
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.getEnum
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.utils.isDiscordPresenceEnabledKey
import it.fast4x.riplay.utils.isPauseOnVolumeZeroEnabledKey
import it.fast4x.riplay.utils.loudnessBaseGainKey
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.minimumSilenceDurationKey
import it.fast4x.riplay.utils.notificationPlayerFirstIconKey
import it.fast4x.riplay.utils.notificationPlayerSecondIconKey
import it.fast4x.riplay.utils.notificationTypeKey
import it.fast4x.riplay.utils.pauseListenHistoryKey
import it.fast4x.riplay.utils.persistentQueueKey
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.playbackFadeAudioDurationKey
import it.fast4x.riplay.utils.playbackPitchKey
import it.fast4x.riplay.utils.playbackSpeedKey
import it.fast4x.riplay.utils.playbackVolumeKey
import it.fast4x.riplay.utils.preferences
import it.fast4x.riplay.utils.queueLoopTypeKey
import it.fast4x.riplay.utils.resumePlaybackOnStartKey
import it.fast4x.riplay.utils.resumePlaybackWhenDeviceConnectedKey
import it.fast4x.riplay.utils.setGlobalVolume
import it.fast4x.riplay.utils.setLikeState
import it.fast4x.riplay.utils.showDownloadButtonBackgroundPlayerKey
import it.fast4x.riplay.utils.showLikeButtonBackgroundPlayerKey
import it.fast4x.riplay.utils.skipMediaOnErrorKey
import it.fast4x.riplay.utils.skipSilenceKey
import it.fast4x.riplay.utils.startFadeAnimator
import it.fast4x.riplay.utils.timer
import it.fast4x.riplay.utils.toggleRepeatMode
import it.fast4x.riplay.utils.toggleShuffleMode
import it.fast4x.riplay.utils.volumeNormalizationKey
import it.fast4x.riplay.utils.wallpaperTypeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import it.fast4x.riplay.appContext
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.extensions.connectivity.AndroidConnectivityObserverLegacy
import it.fast4x.riplay.extensions.players.SimplePlayer
import it.fast4x.riplay.isHandleAudioFocusEnabled
import it.fast4x.riplay.isPreCacheEnabled
import it.fast4x.riplay.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.audioReverbPresetKey
import it.fast4x.riplay.utils.bassboostEnabledKey
import it.fast4x.riplay.utils.bassboostLevelKey
import it.fast4x.riplay.utils.isInvincibilityEnabledKey
import it.fast4x.riplay.utils.principalCache
import it.fast4x.riplay.utils.volumeBoostLevelKey
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import android.os.Binder as AndroidBinder


const val LOCAL_KEY_PREFIX = "local:"

@get:OptIn(UnstableApi::class)
val DataSpec.isLocal get() = key?.startsWith(LOCAL_KEY_PREFIX) == true

val MediaItem.isLocal get() = mediaId.startsWith(LOCAL_KEY_PREFIX)
val Song.isLocal get() = id.startsWith(LOCAL_KEY_PREFIX)

@UnstableApi
class PlayerServiceModern : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnAudioVolumeChangedListener {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback
    private lateinit var sessionToken: SessionToken
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    lateinit var player: ExoPlayer
    val cache: SimpleCache by lazy {
        principalCache.getInstance(this)
    }
    lateinit var downloadCache: SimpleCache
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    private lateinit var bitmapProvider: BitmapProvider
    private var volumeNormalizationJob: Job? = null
    private var isPersistentQueueEnabled: Boolean = false
    private var isclosebackgroundPlayerEnabled = false
    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private lateinit var downloadListener: DownloadManager.Listener

    var loudnessEnhancer: LoudnessEnhancer? = null
    private var binder = Binder()
    private var bassBoost: BassBoost? = null
    private var reverbPreset: PresetReverb? = null
    private var showLikeButton = true
    private var showDownloadButton = true

    lateinit var audioQualityFormat: AudioQualityFormat
    lateinit var sleepTimer: SleepTimer
    private var timerJob: TimerJob? = null
    private var radio: YouTubeRadio? = null

    val currentMediaItem = MutableStateFlow<MediaItem?>(null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private val currentSong = currentMediaItem.flatMapLatest { mediaItem ->
        Database.song(mediaItem?.mediaId)
    }.stateIn(coroutineScope, SharingStarted.Lazily, null)

    var currentSongStateDownload = MutableStateFlow(Download.STATE_STOPPED)

    lateinit var connectivityObserver: AndroidConnectivityObserverLegacy
    private val isNetworkAvailable = MutableStateFlow(true)
    private val waitingForNetwork = MutableStateFlow(false)

    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    private var notificationManager: NotificationManager? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var notificationActionReceiver: NotificationActionReceiver

    @kotlin.OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // Enable Android Auto if disabled, REQUIRE ENABLING DEV MODE IN ANDROID AUTO
//        val component = ComponentName(this, PlayerServiceModern::class.java)
//        packageManager.setComponentEnabledSetting(
//            component,
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP
//        )

        try {
            connectivityObserver.unregister()
        } catch (e: Exception) {
            // isn't registered
        }
        connectivityObserver = AndroidConnectivityObserverLegacy(this@PlayerServiceModern)
        coroutineScope.launch {
            connectivityObserver.networkStatus.collect { isAvailable ->
                isNetworkAvailable.value = isAvailable
                Timber.d("PlayerServiceModern network status: $isAvailable")
                println("PlayerServiceModern network status: $isAvailable")
                if (isAvailable && waitingForNetwork.value) {
                    waitingForNetwork.value = false
                    withContext(Dispatchers.Main) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        val notificationType = preferences.getEnum(notificationTypeKey, NotificationType.Default)
        when(notificationType){
            NotificationType.Default -> {
                // DEFAULT NOTIFICATION PROVIDER
                //        setMediaNotificationProvider(
                //            DefaultMediaNotificationProvider(
                //                this,
                //                { NotificationId },
                //                NotificationChannelId,
                //                R.string.player
                //            )
                //            .apply {
                //                setSmallIcon(R.drawable.app_icon)
                //            }
                //        )

                // DEFAULT NOTIFICATION PROVIDER MODDED
                setMediaNotificationProvider(CustomMediaNotificationProvider(this)
                    .apply {
                        setSmallIcon(R.drawable.app_icon)
                    }
                )
            }
            NotificationType.Advanced -> {
                // CUSTOM NOTIFICATION PROVIDER -> CUSTOM NOTIFICATION PROVIDER WITH ACTIONS AND PENDING INTENT
                // ACTUALLY NOT STABLE
                setMediaNotificationProvider(object : MediaNotification.Provider{
                    override fun createNotification(
                        mediaSession: MediaSession,
                        customLayout: ImmutableList<CommandButton>,
                        actionFactory: MediaNotification.ActionFactory,
                        onNotificationChangedCallback: MediaNotification.Provider.Callback
                    ): MediaNotification {
                        return updateCustomNotification(mediaSession)
                    }

                    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean { return false }
                })
            }
        }

        runCatching {
            bitmapProvider = BitmapProvider(
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) Color.BLACK else Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("Failed init bitmap provider in PlayerService ${it.stackTraceToString()}")
        }

        preferences.registerOnSharedPreferenceChangeListener(this)

        val preferences = preferences
        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, true)

        audioQualityFormat = preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)
        showLikeButton = preferences.getBoolean(showLikeButtonBackgroundPlayerKey, true)
        showDownloadButton = preferences.getBoolean(showDownloadButtonBackgroundPlayerKey, true)

        //downloadCache = MyDownloadHelper.getDownloadCache(applicationContext) as SimpleCache


        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRendersFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
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
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, // 50000
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS, // 50000
                        5000,
                        10000
                    ).build()
            )
            .build()
            .apply {
                addListener(this@PlayerServiceModern)
                sleepTimer = SleepTimer(coroutineScope, this)
                addListener(sleepTimer)
                addAnalyticsListener(PlaybackStatsListener(false, this@PlayerServiceModern))
            }

        // Force player to add all commands available, prior to android 13
        val forwardingPlayer =
            object : ForwardingPlayer(player) {
                override fun getAvailableCommands(): Player.Commands {
                    return super.getAvailableCommands()
                        .buildUpon()
                        .addAllCommands()
                        //.remove(COMMAND_SEEK_TO_PREVIOUS)
                        //.remove(COMMAND_SEEK_TO_NEXT)
                        .build()
                }
            }

        println("PlayerServiceModern.onCreate called")

        mediaLibrarySessionCallback =
            MediaLibrarySessionCallback(this, Database)
            .apply {
                binder = this@PlayerServiceModern.binder
                toggleLike = ::toggleLike
                toggleRepeat = ::toggleRepeat
                toggleShuffle = ::toggleShuffle
                startRadio = ::startRadio
                callPause = ::callActionPause
                actionSearch = ::actionSearch
            }

        // Build the media library session
        mediaSession =
            MediaLibrarySession.Builder(this, forwardingPlayer, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java)
                            .putExtra("expandPlayerBottomSheet", true),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setBitmapLoader(CoilBitmapLoader(
                    this,
                    coroutineScope,
                    512 * resources.displayMetrics.density.toInt()
                ))
                // Temporary fix for bug in ExoPlayer media3 https://github.com/androidx/media/issues/2192
                // Bug cause refresh ui in android auto when media is playing
                .setPeriodicPositionUpdateEnabled(false)
                .build()

        // Keep a connected controller so that notification works
        sessionToken = SessionToken(this, ComponentName(this, PlayerServiceModern::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.let { if (it.isDone) it.get() }}, MoreExecutors.directExecutor())

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.addListener(this@PlayerServiceModern)
        player.addAnalyticsListener(PlaybackStatsListener(false, this@PlayerServiceModern))

        player.repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        binder.player.playbackParameters = PlaybackParameters(
            preferences.getFloat(playbackSpeedKey, 1f),
            preferences.getFloat(playbackPitchKey, 1f)
        )
        binder.player.volume = preferences.getFloat(playbackVolumeKey, 1f)
        binder.player.setGlobalVolume(binder.player.volume)

        audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)

        notificationActionReceiver = NotificationActionReceiver(player)


        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
            addAction(Action.like.value)
            addAction(Action.download.value)
            addAction(Action.playradio.value)
            addAction(Action.shuffle.value)
            addAction(Action.repeat.value)
            addAction(Action.search.value)
        }

        ContextCompat.registerReceiver(
            this,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        playerNotificationManager = PlayerNotificationManager.Builder(this, NotificationId, NotificationChannelId)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    fun startFg() {
                        runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                            } else {
                                startForeground(notificationId, notification)
                            }
                        }.onFailure {
                            Timber.e("PlayerServiceModern onNotificationPosted startForeground failed ${it.stackTraceToString()}")
                            println("PlayerServiceModern onNotificationPosted startForeground failed ${it.stackTraceToString()}")
                        }

                    }

                    // FG keep alive, thanks to OuterTune for solution
                    if (preferences.getBoolean(isInvincibilityEnabledKey, false)) {
                        startFg()
                    } else {
                        // mimic media3 default behaviour
                        if (player.isPlaying) {
                            startFg()
                        } else {
                            runCatching {
                                if (isAtLeastAndroid7)
                                    stopForeground(notificationId)
                                else
                                    stopForeground(true)
                            }.onFailure {
                                Timber.e("PlayerServiceModern onNotificationPosted stopForeground failed ${it.stackTraceToString()}")
                                println("PlayerServiceModern onNotificationPosted stopForeground failed ${it.stackTraceToString()}")
                            }



                        }
                    }
                }
            })
            .setMediaDescriptionAdapter(DefaultMediaDescriptionAdapter(mediaSession.sessionActivity))
            .build()

        playerNotificationManager.setPlayer(player)
        playerNotificationManager.setSmallIcon(R.drawable.app_icon)
        playerNotificationManager.setMediaSessionToken(mediaSession.platformToken)


        // Ensure that song is updated
        currentSong.debounce(1000).collect(coroutineScope) { song ->
            Timber.d("PlayerServiceModern onCreate currentSong $song")
            println("PlayerServiceModern onCreate currentSong $song")

            Timber.d("PlayerServiceModern onCreate currentSongIsDownloaded ${currentSongStateDownload.value}")
            println("PlayerServiceModern onCreate currentSongIsDownloaded ${currentSongStateDownload.value}")

            updateDefaultNotification()
            withContext(Dispatchers.Main) {
                if (song != null) {
                    updateDiscordPresence()
                }
                updateWidgets()
            }
        }

        restorePlayerQueue()

        resumePlaybackWhenDeviceConnected()

        processBassBoost()

        processReverb()

        /* Queue is saved in events without scheduling it (remove this in future)*/
        // Load persistent queue when start activity and save periodically in background
        if (isPersistentQueueEnabled) {
            resumePlaybackOnStart()
            coroutineScope.launch {
                while (isActive) {
                    delay(30.seconds)
                    savePlayerQueue()
                    Timber.d("PlayerServiceModern onCreate savePersistentQueue")
                    println("PlayerServiceModern onCreate savePersistentQueue")
                }
            }

        }


    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

//    @UnstableApi
//    override fun onUpdateNotification(
//        session: MediaSession,
//        startInForegroundRequired: Boolean,
//    ) {
//        super.onUpdateNotification(session, startInForegroundRequired)
//    }

//    override fun onTrimMemory(level: Int) {
//        super.onTrimMemory(level)
//        savePlayerQueue()
//    }
//
//    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
//        savePlayerQueue()
//    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateDefaultNotification()
//        preferences.edit {
//            putEnum(queueLoopTypeKey, QueueLoopType.from(repeatMode))
//        }
    }



    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        println("PlayerServiceModern onPlaybackStatsReady called ")
        // if pause listen history is enabled, don't register statistic event
        if (preferences.getBoolean(pauseListenHistoryKey, false)) return

        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            Database.asyncTransaction {
                incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }


        val minTimeForEvent =
            preferences.getEnum(exoPlayerMinTimeForEventKey, ExoPlayerMinTimeForEvent.`20s`)

        if (totalPlayTimeMs > minTimeForEvent.ms) {
            Database.asyncTransaction {
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = System.currentTimeMillis(),
                            playTime = totalPlayTimeMs
                        )
                    )
                } catch (e: SQLException) {
                    Timber.e("PlayerServiceModern onPlaybackStatsReady SQLException ${e.stackTraceToString()}")
                }
            }

            updateOnlineHistory(mediaItem)

        }



    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        isclosebackgroundPlayerEnabled = preferences.getBoolean(closebackgroundPlayerKey, false)
        if (isclosebackgroundPlayerEnabled
            //|| !player.shouldBePlaying // also stop if player is not playing
            ) {
            // Some system not stop service when app is closed from task manager
            // This workaround permit to simulate stop service when app is closed from task manager
            // When app is relaunched any error will be thrown
            if (isAtLeastAndroid7)
                stopForeground(STOP_FOREGROUND_REMOVE)
            else stopForeground(true)
            player.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    @UnstableApi
    override fun onDestroy() {

        savePlayerQueue()

        if (!player.isReleased) {
            player.removeListener(this@PlayerServiceModern)
            player.stop()
            player.release()
        }

        //unregisterReceiver(notificationActionReceiver)

//        if (isAtLeastAndroid7)
//            stopForeground(STOP_FOREGROUND_DETACH)
//        else stopForeground(true)

        mediaSession.release()
        cache.release()
        downloadCache.release()
        Database.close()

//        timerJob?.cancel()
//        timerJob = null

        //coroutineScope.cancel()

        super.onDestroy()
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            persistentQueueKey -> if (sharedPreferences != null) {
                isPersistentQueueEnabled =
                    sharedPreferences.getBoolean(key, isPersistentQueueEnabled)
            }

            volumeNormalizationKey, loudnessBaseGainKey, volumeBoostLevelKey -> processNormalizeVolume()

            resumePlaybackWhenDeviceConnectedKey -> resumePlaybackWhenDeviceConnected()

            skipSilenceKey -> if (sharedPreferences != null) {
                player.skipSilenceEnabled = sharedPreferences.getBoolean(key, false)
            }

            queueLoopTypeKey -> {
                player.repeatMode =
                    sharedPreferences?.getEnum(queueLoopTypeKey, QueueLoopType.Default)?.type
                        ?: QueueLoopType.Default.type
            }

            bassboostLevelKey, bassboostEnabledKey -> processBassBoost()
            audioReverbPresetKey -> processReverb()
        }
    }

    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (preferences.getBoolean(isPauseOnVolumeZeroEnabledKey, false)) {
            if (player.isPlaying && currentVolume < 1) {
                binder.callPause {}
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                binder.player.play()
                pausedByZeroVolume = false
            }
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

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.d("PlayerServiceModern onMediaItemTransition mediaItem $mediaItem reason $reason")
        println("PlayerServiceModern onMediaItemTransition mediaItem $mediaItem reason $reason")

        if (player.isPlaying && reason == MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            player.prepare()
            player.play()
        }

        currentMediaItem.update { mediaItem }

        recoverPlaybackError()
        processNormalizeVolume()

        loadFromRadio(reason)

        with(bitmapProvider) {
            var newUriForLoad = binder.player.currentMediaItem?.mediaMetadata?.artworkUri
            if(lastUri == binder.player.currentMediaItem?.mediaMetadata?.artworkUri) {
                newUriForLoad = null
            }

            load(newUriForLoad, {
                updateDefaultNotification()
                updateWidgets()
            })
        }

        if (mediaItem != null) {
            println("PlayerServiceModern onMediaItemTransition call updateOnlineHistory with mediaItem $mediaItem")
            updateOnlineHistory(mediaItem)
        }
    }

//    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
//        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
//            savePlayerQueue()
//        }
//    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateDefaultNotification()
        if (shuffleModeEnabled) {
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val fadeDisabled = preferences.getEnum(
            playbackFadeAudioDurationKey,
            DurationInMilliseconds.Disabled
        ) == DurationInMilliseconds.Disabled
        val duration = preferences.getEnum(
            playbackFadeAudioDurationKey,
            DurationInMilliseconds.Disabled
        ).milliSeconds
        if (isPlaying && !fadeDisabled)
            startFadeAnimator(
                player = binder.player,
                duration = duration,
                fadeIn = true
            )

        //val totalPlayTimeMs = player.totalBufferedDuration.toString()
        //Log.d("mediaEvent","isPlaying "+isPlaying.toString() + " buffered duration "+totalPlayTimeMs)
        //Log.d("mediaItem","onIsPlayingChanged isPlaying $isPlaying audioSession ${player.audioSessionId}")


        super.onIsPlayingChanged(isPlaying)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Timber.e("PlayerServiceModern onPlayerError error code ${error.errorCode} message ${error.message} cause ${error.cause?.cause}")
        println("PlayerServiceModern onPlayerError error code ${error.errorCode} message ${error.message} cause ${error.cause?.cause}")

        val playbackConnectionExeptionList = listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, //primary error code to manage
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )

        // check if error is caused by internet connection
        val isConnectionError = (error.cause?.cause is PlaybackException)
                && (error.cause?.cause as PlaybackException).errorCode in playbackConnectionExeptionList

        if (!isNetworkAvailable.value || isConnectionError) {
            waitingForNetwork.value = true
            SmartMessage(resources.getString(R.string.error_no_internet), context = this )
            return
        }

        val playbackHttpExeptionList = listOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            416 // 416 Range Not Satisfiable
        )

        if (error.errorCode in playbackHttpExeptionList) {
            Timber.e("PlayerServiceModern onPlayerError recovered occurred errorCodeName ${error.errorCodeName} cause ${error.cause?.cause}")
            println("PlayerServiceModern onPlayerError recovered occurred errorCodeName ${error.errorCodeName} cause ${error.cause?.cause}")
            player.pause()
            player.prepare()
            player.play()
            return
        }

//        if (error.errorCode in PlayerErrorsToRemoveCorruptedCache) {
//            Timber.e("PlayerServiceModern onPlayerError delete corrupted resource ${currentMediaItem.value?.mediaId} errorCodeName ${error.errorCodeName}")
//            println("PlayerServiceModern onPlayerError delete corrupted resource ${currentMediaItem.value?.mediaId} errorCodeName ${error.errorCodeName}")
//            currentMediaItem.value?.mediaId?.let {
//                try {
//                    cache.removeResource(it) //try to remove from cache if exists
//                } catch (e: Exception) {
//                    Timber.e("PlayerServiceModern onPlayerError delete corrupted cache resource removeResource ${e.stackTraceToString()}")
//                }
//                try {
//                    downloadCache.removeResource(it) //try to remove from download cache if exists
//                } catch (e: Exception) {
//                    Timber.e("PlayerServiceModern onPlayerError delete corrupted downloadCache resource removeResource ${e.stackTraceToString()}")
//                }
//            }
//            player.stop()
//            player.prepare()
//            player.play()
//            return
//        }

        /*
        if (error.errorCode in PlayerErrorsToSkip) {
            //println("mediaItem onPlayerError recovered occurred 2000 errorCodeName ${error.errorCodeName}")
            player.pause()
            player.prepare()
            player.forceSeekToNext()
            player.play()

            showSmartMessage(
                message = getString(
                    R.string.skip_media_on_notavailable_message,
                ))

            return
        }
         */


        if (!preferences.getBoolean(skipMediaOnErrorKey, false) || !player.hasNextMediaItem())
            return

        val prev = player.currentMediaItem ?: return

        player.playNext()

        showSmartMessage(
            message = getString(
                R.string.skip_media_on_error_message,
                prev.mediaMetadata.title
            )
        )

    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                sendOpenEqualizerIntent()
            } else {
                sendCloseEqualizerIntent()
                if (!player.playWhenReady) {
                    waitingForNetwork.value = false
                }
            }
        }

        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaItem.value = player.currentMediaItem
        }
    }

    private fun updateOnlineHistory(mediaItem: MediaItem) {
        if (preferences.getBoolean(pauseListenHistoryKey, false)) return

        println("PlayerServiceModern updateOnlineHistory called with mediaItem $mediaItem")

        if (!mediaItem.isLocal && isYouTubeSyncEnabled()) {
            CoroutineScope(Dispatchers.IO).launch {
                SimplePlayer.playerResponseForMetadata(mediaItem.mediaId)
                    .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ?.let { playbackUrl ->
                        println("PlayerServiceModern updateOnlineHistory addPlaybackToHistory playbackUrl $playbackUrl")
                        EnvironmentExt.addPlaybackToHistory(null, playbackUrl)
                            .onFailure {
                                Timber.e("PlayerServiceModern updateOnlineHistory addPlaybackToHistory ${it.stackTraceToString()}")
                                println("PlayerServiceModern updateOnlineHistory addPlaybackToHistory ${it.stackTraceToString()}")
                            }
                    }
            }
        }
    }

    private fun recoverPlaybackError() {
        if (player.playerError != null) {
            player.prepare()
        }
    }

    private fun loadFromRadio(reason: Int) {
        if (!preferences.getBoolean(autoLoadSongsInQueueKey, true)) return
        /*
        // Old feature add songs only if radio is started by user and when last song in player is played
        radio?.let { radio ->
            if (player.mediaItemCount - player.currentMediaItemIndex == 1) {
                coroutineScope.launch(Dispatchers.Main) {
                    player.addMediaItems(radio.process())
                }
            }
        }

         */
        val isDiscoverEnabled = applicationContext.preferences.getBoolean(discoverKey, false)
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= if (
                isDiscoverEnabled) 10 else 3
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

    private fun processBassBoost() {
        if (!preferences.getBoolean(bassboostEnabledKey, false)) {
            runCatching {
                bassBoost?.enabled = false
                bassBoost?.release()
            }
            bassBoost = null
            processNormalizeVolume()
            return
        }

        runCatching {
            if (bassBoost == null) bassBoost = BassBoost(0, player.audioSessionId)
            val bassboostLevel =
                (preferences.getFloat(bassboostLevelKey, 0.5f) * 1000f).toInt().toShort()
            Timber.d("PlayerServiceModern processBassBoost bassboostLevel $bassboostLevel")
            println("PlayerServiceModern processBassBoost bassboostLevel $bassboostLevel")
            bassBoost?.enabled = false
            bassBoost?.setStrength(bassboostLevel)
            bassBoost?.enabled = true
        }.onFailure {
            SmartMessage(
                "Can't enable bass boost",
                context = this@PlayerServiceModern
            )
        }
    }

    private fun processReverb() {
        val presetType = preferences.getEnum(audioReverbPresetKey, PresetsReverb.NONE)
        Timber.d("PlayerServiceModern processReverb presetType $presetType")
        println("PlayerServiceModern processReverb presetType $presetType")
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
            if (reverbPreset == null) reverbPreset = PresetReverb(1, player.audioSessionId)

            reverbPreset?.enabled = false
            reverbPreset?.preset = presetType.preset
            reverbPreset?.enabled = true
            reverbPreset?.id?.let { player.setAuxEffectInfo(AuxEffectInfo(it, 1f)) }
        }
    }

    @UnstableApi
    private fun processNormalizeVolume() {
        if (!preferences.getBoolean(volumeNormalizationKey, false)) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            return
        }

        runCatching {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
            }
        }.onFailure {
            Timber.e("PlayerServiceModern processNormalizeVolume load loudnessEnhancer ${it.stackTraceToString()}")
            println("PlayerServiceModern processNormalizeVolume load loudnessEnhancer ${it.stackTraceToString()}")
            return
        }

        val baseGain = preferences.getFloat(loudnessBaseGainKey, 5.00f)
        val volumeBoostLevel = preferences.getFloat(volumeBoostLevelKey, 0f)
        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.IO) {
                fun Float?.toMb() = ((this ?: 0f) * 100).toInt()
                Database.loudnessDb(songId).cancellable().collectLatest { loudnessDb ->
                    val loudnessMb = loudnessDb.toMb().let {
                        if (it !in -2000..2000) {
                            withContext(Dispatchers.IO) {
                                SmartMessage(
                                    "Extreme loudness detected",
                                    context = this@PlayerServiceModern
                                )
                            }

                            0
                        } else it
                    }
                    try {
                        loudnessEnhancer?.setTargetGain(baseGain.toMb() + volumeBoostLevel.toMb() - loudnessMb)
                        loudnessEnhancer?.enabled = true
                    } catch (e: Exception) {
                        Timber.e("PlayerServiceModern processNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                        println("PlayerServiceModern processNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                    }
                }
            }
        }
    }


    @SuppressLint("NewApi")
    private fun resumePlaybackWhenDeviceConnected() {
        if (!isAtLeastAndroid6) return

        if (preferences.getBoolean(resumePlaybackWhenDeviceConnectedKey, false)) {
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
                    Timber.d("PlayerServiceModern onAudioDevicesAdded addedDevices ${addedDevices.map { it.type }}")
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) {
                        Timber.d("PlayerServiceModern onAudioDevicesAdded device known ${addedDevices.map { it.productName }}")
                        player.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = Unit
            }

            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)

        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }

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
    }

    private fun createMediaSourceFactory() = DefaultMediaSourceFactory(
        createSimpleDataSourceFactory( coroutineScope ),
        DefaultExtractorsFactory()
    )

    fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(cache)
//            .setCache(downloadCache)
//            .setUpstreamDataSourceFactory(
//                CacheDataSource
//                    .Factory()
//                    .setCache(cache)
//                    .setUpstreamDataSourceFactory(
//                        DefaultDataSource.Factory(
//                            this,
//                            OkHttpDataSource.Factory(
//                                OkHttpClient
//                                    .Builder()
//                                    .proxy(Environment.proxy)
//                                    .build(),
//                            ),
//                        ),
//                    ),
//            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)


    private fun buildCustomCommandButtons(): MutableList<CommandButton> {
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Download)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        val commandButtonsList = mutableListOf<CommandButton>()
        val firstCommandButton = NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerFirstIcon }
                .map {
                    CommandButton.Builder()
                        .setDisplayName(it.displayName)
                        .setIconResId(
                            it.getStateIcon(
                                it,
                                currentSong.value?.likedAt,
                                currentSongStateDownload.value,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }
        }

        val secondCommandButton =  NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerSecondIcon }
                .map {
                    CommandButton.Builder()
                        .setDisplayName(it.displayName)
                        .setIconResId(
                            it.getStateIcon(
                                it,
                                currentSong.value?.likedAt,
                                currentSongStateDownload.value,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }
        }

        val otherCommandButtons = NotificationButtons.entries.let { buttons ->
            buttons
                .filterNot { it == notificationPlayerFirstIcon || it == notificationPlayerSecondIcon }
                .map {
                    CommandButton.Builder()
                        .setDisplayName(it.displayName)
                        .setIconResId(
                            it.getStateIcon(
                                it,
                                currentSong.value?.likedAt,
                                currentSongStateDownload.value,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }
        }

        commandButtonsList += firstCommandButton + secondCommandButton + otherCommandButtons

        return commandButtonsList
    }

    private fun updateCustomNotification(session: MediaSession): MediaNotification {

        val playIntent = Action.play.pendingIntent
        val pauseIntent = Action.pause.pendingIntent
        val nextIntent = Action.next.pendingIntent
        val prevIntent = Action.previous.pendingIntent

        val mediaMetadata = player.mediaMetadata

        bitmapProvider.load(mediaMetadata.artworkUri) {}

        val customNotify = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(this, NotificationChannelId)
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle(cleanPrefix(player.mediaMetadata.title.toString()))
            .setContentText(
                if (mediaMetadata.albumTitle != null && mediaMetadata.artist != "")
                    "${mediaMetadata.artist} | ${mediaMetadata.albumTitle}"
                else mediaMetadata.artist
            )
            .setSubText(
                if (mediaMetadata.albumTitle != null && mediaMetadata.artist != "")
                    "${mediaMetadata.artist} | ${mediaMetadata.albumTitle}"
                else mediaMetadata.artist
            )
            .setLargeIcon(bitmapProvider.bitmap)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(player.playerError?.let { R.drawable.alert_circle }
                ?: R.drawable.app_icon)
            .setOngoing(false)
            .setContentIntent(activityPendingIntent<MainActivity>(
                flags = PendingIntent.FLAG_UPDATE_CURRENT
            ) {
                putExtra("expandPlayerBottomSheet", true)
            })
            .setDeleteIntent(broadCastPendingIntent<NotificationDismissReceiver>())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .addAction(R.drawable.play_skip_back, "Skip back", prevIntent)
            .addAction(
                if (player.isPlaying) R.drawable.pause else R.drawable.play,
                if (player.isPlaying) "Pause" else "Play",
                if (player.isPlaying) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, "Skip forward", nextIntent)

        //***********************
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Download)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerFirstIcon }
                .map {
                    customNotify.addAction(
                        it.getStateIcon(
                            it,
                            currentSong.value?.likedAt,
                            currentSongStateDownload.value,
                            player.repeatMode,
                            player.shuffleModeEnabled
                        ),
                        it.displayName,
                        it.pendingIntent
                    )
                }
        }

        NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerSecondIcon }
                .map {
                    customNotify.addAction(
                        it.getStateIcon(
                            it,
                            currentSong.value?.likedAt,
                            currentSongStateDownload.value,
                            player.repeatMode,
                            player.shuffleModeEnabled
                        ),
                        it.displayName,
                        it.pendingIntent
                    )
                }
        }

        NotificationButtons.entries.let { buttons ->
            buttons
                .filterNot { it == notificationPlayerFirstIcon || it == notificationPlayerSecondIcon }
                .map {
                    customNotify.addAction(
                        it.getStateIcon(
                            it,
                            currentSong.value?.likedAt,
                            currentSongStateDownload.value,
                            player.repeatMode,
                            player.shuffleModeEnabled
                        ),
                        it.displayName,
                        it.pendingIntent
                    )
                }
        }
        //***********************

        updateWallpaper()

        return MediaNotification(NotificationId, customNotify.build())
    }

    private fun updateWallpaper() {
        val wallpaperEnabled = preferences.getBoolean(enableWallpaperKey, false)
        val wallpaperType = preferences.getEnum(wallpaperTypeKey, WallpaperType.Lockscreen)
        if (isAtLeastAndroid7 && wallpaperEnabled) {
            coroutineScope.launch(Dispatchers.IO) {
                val wpManager = WallpaperManager.getInstance(this@PlayerServiceModern)
                wpManager.setBitmap(bitmapProvider.bitmap, null, true,
                    when (wallpaperType) {
                        WallpaperType.Both -> (FLAG_LOCK or FLAG_SYSTEM)
                        WallpaperType.Lockscreen -> FLAG_LOCK
                        WallpaperType.Home -> FLAG_SYSTEM
                    }
                )
            }
        }
    }

    private fun updateDefaultNotification() {
        coroutineScope.launch(Dispatchers.Main) {
            mediaSession.setCustomLayout( buildCustomCommandButtons() )
        }

    }


    private fun updateDiscordPresence() {
        val isDiscordPresenceEnabled = preferences.getBoolean(isDiscordPresenceEnabledKey, false)
        if (!isDiscordPresenceEnabled || !isAtLeastAndroid81) return

        val discordPersonalAccessToken = encryptedPreferences.getString(
            discordPersonalAccessTokenKey, ""
        )

        runCatching {
            if (!discordPersonalAccessToken.isNullOrEmpty()) {
                player.currentMediaItem?.let {
                    sendDiscordPresence(
                        discordPersonalAccessToken,
                        it,
                        timeStart = if (player.isPlaying)
                            System.currentTimeMillis() - player.currentPosition else 0L,
                        timeEnd = if (player.isPlaying)
                            (System.currentTimeMillis() - player.currentPosition) + player.duration else 0L
                    )
                }
            }
        }.onFailure {
            Timber.e("PlayerService Failed sendDiscordPresence in PlayerService ${it.stackTraceToString()}")
        }
    }


    fun toggleLike() {
        binder.toggleLike()
    }

    fun toggleRepeat() {
        binder.toggleRepeat()
    }

    fun toggleShuffle() {
        binder.toggleShuffle()
    }

    fun startRadio() {
       binder.startRadio()
    }

    fun callActionPause() {
        binder.callPause({})
    }

    private fun showSmartMessage(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Main) {
                SmartMessage(
                    message,
                    type = PopupType.Info,
                    durationLong = true,
                    context = this@PlayerServiceModern
                )
            }
        }
    }

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
                bitmap = bitmapProvider.bitmap,
                player = player
            )
            playerHorizontalWidget.updateInfo(
                context = applicationContext,
                songTitle = songTitle,
                songArtist = songArtist,
                isPlaying = isPlaying,
                bitmap = bitmapProvider.bitmap,
                player = player
            )
        }
    }

    @UnstableApi
    private fun sendOpenEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }


    @UnstableApi
    private fun sendCloseEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    private fun actionSearch() {
        binder.actionSearch()
    }

//    override fun onPositionDiscontinuity(
//        oldPosition: Player.PositionInfo,
//        newPosition: Player.PositionInfo,
//        reason: Int
//    ) {
//        Timber.d("PlayerServiceModern onPositionDiscontinuity oldPosition ${oldPosition.mediaItemIndex} newPosition ${newPosition.mediaItemIndex} reason $reason")
//        println("PlayerServiceModern onPositionDiscontinuity oldPosition ${oldPosition.mediaItemIndex} newPosition ${newPosition.mediaItemIndex} reason $reason")
//        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
//    }

    private fun savePlayerQueue() {
        Timber.d("PlayerServiceModern onCreate savePersistentQueue")
        println("PlayerServiceModern onCreate savePersistentQueue")
        if (!isPersistentQueueEnabled) return
        Timber.d("PlayerServiceModern onCreate savePersistentQueue is enabled, processing")
        println("PlayerServiceModern onCreate savePersistentQueue is enabled, processing")

        CoroutineScope(Dispatchers.Main).launch {
            val mediaItems = player.currentTimeline.mediaItems
            val mediaItemIndex = player.currentMediaItemIndex
            val mediaItemPosition = player.currentPosition

            if (mediaItems.isEmpty()) return@launch


            mediaItems.mapIndexed { index, mediaItem ->
                QueuedMediaItem(
                    mediaItem = mediaItem,
                    position = if (index == mediaItemIndex) mediaItemPosition else null
                )
            }.let { queuedMediaItems ->
                if (queuedMediaItems.isEmpty()) return@let

                Database.asyncTransaction {
                    clearQueue()
                    insert( queuedMediaItems )
                }

                Timber.d("PlayerServiceModern QueuePersistentEnabled Saved queue")
            }

        }
    }

    private fun resumePlaybackOnStart() {
        if(!isPersistentQueueEnabled || !preferences.getBoolean(resumePlaybackOnStartKey, false)) return

        if(!player.isPlaying) {
            player.play()
        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    @UnstableApi
    private fun restorePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        Database.asyncQuery {
            val queuedSong = queue()

            if (queuedSong.isEmpty()) return@asyncQuery

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)

            runBlocking(Dispatchers.Main) {
                player.setMediaItems(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    index,
                    queuedSong[index].position ?: C.TIME_UNSET
                )
                player.prepare()
            }
        }

    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    @UnstableApi
    private fun restoreFromDiskPlayerQueue() {
        //if (!isPersistentQueueEnabled) return
        //Log.d("mediaItem", "QueuePersistentEnabled Restore Initial")

        runCatching {
            filesDir.resolve("persistentQueue.data").inputStream().use { fis ->
                ObjectInputStream(fis).use { oos ->
                    oos.readObject() as PersistentQueue
                }
            }
        }.onSuccess { queue ->
            //Log.d("mediaItem", "QueuePersistentEnabled Restored queue $queue")
            //Log.d("mediaItem", "QueuePersistentEnabled Restored ${queue.songMediaItems.size}")
            runBlocking(Dispatchers.Main) {
                player.setMediaItems(
                    queue.songMediaItems.map { song ->
                        song.asMediaItem.buildUpon()
                            .setUri(song.asMediaItem.mediaId)
                            .setCustomCacheKey(song.asMediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    queue.mediaItemIndex,
                    queue.position
                )

                player.prepare()

            }

        }.onFailure {
            //it.printStackTrace()
            Timber.e(it.stackTraceToString())
        }

        //Log.d("mediaItem", "QueuePersistentEnabled Restored ${player.currentTimeline.mediaItems.size}")

    }

    private fun saveToDiskPlayerQueue() {

        //if (!isPersistentQueueEnabled) return
        //Log.d("mediaItem", "QueuePersistentEnabled Save ${player.currentTimeline.mediaItems.size}")

        val persistentQueue = PersistentQueue(
            title = "title",
            songMediaItems = player.currentTimeline.mediaItems.map {
                PersistentSong(
                    id = it.mediaId,
                    title = it.mediaMetadata.title.toString(),
                    durationText = it.mediaMetadata.extras?.getString("durationText").toString(),
                    thumbnailUrl = it.mediaMetadata.artworkUri.toString()
                )
            },
            mediaItemIndex = player.currentMediaItemIndex,
            position = player.currentPosition
        )

        runCatching {
            filesDir.resolve("persistentQueue.data").outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistentQueue)
                }
            }
        }.onFailure {
            //it.printStackTrace()
            Timber.e(it.stackTraceToString())

        }.onSuccess {
            Log.d("mediaItem", "QueuePersistentEnabled Saved $persistentQueue")
        }

    }

//    fun updateDownloadedState() {
//        if (currentSong.value == null) return
//        val mediaId = currentSong.value!!.id
//        val downloads = MyDownloadHelper.downloads.value
//        currentSongStateDownload.value = downloads[mediaId]?.state ?: Download.STATE_STOPPED
//        /*
//        if (downloads[currentSong.value?.id]?.state == Download.STATE_COMPLETED) {
//            currentSongIsDownloaded.value = true
//        } else {
//            currentSongIsDownloaded.value = false
//        }
//        */
//        Timber.d("PlayerServiceModern updateDownloadedState downloads count ${downloads.size} currentSongIsDownloaded ${currentSong.value?.id}")
//        println("PlayerServiceModern updateDownloadedState downloads count ${downloads.size} currentSongIsDownloaded ${currentSong.value?.id}")
//        updateDefaultNotification()
//
//    }

    /**
     * This method should ONLY be called when the application (sc. activity) is in the foreground!
     */
    fun restartForegroundOrStop() {
        binder.restartForegroundOrStop()
    }

    @UnstableApi
    class CustomMediaNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {
        override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence? {
            val customMetadata = MediaMetadata.Builder()
                .setTitle(cleanPrefix(metadata.title?.toString() ?: ""))
                .build()
            return super.getNotificationContentTitle(customMetadata)
        }

//        override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
//            val customMetadata = MediaMetadata.Builder()
//                .setArtist(cleanPrefix(metadata.artist?.toString() ?: ""))
//                .build()
//            return super.getNotificationContentText(customMetadata)
//        }
    }


    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            kotlin.runCatching {
//                context.stopService(context.intent<MyDownloadService>())
//            }.onFailure {
//                Timber.e("Failed NotificationDismissReceiver stopService in PlayerServiceModern (MyDownloadService) ${it.stackTraceToString()}")
//            }
            kotlin.runCatching {
                context.stopService(context.intent<PlayerServiceModern>())
            }.onFailure {
                Timber.e("Failed NotificationDismissReceiver stopService in PlayerServiceModern (PlayerServiceModern) ${it.stackTraceToString()}")
            }
        }
    }

    inner class NotificationActionReceiver(private val player: Player) : BroadcastReceiver() {


        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Action.pause.value -> binder.callPause({ player.pause() } )
                Action.play.value -> player.play()
                Action.next.value -> player.playNext()
                Action.previous.value -> player.playPrevious()
                Action.like.value -> {
                    binder.toggleLike()
                }


                Action.playradio.value -> {
                    binder.stopRadio()
                    binder.playRadio(NavigationEndpoint.Endpoint.Watch(videoId = binder.player.currentMediaItem?.mediaId))
                }

                Action.shuffle.value -> {
                    binder.toggleShuffle()
                }

                Action.search.value -> {
                    binder.actionSearch()
                }

                Action.repeat.value -> {
                    binder.toggleRepeat()
                }


            }

        }

    }

    open inner class Binder : AndroidBinder() {
        val service: PlayerServiceModern
            get() = this@PlayerServiceModern

        /*
        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        */
        val bitmap: Bitmap
            get() = bitmapProvider.bitmap


        val player: ExoPlayer
            get() = this@PlayerServiceModern.player

        val cache: Cache
            get() = this@PlayerServiceModern.cache

        val downloadCache: Cache
            get() = this@PlayerServiceModern.downloadCache

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()



            timerJob = coroutineScope.timer(delayMillis) {
                val notification = NotificationCompat
                    .Builder(this@PlayerServiceModern, SleepTimerNotificationChannelId)
                    .setContentTitle(getString(R.string.sleep_timer_ended))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .build()

                notificationManager?.notify(SleepTimerNotificationId, notification)

                stopSelf()
                exitProcess(0)
            }
        }

        fun cancelSleepTimer() {
            timerJob?.cancel()
            timerJob = null
        }

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set


        @UnstableApi
        private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean, filterArtist: String = "") {
            radioJob?.cancel()
            radio = null
            val isDiscoverEnabled = applicationContext.preferences.getBoolean(discoverKey, false)
            YouTubeRadio(
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

                    val songs = if (filterArtist.isEmpty()) it.process()
                    else it.process().filter { song -> song.mediaMetadata.artist == filterArtist }

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

        @UnstableApi
        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, filterArtist: String = "") =
            startRadio(endpoint = endpoint, justAdd = true, filterArtist = filterArtist)

        @UnstableApi
        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = false)

        fun callPause(onPause: () -> Unit) {
            val fadeDisabled = preferences.getEnum(
                playbackFadeAudioDurationKey,
                DurationInMilliseconds.Disabled
            ) == DurationInMilliseconds.Disabled
            val duration = preferences.getEnum(
                playbackFadeAudioDurationKey,
                DurationInMilliseconds.Disabled
            ).milliSeconds
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

        /**
         * This method should ONLY be called when the application (sc. activity) is in the foreground!
         */
        fun restartForegroundOrStop() {
            binder.callPause({ player.pause() } )
            stopSelf()
        }

        @kotlin.OptIn(FlowPreview::class)
        fun toggleLike() {
            Database.asyncTransaction {
                currentSong.value?.let {
                    like(
                        it.id,
                        setLikeState(it.likedAt)
                    )
                }.also {
                    currentSong.debounce(1000).collect(coroutineScope) { updateDefaultNotification() }
                }
            }

//            currentSong.value
//                ?.let { MyDownloadHelper.autoDownloadWhenLiked(this@PlayerServiceModern, it.asMediaItem) }
        }


        fun toggleRepeat() {
            player.toggleRepeatMode()
            updateDefaultNotification()
        }

        fun toggleShuffle() {
            player.toggleShuffleMode()
            updateDefaultNotification()
        }

        fun startRadio() {
            binder.stopRadio()
            binder.playRadio(NavigationEndpoint.Endpoint.Watch(videoId = binder.player.currentMediaItem?.mediaId))
        }

        fun actionSearch() {
            startActivity(Intent(applicationContext, MainActivity::class.java)
                .setAction(MainActivity.action_search)
                .setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK))
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

            val pause = Action("it.fast4x.rimusic.pause")
            val play = Action("it.fast4x.rimusic.play")
            val next = Action("it.fast4x.rimusic.next")
            val previous = Action("it.fast4x.rimusic.previous")
            val like = Action("it.fast4x.rimusic.like")
            val download = Action("it.fast4x.rimusic.download")
            val playradio = Action("it.fast4x.rimusic.playradio")
            val shuffle = Action("it.fast4x.rimusic.shuffle")
            val search = Action("it.fast4x.rimusic.search")
            val repeat = Action("it.fast4x.rimusic.repeat")

        }
    }

    companion object {
        const val NotificationId = 1001
        const val NotificationChannelId = "default_channel_id"

        const val SleepTimerNotificationId = 1002
        const val SleepTimerNotificationChannelId = "sleep_timer_channel_id"

        const val ChunkLength = 512 * 1024L

//        val PlayerErrorsToReload = arrayOf(
//            416,
//            //4003, // ERROR_CODE_DECODING_FAILED
//        )
//
//        val PlayerErrorsToRemoveCorruptedCache = arrayOf(
//
//            2000, // ERROR_CODE_IO_UNSPECIFIED
//            2003, // ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE
//            2004, // ERROR_CODE_IO_BAD_HTTP_STATUS
//            2005, // ERROR_CODE_IO_FILE_NOT_FOUND
//            2008 // ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
//        )


        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val SEARCHED = "searched"

    }

}