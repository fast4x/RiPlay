package it.fast4x.riplay.services.playback

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Base64
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media.VolumeProviderCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.FlagSet
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
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
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
import it.fast4x.riplay.MainApplication
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
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.utils.timer
import it.fast4x.riplay.R
import it.fast4x.riplay.cast.CastHelper
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.extensions.audiovolume.AudioVolumeObserver
import it.fast4x.riplay.extensions.audiovolume.OnAudioVolumeChangedListener
import it.fast4x.riplay.extensions.discord.DiscordPresenceManager
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOfflinePlayer
import it.fast4x.riplay.extensions.discord.updateDiscordPresenceWithOnlinePlayer
import it.fast4x.riplay.extensions.history.updateOnlineHistory
import it.fast4x.riplay.ui.screens.player.unified.components.customui.CustomDefaultPlayerUiController
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
import it.fast4x.riplay.utils.PrincipalCache
import it.fast4x.riplay.utils.seamlessQueue
import it.fast4x.riplay.commonutils.setLikeState
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.extensions.lastfm.sendNowPlaying
import it.fast4x.riplay.extensions.lastfm.sendScrobble
import it.fast4x.riplay.extensions.players.getOnlineMetadata
import it.fast4x.riplay.cast.ritune.RiTuneCastClient
import it.fast4x.riplay.cast.ritune.models.RiTuneConnectionStatus
import it.fast4x.riplay.cast.ritune.models.RiTuneRemoteCommand
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.enums.CrossfadeDuration
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PlaybackOrigin
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.DiscoveryInfo
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.RelatedItemsService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.service.SongEnricherService
import it.fast4x.riplay.extensions.musicbrainz.MBMetadataHelper
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.musicvault.MusicVaultEvent
import it.fast4x.riplay.musicvault.MusicVaultEvents
import it.fast4x.riplay.musicvault.MusicVaultRepository
import it.fast4x.riplay.musicvault.MusicVaultState
import it.fast4x.riplay.services.helpers.AudioDRCHelper
import it.fast4x.riplay.services.helpers.EqualizerHelper
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget
import it.fast4x.riplay.ui.widgets.PlayerVerticalWidget
import it.fast4x.riplay.ui.widgets.updateState
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.isAtLeastAndroid11
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isPersistentQueueEnabled
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.playAtIndex
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import java.io.File
import java.util.Objects
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import android.os.Binder as AndroidBinder
import it.fast4x.riplay.extensions.appsettings.AppSettingsManager
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.services.playback.common.MediaInfo
import it.fast4x.riplay.services.playback.common.PlaybackContext
import it.fast4x.riplay.services.playback.common.PlaybackState
import it.fast4x.riplay.services.playback.common.PlayerState
import it.fast4x.riplay.utils.BitmapLoader
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.isWebDav
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds

const val SILENT_AUDIO_DATA_URI = "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YQAAAAA="

