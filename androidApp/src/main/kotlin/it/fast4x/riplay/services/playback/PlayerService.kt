package it.fast4x.riplay.services.playback

import it.fast4x.riplay.data.insert
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
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.core.app.NotificationCompat
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
import it.fast4x.environment.utils.fromMusicShelfRenderer
import it.fast4x.riplay.data.mediaItemCompat
import it.fast4x.riplay.data.toMediaItemBytes
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.extensions.alarm.ACTION_ALARM_SNOOZE
import it.fast4x.riplay.extensions.alarm.ACTION_ALARM_STOP
import it.fast4x.riplay.extensions.alarm.AlarmItem
import it.fast4x.riplay.extensions.alarm.AlarmReceiver
import it.fast4x.riplay.extensions.alarm.AlarmServiceActions
import it.fast4x.riplay.extensions.alarm.EXTRA_ALARM_ID
import it.fast4x.riplay.extensions.alarm.loadAlarmOptions
import it.fast4x.riplay.extensions.alarm.loadAlarms
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
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.UnplayableTrack
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.enums.CrossfadeDuration
import it.fast4x.riplay.enums.LoadPhase
import it.fast4x.riplay.enums.PlaybackOrigin
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.appforegroundtracker.AppForegroundTracker
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
import it.fast4x.riplay.services.lyricsoverlay.LyricsOverlayManager
import it.fast4x.riplay.ui.widgets.LikeState
import it.fast4x.riplay.ui.widgets.PlayerWidgetSnapshot
import it.fast4x.riplay.ui.widgets.WidgetArtworkCache
import it.fast4x.riplay.ui.widgets.WidgetUpdater
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.utils.mediaItems
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import java.io.File
import java.util.Objects
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import android.os.Binder as AndroidBinder
import it.fast4x.riplay.extensions.appsettings.AppSettingsManager
import it.fast4x.riplay.extensions.webdavlibrary.DynamicWebDavAuthInterceptor
import it.fast4x.riplay.services.playback.common.PlaybackContext
import it.fast4x.riplay.services.playback.common.PlaybackState
import it.fast4x.riplay.services.playback.common.PlayerState
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.utils.BitmapLoader
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getDeviceVolume
import it.fast4x.riplay.utils.isWebDav
import it.fast4x.riplay.utils.removeVideoMediaItems
import it.fast4x.riplay.utils.setQueueLoopState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Suppress("DEPRECATION")
class PlayerService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    OnAudioVolumeChangedListener
{
    val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("PlayerServiceScope")
    )
    var mediaLibrarySession: MediaLibrarySession? = null
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

    // ------------------------------------------------------------------
    // ALARM (Sveglia): job e stato della sessione sveglia corrente
    // ------------------------------------------------------------------
    private var alarmAutoStopJob: Job? = null
    private var alarmFadeInJob: Job? = null
    private var alarmSessionAlarmId: String? = null

    /**
     * ALARM (Sveglia): true quando la sessione sveglia ha AVUTO almeno un
     * istante di riproduzione effettiva. La chiusura automatica della sessione
     * ("riproduzione interrotta") è valida solo in quel caso: durante l'avvio
     * (buffering iniziale, bridge YouTube da riattivare con il servizio già
     * vivo in background) l'evento "not playing" è normale e chiudere la
     * sessione lì ucciderebbe la sveglia prima che il brano parta.
     */
    @Volatile
    private var alarmSessionHeardPlayback = false

    /**
     * True mentre la sveglia sta eseguendo il fade-in del volume:
     * inibisce i reset di volume del crossfade/ripristino che
     * interromperebbero la rampa graduale.
     */
    @Volatile
    var alarmFadeInProgress = false
        private set

    /**
     * Segnala che l'eventuale ripristino della coda persistente è terminato.
     * La sveglia attende questo segnale prima di sostituire la coda, così da
     * non essere sovrascritta dal caricamento della vecchia coda all'avvio a freddo.
     */
    private val _queueSetupCompleted = MutableStateFlow(false)
    val queueSetupCompleted: StateFlow<Boolean> = _queueSetupCompleted.asStateFlow()

    /**
     * ALARM (Sveglia): brano online da caricare appena la WebView YouTube sarà
     * pronta (onReady). Serve quando la sveglia scatta a app chiusa: la WebView
     * viene creata in modo asincrono e un loadVideo anticipato sarebbe un no-op
     * silenzioso. Consumato in initializeOnlinePlayer().onReady.
     */
    @Volatile
    private var pendingAlarmLoadVideoId: String? = null

    private var radio: OnlineRadio? = null

    var bitmapProvider: BitmapProvider? = null

    private var volumeNormalizationJob: Job? = null
    private var endedObserverJob: Job? = null
    private lateinit var audioManager: AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

    // Forse richiesto su dispositivi auto legacy anche se media3 gestisce anche il pregresso
    var legacyActionReceiver: LegacyActionReceiver? = null


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
        .stateIn(this@PlayerService.serviceScope, SharingStarted.WhileSubscribed(5000), null)

    lateinit var sleepTimerListener: SleepTimerListener

    /**
     * Online configuration
     */
    private val _internalYouTubePlayerView = MutableStateFlow<YouTubePlayerView?>(null)
    val internalYoutubePlayerView: StateFlow<YouTubePlayerView?> = _internalYouTubePlayerView

    val _internalYouTubePlayer = MutableStateFlow<YouTubePlayer?>(null)

    private val _internalBufferedFraction = MutableStateFlow(0f)
    val internalYoutubeBufferedFraction: StateFlow<Float> = _internalBufferedFraction

    var _currentSecond = MutableStateFlow(0f)
    var youtubeCurrentSecond: StateFlow<Float> = _currentSecond

    var _currentDuration = MutableStateFlow(0f)
    var youtubeCurrentDuration: StateFlow<Float> = _currentDuration

    var load = true
    @Volatile private var playFromSecond = 0f
    //var lastError: PlayerConstants.PlayerError? = null

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

    var firstTimeStarted = true

    private val riTuneCastClient: RiTuneCastClient = RiTuneCastClient()
    private var riTuneObserverJob: Job? = null

    private val equalizerHelper: EqualizerHelper by lazy { EqualizerHelper(this) }

