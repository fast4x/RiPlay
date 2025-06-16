package it.fast4x.riplay.service

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import it.fast4x.environment.Environment
import it.fast4x.riplay.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import it.fast4x.riplay.appContext
import it.fast4x.riplay.extensions.players.SelectSimplePlayerType
import it.fast4x.riplay.extensions.players.SimplePlayer
import it.fast4x.riplay.models.Format
import it.fast4x.riplay.utils.principalCache
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
internal fun MyDownloadHelper.createSimpleDataSourceFactory(): DataSource.Factory {
    val songUrlCache = HashMap<String, Pair<String, Long>>()
    return ResolvingDataSource.Factory(
        CacheDataSource
            .Factory()
            .setCache(getDownloadCache(appContext()))
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(
                    OkHttpClient
                        .Builder()
                        .proxy(Environment.proxy)
                        .build(),
                ),
            )
    ) { dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        val length = if (dataSpec.length >= 0) dataSpec.length else 1

        val isDownloaded = try {
            downloadCache.isCached(mediaId, dataSpec.position, length)
        } catch (e: Exception) {
            false
        }

        if( dataSpec.isLocal || isDownloaded ) {
            return@Factory dataSpec
        }

        songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
            return@Factory dataSpec.withUri(it.first.toUri())
        }

        val playedFormat = runBlocking(Dispatchers.IO) { Database.format(mediaId).first() }
        val playbackData = runBlocking(Dispatchers.IO) {
            SelectSimplePlayerType(
                mediaId,
                playedFormat,
                audioQualityFormat
            )
//            SimplePlayer.playerResponseForPlayback(
//                mediaId,
//                playedFormat = playedFormat,
//                audioQuality = audioQualityFormat,
//            )
        }.getOrThrow()
        val format = playbackData.format

        Database.asyncTransaction {
            if (songExist(mediaId) > 0)
                upsert(
                    Format(
                        songId = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        //codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate.toLong(),
                        //sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        //playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )
        }

        val streamUrl = playbackData.streamUrl.let {
            // Specify range to avoid YouTube's throttling
            "${it}&range=0-${format.contentLength ?: 10000000}"
        }

        songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
        dataSpec.withUri(streamUrl.toUri())
    }
}


@OptIn(UnstableApi::class)
internal fun MyPreCacheHelper.createSimpleDataSourceFactory(): DataSource.Factory {
    val songUrlCache = HashMap<String, Pair<String, Long>>()
    return ResolvingDataSource.Factory(
        CacheDataSource
            .Factory()
            .setCache(principalCache.getInstance(appContext()))
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(
                    OkHttpClient
                        .Builder()
                        .proxy(Environment.proxy)
                        .build(),
                ),
            )
    ){ dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        val length = if (dataSpec.length >= 0) dataSpec.length else 1

        val isCached = try {
            cache.isCached(mediaId, dataSpec.position, length)
        } catch (e: Exception) {
            false
        }

        if( dataSpec.isLocal || isCached ) {
            return@Factory dataSpec
        }

        songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
            return@Factory dataSpec.withUri(it.first.toUri())
        }

        val playedFormat = runBlocking(Dispatchers.IO) { Database.format(mediaId).first() }
        val playbackData = runBlocking(Dispatchers.IO) {
//            SelectSimplePlayerType(
//                mediaId,
//                playedFormat,
//                audioQualityFormat
//            )
            // Use default streaming player to preCache
            SimplePlayer.playerResponseForPlayback(
                mediaId,
                playedFormat = playedFormat,
                audioQuality = audioQualityFormat,
            )
        }.getOrThrow()
        val format = playbackData.format

        Database.asyncTransaction {
            if (songExist(mediaId) > 0)
                upsert(
                    Format(
                        songId = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        //codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate.toLong(),
                        //sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        //playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )
        }

        val streamUrl = playbackData.streamUrl.let {
            // Specify range to avoid YouTube's throttling
            "${it}&range=0-${format.contentLength ?: 10000000}"
        }

        songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
        dataSpec.withUri(streamUrl.toUri())
    }
}

@OptIn(UnstableApi::class)
internal fun PlayerService.createSimpleDataSourceFactory(): DataSource.Factory {
    val songUrlCache = HashMap<String, Pair<String, Long>>()
    return ResolvingDataSource.Factory(
        CacheDataSource
            .Factory()
            .setCache(principalCache.getInstance(appContext()))
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(
                    OkHttpClient
                        .Builder()
                        .proxy(Environment.proxy)
                        .build(),
                ),
            )
    ){ dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        val length = if (dataSpec.length >= 0) dataSpec.length else 1

        val isCached = try {
            cache.isCached(mediaId, dataSpec.position, length)
        } catch (e: Exception) {
            false
        }

        if( dataSpec.isLocal || isCached ) {
            return@Factory dataSpec
        }

        songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
            return@Factory dataSpec.withUri(it.first.toUri())
        }

        val playedFormat = runBlocking(Dispatchers.IO) { Database.format(mediaId).first() }
        val playbackData = runBlocking(Dispatchers.IO) {
            SelectSimplePlayerType(
                mediaId,
                playedFormat,
                audioQualityFormat
            )
//            SimplePlayer.playerResponseForPlayback(
//                mediaId,
//                playedFormat = playedFormat,
//                audioQuality = audioQualityFormat,
//            )
        }.getOrThrow()
        val format = playbackData.format

        Database.asyncTransaction {
            if (songExist(mediaId) > 0)
                upsert(
                    Format(
                        songId = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        //codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate.toLong(),
                        //sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        //playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )
        }

        val streamUrl = playbackData.streamUrl.let {
            // Specify range to avoid YouTube's throttling
            "${it}&range=0-${format.contentLength ?: 10000000}"
        }

        songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
        dataSpec.withUri(streamUrl.toUri())
    }
}

