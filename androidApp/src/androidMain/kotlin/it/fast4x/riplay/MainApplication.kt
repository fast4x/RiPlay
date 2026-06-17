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
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.ForgottenGemsStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies.QualityCuratorStrategy
import it.fast4x.riplay.extensions.musicbrainz.MBMetadataHelper
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
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
    private val strategies = listOf(
        ForgottenGemsStrategy(),
        QualityCuratorStrategy()
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
            profileRepository.loadFromDb()
            recommendationService.refreshAll()
            delay(500)

            val sections = recommendationService.sections.value
            sections.forEach { section ->
                Timber.tag("REC_DEBUG").d("Strategy ${section.id}: ${section.items.size} items")
                section.items.take(5).forEach { item ->
                    Timber.tag("REC_DEBUG")
                        .d("  • ${item.primaryTitle} by ${item.album?.authorsText} — score=${item.score}")
                    item.reasons.take(3).forEach { r ->
                        Timber.tag("REC_DEBUG").d("      - $r")
                    }
                }
            }
        }

        /*
        appScopeIO.launch {
            profileRepository.loadFromDb()
            // 1. Pulisci MBAlbum esistenti
//            val deleted = Database.deleteMBAlbumsAll()
//            Timber.tag("REC_DEBUG").d("Cleaned $deleted old MBAlbums")

            // 2. Verifica che profileRepository abbia il profilo
            val profile = profileRepository.profile.value
            Timber.tag("REC_DEBUG").d("Profile: ${profile?.keywordVector?.size ?: 0} keywords")

            // 3. Lancia fetch
            val fetcher = MBAlbumsByGenreFetcher()
            val result = fetcher.fetch(topGenresCount = 5, albumsPerGenre = 100)
            Timber.tag("REC_DEBUG").d("Fetch result: $result")

            // 4. Verifica
            Timber.tag("REC_DEBUG").d("=== FINAL STATS ===")
            Timber.tag("REC_DEBUG").d("Total MBAlbums: ${Database.count()}")
            Timber.tag("REC_DEBUG").d("With rating: ${Database.countWithRating()}")

            val quality = Database.getQualityAlbumsV2(limit = 30)
            Timber.tag("REC_DEBUG").d("Quality candidates: ${quality.size}")
            quality.take(10).forEach { a ->
                Timber.tag("REC_DEBUG").d("  ★ '${a.title}' by ${a.artistCredit}")
                Timber.tag("REC_DEBUG")
                    .d("    rating=${a.rating} votes=${a.ratingVotes} genres=${a.genres?.size} tags=${a.tags?.size} popularity=${a.popularityScore}")
            }

            // 5. Refresh strategies
            recommendationService.refreshAll()
            delay(500)

            val sections = recommendationService.sections.value
            sections.forEach { section ->
                Timber.tag("REC_DEBUG").d("Strategy ${section.id}: ${section.items.size} items")
                section.items.take(3).forEach { item ->
                    Timber.tag("REC_DEBUG").d("  • ${item.primaryTitle} — score=${item.score}")
                }
            }
        }

         */

        // mbalbum fetcher da schedulare
        /*
        val request = OneTimeWorkRequestBuilder<MBAlbumsByGenreWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueue(request)

         */

        /*
        appScopeIO.launch {
            profileRepository.loadFromDb()
            Timber.tag("REC_DEBUG").d("=== STARTING ALBUM BATCH ENRICHMENT ===")

            val albumsToEnrich = Database.getAlbumsToEnrich(limit = 10)
            Timber.tag("REC_DEBUG").d("Albums to enrich: ${albumsToEnrich.size}")

            var success = 0
            var failed = 0
            var skipped = 0

            for (album in albumsToEnrich) {
                try {
                    val mbclient = MusicBrainz()
                    val mdHelper = MBMetadataHelper(mbclient)
                    mdHelper.onAlbumViewed(album.id)

                    // Verifica se l'album è stato effettivamente arricchito
                    delay(500) // lascia propagare la scrittura DB
                    val updated = Database.album(album.id).first()
                    if (updated?.rating != null || !updated?.genres.isNullOrEmpty()) {
                        success++
                        Timber.tag("REC_DEBUG")
                            .d("✓ '${album.title}' enriched (rating=${updated?.rating})")
                    } else {
                        skipped++
                        Timber.tag("REC_DEBUG").d("⊘ '${album.title}' no MB match")
                    }

                    // Rate limit MB: 1 req/sec (onAlbumViewed fa 1-2 chiamate MB)
                    delay(1500)

                } catch (e: Exception) {
                    failed++
                    Timber.tag("REC_DEBUG").w("✗ '${album.title}' failed: ${e.message}")
                    delay(2000) // backoff più lungo su errore
                }
            }

            Timber.tag("REC_DEBUG")
                .d("=== DONE: success=$success, skipped=$skipped, failed=$failed ===")

            // Verifica finale
            val enrichedNow = Database.countAlbumsEnriched()
            val topRatedNow = Database.getTopRatedAlbums(4.0f, 50, 100).size
            Timber.tag("REC_DEBUG").d("Total enriched albums: $enrichedNow")
            Timber.tag("REC_DEBUG").d("Top-rated (>=4.0): $topRatedNow")

            // Rigenera suggerimenti
            recommendationService.refreshAll()
        }

         */

        /*
        appScopeIO.launch {
            val enriched = Database.countAlbumsEnriched()
            Timber.tag("REC_DEBUG").d("Albums enriched: $enriched")
            Database.getAlbumsEnrichedSample().forEach {
                Timber.tag("REC_DEBUG")
                    .d("  Sample: '${it.title}', rating=${it.rating}, votes=${it.ratingVotes}")
            }
        }

         */
        /*
        appScopeIO.launch {
            Timber.tag("REC_DEBUG").d("=== ALBUM TABLE STATS ===")

            val total = Database.countAlbumsTotal()
            val withRating = Database.countAlbumsWithRating()
            val withVotes = Database.countAlbumsWithVotes()

            Timber.tag("REC_DEBUG").d("Total albums: $total")
            Timber.tag("REC_DEBUG").d("Albums with rating: $withRating")
            Timber.tag("REC_DEBUG").d("Albums with votes > 0: $withVotes")

            // Sample di alcuni album per capire lo stato
            val sample = Database.getAlbums(5)
            sample.forEach { album ->
                Timber.tag("REC_DEBUG")
                    .d("  Album: '${album.title}', rating=${album.rating}, votes=${album.ratingVotes}, genres=${album.genres}")
            }
        }
        */

        /*
        appScopeIO.launch {
            profileRepository.loadFromDb()

            Timber.tag("REC_DEBUG").d("=== QUALITY CURATOR DIAGNOSTICS ===")

            // 1. Quanti album top-rated ci sono?
            val topRatedAlbums = Database.getTopRatedAlbums(
                minRating = 4.0f,
                minVotes = 50,
                limit = 1000
            )
            Timber.tag("REC_DEBUG")
                .d("Top-rated albums in DB (rating>=4.0, votes>=50): ${topRatedAlbums.size}")

            // 2. Abbassa la soglia per vedere se è solo un problema di soglia
            val relaxedAlbums = Database.getTopRatedAlbums(
                minRating = 3.5f,
                minVotes = 10,
                limit = 1000
            )
            Timber.tag("REC_DEBUG").d("Relaxed (rating>=3.5, votes>=10): ${relaxedAlbums.size}")

            // 3. Mostra sample degli album top-rated per vedere i generi
            topRatedAlbums.take(5).forEach { album ->
                Timber.tag("REC_DEBUG")
                    .d("  Album: '${album.title}', rating=${album.rating}, votes=${album.ratingVotes}, genres=${album.genres}")
            }

            // 4. Mostra le 39 keyword del profilo utente
            val profile = profileRepository.profile.value
            Timber.tag("REC_DEBUG").d("User keywords (${profile?.keywordVector?.size ?: 0}):")
            profile?.keywordVector?.entries
                ?.sortedByDescending { it.value }
                ?.forEach { (kw, w) ->
                    Timber.tag("REC_DEBUG").d("  - $kw : $w")
                }

            // 5. Cerca match manualmente
            val userKeywordsLower = profile?.keywordVector?.keys?.toSet() ?: emptySet()
            val matchingAlbums = topRatedAlbums.filter { album ->
                val albumKeywords = (album.genres.orEmpty() + album.tags.orEmpty())
                    .map { it.lowercase().trim() }
                    .toSet()
                albumKeywords.intersect(userKeywordsLower).isNotEmpty()
            }
            Timber.tag("REC_DEBUG").d("Albums with genre match: ${matchingAlbums.size}")
        }

         */

        /*
        appScopeIO.launch{

            Timber.tag("REC_DEBUG").d("=== PRE REBUILD ===")
            val profile = profileRepository.profile.value
            Timber.tag("REC_DEBUG").d("Profile: $profile")
            Timber.tag("REC_DEBUG").d("TopArtists: ${profile?.topArtists?.size ?: -1}")
            Timber.tag("REC_DEBUG").d("Keywords: ${profile?.keywordVector?.size ?: -1}")
            Timber.tag("REC_DEBUG").d("Eras: ${profile?.eraVector?.size ?: -1}")
            Timber.tag("REC_DEBUG").d("LastRefreshed: ${profile?.lastRefreshedAt}")

            var shouldShow = recommendationService.shouldShowSection.value
            Timber.tag("REC_DEBUG").d("shouldShowSection: $shouldShow")

            var visibleSections = recommendationService.visibleSections.value
            Timber.tag("REC_DEBUG").d("Visible sections: ${visibleSections.size}")
            visibleSections.forEach { section ->
                Timber.tag("REC_DEBUG").d("  - ${section.id}: ${section.items.size} items")
            }

            // Verifica dati raw nel DB
            val eventCount = Database.getAllEvents().size
            Timber.tag("REC_DEBUG").d("Total events in DB: $eventCount")

            val playedSongs = Database.countDistinctPlayedSongs()
            Timber.tag("REC_DEBUG").d("Distinct played songs: $playedSongs")

            Timber.tag("REC_DEBUG").d("Forcing rebuild...")
            try {
                Dependencies.application.profileRepository.rebuildFull()
                Timber.tag("REC_DEBUG").d("Rebuild completed without exceptions")
            } catch (e: Exception) {
                Timber.tag("REC_DEBUG").e(e, "Rebuild FAILED")
            }

            /*
            Timber.tag("REC_DEBUG").d("Forcing recommendation refresh...")
            try {
                recommendationService.refreshAll()
                Timber.tag("REC_DEBUG").d("Refresh completed without exceptions")
            } catch (e: Exception) {
                Timber.tag("REC_DEBUG").e(e, "Refresh FAILED")
            }
             */

            // === LOG STRATEGIES ===
            delay(500)  // lascia propagare i StateFlow
            val allSections = recommendationService.sections.value
            visibleSections = recommendationService.visibleSections.value
            shouldShow = recommendationService.shouldShowSection.value

            Timber.tag("REC_DEBUG").d("=== STRATEGIES RESULT ===")
            Timber.tag("REC_DEBUG").d("shouldShowSection: $shouldShow")
            Timber.tag("REC_DEBUG").d("All sections: ${allSections.size}")
            Timber.tag("REC_DEBUG").d("Visible sections: ${visibleSections.size}")

            allSections.forEach { section ->
                Timber.tag("REC_DEBUG").d("  - [${section.id}] ${section.title}")
                Timber.tag("REC_DEBUG").d("    items: ${section.items.size}")
                section.items.take(3).forEach { item ->
                    Timber.tag("REC_DEBUG")
                        .d("      • ${item.primaryTitle} — ${item.primarySubtitle}")
                    Timber.tag("REC_DEBUG")
                        .d("        score=${item.score}, reason=${item.reasons.firstOrNull()}")
                }
            }
            /*
            Timber.tag("REC_DEBUG").d("=== POST REBUILD ===")
            val p = profileRepository.profile.value
            Timber.tag("REC_DEBUG").d("Profile null? ${p == null}")
            Timber.tag("REC_DEBUG").d("TopArtists: ${p?.topArtists?.size ?: -1}")
            Timber.tag("REC_DEBUG").d("Keywords: ${p?.keywordVector?.size ?: -1}")
            Timber.tag("REC_DEBUG").d("Eras: ${p?.eraVector?.size ?: -1}")

            val totalSongs = Database.countSongsTotal()
            val songsWithGenres = Database.countSongsWithGenres()
            val songsLiked = Database.countSongsLiked()
            val orphanEvents = Database.countOrphanEvents()

            Timber.tag("REC_DEBUG").d("=== DB STATS ===")
            Timber.tag("REC_DEBUG").d("Total songs: $totalSongs")
            Timber.tag("REC_DEBUG").d("Songs with genres: $songsWithGenres")
            Timber.tag("REC_DEBUG").d("Liked songs: $songsLiked")
            Timber.tag("REC_DEBUG").d("Orphan events: $orphanEvents")
            Timber.tag("REC_DEBUG")
                .d("Events with playTime > 0: ${Database.countEventsWithPlayTime()}")

            val song = Database.getFirstWithGenres()
            Timber.tag("REC_DEBUG").d("Sample song with genres:")
            Timber.tag("REC_DEBUG").d("  id=${song?.id}")
            Timber.tag("REC_DEBUG").d("  title=${song?.title}")
            Timber.tag("REC_DEBUG").d("  artistsText=${song?.artistsText}")
            Timber.tag("REC_DEBUG").d("  genres=${song?.genres}")
            Timber.tag("REC_DEBUG").d("  genres class=${song?.genres?.javaClass?.simpleName}")
            Timber.tag("REC_DEBUG").d("  isPodcast=${song?.isPodcast}")
            Timber.tag("REC_DEBUG").d("  durationText=${song?.durationText}")

             */
        }

         */

        /*
        appScopeIO.launch {
            Timber.tag("REC_DEBUG").d("=== STARTING BACKFILL ===")
            val songsToEnrich = Database.getTopSongsWithoutGenres(limit = 50)
            Timber.tag("REC_DEBUG").d("Songs to enrich: ${songsToEnrich.size}")

            var enriched = 0
            var failed = 0

            for (song in songsToEnrich) {
                try {
                    // Chiama il tuo servizio MB esistente — sostituisci con la tua funzione reale
                    // mbService.fetchAndSaveArtistMetadata(song.artistsText ?: continue)
                    val mbclient = MusicBrainz()
                    val mdHelper = MBMetadataHelper(mbclient)
                    mdHelper.onArtistViewed(song.artistsText ?: continue)
                    enriched++
                    Timber.tag("REC_DEBUG").d("Enriched ${song.id} (${song.title})")

                    // Rate limit MB
                    delay(1100)
                } catch (e: Exception) {
                    failed++
                    Timber.tag("REC_DEBUG").w("Failed ${song.id}: ${e.message}")
                }
            }

            Timber.tag("REC_DEBUG").d("=== BACKFILL DONE: enriched=$enriched, failed=$failed ===")
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