@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    OnAudioVolumeChangedListener
{
    val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var unifiedMediaSession: MediaSessionCompat
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var mediaLibrarySessionCallback: MediaLibraryServiceCallback
    lateinit var hybridPlayer: HybridPlayer

    val cache: SimpleCache by lazy {
        PrincipalCache.getInstance(this)
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
    //private var closeServiceWhenPlayerPausedAfterMinutes by mutableStateOf(DurationInMinutes.Disabled)

    private var isShowingThumbnailInLockscreen = true
    private var medleyDuration by mutableFloatStateOf(0f)

    private lateinit var audioManager: AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

    var legacyActionReceiver: LegacyActionReceiver? = null

    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    var currentMediaItemState = MutableStateFlow<MediaItem?>(null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val currentSong = currentMediaItemState
        .flatMapLatest { mediaItem ->
            Database.song(mediaItem?.mediaId)
                .catch { e ->
                    Timber.e("PlayerService CurrentSong Errore nel recupero della canzone $e")
                    emit(null)
                }
        }
        .stateIn(serviceScope, SharingStarted.Lazily, null)

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

    var _currentSecond = MutableStateFlow(0f)
    var currentSecond: StateFlow<Float> = _currentSecond

    var _currentDuration = MutableStateFlow(0f)
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
    private var onlineEndHandledMediaId: String? = null
    private var onlineNearEndTicks = 0

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

    private val riTuneCastClient: RiTuneCastClient = RiTuneCastClient()
    private var riTuneObserverJob: Job? = null
    //private var riTunePlayerState: RiTunePlayerState? = null

    private lateinit var equalizerHelper: EqualizerHelper

//    private val globalQueue: GlobalQueueViewModel by lazy {
//        ViewModelProvider(AppSharedScope)[GlobalQueueViewModel::class.java]
//    }

    private var unstartedWatchdogJob: Job? = null

    lateinit var audioQualityFormat: AudioQualityFormat

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothDeviceTypes = buildSet {
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) // Cuffie e stereo multimediali standard
        add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO) // Auricolari in modalità chiamata / Mono headset
        if (isAtLeastAndroid12) {
            add(AudioDeviceInfo.TYPE_BLE_HEADSET) // Nuove cuffie e auricolari True Wireless con Bluetooth LE Audio (Android 13+)
        }
    }

    private val wiredDeviceTypes = buildSet {
        add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        if (isAtLeastAndroid8) {
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
        }
        add(AudioDeviceInfo.TYPE_LINE_ANALOG)
        add(AudioDeviceInfo.TYPE_LINE_DIGITAL)
    }

    private val _currentDiscoveryReason = MutableStateFlow<DiscoveryInfo?>(null)
    val currentDiscoveryReason: StateFlow<DiscoveryInfo?> = _currentDiscoveryReason
    val songEnricher: SongEnricherService = SongEnricherService()
    val relatedItemsService: RelatedItemsService = RelatedItemsService()

    private var settingsObserverJob: Job? = null
    private val appSettingsManager: AppSettingsManager by lazy {
        (appContext() as MainApplication).appSettingsManager
    }
    var appSettings = appSettingsManager.activeSettings.value

    // Istanza unica da passare all'HybridPlayer
    private val ytControlWrapper = YouTubeControlImpl()

    //**********
    var playlistSongsSortBy: PlaylistSongSortBy = PlaylistSongSortBy.DateAdded
    var songsSortBy: SongSortBy = SongSortBy.DateAdded
    var playlistSortBy: PlaylistSortBy = PlaylistSortBy.DateAdded
    var artistSortBy: ArtistSortBy = ArtistSortBy.DateAdded
    var albumSortBy: AlbumSortBy = AlbumSortBy.DateAdded


    var songSortOrder: SortOrder = SortOrder.Descending
    var artistSortOrder: SortOrder = SortOrder.Descending
    var albumSortOrder: SortOrder = SortOrder.Descending
    //**********

    private var isServiceInForeground = false

    private var isFading = false // Flag per ignorare il fade del crossfade
    private var crossfadeJob: Job? = null
    private var fadeInJob: Job? = null
    private val FADE_IN_DURATION_MS = 2000L // Durata di default del fade in


    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    @ExperimentalSerializationApi
    @ExperimentalCoroutinesApi
    @FlowPreview
    @SuppressLint("Range")
    @UnstableApi
    override fun onCreate() {

        _isServiceReady.value = false

        createNotificationChannels()
        startForeground(loading = true)

        super.onCreate()

        // Inizializza app settings prima di tutto
        serviceScope.launch(Dispatchers.Main) {

            // Esegui il setup dei settings in background sul dispatcher IO per non bloccare
            withContext(Dispatchers.IO) {
                startObservingSettings()
            }

            initializeBitmapProvider()
            initializeHybridPlayerAndSession()

            initializeVariables()
            replaceOnlinePlayerView()
            initializeOnlinePlayer()


            initializeUnifiedMediaSession()
            // Aggiorna subito il mediasession per allineare lo stato delle azioni
            if (!_playerState.value.isPlaying && _internalOnlinePlayer.value == null) {
                _playerState.update { it.copy(playbackState = PlaybackState.PAUSED) }
                updateUnifiedMediasession()
            }


            startForeground()

            checkAndRestoreTimer()

            initializeAudioManager()
            initializeAudioVolumeObserver()
            initializeAudioEqualizer()
            initializeLegacyNotificationActionReceiver()

            initializeAudioDeviceCallback()
            initializeNormalizeVolume()
            initializeBassBoost()
            initializeReverb()
            initializeSensorListener()
            initializeSongCoverInLockScreen()
            initializeMedleyMode()
            applyPlaybackParameters()
            initializeAudioDRCHelper()

            initializeRiTune()
            initializeDiscordPresence()

            setupPersistentQueueAndObservers()

            _isServiceReady.value = true
        }
    }

    @kotlin.OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
    private fun setupPersistentQueueAndObservers() {
        if (isPersistentQueueEnabled) {
            serviceScope.launch {
                // Caricamento iniziale obbligatorio sul Main thread per ExoPlayer
                withContext(Dispatchers.Main) {
                    loadQueue()
                    resumePlaybackOnStart()
                }

                var secondsWhilePaused = 0
                while (isActive) {
                    delay(10.seconds)
                    val isPlaying = _playerState.value.isPlaying
                    if (isPlaying) {
                        secondsWhilePaused = 0
                        saveQueue()
                        Timber.d("PlayerService saveQueue when playing")
                    } else {
                        secondsWhilePaused += 10
                        if (secondsWhilePaused >= 60) {
                            secondsWhilePaused = 0
                            saveQueue()
                            Timber.d("PlayerService saveQueue periodic when not playing")
                        }
                    }

                    if (_currentSecond.value >= minTimeForEvent.seconds && lastMediaIdInHistory != currentSong.value?.id) {
                        currentSong.value?.let {
                            updateOnlineHistory(it.asMediaItem)
                            lastMediaIdInHistory = it.id
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            currentSong.collect(serviceScope) { song ->
                if (song == null) return@collect

                Timber.d("PlayerService onCreate update currentSong $song mediaItemState ${currentMediaItemState.value}")

                withContext(Dispatchers.Main) {
                    updateUnifiedMediasession()
                    updateUnifiedNotification()
                }


                val currentMediaId = if (!song.isLocal) song.id else song.mediaId.toString()

                if (lastOnlineMediaId != currentMediaId && onlineListenedDurationMs > 0) {
                    Timber.d(
                        "PlayerService incrementOnlineListenedPlaytimeMs update currentSong onlineListenedDurationMs = $onlineListenedDurationMs" +
                                " onlineMediaId = $currentMediaId currentMediaId = $currentMediaId"
                    )
                    incrementOnlineListenedPlaytimeMs()
                    delay(200.milliseconds)
                    onlineListenedDurationMs = 0L
                    lastOnlineMediaId = currentMediaId
                }


                val format = Database.format(currentMediaId).first()
                if (format == null && (!song.isLocal || song.isWebDav)) {
                    getOnlineMetadata(currentMediaId)
                        ?.let {
                            //Timber.d("PlayerService onCreate update currentSong onlinemetadata it $it")
                            val duratiomMs = it.videoDetails?.lengthSeconds?.toLong()
                            try {
                                Database.insert(
                                    Format(
                                        songId = currentMediaId,
                                        contentLength = duratiomMs,
                                        loudnessDb = it.playerConfig?.audioConfig?.loudnessDb
                                            ?: it.playerConfig?.audioConfig?.perceptualLoudnessDb?.toFloat(),
                                        playbackUrl = it.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                                    )
                                )
                            } catch (e: Exception) {
                                Timber.e("PlayerService onCreate update currentSong exception ${e.stackTraceToString()}")
                            }


                            // Aggiorno la durata se è nulla nel db
                            if (currentSong.value?.durationText == "0:00" && duratiomMs != null) {
                                Database.updateDurationText(song.id, formatAsDuration(duratiomMs))
                            }


                        }
                }

                withContext(Dispatchers.Main) {
                    _playerState.update { currentState ->
                        currentState.copy(
                            mediaInfo = MediaInfo(
                                mediaItem = song.asMediaItem,
                                queueIndex = player.currentMediaItemIndex,
                                queueSize = player.mediaItems.size
                            ),
                            errorMessage = null,
                        )
                    }
                }
            }
        }

        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (localMediaItem?.isLocal == false) {
                    if (_playerState.value.isPlaying) {
                        onlineListenedDurationMs += 1000
                        if (onlineListenedDurationMs >= 20000) {
                            incrementOnlineListenedPlaytimeMs()
                            delay(200.milliseconds)
                            onlineListenedDurationMs = 0L
                        }
                    } else {
                        if (onlineListenedDurationMs > 0) {
                            incrementOnlineListenedPlaytimeMs()
                            delay(200.milliseconds)
                            onlineListenedDurationMs = 0L
                        }
                    }
                    //fallback if online player not fire state ended
                    //updateOnlineNearEndTicks() Experimental aternative whatchdog for end time
                    if (_currentDuration.value > 0
                        && appSettings.queueLoopType == QueueLoopType.Default
                    ) {
                        if (_currentSecond.value >= _currentDuration.value - 0.5f) {
                            if (_playerState.value.isPlaying) {
                                Timber.d("PlayerService Watchdog: End of online track detected by time, forcing playNext()")
                                handlePlayNext()
                            }
                        }
                    }

                }
                delay(1000.milliseconds)
            }
        }

        serviceScope.launch {
            MusicVaultEvents.events.collect { event ->
                when (event) {
                    is MusicVaultEvent.DownloadCompleted -> {
                        updateMusicVaultMediaItem(
                            songId = event.songId,
                            fileName = event.fileName,
                            thumbnailFileName = event.thumbnailFileName
                        )
                    }

                    is MusicVaultEvent.DownloadRemoved -> {
                        updateMusicVaultMediaItem(
                            songId = event.songId,
                            fileName = "",
                            thumbnailFileName = ""
                        )
                    }
                }
            }
        }

        updateWidgetState()
    }

    private fun startObservingSettings() {
        // Devo essere sicuro che le impostazioni siano pronte
        appSettings = runBlocking(Dispatchers.IO) {
            appSettingsManager.waitForInitialization()
        }
        settingsObserverJob = serviceScope.launch {
            appSettingsManager.activeSettings      
                .collect { settings -> 
                    Timber.d("PlayerService: impostazioni cambiate $settings")

                    when {
                        (settings.songSortOrder != songSortOrder) -> {
                            songSortOrder = settings.songSortOrder
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.SONGS)
                        }
                        (settings.artistSortOrder != artistSortOrder) -> {
                            artistSortOrder = settings.artistSortOrder
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.ARTISTS_FAVORITES)
                        }
                        (settings.albumSortOrder != albumSortOrder) -> {
                            albumSortOrder = settings.albumSortOrder
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.ALBUMS_FAVORITES)
                        }
                        (settings.playlistSortBy != playlistSortBy) -> {
                            playlistSortBy = settings.playlistSortBy
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.PLAYLISTS)
                        }
                        (settings.playlistSongsSortBy != playlistSongsSortBy) -> {
                            playlistSongsSortBy = settings.playlistSongsSortBy
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.PLAYLISTS)
                        }
                        (settings.artistSortBy != artistSortBy) -> {
                            artistSortBy = settings.artistSortBy
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.ARTISTS_FAVORITES)
                        }
                        (settings.albumSortBy != albumSortBy) -> {
                            albumSortBy = settings.albumSortBy
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.ALBUMS_FAVORITES)
                        }
                        (settings.songSortBy != songsSortBy) -> {
                            songsSortBy = settings.songSortBy
                            notifyAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.SONGS)
                        }
                    }


                    appSettings = settings
                }
        }
        
    }

    fun notifyAutoChildrenChanged(parentId: String) {
        // 1. Controlla che la sessione non sia null (usando il ?. safe call)
        mediaLibrarySession?.let { session ->

            val params = MediaLibraryService.LibraryParams.Builder()
                .setExtras(Bundle().apply {
                    // Diciamo esplicitamente al sistema dell'auto che il contenuto di questo specifico nodo è cambiato
                    putBoolean("android.media.browse.extra.DOWNLOAD_PROGRESS", true) // Sveglia il sistema di caricamento visivo
                })
                .build()

            // 2. connectedControllers è una proprietà di MediaSession/LibrarySession
            for (controller in session.connectedControllers) {

                // 3. Chiamiamo il metodo SULLA SESSIONE
                session.notifyChildrenChanged(
                    controller,
                    parentId,
                    0,
                    params
                )
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Se per qualche motivo il servizio è stato avviato ma non è ancora in foreground, proteggiti
        if (!isServiceInForeground) {
            startForeground(loading = true)
        }

        super.onStartCommand(intent, flags, startId)

        Timber.d("PlayerService onStartCommand intent action ${intent?.action}")
        when (intent?.action) {
            Action.play.value -> { if (localMediaItem?.isLocal == true) player.play() else _internalOnlinePlayer.value?.play() }
            Action.pause.value -> { if (localMediaItem?.isLocal == true) player.pause() else _internalOnlinePlayer.value?.pause() }
            Action.next.value -> handlePlayNext()
            Action.previous.value -> player.playPrevious()
        }
        updateWidgetState()

        return START_STICKY
    }

    @ExperimentalCoroutinesApi
    private fun startForeground(loading: Boolean = false) {

        // Se siamo già in foreground e non è un caricamento, usiamo il NotificationManager per aggiornare
        if (isServiceInForeground && !loading) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification())
            return
        }

        //Timber.d("PlayerService startForeground called from: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        val notification = if (loading) {
            NotificationCompat
                .Builder(this@PlayerService, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(resources.getString(R.string.loading_please_wait))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.app_icon)
                .setSilent(true)
                .build()
        } else {
            notification()
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (isAtLeastAndroid11) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            )
            isServiceInForeground = true
        } catch (e: Exception) {
            if (isAtLeastAndroid12 && e is ForegroundServiceStartNotAllowedException) {
                // Fallback per i produttori OEM aggressivi
                Timber.e("PlayerService Impossibile portare il servizio in foreground da background.")
            } else {
                throw e
            }
        }

    }

    private fun initializeVariables() {

        isPersistentQueueEnabled = appSettings.persistentQueue
        isResumePlaybackOnStart = appSettings.resumePlaybackOnStart
        isShowingThumbnailInLockscreen =
            appSettings.isShowingThumbnailInLockscreen

        medleyDuration = appSettings.playbackDuration

        currentMediaItemState.value = player.currentMediaItem
        audioQualityFormat = appSettings.audioQualityFormat

        closeServiceAfterMinutes = appSettings.closeBackgroundPlayerAfterMinutes

//        closeServiceWhenPlayerPausedAfterMinutes = preferences.getEnum(
//            closePlayerServiceWhenPausedAfterMinutesKey.key, DurationInMinutes.Disabled
//        )
    }

    private fun replaceOnlinePlayerView() {
        _internalOnlinePlayer.value?.pause()
        _internalOnlinePlayer.value = null
        _internalOnlinePlayerView.value.release()
        _internalOnlinePlayerView.value = LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false) as YouTubePlayerView
    }

    private fun applyPlaybackParameters() {
        val speed = appSettings.playbackSpeed
        val pitch = appSettings.playbackPitch

        if (localMediaItem?.isLocal == false) {
            // Mappatura matematica al valore discreto di YouTube più vicino
            val ytValidRates = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
            val closestYtRate = ytValidRates.minByOrNull { kotlin.math.abs(it - speed) } ?: 1.0f

            // Mappa il float nell'enum della libreria
            val onlineRate = when (closestYtRate) {
                0.25f -> PlayerConstants.PlaybackRate.RATE_0_25
                0.5f -> PlayerConstants.PlaybackRate.RATE_0_5
                0.75f -> PlayerConstants.PlaybackRate.RATE_0_75
                1.25f -> PlayerConstants.PlaybackRate.RATE_1_25
                1.5f -> PlayerConstants.PlaybackRate.RATE_1_5
                1.75f -> PlayerConstants.PlaybackRate.RATE_1_75
                2.0f -> PlayerConstants.PlaybackRate.RATE_2
                else -> PlayerConstants.PlaybackRate.RATE_1
            }

            _internalOnlinePlayer.value?.setPlaybackRate(onlineRate)
        } else {
            // ExoPlayer gestisce speed e pitch in modo continuo e perfetto
            hybridPlayer.playbackParameters = PlaybackParameters(speed, pitch)
        }
    }

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

        val isRiTuneEnabled = appSettings.castType == CastType.RITUNECAST
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
                            PlayerConstants.PlayerState.PLAYING -> {
                                startEndedObserver()
                                startCrossfadeMonitor()
                            }
                            else -> {
                                stopEndedObserver()
                                stopCrossFadeMonitor()
                            }
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

        if (appSettings.isDiscordPresenceEnabled) {
            val token = appSettings.discordPersonalAccessToken
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
        if (appSettings.shakeEventEnabled) {
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

            if (appSettings.shakeEventEnabled) {
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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private fun initializeUnifiedMediaSession() {

        unifiedMediaSession = MediaSessionCompat(this, "PlayerService")

        val repeatMode = appSettings.queueLoopType.type

        unifiedMediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        unifiedMediaSession.setRepeatMode(repeatMode)

        if (appSettings.useVolumeKeysToChangeSong)
            unifiedMediaSession.setPlaybackToRemote(getVolumeProvider())

        initializeUnifiedSessionCallback()

        unifiedMediaSession.isActive = true
        unifiedMediaSession.setMediaButtonReceiver(null)

    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    fun recreateOnlinePlayerView() {
        replaceOnlinePlayerView()
        serviceScope.launch { initializeOnlinePlayer(skipAutoload = true) }
    }

    private fun initializeHybridPlayerAndSession() {

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRendersFactory())
//            .setMediaSourceFactory(
//                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this))
//            )
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

        player.repeatMode = appSettings.queueLoopType.type

        player.skipSilenceEnabled = appSettings.skipSilenceEnabled
        player.pauseAtEndOfMediaItems = true

        // Crea l'Hybrid Player
        hybridPlayer = HybridPlayer(this,player, ytControlWrapper)

        hybridPlayer.onRefreshCustomLayoutListener = {
            val activeSession = mediaLibrarySession
            if (activeSession != null) {
                // Chiamiamo il metodo dentro il tuo callback della sessione passando l'istanza corretta
                mediaLibrarySessionCallback.updateCustomLayout(activeSession)
            }
        }

        if (mediaLibrarySession != null) {
            return
        }

        val customBitmapLoader = CacheBitmapLoader(BitmapLoader(
            this,
            serviceScope,
            (512 * resources.displayMetrics.density).roundToInt()
        ))

        mediaLibrarySessionCallback = MediaLibraryServiceCallback(binder, this)

        mediaLibrarySession = MediaLibrarySession
            .Builder(this@PlayerService, hybridPlayer, mediaLibrarySessionCallback)
            .setId("${packageName}.MEDIA_SESSION_ID")
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            ).setBitmapLoader(
                customBitmapLoader
//                BitmapLoader(
//                    this,
//                    serviceScope,
//                    (512 * resources.displayMetrics.density).roundToInt()
//                )
            )
            .build()

    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun handleMediaItemsRequest(items: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        val itemToPlay = items[startIndex]
        val mediaId = itemToPlay.mediaId.substringAfter("/")
        val isLocal = mediaId.isLocal
        Timber.d("PlayerService handleMediaItemsRequest itemToPlay ${itemToPlay.mediaId} cleaned = $mediaId isLocal = $isLocal")

        // Aggiorniamo lo stato per la notifica
        currentMediaItemState.value = itemToPlay
        localMediaItem = itemToPlay

        val safeItems = items.map { item ->
            // 1. Puliamo SOLO il suffisso di navigazione di Auto (es. "songs/")
            // ATTENZIONE: NON puliamo "local:" perché il tuo DataSource lo vuole!
            val id = item.mediaId.substringAfter("/")

            // Esempio: se Auto manda "songs/local:10002998201",
            // "id" diventerà "local:10002998201" (perfetto per il tuo DataSource)
            val isLocal = id.isLocal

            if (!isLocal) {
                // --- YOUTUBE ---
                // Non ha un URI. Usiamo il file silenzioso per far stare zitto ExoPlayer.
                if (item.localConfiguration == null) {
                    item.buildUpon().setUri(SILENT_AUDIO_DATA_URI).build()
                } else {
                    item
                }
            } else {
                // --- FILE LOCALE ---
                // Manca il localConfiguration (nessun URI).
                // Passiamo DIRETTAMENTE l'ID (che contiene già "local:10002998201")
                // Questo farà felici sia ExoPlayer (non crasha) che il tuo DataSource!
                if (item.localConfiguration == null) {
                    item.buildUpon().setUri(id).build()
                } else {
                    item
                }
            }
        }

        val cleanItems = safeItems.map { item ->
            val realId = item.mediaId.substringAfter("/")
            item.buildUpon().setMediaId(realId).setUri(realId).build()
        }.toMutableList()

        // 3. Passiamo la lista PULITA ad ExoPlayer
        //player.setMediaItems(cleanItems, startIndex, startPositionMs)
        //player.prepare()

        // 3. GESTIONE DEL PLAY: Controlliamo se la Timeline è stata popolata
        if (startIndex < hybridPlayer.currentTimeline.windowCount) {
            Timber.d("PlayerService handleMediaItemsRequest play seekToDefaultPosition $startIndex")

            if (!isLocal) {
                // LOGICA YOUTUBE: Diciamo all'HybridPlayer di prepararsi alla WebView
                hybridPlayer.switchToYoutube()
            } else {
                // LOGICA LOCALE: Diciamo all'HybridPlayer di prepararsi per ExoPlayer
                hybridPlayer.switchToExo()
            }

            // Questo comando è sincrono e farà SCATTARE immediatamente il tuo onMediaItemTransition!
            //hybridPlayer.seekToDefaultPosition(startIndex)
            hybridPlayer.forcePlayAtIndex(cleanItems, startIndex)

        } else {
            Timber.e("PlayerService handleMediaItemsRequest ERRORE: Timeline vuota o fuori range. WindowCount: ${hybridPlayer.currentTimeline.windowCount}, Index: $startIndex")

            // FALLBACK: Se la timeline è vuota (probabilmente perché i file locali non avevano l'URI
            // e ExoPlayer li ha ignorati), forziamo il play diretto senza passare dalla coda.
            if (!isLocal) {
                hybridPlayer.switchToYoutube()
                val startFrom = when { startPositionMs > 0 -> startPositionMs.toFloat() / 1000f else -> 0f }
                _internalOnlinePlayer.value?.cueVideo(mediaId, startFrom)
                updateUnifiedNotification() // Aggiorniamo la notifica manualmente qui
            }
        }
    }
    @ExperimentalCoroutinesApi
    suspend private fun initializeOnlinePlayer(skipAutoload: Boolean = false) {

        val onlinePlayerView = _internalOnlinePlayerView.value

        val listener = object : AbstractYouTubePlayerListener() {

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)

                if (onlinePlayerView !== _internalOnlinePlayerView.value) {
                    youTubePlayer.pause()
                    return
                }

                _internalOnlinePlayer.value = youTubePlayer

                val customUiController =
                    CustomDefaultPlayerUiController(
                        this@PlayerService,
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

                Timber.d("PlayerService onlinePlayer onReady localmediaItem ${localMediaItem?.mediaId} queue index ${binder.player?.currentMediaItemIndex}")
                Timber.d("PlayerService onlinePlayer onReady isPersistentQueueEnabled $isPersistentQueueEnabled isResumePlaybackOnStart $isResumePlaybackOnStart")

                youTubePlayer.setVolume(getSystemMediaVolume())

                if (localMediaItem?.isLocal == true) return

                localMediaItem?.let{
                    if (isPersistentQueueEnabled && isResumePlaybackOnStart && firstTimeStarted && !skipAutoload) {
                        youTubePlayer.loadVideo(it.mediaId, playFromSecond)
                        playFromSecond = 0f
                        Timber.d("PlayerService onlinePlayer onReady loadVideo ${it.mediaId}")
                    }
                }

                firstTimeStarted = false

            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                val oldSecond = _currentSecond.value
                _currentSecond.value = second

                if (oldSecond == 0f || kotlin.math.abs(second - oldSecond) >= 1f) {
                    if (hybridPlayer.activeEngine == ActiveEngine.YOUTUBE) {
                        val posEvents = Player.Events(
                            FlagSet.Builder()
                                .add(Player.EVENT_IS_PLAYING_CHANGED)
                                .build()
                        )
                        hybridPlayer.forwardEventsToSession(posEvents)
                    }
                }
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                super.onVideoDuration(youTubePlayer, duration)

                _currentDuration.value = duration

                /*
                val new = appSettings.copy(
                    stateDuration = duration,
                    stateMediaId = localMediaItem?.mediaId ?: ""
                )
                serviceScope.launch {
                    AppSettingsManager().updateSettings(new)
                }

                 */

                updateUnifiedNotification()
                updateDiscordPresence()

                if (duration > 0f && hybridPlayer.activeEngine == ActiveEngine.YOUTUBE) {
                    val timelineEvents = Player.Events(
                        FlagSet.Builder()
                            .add(Player.EVENT_TIMELINE_CHANGED)
                            .build()
                    )
                    hybridPlayer.forwardEventsToSession(timelineEvents)
                }
                hybridPlayer.updateCurrentMediaItemDuration(duration.toLong() * 1000L)
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
                            unstartedWatchdogJob = serviceScope.launch(Dispatchers.Main) {
                                Timber.d("PlayerService onlinePlayerView: onStateChange UNSTARTED watchdog")
                                delay(1000)

                                if (_playerState.value.playbackState == PlaybackState.UNSTARTED) {
                                    Timber.e("PlayerService onlinePlayerView: Persistent UNSTARTED state. Probably webView killed. Force to re-initialize.")

                                    recreateOnlinePlayerView()
                                    val currentPlayer = this@PlayerService._internalOnlinePlayer.first { it != null }!!

                                    localMediaItem?.let { item ->
                                        if(item.isLocal) return@let
                                        Timber.d("PlayerService onlinePlayerView: Try reload song/video")
                                        // Assicura che ExoPlayer sia fermo prima del recovery
                                        if (player.isPlaying) {
                                            player.pause()
                                            player.stop()
                                        }
                                        currentPlayer.pause()
                                        _internalOnlinePlayer.value?.pause() // Pause also primary instance
                                        currentPlayer.cueVideo(item.mediaId, playFromSecond)
                                    }

                                }
                            }
                        }
                    }

                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                        Timber.d("PlayerService onlinePlayerView: onStateChange VIDEO_CUED regular play()")
                        playFromSecond = 0f
                        _internalOnlinePlayer.value?.pause()
                        youTubePlayer.pause()
                        if (!firstTimeStarted) {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                                youTubePlayer.unMute()
                                //youTubePlayer.setVolume(getSystemMediaVolume())
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
                        lastError = null  // reset errore dopo riproduzione riuscita
                        onlineNearEndTicks = 0
                        startEndedObserver()
                        startCrossfadeMonitor()
                        sendOpenExternalEqualizerIntent()

                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        onlineNearEndTicks = 0
                        stopEndedObserver()
                        stopCrossFadeMonitor()
                        sendCloseExternalEqualizerIntent()

                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }
//                        PlayerConstants.PlayerState.ENDED -> {
//                            Timber.d("PlayerService onlinePlayerView: onStateChange ENDED regular playNext()")
//                            player.playNext()
//                        }
                    else -> {}
                }

                /*
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
                 */

                isPlayingNow = state == PlayerConstants.PlayerState.PLAYING

//                if (::hybridPlayer.isInitialized) {
//                    hybridPlayer.invalidateYouTubeState()
//                }

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
                    serviceScope.launch { saveQueue() }


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
                            if(it.isLocal) return@let
                            // Assicura che ExoPlayer sia fermo
                            if (player.isPlaying) {
                                player.pause()
                                player.stop()
                            }

                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                                _internalOnlinePlayer.value?.pause()
                                youTubePlayer.pause()
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

                    //youTubePlayer.setVolume(getSystemMediaVolume())
                    return
                }

                lastError = error

                if (!isSkipMediaOnErrorEnabled()) return
                val prev = binder.player?.currentMediaItem ?: return

                // Ferma ExoPlayer se sta andando
                if (player.isPlaying) {
                    player.pause()
                    player.stop()
                }

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
        if (CastHelper.isCastAvailable
            && appSettings.castType !in listOf(CastType.NONE, CastType.RITUNECAST)) {
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
        onlinePlayerView.apply {
            enableAutomaticInitialization = false

            enableBackgroundPlayback(true)

            keepScreenOn = isKeepScreenOnEnabled()

            val iFramePlayerOptions = IFramePlayerOptions.Builder(appContext())
                .controls(0)
                .listType("playlist")
                .origin(resources.getString(R.string.env_fqqhBZd0cf))
                .autoplay(1)
                .mute(1)
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

        updateWidgetState()
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

    @ExperimentalCoroutinesApi
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
                        val useVolumeKeysToChangeSong = appSettings.useVolumeKeysToChangeSong
                        // Up = 1, Down = -1, Release = 0
                        if (direction == VOLUME_UP) {
                            if (binder.player?.isPlaying == true && useVolumeKeysToChangeSong) {
                                binder.player?.forceSeekToNext()
                            } else {
                                audioManager.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                setCurrentVolume(audioManager.getStreamVolume(STREAM_TYPE))
                            }
                        } else if (direction == VOLUME_DOWN) {
                            if (binder.player?.isPlaying == true && useVolumeKeysToChangeSong) {
                                binder.player?.forceSeekToPrevious()
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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled) {
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.shuffleOrder = DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis())
        }
        updateUnifiedNotification()

        serviceScope.launch { saveQueue() }
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

        _isServiceReady.value = false

        sendCloseExternalEqualizerIntent()

        serviceScope.launch { saveQueue() }


        try {
            unregisterReceiver(legacyActionReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver ${e.message}")
        }

        if (::unifiedMediaSession.isInitialized) {
            unifiedMediaSession.isActive = false
            unifiedMediaSession.release()
        }

        if(::equalizerHelper.isInitialized) {
            equalizerHelper.release()
        }

        if (::hybridPlayer.isInitialized) {
            hybridPlayer.release()
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

        serviceScope.cancel()

        runCatching {

            //preferences.unregisterOnSharedPreferenceChangeListener(this)

            mediaLibrarySession?.release()
            cache.release()
            loudnessEnhancer?.release()
            audioVolumeObserver.unregister()
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
            settingsObserverJob?.cancel()
            settingsObserverJob = null

            AudioDRCHelper.restoreDRC()

            notificationManager?.cancelAll()
            //coroutineScope.launch { delay(500) }
            unregisterAudioDeviceCallback()


        }.onFailure {
            Timber.e("Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceInForeground = false
        stopSelf()

        super.onDestroy()
    }

    private fun tryHandleOnlineTrackEnd(source: String) {
    val mediaId = localMediaItem?.mediaId ?: return
    val now = System.currentTimeMillis()
    if (onlineEndHandledMediaId == mediaId && (now - lastPlayNextTime) < debounceDelayMs) {
        Timber.d("PlayerService tryHandleOnlineTrackEnd ignored duplicate for $mediaId from $source")
        return
    }
    onlineEndHandledMediaId = mediaId
    lastPlayNextTime = now
    onlineNearEndTicks = 0
    Timber.d("PlayerService tryHandleOnlineTrackEnd accepted for $mediaId from $source")
    handlePlayNext()
}

private fun resetOnlineEndGuardIfTrackChanged() {
    //Timber.d("PlayerService Watchdog: resetOnlineEndGuardIfTrackChanged onlineNearEndTicks $onlineNearEndTicks")
    val mediaId = localMediaItem?.mediaId
    if (mediaId == null) {
        onlineEndHandledMediaId = null
        onlineNearEndTicks = 0
        return
    }
    if (onlineEndHandledMediaId != null && onlineEndHandledMediaId != mediaId) {
        onlineEndHandledMediaId = null
        onlineNearEndTicks = 0
        Timber.d("PlayerService Watchdog: resetOnlineEndGuardIfTrackChanged reset onlineNearEndTicks")
    }
}

private fun updateOnlineNearEndTicks() {
    resetOnlineEndGuardIfTrackChanged()

    val shouldTrackNearEnd =
        localMediaItem?.isLocal == false &&
                appSettings.queueLoopType == QueueLoopType.Default &&
        _playerState.value.isPlaying &&
        _currentDuration.value > 0f &&
        _currentSecond.value >= (_currentDuration.value - 0.5f)

    //Timber.d("PlayerService Watchdog: updateOnlineNearEndTicks shouldTrackNearEnd $shouldTrackNearEnd")

    if (shouldTrackNearEnd) {
        onlineNearEndTicks += 1
        if (onlineNearEndTicks >= 2) {
            Timber.d("PlayerService Watchdog: End of online track detected by time, trying guarded playNext()")
            tryHandleOnlineTrackEnd("watchdog_near_end")
        }
    } else {
        onlineNearEndTicks = 0
    }
}

    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (appSettings.isPauseOnVolumeZeroEnabled) {
            if ((player.isPlaying || _playerState.value.isPlaying) && currentVolume < 1) {
                if (player.currentMediaItem?.isLocal == true) {
                    player.pause()
                } else {
                    _internalOnlinePlayer.value?.pause()
                }
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                if (player.currentMediaItem?.isLocal == true) {
                    player.play()
                } else {
                    _internalOnlinePlayer.value?.play()
                }
                pausedByZeroVolume = false
            }
        }

        // Questo serve per il FadeOut

        // Se è in corso un fade out automatico, fermiamoci subito!
        if (isFading) {
            fadeInJob?.cancel()
            crossfadeJob?.cancel() // Ferma anche il fade out se l'utente cambia volume a fine brano
            isFading = false
        }

        // Converto il volume di sistema (es. 0-15) nel float del player (0.0-1.0)
        val newPlayerVolume = currentVolume.toFloat() / maxVolume.toFloat()

        serviceScope.launch(Dispatchers.IO) {
            appSettingsManager.updateSettings(appSettings.copy(userVolume = newPlayerVolume))
        }

        // Ora, impostiamo il volume su HybridPlayer per mantenerlo sincronizzato
        // QUESTO farà scattare l'onVolumeChanged di ExoPlayer se serve,
        // o aggiornerà la WebView se è attiva.
        hybridPlayer.setVolume(newPlayerVolume)

//        if (localMediaItem?.isLocal == false) {
//            val onlineVolume = getSystemMediaVolume()
//            Timber.d("PlayerService onAudioVolumeChanged currentVolume $currentVolume onlineVolume $onlineVolume")
//            _internalOnlinePlayer.value?.setVolume(onlineVolume)
//        }
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

    override fun onPlaybackStateChanged(playbackState: Int) {
        Timber.d("PlayerService onPlaybackStateChanged state=${
            when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
        }")
    }

    @UnstableApi
    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {

        Timber.d("PlayerService onPlaybackStatsReady CALLED eventTime $eventTime playbackStats $playbackStats")

        if (appSettings.isPauseListenHistoryEnabled) return

        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (!mediaItem.isLocal) return

        Timber.d("PlayerService onPlaybackStatsReady PROCESS eventTime $eventTime playbackStats $playbackStats")

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            Timber.d("PlayerService onPlaybackStatsReady INCREMENT totalPlayTimeMs $totalPlayTimeMs mediaItem ${mediaItem.mediaId}")
            serviceScope.launch {
                Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }


        val minTimeForEvent = appSettings.minTimeForEvent

        if (totalPlayTimeMs > minTimeForEvent.ms) {
            Timber.d("PlayerService onPlaybackStatsReady INSERT EVENT totalPlayTimeMs $totalPlayTimeMs")
            serviceScope.launch {
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

    @ExperimentalCoroutinesApi
    @FlowPreview
    @UnstableApi
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        Timber.d("PlayerService onMediaItemTransition mediaId=${mediaItem?.mediaId} uri=${mediaItem?.localConfiguration?.uri} musicVaultState=${mediaItem?.mediaMetadata?.extras?.getString("musicVaultState")} musicVaultFileName=${mediaItem?.mediaMetadata?.extras?.getString("musicVaultFileName")}")

        if (mediaItem == null) return

        // Ferma il fade out se era in corso (es. l'utente ha premuto "Next" a metà brano)
        stopCrossFadeMonitor()

        applyPlaybackParameters()

        val origin = PlaybackContext.currentOrigin.value
        val suggestionInfo = PlaybackContext.currentSuggestionInfo.value
        val isFromSuggestion = origin == PlaybackOrigin.SUGGESTION &&
                suggestionInfo?.itemId == mediaItem.mediaId

        Timber.d("PlayerService onMediaItemTransition to ${mediaItem.mediaId}, origin=$origin, isSuggestion=$isFromSuggestion")

        // todo in the future save in preferences if enabled
        serviceScope.launch {
            if (isFromSuggestion) {
                binder.setDiscoverySource(
                    strategyId = suggestionInfo.strategyId,
                    strategyName = suggestionInfo.strategyName,
                    reasons = suggestionInfo.reasons,
                    itemId = mediaItem.mediaId
                )
            } else {
                binder.clearDiscoverySource()
            }

            // 2. Enrichment (solo se NON è da suggerimento, perché i suggerimenti sono già arricchiti)
            if (!isFromSuggestion && origin != PlaybackOrigin.RELATED) {
                Timber.d("PlayerService onMediaItemTransition Triggering enrichment for non-suggestion song")
                songEnricher.onSongPlayed(mediaItem.mediaId)
            }

            // 3. Preload related items (in ogni caso, per UI pronta)
            relatedItemsService.preloadRelated(mediaItem.mediaId)

            // 4. Registra evento di ascolto nel profilo utente
            recordListeningEvent(mediaItem.mediaId)
        }

        currentMediaItemState.value = mediaItem
        localMediaItem = mediaItem
        _internalOnlinePlayer.value?.pause() // stop online player latency

        _currentSecond.value = 0F

        val newMediaId = mediaItem.mediaId

        if (lastOnlineMediaId == newMediaId) {
            Timber.d("PlayerService: onMediaItemTransition Transition ignored, same MediaID ($newMediaId) skipped")
            handlePlayNext()
            return
        }

        Timber.d("PlayerService onMediaItemTransition mediaItem ${mediaItem.mediaId} reason $reason")

        currentQueuePosition = player.currentMediaItemIndex

        if (parentalControlEnabled && mediaItem.isExplicit) {
            handlePlayNext()
            SmartMessage(resources.getString(androidx.media3.session.R.string.error_message_parental_control_restricted), context = this@PlayerService)
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

            if (!it.isLocal){
                hybridPlayer.switchToYoutube()
                Timber.d("PlayerService onMediaItemTransition mediaItem not local, before")
                // Ferma ExoPlayer prima di avviare il player online
                if (player.isPlaying) {
                    player.pause()
                }
                _internalOnlinePlayer.value?.pause()

                if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                    _internalOnlinePlayer.value?.cueVideo(it.mediaId, playFromSecond)
                    // Avvia il fade in per il nuovo brano appena parte il play
                    startFadeIn(appSettings.userVolume)
                    Timber.d("PlayerService onMediaItemTransition mediaItem not local, inside")
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

                //_internalOnlinePlayer.value?.setVolume(getSystemMediaVolume())

                // Recupera genere
                val mbclient = MusicBrainz()
                val genreHelper = MBMetadataHelper(mbclient)
                serviceScope.launch {
                    genreHelper.onSongPlayed(it.mediaId)
                }

            } else {
                // Stop prima di lanciare il prossimo brano e stop a exo per sicurezza
                _internalOnlinePlayer.value?.pause()
                player.pause()

                hybridPlayer.switchToExo()
                // Canzone locale o MusicVault — ferma il player online e lascia andare ExoPlayer
                _internalOnlinePlayer.value?.pause()
                Timber.d("PlayerService onMediaItemTransition resume playback before firstTimeStarted $firstTimeStarted isResumePlaybackOnStart $isResumePlaybackOnStart")
                if (firstTimeStarted && isResumePlaybackOnStart) {
                    resumePlaybackOnStart()
                    firstTimeStarted = false
                    Timber.d("PlayerService onMediaItemTransition resume playback inside")
                    return
                }

                if (firstTimeStarted && !isResumePlaybackOnStart) {
                    firstTimeStarted = false
                    return
                }

                Timber.d("PlayerService onMediaItemTransition resume playback after")

                if (!player.isPlaying) {
                    Timber.d("PlayerService onMediaItemTransition prepare exo for play local file")
                    player.prepare()
                    player.playWhenReady = true
                    player.play()

                    // Avvia il fade in per il nuovo brano
                    startFadeIn(appSettings.userVolume)
                }
            }

            bitmapProvider?.load(it.mediaMetadata.artworkUri) { bitmap ->
                serviceScope.launch {
                    setWallpaper(this@PlayerService, bitmap)
                }
            }
        }

        updateWidgetState()

        // maybe not needed
        //maybeRecoverPlaybackError()
        initializeNormalizeVolume()
        maybeProcessRadio(reason)

        updateUnifiedNotification()

        updateDiscordPresence()

        serviceScope.launch { saveQueue() }

        if (appSettings.isEnabledLastFM) {
            appSettings.lastFMSessionToken.let {
                when (appSettings.lastFmScrobbleType) {
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
        Timber.d("PlayerService onMediaItemTransition mediaItem: ${mediaItem.mediaId} currentMediaItemIndex: $currentQueuePosition shuffleModeEnabled ${player.shuffleModeEnabled} repeatMode ${player.repeatMode} reason $reason")

    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            updateMediaSessionQueue(timeline, player.currentMediaItemIndex)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        Timber.d("PlayerService onPlayWhenReadyChanged playWhenReady $playWhenReady reason $reason")
    }

    override fun onTrimMemory(level: Int) {
        val isLowMemory = level == TRIM_MEMORY_RUNNING_CRITICAL
        Timber.d("PlayerService onTrimMemory level $level isLowMemory $isLowMemory")
        if (isLowMemory)
            serviceScope.launch { saveQueue() }
    }

    suspend fun recordListeningEvent(songId: String) {
        try {
            // Crea un Event per il profiling
            val event = Event(
                songId = songId,
                timestamp = System.currentTimeMillis(),
                playTime = 0L  // verrà aggiornato al termine
            )
            Database.eventDao().insert(event)

            // Online update del profilo (debounced)
            // profileRepository.applyEventAsync(event)
        } catch (e: Exception) {
            Timber.w("PlayerEvent Failed to record event: ${e.message}")
        }
    }

    @ExperimentalCoroutinesApi
    fun updateUnifiedNotification() {
//        Timber.d("PlayerService notify called from: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        serviceScope.launch {
            withContext(Dispatchers.Main){
                // Aggiorna sempre la sessione per riflettere lo stato reale, anche se vuoto
                updateUnifiedMediasession()

                if (player.mediaItemCount <= 0 && _playerState.value.playbackState == PlaybackState.IDLE) {
                    // Nasconde notifica se completamente idle e vuoto, attenzione il sistema potrebbe killare il servizio
                    // stopForeground(STOP_FOREGROUND_REMOVE)
                    return@withContext
                }

                startForeground()

//                val notifyInstance = notification()
//                notifyInstance.let {
//                    @Suppress("MissingPermission")
//                    NotificationManagerCompat
//                        .from(this@PlayerService)
//                        .notify(NOTIFICATION_ID, it)
//                }
            }
        }
    }

    private fun updateMediaSessionQueue(timeline: Timeline, activeIndex: Int) {
        val queueItems = mutableListOf<MediaSessionCompat.QueueItem>()
        val window = Timeline.Window()

        for (i in 0 until timeline.windowCount) {
            timeline.getWindow(i, window)
            val mediaItem = window.mediaItem
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(mediaItem.mediaId)
                .setTitle(mediaItem.mediaMetadata.title)
                .setSubtitle(mediaItem.mediaMetadata.artist)
                .build()

            queueItems.add(MediaSessionCompat.QueueItem(description, i.toLong()))
        }

        unifiedMediaSession.setQueue(queueItems)

        unifiedMediaSession.setQueueTitle(resources.getString(R.string.now_playing_title))
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
                        if(it.isLocal) return@let

                        _internalOnlinePlayer.value?.pause()
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
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
        if (!appSettings.autoLoadSongsInQueue
            || appSettings.queueLoopType == QueueLoopType.RepeatAll
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

    /**
     * Collega il LoudnessEnhancer alla sessione audio specifica di ExoPlayer.
     */
    @OptIn(UnstableApi::class)
    private fun setupLoudnessEnhancerForExo() {
        if (loudnessEnhancer != null) return

        try {
            val audioSessionId = hybridPlayer.audioSessionId

            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Timber.d("PlayerService LoudnessEnhancer attached to ExoPlayer session: $audioSessionId")
            }
        } catch (e: Exception) {
            Timber.e("PlayerService Errore inizializzazione LoudnessEnhancer: ${e.message}")
        }
    }
    @ExperimentalCoroutinesApi
    @UnstableApi
    private fun initializeNormalizeVolume() {
        if (!appSettings.volumeNormalizationEnabled) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            hybridPlayer.volume = hybridPlayer.volume // Reset al volume utente
            return
        }

        // Collego l'enhancer alla sessione audio di ExoPlayer
        setupLoudnessEnhancerForExo()


        val baseGain = appSettings.loudnessBaseGain
        val boostLevel = appSettings.volumeBoostLevel

        //if (currentSong.value?.isLocal == true && currentSong.value?.mediaId?.isEmpty() == true) return

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
                        // Calcolo il guadagno target (in millibel)
                        val targetGainMb = (baseGain.toMb() + boostLevel.toMb()) - loudnessMb

                        // Applico il guadagno a ExoPlayer
                        loudnessEnhancer?.setTargetGain(targetGainMb)
                        loudnessEnhancer?.enabled = true

                        // Applico l'attenuazione all'HybridPlayer per i brani online
                        hybridPlayer.setYtLoudnessDb(loudnessDb ?: 0f)

                    } catch (e: Exception) {
                        Timber.e("PlayerService apply targetGain ${e.stackTraceToString()}")
                    }
            }
        }
    }

    private fun initializeAudioDRCHelper() {
       val disable = appSettings.disableAudioDrc

        AudioDRCHelper.init(this)
        if (disable) AudioDRCHelper.disableDRC()
         else AudioDRCHelper.restoreDRC()
    }

    private fun initializeSongCoverInLockScreen() {
        val bitmap =
            if (isAtLeastAndroid13 || isShowingThumbnailInLockscreen) bitmapProvider?.bitmap else null

        val uri = player.mediaMetadata.artworkUri?.toString()?.toThumbnail(512)
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

    private fun initializeAudioManager() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private fun initializeAudioDeviceCallback() {
        if (!isAtLeastAndroid6) return

        val resumeOnBt = appSettings.resumeOrPausePlaybackWhenDeviceBt
        val resumeOnWired = appSettings.resumeOrPausePlaybackWhenDeviceWired

        if (!resumeOnBt && !resumeOnWired) {
            unregisterAudioDeviceCallback()
            return
        }

        if (audioDeviceCallback != null) return

        audioDeviceCallback = object : AudioDeviceCallback() {

            private fun isBluetoothSink(device: AudioDeviceInfo): Boolean {
                return device.isSink && device.type in bluetoothDeviceTypes
            }

            private fun isWiredSink(device: AudioDeviceInfo): Boolean {
                return device.isSink && device.type in wiredDeviceTypes
            }

            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                val hasNewBt = addedDevices.any(::isBluetoothSink)
                val hasNewWired = addedDevices.any(::isWiredSink)

                val shouldPlay = (hasNewBt && resumeOnBt) || (hasNewWired && resumeOnWired)

                if (shouldPlay) {
                    val local = currentSong.value?.isLocal == true
                    if (local) {
                        player.play()
                    } else {
                        serviceScope.launch {
                            val onlinePlayer = ensureOnlinePlayerInitialized()
                            onlinePlayer.play()
                        }
                    }
                    SmartMessage(getString(R.string.music_resumed_headphones_connected), context = this@PlayerService)
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                val removedBt = removedDevices.any(::isBluetoothSink)
                val removedWired = removedDevices.any(::isWiredSink)

                if (removedBt || removedWired) {
                    val currentDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    val hasRemainingBt = currentDevices?.any(::isBluetoothSink) == true
                    val hasRemainingWired = currentDevices?.any(::isWiredSink) == true

                    if (!hasRemainingBt && !hasRemainingWired) {
                        player.pause()
                        _internalOnlinePlayer.value?.pause()
                        SmartMessage(getString(R.string.music_paused_headphones_disconnected), context = this@PlayerService)
                    }
                }
            }
        }

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
    }


    fun unregisterAudioDeviceCallback() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        audioDeviceCallback = null
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun ensureOnlinePlayerInitialized(): YouTubePlayer {
        // Se il player esiste già, lo prendo
        _internalOnlinePlayer.value?.let { return it }

        // Altrimenti si inizializza.
        initializeOnlinePlayer()
        // Attendo che sia stato inizializzato prima di andare avanti
        return _internalOnlinePlayer.first { it != null }!!
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

    @ExperimentalCoroutinesApi
    private fun updateUnifiedMediasession() {

        val currentMediaItem = player.currentMediaItem
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
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM

        val notificationPlayerFirstIcon = appSettings.notificationPlayerFirstIcon
        val notificationPlayerSecondIcon = appSettings.notificationPlayerSecondIcon

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
                        player.currentMediaItemIndex.toLong()
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


    // ===================================================================
    // WRAPPER PER YOUTUBE CONTROL
    // Traduce i comandi di Media3 (millisecondi, 0f-1f)
    // nel linguaggio della WebView di YouTube (secondi, 0f-100f)
    // ===================================================================
    private inner class YouTubeControlImpl : YouTubeControl {

        override fun play() {
            _internalOnlinePlayer.value?.play()
        }

        override fun pause() {
            _internalOnlinePlayer.value?.pause()
        }

        override fun seekTo(positionMs: Long) {
            // ATTENZIONE: L'API di YouTube IFrame usa i SECONDI (Float), non i millisecondi!
            val seconds = positionMs.toFloat() / 1000f
            _internalOnlinePlayer.value?.seekTo(seconds)
        }

        override fun getCurrentPositionMs(): Long {
            // Legge da StateFlow e converte secondi -> millisecondi
            return (_currentSecond.value * 1000).toLong()
        }

        override fun getDurationMs(): Long {
            // Legge da StateFlow e converte secondi -> millisecondi
            return (_currentDuration.value * 1000).toLong()
        }

        override fun isPlaying(): Boolean {
            // Legge direttamente dalla variabile di stato Compose
            return isPlayingNow || player.isPlaying
        }

        override fun getVolume(): Float = 1f

        override fun setVolume(volume: Float) {
            // ATTENZIONE CRITICA: L'API di YouTube IFrame vuole il volume da 0 a 100!
            // Media3 manda un float da 0.0 a 1.0, quindi moltiplichiamo per 100.
            _internalOnlinePlayer.value?.setVolume((volume * 100F).toInt())
        }

        override fun setPlaybackRate(rate: Float) {
            // Traduce il float di Media3 nell'Enum specifico della libreria YouTube
            val ytRate = when {
                rate <= 0.25f -> PlayerConstants.PlaybackRate.RATE_0_25
                rate <= 0.5f -> PlayerConstants.PlaybackRate.RATE_0_5
                rate <= 0.75f -> PlayerConstants.PlaybackRate.RATE_0_75
                rate <= 1.0f -> PlayerConstants.PlaybackRate.RATE_1
                rate <= 1.25f -> PlayerConstants.PlaybackRate.RATE_1_25
                rate <= 1.5f -> PlayerConstants.PlaybackRate.RATE_1_5
                rate <= 1.75f -> PlayerConstants.PlaybackRate.RATE_1_75
                else -> PlayerConstants.PlaybackRate.RATE_2
            }
            _internalOnlinePlayer.value?.setPlaybackRate(ytRate)
        }
    }

    inner class LegacyActionReceiver() : BroadcastReceiver() {

        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("MainActivity onReceive intent.action: ${intent.action}")
            val currentMediaItem = binder.player?.currentMediaItem

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
                            it.player?.play()
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
                            it.player?.seamlessQueue(currentMediaItem)

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


    @ExperimentalCoroutinesApi
    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Timber.d("Playerservice onIsPlayingChanged $isPlaying called")

        val currentState = _playerState.value
        if (isPlaying) {
            startEndedObserver()
            startCrossfadeMonitor()
            _playerState.value = currentState.copy(playbackState = PlaybackState.PLAYING)
        }
        else {
            stopEndedObserver()
            stopCrossFadeMonitor()
            _playerState.value = currentState.copy(playbackState = PlaybackState.PAUSED)
        }

        isPlayingNow = isPlaying

        updateWidgetState()
        updateUnifiedNotification()

        //notify external equalizer
        if (!isPlaying) sendCloseExternalEqualizerIntent()
        else sendOpenExternalEqualizerIntent()

        updateDiscordPresence()

        super.onIsPlayingChanged(isPlaying)
    }

    @ExperimentalCoroutinesApi
    private fun initializeBassBoost() {
        if (!appSettings.bassBoostEnabled) {
            runCatching {
                bassBoost?.enabled = false
                bassBoost?.release()
            }
            bassBoost = null
            initializeNormalizeVolume()
            return
        }

        runCatching {
            // Collego l'audiosession di exoplayer
            val audioSessionId = hybridPlayer.audioSessionId
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return@runCatching

            if (bassBoost == null) bassBoost = BassBoost(0, audioSessionId)
            val bassboostLevel =
                ((appSettings.bassBoostLevel) * 1000f).toInt().toShort()
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
        val presetType = appSettings.audioReverbPreset
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
            // Collego l'audiosession di exoplayer
            val audioSessionId = hybridPlayer.audioSessionId
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return@runCatching

            if (reverbPreset == null) reverbPreset = PresetReverb(1,
                audioSessionId
            )

            reverbPreset?.enabled = false
            reverbPreset?.preset = presetType.preset
            reverbPreset?.enabled = true
            reverbPreset?.id?.let { player.setAuxEffectInfo(AuxEffectInfo(it, 1f)) }
        }
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
        Timber.d("PlayerService ExoPlayer Audio Session ID changed to: $audioSessionId")

        // Quando la sessione cambia, vanno ricreati gli effetti e collegati alla nuova sessione
        runCatching {
            bassBoost?.release()
            bassBoost = null
            reverbPreset?.release()
            reverbPreset = null
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        }

        // Riapplico gli effetti se sono abilitati nelle impostazioni
        initializeBassBoost()
        initializeReverb()
        initializeNormalizeVolume()
    }

    private fun startCrossfadeMonitor() {
        if (appSettings.crossfadeDuration == CrossfadeDuration.Off) return

        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(200.milliseconds)
                val duration = hybridPlayer.duration
                val position = hybridPlayer.currentPosition

                if (duration > 0) {
                    val timeLeft = duration - position

                    if (timeLeft in 0..appSettings.crossfadeDuration.milliseconds && !isFading) {
                        isFading = true

                        // SCOPRIAMO CHI È IL PROSSIMO BRANO
                        val nextMediaItem = hybridPlayer.getMediaItemAt(hybridPlayer.nextMediaItemIndex)

                        val isNextExo = nextMediaItem.isLocal

                        if (isNextExo) {
                            // Exo -> Exo (Crossfade Gapless)
                            startExoToExoCrossfade()
                        } else {
                            // Exo -> WebView (Dissolvenza)
                            startWebViewFadeOut()
                        }
                    }
                }
            }
        }
    }

    private fun stopCrossFadeMonitor() {
        crossfadeJob?.cancel()
        crossfadeJob = null
    }

    private fun startFadeIn(targetVolume: Float) {
        if (appSettings.crossfadeDuration == CrossfadeDuration.Off) return

        fadeInJob?.cancel() // Se c'era un vecchio fade in corso, cancellalo

        isFading = true
        hybridPlayer.setVolume(0f) // Inizia il nuovo brano dal silenzio

        fadeInJob = serviceScope.launch(Dispatchers.Main) {
            val steps = 20 // Alza il volume per step di 20 punti
            val stepDelay = FADE_IN_DURATION_MS / steps

            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                hybridPlayer.setVolume(targetVolume * progress)
                delay(stepDelay.milliseconds)
            }

            // Infine imposto il volume target su HybridPlayer
            hybridPlayer.setVolume(targetVolume)
            isFading = false // Fine del fade, la UI può tornare a comandare
        }
    }

    private fun startExoToExoCrossfade() {
        // Usiamo un job temporaneo solo per il fade out del brano Exo
        serviceScope.launch(Dispatchers.Main) {
            val steps = 15
            val stepDelay = appSettings.crossfadeDuration.milliseconds / steps
            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setVolume(appSettings.userVolume * progress)
                delay(stepDelay.milliseconds)
            }
            // ExoPlayer passerà al brano successivo in modo gapless.
            // L'onMediaItemTransition se ne accorgerà e farà partire il Fade In!
        }
    }

    private fun startWebViewFadeOut() {
        serviceScope.launch(Dispatchers.Main) {
            val steps = 15
            val stepDelay = appSettings.crossfadeDuration.milliseconds / steps
            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setVolume(appSettings.userVolume * progress)
                delay(stepDelay.milliseconds)
            }
            // Quando il volume è a 0, la canzone finisce e parte onMediaItemTransition.
        }
    }

    @ExperimentalCoroutinesApi
    fun notification(): Notification {

        val currentMediaItem = binder.player?.currentMediaItem

        createNotificationChannels()

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


        val notificationPlayerFirstIcon = appSettings.notificationPlayerFirstIcon
        val notificationPlayerSecondIcon = appSettings.notificationPlayerSecondIcon

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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return notification

    }

    private fun createNotificationChannels() {
        if (!isAtLeastAndroid8) return

        notificationManager = getSystemService(NotificationManager::class.java)

        notificationManager?.run {

            try {
                // Migrazione canale player: elimina se importance errata
                getNotificationChannel(NOTIFICATION_CHANNEL_ID)?.let { channel ->
                    if (channel.importance == NotificationManager.IMPORTANCE_HIGH) {
                        deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
                    }
                }

                // Migrazione canale sleeptimer: elimina se importance errata
                getNotificationChannel(SLEEPTIMER_NOTIFICATION_CHANNEL_ID)?.let { channel ->
                    if (channel.importance == NotificationManager.IMPORTANCE_HIGH) {
                        deleteNotificationChannel(SLEEPTIMER_NOTIFICATION_CHANNEL_ID)
                    }
                }
            } catch (e: Exception) {
                Timber.d("PlayerService createNotificationChannels migrating channels isn't possible, consider remove and install again")
            }

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
                        NotificationManager.IMPORTANCE_DEFAULT
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

    fun createCacheDataSource(): CacheDataSource.Factory {

        val webDavConfig = WebDavConfig(
            baseUrl = appSettings.webDavUrl,
            username = appSettings.webDavUsername,
            password = CryptoManager.decrypt(appSettings.webDavPassword)
        )

        // 1. Configura il client OkHttp con le credenziali WebDAV (se presenti)
        val okHttpClient = OkHttpClient.Builder()
            .proxy(Environment.proxy)
            .apply {
                addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(
                            "Authorization",
                            okhttp3.Credentials.basic(webDavConfig.username, webDavConfig.password)
                        )
                        .build()
                    chain.proceed(request)
                }
            }
            .build()

        // 2. Crea la factory HTTP che usa il nostro OkHttp
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("RiPlayUserAgent") // Opzionale ma consigliato

        // 3. Unisci locale e remoto!
        // DefaultDataSource userà okHttpDataSourceFactory per il traffico web,
        // e le API native di Android per i file locali.
        val upstreamDataSourceFactory = DefaultDataSource.Factory(
            this,
            okHttpDataSourceFactory
        )

        // 4. Assembla la Cache
        return CacheDataSource
            .Factory()
            .setCache(cache)
            // ATTENZIONE: Rimuovi o modifica questa riga (leggi sotto)
            // .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
    }

    /*
    fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory(null) // Disabilita scrittura in cache — evita corruzione file locali e MusicVault
            // Remove upstream cause issue with local files
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(this) // okHttp is not needed for local files
            )
            /*
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
             */

     */
    private fun createRendersFactory() = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            val minimumSilenceDuration = (appSettings.minimumSilenceDuration)
                .coerceIn(1000L..2_000_000L)

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

    fun updateWidgetState() {
        Timber.d("PlayerService updateWidgetState _playerState ${_playerState.value.isPlaying}")
        serviceScope.launch {
            if (!::player.isInitialized) {
                Timber.w("PlayerService updateWidgetState invocato ma il player non è ancora pronto. Salto l'aggiornamento.")
                return@launch
            }

            val isPlaying = _playerState.value.isPlaying
            val title = withContext(Dispatchers.Main) { cleanPrefix(player.mediaMetadata.title.toString()) }
            val artist = withContext(Dispatchers.Main) { player.mediaMetadata.artist.toString() }

            val artworkBase64 = getOptimizedArtworkBase64(bitmapProvider?.bitmap)

            playerHorizontalWidget.updateState(
                context = this@PlayerService,
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                artworkBase64 = artworkBase64
            )
            playerVerticalWidget.updateState(
                context = this@PlayerService,
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                artworkBase64 = artworkBase64
            )
        }
    }

    private suspend fun getOptimizedArtworkBase64(bitmap: Bitmap?): String? {
        return withContext(Dispatchers.IO) {
            if (bitmap == null || bitmap.isRecycled) return@withContext null

            if (bitmap.width < 20 || bitmap.height < 20) {
                return@withContext null
            }

            var safeBitmap: Bitmap? = null
            try {
                safeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)

                val maxSizePx = 200
                val ratio = safeBitmap.width.toFloat() / safeBitmap.height.toFloat()
                val width = if (safeBitmap.width >= safeBitmap.height) maxSizePx else (maxSizePx * ratio).toInt()
                val height = if (safeBitmap.height >= safeBitmap.width) maxSizePx else (maxSizePx / ratio).toInt()

                val resizedBitmap = Bitmap.createScaledBitmap(safeBitmap, width, height, true)

                if (resizedBitmap != safeBitmap) safeBitmap.recycle()
                safeBitmap = resizedBitmap

                val byteArrayOutputStream = ByteArrayOutputStream()
                safeBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                if (byteArray.size > 400_000) {
                    safeBitmap.recycle()
                    return@withContext null
                }

                val result = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                safeBitmap.recycle()

                result

            } catch (e: Exception) {
                Timber.e(e, "Errore conversione Base64 Widget (Bitmap probabilmente riciclata da Media3)")
                null
            } finally {
                safeBitmap?.recycle()
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun incrementOnlineListenedPlaytimeMs() {
        if (currentSong.value?.isLocal == true
                || appSettings.isPauseListenHistoryEnabled
        ) return

        currentSong.value?.id?.let { mediaId ->
            if (_currentSecond.value > 5) {
                Timber.d("PlayerService incrementOnlineListenedPlaytimeMs INCREMENT totalPlayTimeMs $onlineListenedDurationMs mediaItem ${currentSong.value?.id}")
                Database.asyncTransaction {
                    Database.incrementTotalPlayTimeMs(mediaId, onlineListenedDurationMs)
                }
            }

            val minTimeForEvent = appSettings.minTimeForEvent

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

                    val queueLoopType = appSettings.queueLoopType

                    when (queueLoopType) {
                        QueueLoopType.RepeatOne -> {
                            _internalOnlinePlayer.value?.seekTo(0f)
                        }
                        QueueLoopType.Default -> {
                            if (binder.player?.hasNextMediaItem() == true) {
                                lastProcessedIndex = binder.player?.currentMediaItemIndex
                                handlePlayNext()
                            }
                        }
                        QueueLoopType.RepeatAll -> {
                            if (binder.player?.hasNextMediaItem() == false) {
                                binder.player?.playAtIndex(0)
                            } else {
                                lastProcessedIndex = player.currentMediaItemIndex
                                handlePlayNext()
                            }
                        }
                    }
                }

                delay(200.milliseconds)
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
        if (!isAtLeastAndroid7) return

        val enabled = appSettings.enableWallpaper
        if (!enabled) return
        val wallpaperTarget = appSettings.wallpaperType

        serviceScope.launch {
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
        val savedEndTime = appSettings.timerEndTime

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

    suspend fun saveQueue() {
        if (!isPersistentQueueEnabled()) return

        // SINCRONIZZAZIONE OBBLIGATORIA: withContext è sincrono rispetto alla coroutine.
        val mediaItems: List<MediaItem>
        val mediaItemIndex: Int
        val mediaItemPosition: Long

        // Sincronizzato al thread principale
        withContext(Dispatchers.Main) {
            mediaItems = player.currentTimeline.mediaItems
            mediaItemIndex = player.currentMediaItemIndex
            mediaItemPosition = if (player.currentMediaItem?.isLocal == true) {
                player.currentPosition
            } else {
                (currentSecond.value * 1000).toLong()
            }
        }

        if (mediaItems.isEmpty()) return

        // Lavoro pesante sul thread IO
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

                Database.asyncTransaction {
                    try {
                        clearQueuedMediaItems()
                        queuedMediaItems.forEach { insert(it) }
                    } catch (e: Exception) {
                        Timber.e("SaveQueue QueuePersistentEnabled Error: ${e.message}")
                    }
                }
            }
        }
    }

    /*
    suspend fun saveQueue() {
        if (!isPersistentQueueEnabled()) return

        serviceScope.launch(Dispatchers.Main) {
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


                        Database.asyncTransaction {
                            try {
                                clearQueuedMediaItems()
                                queuedMediaItems.forEach {
                                    insert(it)
                                }
                            } catch (e: Exception) {
                                Timber.e("SaveQueue QueuePersistentEnabled Error: ${e.message}")
                            }
                        }


                }
            }
        }
    }

     */

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
                        val song = mediaItem.mediaItem
                        val isMusicVault = song.mediaMetadata.extras
                            ?.getString("musicVaultState") == MusicVaultState.COMPLETED.name
                        val musicVaultFileName = song.mediaMetadata.extras
                            ?.getString("musicVaultFileName")

                        val uri = when {
                            isMusicVault && musicVaultFileName != null -> {
                                if (musicVaultFileName.startsWith("content://")) musicVaultFileName.toUri()
                                else File(MusicVaultRepository.getOutputDir(), musicVaultFileName).toUri()
                            }
                            else -> song.mediaId.toUri()
                        }

                        song.buildUpon()
                            .setUri(uri)
                            .setCustomCacheKey(song.mediaId)
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
                        appSettings.stateDuration
                    } catch (e: Exception) {
                        0f
                    }
                    val mId = appSettings.stateMediaId
                    playFromSecond = position.toFloat()
                    _currentSecond.value = playFromSecond
                    _currentDuration.value = if (queuedSong.mediaId == mId) duration else 0f
                    _internalOnlinePlayer.value?.pause()
                }

            }
        }
    }

    private fun updateMusicVaultMediaItem(
        songId: String,
        fileName: String,
        thumbnailFileName: String
    ) {
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                val itemCount = player.mediaItemCount
                for (i in 0 until itemCount) {
                    val mediaItem = player.getMediaItemAt(i)
                    val itemId = mediaItem.mediaId
                    val currentMediaItem = player.currentMediaItem

                    // Aggiorno il mediaitem in coda ma solo se non è in riproduzione
                    if (itemId == songId && currentMediaItem?.mediaId != songId ) {
                        if (fileName.isNotEmpty()) {
                            // Costruzione URI corretto
                            val uri = if (fileName.startsWith("content://")) {
                                fileName.toUri()
                            } else {
                                File(MusicVaultRepository.getOutputDir(), fileName).toUri()
                            }

                            // Aggiornamento extras
                            val updatedExtras = mediaItem.mediaMetadata.extras?.apply {
                                putString("musicVaultState", MusicVaultState.COMPLETED.name)
                                putString("musicVaultFileName", fileName)
                                putString("musicVaultThumbnailFileName", thumbnailFileName)
                            }

                            // Sostituzione con URI corretto
                            val updatedMediaItem = mediaItem.buildUpon()
                                .setUri(uri)
                                .setCustomCacheKey(songId)
                                .setMediaMetadata(
                                    mediaItem.mediaMetadata.buildUpon()
                                        .setExtras(updatedExtras)
                                        .build()
                                )
                                .build()

                            Timber.d("PlayerService replaceMediaItem index=$i songId=$songId uri=$uri")
                            player.replaceMediaItem(i, updatedMediaItem)
                            Timber.d("PlayerService replaceMediaItem done — new uri=${player.getMediaItemAt(i).localConfiguration?.uri}")

                        } else {
                            // Resetta extras e ripristina URI originale
                            val updatedExtras = mediaItem.mediaMetadata.extras?.apply {
                                remove("musicVaultState")
                                remove("musicVaultFileName")
                                remove("musicVaultThumbnailFileName")
                            }

                            val updatedMediaItem = mediaItem.buildUpon()
                                .setUri(songId.toUri())
                                .setCustomCacheKey(songId)
                                .setMediaMetadata(
                                    mediaItem.mediaMetadata.buildUpon()
                                        .setExtras(updatedExtras)
                                        .build()
                                )
                                .build()

                            player.replaceMediaItem(i, updatedMediaItem)
                            Timber.d("PlayerService updateMusicVaultMediaItem reset MediaItem at index=$i songId=$songId")
                        }
                        break
                    }
                }
            }
        }
    }

    @Stable
    open inner class Binder : AndroidBinder() {

        val coroutineScope: CoroutineScope
            get() = this@PlayerService.serviceScope

        val player: ExoPlayer
            get() = this@PlayerService.player

        val playerState: StateFlow<PlayerState>
            get() = this@PlayerService.playerState

        val onlinePlayer: YouTubePlayer?
            get() = this@PlayerService._internalOnlinePlayer.value // todo controlla se è ok

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


        val currentMediaItemAsSong: Song?
            get() = this@PlayerService.player.currentMediaItem?.asSong

        val riTuneCastClient: RiTuneCastClient
            @Synchronized
            get() = this@PlayerService.riTuneCastClient

        val equalizer: EqualizerHelper
            get() = this@PlayerService.equalizerHelper

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        val currentDiscoveryReason: StateFlow<DiscoveryInfo?>
            get() = this@PlayerService.currentDiscoveryReason

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        val bitmap: Bitmap?
            get() = this@PlayerService.bitmapProvider?.bitmap

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()

            val endTime = System.currentTimeMillis() + delayMillis
            /*
            val new = appSettings.copy(timerEndTime = endTime)
            serviceScope.launch {
                AppSettingsManager().updateSettings(new)
            }

             */

            Timber.d("PlayerService startSleepTimer delayMillis $delayMillis, scheduled for $endTime")

            timerJob = serviceScope.timer(delayMillis) {
                Timber.d("PlayerService timer finished naturally")
                executeStopServiceLogic()
            }
        }

        fun executeStopServiceLogic() {
            /*
            val new = appSettings.copy(timerEndTime = 0)
            serviceScope.launch {
                AppSettingsManager().updateSettings(new)
            }

             */

            serviceScope.launch { saveQueue() }

            val notification = NotificationCompat
                .Builder(this@PlayerService, SLEEPTIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Self closing timer ended")
                .setSmallIcon(R.drawable.app_icon)
                .build()
            notificationManager?.notify(SLEEPTIMER_NOTIFICATION_ID, notification)

            if(isAtLeastAndroid7)
                stopForeground(STOP_FOREGROUND_REMOVE)

            stopSelf()

            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.appTasks?.forEach { it.finishAndRemoveTask() }

            handler.postDelayed({
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
            val isDiscoverEnabled = appSettings.discoverIsEnabled
            val filterContentType = appSettings.filterContentType

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
                        player?.addMediaItems( songs.drop(1))
                    } else {
                        player?.forcePlayFromBeginning(songs)
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

        @ExperimentalCoroutinesApi
        @FlowPreview
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
                    currentSong.debounce(1000).conflate().collect(serviceScope) { updateUnifiedNotification() }
                }
            }

        }

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        fun toggleShuffle() {
            player?.shuffleModeEnabled?.let { player?.shuffleModeEnabled = !it }

        }

        fun toggleRepeat() {
            val queueLoopType = appSettings.queueLoopType
            /*
            val new = appSettings.copy(queueLoopType = setQueueLoopState(queueLoopType))
            serviceScope.launch {
                AppSettingsManager().updateSettings(new)
            }

             */
        }

//        fun callPause(onPause: () -> Unit) {
//            val fadeDisabled = appSettings.playbackFadeAudioDuration == DurationInMilliseconds.Disabled
//            val duration = appSettings.playbackFadeAudioDuration.milliSeconds
//            if (player.isPlaying) {
//                if (fadeDisabled) {
//                    player.pause()
//                    onPause()
//                } else {
//                    //fadeOut
//                    startFadeAnimator(player, duration, false) {
//                        player.pause()
//                        onPause()
//                    }
//                }
//            }
//        }

        fun actionSearch() {
            startActivity(Intent(applicationContext, MainActivity::class.java)
                .setAction(MainActivity.action_search)
                .setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK))
        }

        fun loadQueue() = this@PlayerService.loadQueue()

        /**
         * Chiamato quando l'utente avvia un brano dai suggerimenti.
         */
        fun setDiscoverySource(strategyId: String, strategyName: String, reasons: List<String>, itemId: String) {
            this@PlayerService._currentDiscoveryReason.value = DiscoveryInfo(
                strategyId = strategyId,
                strategyDisplayName = strategyName,
                reasons = reasons,
                itemId = itemId
            )
        }

        /**
         * Chiamato quando l'utente avvia un brano da altra fonte (ricerca, library, etc.).
         */
        fun clearDiscoverySource() {
            _currentDiscoveryReason.value = null
        }

        fun notifyAutoChildrenChanged(parentId: String) = this@PlayerService.notifyAutoChildrenChanged(parentId)

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


    @kotlin.OptIn(FlowPreview::class)
    @ExperimentalCoroutinesApi
    fun initializeUnifiedSessionCallback() {
        Timber.d("PlayerService InitializeUnifiedSessionCallback")
        val currentMediaItem = binder.player?.currentMediaItem

        binder.let {
            unifiedMediaSession.setCallback(
                LegacyMediaSessionCallback(
                    binder = it,
                    onPlayClick = {
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onPlayClick")

                        // FIX: Se currentMediaItem è nullo, il service sta caricando.
                        // Non fare nulla o prova a forzare il caricamento, ma non buttarti sull'online player a caso.
                        if (player.currentMediaItem == null) {
                            Timber.w("PlayerService PlayClick ignored: No media item loaded yet")
                            // Opzionale: puoi tentare di ripristinare la coda qui se necessario
                            return@LegacyMediaSessionCallback
                        }

                        if (player.currentMediaItem?.isLocal == true)
                            it.player?.play()
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
                        updateUnifiedNotification()
                    },
                    onPauseClick = {
                        Timber.d("PlayerService InitializeUnifiedSessionCallback onPauseClick")

                        // FIX: Se currentMediaItem è nullo, il service sta caricando.
                        // Non fare nulla o prova a forzare il caricamento, ma non buttarti sull'online player a caso.
                        if (player.currentMediaItem == null) {
                            Timber.w("PlayerService PlayClick ignored: No media item loaded yet")
                            // Opzionale: puoi tentare di ripristinare la coda qui se necessario
                            return@LegacyMediaSessionCallback
                        }

                        it.player?.pause()
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
                        updateUnifiedNotification()
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

                        updateUnifiedNotification()
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
                                    it.player?.seamlessQueue(currentMediaItem)

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
        _internalOnlinePlayer.value?.pause()
        val now = System.currentTimeMillis()
        if (now - lastPlayNextTime < debounceDelayMs) {
            Timber.d("PlayerService handlePlayNext ignored (too fast) play current")
            if (localMediaItem?.isLocal == true)
                player.play()
            else
                _internalOnlinePlayer.value?.play()

            return
        }
        lastPlayNextTime = now
        Timber.d("PlayerService handlePlayNext executed")

        playFromSecond = 0f

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
        // Controllo totale sulla disponibilità del servizio
        private val _isServiceReady = MutableStateFlow(false)
        val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()

        const val NOTIFICATION_ID = 1001
        val NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.player_notification_channel_id)

        const val SLEEPTIMER_NOTIFICATION_ID = 1002
        val SLEEPTIMER_NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.sleep_timer_notification_channel_id)

    }

}

