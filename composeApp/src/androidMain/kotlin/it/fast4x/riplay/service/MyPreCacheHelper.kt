package it.fast4x.riplay.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.scheduler.Requirements
import coil.imageLoader
import coil.request.ImageRequest
import it.fast4x.riplay.Database
import it.fast4x.riplay.appContext
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.models.SongEntity
import it.fast4x.riplay.utils.DownloadSyncedLyrics
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.audioQualityFormatKey
import it.fast4x.riplay.utils.download
import it.fast4x.riplay.utils.getEnum
import it.fast4x.riplay.utils.preferences
import it.fast4x.riplay.utils.principalCache
import it.fast4x.riplay.utils.removeDownload
import it.fast4x.riplay.utils.thumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors

@UnstableApi
object MyPreCacheHelper {
    private val executor = Executors.newCachedThreadPool()
    private val coroutineScope = CoroutineScope(
        executor.asCoroutineDispatcher() +
                SupervisorJob() +
                CoroutineName("MyPreCacheService-Executor-Scope")
    )

    // While the class is not a singleton (lifecycle), there should only be one download state at a time
//    private val mutableDownloadState = MutableStateFlow(false)
//    val downloadState = mutableDownloadState.asStateFlow()
//    private val downloadQueue =
//        Channel<DownloadManager>(onBufferOverflow = BufferOverflow.DROP_OLDEST)

    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "precache_channel"

    val databaseProvider: DatabaseProvider by lazy {
        principalCache.getDatabaseProvider(appContext())
    }

    val cache: SimpleCache by lazy {
        principalCache.getInstance(appContext())
    }

    private lateinit var downloadNotificationHelper: DownloadNotificationHelper
    private lateinit var downloadManager: DownloadManager
    lateinit var audioQualityFormat: AudioQualityFormat


    var downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    fun getDownload(songId: String): Flow<Download?> {
        return downloads.map { it[songId] }

    }

    @SuppressLint("LongLogTag")
    @Synchronized
    fun getDownloads() {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result

    }


    @Synchronized
    fun getDownloadNotificationHelper(context: Context?): DownloadNotificationHelper {
        if (!MyPreCacheHelper::downloadNotificationHelper.isInitialized) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context!!, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }
        return downloadNotificationHelper
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        audioQualityFormat =
            context.preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)

        if (!MyPreCacheHelper::downloadManager.isInitialized) {
            downloadManager = DownloadManager(
                context,
                databaseProvider,
                cache,
                //createDataSourceFactory(),
                createSimpleDataSourceFactory(),
                //Executor(Runnable::run)
                executor
            ).apply {
                maxParallelDownloads = 3
                minRetryCount = 2
                requirements = Requirements(Requirements.NETWORK)

//                addListener(
//                    object : DownloadManager.Listener {
//
//                        override fun onDownloadChanged(
//                            downloadManager: DownloadManager,
//                            download: Download,
//                            finalException: Exception?
//                        ) = run {
//                            syncDownloads(download)
//                        }
//
//                        override fun onDownloadRemoved(
//                            downloadManager: DownloadManager,
//                            download: Download
//                        ) = run {
//                            syncDownloads(download)
//                        }
//                    }
//                )
            }

            //downloadTracker =
            //    DownloadTracker(context, getHttpDataSourceFactory(context), downloadManager)
        }
    }

    @Synchronized
    private fun syncDownloads(download: Download) {
        downloads.update { map ->
            map.toMutableMap().apply {
                set(download.request.id, download)
            }
        }
    }

//    @Synchronized
//    private fun getDatabaseProvider(context: Context): DatabaseProvider {
//        if (!MyPreCacheHelper::databaseProvider.isInitialized) databaseProvider =
//            StandaloneDatabaseProvider(context)
//        return databaseProvider
//    }

    fun addDownload(context: Context, mediaItem: MediaItem) {
        if (mediaItem.isLocal) return

        val downloadRequest = DownloadRequest
            .Builder(
                /* id      = */ mediaItem.mediaId,
                /* uri     = */ mediaItem.requestMetadata.mediaUri
                    //try to download from youtube.com
                    ?: Uri.parse("https://youtube.com/watch?v=${mediaItem.mediaId}")
                    //try to download ffrom music.youtube.com
                    //?: Uri.parse("https://music.youtube.com/watch?v=${mediaItem.mediaId}")
            )
            .setCustomCacheKey(mediaItem.mediaId)
            .setData("${cleanPrefix(mediaItem.mediaMetadata.artist.toString())} - ${cleanPrefix(mediaItem.mediaMetadata.title.toString())}".encodeToByteArray()) // Title in notification
            .build()

        Database.asyncTransaction {
            runCatching {
                insert(mediaItem)
            }.also { if (it.isFailure) return@asyncTransaction }
        }

        val imageUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(1200)

//            sendAddDownload(
//                context,MyDownloadService::class.java,downloadRequest,false
//            )

        coroutineScope.launch {
            context.download<MyPreCacheService>(downloadRequest).exceptionOrNull()?.let {
                if (it is CancellationException) throw it

                Timber.e("MyPreCacheService scheduleDownload exception ${it.stackTraceToString()}")
                println("MyPreCacheService scheduleDownload exception ${it.stackTraceToString()}")
            }
            DownloadSyncedLyrics(it = SongEntity(mediaItem.asSong), coroutineScope = coroutineScope)
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    //.networkCachePolicy(CachePolicy.ENABLED)
                    .data(imageUrl)
                    .size(1200)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .allowHardware(false)
                    .diskCacheKey(imageUrl.toString())
                    .build()
            )
        }

    }

    fun removeDownload(context: Context, mediaItem: MediaItem) {
        if (mediaItem.isLocal) return

        //sendRemoveDownload(context,MyDownloadService::class.java,mediaItem.mediaId,false)
        coroutineScope.launch {
            context.removeDownload<MyPreCacheService>(mediaItem.mediaId).exceptionOrNull()?.let {
                if (it is CancellationException) throw it

                Timber.e(it.stackTraceToString())
                println("MyPreCacheHelper removeDownload exception ${it.stackTraceToString()}")
            }
        }
    }


}
