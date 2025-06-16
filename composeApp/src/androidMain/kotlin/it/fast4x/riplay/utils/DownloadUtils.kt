package it.fast4x.riplay.utils


import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.LocalContext

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download

import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalDownloadHelper
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.appContext
import it.fast4x.riplay.enums.DownloadedStateMedia
import it.fast4x.riplay.service.MyDownloadHelper
import it.fast4x.riplay.service.MyPreCacheHelper
import it.fast4x.riplay.service.isLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.dailyislam.android.utilities.isNetworkConnected

@UnstableApi
@Composable
fun InitDownloader() {
    val context = LocalContext.current
    MyDownloadHelper.getDownloadManager(context)
    MyDownloadHelper.getDownloads()
}


@UnstableApi
@Composable
fun downloadedStateMedia(mediaId: String): DownloadedStateMedia {

    val binder = LocalPlayerServiceBinder.current

    val cachedBytes by remember(mediaId) {
        try {
            mutableStateOf(binder?.cache?.getCachedBytes(mediaId, 0, -1))
        } catch (e: Exception) {
            mutableLongStateOf(-1L)
        }

    }

    val downloadedBytes by remember(mediaId) {
        try {
            mutableStateOf(binder?.downloadCache?.getCachedBytes(mediaId, 0, -1))
        } catch (e: Exception) {
            mutableLongStateOf(-1)
        }
    }

    // If cache is in error return
    if (cachedBytes == -1L || downloadedBytes == -1L) {
        println("downloadedStateMedia error: mediaId $mediaId cachedBytes $cachedBytes downloadedBytes $downloadedBytes")
        return DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED
    }

    var mediaFormatContentLenght by remember(mediaId) { mutableLongStateOf(0L) }
    LaunchedEffect(mediaId) {
        Database.format(mediaId).distinctUntilChanged().collectLatest { format ->
            mediaFormatContentLenght = format?.contentLength ?: 0L
        }
    }

    var isDownloaded by remember(mediaId) { mutableStateOf(false) }
    LaunchedEffect(mediaId) {
        MyDownloadHelper.getDownload(mediaId).collect { download ->
            isDownloaded = download?.state == Download.STATE_COMPLETED
        }
    }



    var isCached by remember(mediaId) { mutableStateOf(false) }
    isCached = when (cachedBytes){
        0L -> false
        null -> false
        else -> (cachedBytes ?: 0L) >= mediaFormatContentLenght
    }

    println("downloadedStateMedia: mediaId $mediaId contentLength $mediaFormatContentLenght cachedBytes $cachedBytes isCached $isCached downloadedBytes $downloadedBytes isDownloaded $isDownloaded")

    return when {
        isCached -> DownloadedStateMedia.CACHED
        isDownloaded -> DownloadedStateMedia.DOWNLOADED
        //isDownloaded && isCached -> DownloadedStateMedia.CACHED_AND_DOWNLOADED
        //isDownloaded && !isCached -> DownloadedStateMedia.DOWNLOADED
        //!isDownloaded && isCached -> DownloadedStateMedia.CACHED
        else -> DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED
    }
}


@UnstableApi
fun manageDownload(
    context: android.content.Context,
    mediaItem: MediaItem,
    downloadState: Boolean = false
) {

    if (mediaItem.isLocal || !isNetworkConnected(appContext())) return

    if (downloadState) {
        MyDownloadHelper.removeDownload(context = context, mediaItem = mediaItem)
    } else {
        if (isNetworkConnected(context)) {
            MyDownloadHelper.addDownload(context = context, mediaItem = mediaItem)
        }
    }

}

@UnstableApi
fun preCacheMedia(
    context: Context,
    mediaItem: MediaItem
) {
    if (mediaItem.isLocal || !isNetworkConnected(appContext())) return
    val cache: SimpleCache by lazy {
        principalCache.getInstance(context)
    }
    val downloadCache: SimpleCache by lazy {
        MyDownloadHelper.getDownloadCache(context) as SimpleCache
    }
    var contentLength = 0L
    CoroutineScope(Dispatchers.IO).launch {
         contentLength = Database.formatContentLength(mediaItem.mediaId).also {
             println("preCacheMedia: contentLength inside is $it")
         }
    }
    println("preCacheMedia: mediaId ${mediaItem.mediaId} $contentLength")
    val isCached = try {
        cache.isCached(mediaItem.mediaId,0L, contentLength)
    } catch (e: Exception) {
        false
    }
    val isDownloaded = try {
        downloadCache.isCached(mediaItem.mediaId,0L, contentLength)
    } catch (e: Exception) {
        false
    }
    if (!isCached && !isDownloaded) {
        println("preCacheMedia: mediaId ${mediaItem.mediaId} not cached or downloaded")
        MyPreCacheHelper.addDownload(context = context, mediaItem = mediaItem)
    } else println("preCacheMedia: mediaId ${mediaItem.mediaId} is cached $isCached or downloaded $isDownloaded ")


}

@UnstableApi
@Composable
fun getDownloadState(mediaId: String): Int {
    val downloader = LocalDownloadHelper.current
    val context = LocalContext.current
    if (!isNetworkConnected(context)) return 3

    return downloader.getDownload(mediaId).collectAsState(initial = null).value?.state
        ?: 3
}

@OptIn(UnstableApi::class)
@Composable
fun isDownloadedSong(mediaId: String): Boolean {
    return when (downloadedStateMedia(mediaId)) {
        DownloadedStateMedia.CACHED -> false
        DownloadedStateMedia.CACHED_AND_DOWNLOADED, DownloadedStateMedia.DOWNLOADED -> true
        else -> false
    }
}

@UnstableApi
fun isCachedOrDownloaded(
    context: Context,
    mediaId: String
): Pair<Boolean, Boolean> {
    //if (!isNetworkConnected(appContext())) return false to false
    val cache: SimpleCache by lazy {
        principalCache.getInstance(context)
    }
    val downloadCache: SimpleCache by lazy {
        MyDownloadHelper.getDownloadCache(context) as SimpleCache
    }
    var contentLength = 0L
    CoroutineScope(Dispatchers.IO).launch {
        contentLength = Database.formatContentLength(mediaId).also {
            println("isCachedOrDownloaded: contentLength inside is $it")
        }
    }
    println("isCachedOrDownloaded: mediaId ${mediaId} $contentLength")
    val isCached = try {
        cache.isCached(mediaId,0L, contentLength)
    } catch (e: Exception) {
        false
    }
    val isDownloaded = try {
        downloadCache.isCached(mediaId,0L, contentLength)
    } catch (e: Exception) {
        false
    }

    return isCached to isDownloaded

}