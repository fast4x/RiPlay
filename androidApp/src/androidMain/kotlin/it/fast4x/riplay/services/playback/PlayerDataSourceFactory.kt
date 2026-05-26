package it.fast4x.riplay.services.playback

import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.musicvalt.MusicVaultState
import it.fast4x.riplay.utils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.collectLatest
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isLocalUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(UnstableApi::class)
internal fun PlayerService.createLocalDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->

        //Timber.d("PlayerService createLocalDataSourceFactory dataSpec: uri ${dataSpec.uri} isLocalUri ${dataSpec.isLocalUri} isLocal: ${dataSpec.isLocal} isMusicVault ${dataSpec.isMusicVault}")
        Timber.d("PlayerService createLocalDataSourceFactory dataSpec: uri=${dataSpec.uri} key=${dataSpec.key}")

        // Get current song from player, is same as current dataSpec
//        val mediaItem = runBlocking {
//            withContext(Dispatchers.Main) {
//                player.currentMediaItem
//            }
//        }

//        val song = runBlocking {
//            withContext(Dispatchers.IO) {
//                Database.song(dataSpec.key).firstOrNull()
//            }
//        }
//
//        Timber.d("PlayerService createLocalDataSourceFactory mediaId=${song?.id} musicVaultFileName=${song?.musicVaultFileName}")

        // Ensure that the song is in database
//        Database.asyncTransaction {
//            if (mediaItem != null) {
//                insert(mediaItem.asSong)
//            }
//        }


        when {
            dataSpec.uri.toString().startsWith("content://com.android.externalstorage.documents/tree") -> {
                return@Factory dataSpec
            }
//            song?.isMusicVault == true -> {
//                val uri = song.musicVaultFileName?.toUri() // mediaItem.mediaMetadata.extras?.getString("musicVaultFileName")?.toUri()
//                    ?:  throw PlaybackException(
//                        "PlayerService createLocalDataSourceFactory File not exists or not on device",
//                        Throwable(),
//                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
//                    )
//                Timber.d("PlayerService createLocalDataSourceFactory MusicVault: $uri")
//                return@Factory dataSpec.withUri(uri)
//            }
            dataSpec.isLocal && dataSpec.isLocalUri -> {
                Timber.d("PlayerService createLocalDataSourceFactory dataSpec.isLocalUri: YES")
                return@Factory dataSpec
            }
            dataSpec.isLocal && !dataSpec.isLocalUri -> {
                val contentUriBase =
                    if (isAtLeastAndroid10) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val id = dataSpec.key?.removePrefix(LOCAL_KEY_PREFIX)
                val contentUri = contentUriBase.buildUpon().appendPath(id).build()
                Timber.d("PlayerService createLocalDataSourceFactory dataSpec.isLocal: yes contentUri: $contentUri")
                return@Factory dataSpec.withUri(contentUri)
            }
            else -> {
                throw PlaybackException(
                    "PlayerService createLocalDataSourceFactory File not exists or not on device",
                    Throwable(),
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                )
            }
        }

    }
}