//@OptIn(UnstableApi::class)
//internal fun PlayerService.createDataSourceFactory(): DataSource.Factory {
//    return ResolvingDataSource.Factory(
//        CacheDataSource.Factory()
//            .setCache(downloadCache)
//            .setUpstreamDataSourceFactory(
//                CacheDataSource.Factory()
//                    .setCache(cache)
//                    .setUpstreamDataSourceFactory(
//                        appContext().okHttpDataSourceFactory
//                    )
////            )
////            .setCacheWriteDataSinkFactory(null)
////            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
////    ) { dataSpec: DataSpec ->
////        //try {
////
////        // Get song from player
////        val mediaItem = runBlocking {
////            withContext(Dispatchers.Main) {
////                player.currentMediaItem
////            }
////        }
////        // Ensure that the song is in database
////        Database.asyncTransaction {
////            if (mediaItem != null) {
////                insert(mediaItem.asSong)
////            }
////        }
////
////
////        //println("PlayerService DataSourcefactory currentMediaItem: ${mediaItem?.mediaId}")
////        //dataSpec.key?.let { player.findNextMediaItemById(it)?.mediaMetadata }
////
////        return@Factory runBlocking {
////            try {
////                dataSpecProcess(dataSpec, appContext(), appContext().isConnectionMetered())
////            } catch (e: Exception) {
////                Timber.e("PlayerService DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
////                println("PlayerService DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
////                dataSpec
////            }
////        }
//////        }
//////        catch (e: Throwable) {
//////            println("PlayerService DataSourcefactory Error: ${e.message}")
//////            throw IOException(e)
//////        }
////    }
////}

//@OptIn(UnstableApi::class)
//internal fun MyPreCacheHelper.createDataSourceFactory(): DataSource.Factory {
//    return ResolvingDataSource.Factory(
//        CacheDataSource.Factory()
//            .setCache(principalCache.getInstance(appContext())).apply {
//                setUpstreamDataSourceFactory(
//                    appContext().okHttpDataSourceFactory
//                )
//                setCacheWriteDataSinkFactory(null)
//            }
//    ) { dataSpec: DataSpec ->
//        //try {
//
//            return@Factory runBlocking {
//                try {
//                    dataSpecProcess(dataSpec, appContext(), appContext().isConnectionMetered())
//                } catch (e: Exception) {
//                    Timber.e("MyPreCacheHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
//                    println("MyPreCacheHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
//                    dataSpec
//                }
//            }
////        }
////        catch (e: Throwable) {
////            Timber.e("MyPreCacheHelper DataSourcefactory Error: ${e.stackTraceToString()}")
////            println("MyPreCacheHelper DataSourcefactory Error: ${e.stackTraceToString()}")
////            dataSpec
////        }
//    }
////        .retryIf<UnplayableException>(
////        maxRetries = 3,
////        printStackTrace = true
////    )
////        .retryIf(
////            maxRetries = 1,
////            printStackTrace = true
////        ) { ex ->
////            ex.findCause<InvalidResponseCodeException>()?.responseCode == 403 ||
////                    ex.findCause<ClientRequestException>()?.response?.status?.value == 403 ||
////                    ex.findCause<InvalidHttpCodeException>() != null
////        }.handleRangeErrors()
//}

//@OptIn(UnstableApi::class)
//internal fun MyDownloadHelper.createDataSourceFactory(): DataSource.Factory {
//    return ResolvingDataSource.Factory(
//        CacheDataSource.Factory()
//            .setCache(getDownloadCache(appContext())).apply {
//                setUpstreamDataSourceFactory(
//                    appContext().okHttpDataSourceFactory
//                )
//                setCacheWriteDataSinkFactory(null)
//            }
//    ) { dataSpec: DataSpec ->
//        //try {
//
//            return@Factory runBlocking {
//                try {
//                    dataSpecProcess(dataSpec, appContext(), appContext().isConnectionMetered())
//                } catch (e: Exception) {
//                    Timber.e("MyDownloadHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
//                    println("MyDownloadHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
//                    dataSpec
//                }
//            }
////        } catch (e: Throwable) {
////            Timber.e("MyDownloadHelper DataSourcefactory Error: ${e.stackTraceToString()}")
////            println("MyDownloadHelper DataSourcefactory Error: ${e.stackTraceToString()}")
////            dataSpec
////        }
//    }
////        .retryIf<UnplayableException>(
////        maxRetries = 3,
////        printStackTrace = true
////    )
////        .retryIf(
////            maxRetries = 1,
////            printStackTrace = true
////        ) { ex ->
////            ex.findCause<InvalidResponseCodeException>()?.responseCode == 403 ||
////                    ex.findCause<ClientRequestException>()?.response?.status?.value == 403 ||
////                    ex.findCause<InvalidHttpCodeException>() != null
////        }.handleRangeErrors()
//}