package it.fast4x.riplay.services.playback

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
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
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Base64
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
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
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.themed.SmartMessage
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
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.isOfficialContent
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
import it.fast4x.riplay.data.Database.Companion.clearOldEmptyQueuedMediaItems
import it.fast4x.riplay.data.Database.Companion.queuedMediaItems
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.enums.CrossfadeDuration
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
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.mediaItems
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
import kotlin.time.Duration.Companion.seconds
import android.os.Binder as AndroidBinder
import it.fast4x.riplay.extensions.appsettings.AppSettingsManager
import it.fast4x.riplay.extensions.experimental.webdavlibrary.DynamicWebDavAuthInterceptor
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.services.playback.common.PlaybackContext
import it.fast4x.riplay.services.playback.common.PlaybackState
import it.fast4x.riplay.services.playback.common.PlayerState
import it.fast4x.riplay.utils.BitmapLoader
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getDeviceVolume
import it.fast4x.riplay.utils.isWebDav
import it.fast4x.riplay.utils.setQueueLoopState
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import kotlin.collections.none
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    OnAudioVolumeChangedListener
{
    val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var mediaLibrarySessionCallback: MediaLibraryServiceCallback
    lateinit var hybridPlayer: HybridPlayer

    val cache: SimpleCache by lazy {
        PrincipalCache.getInstance(this)
    }
    lateinit var exoPlayer: ExoPlayer
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
    private val _internalYouTubePlayerView = MutableStateFlow<YouTubePlayerView?>(null)
    val internalYoutubePlayerView: StateFlow<YouTubePlayerView?> = _internalYouTubePlayerView

    private val _internalYouTubePlayer = MutableStateFlow<YouTubePlayer?>(null)

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

    var playlistSongsSortBy: PlaylistSongSortBy = PlaylistSongSortBy.DateAdded
    var songsSortBy: SongSortBy = SongSortBy.DateAdded
    var playlistSortBy: PlaylistSortBy = PlaylistSortBy.DateAdded
    var artistSortBy: ArtistSortBy = ArtistSortBy.DateAdded
    var albumSortBy: AlbumSortBy = AlbumSortBy.DateAdded


    var songSortOrder: SortOrder = SortOrder.Descending
    var artistSortOrder: SortOrder = SortOrder.Descending
    var albumSortOrder: SortOrder = SortOrder.Descending

    private var isServiceInForeground = false

    private var isFading = false // Flag per ignorare il fade del crossfade
    private var playbackWathcDogJob: Job? = null
    private var fadeInJob: Job? = null
    private val FADE_IN_DURATION_MS = 2000L // Durata di default del fade in
    private var lastWatchdogPosition = -1L
    private var controllerFuture: ListenableFuture<MediaController>? = null

    var lastProcessedIndex: Int? = null
    private var lastPlayPreviousTime = 0L // Lo uso per gestire i click frenetici per andare alla canzone precedente

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Timber.d("PlayerService Screenreceiver: Schermo acceso, eseguo refresh software del volume")

                    // NON CHIAMO initializeNormalizeVolume() QUI!
                    // Rischia di resettare il guadagno hardware a metà canzone.

                    if (!isFading) {
                        serviceScope.launch(Dispatchers.Main) {
                            // Abbassiamo impercettibilmente il volume del player per un millisecondo
                            // per forzare Android a sbloccare lo stato di Standby/Ducking hardware
                            hybridPlayer.setFadeVolume(0.95f)
                            delay(50.milliseconds)

                            // Riportiamo immediatamente il volume al 100% del volume utente
                            hybridPlayer.setFadeVolume(1.0f)
                            Timber.d("PlayerService: Hard refresh del volume eseguito con successo")
                        }
                    }
                }
            }
        }
    }

    private val myMainHandler = Handler(Looper.getMainLooper())

    // Observer per il ciclo di vita dell'intero processo (app in background)
