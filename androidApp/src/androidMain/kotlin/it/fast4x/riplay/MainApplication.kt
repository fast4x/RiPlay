package it.fast4x.riplay

import android.app.Application
import android.content.ComponentName
import android.content.Context
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
import it.fast4x.riplay.enums.ArtistNature
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
import it.fast4x.riplay.extensions.musicbrainz.fillers.NatureBackfiller
import it.fast4x.riplay.extensions.musicbrainz.fillers.SongArtistBackfiller
import it.fast4x.riplay.extensions.musicbrainz.workers.ArtistRelationFetcher
import it.fast4x.riplay.extensions.musicbrainz.workers.MBAlbumsByGenreFetcher
import it.fast4x.riplay.extensions.musicbrainz.workers.MBAlbumsByGenreWorker
import it.fast4x.riplay.extensions.musicbrainz.workers.MBMetadataBackfillWorker
import it.fast4x.riplay.extensions.musicbrainz.workers.WorkScheduler
import it.fast4x.riplay.extensions.musicbrainz.workers.WorkerDependencies
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


    // Prepara Profile Builder
    val profileBuilder = UserProfileBuilder()
    // Repository
    val profileRepository = UserProfileRepository(
        builder = profileBuilder,
    )
    //Si inizializza recommendationService senza strategie, in attesa che il db venga inizializzato in onCreate
    lateinit var recommendationService: RecommendationService


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


        // Strategie, viene chiamato dopo l'inizializzazione  del database
        val strategies = listOf(
            ForgottenGemsStrategy(),
            QualityCuratorStrategy(),
            DeepCutsStrategy(),
            EraExplorerStrategy(),
            MBGraphWalkStrategy()
        )

        // Inizializza Service con le strategie

        recommendationService = RecommendationService(
            profileRepo = profileRepository,
            strategies = strategies,
        )

        // Inizializza WorkerDependencies
        WorkerDependencies.initialize(
            database = Database,
            mbClient = MusicBrainz(),
            profileRepository = profileRepository
        )

        // Schedula job periodici
        WorkScheduler.scheduleAll(this)

        // Avvia caricamento profilo + refresh UI
        appScopeIO.launch {
            profileRepository.loadFromDb()

            // Se il profilo è vecchio (>24h) o non c'è, forza rebuild
            val lastRefresh = profileRepository.profile.value?.lastRefreshedAt ?: 0L
            val now = System.currentTimeMillis()
            val oneDayMs = 24L * 3600 * 1000

            if (now - lastRefresh > oneDayMs) {
                profileRepository.rebuildFull()
            }

            recommendationService.refreshAll()
        }


        /*
        appScopeIO.launch {
            val backfiller = NatureBackfiller()
            val result = backfiller.backfillAll()
            Timber.tag("REC_DEBUG").d("Nature backfill: $result")

            // Verifica artisti riclassificati
            val humanCount = Database.artistDao().getArtistsByNature(ArtistNature.HUMAN).size
            val aiCount = Database.artistDao().getArtistsByNature(ArtistNature.AI_GENERATED).size
            Timber.tag("REC_DEBUG").d("After reclassify: HUMAN=$humanCount, AI=$aiCount")

            // Refresh recommendations
            recommendationService.refreshAll()
            delay(500)

            // Verifica nature nei suggerimenti
            val sections = recommendationService.sections.value
            sections.forEach { section ->
                Timber.tag("REC_DEBUG").d("=== ${section.id} ===")
                section.items.take(3).forEach { item ->
                    Timber.tag("REC_DEBUG").d("  • ${item.primaryTitle}")
                    Timber.tag("REC_DEBUG")
                        .d("    artist=${item.artist?.name}, nature=${item.artist?.nature}")
                    Timber.tag("REC_DEBUG")
                        .d("    album=${item.album?.title}, nature=${item.album?.nature}")
                }
            }
        }

         */

        appScopeIO.launch {
            profileRepository.loadFromDb()
            recommendationService.refreshAll()
        }


        /*
        appScopeIO.launch {
            recommendationService.refreshAll()
            delay(500)

            val sections = recommendationService.sections.value
            sections.forEach { section ->
                Timber.tag("REC_DEBUG").d("=== ${section.id} ===")
                section.items.take(3).forEach { item ->
                    val artistNature = item.artist?.nature
                    val albumNature = item.album?.nature
                    Timber.tag("REC_DEBUG").d("  • ${item.primaryTitle}")
                    Timber.tag("REC_DEBUG")
                        .d("    artist=${item.artist?.name}, nature=$artistNature")
                    Timber.tag("REC_DEBUG").d("    album=${item.album?.title}, nature=$albumNature")
                }
            }
        }
        */

        /*
        appScopeIO.launch {
            Timber.tag("REC_DEBUG").d("=== NATURE DATA CHECK ===")

            // Artisti con nature != UNKNOWN
            val aiArtists = Database.artistDao().getArtistsByNature(ArtistNature.AI_GENERATED)
            val humanArtists = Database.artistDao().getArtistsByNature(ArtistNature.HUMAN)
            val compilationArtists = Database.artistDao().getArtistsByNature(ArtistNature.COMPILATION)
            Timber.tag("REC_DEBUG")
                .d("Artists: AI=${aiArtists.size}, HUMAN=${humanArtists.size}, COMPILATION=${compilationArtists.size}")

            // Album con nature != UNKNOWN
            val albumStats = Database.getInstance.openHelper.readableDatabase.query("""
        SELECT nature, COUNT(*) FROM Album WHERE nature != 'UNKNOWN' GROUP BY nature
    """.trimIndent()).use { cursor ->
                val map = mutableMapOf<String, Int>()
                while (cursor.moveToNext()) {
                    map[cursor.getString(0)] = cursor.getInt(1)
                }
                map
            }
            Timber.tag("REC_DEBUG").d("Album natures: $albumStats")

            // MBAlbum con nature
            val mbStats = Database.getInstance.openHelper.readableDatabase.query("""
        SELECT nature, COUNT(*) FROM mb_album WHERE nature != 'UNKNOWN' GROUP BY nature
    """.trimIndent()).use { cursor ->
                val map = mutableMapOf<String, Int>()
                while (cursor.moveToNext()) {
                    map[cursor.getString(0)] = cursor.getInt(1)
                }
                map
            }
            Timber.tag("REC_DEBUG").d("MBAlbum natures: $mbStats")
        }

         */

        /*
        appScopeIO.launch {
            Timber.tag("REC_DEBUG").d("=== NATURE BACKFILL START ===")

            val backfiller = NatureBackfiller()
            val result = backfiller.backfillAll()

            Timber.tag("REC_DEBUG").d("=== NATURE BACKFILL DONE ===")
            Timber.tag("REC_DEBUG").d("Artists classified: ${result.artistsClassified}")
            Timber.tag("REC_DEBUG").d("Albums classified: ${result.albumsClassified}")
            Timber.tag("REC_DEBUG").d("MBAlbums classified: ${result.mbAlbumsClassified}")

            // Verifica campionamento AI artists
            val aiArtists = Database.artistDao().getArtistsByNature(ArtistNature.AI_GENERATED)
            Timber.tag("REC_DEBUG").d("=== AI ARTISTS FOUND: ${aiArtists.size} ===")
            aiArtists.take(5).forEach {
                Timber.tag("REC_DEBUG").d("  🤖 ${it.name} (genres: ${it.genres?.take(3)})")
            }

            // Verifica campionamento Human artists
            val humanArtists = Database.artistDao().getArtistsByNature(ArtistNature.HUMAN)
            Timber.tag("REC_DEBUG").d("=== HUMAN ARTISTS SAMPLE ===")
            humanArtists.take(3).forEach {
                Timber.tag("REC_DEBUG").d("  🎸 ${it.name}")
            }
        }
        */

        /*
        // Genera struttura db
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

         */

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

    }
}