package it.fast4x.riplay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.media.VolumeProviderCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import it.fast4x.environment.Environment
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.PlayerBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.environment.utils.from
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.PersistentQueue
import it.fast4x.riplay.data.models.PersistentSong
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.data.models.asMediaItem
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.activityPendingIntent
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.broadCastPendingIntent
import it.fast4x.riplay.utils.findNextMediaItemById
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.forceSeekToNext
import it.fast4x.riplay.utils.forceSeekToPrevious
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isAtLeastAndroid13
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.utils.isAtLeastAndroid81
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.shouldBePlaying
import it.fast4x.riplay.utils.startFadeAnimator
import it.fast4x.riplay.utils.thumbnail
import it.fast4x.riplay.utils.timer
import it.fast4x.riplay.R
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.extensions.audiovolume.AudioVolumeObserver
import it.fast4x.riplay.extensions.audiovolume.OnAudioVolumeChangedListener
import it.fast4x.riplay.extensions.preferences.closebackgroundPlayerKey
import it.fast4x.riplay.extensions.preferences.discoverKey
import it.fast4x.riplay.extensions.preferences.exoPlayerMinTimeForEventKey
import it.fast4x.riplay.extensions.preferences.filterContentTypeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.isInvincibilityEnabledKey
import it.fast4x.riplay.extensions.preferences.isPauseOnVolumeZeroEnabledKey
import it.fast4x.riplay.extensions.preferences.isShowingThumbnailInLockscreenKey
import it.fast4x.riplay.extensions.preferences.loudnessBaseGainKey
import it.fast4x.riplay.extensions.preferences.minimumSilenceDurationKey
import it.fast4x.riplay.extensions.preferences.pauseListenHistoryKey
import it.fast4x.riplay.extensions.preferences.persistentQueueKey
import it.fast4x.riplay.extensions.preferences.playbackFadeAudioDurationKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.extensions.preferences.resumeOrPausePlaybackWhenDeviceKey
import it.fast4x.riplay.extensions.preferences.showLikeButtonBackgroundPlayerKey
import it.fast4x.riplay.extensions.preferences.skipSilenceKey
import it.fast4x.riplay.extensions.preferences.useVolumeKeysToChangeSongKey
import it.fast4x.riplay.extensions.preferences.volumeNormalizationKey
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget
import it.fast4x.riplay.ui.widgets.PlayerVerticalWidget
import it.fast4x.riplay.utils.BitmapProvider
import it.fast4x.riplay.utils.OnlineRadio
import it.fast4x.riplay.utils.SleepTimerListener
import it.fast4x.riplay.utils.TimerJob
import it.fast4x.riplay.utils.isHandleAudioFocusEnabled
import it.fast4x.riplay.utils.isOfficialContent
import it.fast4x.riplay.utils.isUserGeneratedContent
import it.fast4x.riplay.utils.loadMasterQueue
import it.fast4x.riplay.utils.saveMasterQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import android.os.Binder as AndroidBinder

//
//const val LOCAL_KEY_PREFIX = "local:"
//
//@get:OptIn(UnstableApi::class)
//val DataSpec.isLocal get() = key?.startsWith(LOCAL_KEY_PREFIX) == true
//
//val MediaItem.isLocal get() = mediaId.startsWith(LOCAL_KEY_PREFIX)
//val Song.isLocal get() = id.startsWith(LOCAL_KEY_PREFIX)

