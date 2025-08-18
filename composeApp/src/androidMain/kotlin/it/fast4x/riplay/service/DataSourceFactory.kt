package it.fast4x.riplay.service

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import it.fast4x.riplay.Database
import it.fast4x.riplay.utils.asSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope


@OptIn(UnstableApi::class)
internal fun OfflinePlayerService.createSimpleDataSourceFactory(scope: CoroutineScope): DataSource.Factory {
    return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->

        //println("createSimpleDataSourceFactory dataSpec: uri ${dataSpec.uri} isLocalUri ${dataSpec.isLocalUri} isLocal: ${dataSpec.isLocal}")

        // Get current song from player, is same as current dataSpec
        val mediaItem = runBlocking {
            withContext(Dispatchers.Main) {
                player.currentMediaItem
            }
        }
        // Ensure that the song is in database
        Database.asyncTransaction {
            if (mediaItem != null) {
                insert(mediaItem.asSong)
            }
        }

        when {
            dataSpec.isLocal && dataSpec.isLocalUri -> {
                return@Factory dataSpec
            }
            dataSpec.isLocal && !dataSpec.isLocalUri-> {
                val uri = "${LOCAL_AUDIO_URI_PATH}${dataSpec.key?.removePrefix(LOCAL_KEY_PREFIX)}".toUri()
                return@Factory dataSpec.withUri(uri)
            }
            else -> {
                throw PlaybackException(
                    "File not exists or not on device",
                    Throwable(),
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                )
            }
        }

    }
}