//    private val processLifecycleObserver = object : DefaultLifecycleObserver {
//        override fun onStop(owner: LifecycleOwner) {
//            Timber.d("PlayerService: ProcessLifecycleOwner.onStop() schermo spento, rimuovo video")
//            // Chiamato quando l'app va in background o lo schermo si spegne
//            // Elimino i video perchè in background non sono più visibili ma creano problemi di avanzamento al successivo mediaitem
//            player.removeVideoMediaItems()
//        }
//    }

    override fun onBind(intent: Intent?): IBinder {
        return super.onBind(intent) ?: binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("PlayerService: onUnbind standard chiamato dal sistema")
        return true // Consente il rebind pulito quando l'utente riapre l'app
    }



    @ExperimentalSerializationApi
    @ExperimentalCoroutinesApi
    @FlowPreview
    @SuppressLint("Range")
    @UnstableApi
    override fun onCreate() {
        _isServiceReady.value = false

        createNotificationChannels()
        val mediaNotificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.player_notification_channel_id)
            .build()
        setMediaNotificationProvider(mediaNotificationProvider)

        // Carico le impostazioni prima di tutto
        loadInitialSettingsFromDatabase()

        // Inizializzo l'hardware audio e la sessione Media3
        initializeBitmapProvider()
        initializeHybridPlayerAndSession()
        initializeMediaItemState()

        // Uso il Main Looper per accodare la creazione di YouTube.
        // In questo modo la l'inizializzazione della webview e del player di youtube vengono fatte appena il servizio è pronto e non prima
        myMainHandler.post {
            try {
                Timber.d("PlayerService: Avvio inizializzazione sequenziale sicura di YouTube")

                // Facciamo l'inflate della View
                val inflatedView = LayoutInflater.from(this)
                    .inflate(R.layout.youtube_player, null, false) as YouTubePlayerView

                // Popoliamo lo StateFlow: adesso non è più nullo!
                _internalYouTubePlayerView.value = inflatedView

                // Lanciamo la creazione del player online di youtube
                initializeOnlinePlayer(skipAutoload = false)

                Timber.d("PlayerService: YouTube e OnlinePlayer inizializzati con successo in sicurezza")
            } catch (e: Exception) {
                Timber.e("PlayerService: Errore nell'inizializzazione asincrona di YouTube: ${e.stackTraceToString()}")
            }
        }


        super.onCreate()

        // Lancio tutto il resto delle configurazioni in background senza bloccare l'avvio nativo
        serviceScope.launch(Dispatchers.Main) {

            withContext(Dispatchers.IO) {
                startObservingSettings()
            }

            startPlaybackWatchdog()

            checkAndRestoreTimer()

            initializeAudioManager()
            initializeAudioVolumeObserver()
            initializeAudioEqualizer()

            initializeAudioDeviceCallback()

            // Ora che la webview e l'hybrid player sono stabili, calcolo il volume di normalizzazione
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

            val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
            registerReceiver(screenReceiver, filter)

            _isServiceReady.value = true
        }
    }


    @kotlin.OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
    private fun setupPersistentQueueAndObservers() {
        if (appSettings.persistentQueue) {
            serviceScope.launch {
                // Caricamento iniziale obbligatorio sul Main thread per ExoPlayer
                withContext(Dispatchers.Main) {
                    loadQueue()
                    resumePlaybackOnStart()
                }

                // Usiamo il flusso reattivo dello stato del player per salvare la coda SOLO quando cambia lo stato
                // Questo evita di bloccare il thread Main ogni 10 secondi eliminando i picchettii audio (buffer underrun)
                _playerState
                    .map { it.isPlaying }
                    .distinctUntilChanged()
                    .collectLatest { isPlaying ->
                        // Salva la coda non appena l'app cambia stato (es. passa da Play a Pausa o viceversa)
                        saveQueue()
                        Timber.d("PlayerService saveQueue ottimizzato eseguito per cambio stato riproduzione: isPlaying=$isPlaying")
                    }
            }

            // Ciclo leggero isolato solo per l'avanzamento della cronologia online (senza toccare la timeline di ExoPlayer)
            serviceScope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(10.seconds)
                    if (_playerState.value.isPlaying && youtubeCurrentSecond.value >= minTimeForEvent.seconds && lastMediaIdInHistory != currentSong.value?.id) {
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

                val currentMediaId = if (!song.isLocal) song.id else song.mediaId.toString()

                if (lastOnlineMediaId != currentMediaId && onlineListenedDurationMs > 0) {
                    Timber.d("PlayerService incrementOnlineListenedPlaytimeMs update currentSong onlineListenedDurationMs = $onlineListenedDurationMs onlineMediaId = $currentMediaId currentMediaId = $currentMediaId")
                    incrementOnlineListenedPlaytimeMs()
                    delay(200.milliseconds)
                    onlineListenedDurationMs = 0L
                    lastOnlineMediaId = currentMediaId
                }

                val format = Database.format(currentMediaId).first()
                if (format == null && (!song.isLocal || song.isWebDav)) {
                    getOnlineMetadata(currentMediaId)
                        ?.let {
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
                            queueIndex = exoPlayer.currentMediaItemIndex,
                            queueSize = exoPlayer.mediaItemCount,
                        )
                    }
                }
            }
        }

        // Monitora il tempo di ascolto della canzone riprodotta dalla webview
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

                }
                delay(500.milliseconds)
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



    @UnstableApi
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Forziamo startInForegroundRequired a true quando il player è effettivamente in riproduzione
        // Questo risolve i bug legati alle notifiche invisibili o "congelate" nel system_server
        val forceForeground = hybridPlayer.isPlaying || startInForegroundRequired
        super.onUpdateNotification(session, forceForeground)
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

        super.onStartCommand(intent, flags, startId)

        Timber.d("PlayerService onStartCommand intent action ${intent?.action}")
        when (intent?.action) {
            Action.play.value -> { if (currentSong.value?.isLocal == true) exoPlayer.play() else _internalYouTubePlayer.value?.play() }
            Action.pause.value -> { if (currentSong.value?.isLocal == true) exoPlayer.pause() else _internalYouTubePlayer.value?.pause() }
            Action.next.value -> handlePlayNext("PlayerService.onStartCommand")
            Action.previous.value -> handlePlayPrevious()
        }
        updateWidgetState()

        return START_STICKY
    }

    private fun initializeMediaItemState() {
        currentMediaItemState.value = exoPlayer.currentMediaItem
    }

    fun replaceOnlinePlayerView() {
        _internalYouTubePlayer.value?.pause()
        _internalYouTubePlayer.value = null

        // DISTRUZIONE REALE DELLA VECCHIA WEBVIEW
        _internalYouTubePlayerView.value?.let { oldView ->
            try {
                (oldView.parent as? android.view.ViewGroup)?.removeView(oldView)
                oldView.removeAllViews()
                oldView.release()
            } catch (e: Exception) {
                Timber.e("Errore nel destroy durante il rimpiazzo: ${e.message}")
            }
        }

        // Ora creiamo la nuova View in uno stato di memoria pulito
        _internalYouTubePlayerView.value = LayoutInflater.from(this)
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
            while (appSettings.playbackDuration > 0) {
                withContext(Dispatchers.Main) {
                    Timber.d("PlayerService initializeMedleyMode medleyDuration ${appSettings.playbackDuration} player.isPlaying ${exoPlayer.isPlaying} internalOnlinePlayerState ${_playerState.value.isPlaying}")
                    val seconds =
                        if (currentSong.value?.isLocal == true) exoPlayer.currentPosition.div(1000)
                            .toInt() else _currentSecond.value.toInt()
                    if (appSettings.playbackDuration.toInt() <= seconds) {
                        handlePlayNext("PlayerService.initializeMedleyMode")
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
//                        when (playerState) {
//                            PlayerConstants.PlayerState.PLAYING -> {
//                                startPlaybackWatchdog()
//                            }
//                            else -> {
//                                stopPlaybackWatchdog()
//                            }
//                        }

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
                delay(1000.milliseconds)
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
                    handlePlayNext("PlayerService.onSensorChanged")
                }

            }

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun resumePlaybackOnStart() {
        if (!appSettings.persistentQueue && !appSettings.resumePlaybackOnStart) return

        when (currentSong.value?.isLocal) {
            true -> {
                if (!exoPlayer.isPlaying) exoPlayer.play()
            }

            else -> {}
        }

    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    override fun onRepeatModeChanged(repeatMode: Int) {
        val currentState = _playerState.value
        val settings = currentState.settings
        _playerState.value = currentState.copy(settings = settings.copy(repeatMode = QueueLoopType.from(repeatMode)))
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
    fun recreateOnlinePlayerView() {
        replaceOnlinePlayerView()
        initializeOnlinePlayer(skipAutoload = true)
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private fun initializeHybridPlayerAndSession() {

        exoPlayer = ExoPlayer.Builder(this)
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
                appSettings.handleAudioFocusEnabled
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
                //addListener(this@PlayerService) // listener è registrato su hybridPlayer
                sleepTimerListener = SleepTimerListener(serviceScope, this)
                addListener(sleepTimerListener)
                addAnalyticsListener(PlaybackStatsListener(false, this@PlayerService))
            }

        exoPlayer.repeatMode = appSettings.queueLoopType.type

        exoPlayer.skipSilenceEnabled = appSettings.skipSilenceEnabled
        exoPlayer.pauseAtEndOfMediaItems = true

        // Crea l'Hybrid Player
        hybridPlayer = HybridPlayer(this,exoPlayer, ytControlWrapper)
        // REGISTRA IL SERVIZIO SULL'HYBRID PLAYER
        // In questo modo onIsPlayingChanged riceverà gli eventi di ENTRAMBI i motori
        hybridPlayer.addListener(this@PlayerService)

        // Imposto il volume dell'Hybrid Player
        val deviceVol = getDeviceVolume(this)
        /* // Osservo il volume iniziale del dispositivo ma non mi fido quindi non lo salvo
        hybridPlayer.volume = deviceVol
        // Lo salvo nelle impostazioni
        serviceScope.launch {
            appSettingsManager.updateSettings(appSettings.copy(userVolume = deviceVol))
        }

         */
        Timber.d("PlayerService initializeHybridPlayerAndSession initial device volume = $deviceVol")

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

        // Keep a connected controller so maybe that notification works
        val sessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controllerFuture?.get()
            } catch (e: Exception) {
                Timber.tag("PlayerService").e(e, "Failed to initialize MediaController")
                controllerFuture = null
                stopSelf()
            }
        }, MoreExecutors.directExecutor())

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

                youTubePlayerView?.let { view ->
                    val customUiController =
                        CustomDefaultPlayerUiController(
                            this@PlayerService,
                            view,
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
                    view.setCustomPlayerUi(customUiController.rootView)
                }



                Timber.d("PlayerService onlinePlayer onReady localmediaItem ${currentSong.value?.id} queue index ${hybridPlayer.currentMediaItemIndex}")
                Timber.d("PlayerService onlinePlayer onReady isPersistentQueueEnabled $appSettings.persistentQueue isResumePlaybackOnStart ${appSettings.resumePlaybackOnStart}")

                youTubePlayer.setVolume(getSystemMediaVolume())

                if (currentSong.value?.isLocal == true) return

                currentSong.value?.id?.let{
                    if (appSettings.persistentQueue && appSettings.resumePlaybackOnStart && firstTimeStarted && !skipAutoload) {
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
                        // 1. Cancelliamo subito eventuali watchdog precedenti per evitare sovrapposizioni
                        unstartedWatchdogJob?.cancel()

                        if (!firstTimeStarted) {
                            val expectedMediaId = currentSong.value?.id

                            unstartedWatchdogJob = serviceScope.launch(Dispatchers.Main) {
                                Timber.d("PlayerService onlinePlayerView: onStateChange UNSTARTED rilevato per mediaId=$expectedMediaId")

                                delay(5000.milliseconds)

                                val stillUnstartedHeavy = _playerState.value.playbackState == PlaybackState.UNSTARTED
                                val sameMediaHeavy = currentSong.value?.id == expectedMediaId

                                if (stillUnstartedHeavy && sameMediaHeavy && expectedMediaId != null) {
                                    Timber.e("PlayerService onlinePlayerView: KICK fallito. Persistent UNSTARTED dopo 5s. Probabilmente webView killed. Avvio ricreazione pesante.")

                                    recreateOnlinePlayerView()
                                    // Recuperiamo la prima istanza valida post-ricreazione
                                    val currentPlayer = this@PlayerService._internalYouTubePlayer.first { it != null }!!

                                    currentSong.value?.let { item ->
                                        if (item.isLocal) return@let
                                        Timber.d("PlayerService onlinePlayerView: Try reload song/video post-crash")

                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                            exoPlayer.stop()
                                        }
                                        currentPlayer.pause()
                                        _internalYouTubePlayer.value?.pause()

                                        // Invece di cueVideo, proviamo a usare loadVideo se la tua libreria lo espone,
                                        // altrimenti manteniamo cueVideo ma seguito da un piccolo delay e play()
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

                                // Prepara hybridPlayer per la riproduzione
                                hybridPlayer.playWhenReady = true

                                youTubePlayer.play()
                            }

                        }

                    }
                    PlayerConstants.PlayerState.PLAYING -> {
                        lastError = null  // reset errore dopo riproduzione riuscita
                        onlineNearEndTicks = 0
                        //startPlaybackWatchdog()

                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        onlineNearEndTicks = 0
                        //stopPlaybackWatchdog()

                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }

                    else -> {}
                }

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

                if (appSettings.persistentQueue)
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
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                exoPlayer.stop()
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

                    return
                }

                lastError = error

                if (!appSettings.skipMediaOnError) return
                val prev = hybridPlayer.currentMediaItem ?: return

                // Ferma ExoPlayer se sta andando
//                if (exoPlayer.isPlaying) {
//                    exoPlayer.pause()
//                    exoPlayer.stop()
//                }

                handlePlayNext("PlayerService.initializeOnlinePlayer.onError")

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
        youTubePlayerView?.apply {
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
                            if (hybridPlayer.isPlaying && useVolumeKeysToChangeSong) {
                                handlePlayNext("PlayerService.getVolumeProvider.onAdjustVolume")
                            } else {
                                audioManager.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                setCurrentVolume(audioManager.getStreamVolume(STREAM_TYPE))
                            }
                        } else if (direction == VOLUME_DOWN) {
                            if (hybridPlayer.isPlaying && useVolumeKeysToChangeSong) {
                                handlePlayPrevious()
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
            val shuffledIndices = IntArray(exoPlayer.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(exoPlayer.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = exoPlayer.currentMediaItemIndex
            exoPlayer.shuffleOrder = DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis())
        }

        serviceScope.launch { saveQueue() }
    }

    // Gestito tramite onUnBind
    override fun onTaskRemoved(rootIntent: Intent?) {
        val closeServiceAfterMinutes = appSettings.closeBackgroundPlayerAfterMinutes
        Timber.d("PlayerService onTaskRemoved closeServiceAfterMinutes $closeServiceAfterMinutes")
        if (closeServiceAfterMinutes != DurationInMinutes.Disabled) {
            binder.startAutoCloseTimer(closeServiceAfterMinutes.milliSeconds)
        }
    }



    @UnstableApi
    override fun onDestroy() {
        Timber.d("PlayerService: onDestroy AVVIATO")

        _isServiceReady.value = false

        stopPlaybackWatchdog()

        sendCloseExternalEqualizerIntent()

        // RIMOZIONE SICURA DEI RECEIVER
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy Errore nella rimozione dello screenReceiver: ${e.message}")
        }

        try {
            unregisterReceiver(legacyActionReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver legacyActionReceiver: ${e.message}")
        }

        if (::equalizerHelper.isInitialized) {
            equalizerHelper.release()
        }

        if (::hybridPlayer.isInitialized) {
            hybridPlayer.release()
        }


        try {
            exoPlayer.removeListener(this@PlayerService)
            exoPlayer.release()
            Timber.d("PlayerService onDestroy: ExoPlayer rilasciato in sicurezza")
        } catch (e: Exception) {
            Timber.e("PlayerService Error in local player release: ${e.message}")
        }

        // ANNULLAMENTO DEI JOB E PULIZIA RISORSE MEDIA3
        runCatching {
            controllerFuture?.let { MediaController.releaseFuture(it) }
            controllerFuture = null

            mediaLibrarySession?.release()
            mediaLibrarySession = null

            cache.release()
            loudnessEnhancer?.release()
            audioVolumeObserver.unregister()
            discordPresenceManager?.onStop()

            endedObserverJob?.cancel()
            riTuneObserverJob?.cancel()
            timerJob?.cancel()
            unstartedWatchdogJob?.cancel()
            volumeNormalizationJob?.cancel()
            settingsObserverJob?.cancel()

            AudioDRCHelper.restoreDRC()

            // non chiamare notificationManager?.cancelAll()
            // se no viene cancellata la notifica di cortesia del timer!

            unregisterAudioDeviceCallback()

        }.onFailure {
            Timber.e("PlayerService: Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }

        // Chiudiamo il raggio d'azione delle coroutine solo alla fine, dopo che l'hardware è spento
        serviceScope.cancel()

        isServiceInForeground = false

        Timber.d("PlayerService: onDestroy COMPLETATO CON SUCCESSO")
        super.onDestroy()
    }


    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (appSettings.isPauseOnVolumeZeroEnabled) {
            if ((_playerState.value.isPlaying) && currentVolume < 1) {
                serviceScope.launch {
                    delay(300.milliseconds)
                    if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < 1) {
                        hybridPlayer.pause()
                        pausedByZeroVolume = true
                    }
                }
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                hybridPlayer.play()
                pausedByZeroVolume = false
            }
        }

        // Se l'utente tocca i tasti del volume MENTRE c'è un fade in corso,
        // interrompiamo giustamente il fade per dare il controllo all'utente.
        if (isFading) {
            fadeInJob?.cancel()
            playbackWathcDogJob?.cancel()
            isFading = false
        }

        // Osservo il volume del dispositivo ma non mi fido e non lo salvo
        // nè lo imposto su hybridPlayer perchè potrebbe essere falsato
        /*
        // Convertiamo il volume di sistema in float (0.0 - 1.0)
        val newPlayerVolume = currentVolume.toFloat() / maxVolume.toFloat()

        // Salva il volume reale scelto dall'utente nelle impostazioni
        serviceScope.launch(Dispatchers.IO) {
            appSettingsManager.updateSettings(appSettings.copy(userVolume = newPlayerVolume))
        }

        Timber.d("PlayerService onAudioVolumeChanged: userVolume aggiornato a $newPlayerVolume")

        //  Chiamiamo il setVolume di HybridPlayer.
        // Con la nuova logica, questo aggiornerà 'userVolume' dentro il player,
        // ricalcolando il volume finale SENZA distruggere il fadeMultiplier!
        hybridPlayer.setVolume(newPlayerVolume)
         */
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
        // Gestione sicura del brano locale che arriva alla fine naturale (con Crossfade OFF)
        if (playbackState == Player.STATE_ENDED && hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {
            if (appSettings.crossfadeDuration == CrossfadeDuration.Off) {
                Timber.d("PlayerService onPlaybackStateChanged: Fine naturale del file Exo (Crossfade OFF). Forzo handlePlayNext()")
                handlePlayNext("PlayerService.onPlaybackStateChanged")
            }
        }

        // 2. Gestione dello stato READY per il fade-in
        if (playbackState == Player.STATE_READY && hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {
            if (appSettings.crossfadeDuration != CrossfadeDuration.Off) {
                if (!isFading) {
                    startFadeIn()
                } else {
                    hybridPlayer.applyCurrentVolume()
                }
            } else {
                hybridPlayer.setFadeVolume(1.0f) // Volume pieno se il crossfade è spento
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        // Questo evento scatta quando ExoPlayer cambia traccia automaticamente alla fine del file!
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {

            if (appSettings.crossfadeDuration == CrossfadeDuration.Off) {
                // SE IL CROSSFADE È SPENTO:
                // Intercettiamo la transizione automatica nativa di ExoPlayer e la deviamo
                // sulla nostra handlePlayNext() per pulire i motori (WebView/Exo) e caricare tutto correttamente!
                Timber.d("PlayerService Listener: Rilevata fine brano Exo con Crossfade OFF. Forzo handlePlayNext()")

                // handlePlayNext() fa il lavoro di sostituzione traccia pulito.
                // Se dovesse saltare un brano di troppo, uso il workaround diretto
                //exoPlayer.seekToPreviousMediaItem()

                handlePlayNext("PlayerService.onPositionDiscontinuity")
            } else {
                // SE IL CROSSFADE È ATTIVO:
                // (Ci ha già pensato il monitor del crossfade a sfumare, quindi qui resettiamo solo il volume)
                hybridPlayer.setFadeVolume(0f)
                startFadeIn()
            }
        }
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
        //stopPlaybackWatchdog()

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

        _currentSecond.value = 0F

        val newMediaId = mediaItem.mediaId

        if (lastOnlineMediaId == newMediaId) {
            Timber.d("PlayerService: onMediaItemTransition Transition ignored, same MediaID ($newMediaId) skipped")
            handlePlayNext("PlayerService.onMediaTransition Transition ignored same id")
            return
        }

        Timber.d("PlayerService onMediaItemTransition mediaItem ${mediaItem.mediaId} reason $reason")

        currentQueuePosition = exoPlayer.currentMediaItemIndex

        if (parentalControlEnabled && mediaItem.isExplicit) {
            handlePlayNext("PlayerService.onMediaItemTransition parental control enabled")
            SmartMessage(resources.getString(androidx.media3.session.R.string.error_message_parental_control_restricted), context = this@PlayerService)
            return
        }

        if (excludeIfIsVideoEnabled && mediaItem.isVideo) {
            handlePlayNext("PlayerService.onMediaItemTransition excludeIfIsVideoEnabled")
            SmartMessage(getString(R.string.warning_skipped_video), context = this@PlayerService)
            return
        }

        var blacklisted by mutableStateOf(false)
        runBlocking(Dispatchers.IO) {
            blacklisted = Database.blacklisted(mediaItem.mediaId) > 0
        }
        if (blacklisted) {
            handlePlayNext("PlayerService.onMediaItemTransition blacklisted")
            SmartMessage(getString(R.string.warning_skipped_blacklisted_song), context = this@PlayerService)
            return
        }

        // Aggiorno stato del mediaitem in modo da recuperare la canzone dal database con currentSong
        currentMediaItemState.value = mediaItem
        _playerState.update { state ->
            state.withMediaTransition(
                mediaItem = mediaItem,
                queueIndex = exoPlayer.currentMediaItemIndex,
                queueSize = exoPlayer.mediaItemCount,
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
                    startFadeIn()
                    Timber.d("PlayerService onMediaItemTransition mediaItem not local, inside")
                } else
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

                // Mettiamo in sicurezza il moltiplicatore del fade AZZERANDOLO subito via codice
                // prima ancora che parta la coroutine, così ExoPlayer nasce nel silenzio.
                hybridPlayer.setFadeVolume(0f)

                hybridPlayer.switchToExo()

                Timber.d("PlayerService onMediaItemTransition resume playback before firstTimeStarted $firstTimeStarted isResumePlaybackOnStart ${appSettings.resumePlaybackOnStart}")
                if (firstTimeStarted && appSettings.resumePlaybackOnStart) {
                    resumePlaybackOnStart()
                    firstTimeStarted = false
                    Timber.d("PlayerService onMediaItemTransition resume playback inside")
                    return
                }

                if (firstTimeStarted && !appSettings.resumePlaybackOnStart) {
                    firstTimeStarted = false
                    return
                }

                Timber.d("PlayerService onMediaItemTransition resume playback after")

                if (!exoPlayer.isPlaying) {
                    Timber.d("PlayerService onMediaItemTransition prepare exo for play local file")
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()

                    // startFadeIn() verrà chiamato in onPlaybackStateChanged
                    // quando ExoPlayer dichiara di essere STATE_READY.
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
        Timber.d("PlayerService onMediaItemTransition mediaItem: ${mediaItem.mediaId} currentMediaItemIndex: $currentQueuePosition shuffleModeEnabled ${exoPlayer.shuffleModeEnabled} repeatMode ${exoPlayer.repeatMode} reason $reason")

    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            updateMediaSessionQueue(timeline, exoPlayer.currentMediaItemIndex)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)

        Timber.d("PlayerService onPlayWhenReadyChanged playWhenReady=$playWhenReady, reason=$reason")

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

    }

    private fun maybeProcessRadio(reason: Int) {
        if (!appSettings.autoLoadSongsInQueue
            || appSettings.queueLoopType == QueueLoopType.RepeatAll
        ) return

        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            exoPlayer.mediaItemCount - exoPlayer.currentMediaItemIndex <= 10
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
                        if (exoPlayer.playbackState != STATE_IDLE)
                            exoPlayer.addMediaItems(radio.process())
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

            // In teoria non è necessario agire sul volume interno
            //hybridPlayer.setVolume(appSettings.userVolume)
            //binder.restoreDefaultVolume()
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
                        val targetGainMb = (baseGain.toMb() + boostLevel.toMb()) - loudnessMb

                        // Applico il guadagno hardware a ExoPlayer (Questo non tocca il volume software!)
                        loudnessEnhancer?.setTargetGain(targetGainMb)
                        loudnessEnhancer?.enabled = true


                        // Questa chiamata aggiornerà la WebView se siamo su YT,
                        // ma rimarrà inerte se siamo su ExoPlayer, proteggendo il Fade In!
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
            if (isAtLeastAndroid13 || appSettings.isShowingThumbnailInLockscreen) bitmapProvider?.bitmap else null

        val uri = exoPlayer.mediaMetadata.artworkUri?.toString()?.toThumbnail(512)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, uri)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)

        if (isAtLeastAndroid13 && exoPlayer.currentMediaItemIndex == 0) {
            metadataBuilder.putText(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                "${cleanPrefix(exoPlayer.mediaMetadata.title.toString())} "
            )
        }

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
                    serviceScope.launch {
                        ensureOnlinePlayerInitialized()
                        requestSmoothPlay()
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
                        requestSmoothPause()
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
    fun sendOpenExternalEqualizerIntent() {
        // Estraiamo l'ID in modo dinamico: se l'engine attivo è ExoPlayer usiamo il suo id nativo,
        // altrimenti per YouTube forziamo la scansione globale del processo dell'app
        val sessionId = if (hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {
            try { exoPlayer.audioSessionId } catch (e: Exception) { 0 }
        } else {
            AudioManager.AUDIO_SESSION_ID_GENERATE
        }

        if (sessionId == AudioEffect.ERROR_BAD_VALUE || sessionId == 0) return

        try {
            Timber.d("PlayerService External Equalizer: Invio OPEN Intent centralizzato per engine: ${hybridPlayer.activeEngine}, session: $sessionId")
            sendBroadcast(
                Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
            )
        } catch (e: Exception) {
            Timber.e("PlayerService External Equalizer: Errore OPEN Intent centralizzato: ${e.message}")
        }
    }



    @UnstableApi
    private fun sendCloseExternalEqualizerIntent() {
        val sessionId = try { exoPlayer.audioSessionId } catch (e: Exception) { 0 }
        if (sessionId <= 0) return

        try {
            Timber.d("PlayerService External Equalizer: Invio intent di CHIUSURA sessione audio: $sessionId")
            sendBroadcast(
                Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                }
            )
        } catch (e: Exception) {
            Timber.e("PlayerService External Equalizer: Errore durante l'invio dell'intent CLOSE: ${e.message}")
        }
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
            _currentSecond.value = seconds
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
                        exoPlayer.pause()
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
                    Action.next.value -> handlePlayNext("LegacyActionReceiver.onReceive")
                    Action.previous.value -> handlePlayPrevious()
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

        }

    }

    @ExperimentalCoroutinesApi
    @UnstableApi
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        // Passo l'evento a super così Media3 esegue le sue operazioni interne sulla sessione
        super.onIsPlayingChanged(isPlaying)
        Timber.d("PlayerService onIsPlayingChanged intercettato: isPlaying=$isPlaying ")

        lastWatchdogPosition = -1 // resetto la posizione precedente se cambia lo stato

        if (isPlaying) {
            sendOpenExternalEqualizerIntent()
            //startPlaybackWatchdog()
            updatePlayerState(PlayerConstants.PlayerState.PLAYING)
        } else {
            //stopPlaybackWatchdog()
            updatePlayerState(PlayerConstants.PlayerState.PAUSED)

            // Rimuove lo stato di foreground aggressivo quando l'app va in pausa
            if (isAtLeastAndroid7) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        }

        // Forziamo il ridisegno dei pulsanti nella notifica, utile per android auto e lettori bluetooth in auto
        hybridPlayer.onRefreshCustomLayoutListener?.invoke()

        updateWidgetState()

        updateDiscordPresence()

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
                exoPlayer.clearAuxEffectInfo()
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
            reverbPreset?.id?.let { exoPlayer.setAuxEffectInfo(AuxEffectInfo(it, 1f)) }
        }
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
        Timber.d("PlayerService ExoPlayer Audio Session ID changed to: $audioSessionId")

        if (currentSong.value?.isLocal == false) {
            Timber.d("PlayerService onAudioSessionIdChanged la canzone non è locale, non c'è necessità di ricreare l'audio session perchè potrebbe creare un mute dell'audio")
            return
        }

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

    private fun startPlaybackWatchdog() {
        playbackWathcDogJob?.cancel()
        isFading = false
        lastWatchdogPosition = -1L

        Timber.d("PlayerService PlaybackWatchdog: Avvio watchdog")

        playbackWathcDogJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(200.milliseconds)

                val duration = hybridPlayer.duration
                val position = hybridPlayer.currentPosition
                val isPlaying = _playerState.value.playbackState == PlaybackState.PLAYING
                val isPaused = _playerState.value.playbackState == PlaybackState.PAUSED

                // Il guardiano calcola i dati SOLO se c'è una canzone caricata nel player
                if (duration > 0) {
                    val timeLeft = duration - position
                    val crossfadeDurationMs = appSettings.crossfadeDuration.milliseconds

                    if (BuildConfig.DEBUG)
                        Timber.d("PlayerService PlaybackWatchdog: isPlaying = $isPlaying isPaused = $isPaused timeleft $timeLeft duration=$duration ms, position=$position ms")

                    // ─── SENTINELLA AUDIO FOCUS PER YOUTUBE ONLINE (PERENNE) ───
                    if (hybridPlayer.activeEngine == ActiveEngine.YOUTUBE) {

                        // CASO A: Rilevamento perdita focus (Timeline si blocca mentre l'interfaccia è in PLAY)
                        if (isPlaying && !isFading) {
                            val isWebViewStalledByFocusLoss = position == lastWatchdogPosition &&
                                    position > 5000 &&
                                    timeLeft > 3500

                            if (isWebViewStalledByFocusLoss) {
                                Timber.w("PlayerService PlaybackWatchdog: RILEVATO STALLO TIMELINE (Focus Loss). Sincronizzo in PAUSA.")
                                hybridPlayer.executeActualPause() // non usare pausa con fade
                                lastWatchdogPosition = position
                                continue
                            }
                        }

                        // CASO B: Rilevamento risveglio automatico (Timeline avanza mentre l'interfaccia è in PAUSA)
                        // Ora funziona al 100% perché il Watchdog è vivo e vegeto!
                        if (isPaused && lastWatchdogPosition > 0 && !isFading) {
                            val isWebViewAwakenedByFocusGain = position > lastWatchdogPosition &&
                                    position > 2000 &&
                                    timeLeft > 3500

                            if (isWebViewAwakenedByFocusGain) {
                                Timber.d("PlayerService PlaybackWatchdog: RILEVATO AVANZAMENTO ANOMALO (Focus Gain). Sincronizzo in PLAY.")
                                hybridPlayer.executeActualPlay()
                                lastWatchdogPosition = position
                                continue
                            }
                        }
                    }
                    // ─────────────────────────────────────────────────────────────

                    // LE LOGICHE DI FINE BRANO SCATTANO SOLO SE L'APP STA EFFETTIVAMENTE SUONANDO
                    if (isPlaying) {
                        // SE IL CROSSFADE È ATTIVO
                        if (appSettings.crossfadeDuration != CrossfadeDuration.Off) {
                            if (timeLeft <= crossfadeDurationMs && timeLeft > -10000 && !isFading) {
                                isFading = true
                                val nextMediaItem = hybridPlayer.getMediaItemAt(hybridPlayer.nextMediaItemIndex)
                                val isNextLocal = nextMediaItem.isLocal

                                Timber.d("PlayerService PlaybackWatchdog: Attivazione Fade Out ($timeLeft ms). Prossimo locale=$isNextLocal")
                                lastWatchdogPosition = -1L // resetto la posizione precedente durante il cambio
                                if (isNextLocal) startExoToExoCrossfade() else startWebViewFadeOut()
                            }
                        }
                        // SE IL CROSSFADE È DISATTIVATO
                        else {
                            val isPlayerStalled = timeLeft <= 3500 && position == lastWatchdogPosition
                            val isNaturalEnd = timeLeft <= 1200

                            if ((isNaturalEnd || isPlayerStalled) && !isFading) {
                                isFading = true
                                if (isPlayerStalled) {
                                    Timber.w("PlayerService PlaybackWatchdog: RILEVATO STALLO FINE TRACCIA a $timeLeft ms. Forzo il cambio!")
                                } else {
                                    Timber.d("PlayerService PlaybackWatchdog: Fine brano naturale a $timeLeft ms")
                                }
                                lastWatchdogPosition = -1L // resetto la posizione precedente durante il cambio
                                handlePlayNext("PlayerService PlaybackWatchdog with crossfade disabled")
                            }
                        }
                    }

                    // Aggiorniamo la posizione storica per il prossimo ciclo
                    lastWatchdogPosition = position
                } else {
                    // Se non c'è nessun brano caricato (es. coda vuota), resettiamo i contatori storici
                    lastWatchdogPosition = -1L
                }
            }
        }
    }


    private fun stopPlaybackWatchdog() {
        playbackWathcDogJob?.cancel()
        playbackWathcDogJob = null
        isFading = false // Sblocchiamo il fading
        //Timber.d("PlayerService PlaybackWatchdog: Cancello crossfadeJob")
    }

    private fun startFadeIn() {
        if (appSettings.crossfadeDuration == CrossfadeDuration.Off) return

        fadeInJob?.cancel() // Cancelliamo eventuali fade-in residui

        isFading = true
        hybridPlayer.setFadeVolume(0f) // Inizia dal silenzio totale

        fadeInJob = serviceScope.launch(Dispatchers.Main) {
            val steps = 20
            val stepDelay = FADE_IN_DURATION_MS / steps

            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                // Sale linearmente da 0.0 a 1.0
                hybridPlayer.setFadeVolume(progress)
                delay(stepDelay.milliseconds)
            }

            // Il fade è completato, impostiamo il massimo livello software REALE (1.0f)
            hybridPlayer.setFadeVolume(1.0f)
            isFading = false
            Timber.d("PlayerService: Fade In completato volume 1.0")
        }
    }


    private fun startExoToExoCrossfade() {
        fadeInJob?.cancel() // Cancelliamo eventuali fade-in residui

        serviceScope.launch(Dispatchers.Main) {
            val steps = 30 // Aumentiamo gli step per rendere la sfumatura ultra-morbida nelle cuffie

            // Recuperiamo la durata reale impostata (es. 7 secondi)
            val durationMs = appSettings.crossfadeDuration.milliseconds
            val stepDelay = durationMs / steps

            Timber.d("PlayerService: Avvio Fade Out Exo su base di $durationMs ms")

            for (i in 1..steps) {
                // Sfumiamo da 1.0f a 0.0f
                val progress = 1f - (i.toFloat() / steps)

                // CURVA LOGARITMICA DI SICUREZZA (Opzionale, rende il fade infinitamente più morbido):
                // Invece di un calo lineare, usiamo una curva quadratica (progress * progress)
                // così l'orecchio percepisce la sfumatura fin dal primo millisecondo!
                hybridPlayer.setFadeVolume(progress * progress)

                delay(stepDelay.milliseconds)
            }

            // Garanzia di silenzio totale al termine dei 7 secondi
            hybridPlayer.setFadeVolume(0f)

            Timber.d("PlayerService: Fade Out Exo completato, lancio handlePlayNext()")
            handlePlayNext("PlayerService.startExoToExoCrossfade")
        }
    }

    private fun startWebViewFadeOut() {
        fadeInJob?.cancel()

        serviceScope.launch(Dispatchers.Main) {
            val steps = 30
            val durationMs = appSettings.crossfadeDuration.milliseconds
            val stepDelay = durationMs / steps

            Timber.d("PlayerService: Avvio Fade Out WebView su base di $durationMs ms")

            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setFadeVolume(progress * progress) // Curva morbida
                delay(stepDelay.milliseconds)
            }

            hybridPlayer.setFadeVolume(0f)

            Timber.d("PlayerService: Fade Out WebView completato, lancio handlePlayNext()")
            handlePlayNext("PlayerService.startWebViewFadeOut")
        }
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
            if (!::exoPlayer.isInitialized) {
                Timber.w("PlayerService updateWidgetState invocato ma il player non è ancora pronto. Salto l'aggiornamento.")
                return@launch
            }

            val isPlaying = _playerState.value.isPlaying
            val title = withContext(Dispatchers.Main) { cleanPrefix(exoPlayer.mediaMetadata.title.toString()) }
            val artist = withContext(Dispatchers.Main) { exoPlayer.mediaMetadata.artist.toString() }

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

        // Se savedEndTime è 0, significa che non c'era nessun timer attivo
        if (savedEndTime != 0L) {
            val currentTime = System.currentTimeMillis()
            val remainingMillis = savedEndTime - currentTime

            if (remainingMillis > 0) {
                Timber.d("PlayerService: Ripristino timer di spegnimento rilevato. Rimanenti: $remainingMillis ms")
                // Riavvia il timer usando l'estensione custom del serviceScope
                timerJob = serviceScope.timer(remainingMillis) {
                    binder.executeAutoCloseLogic()
                }
            } else {
                Timber.d("PlayerService: Timer scaduto mentre il servizio era spento. Chiudo ora.")
                binder.executeAutoCloseLogic()
            }
        }
    }

    suspend fun saveQueue() {
        if (!appSettings.persistentQueue) return

        val mediaItems: List<MediaItem>
        val mediaItemIndex: Int
        val mediaItemPosition: Long

        withContext(Dispatchers.Main) {
            mediaItems = exoPlayer.currentTimeline.mediaItems
            mediaItemIndex = exoPlayer.currentMediaItemIndex
            mediaItemPosition = if (currentSong.value?.isLocal == true) {
                exoPlayer.currentPosition
            } else {
                (youtubeCurrentSecond.value * 1000).toLong()
            }
        }

        if (mediaItems.isEmpty()) return

        withContext(Dispatchers.IO) {
            val queuedMediaItems = mediaItems.mapIndexed { index, mediaItem ->
                QueuedMediaItem(
                    mediaItem = mediaItem,
                    mediaId = mediaItem.mediaId,
                    position = if (index == mediaItemIndex) mediaItemPosition else -1,
                    idQueue = mediaItem.mediaMetadata.extras?.getLong("idQueue", defaultQueueId())
                )
            }

            if (queuedMediaItems.isEmpty()) return@withContext

            try {
                Database.clearQueuedMediaItems()
                queuedMediaItems.forEach { Database.insert(it) }

                Timber.d("PlayerService SaveQueue: Coda persistente salvata con successo su disco.")
            } catch (e: Exception) {
                Timber.e("PlayerService SaveQueue persistent error: ${e.message}")
            }
        }
    }


    @OptIn(UnstableApi::class)
    suspend fun loadQueue() {
        Timber.d("PlayerService LoadQueue: Avvio caricamento coda persistente asincrona")
        if (!appSettings.persistentQueue) return

        // 1. Eseguiamo la query pesante nel thread IO
        val queuedSongs = withContext(Dispatchers.IO) {
            clearOldEmptyQueuedMediaItems()
            try { queuedMediaItems() } catch (e: Exception) { emptyList() }
        }

        if (queuedSongs.isEmpty()) {
            Timber.d("PlayerService LoadQueue: Nessun brano salvato nel database.")
            return
        }

        val index = queuedSongs.indexOfFirst { (it.position ?: 0L) >= 0L }.coerceAtLeast(0)
        val queuedSong = queuedSongs[index]
        val position = if (queuedSong.mediaItem.isLocal) {
            queuedSong.position ?: C.TIME_UNSET
        } else {
            (queuedSong.position ?: 0L) / 1000
        }

        Timber.d("PlayerService LoadQueue: Dati DB pronti. Ripristino indice: $index, Posizione: $position")

        withContext(Dispatchers.Main) {
            val mappedItems = queuedSongs.map { mediaItem ->
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
            }

            exoPlayer.setMediaItems(mappedItems, index, if (queuedSong.mediaItem.isLocal) position else 0)
            exoPlayer.prepare()

            if (!queuedSong.mediaItem.isLocal) {
                val duration = try { appSettings.stateDuration } catch (e: Exception) { 0f }
                val mId = appSettings.stateMediaId

                playFromSecond = position.toFloat()
                _currentSecond.value = playFromSecond
                _currentDuration.value = if (queuedSong.mediaId == mId) duration else 0f
                _internalYouTubePlayer.value?.pause()
            }

            Timber.d("PlayerService LoadQueue: Ripristino della coda in ExoPlayer completato con successo.")
        }
    }


    private fun updateMusicVaultMediaItem(
        songId: String,
        fileName: String,
        thumbnailFileName: String
    ) {
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                val itemCount = exoPlayer.mediaItemCount
                for (i in 0 until itemCount) {
                    val mediaItem = exoPlayer.getMediaItemAt(i)
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
                            exoPlayer.replaceMediaItem(i, updatedMediaItem)
                            Timber.d("PlayerService replaceMediaItem done — new uri=${exoPlayer.getMediaItemAt(i).localConfiguration?.uri}")

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

                            exoPlayer.replaceMediaItem(i, updatedMediaItem)
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

        fun restoreDefaultVolume() {
            if (!_isServiceReady.value || !this@PlayerService::hybridPlayer.isInitialized) return
            if (isFading) return // Se sta facendo il fade, non sovrascrivere l'audio

            // Impostiamo il volume interno al massimo possibile,
            // l'utente avrà gestito il volume con i tasti fisici, quindi lasciamo al sistema il da farsi
            hybridPlayer.setVolume(1f)
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

        fun startAutoCloseTimer(delayMillis: Long) {
            if (timerJob != null) {
                Timber.d("PlayerService: startAutoCloseTimer ignorato, timer già in esecuzione")
                return
            }

            timerJob?.cancel()

            // Calcoliamo e salviamo il timestamp esatto di fine
            val endTime = System.currentTimeMillis() + delayMillis
            serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(timerEndTime = endTime))
            }

            Timber.d("PlayerService startAutoCloseTimer delayMillis $delayMillis, pianificato per timestamp: $endTime")

            timerJob = serviceScope.timer(delayMillis) {
                Timber.d("PlayerService: Timer multiuso terminato naturalmente")
                executeAutoCloseLogic()
            }
        }

        fun executeAutoCloseLogic() {
            serviceScope.launch(Dispatchers.Main) {

                val wasPlaying = _playerState.value.isPlaying

                // Fade out morbido
                if (wasPlaying && !isFading) {
                    isFading = true
                    val steps = 15
                    val stepDelay = 2000 / steps
                    for (i in 1..steps) {
                        val progress = 1f - (i.toFloat() / steps)
                        hybridPlayer.setFadeVolume(progress)
                        delay(stepDelay.milliseconds)
                    }
                }

                hybridPlayer.pause()
                isFading = false

                // SALVATAGGIOnSUL DB PRIMA DI DISTRUGGERE I THREAD
                withContext(Dispatchers.IO) {
                    saveQueue()
                    appSettingsManager.updateSettings(appSettings.copy(timerEndTime = 0L))
                }

                // COSTRUZIONE NOTIFICA DI CORTESIA
                // In futuro potrò gestire una notifica diversa in base allo stato del player
                val titleStr = getString(R.string.notify_title_auto_close_timer_finished)
                val textStr = getString(R.string.notify_message_the_app_has_been_closed_automatically)
//                val titleStr = if (wasPlaying) getString(R.string.notify_title_auto_close_timer_finished)  else "App closed to save battery"
//                val textStr = if (wasPlaying) getString(R.string.notify_message_the_app_has_been_closed_automatically) else "The background service was stopped because playback was paused."

                val courtesyNotification = NotificationCompat
                    .Builder(this@PlayerService, SLEEPTIMER_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(titleStr)
                    .setContentText(textStr)
                    .setSmallIcon(R.drawable.app_icon)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                // MOSTRIAMO LA NOTIFICA DI CORTESIA E SMANTELLIAMO IL FOREGROUND
                if (isAtLeastAndroid7) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                notificationManager?.notify(SLEEPTIMER_NOTIFICATION_ID, courtesyNotification)

                // Diamo 300ms ad Android per stampare la notifica visivamente sul display
                delay(300.milliseconds)

                // COMANDO UNICO DI SMANTELLAMENTO
                Timber.d("PlayerService: Lancio lo stopSelf() reale della catena")
                stopSelf() // Qui viene chiamato onDestroy

                // Rimuoviamo tutte le attività in background
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                activityManager?.appTasks?.forEach { it.finishAndRemoveTask() }

                // Salvagente finale protetto per non lasciare limbo in background
                delay(200.milliseconds)
                Timber.d("PlayerService: KillProcess di sicurezza finale")
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }



        fun cancelAutoCloseTimer() {
            Timber.d("PlayerService cancelAutoCloseTimer")
            timerJob?.cancel()
            timerJob = null
            // Resettiamo il valore nel database/impostazioni per evitare ripristini errati al riavvio
            serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(timerEndTime = 0L))
            }
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
                }

            }

        }

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        fun toggleShuffle() {
            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    hybridPlayer.shuffleModeEnabled.let { hybridPlayer.shuffleModeEnabled = !it }
                }
            }

        }

        fun toggleRepeat() {
            val queueLoopType = appSettings.queueLoopType
            val newQueueLoopType = setQueueLoopState(queueLoopType)
            val repeatMode = newQueueLoopType.type
            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    hybridPlayer.repeatMode = repeatMode
                }
                val new = appSettings.copy(
                    queueLoopType = newQueueLoopType
                )
                appSettingsManager.updateSettings(new)

            }
        }

        fun actionSearch() {
            startActivity(Intent(applicationContext, MainActivity::class.java)
                .setAction(MainActivity.action_search)
                .setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK))
        }

        suspend fun loadQueue() = this@PlayerService.loadQueue()

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

    fun handlePlayNext(origine: String? = null) {
        Timber.d("PlayerService PlaybackWatchdog: handlePlayNext cambio brano richiesto da $origine")

        // Prima di cambiare brano, azzeriamo la variabile storica nel Service
        // così il Watchdog sa che la nuova traccia deve ricominciare a fare i calcoli da zero!
        lastWatchdogPosition = -1L
        //isFading = false

        val now = System.currentTimeMillis()
        if (now - lastPlayNextTime < debounceDelayMs) {
            Timber.d("PlayerService handlePlayNext ignored (too fast) play current")
            isFading = false
            hybridPlayer.play()
            return
        }
        lastPlayNextTime = now
        Timber.d("PlayerService handlePlayNext executed")

        playFromSecond = 0f

        // Resettiamo lo stato di fade prima di lanciare la nuova canzone,
        // così onMediaItemTransition e startFadeIn()
        //isFading = false

        serviceScope.launch {
            withContext(Dispatchers.Main) {
                // USO IL PLAYER REALE SOTTOSTANTE (exoPlayer)
                // per eseguire il vero salto atomico nella timeline di Media3,
                // bypassando l'override di HybridPlayer ed evitando il loop!
                exoPlayer.seekToNextMediaItem()
            }
        }
    }

    fun handlePlayPrevious() {
        val now = System.currentTimeMillis()
        if (now - lastPlayPreviousTime < debounceDelayMs) {
            Timber.d("PlayerService handlePlayPrevious: Click filtrato (troppo veloce)")
            return
        }
        lastPlayPreviousTime = now

        hybridPlayer.pause()

        // Prima di cambiare brano, azzeriamo la variabile storica nel Service
        // così il Watchdog sa che la nuova traccia deve ricominciare a fare i calcoli da zero!
        lastWatchdogPosition = -1L
        isFading = false

        val currentPos = hybridPlayer.currentPosition
        val rewindThreshold = appSettings.rewindThresholdDuration.milliSeconds

        // Se rewindThreshold > 0 ed il brano è avviato da più di n secondi, ricomincia da capo (comportamento standard)
        if (rewindThreshold in 1..<currentPos) {
            Timber.d("PlayerService: Brano oltre i ${rewindThreshold / 1000} secondi, eseguo seekTo(0)")
            playFromSecond = 0f
            hybridPlayer.seekTo(0)
            hybridPlayer.play()
        } else {
            // 2. Se siamo sotto i 3 secondi, forziamo il salto REALE al brano precedente
            val currentIndex = exoPlayer.currentMediaItemIndex

            if (currentIndex > 0) {
                // C'è un brano precedente nella coda! Calcoliamo l'indice esatto
                val targetPreviousIndex = currentIndex - 1
                Timber.d("PlayerService: Salto forzato al brano precedente con indice: $targetPreviousIndex")

                playFromSecond = 0f
                serviceScope.launch(Dispatchers.Main) {
                    // Usiamo il comando atomico che forza ExoPlayer ad andare all'indice desiderato,
                    // aggirando qualsiasi problema di validazione dell'URI o della timeline!
                    exoPlayer.seekToDefaultPosition(targetPreviousIndex)
                }
            } else {
                // Se l'indice è 0, siamo davvero all'inizio della playlist. Ricominciamo da capo.
                Timber.d("PlayerService: Siamo al primo brano della coda, resetto a 0")
                playFromSecond = 0f
                hybridPlayer.seekTo(0)
                hybridPlayer.play()
            }
        }
    }

    fun handlePlayNextRequestedByUser(source: String) {
        // Alziamo SUBITO il semaforo prima di fare qualsiasi controllo.
        // Questo congela istantaneamente i controlli del Watchdog supremo perenne!
        isFading = true
        //hybridPlayer.setFadeVolume(0f)


        // Se l'app non sta suonando o è in pausa, cambiamo subito senza aspettare i 250ms
        if (!_playerState.value.isPlaying) {
            isFading = false // Resettiamo prima di cambiare traccia
            handlePlayNext(source)
            return
        }

        // Avviamo la dissolvenza lampo sul Thread Principale
        serviceScope.launch(Dispatchers.Main) {
            Timber.d("PlayerService: Svuotamento buffer in corso per $source (volume silenziato)")

            hybridPlayer.setFadeVolume(0f)
            //delay(150.milliseconds) // Tempo sufficiente per non far sentire il pop, ma invisibile per l'utente

            val steps = 6
            val quickFadeDurationMs = 250
            val stepDelay = quickFadeDurationMs / steps

            Timber.d("PlayerService: Quick Fade-Out UTENTE (Avanti) avviato da $source")

            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setFadeVolume(progress * progress)
                delay(stepDelay.milliseconds)
            }

            // Garantiamo il silenzio assoluto prima dello switch hardware
            hybridPlayer.setFadeVolume(0f)

            // Mettiamo a false un istante prima di muovere la timeline,
            // così il brano successivo partirà con la lavagna pulita
            //isFading = false

            // Lanciamo finalmente il cambio canzone reale
            handlePlayNext(source)
        }
    }

    fun handlePlayPreviousRequestedByUser(source: String) {
        // Alziamo subito lo scudo protettivo contro il Watchdog perenne
        isFading = true

        if (!_playerState.value.isPlaying) {
            isFading = false
            handlePlayPrevious()
            return
        }

        // Dissolvenza lampo simmetrica per il tasto "Indietro"
        serviceScope.launch(Dispatchers.Main) {
            val steps = 6
            val quickFadeDurationMs = 250
            val stepDelay = quickFadeDurationMs / steps

            Timber.d("PlayerService: Quick Fade-Out UTENTE (Indietro) avviato da $source")

            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setFadeVolume(progress * progress)
                delay(stepDelay.milliseconds)
            }

            hybridPlayer.setFadeVolume(0f)
            isFading = false

            // Torniamo alla traccia precedente
            handlePlayPrevious()
        }
    }


    fun requestSmoothPause() {
        // Se non sta riproducendo o sta già sfumando, applichiamo la pausa hardware diretta
        if (!_playerState.value.isPlaying || isFading) {
            hybridPlayer.executeActualPause()
            return
        }

        serviceScope.launch(Dispatchers.Main) {
            isFading = true
            val steps = 5
            val fadeDurationMs = 200 // 200ms sono impercettibili come ritardo ma perfetti per l'orecchio
            val stepDelay = fadeDurationMs / steps

            Timber.d("PlayerService UX: Avvio Fade-Out lampo per PAUSA")

            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setFadeVolume(progress * progress) // Curva morbida
                delay(stepDelay.milliseconds)
            }

            hybridPlayer.setFadeVolume(0f)
            isFading = false

            // Il volume è a zero: ora eseguiamo la vera logica di pausa
            hybridPlayer.executeActualPause()
            Timber.d("PlayerService UX: Pausa hardware completata")
        }
    }

    fun requestSmoothPlay() {
        // Se sta già riproducendo, non facciamo nulla
        if (_playerState.value.isPlaying) return

        serviceScope.launch(Dispatchers.Main) {
            // 1. Azzeriamo il volume software PRIMA di avviare i motori audio
            hybridPlayer.setFadeVolume(0f)

            // 2. Facciamo partire il player (Exo o YT) che inizierà a emettere audio ma in silenzio
            hybridPlayer.executeActualPlay()
            Timber.d("PlayerService UX: Play hardware eseguito, avvio Fade-In lampo")

            isFading = true
            val steps = 5
            val fadeDurationMs = 250 // Un filo più lungo in risalita per un effetto avvolgente
            val stepDelay = fadeDurationMs / steps

            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                hybridPlayer.setFadeVolume(progress)
                delay(stepDelay.milliseconds)
            }

            // Apertura totale al 100% software: il controllo torna ai tasti fisici del telefono
            hybridPlayer.setFadeVolume(1.0f)
            isFading = false
            Timber.d("PlayerService UX: Riproduzione tornata a volume pieno")
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

