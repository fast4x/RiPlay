package it.fast4x.riplay.service.modern

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import it.fast4x.riplay.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.asSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope


@OptIn(UnstableApi::class)
internal fun PlayerServiceModern.createSimpleDataSourceFactory(scope: CoroutineScope): DataSource.Factory {
    return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->

        // Get song from player
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

        return@Factory dataSpec
    }
}


