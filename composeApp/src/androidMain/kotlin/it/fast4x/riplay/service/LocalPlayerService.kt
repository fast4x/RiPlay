package it.fast4x.riplay.service

import android.annotation.SuppressLint
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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import it.fast4x.environment.Environment
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
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.extensions.audiovolume.AudioVolumeObserver
import it.fast4x.riplay.extensions.audiovolume.OnAudioVolumeChangedListener
import it.fast4x.riplay.models.Event
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget
import it.fast4x.riplay.ui.widgets.PlayerVerticalWidget
import it.fast4x.riplay.utils.TimerJob
import it.fast4x.riplay.utils.OnlineRadio
import it.fast4x.riplay.utils.activityPendingIntent
import it.fast4x.riplay.extensions.preferences.audioQualityFormatKey
import it.fast4x.riplay.extensions.preferences.autoLoadSongsInQueueKey
import it.fast4x.riplay.utils.broadCastPendingIntent
import it.fast4x.riplay.extensions.preferences.closebackgroundPlayerKey
import it.fast4x.riplay.utils.collect
import it.fast4x.riplay.extensions.preferences.discordPersonalAccessTokenKey
import it.fast4x.riplay.extensions.preferences.discoverKey
import it.fast4x.riplay.extensions.preferences.enableWallpaperKey
import it.fast4x.riplay.extensions.encryptedpreferences.encryptedPreferences
import it.fast4x.riplay.extensions.preferences.exoPlayerMinTimeForEventKey
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.extensions.preferences.isDiscordPresenceEnabledKey
import it.fast4x.riplay.extensions.preferences.isPauseOnVolumeZeroEnabledKey
import it.fast4x.riplay.extensions.preferences.loudnessBaseGainKey
import it.fast4x.riplay.extensions.preferences.minimumSilenceDurationKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerFirstIconKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerSecondIconKey
import it.fast4x.riplay.extensions.preferences.pauseListenHistoryKey
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.extensions.preferences.playbackFadeAudioDurationKey
import it.fast4x.riplay.extensions.preferences.playbackPitchKey
import it.fast4x.riplay.extensions.preferences.playbackSpeedKey
import it.fast4x.riplay.extensions.preferences.playbackVolumeKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.resumePlaybackOnStartKey
import it.fast4x.riplay.extensions.preferences.resumePlaybackWhenDeviceConnectedKey
import it.fast4x.riplay.utils.setGlobalVolume
import it.fast4x.riplay.utils.setLikeState
import it.fast4x.riplay.extensions.preferences.showLikeButtonBackgroundPlayerKey
import it.fast4x.riplay.extensions.preferences.skipMediaOnErrorKey
import it.fast4x.riplay.extensions.preferences.skipSilenceKey
import it.fast4x.riplay.utils.startFadeAnimator
import it.fast4x.riplay.utils.timer
import it.fast4x.riplay.utils.toggleRepeatMode
import it.fast4x.riplay.utils.toggleShuffleMode
import it.fast4x.riplay.extensions.preferences.volumeNormalizationKey
import it.fast4x.riplay.extensions.preferences.wallpaperTypeKey
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
import kotlinx.coroutines.withContext
import it.fast4x.riplay.appContext
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.extensions.discord.DiscordPresenceManager
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOfflinePlayer
import it.fast4x.riplay.isHandleAudioFocusEnabled
import it.fast4x.riplay.extensions.preferences.audioReverbPresetKey
import it.fast4x.riplay.extensions.preferences.bassboostEnabledKey
import it.fast4x.riplay.extensions.preferences.bassboostLevelKey
import it.fast4x.riplay.extensions.preferences.filterContentTypeKey
import it.fast4x.riplay.extensions.preferences.isInvincibilityEnabledKey
import it.fast4x.riplay.extensions.preferences.lastMediaItemWasLocalKey
import it.fast4x.riplay.utils.loadMasterQueue
import it.fast4x.riplay.utils.principalCache
import it.fast4x.riplay.utils.saveMasterQueue
import it.fast4x.riplay.extensions.preferences.volumeBoostLevelKey
import it.fast4x.riplay.getPlaybackFadeAudioDuration
import it.fast4x.riplay.isPersistentQueueEnabled
import it.fast4x.riplay.utils.BitmapProvider
import it.fast4x.riplay.utils.SleepTimerListener
import it.fast4x.riplay.utils.isOfficialContent
import it.fast4x.riplay.utils.isUserGeneratedContent
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.system.exitProcess
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
class LocalPlayerService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnAudioVolumeChangedListener {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var localPlayerSessionCallback: LocalPlayerSessionCallback
    private lateinit var sessionToken: SessionToken
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    lateinit var player: ExoPlayer
    val cache: SimpleCache by lazy {
        principalCache.getInstance(this)
    }
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    private lateinit var bitmapProvider: BitmapProvider
    private var volumeNormalizationJob: Job? = null