@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : InvincibleService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnAudioVolumeChangedListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var cache: SimpleCache
    lateinit var player: ExoPlayer
    private lateinit var downloadCache: SimpleCache
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    //private lateinit var connectivityManager: ConnectivityManager

    private val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_REWIND or
            PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH


    @ExperimentalCoroutinesApi
    @FlowPreview
    private val stateBuilderWithoutCustomAction
        get() = PlaybackStateCompat.Builder().setActions(actions.let {
            if (isAtLeastAndroid12) it or PlaybackState.ACTION_SET_PLAYBACK_SPEED else it
        })

    @ExperimentalCoroutinesApi
    @FlowPreview
    private val stateBuilder
        get() = PlaybackStateCompat.Builder().setActions(actions.let {
            if (isAtLeastAndroid12) it or PlaybackState.ACTION_SET_PLAYBACK_SPEED else it
        })
            .addCustomAction(
                "SHUFFLE",
                "Shuffle",
                if (shuffleModeEnabled) R.drawable.shuffle_filled else R.drawable.shuffle
            )
            .addCustomAction(
                "LIKE",
                "Like",
                if (isLikedState.value) R.drawable.heart else R.drawable.heart_outline
            )
            .addCustomAction(
                "PLAYRADIO",
                "Play radio",
                R.drawable.radio
            )

    @ExperimentalCoroutinesApi
    @FlowPreview
    private val stateBuilderWithDownloadOnly
        get() = PlaybackStateCompat.Builder().setActions(actions.let {
            if (isAtLeastAndroid12) it or PlaybackState.ACTION_SET_PLAYBACK_SPEED else it
        }).addCustomAction(
                "PLAYRADIO",
                "Play radio",
                R.drawable.radio
            )

    @ExperimentalCoroutinesApi
    @FlowPreview
    private val stateBuilderWithLikeOnly
        get() = PlaybackStateCompat.Builder().setActions(actions.let {
            if (isAtLeastAndroid12) it or PlaybackState.ACTION_SET_PLAYBACK_SPEED else it
        }).addCustomAction(
            "LIKE",
            "Like",
            if (isLikedState.value) R.drawable.heart else R.drawable.heart_outline
        )
            .addCustomAction(
                "PLAYRADIO",
                "Play radio",
                R.drawable.radio
            )

    private val playbackStateMutex = Mutex()

    private val metadataBuilder = MediaMetadataCompat.Builder()

    private var notificationManager: NotificationManager? = null

    private var timerJob: TimerJob? = null

    private var radio: OnlineRadio? = null

    private lateinit var bitmapProvider: BitmapProvider

    private var volumeNormalizationJob: Job? = null

    private var isPersistentQueueEnabled = false
    private var isclosebackgroundPlayerEnabled = false
    private var isShowingThumbnailInLockscreen = true
    override var isInvincibilityEnabled = false

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

    private var isNotificationStarted = false

    //private var exoPlayerAlternateCacheLocation = ""

    override val notificationId: Int
        get() = NotificationId

    private lateinit var notificationActionReceiver: NotificationActionReceiver
    //private lateinit var audioQualityFormat: AudioQualityFormat
    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    /*
    private val media = MutableStateFlow<MediaItem?>(null)
    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private val currentMedia = media.flatMapLatest { mediaItem ->
        Database.song(mediaItem?.mediaId)
    }.stateIn(coroutineScope, SharingStarted.Lazily, null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private var currentMediaLiked = media.flatMapLatest { mediaItem ->
       if (mediaItem?.mediaId?.let { Database.songliked(it) } == 0) flowOf(false) else flowOf(true)
    }.stateIn(coroutineScope, SharingStarted.Lazily, false)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private var currentMediaDownloaded = media.flatMapLatest { mediaItem ->
        val downloads = DownloadUtil.downloads.value
        flowOf(downloads[mediaItem?.mediaId]?.state == Download.STATE_COMPLETED)
    }.stateIn(coroutineScope, SharingStarted.Lazily, false)
     */

    private val mediaItemState = MutableStateFlow<MediaItem?>(null)

    @ExperimentalCoroutinesApi
    @FlowPreview
    private val isLikedState = mediaItemState
        .flatMapMerge { item ->
            item?.mediaId?.let { Database.likedAt(it).distinctUntilChanged() } ?: flowOf(null)
        }
        .map { it != null }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private var showLikeButton = true
    private var shuffleModeEnabled = false

    lateinit var sleepTimerListener: SleepTimerListener
    
    override fun onBind(intent: Intent?): AndroidBinder {
        super.onBind(intent)
        return binder
    }


    @ExperimentalCoroutinesApi
    @FlowPreview
    @SuppressLint("Range")
    @UnstableApi
    override fun onCreate() {
        super.onCreate()

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

        createNotificationChannel()

        preferences.registerOnSharedPreferenceChangeListener(this)

        val preferences = preferences
        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, false)
        isInvincibilityEnabled = preferences.getBoolean(isInvincibilityEnabledKey, false)
        isShowingThumbnailInLockscreen =
            preferences.getBoolean(isShowingThumbnailInLockscreenKey, false)

        //audioQualityFormat = preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.High)
        showLikeButton = preferences.getBoolean(showLikeButtonBackgroundPlayerKey, true)

        player = ExoPlayer.Builder(this)
            // todo MediaSourceFactory not needed for local content
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
        player.addListener(this)
        player.addAnalyticsListener(PlaybackStatsListener(false, this))

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        mediaSession = MediaSessionCompat(baseContext, "PlayerService")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setRatingType(RatingCompat.RATING_NONE)
        mediaSession.setSessionActivity(sessionActivityPendingIntent)
        mediaSession.setCallback(SessionCallback(player))

        if (preferences.getBoolean(useVolumeKeysToChangeSongKey, false))
            mediaSession.setPlaybackToRemote(getVolumeProvider())

        audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)

        //connectivityManager = getSystemService()!!

