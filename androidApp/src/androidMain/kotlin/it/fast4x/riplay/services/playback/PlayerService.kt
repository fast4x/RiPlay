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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
import it.fast4x.riplay.extensions.experimental.webdavlibrary.DynamicWebDavAuthInterceptor
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.services.playback.common.PlaybackContext
import it.fast4x.riplay.services.playback.common.PlaybackState
import it.fast4x.riplay.services.playback.common.PlayerState
import it.fast4x.riplay.services.playback.common.restorePlayerVolume
import it.fast4x.riplay.utils.BitmapLoader
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getDeviceVolume
import it.fast4x.riplay.utils.isWebDav
import it.fast4x.riplay.utils.removeVideoMediaItems
import it.fast4x.riplay.utils.setQueueLoopState
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    OnAudioVolumeChangedListener
{
    val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var legacyMediaSession: MediaSessionCompat
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var mediaLibrarySessionCallback: MediaLibraryServiceCallback
    lateinit var hybridPlayer: HybridPlayer

    val cache: SimpleCache by lazy {
        PrincipalCache.getInstance(this)
    }
    lateinit var player: ExoPlayer
    private lateinit var audioVolumeObserver: AudioVolumeObserver

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
    private var closeServiceAfterMinutes by mutableStateOf(DurationInMinutes.Disabled)
    private var isShowingThumbnailInLockscreen = true
    private var medleyDuration by mutableFloatStateOf(0f)

    private lateinit var audioManager: AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

    var legacyActionReceiver: LegacyActionReceiver? = null

    private val playerVerticalWidget = PlayerVerticalWidget()
    private val playerHorizontalWidget = PlayerHorizontalWidget()

    private val currentMediaItemState = MutableStateFlow<MediaItem?>(null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val currentSong = currentMediaItemState
        .map { it?.mediaId }
        .distinctUntilChanged()
        .flatMapLatest { mediaId ->
            if (mediaId == null) {
                flowOf(null)
            } else {
                Database.song(mediaId)
                    .catch { e ->
                        Timber.e("PlayerService CurrentSong Errore nel recupero della canzone $e")
                        // Non emette nulla mantenendo l'ultimo flow
                    }
            }
        }
        .stateIn(serviceScope, SharingStarted.WhileSubscribed(5000), null)

    lateinit var sleepTimerListener: SleepTimerListener

    /**
     * Online configuration
     */

    private val _internalYouTubePlayerView = MutableStateFlow<YouTubePlayerView>(
        LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false)
                as YouTubePlayerView
    )
    val internalYoutubePlayerView: StateFlow<YouTubePlayerView?> = _internalYouTubePlayerView

    private val _internalYouTubePlayer = MutableStateFlow<YouTubePlayer?>(null)
    //val internalYoutubePlayer: StateFlow<YouTubePlayer?> = _internalOnlinePlayer

    private val _internalBufferedFraction = MutableStateFlow(0f)
    val internalYoutubeBufferedFraction: StateFlow<Float> = _internalBufferedFraction

    var _currentSecond = MutableStateFlow(0f)
    var youtubeCurrentSecond: StateFlow<Float> = _currentSecond

    var _currentDuration = MutableStateFlow(0f)
    var youtubeCurrentDuration: StateFlow<Float> = _currentDuration

    var load = true
    var playFromSecond by mutableFloatStateOf(0f)
    var lastError: PlayerConstants.PlayerError? = null

    private var onlineListenedDurationMs = 0L
    private var lastOnlineMediaId: String? = null
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

    // Observer per il ciclo di vita dell'intero processo (app in background)
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            Timber.d("PlayerService: ProcessLifecycleOwner.onStop() schermo spento, rimuovo video")
            // Chiamato quando l'app va in background o lo schermo si spegne
            // Elimino i video perchè in background non sono più visibili ma creano problemi di avanzamento al successivo mediaitem
            player.removeVideoMediaItems()
        }
    }


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

        // Carico le impostazioni prima di tutto
        loadInitialSettingsFromDatabase()

        // Lancio in sequenza le inizializzazioni necessarie anche per android auto sul thread principale
        initializeBitmapProvider()
        initializeHybridPlayerAndSession()

        initializeVariables()
        replaceOnlinePlayerView()
        initializeOnlinePlayer()
        initializeLegacyMediaSession()

        // Aggiorna subito il mediasession per allineare lo stato delle azioni
        if (!_playerState.value.isPlaying && _internalYouTubePlayer.value == null) {
            _playerState.update { it.copy(playbackState = PlaybackState.PAUSED) }
            updateLegacyMediasession()
        }


        // Lancio tutto il resto in uno scope diverso
        serviceScope.launch(Dispatchers.Main) {

            // Avvio l'osservazione delle impostazioni
            withContext(Dispatchers.IO) {
                startObservingSettings()
            }

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

            startForeground()

            // Registra l'observer sul ciclo di vita del processo
            ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

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
            currentSong.collect { song ->
                if (song == null) return@collect
                if (currentMediaItemState.value?.mediaId != song.id) return@collect

                Timber.d("PlayerService onCreate update currentSong $song")

                withContext(Dispatchers.Main) {
                    updateLegacyMediasession()
                    updateLegacyNotification()
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
                        currentState.withDatabaseMediaItemIfCurrent(
                            currentMediaId = song.mediaId,
                            databaseMediaItem = song.asMediaItem,
                            queueIndex = player.currentMediaItemIndex,
                            queueSize = player.mediaItemCount,
                        )
                    }
                }
            }
        }

        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (currentSong.value?.isLocal == false) {
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

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private fun handleForeground(isPlaying: Boolean) {
        if (isPlaying) {
            // Se riproduce qualcosa dopo aver impostato un timer, viene cancellato
            binder.cancelTimer()
            // Qualsiasi sia il player attivo (Exo o l'altro), se sta suonando rimettiamo il foreground
            startForeground(loading = false)
            //Timber.d("PlayerService handleForeground - Lancio foreground")
        } else {
            // Se va in pausa, stop o buffering, sganciamo il foreground in sicurezza
            detachForegroundSafely()
            // Se va in pausa, ci rimane per almeno 15 minuti e non è attiva l'opzione di chiusura automatica, chiudiamo il servizio per liberare memoria
            if (closeServiceAfterMinutes == DurationInMinutes.Disabled) {
                binder.startSleepTimer(DurationInMinutes.`15`.milliSeconds)
            }
            //Timber.d("PlayerService handleForeground - Sgancio foreground")
        }
        _internalYouTubePlayer.value?.setVolume(getSystemMediaVolume())
    }

    private fun detachForegroundSafely() {
        if (isServiceInForeground) {
            try {
                if (isAtLeastAndroid7) {
                    // STOP_FOREGROUND_DETACH mantiene la notifica visibile nel pannello media
                    // ma rimuove lo stato di servizio in primo piano aggressivo
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false) // Comportamento legacy per versioni vecchie
                }
                isServiceInForeground = false
                Timber.d("PlayerService detachForegroundSafely - Sganciato il foreground in modo sicuro (In Pausa)")
            } catch (e: Exception) {
                Timber.e("PlayerService detachForegroundSafely - Errore durante il stopForeground detach: $e")
            }
        }
    }

    fun loadInitialSettingsFromDatabase(){
        // Devo essere sicuro che le impostazioni siano pronte
        appSettings = runBlocking(Dispatchers.IO) {
            appSettingsManager.waitForInitialization()
        }
    }

    private fun startObservingSettings() {
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

            val params = LibraryParams.Builder()
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
            Action.play.value -> { if (currentSong.value?.isLocal == true) player.play() else _internalYouTubePlayer.value?.play() }
            Action.pause.value -> { if (currentSong.value?.isLocal == true) player.pause() else _internalYouTubePlayer.value?.pause() }
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

    }

    private fun replaceOnlinePlayerView() {
        _internalYouTubePlayer.value?.pause()
        _internalYouTubePlayer.value = null
        _internalYouTubePlayerView.value.release()
        _internalYouTubePlayerView.value = LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false) as YouTubePlayerView
    }

    private fun applyPlaybackParameters() {
        val speed = appSettings.playbackSpeed
        val pitch = appSettings.playbackPitch

        if (currentSong.value?.isLocal == false) {
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

            _internalYouTubePlayer.value?.setPlaybackRate(onlineRate)
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
                        if (currentSong.value?.isLocal == true) player.currentPosition.div(1000)
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
                            hybridPlayer.pause()
                        }
                        updatePlayerState(PlayerConstants.PlayerState.PAUSED)
                        Timber.d("PlayerService initializeRiTune CAST NOT ACTIVE - Disconnected")
                    }

                } else {

                    if (connectionStatus == RiTuneConnectionStatus.Connected) {
                        if (isConnecting) {
                            isConnecting = false
                            withContext(Dispatchers.Main) {
                                hybridPlayer.pause()
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
            if (token.isNotEmpty()) {
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
                }

            }

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun resumePlaybackOnStart() {
        if (!isPersistentQueueEnabled && !isResumePlaybackOnStart) return

        when (currentSong.value?.isLocal) {
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
        updateLegacyNotification()
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
    private fun initializeLegacyMediaSession() {

        legacyMediaSession = MediaSessionCompat(this, "PlayerService")

        val repeatMode = appSettings.queueLoopType.type

        legacyMediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        legacyMediaSession.setRepeatMode(repeatMode)

        if (appSettings.useVolumeKeysToChangeSong)
            legacyMediaSession.setPlaybackToRemote(getVolumeProvider())

        initializeLegacySessionCallback()

        legacyMediaSession.isActive = true
        legacyMediaSession.setMediaButtonReceiver(null)

    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    fun recreateOnlinePlayerView() {
        replaceOnlinePlayerView()
        initializeOnlinePlayer(skipAutoload = true)
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
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

        // Imposto il volume dell'Hybrid Player
        hybridPlayer.volume = getDeviceVolume(this)
        // Lo salvo nelle impostazioni
        serviceScope.launch {
            appSettingsManager.updateSettings(appSettings.copy(userVolume = hybridPlayer.volume))
        }
        Timber.d("PlayerService initializeHybridPlayerAndSession initial hybridPlayer volume = ${hybridPlayer.volume}")

        // Listener specifico per hybridPlayer e refreshare il layout di AA
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
            ).setBitmapLoader( customBitmapLoader )
            .build()

    }

    @ExperimentalCoroutinesApi
    private fun initializeOnlinePlayer(skipAutoload: Boolean = false) {

        val youTubePlayerView = _internalYouTubePlayerView.value

        val listener = object : AbstractYouTubePlayerListener() {

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)

                if (youTubePlayerView !== _internalYouTubePlayerView.value) {
                    youTubePlayer.pause()
                    return
                }

                _internalYouTubePlayer.value = youTubePlayer

                val customUiController =
                    CustomDefaultPlayerUiController(
                        this@PlayerService,
                        youTubePlayerView,
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
                youTubePlayerView.setCustomPlayerUi(customUiController.rootView)

                Timber.d("PlayerService onlinePlayer onReady localmediaItem ${currentSong.value?.id} queue index ${hybridPlayer.currentMediaItemIndex}")
                Timber.d("PlayerService onlinePlayer onReady isPersistentQueueEnabled $isPersistentQueueEnabled isResumePlaybackOnStart $isResumePlaybackOnStart")

                youTubePlayer.setVolume(getSystemMediaVolume())

                if (currentSong.value?.isLocal == true) return

                currentSong.value?.id?.let{
                    if (isPersistentQueueEnabled && isResumePlaybackOnStart && firstTimeStarted && !skipAutoload) {
                        youTubePlayer.loadVideo(it, playFromSecond)
                        playFromSecond = 0f
                        Timber.d("PlayerService onlinePlayer onReady loadVideo ${it}")
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

                updateLegacyNotification()
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
                if (currentSong.value?.isLocal == true) return
                Timber.d("PlayerService onlinePlayerView: onStateChange $state")

                unstartedWatchdogJob?.cancel()

                updatePlayerState(state)

                when(state) {
                    PlayerConstants.PlayerState.UNSTARTED -> {
                        if (!firstTimeStarted) {

                            val expectedMediaId = currentSong.value?.id

                            unstartedWatchdogJob = serviceScope.launch(Dispatchers.Main) {
                                Timber.d("PlayerService onlinePlayerView: onStateChange UNSTARTED watchdog scheduled for mediaId=$expectedMediaId")
                                delay(5000.milliseconds)
                                val stillUnstarted =
                                    _playerState.value.playbackState == PlaybackState.UNSTARTED

                                val sameMedia =
                                    currentSong.value?.id == expectedMediaId

                                if (stillUnstarted && sameMedia && expectedMediaId != null) {
                                    Timber.e("PlayerService onlinePlayerView: Persistent UNSTARTED state. Probably webView killed. Force to re-initialize for mediaId=$expectedMediaId")

                                    recreateOnlinePlayerView()
                                    val currentPlayer = this@PlayerService._internalYouTubePlayer.first { it != null }!!

                                    currentSong.value?.let { item ->
                                        if(item.isLocal) return@let
                                        Timber.d("PlayerService onlinePlayerView: Try reload song/video")
                                        // Assicura che ExoPlayer sia fermo prima del recovery
                                        if (player.isPlaying) {
                                            player.pause()
                                            player.stop()
                                        }
                                        currentPlayer.pause()
                                        _internalYouTubePlayer.value?.pause() // Pause also primary instance
                                        currentPlayer.cueVideo(expectedMediaId, playFromSecond)
                                    }

                                }
                            }
                        }
                    }

                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                        Timber.d("PlayerService onlinePlayerView: onStateChange VIDEO_CUED regular play()")
                        playFromSecond = 0f
                        _internalYouTubePlayer.value?.pause()
                        youTubePlayer.pause()
                        if (!firstTimeStarted) {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                                youTubePlayer.unMute()
                                youTubePlayer.setVolume(getSystemMediaVolume())
                                youTubePlayer.play()
                            }

                        }

                    }
                    PlayerConstants.PlayerState.PLAYING -> {
                        handleForeground(true)
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
                        handleForeground(false)
                        onlineNearEndTicks = 0
                        stopEndedObserver()
                        stopCrossFadeMonitor()
                        sendCloseExternalEqualizerIntent()

                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }

                    else -> {}
                }


                updateLegacyNotification()
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

                if (currentSong.value == null || currentSong.value?.isLocal == true) return

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

                    if (error == PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST)
                        currentSong.value?.id?.let {
                            if(it.isLocal) return@let
                            // Assicura che ExoPlayer sia fermo
                            if (player.isPlaying) {
                                player.pause()
                                player.stop()
                            }

                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                                _internalYouTubePlayer.value?.pause()
                                youTubePlayer.pause()
                                youTubePlayer.cueVideo(it, playFromSecond)
                            }
                            else serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "load",
                                        mediaId = it,
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
                val prev = hybridPlayer.currentMediaItem ?: return

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
                                _internalYouTubePlayer.value?.pause()
                            }
                            val currentState = _playerState.value
                            _playerState.value = currentState.copy(
                                playbackState = PlaybackState.PAUSED
                            )
                            return@let
                        }
                        _internalYouTubePlayer.value = it.internalCastOnlinePlayer.value
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
        youTubePlayerView.apply {
            enableAutomaticInitialization = false

            enableBackgroundPlayback(true)

            keepScreenOn = appSettings.keepScreenEnabled

            // Per impostare la cover personalizzata della webview
            /*
            setCustomVideoPoster(
                // Se drawable è una immagine
                //BitmapFactory.decodeResource(context.resources, R.drawable.app_icon)
                // Se drawable è un'immagine xml
                vectorToBitmap(this@PlayerService, R.drawable.app_icon)
            )
             */


            val iFramePlayerOptions = IFramePlayerOptions.Builder(appContext())
                .listType("playlist")
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
                            if (hybridPlayer.isPlaying == true && useVolumeKeysToChangeSong) {
                                hybridPlayer.forceSeekToNext()
                            } else {
                                audioManager.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                setCurrentVolume(audioManager.getStreamVolume(STREAM_TYPE))
                            }
                        } else if (direction == VOLUME_DOWN) {
                            if (hybridPlayer.isPlaying == true && useVolumeKeysToChangeSong) {
                                hybridPlayer.forceSeekToPrevious()
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
        updateLegacyNotification()

        serviceScope.launch { saveQueue() }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("PlayerService onTaskRemoved closeServiceAfterMinutes $closeServiceAfterMinutes")
        if (closeServiceAfterMinutes != DurationInMinutes.Disabled) {
            binder.startSleepTimer(closeServiceAfterMinutes.milliSeconds)
        }
    }

    @UnstableApi
    override fun onDestroy() {
        Timber.d("PlayerService onDestroy")

        _isServiceReady.value = false

        sendCloseExternalEqualizerIntent()

        // Rimuovi l'observer per evitare memory leak
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)

        serviceScope.launch { saveQueue() }


        try {
            unregisterReceiver(legacyActionReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver ${e.message}")
        }

        if (::legacyMediaSession.isInitialized) {
            legacyMediaSession.isActive = false
            legacyMediaSession.release()
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

            _internalYouTubePlayer.value = null

            _internalYouTubePlayerView.value.release()
        } catch (e: Exception) {
            Timber.e("PlayerService Error in online player release: ${e.message}")
        }

        serviceScope.cancel()

        runCatching {

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

            unregisterAudioDeviceCallback()


        }.onFailure {
            Timber.e("Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceInForeground = false
        stopSelf()

        super.onDestroy()
    }

    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (appSettings.isPauseOnVolumeZeroEnabled) {
            if ((_playerState.value.isPlaying) && currentVolume < 1) {
                hybridPlayer.pause()
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                hybridPlayer.play()
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

        Timber.d("PlayerService onAudioVolumeChanged currentVolume=$currentVolume maxVolume=$maxVolume newPlayerVolume=$newPlayerVolume as userVolume")

        // Ora, impostiamo il volume su HybridPlayer per mantenerlo sincronizzato
        // QUESTO farà scattare l'onVolumeChanged di ExoPlayer se serve,
        // o aggiornerà la WebView se è attiva.
        hybridPlayer.setVolume(newPlayerVolume)

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

        hybridPlayer.pause()

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

        // Aggiorno stato del mediaitem in modo da recuperare la canzone dal database con currentSong
        currentMediaItemState.value = mediaItem
        _playerState.update { state ->
            state.withMediaTransition(
                mediaItem = mediaItem,
                queueIndex = player.currentMediaItemIndex,
                queueSize = player.mediaItemCount,
            )
        }

        mediaItem.let {

            if (!it.isLocal){
                // Ferma ExoPlayer prima di avviare il player online
                hybridPlayer.pause()
                hybridPlayer.switchToYoutube()
                Timber.d("PlayerService onMediaItemTransition mediaItem not local, before")

                if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                    _internalYouTubePlayer.value?.cueVideo(it.mediaId, playFromSecond)
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

                // Forza il volume al massimo
                _internalYouTubePlayer.value?.setVolume(getSystemMediaVolume())

                // Recupera genere
                val mbclient = MusicBrainz()
                val genreHelper = MBMetadataHelper(mbclient)
                serviceScope.launch {
                    genreHelper.onSongPlayed(it.mediaId)
                }

            } else {
                // Canzone locale o MusicVault — ferma il player online e lascia andare ExoPlayer
                hybridPlayer.pause()

                hybridPlayer.switchToExo()

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

        initializeNormalizeVolume()
        maybeProcessRadio(reason)

        updateLegacyNotification()

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
    fun updateLegacyNotification() {
//        Timber.d("PlayerService notify called from: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        serviceScope.launch {
            withContext(Dispatchers.Main){
                // Aggiorna sempre la sessione per riflettere lo stato reale, anche se vuoto
                updateLegacyMediasession()

                if (player.mediaItemCount <= 0 && _playerState.value.playbackState == PlaybackState.IDLE) {
                    // Nasconde notifica se completamente idle e vuoto, attenzione il sistema potrebbe killare il servizio
                    // stopForeground(STOP_FOREGROUND_REMOVE)
                    return@withContext
                }

                startForeground()
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

        legacyMediaSession.setQueue(queueItems)

        legacyMediaSession.setQueueTitle(resources.getString(R.string.now_playing_title))
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
                        videoId = currentSong.value?.id
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

        legacyMediaSession.setMetadata(metadataBuilder.build())
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
                        _internalYouTubePlayer.value?.pause()
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
        _internalYouTubePlayer.value?.let { return it }

        // Altrimenti si inizializza.
        initializeOnlinePlayer()
        // Attendo che sia stato inizializzato prima di andare avanti
        return _internalYouTubePlayer.first { it != null }!!
    }

    @UnstableApi
    private fun sendOpenExternalEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                    if (currentSong.value?.isLocal == true) player.audioSessionId
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
                    if (currentSong.value?.isLocal == true) player.audioSessionId
                    else 0
                )
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    @ExperimentalCoroutinesApi
    private fun updateLegacyMediasession() {

        val currentMediaItem = currentSong.value?.asMediaItem
        val currentMediaItemDuration = if (currentMediaItem?.isLocal == false) (_currentDuration.value * 1000).toLong() else player.duration
        val currentMediaItemPosition = if(currentMediaItem?.isLocal == false) (_currentSecond.value * 1000).toLong() else player.currentPosition

        legacyMediaSession.setMetadata(
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


        legacyMediaSession.setPlaybackState(
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

        Timber.d("PlayerService updateLegacyMediasessionData onlineplayer playing ${_playerState.value.isPlaying} currentSecond ${_currentSecond.value} localplayer playing ${player.isPlaying}")
    }


    // ===================================================================
    // WRAPPER PER YOUTUBE CONTROL
    // Traduce i comandi di Media3 (millisecondi, 0f-1f)
    // nel linguaggio della WebView di YouTube (secondi, 0f-100f)
    // ===================================================================
    private inner class YouTubeControlImpl : YouTubeControl {

        override fun play() {
            _internalYouTubePlayer.value?.play()
        }

        override fun pause() {
            _internalYouTubePlayer.value?.pause()
        }

        override fun seekTo(positionMs: Long) {
            // ATTENZIONE: L'API di YouTube IFrame usa i SECONDI (Float), non i millisecondi!
            val seconds = positionMs.toFloat() / 1000f
            _internalYouTubePlayer.value?.seekTo(seconds)
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
            //return isPlayingNow || player.isPlaying
            return _playerState.value.isPlaying
        }

        override fun getVolume(): Float = 1f

        override fun setVolume(volume: Float) {
            // ATTENZIONE CRITICA: L'API di YouTube IFrame vuole il volume da 0 a 100!
            // Media3 manda un float da 0.0 a 1.0, quindi moltiplichiamo per 100.
            _internalYouTubePlayer.value?.setVolume((volume * 100F).toInt().coerceIn(0, 100))
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
            _internalYouTubePlayer.value?.setPlaybackRate(ytRate)
        }
    }

    inner class LegacyActionReceiver() : BroadcastReceiver() {

        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("MainActivity onReceive intent.action: ${intent.action}")
            val currentMediaItem = hybridPlayer.currentMediaItem

            binder.let {
                when (intent.action) {
                    Action.pause.value -> {
                        player.pause()
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                            _internalYouTubePlayer.value?.pause()
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

                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                            hybridPlayer.play()
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
                            hybridPlayer.seamlessQueue(currentMediaItem)

                            if(!GlobalSharedData.riTuneCastActive)
                                _internalYouTubePlayer.value?.play()
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
            updateLegacyNotification()
        }

    }

    @ExperimentalCoroutinesApi
    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Timber.d("Playerservice onIsPlayingChanged $isPlaying called")

        handleForeground(isPlaying)

        if (isPlaying) {
            startEndedObserver()
            startCrossfadeMonitor()
            updatePlayerState(PlayerConstants.PlayerState.PLAYING)
        }
        else {
            stopEndedObserver()
            stopCrossFadeMonitor()
            updatePlayerState(PlayerConstants.PlayerState.PAUSED)
        }

        updateWidgetState()
        updateLegacyNotification()

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

        val currentMediaItem = hybridPlayer.currentMediaItem

        createNotificationChannels()

        val forwardAction = NotificationCompat.Action.Builder(
            R.drawable.play_skip_forward,
            "next",
            Action.next.pendingIntent
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            if (_playerState.value.isPlaying) R.drawable.pause else R.drawable.play,
            if (_playerState.value.isPlaying || player.isPlaying) "pause" else "play",
            if (_playerState.value.isPlaying) Action.pause.pendingIntent
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
                    .setMediaSession(legacyMediaSession.sessionToken)

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
        val webDavPwdDecrypted = if (appSettings.isWebDavEnabled)
            CryptoManager.decrypt(appSettings.webDavPassword)
        else ""

        if (webDavPwdDecrypted.isEmpty() && appSettings.isWebDavEnabled) {
            SmartMessage(getString(R.string.warning_you_must_re_enter_your_webdav_password), type = PopupType.Warning, context = this@PlayerService)
        }

        val webDavConfig = WebDavConfig(
            baseUrl = appSettings.webDavUrl,
            username = appSettings.webDavUsername,
            password = webDavPwdDecrypted
        )

        // 1. Configura il client OkHttp con le credenziali WebDAV (se presenti)
        val okHttpClient = OkHttpClient.Builder()
            .proxy(Environment.proxy)
            .apply {
                if (appSettings.isWebDavEnabled)
                    addInterceptor(DynamicWebDavAuthInterceptor())
                    /*
                    addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "Authorization",
                                okhttp3.Credentials.basic(webDavConfig.username, webDavConfig.password)
                            )
                            .build()
                        chain.proceed(request)
                    }
                     */
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

                val isLocal = currentSong.value?.isLocal == true
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
                            _internalYouTubePlayer.value?.seekTo(0f)
                        }
                        QueueLoopType.Default -> {
                            if (hybridPlayer.hasNextMediaItem()) {
                                lastProcessedIndex = hybridPlayer.currentMediaItemIndex
                                handlePlayNext()
                            }
                        }
                        QueueLoopType.RepeatAll -> {
                            if (!hybridPlayer.hasNextMediaItem()) {
                                hybridPlayer.playAtIndex(0)
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

    private fun getSystemMediaVolume() = 100

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
            mediaItemPosition = if (currentSong.value?.isLocal == true) {
                player.currentPosition
            } else {
                (youtubeCurrentSecond.value * 1000).toLong()
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
                    _internalYouTubePlayer.value?.pause()
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

                    // Aggiorno il mediaitem in coda ma solo se non è in riproduzione
                    if (itemId == songId && currentSong.value?.mediaId != songId ) {
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

        // Migrated to hybridPlayer
//        val exoPlayer: ExoPlayer?
//            get() = if (::player.isInitialized) this@PlayerService.player else null

//        val youtubePlayer: YouTubePlayer?
//            get() = this@PlayerService.internalYoutubePlayer.value

        val hybridPlayer: HybridPlayer
            get() = this@PlayerService.hybridPlayer

        val playerState: StateFlow<PlayerState>
            get() = this@PlayerService.playerState

        val youtubePlayerPlayingState: Boolean
            get() = this@PlayerService.playerState.value.isPlaying

        val youtubePlayerBufferedFraction: StateFlow<Float>
            get() = this@PlayerService.internalYoutubeBufferedFraction

        val youtubePlayerCurrentDuration: StateFlow<Float>
            get() = this@PlayerService.youtubeCurrentDuration

        val youtubePlayerCurrentSecond: StateFlow<Float>
            get() = this@PlayerService.youtubeCurrentSecond

        val youtubePlayerView: StateFlow<YouTubePlayerView?>
            get() = this@PlayerService.internalYoutubePlayerView

        val cache: Cache
            get() = this@PlayerService.cache


        val currentMediaItemAsSong: Song?
            get() = this@PlayerService.currentSong.value

        fun restoreUserVolume() {
            if (!_isServiceReady.value || !this@PlayerService::hybridPlayer.isInitialized) return
            val currentSettings = appSettingsManager.activeSettings.value
            restorePlayerVolume(
                userVolume = currentSettings.userVolume,
                isFading = isFading,
                isServiceReady = true,
                setVolume = { hybridPlayer.applyVolumeNormalization() },
            )
        }

        val riTuneCastClient: RiTuneCastClient
            @Synchronized
            get() = this@PlayerService.riTuneCastClient

        val equalizer: EqualizerHelper?
            get() = if (this@PlayerService::equalizerHelper.isInitialized) this@PlayerService.equalizerHelper else null

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

            Timber.d("PlayerService startSleepTimer delayMillis $delayMillis, scheduled for $endTime")

            timerJob = serviceScope.timer(delayMillis) {
                Timber.d("PlayerService timer finished naturally")
                executeStopServiceLogic()
            }
        }

        fun executeStopServiceLogic() {

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

        fun cancelTimer() {
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
                        hybridPlayer.addMediaItems( songs.drop(1))
                    } else {
                        hybridPlayer.forcePlayFromBeginning(songs)
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
                    currentSong.debounce(1000).conflate().collect(serviceScope) { updateLegacyNotification() }
                }
            }

        }

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        fun toggleShuffle() {
            hybridPlayer.shuffleModeEnabled.let { hybridPlayer.shuffleModeEnabled = !it }

        }

        fun toggleRepeat() {
            val queueLoopType = appSettings.queueLoopType
            val new = appSettings.copy(queueLoopType = setQueueLoopState(queueLoopType))
            serviceScope.launch {
                AppSettingsManager().updateSettings(new)
            }
        }

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
    fun initializeLegacySessionCallback() {
        Timber.d("PlayerService InitializeLegacySessionCallback")
        val currentMediaItem = currentSong.value?.asMediaItem

        binder.let {
            legacyMediaSession.setCallback(
                LegacyMediaSessionCallback(
                    binder = it,
                    onPlayClick = {
                        Timber.d("PlayerService InitializeLegacySessionCallback onPlayClick")

                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)

                            hybridPlayer.play()
                        else
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "play",
                                        position = playFromSecond
                                    )
                                )
                            }

                        updateLegacyNotification()
                    },
                    onPauseClick = {
                        Timber.d("PlayerService InitializeLegacySessionCallback onPauseClick")

                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected) {
                            hybridPlayer.pause()
                        } else {
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "pause",
                                    )
                                )
                            }
                        }
                        updateLegacyNotification()
                    },
                    onSeekToPos = { newPosition ->
                        Timber.d("PlayerService InitializeLegacySessionCallback onSeekPosTo ${newPosition}")
                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus != RiTuneConnectionStatus.Connected)
                            hybridPlayer.seekTo(newPosition)
                        else
                            serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "seek",
                                        position = newPosition.div(1000) .toFloat()
                                    )
                                )
                            }

                        _currentSecond.value = newPosition.div(1000).toFloat()

                        updateLegacyNotification()
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
                            hybridPlayer.seekToDefaultPosition(timelineIndex)
                        }
                    },
                    onCustomClick = { customAction ->
                        Timber.d("PlayerService InitializeLegacySessionCallback onCustomClick $customAction")
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
                                    hybridPlayer.seamlessQueue(currentMediaItem)

                                    if(!GlobalSharedData.riTuneCastActive)
                                        hybridPlayer.play()
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
        hybridPlayer.pause()
        val now = System.currentTimeMillis()
        if (now - lastPlayNextTime < debounceDelayMs) {
           Timber.d("PlayerService handlePlayNext ignored (too fast) play current")
           hybridPlayer.play()
            return
        }
        lastPlayNextTime = now
        Timber.d("PlayerService handlePlayNext executed")

        playFromSecond = 0f

        serviceScope.launch {
            withContext(Dispatchers.Main) {
                hybridPlayer.playNext()
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
        // Controllo totale sulla disponibilità del servizio esterno al binder
        private val _isServiceReady = MutableStateFlow(false)
        val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()

        const val NOTIFICATION_ID = 1001
        val NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.player_notification_channel_id)

        const val SLEEPTIMER_NOTIFICATION_ID = 1002
        val SLEEPTIMER_NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.sleep_timer_notification_channel_id)

    }

}