//    private val globalQueue: GlobalQueueViewModel by lazy {
//        ViewModelProvider(AppSharedScope)[GlobalQueueViewModel::class.java]
//    }

    //private var unstartedWatchdogJob: Job? = null

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
    val appSettingsManager: AppSettingsManager by lazy {
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

                    // Pipeline widget: a schermo spento è congelata, la riattivo
                    // e allineo subito i widget allo stato più recente.
                    runCatching { WidgetUpdater.setScreenInteractive(true) }
                        .onFailure { Timber.e("PlayerService Screenreceiver: riattivazione widget fallita: ${it.message}") }

                    if (!isFading) {
                        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
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

                Intent.ACTION_SCREEN_OFF -> {
                    Timber.d("PlayerService Screenreceiver: Schermo spento, congelamento pipeline widget")
                    runCatching { WidgetUpdater.setScreenInteractive(false) }
                        .onFailure { Timber.e("PlayerService Screenreceiver: congelamento widget fallito: ${it.message}") }
                }
            }
        }
    }

    private val myMainHandler = Handler(Looper.getMainLooper())

    // Observer per il ciclo di vita dell'intero processo (app in background)
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // Chiamato quando l'app va in background o lo schermo si spegne se l'utente ha impostato l'esclusione dei video
            // Elimino i video perchè in background nella webview non sono più visibili ma creano problemi di avanzamento al successivo mediaitem
            if (appSettings.videoContentMode.excluded) {
                Timber.d("PlayerService: ProcessLifecycleOwner.onStop() schermo spento o app in secondo piano, rimuovo video")
                hybridPlayer.removeVideoMediaItems()
            }
        }
    }

    private val _isLoadingRadio = MutableStateFlow(false)
    val isLoadingRadio: StateFlow<Boolean> = _isLoadingRadio.asStateFlow()

    @Volatile private var resumePending = false
    @Volatile var userInitiatedPause = false

    // Istante dell'ultimo comando di pausa DAVVERO richiesto dall'utente.
    // userInitiatedPause da solo non basta: viene azzerato solo quando arriva
    // l'evento PAUSED, ma quella conferma può non arrivare MAI (pausa durante
    // il buffering, WebView assente, iframe morto). Senza il timestamp un flag
    // "stale" fa scambiare l'ECO della pausa di connettività per una pausa
    // dell'utente, annullando l'auto-ripresa (bug verificato in test).
    @Volatile private var userPauseRequestedAtMs = 0L

    /** Registra un comando di pausa proveniente DAVVERO dall'utente (UI, notifica, media button). */
    fun markUserPauseRequested() {
        userInitiatedPause = true
        userPauseRequestedAtMs = System.currentTimeMillis()
    }

    /**
     * La volontà di pausa dell'utente è ancora attribuibile all'evento PAUSED
     * in corso? Solo un comando di pausa arrivato da pochissimo (vedi
     * USER_PAUSE_EVENT_WINDOW_MS) può essere la causa di un PAUSED.
     */
    private fun isUserPauseIntentFresh(): Boolean {
        if (!userInitiatedPause) return false
        val age = System.currentTimeMillis() - userPauseRequestedAtMs
        return age in 0..USER_PAUSE_EVENT_WINDOW_MS
    }

    /** Azzera l'intenzione di pausa dell'utente (consumata o superata da un play/skip). */
    private fun clearUserPauseIntent() {
        userInitiatedPause = false
        userPauseRequestedAtMs = 0L
    }

    // ═══════════ GESTIONE CONNETTIVITÀ ONLINE (motore AYP) ═══════════
    // Monitor robusto della rete + policy di pausa/ripresa/stallo.
    // La policy vive qui perché è l'unico proprietario dello stato del player ibrido.
    val onlineConnectivityMonitor by lazy { OnlineConnectivityMonitor(this) }
    private var connectivityListener: OnlineConnectivityMonitor.Listener? = null
    private var networkGraceJob: Job? = null          // finestra di grazia sull'handover WiFi<->Cellulare
    private var connectivityResumeJob: Job? = null    // ripresa post-ripristino con stabilizzazione
    private var onlineStallWatchdogJob: Job? = null   // watchdog anti-stallo del motore online

    // true = siamo stati NOI a mettere in pausa per assenza di rete
    // (bypassa YouTubeControlImpl, quindi NON alza userInitiatedPause)
    @Volatile private var pausedForConnectivity = false

    // true = la riproduzione era attiva (o desiderata) quando la rete è caduta:
    // al ritorno della rete va ripresa automaticamente
    @Volatile private var resumeDesiredAfterConnectivity = false

    @Volatile private var lastNetworkLostAtMs = 0L
    @Volatile private var lastNetworkRestoredAtMs = 0L

    // Load di recovery in attesa che la WebView torni pronta (onReady)
    private var pendingRecoveryLoadVideoId: String? = null
    private var pendingRecoveryLoadSecond = 0f

    // Watchdog anti-stallo del motore online
    private var lastProgressSecond = -1f
    private var lastProgressAtMs = 0L
    private var onlineRecoveryLevel = 0               // 0=nessuno, 1=seek, 2=loadVideo, 3=ricrea WebView
    private var onlineRecoveryNextAtMs = 0L
    private var onlineRecoveryMediaId: String? = null

    private val NETWORK_LOSS_GRACE_MS = 2500L         // tolleranza handover WiFi<->Cellulare
    private val NETWORK_STABILIZE_MS = 1200L          // attesa post-ritorno rete prima della ripresa
    private val USER_PAUSE_EVENT_WINDOW_MS = 2000L    // un PAUSED è attribuibile all'utente solo se il comando è più recente di così
    private val TRANSIENT_ERROR_WINDOW_MS = 15000L    // finestra in cui un HTML_5_PLAYER è considerato transitorio
    private val ONLINE_STALL_THRESHOLD_MS = 12000L    // posizione ferma oltre questa soglia = stallo
    private val ONLINE_RECOVERY_STEP_DELAY_MS = 8000L // attesa tra un livello e il successivo di escalation
    private val ONLINE_RECREATE_STEP_DELAY_MS = 12000L
    // ═════════════════════════════════════════════════════════════════

    private var eqSetupDone = false

    private var consecutivePermanentErrors = 0

    val unplayableIds: StateFlow<Set<String>> =
        Database.unplayableTracksDao().observeAllIds()
            .map { it.toSet() }
            .stateIn(serviceScope, SharingStarted.Eagerly, emptySet())




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

        // Carico le impostazioni prima di tutto.
        // ROBUSTEZZA AVVIO (fondamentale su Android TV): ogni inizializzazione
        // "periferica" è isolata con runCatching. Su alcuni TV box mancano
        // sensori, HAL audio particolari o componenti di sistema: un errore
        // qui non deve MAI bloccare l'avvio dell'app né impedire che il
        // servizio dichiari "ready" (altrimenti l'app resta sul loader per
        // sempre o — peggio — il processo muore e dal launcher l'app
        // "lampeggia" a ciclo infinito).
        runCatching { loadInitialSettingsFromDatabase() }
            .onFailure { Timber.e("PlayerService.onCreate loadInitialSettingsFromDatabase failed: $it") }

        // Inizializzo l'hardware audio e la sessione Media3
        runCatching { initializeBitmapProvider() }
            .onFailure { Timber.e("PlayerService.onCreate initializeBitmapProvider failed: $it") }
        // Cuore del player: volutamente NON isolato, senza questo l'app non
        // può funzionare ed è meglio un crash esplicito che uno stato impair.
        initializeHybridPlayerAndSession()
        runCatching { initializeMediaItemState() }
            .onFailure { Timber.e("PlayerService.onCreate initializeMediaItemState failed: $it") }

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

        // Lancio tutto il resto delle configurazioni in background senza bloccare l'avvio nativo.
        // ROBUSTEZZA AVVIO:
        //  - CoroutineExceptionHandler: un'eccezione non deve mai uccidere il processo
        //    (su Android TV il sintomo è l'app che "lampeggia" dal launcher);
        //  - ogni step è isolato con runCatching: un errore non salta gli step successivi;
        //  - _isServiceReady passa a true nel finally: l'Activity sblocca SEMPRE
        //    il loader, anche se qualche inizializzazione è fallita.
        val startupExceptionHandler = CoroutineExceptionHandler { _, e ->
            Timber.e("PlayerService.onCreate: eccezione durante l'inizializzazione: $e")
        }
        this@PlayerService.serviceScope.launch(Dispatchers.Main + startupExceptionHandler) {
            try {
                runCatching { startObservingSettings() }
                    .onFailure { Timber.e("PlayerService startup startObservingSettings failed: $it") }

                runCatching { startPlaybackWatchdog() }
                    .onFailure { Timber.e("PlayerService startup startPlaybackWatchdog failed: $it") }

                // Osservazione della rete + policy di pausa/ripresa/stallo per il motore online
                runCatching { initializeOnlineConnectivityHandling() }
                    .onFailure { Timber.e("PlayerService startup initializeOnlineConnectivityHandling failed: $it") }

                runCatching { checkAndRestoreTimer() }
                    .onFailure { Timber.e("PlayerService startup checkAndRestoreTimer failed: $it") }

                runCatching { initializeAudioManager() }
                    .onFailure { Timber.e("PlayerService startup initializeAudioManager failed: $it") }
                runCatching { initializeAudioVolumeObserver() }
                    .onFailure { Timber.e("PlayerService startup initializeAudioVolumeObserver failed: $it") }

                runCatching { initializeLegacyNotificationActionReceiver() }
                    .onFailure { Timber.e("PlayerService startup initializeLegacyNotificationActionReceiver failed: $it") }
                runCatching { initializeWidgetUpdater() }
                    .onFailure { Timber.e("PlayerService startup initializeWidgetUpdater failed: $it") }

                // Overlay testi fluttuante: la finestra viene mostrata/nascosta
                // osservando preferenza, permesso, PiP e brano corrente
                runCatching { LyricsOverlayManager.initialize(this@PlayerService) }
                    .onFailure { Timber.e("PlayerService startup lyrics overlay failed: $it") }
                runCatching { initializeAudioDeviceCallback() }
                    .onFailure { Timber.e("PlayerService startup initializeAudioDeviceCallback failed: $it") }

                // Ora che la webview e l'hybrid player sono stabili, calcolo il volume di normalizzazione
                runCatching { initializeNormalizeVolume() }
                    .onFailure { Timber.e("PlayerService startup initializeNormalizeVolume failed: $it") }
                runCatching { initializeBassBoost() }
                    .onFailure { Timber.e("PlayerService startup initializeBassBoost failed: $it") }
                runCatching { initializeReverb() }
                    .onFailure { Timber.e("PlayerService startup initializeReverb failed: $it") }

                runCatching { initializeSensorListener() }
                    .onFailure { Timber.e("PlayerService startup initializeSensorListener failed: $it") }
                runCatching { initializeSongCoverInLockScreen() }
                    .onFailure { Timber.e("PlayerService startup initializeSongCoverInLockScreen failed: $it") }
                runCatching { initializeMedleyMode() }
                    .onFailure { Timber.e("PlayerService startup initializeMedleyMode failed: $it") }
                runCatching { applyPlaybackParameters() }
                    .onFailure { Timber.e("PlayerService startup applyPlaybackParameters failed: $it") }
                runCatching { initializeAudioDRCHelper() }
                    .onFailure { Timber.e("PlayerService startup initializeAudioDRCHelper failed: $it") }

                runCatching { initializeRiTune() }
                    .onFailure { Timber.e("PlayerService startup initializeRiTune failed: $it") }
                runCatching { initializeDiscordPresence() }
                    .onFailure { Timber.e("PlayerService startup initializeDiscordPresence failed: $it") }

                runCatching { setupPersistentQueueAndObservers() }
                    .onFailure { Timber.e("PlayerService startup setupPersistentQueueAndObservers failed: $it") }

                runCatching {
                    // SCREEN_ON: refresh volume + riattivazione pipeline widget.
                    // SCREEN_OFF: widget invisibili, ogni scrittura widget viene congelata.
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                    registerReceiver(screenReceiver, filter)
                }.onFailure { Timber.e("PlayerService startup registerReceiver screenReceiver failed: $it") }

                runCatching {
                    // Registra l'observer sul ciclo di vita del processo
                    ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
                }.onFailure { Timber.e("PlayerService startup ProcessLifecycleOwner observer failed: $it") }

                // Listener di appoggio per la ripresa della riproduzione al termine di una chiamata
                runCatching {
                    serviceScope.launch {
                        var recoveryAttempts = 0
                        while (isActive) {
                            delay(2000.milliseconds)
                            if (!resumePending || hybridPlayer.activeEngine != ActiveEngine.YOUTUBE) continue
                            if (audioManager.mode != AudioManager.MODE_NORMAL) continue   // chiamata ancora attiva
                            if (recoveryAttempts++ > 5) { resumePending = false; recoveryAttempts = 0; continue }
                            Timber.d("PlayerService YT: recovery playVideo() (tentativo $recoveryAttempts)")
                            _internalYouTubePlayer.value?.play()
                        }
                    }
                }.onFailure { Timber.e("PlayerService startup call-recovery loop failed: $it") }
            } finally {
                // SEMPRE, anche se uno degli step è fallito: l'Activity (soprattutto
                // su Android TV, dove il loader attende questo segnale) deve sbloccare.
                _isServiceReady.value = true
            }
        }
    }

    private fun startForegroundPlaceholder() {
        // Se l'app è già in foreground non facciamo nulla
        if (AppForegroundTracker.isAppInForeground) return

        // Questo metodo viene chiamato anche a schermo spento
        // (app in background) durante la transizione al brano online. Su Android 12+,
        // se il servizio era stato degradato da Media3, startForeground può lanciare
        // ForegroundServiceStartNotAllowedException: un'eccezione qui ucciderebbe
        // la catena dei listener del player e fermerebbe la musica. Un fallimento
        // del placeholder è recuperabile (Media3 riprova con la sua notifica),
        // quindi viene solo registrato.
        runCatching {
            // Media3 dalla versione 1.11.1 ha probabilmente un bug con il foreground service automatico
            // gestito dal suo default notification media provider
            // Molto spesso non richiede il permesso di foreground service per far funzionare correttamente il playback
            // Con questo metodo ci assicuriamo che venga richiesto il permesso di foreground service
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.riplay_app_name))
                .setSmallIcon(R.drawable.app_icon)
                .setOngoing(true)
                .build()
            if (isAtLeastAndroid10) {
                startForeground(
                    NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            Timber.d("PlayerService FGS placeholder avviato prima del playback")
        }.onFailure {
            Timber.e("PlayerService FGS placeholder rifiutato dal sistema (playback non influenzato): ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    @kotlin.OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
    private fun setupPersistentQueueAndObservers() {
        if (appSettings.persistentQueue) {
            this@PlayerService.serviceScope.launch {
                // Caricamento iniziale obbligatorio sul Main thread per ExoPlayer
                withContext(Dispatchers.Main) {
                    loadQueue()
                    resumePlaybackOnStart()
                }

                // Il ripristino della coda è concluso: la sveglia può subentrare in sicurezza
                _queueSetupCompleted.value = true

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
            this@PlayerService.serviceScope.launch(Dispatchers.IO) {
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
        } else {
            // Coda persistente disattivata: nessun ripristino, la sveglia non deve attendere
            _queueSetupCompleted.value = true
        }

        this@PlayerService.serviceScope.launch {
            currentSong.collect { song ->
                if (song == null) return@collect
                if (currentMediaItemState.value?.mediaId != song.id) return@collect

                Timber.d("PlayerService onCreate update currentSong $song")

                val currentMediaId = if (!song.isLocal) song.id else song.mediaId ?: song.id

                if (lastOnlineMediaId != currentMediaId && onlineListenedDurationMs > 0) {
                    Timber.d("PlayerService incrementOnlineListenedPlaytimeMs update currentSong onlineListenedDurationMs = $onlineListenedDurationMs onlineMediaId = $currentMediaId currentMediaId = $currentMediaId")
                    incrementOnlineListenedPlaytimeMs()
                    delay(200.milliseconds)
                    onlineListenedDurationMs = 0L
                    //lastOnlineMediaId = currentMediaId // non qui
                }

                // Recuperiamo alcuni metadata utili per la riproduzione
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
        this@PlayerService.serviceScope.launch(Dispatchers.IO) {
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

        // Gestisce gli eventi di MediaVault
        this@PlayerService.serviceScope.launch {
            MusicVaultEvents.events.collect { event ->
                when (event) {
                    is MusicVaultEvent.Completed -> {
                        updateMusicVaultMediaItem(
                            songId = event.songId,
                            fileName = event.fileName,
                            thumbnailFileName = event.thumbnailFileName
                        )
                    }

                    is MusicVaultEvent.Removed -> {
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
        // MAI degradare ciò che media3 richiede; solo potenziare.
        // Il foreground serve anche DURANTE il load, se l'intento è di suonare:
        val state = _playerState.value.playbackState
        val wantsForeground = hybridPlayer.playWhenReady &&
                state != PlaybackState.IDLE //&& state != PlaybackState.ENDED
//                hybridPlayer.playbackState != Player.STATE_IDLE &&
//                hybridPlayer.playbackState != Player.STATE_ENDED

        super.onUpdateNotification(session, startInForegroundRequired || wantsForeground)
    }

//    @UnstableApi
//    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
//        // Forziamo startInForegroundRequired a true quando il player è effettivamente in riproduzione
//        // Questo risolve i bug legati alle notifiche invisibili o "congelate" nel system_server
//        val forceForeground = hybridPlayer.isPlaying || startInForegroundRequired
//        super.onUpdateNotification(session, forceForeground)
//    }


    fun loadInitialSettingsFromDatabase(){
        // Devo essere sicuro che le impostazioni siano pronte
        appSettings = runBlocking(Dispatchers.IO) {
            appSettingsManager.waitForInitialization()
        }
    }

    private fun startObservingSettings() {
        settingsObserverJob = this@PlayerService.serviceScope.launch(Dispatchers.Main) {
            appSettingsManager.activeSettings      
                .collect { settings -> 
                    //Timber.d("PlayerService: impostazioni cambiate $settings")

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("PlayerService onGetSession controllerInfo = $controllerInfo mediaLibrarySession = $mediaLibrarySession")

        return mediaLibrarySession
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        super.onStartCommand(intent, flags, startId)

        Timber.d("PlayerService onStartCommand intent action ${intent?.action}")
        when (intent?.action) {
            Action.play.value -> {
                hybridPlayer.play()
                //if (currentSong.value?.isLocal == true) exoPlayer.play() else _internalYouTubePlayer.value?.play()
            }
            Action.pause.value -> {
                markUserPauseRequested()
                hybridPlayer.pause()
               // if (currentSong.value?.isLocal == true) exoPlayer.pause() else _internalYouTubePlayer.value?.pause()
            }
            Action.next.value -> handlePlayNextRequestedByUser("PlayerService.onStartCommand")
            Action.previous.value -> handlePlayPreviousRequestedByUser("PlayerService.onStartCommand")
            AlarmServiceActions.PLAY -> handleAlarmPlay(intent)
            AlarmServiceActions.STOP -> handleAlarmStop()
        }
        updateWidgetState()

        return START_STICKY
    }

    // ------------------------------------------------------------------
    // ALARM (Sveglia): avvio riproduzione, fade-in, auto-stop, notifica
    // ------------------------------------------------------------------

    private fun handleAlarmPlay(intent: Intent?) {
        val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId == null) {
            Timber.w("PlayerService ALARM: azione alarmplay senza alarmId")
            return
        }

        Timber.i("PlayerService ALARM: avvio sveglia $alarmId")

        // ═══ FIX STABILITÀ SVEGLIA (1/3): foreground service IMMEDIATO ═══
        // Il receiver avvia il servizio con startForegroundService(): da quel
        // momento Android concede una finestra limitata (~5-10s) per chiamare
        // startForeground(), altrimenti UCCIDE il processo. Prima di questo fix
        // la promozione avveniva solo in startForegroundPlaceholder() dentro
        // onMediaItemTransition — ma: (a) per i brani LOCALI non veniva mai
        // chiamato; (b) con schermo spento e app aperta AppForegroundTracker
        // considera l'app "in foreground" (activity in pausa ma non stoppata)
        // e il check saltava la promozione; (c) all'avvio a freddo onCreate +
        // attesa coda persistente superavano la finestra. Risultato: notifica
        // visibile ma processo ucciso prima che la musica partisse.
        startForegroundForAlarm()

        // Marca la sessione sveglia SUBITO (sincrono): serve a onReady per NON
        // far ripartire in automatico la vecchia coda persistente mentre la
        // sveglia è in fase di avvio a freddo.
        alarmSessionAlarmId = alarmId
        alarmSessionHeardPlayback = false

        serviceScope.launch {
            // Attende l'eventuale ripristino della coda persistente per evitare
            // che il caricamento della vecchia coda sovrascriva la sveglia
            withTimeoutOrNull(8_000L) { _queueSetupCompleted.first { it } }

            val alarm = withContext(Dispatchers.IO) { loadAlarms().firstOrNull { it.id == alarmId } }
            if (alarm == null) {
                Timber.w("PlayerService ALARM: sveglia $alarmId non trovata")
                // Rimuove la notifica temporanea "Avvio sveglia…" della promozione FGS
                notificationManager?.cancel(ALARM_NOTIFICATION_ID)
                return@launch
            }

            val options = loadAlarmOptions()
            val mediaItems = buildAlarmMediaItems(alarm)

            if (mediaItems.isEmpty()) {
                Timber.w("PlayerService ALARM: sorgente musicale non disponibile per la sveglia $alarmId")
                postAlarmNotification(
                    alarm = alarm,
                    text = getString(R.string.alarm_source_missing),
                    snoozeMinutes = options.snoozeMinutes,
                    sourceMissing = true
                )
                return@launch
            }

            // ═══ FIX STABILITÀ SVEGLIA (2/3): WebView YouTube non ancora pronta ═══
            // L'avvio a freddo (app chiusa) crea la WebView in modo asincrono:
            // _internalYouTubePlayer diventa non-null solo nell'onReady dell'IFrame.
            // Un loadVideo chiamato prima è un NO-OP silenzioso -> notifica sì,
            // musica no. Attendiamo con grazia la prontezza e, se non arriva,
            // registriamo un load pendente che onReady eseguirà appena possibile.
            val firstItemIsOnline = mediaItems.first().isLocal == false
            if (firstItemIsOnline && _internalYouTubePlayer.value == null) {
                Timber.i("PlayerService ALARM: WebView YouTube non ancora pronta, attesa grazia...")
                val ready = withTimeoutOrNull(12_000L) { _internalYouTubePlayer.first { it != null } } != null
                if (!ready) {
                    pendingAlarmLoadVideoId = mediaItems.first().mediaId
                    Timber.w("PlayerService ALARM: WebView YouTube non pronta dopo 12s, load pendente registrato (${mediaItems.first().mediaId})")
                }
            }

            withContext(Dispatchers.Main) {
                runCatching {
                    alarmSessionAlarmId = alarmId
                    clearUserPauseIntent()
                    binder.stopRadio()

                    // ═══ FIX STABILITÀ SVEGLIA (3/3): nessuna esclusione silenziosa ═══
                    // forcePlay() esclude in silenzio i brani filtrati (video con
                    // "escludi video" attivo, blacklist, limite di durata): per una
                    // sveglia — scelta esplicita dell'utente — non deve mai restare
                    // in silenzio, quindi la riproduzione avviene direttamente.
                    if (mediaItems.size == 1) {
                        exoPlayer.setMediaItem(mediaItems.first(), true)
                    } else {
                        val list = if (alarm.shuffle) mediaItems.shuffled() else mediaItems
                        exoPlayer.shuffleModeEnabled = false
                        exoPlayer.setMediaItems(list, 0, C.TIME_UNSET)
                    }
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()

                    // ═══ FIX SVEGLIA CON SERVIZIO GIÀ VIVO IN BACKGROUND ═══
                    // Se il servizio esisteva già (processo NON ricreato
                    // dall'allarme), il bridge YouTube è pronto ma fermo nello
                    // stato di pausa della sessione precedente:
                    // onMediaItemTransition emette il loadVideo però poi mette in
                    // pausa exoPlayer e il bridge non riceve mai il comando di
                    // play (sintomo: brano caricato, musica muta). Come già fa
                    // l'avvio a freddo (load pendente in onReady), appena la
                    // transizione è assestata commandiamo l'avvio effettivo con
                    // il pipeline canonica executeActualPlay(), che emette
                    // loadVideo/play e sincronizza youtubePlayWhenReady.
                    if (firstItemIsOnline && _internalYouTubePlayer.value != null) {
                        myMainHandler.postDelayed({
                            if (alarmSessionAlarmId != null &&
                                hybridPlayer.activeEngine == ActiveEngine.YOUTUBE
                            ) {
                                Timber.i("PlayerService ALARM: servizio già attivo, comando l'avvio effettivo sul bridge YouTube")
                                hybridPlayer.executeActualPlay()
                            }
                        }, 600)
                    }

                    if (options.fadeInEnabled) startAlarmFadeIn()

                    Timber.i("PlayerService ALARM: riproduzione avviata (${mediaItems.size} brani)")
                }.onFailure {
                    Timber.e("PlayerService ALARM: avvio riproduzione fallito: ${it.message}")
                }
            }

            if (options.autoStopMinutes > 0) scheduleAlarmAutoStop(options.autoStopMinutes)

            val sourceName = alarm.sourceName.ifBlank { getString(R.string.alarm_title) }
            postAlarmNotification(alarm, sourceName, options.snoozeMinutes)
        }
    }

    /**
     * ALARM: porta il servizio in stato foreground SUBITO, prima di qualsiasi
     * lavoro asincrono. Non è condizionato da AppForegroundTracker: con lo
     * schermo spento e l'app aperta il tracker vede l'app "in foreground"
     * (l'activity è in pausa ma non stoppata) e la promozione verrebbe saltata,
     * lasciando scadere il contratto di startForegroundService con conseguente
     * kill del processo da parte del sistema.
     */
    private fun startForegroundForAlarm() {
        runCatching {
            val notification = NotificationCompat.Builder(this, ALARM_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.alarm)
                .setContentTitle(getString(R.string.alarm_notification_title))
                .setContentText(getString(R.string.alarm_notification_starting))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .build()
            if (isAtLeastAndroid10) {
                startForeground(
                    ALARM_NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(ALARM_NOTIFICATION_ID, notification)
            }
            Timber.d("PlayerService ALARM: foreground service acquisito immediatamente")
        }.onFailure {
            Timber.e("PlayerService ALARM: startForeground rifiutato (playback prosegue comunque): ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    private fun handleAlarmStop() {
        Timber.i("PlayerService ALARM: stop sveglia richiesto")
        markUserPauseRequested()
        hybridPlayer.pause()
        endAlarmSession("stop richiesto")
    }

    /**
     * Risolve i MediaItem della sorgente della sveglia (brano, playlist o album).
     * Per i brani scelti online non presenti in libreria, usa lo snapshot
     * salvato nella sveglia e lo inserisce nel database.
     * Per gli album usa l'ordine traccia (position) come in AlbumDetails.
     */
    private suspend fun buildAlarmMediaItems(alarm: AlarmItem): List<MediaItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                when {
                    alarm.playlistId != null ->
                        Database.songsInPlaylist(alarm.playlistId).first()
                            .map { it.asMediaItem }

                    alarm.albumId != null ->
                        Database.albumSongsList(alarm.albumId)
                            .map { it.asMediaItem }

                    alarm.songId != null -> {
                        val song = Database.songDao().getById(alarm.songId)
                            ?: alarm.songSnapshot?.also { Database.insert(it) }
                        song?.let { listOf(it.asMediaItem) } ?: emptyList()
                    }

                    else -> emptyList()
                }
            }.onFailure {
                Timber.e("PlayerService ALARM: caricamento sorgente fallito: ${it.message}")
            }.getOrDefault(emptyList())
        }

    /**
     * Fade-in del volume su ~30 secondi. Inibisce temporaneamente i reset di
     * volume automatici del servizio (crossfade/ripristino) tramite
     * [alarmFadeInProgress].
     */
    private fun startAlarmFadeIn() {
        alarmFadeInJob?.cancel()

        alarmFadeInProgress = true
        isFading = true
        hybridPlayer.setFadeVolume(0f)

        alarmFadeInJob = serviceScope.launch(Dispatchers.Main) {
            runCatching {
                val steps = 30
                for (i in 1..steps) {
                    delay(1.seconds)
                    hybridPlayer.setFadeVolume(i / steps.toFloat())
                }
                hybridPlayer.setFadeVolume(1f)
                Timber.i("PlayerService ALARM: fade-in completato")
            }.onFailure {
                Timber.e("PlayerService ALARM: fade-in interrotto: ${it.message}")
            }
            alarmFadeInProgress = false
            isFading = false
        }
    }

    /** Ferma la riproduzione della sveglia dopo i minuti indicati. */
    private fun scheduleAlarmAutoStop(minutes: Int) {
        alarmAutoStopJob?.cancel()
        if (minutes <= 0) return

        Timber.i("PlayerService ALARM: auto-stop programmato tra $minutes minuti")
        alarmAutoStopJob = serviceScope.launch(Dispatchers.Main) {
            runCatching {
                delay(minutes.minutes)

                // Sfumatura rapida di cortesia prima della pausa
                for (i in 1..5) {
                    hybridPlayer.setFadeVolume(1f - (i / 5f))
                    delay(120.milliseconds)
                }

                hybridPlayer.setFadeVolume(1f)
                markUserPauseRequested()
                hybridPlayer.pause()
                Timber.i("PlayerService ALARM: auto-stop eseguito")
            }.onFailure {
                Timber.e("PlayerService ALARM: auto-stop interrotto: ${it.message}")
            }
            endAlarmSession("auto-stop")
        }
    }

    /** Chiude la sessione sveglia: cancella job, resetta il volume e rimuove la notifica. */
    private fun endAlarmSession(reason: String) {
        Timber.i("PlayerService ALARM: fine sessione sveglia ($reason)")
        alarmSessionAlarmId = null
        alarmSessionHeardPlayback = false
        alarmAutoStopJob?.cancel()
        alarmAutoStopJob = null
        alarmFadeInJob?.cancel()
        alarmFadeInJob = null
        alarmFadeInProgress = false
        isFading = false
        runCatching { hybridPlayer.setFadeVolume(1f) }
        notificationManager?.cancel(ALARM_NOTIFICATION_ID)
    }

    private fun postAlarmNotification(
        alarm: AlarmItem,
        text: String,
        snoozeMinutes: Int,
        sourceMissing: Boolean = false,
    ) {
        runCatching {
            val snoozePi = PendingIntent.getBroadcast(
                this, 20_001,
                Intent(this, AlarmReceiver::class.java)
                    .setAction(ACTION_ALARM_SNOOZE)
                    .setPackage(packageName)
                    .putExtra(EXTRA_ALARM_ID, alarm.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stopPi = PendingIntent.getBroadcast(
                this, 20_002,
                Intent(this, AlarmReceiver::class.java)
                    .setAction(ACTION_ALARM_STOP)
                    .setPackage(packageName)
                    .putExtra(EXTRA_ALARM_ID, alarm.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val openAppPi = PendingIntent.getActivity(
                this, 20_003,
                Intent(this, MainActivity::class.java).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, ALARM_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.alarm)
                .setContentTitle(getString(R.string.alarm_notification_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(!sourceMissing)
                .setContentIntent(openAppPi)

            if (!sourceMissing) {
                builder.addAction(0, getString(R.string.alarm_snooze, snoozeMinutes), snoozePi)
                builder.addAction(0, getString(R.string.stop), stopPi)
            }

            notificationManager?.notify(ALARM_NOTIFICATION_ID, builder.build())
        }.onFailure {
            Timber.e("PlayerService ALARM: notifica sveglia non mostrata: ${it.message}")
        }
    }

    private fun initializeMediaItemState() {
        currentMediaItemState.value = exoPlayer.currentMediaItem
    }

    fun replaceOnlinePlayerView() {
//        _internalYouTubePlayer.value?.pause()
//        _internalYouTubePlayer.value = null

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
        this@PlayerService.serviceScope.launch {
            while (appSettings.playbackDuration > 0) {
                withContext(Dispatchers.Main) {
                    Timber.d("PlayerService initializeMedleyMode medleyDuration ${appSettings.playbackDuration} player.isPlaying ${exoPlayer.isPlaying} internalOnlinePlayerState ${_playerState.value.isPlaying}")
                    val seconds =
                        if (currentSong.value?.isLocal == true) exoPlayer.currentPosition.div(1000)
                            .toInt() else _currentSecond.value.toInt()
                    if (appSettings.playbackDuration.toInt() <= seconds) {
                        handlePlayNext("initializeMedleyMode")
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

        riTuneObserverJob = this@PlayerService.serviceScope.launch {

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
                    handlePlayNext("onSensorChanged")
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
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
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
                //addAnalyticsListener(PlaybackStatsListener(false, this@PlayerService)) // listener è registrato dinamicamente dentro hybridPlayer
                sleepTimerListener = SleepTimerListener(this@PlayerService.serviceScope, this)
                addListener(sleepTimerListener)
            }

        exoPlayer.repeatMode = appSettings.queueLoopType.type

        exoPlayer.skipSilenceEnabled = appSettings.skipSilenceEnabled
        exoPlayer.pauseAtEndOfMediaItems = true

        // Crea l'Hybrid Player
        hybridPlayer = HybridPlayer(this,exoPlayer, ytControlWrapper)
        // REGISTRA IL SERVIZIO SULL'HYBRID PLAYER
        // In questo modo onIsPlayingChanged riceverà gli eventi di ENTRAMBI i motori
        hybridPlayer.addListener(this@PlayerService)
        // Dichiariamo subito qual è il motore da preparare, serve anche a registrare il listener delle statistiche se la canzone dipende da exo
        if (currentSong.value?.isLocal == true) hybridPlayer.switchToExo() else hybridPlayer.switchToYoutube()

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
            this@PlayerService.serviceScope,
            (512 * resources.displayMetrics.density).roundToInt()
        ))

        mediaLibrarySessionCallback = MediaLibraryServiceCallback(binder, this)

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("fromNotification", true)
        }

        mediaLibrarySession = MediaLibrarySession
            .Builder(this@PlayerService, hybridPlayer, mediaLibrarySessionCallback)
            .setId("${packageName}.MEDIA_SESSION_ID")
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    sessionActivityIntent,
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setBitmapLoader(customBitmapLoader)
            // DISABILITA gli aggiornamenti di posizione automatici troppo frequenti
            // che causano il freeze della coda su Android Auto nelle ultime release
            .setPeriodicPositionUpdateEnabled(false)
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

    fun nextPlayableIndexAfter(current: Int): Int? {
        val timeline = hybridPlayer.currentTimeline
        if (timeline.isEmpty) return null

        val marked = unplayableIds.value

        // REPEAT_MODE_OFF forzato: la ricerca del target-skip deve replicare
        // esattamente la semantica di seekToNextMediaItem(),
        // che a sua volta tratta REPEAT_MODE_ONE come se fosse OFF.
        val shuffled = hybridPlayer.shuffleModeEnabled

        val window = Timeline.Window()
        var idx = timeline.getNextWindowIndex(current, Player.REPEAT_MODE_OFF, shuffled)
        var hops = 0
        while (idx != C.INDEX_UNSET && hops < timeline.windowCount) {
            timeline.getWindow(idx, window)
            if (window.mediaItem.mediaId !in marked) return idx
            idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, shuffled)
            hops++
        }
        return null   // fine coda, oppure tutte le successive sono marcate
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
                //Timber.d("PlayerService onlinePlayer onReady isPersistentQueueEnabled $appSettings.persistentQueue isResumePlaybackOnStart ${appSettings.resumePlaybackOnStart}")

                youTubePlayer.setVolume(getSystemMediaVolume())

                if (currentSong.value?.isLocal == true) return

                // Diciamo a HybridPlayer di passare all'engine YT
                hybridPlayer.switchToYoutube()
                // Imposto i secondi dall'inizio in base a playFromSecond che viene salvata nella coda
                _currentSecond.value = playFromSecond

                currentSong.value?.id?.let{
                    // SVEGLIA: durante l'avvio di una sveglia non ripartire in automatico
                    // con la vecchia coda persistente (a freddo, prima che la sveglia
                    // sostituisca la coda, onReady qui farebbe esplodere il brano vecchio).
                    if (appSettings.persistentQueue && appSettings.resumePlaybackOnStart && firstTimeStarted && !skipAutoload && alarmSessionAlarmId == null) {
                        //Timber.d("LOAD-COMMAND videoId=${it} start=${playFromSecond}")

                        youTubePlayer.loadVideo(it, playFromSecond)
                        Timber.d("PlayerService onlinePlayer onReady loadVideo = $it playFromSecond = $playFromSecond")
                    } else {
                        youTubePlayer.cueVideo(it, playFromSecond)
                        Timber.d("PlayerService onlinePlayer onReady cueVideo = $it playFromSecond = $playFromSecond")
                    }
                }

                // ── Connettività: load di recovery rimasto in sospeso (WebView mai pronta
                //    perché partita offline, o ricreata dal watchdog di livello 3) ──
                val pendingRecoveryId = pendingRecoveryLoadVideoId
                if (pendingRecoveryId != null && currentSong.value?.isLocal == false) {
                    pendingRecoveryLoadVideoId = null
                    val pendingSecond = pendingRecoveryLoadSecond
                    Timber.i("PlayerService Connettività: onReady esegue il load di recovery id=$pendingRecoveryId second=$pendingSecond resumeDesired=$resumeDesiredAfterConnectivity")
                    if (resumeDesiredAfterConnectivity) {
                        youTubePlayer.loadVideo(pendingRecoveryId, pendingSecond)
                        hybridPlayer.youtubePlayWhenReady = true
                        hybridPlayer.invalidateYouTubePlayPause()
                    } else {
                        youTubePlayer.cueVideo(pendingRecoveryId, pendingSecond)
                    }
                }

                // ── SVEGLIA: load pendente della sveglia (WebView non era pronta
                //    quando la sveglia ha provato a partire, tipico avvio a freddo
                //    con app chiusa) ──
                val pendingAlarmId = pendingAlarmLoadVideoId
                if (pendingAlarmId != null && alarmSessionAlarmId != null) {
                    val currentId = hybridPlayer.currentMediaItem?.mediaId ?: currentSong.value?.id
                    if (currentId == pendingAlarmId) {
                        pendingAlarmLoadVideoId = null
                        Timber.i("PlayerService ALARM: onReady esegue il load pendente della sveglia id=$pendingAlarmId")
                        youTubePlayer.loadVideo(pendingAlarmId, 0f)
                        hybridPlayer.youtubePlayWhenReady = true
                        hybridPlayer.invalidateYouTubePlayPause()
                    } else {
                        // La sveglia nel frattempo è stata annullata/sostituita:
                        // scarta il load pendente per non suonare brani sbagliati.
                        Timber.i("PlayerService ALARM: onReady scarta il load pendente (brano corrente $currentId ≠ sveglia $pendingAlarmId)")
                        pendingAlarmLoadVideoId = null
                    }
                }

                // Nuova base temporale per il watchdog anti-stallo online
                lastProgressSecond = -1f
                lastProgressAtMs = System.currentTimeMillis()

                firstTimeStarted = false

            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {

                //Timber.d("PlayerService LOAD-PENDING onCurrentSecond sec=$second isPlaying = ${_playerState.value.isPlaying} playerState = ${_playerState.value.playbackState}")
                //val limit = if (firstTimeStarted) (playFromSecond..playFromSecond + 4f) else (1f..4f)
                val limit = playFromSecond + 1f..playFromSecond + 4f
                if(_playerState.value.loadPending.isPending && _playerState.value.isPlaying && (second in limit)) {
                    //Timber.d("PlayerService LOAD-PENDING OnCurrentSecond limit = $limit sec=$second >> firstTimeStarted = $firstTimeStarted << ")
                    setLoadPending(LoadPhase.NONE, "onCurrentSecond")
                }

                val oldSecond = _currentSecond.value
                _currentSecond.value = second


                if (hybridPlayer.activeEngine == ActiveEngine.YOUTUBE) {
                    hybridPlayer.invalidateYouTubePositionOnly()
                    if (oldSecond == 0f || kotlin.math.abs(second - oldSecond) >= 1f) {

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

//            override fun onVolumeChange(youTubePlayer: YouTubePlayer, volume: Int) {
//                super.onVolumeChange(youTubePlayer, volume)
//                Timber.d("PlayerService onlinePlayer onVolumeChange $volume")
//            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                Timber.d("PlayerService onStateChange: $state")

                if (currentSong.value?.isLocal == true) return
                //Timber.d("PlayerService onlinePlayerView: onStateChange $state")
                //unstartedWatchdogJob?.cancel()

                updatePlayerState(state)

                when(state) {
                    /* UNSTARTED è gestito a livello di js
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
                     */

                    /*
                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                        Timber.d("PlayerService onlinePlayerView: onStateChange VIDEO_CUED")
                        playFromSecond = 0f
                        hybridPlayer.pause()
                        //_internalYouTubePlayer.value?.pause()
                        //youTubePlayer.pause()
                        if (!firstTimeStarted) {
                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus.value != RiTuneConnectionStatus.Connected) {
                                youTubePlayer.unMute()
                                youTubePlayer.setVolume(getSystemMediaVolume())

                                // Prepara hybridPlayer per la riproduzione
                                hybridPlayer.playWhenReady = true
                                hybridPlayer.play()
                                //youTubePlayer.play()
                                Timber.d("PlayerService onlinePlayerView: onStateChange VIDEO_CUED regular play() > ${currentSong.value?.id} ${currentSong.value?.title}")
                            }

                        }

                    }
                    */
                    PlayerConstants.PlayerState.PLAYING -> {
                        setLoadPending(LoadPhase.NONE, "onStateChange PLAYING")
                        resumePending = false

                        // Il motore è tornato a suonare: qualunque vecchia intenzione
                        // di pausa dell'utente è superata e non deve più inquinare i
                        // successivi eventi PAUSED (flag stale)
                        clearUserPauseIntent()

                        //lastError = null  // reset errore dopo riproduzione riuscita
                        onlineNearEndTicks = 0
                        Timber.d("PlayerService YT: Evento PLAYING")

                        // ── Connettività: il motore online è davvero ripartito,
                        //    chiudiamo qualunque ciclo di pausa/ripresa o recovery ──
                        if (pausedForConnectivity || onlineRecoveryLevel > 0 || resumeDesiredAfterConnectivity) {
                            Timber.i("PlayerService Connettività: PLAYING ricevuto, reset completo dello stato di recovery")
                            pausedForConnectivity = false
                            resumeDesiredAfterConnectivity = false
                            resetOnlineRecoveryState("onStateChange PLAYING")
                        }

                        hybridPlayer.youtubePlayWhenReady = true// Non invia a AYP il comando di play perchè il load invoca già il play


                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        val inCall = audioManager.mode != AudioManager.MODE_NORMAL
                        if (inCall && !isUserPauseIntentFresh() && hybridPlayer.activeEngine == ActiveEngine.YOUTUBE) {
                            resumePending = true
                            Timber.d("PlayerService YT: pausa esterna durante chiamata (mode=${audioManager.mode}) → resumePending")
                        }

                        // ── Connettività: distinguiamo CHI ha messo in pausa ──
                        // userInitiatedPause viene onorato SOLO se un comando di pausa
                        // utente è arrivato da pochissimo (USER_PAUSE_EVENT_WINDOW_MS).
                        // Due falsi positivi da evitare:
                        //   1) l'ECO della nostra stessa pausa per connettività (arriva
                        //      ~50-300ms dopo il nostro pause diretto sull'iframe)
                        //   2) un flag STALE di una vecchia pausa il cui evento PAUSED
                        //      non è mai arrivato (pausa durante il buffering, WebView
                        //      assente, iframe morto)
                        // hadPendingUserPause = è arrivato un comando di pausa utente che
                        // questo PAUSED sta per consumare; blocca l'auto-arm di riprese
                        // basate su una pausa che in realtà l'utente HA richiesto.
                        val hadPendingUserPause = userInitiatedPause

                        if (userInitiatedPause && !isUserPauseIntentFresh()) {
                            val age = System.currentTimeMillis() - userPauseRequestedAtMs
                            Timber.w("PlayerService Connettività: flag pausa-utente STALE ignorato (età=${age}ms, non può essere la causa di questo PAUSED)")
                        }

                        if (isUserPauseIntentFresh()) {
                            // Pausa appena richiesta dall'utente (anche durante un'interruzione
                            // di rete): annulliamo qualunque auto-ripresa programmata
                            if (pausedForConnectivity || resumeDesiredAfterConnectivity) {
                                Timber.i("PlayerService Connettività: pausa dell'utente durante l'interruzione, auto-ripresa annullata")
                            }
                            pausedForConnectivity = false
                            resumeDesiredAfterConnectivity = false
                        } else if (pausedForConnectivity && !hadPendingUserPause) {
                            // Eco della pausa eseguita da NOI per l'assenza di rete:
                            // l'auto-ripresa programmata resta valida
                            Timber.d("PlayerService Connettività: PAUSED = eco della pausa di connettività, auto-ripresa confermata (resumeDesired=$resumeDesiredAfterConnectivity)")
                        } else if (!hadPendingUserPause && !inCall &&
                            hybridPlayer.activeEngine == ActiveEngine.YOUTUBE &&
                            !onlineConnectivityMonitor.isNetworkAvailable
                        ) {
                            // L'iframe si è auto-pausato per l'assenza di rete:
                            // ricordiamo la volontà di riprendere al ritorno della rete
                            Timber.i("PlayerService Connettività: pausa dell'iframe per rete assente, ripresa programmata al ritorno")
                            resumeDesiredAfterConnectivity = true
                        }

                        clearUserPauseIntent() // il flag è one-shot: consumato da questo PAUSED
                        Timber.d("PlayerService YT: Evento PAUSED (inCall = $inCall audioManager.mode=${audioManager.mode})")

                        hybridPlayer.youtubePlayWhenReady = false // Non invia a AYP il comando di pause, in questo modo il ciclo di riproduzione continua al ricevimento del focus

                        onlineNearEndTicks = 0

                        if (::hybridPlayer.isInitialized) {
                            hybridPlayer.invalidateYouTubePlayPause()
                        }
                    }
                    PlayerConstants.PlayerState.ENDED -> {
                        onlineNearEndTicks = 0
                        serviceScope.launch(Dispatchers.Main) {
                            handleEndOfSong("onStateChange ENDED")
                        }
                    }
                    PlayerConstants.PlayerState.BUFFERING,
                    PlayerConstants.PlayerState.UNSTARTED -> {
                        setLoadPending(LoadPhase.PENDING, "onStateChange BUFFERING OR UNSTARTED")
                    }
                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                        setLoadPending(LoadPhase.NONE, "onStateChange VIDEO_CUED")
                    }

                    else -> {}
                }

                updateDiscordPresence()

            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {
                // Connettività: ricordiamo se stava suonando PRIMA DI azzerare lo stato.
                // Serve anche youtubePlayWhenReady: con la rete caduta l'iframe passa
                // prima in BUFFERING (isPlaying=false) e poi emette l'errore.
                val wasPlayingWhenError = _playerState.value.isPlaying || hybridPlayer.youtubePlayWhenReady

                // Nessun errore può lasciare la sessione "playing"
                hybridPlayer.youtubePlayWhenReady = false
                hybridPlayer.invalidateYouTubePlayPause()

                val currentState = _playerState.value
                _playerState.value = currentState.copy(
                    playbackState = PlaybackState.ERROR
                )

                if (currentSong.value == null || currentSong.value?.isLocal == true) return

                if (appSettings.persistentQueue)
                    this@PlayerService.serviceScope.launch { saveQueue() }


                if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus.value != RiTuneConnectionStatus.Connected)
                    youTubePlayer.pause()
                else
                    this@PlayerService.serviceScope.launch {
                        riTuneCastClient.sendCommand(
                            RiTuneRemoteCommand(
                                "pause",
                                position = playFromSecond
                            )
                        )
                    }

                //clearWebViewData()

                // ── Connettività: errore TRANSITORIO causato dall'assenza di rete ──
                // HTML_5_PLAYER durante (o poco dopo) un'interruzione NON è un problema
                // del brano: niente skip, niente marcatura "unplayable". Mettiamo in pausa
                // e lasciamo che la ripresa della rete riavvii il playback.
                val networkDownRecently = !onlineConnectivityMonitor.isNetworkAvailable ||
                        System.currentTimeMillis() - lastNetworkLostAtMs in 0..TRANSIENT_ERROR_WINDOW_MS
                if (networkDownRecently && error == PlayerConstants.PlayerError.HTML_5_PLAYER) {
                    Timber.w("PlayerService Connettività: errore $error TRANSITORIO (rete assente o appena rientrata). Pausa + attesa ripresa, nessuno skip")
                    pausedForConnectivity = true
                    resumeDesiredAfterConnectivity = wasPlayingWhenError
                    updatePlayerState(PlayerConstants.PlayerState.PAUSED)
                    return
                }

                val isNotPlaybalePermanent = error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER
                        || error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND

                if (isNotPlaybalePermanent) {
                    consecutivePermanentErrors++
                    if (consecutivePermanentErrors <= 2) {

                        // Marchiamo la traccia come non riproducibile perchè l'errore not playable è permanente
                        serviceScope.launch {
                            currentSong.value?.id?.let {
                                val unplayableTrack = UnplayableTrack(
                                    videoId = it,
                                    reason = error.code,
                                    markedAt = System.currentTimeMillis(),
                                    markedWhileLoggedIn = isYtLoggedIn()
                                )
                                Database.unplayableTracksDao().upsert(unplayableTrack)
                            }
                        }

                        Timber.e("PlayerService: permanent error = $error on id = ${currentSong.value?.id}, marked as unplayable")

                        consecutivePermanentErrors = 0  // azzero prima di lanciare la prossima traccia
                        val title = currentSong.value?.title
                        SmartMessage(
                            getString(R.string.skip_unplayable_track_message, title),
                            PopupType.Warning,
                            context = this@PlayerService
                        )
                        handlePlayNext("onError permanent error = $error")
                        return
                    } else {
                        // Coda interamente non riproducibile?
                        val nextPlayable = nextPlayableIndexAfter(hybridPlayer.currentMediaItemIndex)
                        if (nextPlayable == null) {
                            consecutivePermanentErrors = 0  // non ci sono altre tracce riproducibili azzero
                            SmartMessage(this@PlayerService.getString(R.string.no_playable_tracks_message),
                                PopupType.Error,
                                context = this@PlayerService

                            )
                            return // Ritorna solo se tutte le tracce successive sono marcate come non riproducibili
                        }
                    }
                }



                /*

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
                            hybridPlayer.pause()
//                            if (exoPlayer.isPlaying) {
//                                exoPlayer.pause()
//                                exoPlayer.stop()
//                            }

                            if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus.value != RiTuneConnectionStatus.Connected) {

                                hybridPlayer.pause()
                                youTubePlayer.pause()
                                youTubePlayer.cueVideo(it, playFromSecond)
                            }
                            else this@PlayerService.serviceScope.launch {
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

                 */

                //lastError = error

                if (!appSettings.skipMediaOnError) return
                val prev = hybridPlayer.currentMediaItem ?: return

                // Ferma ExoPlayer se sta andando
//                if (exoPlayer.isPlaying) {
//                    exoPlayer.pause()
//                    exoPlayer.stop()
//                }

                handlePlayNext(
                    "initializeOnlinePlayer.onError with skip on errore enabled"
                )

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
            this@PlayerService.serviceScope.launch {
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

            // Connettività: la ripresa al ritorno della rete è gestita dal Service
            // (OnlineConnectivityMonitor + policy dedicata). Il PlaybackResumer
            // interno della libreria parte con canLoad=false e senza lifecycle non
            // potrebbe mai riprendere davvero: lo disattiviamo per evitare conflitti.
            setInternalPlaybackResumeEnabled(false)

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
                //.listType("playlist")
                .build()

            initialize(listener, iFramePlayerOptions)

        }

    }

    private fun updatePlayerState(state: PlayerConstants.PlayerState) {
        val currentState = _playerState.value
        _playerState.value = when (state) {
            PlayerConstants.PlayerState.PLAYING -> currentState.copy(playbackState = PlaybackState.PLAYING)
            PlayerConstants.PlayerState.UNSTARTED -> currentState.copy(playbackState = PlaybackState.UNSTARTED)
            PlayerConstants.PlayerState.PAUSED,
            PlayerConstants.PlayerState.VIDEO_CUED,
            PlayerConstants.PlayerState.UNKNOWN -> currentState.copy(playbackState = PlaybackState.PAUSED)
            PlayerConstants.PlayerState.ENDED -> currentState.copy(playbackState = PlaybackState.ENDED)
            PlayerConstants.PlayerState.BUFFERING -> currentState.copy(playbackState = PlaybackState.BUFFERING)
        }

        updateWidgetState()
    }

    private fun initializeAudioVolumeObserver() {
        audioVolumeObserver = AudioVolumeObserver(this, audioManager)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)
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
                                handlePlayNext("getVolumeProvider.onAdjustVolume")
                            } else {
                                audioManager.adjustStreamVolume(
                                    STREAM_TYPE,
                                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                                setCurrentVolume(audioManager.getStreamVolume(STREAM_TYPE))
                            }
                        } else if (direction == VOLUME_DOWN) {
                            if (hybridPlayer.isPlaying && useVolumeKeysToChangeSong) {
                                handlePlayPreviousRequestedByUser("PlayerService.getVolumeProvider.onAdjustVolume")
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

        this@PlayerService.serviceScope.launch { saveQueue() }
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
        Timber.d("PlayerService: onDestroy STARTED")
        //Timber.d("SCOPES PlayerService.serviceScope CANCELLED (was ${this@PlayerService.serviceScope.hashCode()})")
        _isServiceReady.value = false

        runCatching { LyricsOverlayManager.release() }
            .onFailure { Timber.e("PlayerService onDestroy lyrics overlay release failed: $it") }

        stopPlaybackWatchdog()

        // Tolgo il lifecycle observer per la cancellazione dei video nella coda quando l'app va in backgroound
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)

        sendCloseExternalEqualizerIntent()

        // RIMOZIONE SICURA DEI RECEIVER
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy Error when removing screenReceiver: ${e.message}")
        }

        try {
            unregisterReceiver(legacyActionReceiver)
        } catch (e: Exception) {
            Timber.e("PlayerService onDestroy unregisterReceiver legacyActionReceiver: ${e.message}")
        }


        equalizerHelper.release()


        if (::hybridPlayer.isInitialized) {
            hybridPlayer.release()
        }


        try {
            exoPlayer.removeListener(this@PlayerService)
            exoPlayer.release()
            Timber.d("PlayerService onDestroy: ExoPlayer released")
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
            //unstartedWatchdogJob?.cancel()
            volumeNormalizationJob?.cancel()
            settingsObserverJob?.cancel()

            // Rilascio della gestione connettività online
            onlineStallWatchdogJob?.cancel()
            networkGraceJob?.cancel()
            connectivityResumeJob?.cancel()
            connectivityListener?.let { onlineConnectivityMonitor.removeListener(it) }
            onlineConnectivityMonitor.stop()

            AudioDRCHelper.restoreDRC()

            // non chiamare notificationManager?.cancelAll()
            // se no viene cancellata la notifica di cortesia del timer!

            unregisterAudioDeviceCallback()

        }.onFailure {
            Timber.e("PlayerService: Failed onDestroy in PlayerService ${it.stackTraceToString()}")
        }

        // Chiudiamo il raggio d'azione delle coroutine solo alla fine, dopo che l'hardware è spento
        this@PlayerService.serviceScope.cancel()

        isServiceInForeground = false

        Timber.d("PlayerService: onDestroy completed")
        super.onDestroy()
    }


    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (appSettings.isPauseOnVolumeZeroEnabled) {
            if ((_playerState.value.isPlaying) && currentVolume < 1) {
                this@PlayerService.serviceScope.launch {
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

        Timber.d("PlayerService onPlaybackStateChanged playbackState $playbackState")

        //if (hybridPlayer.activeEngine != ActiveEngine.EXOPLAYER) return

        if (playbackState == Player.STATE_READY) {

            // Inizializziamo l'equalizzatore solo se non è già inizializzato
            if (!eqSetupDone) {
                eqSetupDone = true
                equalizerHelper.setupWithRetry(0)
            }

            // Torniamo indietro se l'engine non è EXO
            if (hybridPlayer.activeEngine != ActiveEngine.EXOPLAYER) return

            // Durante il fade-in della sveglia il volume è gestito dalla rampa dedicata:
            // un ripristino qui lo interromperebbe bruscamente
            if (alarmFadeInProgress) return

            // Fade-in solo all'inizio del brano
            if (appSettings.crossfadeDuration != CrossfadeDuration.Off) {
                if (!isFading) startFadeIn()
                else hybridPlayer.setFadeVolume(1.0f)
            } else {
                hybridPlayer.setFadeVolume(1.0f)
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

                handlePlayNext("onPositionDiscontinuity")
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
            this@PlayerService.serviceScope.launch {
                Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }


        val minTimeForEvent = appSettings.minTimeForEvent

        if (totalPlayTimeMs > minTimeForEvent.ms) {
            Timber.d("PlayerService onPlaybackStatsReady INSERT EVENT totalPlayTimeMs $totalPlayTimeMs")
            this@PlayerService.serviceScope.launch {
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

        // ── Connettività: cambio traccia = nuovo ciclo, azzeriamo pausa e recovery ──
        pausedForConnectivity = false
        resumeDesiredAfterConnectivity = false
        clearUserPauseIntent() // anche una vecchia intenzione di pausa non deve sopravvivere al cambio traccia
        resetOnlineRecoveryState("onMediaItemTransition")

        // Ferma il fade out se era in corso (es. l'utente ha premuto "Next" a metà brano)
        //stopPlaybackWatchdog()

        applyPlaybackParameters()

        val origin = PlaybackContext.currentOrigin.value
        val suggestionInfo = PlaybackContext.currentSuggestionInfo.value
        val isFromSuggestion = origin == PlaybackOrigin.SUGGESTION &&
                suggestionInfo?.itemId == mediaItem.mediaId

        Timber.d("PlayerService onMediaItemTransition to ${mediaItem.mediaId}, origin=$origin, isSuggestion=$isFromSuggestion")

        // todo in the future save in preferences if enabled
        this@PlayerService.serviceScope.launch {
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

        //_currentSecond.value = 0F
        //hybridPlayer.invalidateYouTubePositionOnly() // resetta la posizione

        val newMediaId = mediaItem.mediaId

        if (lastOnlineMediaId == newMediaId) {
            Timber.d("PlayerService: onMediaItemTransition Transition ignored, same MediaID ($newMediaId) skipped")
            return
        }

        // Aggiorniamo l'ultimo mediaId caricato
        lastOnlineMediaId = newMediaId

        Timber.d("PlayerService onMediaItemTransition mediaItem ${mediaItem.mediaId} reason $reason")

        currentQueuePosition = exoPlayer.currentMediaItemIndex

        if (appSettings.parentalControlEnabled && mediaItem.isExplicit) {
            handlePlayNext("onMediaItemTransition parental control enabled")
            SmartMessage(resources.getString(androidx.media3.session.R.string.error_message_parental_control_restricted), context = this@PlayerService)
            return
        }

        if (appSettings.videoContentMode.excluded && mediaItem.isVideo) {
            handlePlayNext("onMediaItemTransition excludeIfIsVideoEnabled")
            SmartMessage(getString(R.string.warning_skipped_video), context = this@PlayerService)
            return
        }

        var blacklisted = false
        runBlocking(Dispatchers.IO) {
            blacklisted = Database.blacklisted(mediaItem.mediaId) > 0
        }
        if (blacklisted) {
            handlePlayNext("onMediaItemTransition blacklisted")
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

        // Se l'utente aveva richiesto la visualizzazione di un video, quando passiamo avanti lo resettiamo
        // se in onmediaitemtransition non è ancora stato intercettato, lo facciamo qua
        if (appSettings.forceUserVideoPlayback) {
            this@PlayerService.serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(forceUserVideoPlayback = false))
            }
        }

        mediaItem.let {

            if (!it.isLocal){
                if (!firstTimeStarted) // Ci assicuriamo di Non mostro il loader se è il primo avvio, serve solo a precaricare il player online
                    setLoadPending(LoadPhase.PENDING, "onMediaItemTsransition >> firstTimeStarted = $firstTimeStarted << ")

                exoPlayer.pause()
                hybridPlayer.switchToYoutube()
                playFromSecond = 0f // Resettiamo la posizione iniziale perchè passiamo al successivo

                // ═══ FIX PICCO DI VOLUME ALL'AVVIO DI UN BRANO ═══
                // Non forzare il volume direttamente su _internalYouTubePlayer.value?.setVolume(getSystemMediaVolume())
                // per non bypassare il pipeline centralizzato di HybridPlayer (fadeMultiplier + normalizzazione loudness).
                hybridPlayer.setFadeVolume(0f)

                startForegroundPlaceholder()

                if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus.value != RiTuneConnectionStatus.Connected) {
                    Timber.d("PlayerService [AYP JS YT-BRIDGE] onMediaItemTransition mediaItem not local video cued id = ${it.mediaId} playFromSecond $playFromSecond")
                    _internalYouTubePlayer.value?.loadVideo(it.mediaId, 0f) // Si parte sempre dall'inizio perchè è il successivo

                    if (appSettings.crossfadeDuration == CrossfadeDuration.Off) {
                        // ═══ FIX PICCO RESIDUO ═══
                        // Con crossfade disattivo startFadeIn() esce subito senza toccare il volume:
                        // ripristiniamo subito il livello corretto SEMPRE via pipeline (rispetta la
                        // normalizzazione), senza forzature immediate al 100%.
                        // La normalizzazione definitiva del brano arriverà comunque da
                        // setYtLoudnessDb() non appena il DB fornirà il loudness corretto.
                        // Il brano, intanto, nasce nel silenzio (setFadeVolume(0f) sopra) mentre la
                        // WebView sta ancora caricando: il tempo di lettura DB (pochi ms) è coperto.
                        if (appSettings.volumeNormalizationEnabled) {
                            val targetMediaId = it.mediaId
                            this@PlayerService.serviceScope.launch(Dispatchers.Main) {
                                val freshLoudness = withTimeoutOrNull(600L.milliseconds) {
                                    Database.loudnessDb(targetMediaId).cancellable().first()
                                }
                                // Se nel frattempo è già partito un altro brano, lasciamo
                                // gestire il volume alla sua stessa transizione
                                if (hybridPlayer.currentMediaItem?.mediaId == targetMediaId) {
                                    hybridPlayer.setYtLoudnessDb(freshLoudness ?: 0f)
                                    hybridPlayer.setFadeVolume(1.0f)
                                }
                            }
                        } else {
                            // Normalizzazione disattiva: nessun fattore da attendere, ripristino immediato
                            hybridPlayer.setFadeVolume(1.0f)
                        }
                    } else {
                        // Avvia il fade in per il nuovo brano appena parte il play se il fade/crossfade è attivo
                        startFadeIn()
                    }
                    Timber.d("PlayerService onMediaItemTransition mediaItem not local video cued id = ${it.mediaId} playFromSecond $playFromSecond")
                } else
                    this@PlayerService.serviceScope.launch {
                        riTuneCastClient.sendCommand(
                            RiTuneRemoteCommand(
                                "load",
                                mediaId = it.mediaId,
                                position = playFromSecond
                            )
                        )
                    }

                // Recupera genere
                val mbclient = MusicBrainz()
                val genreHelper = MBMetadataHelper(mbclient)
                this@PlayerService.serviceScope.launch {
                    genreHelper.onSongPlayed(it.mediaId)
                }

            } else {
                // Canzone locale o MusicVault — ferma il player online e lascia andare ExoPlayer
                _internalYouTubePlayer.value?.pause()

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
                    //exoPlayer.play()

                    // startFadeIn() verrà chiamato in onPlaybackStateChanged
                    // quando ExoPlayer dichiara di essere STATE_READY.
                }
            }

            bitmapProvider?.load(it.mediaMetadata.artworkUri) { bitmap ->
                this@PlayerService.serviceScope.launch {
                    setWallpaper(this@PlayerService, bitmap)
                }
                // L'artwork è pronto: aggiorna subito i widget con la copertina vera
                updateWidgetState()
            }
        }

        updateWidgetState()

        // ═══ FIX NORMALIZZAZIONE IN RITARDO DI UN BRANO ═══
        // Passiamo SUBITO il mediaId del brano che sta partendo in modo da escludere
        // l'uso del brano precedente
        initializeNormalizeVolume(mediaItem.mediaId)
        maybeProcessRadio(reason)

        updateDiscordPresence()

        this@PlayerService.serviceScope.launch { saveQueue() }

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

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)

        Timber.d("PlayerService onPlayWhenReadyChanged playWhenReady=$playWhenReady, reason=$reason")

    }

    override fun onTrimMemory(level: Int) {
        val isLowMemory = level == TRIM_MEMORY_RUNNING_CRITICAL
        Timber.d("PlayerService onTrimMemory level $level isLowMemory $isLowMemory")
        if (isLowMemory)
            this@PlayerService.serviceScope.launch { saveQueue() }
    }

    suspend fun recordListeningEvent(songId: String) {
        Timber.d("PlayerService recordListeningEvent")
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
                    this@PlayerService.serviceScope.launch(Dispatchers.Main) {
                        if (exoPlayer.playbackState != STATE_IDLE)
                            exoPlayer.addMediaItems(radio.process())
                    }
                }
            }
        }

    }

    private fun getAudioSessionId() =
        if (hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) hybridPlayer.audioSessionId else 0

    /**
     * Collega il LoudnessEnhancer alla sessione audio specifica di ExoPlayer.
     */
    @OptIn(UnstableApi::class)
    private fun setupLoudnessEnhancer() {
        if (loudnessEnhancer != null) return

        try {
            val audioSessionId = getAudioSessionId()

            //if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Timber.d("PlayerService LoudnessEnhancer attached to audio session: $audioSessionId")
            //}
        } catch (e: Exception) {
            Timber.e("PlayerService Error in initialization LoudnessEnhancer: ${e.message}")
        }
    }

    @ExperimentalCoroutinesApi
    @UnstableApi
    private fun initializeNormalizeVolume(targetMediaId: String? = null) {
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

        // Collego l'enhancer alla sessione audio di ExoPlayer o a quella di sistema in base al tipo di riproduzione
        setupLoudnessEnhancer()

        val baseGain = appSettings.loudnessBaseGain
        val boostLevel = appSettings.volumeBoostLevel

        volumeNormalizationJob?.cancel()
        volumeNormalizationJob = this@PlayerService.serviceScope.launch(Dispatchers.Main) {

            fun Float?.toMb() = ((this ?: 0f) * 100).toInt()

            // FIX PICCO RESIDUO: la chiave del loudness DEVE essere quella del brano che
            // sta partendo ADESSO. Se il chiamante (onMediaItemTransition) passa il mediaId
            // usiamo quello; currentSong.value letto qui sarebbe ancora il brano precedente
            // quindi userebbe sempre il brano precedente.
            val loudnessKey = targetMediaId
                ?: (if (currentSong.value?.isLocal == true)
                    currentSong.value?.mediaId else currentSong.value?.id).toString()

            Database.loudnessDb(loudnessKey)
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

                // Non riproduciamo se il dispositivo viene ricollegato ma il servizio è appena partito
                if (shouldPlay && !firstTimeStarted) {
                    this@PlayerService.serviceScope.launch {
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
        val sessionId = getAudioSessionId()

        //if (sessionId == AudioEffect.ERROR_BAD_VALUE || sessionId == 0) return

        try {
            Timber.d("PlayerService External Equalizer: Sent OPEN Intent for engine: ${hybridPlayer.activeEngine}, session: $sessionId")
            sendBroadcast(
                Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
            )
        } catch (e: Exception) {
            Timber.e("PlayerService External Equalizer: Error OPEN Intent centralized: ${e.message}")
        }
    }



    @UnstableApi
    private fun sendCloseExternalEqualizerIntent() {
        val sessionId = getAudioSessionId()

        try {
            Timber.d("PlayerService External Equalizer: Sent intent CLOSE audio sessionId: $sessionId")
            sendBroadcast(
                Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                }
            )
        } catch (e: Exception) {
            Timber.e("PlayerService External Equalizer: Error during sent intent CLOSE: ${e.message}")
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
            markUserPauseRequested()
            _internalYouTubePlayer.value?.pause()
        }

        override fun seekTo(positionMs: Long) {
            Timber.d("TICK-SEEK-RECV positionMs=$positionMs")
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
                        markUserPauseRequested()

                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus.value != RiTuneConnectionStatus.Connected)
                            hybridPlayer.pause()
                        else
                            this@PlayerService.serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "pause",
                                        position = playFromSecond
                                    )
                                )
                            }
                    }
                    Action.play.value -> {

                        if (!GlobalSharedData.riTuneCastActive || riTuneCastClient.connectionStatus.value != RiTuneConnectionStatus.Connected)
                            hybridPlayer.play()
                        else
                            this@PlayerService.serviceScope.launch {
                                riTuneCastClient.sendCommand(
                                    RiTuneRemoteCommand(
                                        "play",
                                        position = playFromSecond
                                    )
                                )
                            }

                    }
                    Action.next.value -> handlePlayNextRequestedByUser("LegacyActionReceiver.onReceive")
                    Action.previous.value -> handlePlayPreviousRequestedByUser("LegacyActionReceiver.onReceive")
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
                                hybridPlayer.play()
                            else
                                this@PlayerService.serviceScope.launch {
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

        if (hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER)
            setLoadPending(LoadPhase.NONE, "ExoPlayer onIsPlayingChanged = $isPlaying")

        if (isPlaying) {
            if (alarmSessionAlarmId != null) alarmSessionHeardPlayback = true
            if (appSettings.crossfadeDuration == CrossfadeDuration.Off && !alarmFadeInProgress){
                // SE IL CROSSFADE È DISATTIVATO (e la sveglia non sta facendo il fade-in):
                // Ripristiniamo immediatamente il volume pieno in modo sincrono
                hybridPlayer.setFadeVolume(1.0f)
                // E RISBLOCCHIAMO SUBITO IL WATCHDOG PERENNE PER IL NUOVO BRANO!
                isFading = false
                Timber.d("PlayerService onIsPlayingChanged: Nuovo brano avviato (Crossfade OFF). Scudo abbassato e Watchdog pronto.")
            }
            sendOpenExternalEqualizerIntent()

            updatePlayerState(PlayerConstants.PlayerState.PLAYING)
        } else {
            updatePlayerState(PlayerConstants.PlayerState.PAUSED)

            // Sveglia: se la riproduzione si interrompe (pausa dell'utente dalla
            // notifica media o dai controlli), la sessione sveglia termina e la
            // notifica Snooze/Stop viene rimossa. La sessione viene chiusa SOLO
            // se la sveglia ha cominciato effettivamente a suonare
            // ([alarmSessionHeardPlayback]): durante l'avvio (buffering
            // iniziale, bridge YouTube da riattivare con il servizio già vivo)
            // l'evento "not playing" è NORMALE e chiudere la sessione lì
            // ucciderebbe la sveglia prima che il brano parta.
            if (alarmSessionAlarmId != null && alarmSessionHeardPlayback) {
                val alsoIsPlaying = hybridPlayer.isPlaying
                serviceScope.launch {
                    delay(2_500.milliseconds)
                    if (alarmSessionAlarmId != null && !alsoIsPlaying && !alarmFadeInProgress)
                        endAlarmSession("riproduzione interrotta")
                }
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
            val audioSessionId = getAudioSessionId()
            //if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return@runCatching

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
            val audioSessionId = getAudioSessionId()
            //if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return@runCatching

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

        playbackWathcDogJob = this@PlayerService.serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(200.milliseconds)

                val duration = hybridPlayer.duration
                val position = hybridPlayer.currentPosition
                val isPlaying = _playerState.value.playbackState == PlaybackState.PLAYING

                // Il guardiano calcola i dati SOLO se c'è una canzone caricata nel player
                if (duration > 0) {

                    val timeLeft = duration - position
                    val crossfadeDurationMs = appSettings.crossfadeDuration.milliseconds

                    // LE LOGICHE DI FINE BRANO SCATTANO SOLO SE L'APP STA EFFETTIVAMENTE SUONANDO
                    // La logica di fine brano la limitiamo a EXOPLAYER perchè abbiamo mmesso la pausa tra i mediaitems per non creare doppi salti
                    if (isPlaying && hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {
                        // SE IL CROSSFADE È ATTIVO
                        //Timber.d("PlayerService PlaybackWatchdog: engine = ${hybridPlayer.activeEngine} isFading = $isFading isPlaying = $isPlaying isPaused = $isPaused timeleft $timeLeft duration=$duration ms, position=$position ms")
                        if (appSettings.crossfadeDuration != CrossfadeDuration.Off) {
                            if (timeLeft <= crossfadeDurationMs && timeLeft > -10000 && !isFading) {
                                isFading = true
                                val nextMediaItemIndex = hybridPlayer.nextMediaItemIndex
                                if (nextMediaItemIndex == -1) return@launch // se è nullo non c'è nulla da fare
                                val nextMediaItem = hybridPlayer.getMediaItemAt(nextMediaItemIndex)
                                val isNextLocal = nextMediaItem.isLocal

                                Timber.d("PlayerService PlaybackWatchdog: Attivazione Fade Out ($timeLeft ms). Prossimo locale=$isNextLocal")
                                lastWatchdogPosition = -1L // resetto la posizione precedente durante il cambio

                                handleEndOfSong("cambio con crossfade attivo")

                            }
                        }
                        // SE IL CROSSFADE È DISATTIVATO
                        else {
                            val isPlayerStalled = timeLeft <= 3500
                                    && position == lastWatchdogPosition
                                    && position > 2000 // Protezione: lo stallo non può avvenire nei primi 2 secondi!


                            // USIAMO UNA SOGLIA APERTA PER CONTRASTARE LE CHIAMATE MULTIPLE
                            // La fine naturale può scattare SOLO se la canzone
                            // è avviata da almeno 5 secondi (position > 5000).
                            // Questo impedisce al Watchdog di confondersi con i vecchi dati hardware
                            // nei primissimi millisecondi del cambio traccia!
                            val isNaturalEnd = timeLeft <= 1200 && position > 5000

                            if ((isNaturalEnd || isPlayerStalled) && !isFading) {
                                isFading = true
                                if (isPlayerStalled) {
                                    Timber.w("PlayerService PlaybackWatchdog: RILEVATO STALLO FINE TRACCIA a $timeLeft ms. Forzo il cambio!")
                                } else {
                                    Timber.d("PlayerService PlaybackWatchdog: Fine brano naturale a $timeLeft ms")
                                }
                                lastWatchdogPosition = -1L // resetto la posizione precedente durante il cambio

                                handleEndOfSong("cambio con crossfade disattivo")

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


    suspend fun handleEndOfSong(source: String) {
        isFading = true
        lastWatchdogPosition = -1L

        // I/O fuori dalla finestra (già concordato): fire-and-forget, non blocca il next
        if (appSettings.forceUserVideoPlayback) {
            serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(forceUserVideoPlayback = false))
            }
        }
        if (processQueueRepeat()) return

        // Calcoliamo il prossimo riproducibile controllando tra i non riproducibili
        val targetIndex = nextPlayableIndexAfter(hybridPlayer.currentMediaItemIndex)
        if (targetIndex == null) {
            // Fine coda reale OPPURE tutte le successive marcate, stop onesto non c'è altro da riprodurre
            if (hybridPlayer.activeEngine == ActiveEngine.YOUTUBE) {
                hybridPlayer.youtubePlayWhenReady = false
                hybridPlayer.invalidateYouTubePlayPause()
            } else {
                exoPlayer.pause()  // Exo è nativamente in ENDED: basta fermare
            }
            isFading = false   // Sblocchiamo il watchdog in modo da non tenerlo disattivato per sempre
            SmartMessage(getString(R.string.no_playable_tracks_message), PopupType.Warning, context = this@PlayerService)
            return
        }

        if (appSettings.crossfadeDuration != CrossfadeDuration.Off) {
            val isNextLocal = hybridPlayer.getMediaItemAt(targetIndex).isLocal
            if (isNextLocal) startExoToExoCrossfade(targetIndex) else startWebViewFadeOut(targetIndex)
        } else {
            handlePlayNext(
                "handleEndOfSong chiamato da $source targetIndex = $targetIndex",
                targetIndex
            )
        }
    }


    private fun processQueueRepeat(): Boolean { // Ritorna true se esegue il cambio di canzone
        when (appSettings.queueLoopType) {
            QueueLoopType.RepeatOne -> {
                isFading = true
                hybridPlayer.seekTo(0)

                // FIX: il riavvio del brano qui NON passa da onMediaItemTransition né da un
                // nuovo evento PLAYING affidabile, quindi il volume va ripristinato con lo
                // stesso schema del playPrevious: con crossfade attivo fa la rampa startFadeIn()
                // (che al termine azzera isFading), senza crossfade si ripristina subito il
                // volume ATTRAVERSO HybridPlayer.
                startFadeIn()
                if (appSettings.crossfadeDuration == CrossfadeDuration.Off) {
                    hybridPlayer.setFadeVolume(1.0f)
                    isFading = false
                }
                return true
            }

            QueueLoopType.RepeatAll -> {
                if (currentQueuePosition == hybridPlayer.mediaItemCount-1){
                    isFading = true
                    hybridPlayer.seekToDefaultPosition(0)
                    return true
                } else return false
            }

            else -> return false
        }
    }

    private fun stopPlaybackWatchdog() {
        playbackWathcDogJob?.cancel()
        playbackWathcDogJob = null
        isFading = false // Sblocchiamo il fading
        //Timber.d("PlayerService PlaybackWatchdog: Cancello crossfadeJob")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══ GESTIONE CONNETTIVITÀ ONLINE: PAUSA / RIPRESA / RECOVERY ══════════
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Obiettivi:
    //  1. Rete assente -> pausa ONESTA del motore online (la notifica e l'UI
    //     mostrano "pausa" invece di fingere di suonare)
    //  2. Rete ritornata -> ripresa AUTOMATICA dal secondo esatto in cui si era
    //     fermato, SOLO se la riproduzione era attiva/desiderata
    //  3. Handover WiFi<->Cellulare -> tollerato con una finestra di grazia
    //     (non fermiamo se la rete rientra da sola entro pochi secondi)
    //  4. Stalli residui (iframe morto, buffering infinito, WebView uccisa)
    //     -> escalation graduale: seek -> loadVideo -> ricreazione WebView
    //  5. Errori HTML_5_PLAYER durante un'interruzione -> TRANSITORI:
    //     nessuno skip della coda, nessuna marcatura "unplayable"
    //  6. La volontà dell'utente ha SEMPRE la precedenza: se mette in pausa
    //     durante un'interruzione, non riprendiamo al ritorno della rete
    //
    // ═══════════════════════════════════════════════════════════════════════

    /** True se il playback è guidato da un cast (RiTune/Chromecast): la rete
     *  del dispositivo non governa il player remoto, non interveniamo. */
    private fun isCastDrivingPlayback(): Boolean {
        if (GlobalSharedData.riTuneCastActive &&
            riTuneCastClient.connectionStatus.value == RiTuneConnectionStatus.Connected
        ) return true
        return CastHelper.isCastAvailable &&
                appSettings.castType !in listOf(CastType.NONE, CastType.RITUNECAST)
    }

    /** True se c'è una chiamata in corso (il ciclo resumePending dedicato alle
     *  chiamate ha già la sua gestione, non la duplichiamo). */
    private fun isPhoneCallActive(): Boolean =
        ::audioManager.isInitialized && audioManager.mode != AudioManager.MODE_NORMAL

    /** Registra il monitor di connettività e avvia il watchdog anti-stallo. */
    private fun initializeOnlineConnectivityHandling() {
        if (connectivityListener != null) return // già inizializzato

        val listener = object : OnlineConnectivityMonitor.Listener {
            override fun onNetworkLost() {
                // I callback di sistema arrivano su un thread dedicato:
                // riposizioniamo la policy sul Main thread del service
                serviceScope.launch(Dispatchers.Main) { handleNetworkLost() }
            }

            override fun onNetworkRestored() {
                serviceScope.launch(Dispatchers.Main) { handleNetworkRestored() }
            }
        }

        connectivityListener = listener
        onlineConnectivityMonitor.addListener(listener)
        onlineConnectivityMonitor.start()

        startOnlineStallWatchdog()
    }

    /** Rete appena perduta: valutiamo se fermare il motore online. */
    private fun handleNetworkLost() {
        lastNetworkLostAtMs = System.currentTimeMillis()

        if (isCastDrivingPlayback()) return

        val song = currentSong.value
        if (song == null || song.isLocal) return

        // EXOPLAYER su sorgente remota (WebDAV): pausa preventiva invece di
        // lasciare Media3 stantuffare in buffering fino all'errore IO
        if (hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {
            if (song.isWebDav && hybridPlayer.isPlaying) {
                pausedForConnectivity = true
                resumeDesiredAfterConnectivity = true
                Timber.w("PlayerService Connettività: rete persa su stream WebDAV, pausa preventiva di ExoPlayer")
                runCatching { exoPlayer.pause() }
                    .onFailure { Timber.e("PlayerService Connettività: pausa ExoPlayer fallita: ${it.message}") }
            }
            return
        }

        // Interviene solo se la riproduzione era attiva o desiderata
        val wantsPlayback = hybridPlayer.youtubePlayWhenReady || _playerState.value.isPlaying
        if (!wantsPlayback) {
            Timber.d("PlayerService Connettività: rete persa ma il player era fermo, nessuna azione")
            return
        }

        // FINESTRA DI GRAZIA: l'handover WiFi<->Cellulare genera onLost+onAvailable
        // ravvicinati. Se la rete rientra da sola entro la finestra, il playback
        // prosegue indenne; se la finestra scade senza rete, pausa onesta.
        networkGraceJob?.cancel()
        networkGraceJob = serviceScope.launch(Dispatchers.Main) {
            delay(NETWORK_LOSS_GRACE_MS.milliseconds)
            if (!onlineConnectivityMonitor.isNetworkAvailable && !isCastDrivingPlayback()) {
                pauseForConnectivityLoss()
            }
        }
    }

    /** Pausa ONESTA del motore online dopo la conferma di assenza di rete. */
    private fun pauseForConnectivityLoss() {
        if (pausedForConnectivity) return
        pausedForConnectivity = true

        resumeDesiredAfterConnectivity =
            hybridPlayer.youtubePlayWhenReady || _playerState.value.isPlaying

        // Congeliamo la baseline del watchdog: mentre siamo offline niente escalation
        lastProgressAtMs = System.currentTimeMillis()
        onlineRecoveryLevel = 0
        onlineRecoveryNextAtMs = 0L

        Timber.w("PlayerService Connettività: rete assente CONFERMATA, pausa del motore online (resumeDesired=$resumeDesiredAfterConnectivity, second=${_currentSecond.value})")

        // Pausa DIRETTA del player AYP: bypassa YouTubeControlImpl così
        // userInitiatedPause NON viene alzato e nel PAUSED riusciamo a
        // distinguere la nostra pausa da quella dell'utente
        runCatching { _internalYouTubePlayer.value?.pause() }
            .onFailure { Timber.e("PlayerService Connettività: pausa AYP fallita: ${it.message}") }

        hybridPlayer.youtubePlayWhenReady = false
        if (::hybridPlayer.isInitialized) hybridPlayer.invalidateYouTubePlayPause()

        // Notifica/widget/Android Auto aggiornati allo stato REALE (pausa)
        updatePlayerState(PlayerConstants.PlayerState.PAUSED)
    }

    /** Rete appena ritornata: programmiamo la ripresa con stabilizzazione. */
    private fun handleNetworkRestored() {
        lastNetworkRestoredAtMs = System.currentTimeMillis()

        // La finestra di grazia non serve più: la rete è rientrata
        networkGraceJob?.cancel()

        if (isCastDrivingPlayback()) return
        // Interveniamo se abbiamo messo in pausa NOI, se l'iframe si è auto-pausato
        // per la rete assente (resumeDesired) o se c'è un load di recovery in attesa
        if (!pausedForConnectivity && !resumeDesiredAfterConnectivity && pendingRecoveryLoadVideoId == null) return

        // STABILIZZAZIONE: il callback di sistema arriva PRIMA che la rete sia
        // realmente utilizzabile (DHCP/DNS/registrazione radio). Un play lanciato
        // subito qui farebbe fallire la rinegoziazione dello stream.
        connectivityResumeJob?.cancel()
        connectivityResumeJob = serviceScope.launch(Dispatchers.Main) {
            delay(NETWORK_STABILIZE_MS.milliseconds)

            // Doppio controllo anti-flapping: se durante l'attesa è ricaduta, esce
            if (!onlineConnectivityMonitor.isNetworkAvailable) return@launch
            resumeAfterConnectivityRestored()
        }
    }

    /** Ripresa automatica dopo il ripristino della rete. */
    private fun resumeAfterConnectivityRestored() {
        // Ricopre entrambi i casi: pausa eseguita dal Service (pausedForConnectivity)
        // o auto-pausa dell'iframe per rete assente (resumeDesiredAfterConnectivity)
        if (!pausedForConnectivity && !resumeDesiredAfterConnectivity) return
        pausedForConnectivity = false

        val song = currentSong.value
        if (song == null || song.isLocal) return

        Timber.i("PlayerService Connettività: rete ripristinata, ripresa del playback online (resumeDesired=$resumeDesiredAfterConnectivity, mediaId=${song.id}, second=${_currentSecond.value})")

        if (hybridPlayer.activeEngine == ActiveEngine.EXOPLAYER) {
            // Era lo stream WebDAV messo in pausa per la mancanza di rete
            if (resumeDesiredAfterConnectivity) {
                runCatching { exoPlayer.play() }
                    .onFailure { Timber.e("PlayerService Connettività: play ExoPlayer fallito: ${it.message}") }
            }
            return
        }

        if (!resumeDesiredAfterConnectivity) {
            // L'utente ha messo in pausa durante l'interruzione: solo ripristino flag
            Timber.i("PlayerService Connettività: nessuna auto-ripresa, l'utente ha messo in pausa")
            return
        }

        val videoId = song.id
        val positionSecond = _currentSecond.value.coerceAtLeast(0f)

        if (_internalYouTubePlayer.value == null) {
            // WebView mai pronta (avvio offline) o ricreata dal watchdog:
            // il load partirà da onReady appena l'IFrame API sarà caricata
            pendingRecoveryLoadVideoId = videoId
            pendingRecoveryLoadSecond = positionSecond
            setLoadPending(LoadPhase.PENDING, "network-restored: in attesa di onReady della WebView")
            return
        }

        // KICK MORBIDO: seekTo sulla posizione esatta + play(). L'iframe riparte
        // dal punto in cui si era fermato rinegoziando lo stream. Gli stalli
        // residui li intercetta il watchdog con escalation graduale.
        setLoadPending(LoadPhase.PENDING, "network-restored kick")
        runOnlineRecoveryKick(positionSecond, "rete-ripristinata")
    }

    /** Kick di ripresa sul motore YouTube (da chiamare sul Main thread). */
    private fun runOnlineRecoveryKick(positionSecond: Float, reason: String) {
        val player = _internalYouTubePlayer.value ?: return
        Timber.i("PlayerService Connettività: kick online ($reason) a ${positionSecond}s")

        runCatching {
            player.seekTo(positionSecond)
            player.play()
        }.onFailure { Timber.e("PlayerService Connettività: kick fallito ($reason): ${it.message}") }

        hybridPlayer.youtubePlayWhenReady = true
        if (::hybridPlayer.isInitialized) hybridPlayer.invalidateYouTubePlayPause()

        // Nuova baseline: da qui il watchdog misura di nuovo la quiete
        lastProgressSecond = positionSecond
        lastProgressAtMs = System.currentTimeMillis()
    }

    /** Watchdog dedicato agli stalli del motore online (tick 2s). */
    private fun startOnlineStallWatchdog() {
        onlineStallWatchdogJob?.cancel()
        onlineStallWatchdogJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(2000.milliseconds)
                evaluateOnlineStall()
            }
        }
    }

    /**
     * Rileva il classico stallo mobile: la rete c'è, il player "vuole" suonare,
     * ma la posizione webview non avanza da ONLINE_STALL_THRESHOLD_MS (buffering
     * infinito dopo handover, iframe morto, renderer ucciso dal sistema...).
     * Interviene con escalation graduale, MAI con loop infiniti:
     *  livello 1: seekTo(posizione)+play() (kick morbido)
     *  livello 2: loadVideo(id, posizione) (rinegoziazione completa dello stream)
     *  livello 3: ricreazione della WebView + load dall'onReady
     */
    private fun evaluateOnlineStall() {
        val now = System.currentTimeMillis()

        // 1. Aggiorniamo la traccia dell'avanzamento della posizione webview
        val second = _currentSecond.value
        if (second != lastProgressSecond) {
            lastProgressSecond = second
            lastProgressAtMs = now
        }

        if (isCastDrivingPlayback()) return
        if (pausedForConnectivity) return
        if (!onlineConnectivityMonitor.isNetworkAvailable) return
        if (isPhoneCallActive()) return
        if (isFading) return

        val song = currentSong.value ?: return
        if (song.isLocal) return
        if (hybridPlayer.activeEngine != ActiveEngine.YOUTUBE) return

        // Interviene solo se la riproduzione è desiderata (anche ferma in buffering)
        if (!hybridPlayer.youtubePlayWhenReady && !_playerState.value.isPlaying) return

        val duration = _currentDuration.value
        // Niente escalation negli ultimi 4 secondi: l'ENDED arriverà da solo
        if (duration > 0f && second >= duration - 4f) return

        val stalledFor = now - lastProgressAtMs
        if (stalledFor < ONLINE_STALL_THRESHOLD_MS) {
            // Stiamo avanzando normalmente: azzera la catena di escalation
            if (onlineRecoveryLevel != 0) resetOnlineRecoveryState("posizione in avanzamento")
            return
        }

        // Se siamo passati a un'altra traccia riparte la catena da zero.
        // Il primo kick scatta SUBITO alla rilevazione (la soglia di stallo
        // è già maturata): le attese valgono solo tra i livelli successivi.
        if (onlineRecoveryMediaId != song.id) {
            onlineRecoveryMediaId = song.id
            onlineRecoveryLevel = 0
            onlineRecoveryNextAtMs = now
        }

        if (onlineRecoveryLevel >= 3) {
            // WebView già ricreata: aspettiamo onReady, niente loop infiniti
            return
        }

        if (now < onlineRecoveryNextAtMs) return

        when (onlineRecoveryLevel) {
            0 -> {
                Timber.w("PlayerService Connettività: STALLO online da ${stalledFor / 1000}s (second=$second, id=${song.id}). Livello 1: seek+play")
                onlineRecoveryLevel = 1
                onlineRecoveryNextAtMs = now + ONLINE_RECOVERY_STEP_DELAY_MS
                setLoadPending(LoadPhase.PENDING, "watchdog-livello1-seek")
                runOnlineRecoveryKick(second, "watchdog-livello1-seek")
            }

            1 -> {
                Timber.w("PlayerService Connettività: stallo persiste dopo il seek. Livello 2: loadVideo id=${song.id} a ${second}s")
                onlineRecoveryLevel = 2
                onlineRecoveryNextAtMs = now + ONLINE_RECOVERY_STEP_DELAY_MS
                setLoadPending(LoadPhase.PENDING, "watchdog-livello2-loadVideo")
                runCatching { _internalYouTubePlayer.value?.loadVideo(song.id, second) }
                    .onFailure { Timber.e("PlayerService Connettività: loadVideo fallito: ${it.message}") }
                hybridPlayer.youtubePlayWhenReady = true
                lastProgressAtMs = now
            }

            else -> {
                Timber.e("PlayerService Connettività: stallo persiste dopo il loadVideo. Livello 3: ricreazione WebView")
                onlineRecoveryLevel = 3
                onlineRecoveryNextAtMs = now + ONLINE_RECREATE_STEP_DELAY_MS
                setLoadPending(LoadPhase.PENDING, "watchdog-livello3-recreate")
                pendingRecoveryLoadVideoId = song.id
                pendingRecoveryLoadSecond = second
                recreateOnlinePlayerView()
            }
        }
    }

    /** Azzera lo stato di recovery: su PLAYING, cambio traccia, posizione che avanza. */
    private fun resetOnlineRecoveryState(reason: String) {
        if (onlineRecoveryLevel != 0 || pendingRecoveryLoadVideoId != null) {
            Timber.d("PlayerService Connettività: reset stato recovery ($reason)")
        }
        onlineRecoveryLevel = 0
        onlineRecoveryNextAtMs = 0L
        onlineRecoveryMediaId = null
        pendingRecoveryLoadVideoId = null
        pendingRecoveryLoadSecond = 0f
        lastProgressSecond = -1f
        lastProgressAtMs = System.currentTimeMillis()
    }

    // ═════════════════ fine gestione connettività online ═════════════════

    private fun startFadeIn() {
        if (appSettings.crossfadeDuration == CrossfadeDuration.Off) return

        fadeInJob?.cancel() // Cancelliamo eventuali fade-in residui

        isFading = true
        hybridPlayer.setFadeVolume(0f) // Inizia dal silenzio totale

        fadeInJob = this@PlayerService.serviceScope.launch(Dispatchers.Main) {
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


    private fun startExoToExoCrossfade(targetIndex: Int) {
        fadeInJob?.cancel() // Cancelliamo eventuali fade-in residui

        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
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
            handlePlayNext("startExoToExoCrossfade", targetIndex)
        }
    }

    private fun startWebViewFadeOut(targetIndex: Int) {
        fadeInJob?.cancel()

        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
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
            handlePlayNext("startWebViewFadeOut", targetIndex)
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

            // Canale sveglia: heads-up per avvisare subito che la sveglia è suonata
            if (getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        ALARM_NOTIFICATION_CHANNEL_ID,
                        ALARM_NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        setSound(null, null)
                        enableLights(true)
                        enableVibration(true)
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

        val okHttpClient = OkHttpClient.Builder()
            .proxy(Environment.proxy)
            .apply {
                if (appSettings.isWebDavEnabled)
                    addInterceptor(DynamicWebDavAuthInterceptor())
                // Configuriamo un interceptor che recupera le credenziali di autenticazione webdav
                // dall'account webdav corrispondente all'url della canzone da riprodurre
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

    /**
     * Raccoglie lo stato corrente del player e lo pubblica alla pipeline widget.
     * Non scrive più direttamente nei datastore: il debounce, la deduplica e la
     * scrittura su tutte le istanze sono delegati a WidgetUpdater.
     */
    fun updateWidgetState() {
        // Correzione 2.1.1: qualunque eccezione qui dentro (lettura stato player durante
        // una transizione, artwork, datastore) arrivava all'UncaughtExceptionHandler e
        // uccideva l'intero processo: la musica si fermava, tipicamente a schermo spento
        // quando gli eventi del player arrivano a raffica. I widget sono un consumer di
        // sola lettura: un loro errore non deve MAI influire sulla riproduzione.
        this@PlayerService.serviceScope.launch {
            runCatching { publishWidgetSnapshotSafely() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "PlayerService updateWidgetState: pubblicazione widget saltata, riproduzione non influenzata")
                }
        }
    }

    private suspend fun publishWidgetSnapshotSafely() {
        if (!::exoPlayer.isInitialized) {
            Timber.w("PlayerService updateWidgetState invocato ma il player non è ancora pronto. Salto l'aggiornamento.")
            return
        }

        val title = withContext(Dispatchers.Main) { cleanPrefix(exoPlayer.mediaMetadata.title.toString()) }
        val artist = withContext(Dispatchers.Main) { exoPlayer.mediaMetadata.artist.toString() }
        val mediaId = withContext(Dispatchers.Main) { exoPlayer.currentMediaItem?.mediaId }
        val isPlaying = _playerState.value.isPlaying
        val shuffleEnabled = withContext(Dispatchers.Main) { exoPlayer.shuffleModeEnabled }
        // Posizione e durata dipendono dall'engine attivo: durante lo streaming
        // ExoPlayer resta fermo, la fonte valida è il watcher interno (_currentSecond).
        val positionMs = withContext(Dispatchers.Main) {
            if (currentSong.value?.isLocal == true) exoPlayer.currentPosition.coerceAtLeast(0L)
            else (_currentSecond.value * 1000f).toLong().coerceAtLeast(0L)
        }
        val durationMs = withContext(Dispatchers.Main) {
            if (currentSong.value?.isLocal == true) (exoPlayer.duration.takeIf { it > 0 } ?: 0L)
            else (_currentDuration.value * 1000f).toLong().coerceAtLeast(0L)
        }
        val queueIndex = withContext(Dispatchers.Main) { exoPlayer.currentMediaItemIndex }
        val queueSize = withContext(Dispatchers.Main) { exoPlayer.mediaItemCount }

        val likedState = when (currentSong.value?.likedAt) {
            null -> LikeState.NONE
            -1L -> LikeState.DISLIKED
            else -> LikeState.LIKED
        }

        // La copertina ridimensionata viene salvata una sola volta in cache:
        // nei widget viaggia solo il percorso del file.
        // bitmapProvider.bitmap non è mai null: restituisce un placeholder tinta
        // unita (bianco/nero) finché l'artwork non è caricato. Per i widget usiamo
        // lastBitmap, che è null finché l'immagine vera non è pronta.
        val artworkPath = WidgetArtworkCache.save(
            context = this@PlayerService,
            mediaId = mediaId,
            source = bitmapProvider?.lastBitmap
        )

        WidgetUpdater.push(
            PlayerWidgetSnapshot(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                artworkPath = artworkPath,
                likedState = likedState,
                shuffleEnabled = shuffleEnabled,
                positionMs = positionMs,
                durationMs = durationMs,
                queueIndex = queueIndex,
                queueSize = queueSize,
            )
        )
    }

    /**
     * Collega la pipeline widget allo scope del servizio e avvia il ticker che
     * aggiorna la posizione del brano (solo mentre il brano è in riproduzione
     * e solo nel widget grande, se aggiunto alla home).
     */
    private fun initializeWidgetUpdater() {
        WidgetUpdater.attach(
            context = applicationContext,
            scope = serviceScope,
            settings = appSettingsManager.activeSettings
        )

        // Il like (da widget, notifica o UI) scrive sul database: currentSong è un
        // flow live del DB e riemette il brano aggiornato, quindi ripubblichiamo
        // subito lo stato senza aspettare un evento di pausa/riproduzione.
        this@PlayerService.serviceScope.launch {
            currentSong.collect { song ->
                if (song != null) updateWidgetState()
            }
        }

        // Ticker posizione: posizione e durata dipendono dall'engine attivo
        // (ExoPlayer per i file locali, watcher interno per YouTube/Cast),
        // altrimenti il tempo mostrato nei widget resta fermo durante lo streaming.
        // Correzione 2.1.1: ogni iterazione è isolata; un errore qui non deve mai
        // terminare né il ticker né il processo (il debounce delle scritture e il
        // congelamento a schermo spento sono gestiti dentro WidgetUpdater).
        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                try {
                    if (::exoPlayer.isInitialized && _playerState.value.isPlaying) {
                        val isLocal = currentSong.value?.isLocal == true
                        WidgetUpdater.pushPosition(
                            positionMs = if (isLocal) exoPlayer.currentPosition.coerceAtLeast(0L)
                                         else (_currentSecond.value * 1000f).toLong().coerceAtLeast(0L),
                            durationMs = if (isLocal) (exoPlayer.duration.takeIf { it > 0 } ?: 0L)
                                         else (_currentDuration.value * 1000f).toLong().coerceAtLeast(0L)
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "PlayerService ticker posizione widget: iterazione saltata, riproduzione non influenzata")
                }
                delay(1_000)
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

        this@PlayerService.serviceScope.launch {
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
                timerJob = this@PlayerService.serviceScope.timer(remainingMillis) {
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
                    mediaItemBytes = mediaItem.toMediaItemBytes() ?: ByteArray(0),
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
        withContext(Dispatchers.Main) {
            Timber.d("PlayerService LoadQueue: Avvio caricamento coda persistente asincrona")
        }
        if (!appSettings.persistentQueue) return

        // 1. Eseguiamo la query pesante nel thread IO
        val queuedSongs = withContext(Dispatchers.IO) {
            Database.clearOldEmptyQueuedMediaItems()
            try { Database.queuedMediaItems() } catch (e: Exception) { emptyList() }
        }

        if (queuedSongs.isEmpty()) {
            withContext(Dispatchers.Main) {
                Timber.d("PlayerService LoadQueue: Nessun brano salvato nel database.")
            }
            return
        }

        val index = queuedSongs.indexOfFirst { (it.position ?: 0L) >= 0L }.coerceAtLeast(0)
        val queuedSong = queuedSongs[index]
        val position = if (queuedSong.mediaItemCompat?.isLocal == true) {
            queuedSong.position ?: C.TIME_UNSET
        } else {
            (queuedSong.position ?: 0L) / 1000
        }



        withContext(Dispatchers.Main) {
            Timber.d("PlayerService LoadQueue: Dati DB pronti. Ripristino indice: $index, Posizione: $position")
            val mappedItems = queuedSongs.map { mediaItem ->
                val song = mediaItem.mediaItemCompat ?: androidx.media3.common.MediaItem.EMPTY
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

            exoPlayer.setMediaItems(mappedItems, index, if (queuedSong.mediaItemCompat?.isLocal == true) position else 0)
            exoPlayer.prepare()

            // valorizza la variabile globale lastOnlineMediaId in modo da non riprodurre la stessa canzone
            lastOnlineMediaId = queuedSong.mediaId

            if (queuedSong.mediaItemCompat?.isLocal != true) {
                val duration = try { appSettings.stateDuration } catch (e: Exception) { 0f }
                val mId = appSettings.stateMediaId

                playFromSecond = position.toFloat()
                _currentSecond.value = playFromSecond
                _currentDuration.value = if (queuedSong.mediaId == mId) duration else 0f
                hybridPlayer.pause()
            }

            Timber.d("PlayerService LoadQueue: Ripristino della coda completato con successo.")
        }
    }


    private fun updateMusicVaultMediaItem(
        songId: String,
        fileName: String,
        thumbnailFileName: String
    ) {
        this@PlayerService.serviceScope.launch {
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

    fun setLoadPending(phase: LoadPhase, reason: String) {
        val cur = _playerState.value
        val now = System.currentTimeMillis()
        Timber.w("PlayerService LOAD-PENDING %s→%s reason=%s state=%s Δsince=%d ΔlastAct=%d",
            cur.loadPending, phase, reason, cur.playbackState,
            now - cur.loadPendingSince, now - cur.loadPendingLastActivity)

        val currentState = _playerState.value
        _playerState.value = currentState.copy(
            loadPending = phase,
            loadPendingSince = if (phase == LoadPhase.PENDING) System.currentTimeMillis() else 0L,
            loadPendingLastActivity = if (phase == LoadPhase.PENDING) System.currentTimeMillis() else 0L,
        )
    }

    @Stable
    open inner class Binder : AndroidBinder() {

        val serviceScope: CoroutineScope
            get() = this@PlayerService.serviceScope

        val hybridPlayer: HybridPlayer
            get() = this@PlayerService.hybridPlayer

        val playerState: StateFlow<PlayerState>
            get() = this@PlayerService.playerState

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

        val unplayableTracks: StateFlow<Set<String>>
            get() = this@PlayerService.unplayableIds

        val currentMediaItemAsSong: Song?
            get() = this@PlayerService.currentSong.value

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

        val isLoadingRadio: StateFlow<Boolean>
            get() = this@PlayerService.isLoadingRadio

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
            this@PlayerService.serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(timerEndTime = endTime))
            }

            Timber.d("PlayerService startAutoCloseTimer delayMillis $delayMillis, pianificato per timestamp: $endTime")

            timerJob = this@PlayerService.serviceScope.timer(delayMillis) {
                Timber.d("PlayerService: Timer multiuso terminato naturalmente")
                executeAutoCloseLogic()
            }
        }

        fun executeAutoCloseLogic() {
            this@PlayerService.serviceScope.launch(Dispatchers.Main) {

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
            this@PlayerService.serviceScope.launch {
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
                this@PlayerService.serviceScope
            ).let {
                _isLoadingRadio.value = true
                radioJob = this@PlayerService.serviceScope.launch(Dispatchers.Main) {

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
                        Database.asyncTransaction { Database.insert(it) }
                    }

                    if (justAdd) {
                        hybridPlayer.addMediaItems( songs.drop(1))
                    } else {
                        hybridPlayer.forcePlayFromBeginning(songs)
                    }
                    radio = it
                    _isLoadingRadio.value = false
                }
            }
        }

        fun stopRadio() {
            _isLoadingRadio.value = false
            radioJob?.cancel()
            radio = null
        }

        fun playFromSearch(query: String) {
            this@PlayerService.serviceScope.launch {
                Environment.searchPage(
                    body = SearchBody(
                        query = query,
                        params = Environment.SearchFilter.Song.value
                    ),
                    fromMusicShelfRendererContent = Environment.SongItem.Companion::fromMusicShelfRenderer
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
            this@PlayerService.serviceScope.launch {
                withContext(Dispatchers.Main) {
                    hybridPlayer.shuffleModeEnabled.let { hybridPlayer.shuffleModeEnabled = !it }
                }
                // Ripubblica subito lo stato così l'icona shuffle nel widget
                // cambia senza dover mettere in pausa e riprendere.
                updateWidgetState()
            }

        }

        fun toggleRepeat() {
            val queueLoopType = appSettings.queueLoopType
            val newQueueLoopType = setQueueLoopState(queueLoopType)
            val repeatMode = newQueueLoopType.type
            this@PlayerService.serviceScope.launch {
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

        fun notifyAndroidAutoChildrenChanged(parentId: String) = this@PlayerService.notifyAutoChildrenChanged(parentId)

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

    fun handlePlayNext(origine: String? = null, targetIndex: Int? = null) {
        Timber.d("PlayerService PlaybackWatchdog: handlePlayNext cambio brano richiesto da $origine")

        // Se l'utente aveva richiesto la visualizzazione di un video, quando passiamo avanti lo resettiamo
        if (appSettings.forceUserVideoPlayback) {
            this@PlayerService.serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(forceUserVideoPlayback = false))
            }
        }

        // Prima di cambiare brano, azzeriamo la variabile storica nel Service
        // così il Watchdog sa che la nuova traccia deve ricominciare a fare i calcoli da zero!
        lastWatchdogPosition = -1L

        val now = System.currentTimeMillis()
        if (now - lastPlayNextTime < debounceDelayMs) {
            isFading = false
            Timber.d("PlayerService handlePlayNext ignored (too fast) play current")
            // play() SOLO se il player è fermo su READY: nessuna transizione in volo
            if (hybridPlayer.playbackState == Player.STATE_READY && !hybridPlayer.isPlaying) {
                hybridPlayer.play()
            }
            return
        }
        lastPlayNextTime = now
        Timber.d("PlayerService handlePlayNext executed")

        playFromSecond = 0f

        // Resettiamo lo stato di fade prima di lanciare la nuova canzone,
        // così onMediaItemTransition e startFadeIn()
        isFading = false

        this@PlayerService.serviceScope.launch {
            withContext(Dispatchers.Main) {
                // USO IL PLAYER REALE SOTTOSTANTE (exoPlayer)
                // per eseguire il vero salto atomico nella timeline di Media3,
                // bypassando l'override di HybridPlayer ed evitando il loop!
                // Va considerato l'eventuale index specificato dall'eventuale errore permanente di unplayable
                // Se l'index non viene passato, si va al successivo mediaitem
                if (targetIndex != null) {
                    exoPlayer.seekToDefaultPosition(targetIndex)   // salta DOVE viene indicato come riproducibile
                } else {
                    exoPlayer.seekToNextMediaItem()
                }
            }
        }
    }

    fun handlePlayPrevious() {
        val now = System.currentTimeMillis()
        if (now - lastPlayPreviousTime < debounceDelayMs) {
            Timber.d("PlayerService handlePlayPrevious: Click filtrato (troppo veloce)")
            return
        }

        // Se l'utente aveva richiest la visualizzazione di un video, quando passiamo avanti lo resettiamo
        if (appSettings.forceUserVideoPlayback) {
            this@PlayerService.serviceScope.launch {
                appSettingsManager.updateSettings(appSettings.copy(forceUserVideoPlayback = false))
            }
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
                this@PlayerService.serviceScope.launch(Dispatchers.Main) {
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
        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
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

        // Estraiamo la posizione attuale PRIMA di avviare la coroutine
        val currentPositionMs = hybridPlayer.currentPosition

        // Leggiamo il tempo di riavvolgimento impostato dall'utente
        val rewindThreshold = appSettings.rewindThresholdDuration.milliSeconds

        // Dissolvenza lampo simmetrica per il tasto "Indietro"
        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
            val steps = 6
            val quickFadeDurationMs = 250
            val stepDelay = quickFadeDurationMs / steps

            Timber.d("PlayerService: Quick Fade-Out UTENTE (Indietro) avviato da $source")

            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setFadeVolume(progress * progress)
                delay(stepDelay.milliseconds)
            }

            // Silenzio assoluto garantito sui buffer
            hybridPlayer.setFadeVolume(0f)

            if (currentPositionMs > rewindThreshold) {
                Timber.d("PlayerService UX: Canzone avviata da >$rewindThreshold Eseguo il riavvolgimento morbido dello stesso brano.")

                // Diciamo a Media3 di resettare il tempo della canzone corrente
                hybridPlayer.seekTo(0L)

                // Resettiamo la timeline storicizzata così il Watchdog perenne sa che il salto a zero è voluto
                lastWatchdogPosition = -1L

                // Facciamo ripartire la rampa di risalita morbida dell'audio!
                // Questa funzione al termine della risalita metterà regolarmente 'isFading = false', sbloccando l'app!
                // Parte solo se fade è abilitato
                startFadeIn()

                // FIX volume azzerato sul riavvio del brano (playPrevious online):
                // con crossfade OFF startFadeIn() esce subito senza toccare nulla, quindi
                // il volume DEVE essere ripristinato ATTRAVERSO HybridPlayer con
                // setFadeVolume(1.0f), che riallinea fadeMultiplier, rispetta la
                // normalizzazione (ytLoudnessDb) e vale per entrambi i motori.
                if (appSettings.crossfadeDuration == CrossfadeDuration.Off) {
                    // FIX: ripristino via pipeline invece di una forzatura al 100%:
                    // applyCurrentVolume() applica 1.0 × fattore di normalizzazione del brano
                    // corrente (in un riavvio dello STESSO brano è già quello giusto), quindi
                    // nessun picco al riavvio con normalizzazione attiva. Vale anche per
                    // ExoPlayer, che altrimenti resta a volume 0 dopo il quick-fade.
                    hybridPlayer.setFadeVolume(1.0f)
                    isFading = false
                    Timber.d("PlayerService UX: riavvio brano senza crossfade, volume ripristinato via HybridPlayer (fadeMultiplier=1.0, isFading=false)")
                }

            } else {
                Timber.d("PlayerService UX: Canzone all'inizio (<$rewindThreshold). Salto regolarmente al BRANO PRECEDENTE.")

                // In questo caso NON mettiamo isFading = false qui!
                // Lasciamo lo scudo alzato (true) per proteggere il transitorio di handlePlayPrevious().
                // Sarà startFadeIn() del brano precedente, quando si sveglierà in PLAYING, a rimetterlo a false!

                // Torniamo alla traccia precedente reale
                handlePlayPrevious()
            }
        }
    }



    fun requestSmoothPause() {
        //Timber.d("EXECUTE_SMOOTH_PAUSE stack:\n${Exception().stackTraceToString()}")

        // L'utente richiede esplicitamente la pausa: disattiviamo qualunque
        // auto-ripresa programmata dalla gestione connettività
        pausedForConnectivity = false
        resumeDesiredAfterConnectivity = false
        markUserPauseRequested()

        if (!_playerState.value.isPlaying || isFading) {
            hybridPlayer.executeActualPause()
            return
        }

        // ADATTAMENTO DINAMICO DELLA DURATA
        // Se il crossfade è disattivato, impostiamo un micro-fade di appena 60ms (invisibile ma salva-pop),
        // altrimenti manteniamo i regolari 200ms morbidi.
        val isCrossfadeOff = appSettings.crossfadeDuration == CrossfadeDuration.Off
        val fadeDurationMs = if (isCrossfadeOff) 60 else 200
        val steps = if (isCrossfadeOff) 3 else 5 // Meno step per il taglio netto
        val stepDelay = fadeDurationMs / steps

        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
            isFading = true
            Timber.d("PlayerService UX: Avvio Pause Fade (duration=$fadeDurationMs ms)")

            for (i in 1..steps) {
                val progress = 1f - (i.toFloat() / steps)
                hybridPlayer.setFadeVolume(progress * progress)
                delay(stepDelay.milliseconds)
            }

            hybridPlayer.setFadeVolume(0f)
            isFading = false

            hybridPlayer.executeActualPause()
        }
    }

    fun requestSmoothPlay() {
        // L'utente richiede esplicitamente il play: qualunque vecchia intenzione
        // di pausa è superata e le gestioni automatiche in corso vengono chiuse
        clearUserPauseIntent()
        pausedForConnectivity = false
        pendingRecoveryLoadVideoId = null

        // Se la rete è ASSENTE, il play dell'utente esprime la volontà di partire
        // appena possibile: teniamolo a memoria così al ritorno della rete la
        // ripresa è automatica (invece di lasciare il brano fermo)
        resumeDesiredAfterConnectivity = !onlineConnectivityMonitor.isNetworkAvailable &&
                !isCastDrivingPlayback()

        if (_playerState.value.isPlaying) return

        // ADATTAMENTO DINAMICO IN RISALITA
        val isCrossfadeOff = appSettings.crossfadeDuration == CrossfadeDuration.Off
        val fadeDurationMs = if (isCrossfadeOff) 60 else 250
        val steps = if (isCrossfadeOff) 3 else 5
        val stepDelay = fadeDurationMs / steps

        this@PlayerService.serviceScope.launch(Dispatchers.Main) {
            hybridPlayer.setFadeVolume(0f)
            hybridPlayer.executeActualPlay()

            isFading = true
            Timber.d("PlayerService UX: Avvio Play Fade (duration=$fadeDurationMs ms)")

            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                hybridPlayer.setFadeVolume(progress)
                delay(stepDelay.milliseconds)
            }

            hybridPlayer.setFadeVolume(1.0f)
            isFading = false
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

        const val ALARM_NOTIFICATION_ID = 1003
        val ALARM_NOTIFICATION_CHANNEL_ID = globalContext().resources.getString(R.string.alarm_notification_channel_id)

    }

}

