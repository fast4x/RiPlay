package it.fast4x.riplay

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.DatabaseInitializer
import it.fast4x.riplay.enums.CoilDiskCacheMaxSize
import it.fast4x.riplay.extensions.appviewmodel.AppViewModel
import it.fast4x.riplay.extensions.appviewmodel.models.NetworkConnectivity
import it.fast4x.riplay.extensions.appviewmodel.observeNetworkType
import it.fast4x.riplay.utils.FileLoggingTree
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COIL_CUSTOM_DISK_CACHE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COIL_DISK_CACHE_MAX_SIZE
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LOG_DEBUG_ENABLED
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.PreferenceKey.USE_PLACEHOLDER_IN_IMAGE_LOADER
import it.fast4x.riplay.extensions.crashreporter.CrashReporter
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationService
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfileRepository
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.builders.UserProfileBuilder
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.DeepCutsStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.EraExplorerStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.ForgottenGemsStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.MBGraphWalkStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.QualityCuratorStrategy
import it.fast4x.riplay.extensions.musicbrainz.MBMetadataHelper
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.extensions.musicbrainz.fillers.ArtistMbIdBackfiller
import it.fast4x.riplay.extensions.musicbrainz.fillers.SongArtistBackfiller
import it.fast4x.riplay.extensions.musicbrainz.workers.ArtistRelationFetcher
import it.fast4x.riplay.extensions.musicbrainz.workers.MBAlbumsByGenreFetcher
import it.fast4x.riplay.extensions.musicbrainz.workers.MBAlbumsByGenreWorker
import it.fast4x.riplay.extensions.musicbrainz.workers.MBMetadataBackfillWorker
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.utils.InitializeEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class MainApplication : Application(), ImageLoaderFactory {

    // Stato condiviso accessibile ovunque, anche dai Service
    val networkConnectivity: StateFlow<NetworkConnectivity>
        get() = _networkConnectivity

    private val _networkConnectivity = MutableStateFlow<NetworkConnectivity>(
        NetworkConnectivity.Disconnected
    )

    private val appScopeMain = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val appScopeIO = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val appViewModelFactory by lazy {
        AppViewModel.factory(this)
    }

    // Inizializza RecommendationService
    // Builder
    private val profileBuilder = UserProfileBuilder()
    // Repository
    val profileRepository = UserProfileRepository(
        builder = profileBuilder,
    )
    // Strategie
    val strategies = listOf(
        ForgottenGemsStrategy(),
        QualityCuratorStrategy(),
        DeepCutsStrategy(),
        EraExplorerStrategy(),
        MBGraphWalkStrategy()
    )
    // Service
    val recommendationService = RecommendationService(
        profileRepo = profileRepository,
        strategies = strategies,
    )

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        appScopeMain.launch {
            observeNetworkType(this@MainApplication).collect {
                _networkConnectivity.value = it
            }
        }

        val receiver = ComponentName(this, PlayerService::class.java)
        val pm = packageManager

        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        Dependencies.init(this)

        /***** CRASH LOG ALWAYS ENABLED *****/
        val dir = filesDir.resolve("logs").also {
            if (it.exists()) return@also
            it.mkdir()
        }
        Thread.setDefaultUncaughtExceptionHandler(CrashReporter(dir.absolutePath))
        /***** CRASH LOG ALWAYS ENABLED *****/

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(LOG_DEBUG_ENABLED.key, false)
        if (logEnabled) {
            Timber.plant(FileLoggingTree(File(dir, "RiPlay_log.txt")))
            Timber.d("Log enabled at ${dir.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/

        appScopeIO.launch {
            val db = Database.getInstance.openHelper.readableDatabase

            // 1. Versione DB
            val versionCursor = db.query("PRAGMA user_version")
            versionCursor.use {
                if (it.moveToFirst()) Timber.tag("DB_DEBUG").d("DB version: ${it.getInt(0)}")
            }

            // 2. Tutte le tabelle
            val tablesCursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
            )
            Timber.tag("DB_DEBUG").d("=== TABELLE ===")
            tablesCursor.use {
                while (it.moveToNext()) Log.d("DB_DEBUG", "  - '${it.getString(0)}'")
            }

            // 3. Verifica song_artist_cross_ref esiste
            val crossRefCursor = db.query("PRAGMA table_info(song_artist_cross_ref)")
            val cols = mutableListOf<String>()
            crossRefCursor.use {
                while (it.moveToNext()) cols.add(it.getString(1))
            }
            Timber.tag("DB_DEBUG").d("song_artist_cross_ref columns: $cols")

            // 4. Verifica count righe
            try {
                val countCursor = db.query("SELECT COUNT(*) FROM song_artist_cross_ref")
                countCursor.use {
                    if (it.moveToFirst()) Timber.tag("DB_DEBUG")
                        .d("cross_ref rows: ${it.getInt(0)}")
                }
            } catch (e: Exception) {
                Timber.tag("DB_DEBUG").d("cross_ref table MISSING: ${e.message}")
            }

            // 5. Verifica campi nuovi su Artist
            val artistCursor = db.query("PRAGMA table_info(Artist)")
            val artistCols = mutableListOf<String>()
            artistCursor.use {
                while (it.moveToNext()) artistCols.add(it.getString(1))
            }
            Timber.tag("DB_DEBUG").d("Artist has mbId: ${"mbId" in artistCols}")
            Timber.tag("DB_DEBUG")
                .d("Artist has youtubeChannelId: ${"youtubeChannelId" in artistCols}")
            Timber.tag("DB_DEBUG").d("Artist has nature: ${"nature" in artistCols}")
        }

        /*
        // Verifica tutte le strategie e le sezioni create
        appScopeIO.launch {
            profileRepository.loadFromDb()
            recommendationService.refreshAll()
            delay(500)

            val sections = recommendationService.sections.value
            sections.forEach { section ->
                Timber.tag("REC_DEBUG").d("Strategy ${section.id}: ${section.items.size} items")
                section.items.take(3).forEach { item ->
                    // ★ Verifica che artist non sia null per ForgottenGems
                    Timber.tag("REC_DEBUG")
                        .d( "  • ${item.primaryTitle} by ${item.primarySubtitle.ifBlank { "?" }} — score=${item.score}")
                    item.reasons.firstOrNull()?.let { Timber.tag("REC_DEBUG").d("      - $it") }
                }
            }
        }

         */

        /*
        appScopeIO.launch {
            Timber.tag("REC_DEBUG").d("=== ROBUST SEQUENCE ===")
            // Step 1: Carica profilo esistente
            profileRepository.loadFromDb()
            Timber.tag("REC_DEBUG")
                .d("Step 1: Profile loaded: ${profileRepository.profile.value != null}")

            // Step 2: Popola mbId per top artisti del profilo ATTUALE
            val mbIdBackfiller = ArtistMbIdBackfiller()
            val mbIdResult = mbIdBackfiller.backfill(limit = 50)
            Timber.tag("REC_DEBUG").d("Step 2: mbId backfill: $mbIdResult")
            Timber.tag("REC_DEBUG").d("Artists with mbId: ${Database.artistDao().countWithMbId()}")

            // Step 3: Rebuild profilo (ora include artisti con mbId)
            profileRepository.rebuildFull()
            Timber.tag("REC_DEBUG").d("Step 3: Rebuild done")
            Timber.tag("REC_DEBUG")
                .d("Top artists: ${profileRepository.profile.value?.topArtists?.size}")
            Timber.tag("REC_DEBUG").d(
                "Top 5 with mbId: ${
                profileRepository.profile.value?.topArtists?.take(10)
                    ?.count { !it.artistId.startsWith("virtual::") }
            }")

            // Step 4: Fetch relations SOLO per artisti con mbId
            val relFetcher = ArtistRelationFetcher()
            val relResult = relFetcher.fetch(topArtistsCount = 20)
            Log.d("REC_DEBUG", "Step 4: Relations: $relResult")

            // Step 5: Refresh strategies
            recommendationService.refreshAll()
            delay(500)

            val sections = recommendationService.sections.value
            sections.forEach { section ->
                Timber.tag("REC_DEBUG").d("Strategy ${section.id}: ${section.items.size} items")
                section.items.take(3).forEach { item ->
                    Timber.tag("REC_DEBUG")
                        .d("  • ${item.primaryTitle} by ${item.artist?.name} — score=${item.score}")
                    item.reasons.take(2).forEach { r ->
                        Timber.tag("REC_DEBUG").d("      - $r")
                    }
                }
            }
        }

         */



//        val backfillRequest = OneTimeWorkRequestBuilder<MBMetadataBackfillWorker>()
//            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
//            .build()
//        WorkManager.getInstance(this).enqueueUniqueWork(
//            "mb_backfill",
//            ExistingWorkPolicy.KEEP,
//            backfillRequest
//        )

    }

    override fun onTerminate() {
        super.onTerminate()
        appScopeMain.cancel()
    }

    override fun newImageLoader(): ImageLoader {
        val coilCustomDiskCache = preferences.getInt(COIL_CUSTOM_DISK_CACHE.key, 128) * 1000 * 1000L
        val coilDiskCacheMaxSize = preferences.getEnum(COIL_DISK_CACHE_MAX_SIZE.key,CoilDiskCacheMaxSize.`128MB`)
        val usePlaceholder = preferences.getBoolean(USE_PLACEHOLDER_IN_IMAGE_LOADER.key, true)
        val coilCacheSize = when (coilDiskCacheMaxSize) {
            CoilDiskCacheMaxSize.Custom -> coilCustomDiskCache
            else -> coilDiskCacheMaxSize.bytes
        }


        return ImageLoader.Builder(this)
            .crossfade(true)
            //.allowHardware(if (isAtLeastAndroid8) true else false)
            //.bitmapConfig(if (isAtLeastAndroid8) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888)
            //.networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .error(R.drawable.noimage)
            .fallback(R.drawable.noimage)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            )
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(
                        if (coilCacheSize == 0L) CoilDiskCacheMaxSize.`128MB`.bytes
                        else coilCacheSize
                    )
                    .build()
            )
            .apply {
                if (usePlaceholder) {
                    placeholder(R.drawable.loader)
                }
            }
            .build()
    }

}

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
        DatabaseInitializer()
        InitializeEnvironment( this.application ) // android initialization
        //initializeEnvironment() // multiplatform initialization
    }
}