    private var isclosebackgroundPlayerEnabled = false
    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    var loudnessEnhancer: LoudnessEnhancer? = null
    private var binder = Binder()
    private var bassBoost: BassBoost? = null
    private var reverbPreset: PresetReverb? = null
    private var showLikeButton = true

    lateinit var audioQualityFormat: AudioQualityFormat
    lateinit var sleepTimerListener: SleepTimerListener
    private var timerJob: TimerJob? = null
    private var radio: OnlineRadio? = null

    val currentMediaItem = MutableStateFlow<MediaItem?>(null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private val currentSong = currentMediaItem.flatMapLatest { mediaItem ->
        Database.song(mediaItem?.mediaId)
    }.stateIn(coroutineScope, SharingStarted.Lazily, null)

    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    private var notificationManager: NotificationManager? = null
    //private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var notificationActionReceiver: NotificationActionReceiver
    private var discordPresenceManager: DiscordPresenceManager? = null

    @kotlin.OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // reset lastMediaItemstate
        preferences.edit(commit = true) { putBoolean(lastMediaItemWasLocalKey, false) }

        // DEFAULT NOTIFICATION PROVIDER MODDED
        setMediaNotificationProvider(CustomMediaNotificationProvider(this)
            .apply {
                setSmallIcon(R.drawable.app_icon)
            }
        )


//        setMediaNotificationProvider(object : MediaNotification.Provider{
//            override fun createNotification(
//                mediaSession: MediaSession,
//                customLayout: ImmutableList<CommandButton>,
//                actionFactory: MediaNotification.ActionFactory,
//                onNotificationChangedCallback: MediaNotification.Provider.Callback
//            ): MediaNotification {
//                return updateCustomNotification(mediaSession)
//            }
//
//            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean { return false }
//        })


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


        audioQualityFormat = preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)
        showLikeButton = preferences.getBoolean(showLikeButtonBackgroundPlayerKey, true)


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
                addListener(this@LocalPlayerService)
                sleepTimerListener = SleepTimerListener(coroutineScope, this)
                addListener(sleepTimerListener)
                addAnalyticsListener(PlaybackStatsListener(false, this@LocalPlayerService))
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

        localPlayerSessionCallback =
            LocalPlayerSessionCallback(this, Database)
            .apply {
                binder = this@LocalPlayerService.binder
                toggleLike = ::toggleLike
                toggleRepeat = ::toggleRepeat
                toggleShuffle = ::toggleShuffle
                startRadio = ::startRadio
                callPause = ::callActionPause
                actionSearch = ::actionSearch
            }

        // Build the media library session
        mediaSession =
            MediaLibrarySession.Builder(this, forwardingPlayer, localPlayerSessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java)
                            .putExtra("expandPlayerBottomSheet", true),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                //TODO CHECK IF THIS IS NEEDED
//                .setBitmapLoader(BitmapLoader(
//                    this,
//                    coroutineScope,
//                    512 * resources.displayMetrics.density.toInt()
//                ))
                // Temporary fix for bug in ExoPlayer media3 https://github.com/androidx/media/issues/2192
                // Bug cause refresh ui in android auto when media is playing
                .setPeriodicPositionUpdateEnabled(false)
                .build()

        // Keep a connected controller so that notification works
        sessionToken = SessionToken(this, ComponentName(this, LocalPlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.let { if (it.isDone) it.get() }}, MoreExecutors.directExecutor())

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.addListener(this@LocalPlayerService)
        player.addAnalyticsListener(PlaybackStatsListener(false, this@LocalPlayerService))

        player.repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        player.playbackParameters = PlaybackParameters(
            preferences.getFloat(playbackSpeedKey, 1f),
            preferences.getFloat(playbackPitchKey, 1f)
        )
        player.volume = preferences.getFloat(playbackVolumeKey, 1f)
        player.setGlobalVolume(player.volume)

        audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)

        notificationActionReceiver = NotificationActionReceiver(player)


        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
            addAction(Action.like.value)
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

//        playerNotificationManager = PlayerNotificationManager.Builder(this, NotificationId, NotificationChannelId)
//            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
//                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
//                    fun startFg() {
//                        runCatching {
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
//                            } else {
//                                startForeground(notificationId, notification)
//                            }
//                        }.onFailure {
//                            Timber.e("LocalPlayerService onNotificationPosted startForeground failed ${it.stackTraceToString()}")
//                            println("LocalPlayerService onNotificationPosted startForeground failed ${it.stackTraceToString()}")
//                        }
//
//                    }
//
//                    // Foreground keep alive
//                    if (preferences.getBoolean(isInvincibilityEnabledKey, false))
//                        startFg()
//                    else
//                        super.onNotificationPosted(notificationId, notification, ongoing)
//
//                }
//            })
//            .setMediaDescriptionAdapter(DefaultMediaDescriptionAdapter(mediaSession.sessionActivity))
//            .build()
//
//        playerNotificationManager.setPlayer(player)
//        playerNotificationManager.setSmallIcon(R.drawable.app_icon)
//        playerNotificationManager.setMediaSessionToken(mediaSession.platformToken)


        // Ensure that song is updated
        currentSong.debounce(1000).collect(coroutineScope) { song ->
            Timber.d("LocalPlayerService onCreate currentSong $song")

            updateDefaultNotification()
            withContext(Dispatchers.Main) {
//                if (song != null) {
//                    updateDiscordPresence(
//                        discordPresenceManager,
//                        binder
//                    )
//                }
                updateWidgets()
            }
        }

        resumePlaybackWhenDeviceConnected()

        processBassBoost()

        processReverb()

        /* Queue is saved in events without scheduling it (remove this in future)*/
        // Load persistent queue when start activity and save periodically in background
        if (isPersistentQueueEnabled()) {
            //restorePlayerQueue()
            player.loadMasterQueue()
            resumePlaybackOnStart()
            // todo maybe not needed
//            coroutineScope.launch {
//                while (isActive) {
//                    delay(
//                    //    30.seconds
//                        5.minutes
//                    )
//                    //savePlayerQueue()
//                    player.saveMasterQueue()
//                }
//            }

        }

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

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent) ?: binder

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession


    override fun onRepeatModeChanged(repeatMode: Int) {
        updateDefaultNotification()
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        Timber.d("LocalPlayerService onUpdateNotification called startInForegroundRequired ${startInForegroundRequired}")
        // Foreground keep alive
        if (!(!player.isPlaying && preferences.getBoolean(isInvincibilityEnabledKey, true))) {
            Timber.d("LocalPlayerService onUpdateNotification PASSED WITH startInForegroundRequired ${startInForegroundRequired}")
            super.onUpdateNotification(session, startInForegroundRequired)
        }
    }


    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SKIP)
            updateDiscordPresenceWithOfflinePlayer(
                discordPresenceManager,
                binder
            )

        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        Timber.d("LocalPlayerService onPlaybackStatsReady called ")
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
            preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)

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
                    Timber.e("LocalPlayerService onPlaybackStatsReady SQLException ${e.stackTraceToString()}")
                }
            }

        }



    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        Timber.d("LocalPlayerService onTimelineChanged timeline $timeline reason $reason")
        if (isPersistentQueueEnabled())
            player.saveMasterQueue()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        Timber.d("LocalPlayerService onPlayWhenReadyChanged playWhenReady $playWhenReady reason $reason")
        if (isPersistentQueueEnabled())
            player.saveMasterQueue()
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

        //savePlayerQueue()
        if (isPersistentQueueEnabled())
            player.saveMasterQueue()

        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            Timber.d("[DiscordPresence] onStop: call the manager (close discord presence)")
            discordPresenceManager?.onStop()
        }

        if (!player.isReleased) {
            player.removeListener(this@LocalPlayerService)
            player.stop()
            player.release()
        }

        mediaSession.release()
        cache.release()
        Database.close()

        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {

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
        Timber.d("LocalPlayerService onMediaItemTransition mediaItem $mediaItem reason $reason")

        if (player.isPlaying && reason == MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            player.prepare()
            player.play()
        }

        currentMediaItem.update { mediaItem }

        if (mediaItem?.isLocal == true)
            recoverPlaybackError()

        processNormalizeVolume()

        if (preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type == Player.REPEAT_MODE_OFF)
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

        updateDiscordPresenceWithOfflinePlayer(
            discordPresenceManager,
            binder
        )

    }

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
        Timber.d("LocalPlayerService onIsPlayingChanged isPlaying $isPlaying")

        preferences.edit(commit = true) { putBoolean(lastMediaItemWasLocalKey, currentMediaItem.value?.isLocal == true) }

        val fadeDisabled = getPlaybackFadeAudioDuration() == DurationInMilliseconds.Disabled
        val duration = getPlaybackFadeAudioDuration().milliSeconds
        if (isPlaying && !fadeDisabled)
            startFadeAnimator(
                player = binder.player,
                duration = duration,
                fadeIn = true
            )


        updateDiscordPresenceWithOfflinePlayer(
            discordPresenceManager,
            binder
        )


        super.onIsPlayingChanged(isPlaying)
    }

    override fun onPlayerError(error: PlaybackException) {

        currentMediaItem.value?.isLocal?.let { if (!it) return }

        super.onPlayerError(error)

        Timber.e("LocalPlayerService onPlayerError error code ${error.errorCode} message ${error.message} cause ${error.cause?.cause}")

//        val playbackHttpExeptionList = listOf(
//            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
//            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
//            416 // 416 Range Not Satisfiable
//        )
//
//        if (error.errorCode in playbackHttpExeptionList) {
//            Timber.e("LocalPlayerService onPlayerError recovered occurred errorCodeName ${error.errorCodeName} cause ${error.cause?.cause}")
//            println("LocalPlayerService onPlayerError recovered occurred errorCodeName ${error.errorCodeName} cause ${error.cause?.cause}")
//            player.pause()
//            player.prepare()
//            player.play()
//            return
//        }

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
//                if (!player.playWhenReady) {
//                    waitingForNetwork.value = false
//                }
            }
        }

        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaItem.value = player.currentMediaItem
        }
    }



    private fun recoverPlaybackError() {
        if (player.playerError != null) {
            player.prepare()
        }
    }

    private fun loadFromRadio(reason: Int) {
        if (!preferences.getBoolean(autoLoadSongsInQueueKey, true)) return

        val isDiscoverEnabled = applicationContext.preferences.getBoolean(discoverKey, false)
        val filterContentType = applicationContext.preferences.getEnum(filterContentTypeKey,
            ContentType.All)
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
                    coroutineScope.launch(Dispatchers.Main) {
                        if (player.playbackState != STATE_IDLE)
                            player.addMediaItems(
                                radio.process()
                                    .filter { song ->
                                        when (filterContentType) {
                                            ContentType.All -> true
                                            ContentType.Official -> song.isOfficialContent
                                            ContentType.UserGenerated -> song.isUserGeneratedContent
                                        }
                                    }
                            )
                    }

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
            Timber.d("LocalPlayerService processBassBoost bassboostLevel $bassboostLevel")
            bassBoost?.enabled = false
            bassBoost?.setStrength(bassboostLevel)
            bassBoost?.enabled = true
        }.onFailure {
            SmartMessage(
                "Can't enable bass boost",
                context = this@LocalPlayerService
            )
        }
    }

    private fun processReverb() {
        val presetType = preferences.getEnum(audioReverbPresetKey, PresetsReverb.NONE)
        Timber.d("LocalPlayerService processReverb presetType $presetType")
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
            Timber.e("LocalPlayerService processNormalizeVolume load loudnessEnhancer ${it.stackTraceToString()}")
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
                                    context = this@LocalPlayerService
                                )
                            }

                            0
                        } else it
                    }
                    try {
                        loudnessEnhancer?.setTargetGain(baseGain.toMb() + volumeBoostLevel.toMb() - loudnessMb)
                        loudnessEnhancer?.enabled = true
                    } catch (e: Exception) {
                        Timber.e("LocalPlayerService processNormalizeVolume apply targetGain ${e.stackTraceToString()}")
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
                    Timber.d("LocalPlayerService onAudioDevicesAdded addedDevices ${addedDevices.map { it.type }}")
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) {
                        Timber.d("LocalPlayerService onAudioDevicesAdded device known ${addedDevices.map { it.productName }}")
                        player.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    Timber.d("LocalPlayerService onAudioDevicesRemoved removedDevices ${removedDevices.map { it.type }}")
                    if (player.isPlaying && removedDevices.any(::canPlayMusic)) {
                        Timber.d("LocalPlayerService onAudioDevicesRemoved device known ${removedDevices.map { it.productName }}")
                        player.pause()
                    }
                }
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
    }.setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER) // prefer extension renderers to opus format

    private fun createMediaSourceFactory() = DefaultMediaSourceFactory(
            createLocalDataSourceFactory( coroutineScope ),
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


    private fun buildCustomCommandButtons(): MutableList<CommandButton> {
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
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
            NotificationCompat.Builder(this@LocalPlayerService, NotificationChannelId)
        } else {
            NotificationCompat.Builder(this@LocalPlayerService)
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
            .setAutoCancel(true)
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
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerFirstIcon }
                .map {
                    customNotify.addAction(
                        it.getStateIcon(
                            it,
                            currentSong.value?.likedAt,
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
                val wpManager = WallpaperManager.getInstance(this@LocalPlayerService)
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
        if (currentMediaItem.value?.isLocal == false) return

        coroutineScope.launch(Dispatchers.Main) {
            mediaSession.setCustomLayout( buildCustomCommandButtons() )
        }

    }


//    private fun updateDiscordPresence() {
//        val isDiscordPresenceEnabled = preferences.getBoolean(isDiscordPresenceEnabledKey, false)
//        if (!isDiscordPresenceEnabled || !isAtLeastAndroid8) return
//
//        val discordPersonalAccessToken = encryptedPreferences.getString(
//            discordPersonalAccessTokenKey, ""
//        )
//
//        runCatching {
//            if (!discordPersonalAccessToken.isNullOrEmpty()) {
//                player.currentMediaItem?.let {
//                    discordPresenceManager?.onPlayingStateChanged(
//                        it,
//                        player.isPlaying,
//                        player.currentPosition,
//                        player.duration,
//                        System.currentTimeMillis(),
//                        getCurrentPosition = { player.currentPosition },
//                        isPlayingProvider = { player.isPlaying }
//                    )
//                }
//            }
//        }.onFailure {
//            Timber.e("PlayerService Failed sendDiscordPresence in PlayerService ${it.stackTraceToString()}")
//        }
//    }


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
                    context = this@LocalPlayerService
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


//    private fun savePlayerQueue() {
//        Timber.d("LocalPlayerService onCreate savePersistentQueue")
//        println("LocalPlayerService onCreate savePersistentQueue")
//        if (!isPersistentQueueEnabled) return
//        Timber.d("LocalPlayerService onCreate savePersistentQueue is enabled, processing")
//        println("LocalPlayerService onCreate savePersistentQueue is enabled, processing")
//
//        CoroutineScope(Dispatchers.Main).launch {
//            val mediaItems = player.currentTimeline.mediaItems
//            val mediaItemIndex = player.currentMediaItemIndex
//            val mediaItemPosition = player.currentPosition
//
//            if (mediaItems.isEmpty()) return@launch
//
//
//            mediaItems.mapIndexed { index, mediaItem ->
//                QueuedMediaItem(
//                    mediaItem = mediaItem,
//                    position = if (index == mediaItemIndex) mediaItemPosition else null,
//                    idQueue = mediaItem.mediaMetadata.extras?.getLong("idQueue", defaultQueueId())
//                )
//            }.let { queuedMediaItems ->
//                if (queuedMediaItems.isEmpty()) return@let
//
//                Database.asyncTransaction {
//                    clearQueuedMediaItems()
//                    insert( queuedMediaItems )
//                }
//
//                Timber.d("LocalPlayerService QueuePersistentEnabled Saved queue")
//            }
//
//        }
//    }

    private fun resumePlaybackOnStart() {
        if(!isPersistentQueueEnabled() || !preferences.getBoolean(resumePlaybackOnStartKey, false)) return

        if(!player.isPlaying) {
            player.play()
        }
    }

//    @ExperimentalCoroutinesApi
//    @FlowPreview
//    @UnstableApi
//    private fun restorePlayerQueue() {
//        if (!isPersistentQueueEnabled) return
//
//        Database.asyncQuery {
//            val queuedSong = queuedMediaItems()
//
//            if (queuedSong.isEmpty()) return@asyncQuery
//
//            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)
//
//            runBlocking(Dispatchers.Main) {
//                player.setMediaItems(
//                    queuedSong.map { mediaItem ->
//                        mediaItem.mediaItem.buildUpon()
//                            .setUri(mediaItem.mediaItem.mediaId)
//                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
//                            .build().apply {
//                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
//                                mediaMetadata.extras?.putLong("idQueue", mediaItem.idQueue ?: defaultQueueId())
//                            }
//                    },
//                    index,
//                    queuedSong[index].position ?: C.TIME_UNSET
//                )
//                player.prepare()
//            }
//        }
//
//    }

//    @ExperimentalCoroutinesApi
//    @FlowPreview
//    @UnstableApi
//    private fun restoreFromDiskPlayerQueue() {
//
//        runCatching {
//            filesDir.resolve("persistentQueue.data").inputStream().use { fis ->
//                ObjectInputStream(fis).use { oos ->
//                    oos.readObject() as PersistentQueue
//                }
//            }
//        }.onSuccess { queue ->
//            //Log.d("mediaItem", "QueuePersistentEnabled Restored queue $queue")
//            //Log.d("mediaItem", "QueuePersistentEnabled Restored ${queue.songMediaItems.size}")
//            runBlocking(Dispatchers.Main) {
//                player.setMediaItems(
//                    queue.songMediaItems.map { song ->
//                        song.asMediaItem.buildUpon()
//                            .setUri(song.asMediaItem.mediaId)
//                            .setCustomCacheKey(song.asMediaItem.mediaId)
//                            .build().apply {
//                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
//                            }
//                    },
//                    queue.mediaItemIndex,
//                    queue.position
//                )
//
//                player.prepare()
//
//            }
//
//        }.onFailure {
//            Timber.e(it.stackTraceToString())
//        }
//
//    }
//
//    private fun saveToDiskPlayerQueue() {
//
//        val persistentQueue = PersistentQueue(
//            title = "title",
//            songMediaItems = player.currentTimeline.mediaItems.map {
//                PersistentSong(
//                    id = it.mediaId,
//                    title = it.mediaMetadata.title.toString(),
//                    durationText = it.mediaMetadata.extras?.getString("durationText").toString(),
//                    thumbnailUrl = it.mediaMetadata.artworkUri.toString()
//                )
//            },
//            mediaItemIndex = player.currentMediaItemIndex,
//            position = player.currentPosition
//        )
//
//        runCatching {
//            filesDir.resolve("persistentQueue.data").outputStream().use { fos ->
//                ObjectOutputStream(fos).use { oos ->
//                    oos.writeObject(persistentQueue)
//                }
//            }
//        }.onFailure {
//            //it.printStackTrace()
//            Timber.e(it.stackTraceToString())
//
//        }.onSuccess {
//            Log.d("mediaItem", "QueuePersistentEnabled Saved $persistentQueue")
//        }
//
//    }


    /**
     * This method should ONLY be called when the application (sc. activity) is in the foreground!
     */
//    fun restartForegroundOrStop() {
//        binder.restartForegroundOrStop()
//    }

    @UnstableApi
    class CustomMediaNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {
        override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence? {
            val customMetadata = MediaMetadata.Builder()
                .setTitle(cleanPrefix(metadata.title?.toString() ?: ""))
                .build()
            return super.getNotificationContentTitle(customMetadata)
        }

    }


    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatching {
                context.stopService(context.intent<LocalPlayerService>())
            }.onFailure {
                Timber.e("Failed NotificationDismissReceiver stopService in LocalPlayerService (LocalPlayerService) ${it.stackTraceToString()}")
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
        val service: LocalPlayerService
            get() = this@LocalPlayerService

        /*
        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        */
        val bitmap: Bitmap
            get() = bitmapProvider.bitmap


        val player: ExoPlayer
            get() = this@LocalPlayerService.player

        val cache: Cache
            get() = this@LocalPlayerService.cache

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        val currentMediaItemAsSong: Song?
            get() = this@LocalPlayerService.currentSong.value

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()



            timerJob = coroutineScope.timer(delayMillis) {
                val notification = NotificationCompat
                    .Builder(this@LocalPlayerService, SleepTimerNotificationChannelId)
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