//        if (showLikeButton && showDownloadButton)
//            mediaSession.setPlaybackState(stateBuilder.build())
//        if (showLikeButton && !showDownloadButton)
//            mediaSession.setPlaybackState(stateBuilderWithLikeOnly.build())
//        if (showDownloadButton && !showLikeButton)
//            mediaSession.setPlaybackState(stateBuilderWithDownloadOnly.build())
//        if (!showLikeButton && !showDownloadButton)
//            mediaSession.setPlaybackState(stateBuilderWithoutCustomAction.build())

        mediaSession.isActive = true

        updatePlaybackState()


        coroutineScope.launch {
            var first = true
            mediaItemState.zip(isLikedState) { mediaItem, _ ->
                if (first) {
                    first = false
                    return@zip
                }
                if (mediaItem == null) return@zip
                withContext(Dispatchers.Main) {
                    updatePlaybackState()
                    handler.post {
                        runCatching {
                            applicationContext.getSystemService<NotificationManager>()
                                ?.notify(NotificationId, notification())
                        }.onFailure {
                            Timber.e("Failed likedState in PlayerService ${it.stackTraceToString()}")
                        }
                    }
                }
            }.collect()
        }

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
        }

        ContextCompat.registerReceiver(
            this,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        player.loadMasterQueue()

        if (isPersistentQueueEnabled) {
            // Save persistent queue periodically
            coroutineScope.launch {
                while (isActive) {
                    delay(30.seconds)
                    withContext(Dispatchers.Main) {
                        player.saveMasterQueue()
                        //maybeSaveToDiskPlayerQueue()
                    }
                }
            }
        }

        maybeResumePlaybackWhenDeviceConnected()

        //workaround for android 12+
        runCatching {
            notification()?.let {
                ServiceCompat.startForeground(
                    this@PlayerService,
                    NotificationId,
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


        updateDiscordPresence()

    }

    private fun updateDiscordPresence() {
        if (!isAtLeastAndroid81) return
//        val discordPersonalAccessToken = preferences.getString(discordPersonalAccessTokenKey, "")
//        val isDiscordPresenceEnabled = preferences.getBoolean(isDiscordPresenceEnabledKey, false)
//        runCatching {
//            if (!discordPersonalAccessToken.isNullOrEmpty() && isDiscordPresenceEnabled) {
//                player.currentMediaItem?.let {
//                    sendDiscordPresence(
//                        discordPersonalAccessToken,
//                        it,
//                        player.duration
//                    )
//                }
//            }
//        }.onFailure {
//            Timber.e("PlayerService Failed sendDiscordPresence in PlayerService ${it.stackTraceToString()}")
//        }
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

    override fun onTaskRemoved(rootIntent: Intent?) {

        isclosebackgroundPlayerEnabled = preferences.getBoolean(closebackgroundPlayerKey, false)
        if (isclosebackgroundPlayerEnabled) {
            broadCastPendingIntent<NotificationDismissReceiver>().send()
            this.stopService(this.intent<PlayerService>())
            //stopSelf()
            onDestroy()
        }
        super.onTaskRemoved(rootIntent)
    }

    @UnstableApi
    override fun onDestroy() {
        runCatching {
            player.saveMasterQueue()

            preferences.unregisterOnSharedPreferenceChangeListener(this)

            player.removeListener(this)
            player.stop()
            player.release()

            unregisterReceiver(notificationActionReceiver)

            mediaSession.isActive = false
            mediaSession.release()
            cache.release()
            //downloadCache.release()
            loudnessEnhancer?.release()
            audioVolumeObserver.unregister()

            coroutineScope.cancel()
        }.onFailure {
            Timber.e("Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }
        super.onDestroy()
    }

    override fun shouldBeInvincible(): Boolean {
        return !player.shouldBePlaying
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


    @ExperimentalCoroutinesApi
    @FlowPreview
    override fun onConfigurationChanged(newConfig: Configuration) {
        handler.post {
            runCatching {
                if (bitmapProvider.setDefaultBitmap() && player.currentMediaItem != null) {
                    notificationManager?.notify(NotificationId, notification())
                }
            }.onFailure {
                Timber.e("Failed onConfigurationChanged in PlayerService ${it.stackTraceToString()}")
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    @UnstableApi
    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        // if pause listen history is enabled, don't register statistic event
        if (preferences.getBoolean(pauseListenHistoryKey, false)) return

        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            Database.asyncTransaction {
                Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }


        val minTimeForEvent =
            preferences.getEnum(exoPlayerMinTimeForEventKey, MinTimeForEvent.`20s`)

        if (totalPlayTimeMs > minTimeForEvent.ms) {
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

    @UnstableApi
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

        mediaItemState.update { mediaItem }

        maybeRecoverPlaybackError()
        maybeNormalizeVolume()
        maybeProcessRadio()

        if (mediaItem == null) {
            runCatching {
                bitmapProvider.listener?.invoke(null)
            }.onFailure {
                Timber.e("Failed onMediaItemTransition bitmapProvider.invoke in PlayerService ${it.stackTraceToString()}")
            }
        } else if (mediaItem.mediaMetadata.artworkUri == bitmapProvider.lastUri) {
            runCatching {
                bitmapProvider.listener?.invoke(bitmapProvider.lastBitmap)
            }.onFailure {
                Timber.e("Failed onMediaItemTransition bitmapProvider.invoke lastbitmap in PlayerService ${it.stackTraceToString()}")
            }
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            updateMediaSessionQueue(player.currentTimeline)
        }

        updateWidgets()
        updateDiscordPresence()

    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            updateMediaSessionQueue(timeline)
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

        mediaSession.setQueue(
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
        if (player.playerError != null) {
            player.prepare()
        }
    }

    private fun maybeProcessRadio() {
        // Old feature add songs only if radio is started by user and when last song in player is played
        radio?.let { radio ->
            if (player.mediaItemCount - player.currentMediaItemIndex == 1) {
                coroutineScope.launch(Dispatchers.Main) {
                    player.addMediaItems(radio.process())
                }
            }
        }

        /* // New feature auto start radio in queue
        if (radio == null) {
            binder.setupRadio(
                NavigationEndpoint.Endpoint.Watch(
                    videoId = player.currentMediaItem?.mediaId
                )
            )
        } else {
            radio?.let { radio ->
                if (player.mediaItemCount - player.currentMediaItemIndex <= 3) {
                    coroutineScope.launch(Dispatchers.Main) {
                        player.addMediaItems(radio.process())
                    }
                }
            }
        }
        */
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    @UnstableApi
    private fun maybeRestoreFromDiskPlayerQueue() {
        if (!isPersistentQueueEnabled) return
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

                isNotificationStarted = true
                runCatching {
                    startForegroundService(this@PlayerService, intent<PlayerService>())
                }.onFailure {
                    Timber.e("maybeRestoreFromDiskPlayerQueue PlayerService startForegroundService ${it.stackTraceToString()}")
                }
                runCatching {
                    //startForeground(NotificationId, notification())
                    notification()?.let {
                        ServiceCompat.startForeground(
                            this@PlayerService,
                            NotificationId,
                            it,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            } else {
                                0
                            }
                        )
                    }
                }.onFailure {
                    Timber.e("maybeRestoreFromDiskPlayerQueue PlayerService startForeground ${it.stackTraceToString()}")
                }
            }

        }.onFailure {
            //it.printStackTrace()
            Timber.e(it.stackTraceToString())
        }

        //Log.d("mediaItem", "QueuePersistentEnabled Restored ${player.currentTimeline.mediaItems.size}")

    }

    private fun maybeSaveToDiskPlayerQueue() {

        if (!isPersistentQueueEnabled) return
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

//    private fun maybeSavePlayerQueue() {
//        if (!isPersistentQueueEnabled) return
//        /*
//        if (player.playbackState == Player.STATE_IDLE) {
//            Log.d("mediaItem", "QueuePersistentEnabled playbackstate idle return")
//            return
//        }
//         */
//        //Log.d("mediaItem", "QueuePersistentEnabled Save ${player.currentTimeline.mediaItems.size}")
//        //Log.d("mediaItem", "QueuePersistentEnabled Save initial")
//
//        val mediaItems = player.currentTimeline.mediaItems
//        val mediaItemIndex = player.currentMediaItemIndex
//        val mediaItemPosition = player.currentPosition
//
//        mediaItems.mapIndexed { index, mediaItem ->
//            QueuedMediaItem(
//                mediaItem = mediaItem,
//                position = if (index == mediaItemIndex) mediaItemPosition else null
//            )
//        }.let { queuedMediaItems ->
//            query {
//                Database.clearQueue()
//                Database.insert(queuedMediaItems)
//            }
//        }
//    }

//    @ExperimentalCoroutinesApi
//    @FlowPreview
//    @UnstableApi
//    private fun maybeRestorePlayerQueue() {
//        if (!isPersistentQueueEnabled) return
//
//        Database.asyncTransaction {
//            val queuedSong = Database.queue()
//
//            if (queuedSong.isEmpty()) return@query
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
//                            }
//                    },
//                    index,
//                    queuedSong[index].position ?: C.TIME_UNSET
//                )
//                player.prepare()
//
//                isNotificationStarted = true
//                runCatching {
//                    startForegroundService(this@PlayerService, intent<PlayerService>())
//                }.onFailure {
//                    Timber.e("maybeRestorePlayerQueue startForegroundService ${it.stackTraceToString()}")
//                }
//                runCatching {
//                    //startForeground(NotificationId, notification())
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
//                    Timber.e("maybeRestorePlayerQueue startForeground ${it.stackTraceToString()}")
//                }
//            }
//        }
//
//    }

    @UnstableApi
    private fun maybeNormalizeVolume() {
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
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
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

    private fun maybeShowSongCoverInLockScreen() {
        val bitmap =
            if (isAtLeastAndroid13 || isShowingThumbnailInLockscreen) bitmapProvider.bitmap else null

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

        mediaSession.setMetadata(metadataBuilder.build())
    }

    @SuppressLint("NewApi")
    private fun maybeResumePlaybackWhenDeviceConnected() {
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
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) {
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

    @ExperimentalCoroutinesApi
    @FlowPreview
    private fun updatePlaybackState() = coroutineScope.launch {
        playbackStateMutex.withLock {
            withContext(Dispatchers.Main) {
                shuffleModeEnabled = player.shuffleModeEnabled

//                if (showLikeButton && showDownloadButton)
//                    mediaSession.setPlaybackState(
//                        stateBuilder
//                            .setState(player.androidPlaybackState, player.currentPosition, 1f)
//                            .setBufferedPosition(player.bufferedPosition)
//                            .build()
//                    )
//                if (showLikeButton && !showDownloadButton)
//                    mediaSession.setPlaybackState(
//                        stateBuilderWithLikeOnly
//                            .setState(player.androidPlaybackState, player.currentPosition, 1f)
//                            .setBufferedPosition(player.bufferedPosition)
//                            .build()
//                    )
//                if (showDownloadButton && !showLikeButton)
//                    mediaSession.setPlaybackState(
//                        stateBuilderWithDownloadOnly
//                            .setState(player.androidPlaybackState, player.currentPosition, 1f)
//                            .setBufferedPosition(player.bufferedPosition)
//                            .build()
//                    )
                //if (!showDownloadButton && !showLikeButton)
                    mediaSession.setPlaybackState(
                        stateBuilderWithoutCustomAction
                            .setState(player.androidPlaybackState, player.currentPosition, 1f)
                            .setBufferedPosition(player.bufferedPosition)
                            .build()
                    )
            }
        }



    }

    private val Player.androidPlaybackState
        get() = when (playbackState) {
            Player.STATE_BUFFERING -> if (playWhenReady) PlaybackStateCompat.STATE_BUFFERING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_READY -> if (playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            else -> PlaybackStateCompat.STATE_NONE
        }

    // legacy behavior may cause inconsistencies, but not available on sdk 24 or lower
    @ExperimentalCoroutinesApi
    @FlowPreview
    @Suppress("DEPRECATION")
    override fun onEvents(player: Player, events: Player.Events) {
        if (player.duration != C.TIME_UNSET) mediaSession.setMetadata(
            metadataBuilder
                .putText(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    cleanPrefix(player.mediaMetadata.title.toString())
                )
                .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, player.mediaMetadata.artist)
                .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, player.mediaMetadata.albumTitle)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration)
                .build()
        )


        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_IS_LOADING_CHANGED,
                Player.EVENT_MEDIA_METADATA_CHANGED
                //Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED
            )
        ) {
            val notification = notification()

            if (notification == null) {
                isNotificationStarted = false
                makeInvincible(false)
                runCatching {
                    stopForeground(false)
                }.onFailure {
                    Timber.e("Failed stopForeground in PlayerService onEvents ${it.stackTraceToString()}")
                }
                sendCloseEqualizerIntent()
                notificationManager?.cancel(NotificationId)
                return
            }

            if (player.shouldBePlaying && !isNotificationStarted) {
                isNotificationStarted = true
                runCatching {
                    startForegroundService(this@PlayerService, intent<PlayerService>())
                }.onFailure {
                    Timber.e("Failed startForegroundService in PlayerService onEvents ${it.stackTraceToString()}")
                }
                runCatching {
                    //startForeground(NotificationId, notification)
                    notification()?.let {
                        ServiceCompat.startForeground(
                            this@PlayerService,
                            NotificationId,
                            it,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            } else {
                                0
                            }
                        )
                    }
                }.onFailure {
                    Timber.e("Failed startForeground in PlayerService onEvents ${it.stackTraceToString()}")
                }
                makeInvincible(false)
                sendOpenEqualizerIntent()
            } else {
                if (!player.shouldBePlaying) {
                    isNotificationStarted = false
                    runCatching {
                        stopForeground(false)
                    }.onFailure {
                        Timber.e("Failed stopForeground 1 in PlayerService onEvents ${it.stackTraceToString()}")
                    }
                    makeInvincible(true)
                    sendCloseEqualizerIntent()
                }
                runCatching {
                    notificationManager?.notify(NotificationId, notification)
                }.onFailure {
                    Timber.e("Failed onEvents in PlayerService notificationManager.notify ${it.stackTraceToString()}")
                }
            }
        }
        //updateNotification()
        updatePlaybackState()
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Timber.e("PlayerService onPlayerError ${error.stackTraceToString()}")
        //this.stopService(this.intent<MyDownloadService>())
        //this.stopService(this.intent<PlayerService>())
        //Log.d("mediaItem","onPlayerError ${error.errorCodeName}")
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        Timber.e("PlayerService onPlayerErrorChanged ${error?.stackTraceToString()}")
        //this.stopService(this.intent<MyDownloadService>())
        //this.stopService(this.intent<PlayerService>())
        //Log.d("mediaItem","onPlayerErrorChanged ${error?.errorCodeName}")
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        super.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
        //Timber.e("PlayerService onPlaybackSuppressionReasonChanged $playbackSuppressionReason")
        //Log.d("mediaItem","onPlaybackSuppressionReasonChanged $playbackSuppressionReason")
    }

    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val fadeDisabled = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled) == DurationInMilliseconds.Disabled
        val duration = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled).milliSeconds
        if (isPlaying && !fadeDisabled)
            startFadeAnimator(
                player = binder.player,
                duration = duration,
                fadeIn = true
            )

        //val totalPlayTimeMs = player.totalBufferedDuration.toString()
        //Log.d("mediaEvent","isPlaying "+isPlaying.toString() + " buffered duration "+totalPlayTimeMs)
        //Log.d("mediaItem","onIsPlayingChanged isPlaying $isPlaying audioSession ${player.audioSessionId}")

        updateWidgets()


        super.onIsPlayingChanged(isPlaying)
    }


    @UnstableApi
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            persistentQueueKey -> if (sharedPreferences != null) {
                isPersistentQueueEnabled =
                    sharedPreferences.getBoolean(key, isPersistentQueueEnabled)
            }

            volumeNormalizationKey, loudnessBaseGainKey -> maybeNormalizeVolume()

            resumeOrPausePlaybackWhenDeviceKey -> maybeResumePlaybackWhenDeviceConnected()

            isInvincibilityEnabledKey -> if (sharedPreferences != null) {
                isInvincibilityEnabled =
                    sharedPreferences.getBoolean(key, isInvincibilityEnabled)
            }

            skipSilenceKey -> if (sharedPreferences != null) {
                player.skipSilenceEnabled = sharedPreferences.getBoolean(key, false)
            }

            isShowingThumbnailInLockscreenKey -> {
                if (sharedPreferences != null) {
                    isShowingThumbnailInLockscreen = sharedPreferences.getBoolean(key, true)
                }
                maybeShowSongCoverInLockScreen()
            }

            queueLoopTypeKey -> {
                player.repeatMode =
                    sharedPreferences?.getEnum(queueLoopTypeKey, QueueLoopType.Default)?.type
                        ?: QueueLoopType.Default.type
            }
        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    override fun notification(): Notification? {
        if (player.currentMediaItem == null) return null

        val playIntent = Action.play.pendingIntent
        val pauseIntent = Action.pause.pendingIntent
        val nextIntent = Action.next.pendingIntent
        val prevIntent = Action.previous.pendingIntent
        val likeIntent = Action.like.pendingIntent
        val downloadIntent = Action.download.pendingIntent
        val playradioIntent = Action.playradio.pendingIntent
        val shuffleIntent = Action.shuffle.pendingIntent

        val mediaMetadata = player.mediaMetadata

        shuffleModeEnabled = player.shuffleModeEnabled

        val builder = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(applicationContext, NotificationChannelId)
        } else {
            NotificationCompat.Builder(applicationContext)
        }
            .setContentTitle(cleanPrefix(player.mediaMetadata.title.toString()))
            .setContentText(
                if (mediaMetadata.albumTitle != null || mediaMetadata.artist != "")
                    "${mediaMetadata.artist} | ${mediaMetadata.albumTitle}"
                else mediaMetadata.artist
            )
            .setSubText(
                if (mediaMetadata.albumTitle != null || mediaMetadata.artist != "")
                    "${mediaMetadata.artist} | ${mediaMetadata.albumTitle}"
                else mediaMetadata.artist
            )
            //.setSubText(player.playerError?.message)
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
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .addAction(R.drawable.play_skip_back, "Skip back", prevIntent)
            .addAction(
                if (player.shouldBePlaying) R.drawable.pause else R.drawable.play,
                if (player.shouldBePlaying) "Pause" else "Play",
                if (player.shouldBePlaying) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, "Skip forward", nextIntent)

            //Prior Android 11
            builder
                .addAction(
                    if (shuffleModeEnabled) R.drawable.shuffle_filled else R.drawable.shuffle,
                    "Shuffle", shuffleIntent
                )
                .addAction(
                    if (isLikedState.value) R.drawable.heart else R.drawable.heart_outline,
                    //if (currentMediaLiked.value) R.drawable.heart else R.drawable.heart_outline,
                    "Like",
                    likeIntent
                )
                .addAction(
                    R.drawable.radio,
                    "Play radio",
                    playradioIntent
                )

        runCatching {
            bitmapProvider.load(mediaMetadata.artworkUri) { bitmap ->
                maybeShowSongCoverInLockScreen()
                handler.post {
                    runCatching {
                        notificationManager?.notify(
                            NotificationId,
                            builder.setLargeIcon(bitmap).build()
                        )
                    }.onFailure {
                        Timber.e("Failed notification() bitmapProvider.invoke in PlayerService ${it.stackTraceToString()}")
                    }
                }
            }
        }.onFailure {
            Timber.e("Failed notification() load in bitmapProvider in PlayerService ${it.stackTraceToString()}")
        }


        return builder.build()
    }


    private fun createNotificationChannel() {
        notificationManager = getSystemService()

        if (!isAtLeastAndroid8) return

        notificationManager?.run {
            if (getNotificationChannel(NotificationChannelId) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        NotificationChannelId,
                        "Now playing",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }

            if (getNotificationChannel(SleepTimerNotificationChannelId) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        SleepTimerNotificationChannelId,
                        "Sleep timer",
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

    private fun okHttpClient(): OkHttpClient {
        ProxyPreferences.preference?.let {
            return OkHttpClient.Builder()
                .proxy(
                    Proxy(
                        it.proxyMode,
                        InetSocketAddress(it.proxyHost, it.proxyPort)
                    )
                )
                .connectTimeout(Duration.ofSeconds(16))
                .readTimeout(Duration.ofSeconds(8))
                .build()
        }
        return OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(16))
            .readTimeout(Duration.ofSeconds(8))
            .build()
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


    inner class Binder : AndroidBinder() {
        val player: ExoPlayer
            get() = this@PlayerService.player

        val cache: Cache
            get() = this@PlayerService.cache

        val downloadCache: Cache
            get() = this@PlayerService.downloadCache

        val mediaSession
            get() = this@PlayerService.mediaSession

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        //var mediaItems = mutableListOf<MediaItem>()

        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()



            timerJob = coroutineScope.timer(delayMillis) {
                val notification = NotificationCompat
                    .Builder(this@PlayerService, SleepTimerNotificationChannelId)
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
            //todo refactor
//            OnlineRadio(
//                endpoint?.videoId,
//                endpoint?.playlistId,
//                endpoint?.playlistSetVideoId,
//                endpoint?.params,
//                isDiscoverEnabled,
//                applicationContext,
//                binder,
//                coroutineScope
//            ).let {
//                isLoadingRadio = true
//                radioJob = coroutineScope.launch(Dispatchers.Main) {
//
//                    val songs =
//                        (if (filterArtist.isEmpty()) it.process()
//                        else it.process().filter { song -> song.mediaMetadata.artist == filterArtist })
//                            .filter { song ->
//                                when (filterContentType) {
//                                    ContentType.All -> true
//                                    ContentType.Official -> song.isOfficialContent
//                                    ContentType.UserGenerated -> song.isUserGeneratedContent
//                                }
//                            }
//
//                    songs.forEach {
//                        Database.asyncTransaction { insert(it) }
//                    }
//
//                    if (justAdd) {
//                        player.addMediaItems(songs.drop(1))
//                    } else {
//                        player.forcePlayFromBeginning(songs)
//                    }
//                    radio = it
//                    isLoadingRadio = false
//                }
//            }
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
            isInvincibilityEnabled = false
            stopSelf()
        }

        @ExperimentalCoroutinesApi
        @FlowPreview
        fun toggleLike() = mediaItemState.value?.let { mediaItem ->
            //mediaItemToggleLike(mediaItem)
            Database.asyncTransaction {
                Database.like(
                    mediaItem.mediaId,
                    if (isLikedState.value) null else System.currentTimeMillis()
                )
            }
            updatePlaybackState()
        }


        @kotlin.OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        fun toggleShuffle() {
            player.shuffleModeEnabled = !player.shuffleModeEnabled
            updatePlaybackState()
        }

        fun refreshPlayer() {
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    if (player.isPlaying) {
                        player.pause()
                        player.play()
                    } else {
                        player.play()
                        player.pause()
                    }
                }
            }
        }

        fun callPause(onPause: () -> Unit) {
            val fadeDisabled = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled) == DurationInMilliseconds.Disabled
            val duration = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled).milliSeconds
            //println("mediaItem callPause fadeDisabled $fadeDisabled duration $duration")
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


    }

    private inner class SessionCallback(private val player: Player) :
        MediaSessionCompat.Callback() {
        override fun onPlay() = player.play()
        //override fun onPause() = player.pause()
        override fun onPause() = binder.callPause({})
        override fun onSkipToPrevious() = runCatching(player::forceSeekToPrevious).let { }
        override fun onSkipToNext() = runCatching(player::forceSeekToNext).let { }
        override fun onSeekTo(pos: Long) = player.seekTo(pos)
        //override fun onStop() = player.pause()
        override fun onStop() = binder.callPause({} )
        override fun onRewind() = runCatching {player.seekToDefaultPosition()}.let { }
        override fun onSkipToQueueItem(id: Long) =
            runCatching { player.seekToDefaultPosition(id.toInt()) }.let { }


        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onCustomAction(action: String, extras: Bundle?) {
            super.onCustomAction(action, extras)
            //println("mediaItem $action")
            if (action == "LIKE") {
                binder.toggleLike()
                refreshPlayer()
            }
            if (action == "SHUFFLE") {
                binder.toggleShuffle()
                refreshPlayer()
            }
            if (action == "PLAYRADIO") {
                    binder.stopRadio()
                    binder.playRadio(NavigationEndpoint.Endpoint.Watch(videoId = binder.player.currentMediaItem?.mediaId))
            }
            updatePlaybackState()
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            if (query.isNullOrBlank()) return
            binder.playFromSearch(query)
        }


        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            mediaButtonEvent?.let {
                if (it.action == Intent.ACTION_MEDIA_BUTTON) {
                    if (it.extras?.getBoolean(Intent.EXTRA_KEY_EVENT) == true) {
                        val keyEvent = it.extras?.getParcelable<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                        when(keyEvent?.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (player.isPlaying)
                                    onPause()
                                else onPlay()

                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                onSkipToNext()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                onSkipToPrevious()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                onStop()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                onPlay()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                onPause()
                                return true
                            }
                        }
                    }
                }
            }
            //return super.onMediaButtonEvent(mediaButtonEvent)
            return false
        }


    }


    private fun refreshPlayer() {
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                if (player.isPlaying) {
                    player.pause()
                    player.play()
                } else {
                    player.play()
                    player.pause()
                }
            }
        }
    }

    inner class NotificationActionReceiver(private val player: Player) : BroadcastReceiver() {


        @ExperimentalCoroutinesApi
        @FlowPreview
        // Prior Android 11
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                //Action.pause.value -> player.pause()
                Action.pause.value -> binder.callPause({ player.pause() } )
                Action.play.value -> player.play()
                Action.next.value -> player.forceSeekToNext()
                Action.previous.value -> player.forceSeekToPrevious()
                Action.like.value -> {
                    binder.toggleLike()
                    refreshPlayer()
                }

                Action.playradio.value -> {
                        binder.stopRadio()
                        binder.playRadio(NavigationEndpoint.Endpoint.Watch(videoId = binder.player.currentMediaItem?.mediaId))
                }

                Action.shuffle.value -> {
                    binder.toggleShuffle()
                    refreshPlayer()
                }
            }

            updatePlaybackState()
        }

    }


    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatching {
                context.stopService(context.intent<PlayerService>())
            }.onFailure {
                Timber.e("Failed NotificationDismissReceiver stopService in PlayerService (PlayerService) ${it.stackTraceToString()}")
            }
        }
    }


    @JvmInline
    private value class Action(val value: String) {
        context(Context)
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                this@Context,
                100,
                Intent(value).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {

            val pause = Action("it.fast4x.riplay.pause")
            val play = Action("it.fast4x.riplay.play")
            val next = Action("it.fast4x.riplay.next")
            val previous = Action("it.fast4x.riplay.previous")
            val like = Action("it.fast4x.riplay.like")
            val download = Action("it.fast4x.riplay.download")
            val playradio = Action("it.fast4x.riplay.playradio")
            val shuffle = Action("it.fast4x.riplay.shuffle")
        }
    }

    private companion object {
        const val NotificationId = 1001
        const val NotificationChannelId = "default_channel_id"

        const val SleepTimerNotificationId = 1002
        const val SleepTimerNotificationChannelId = "sleep_timer_channel_id"


    }


}


