package it.fast4x.riplay.services.playback

import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.utils.isAtLeastAndroid10
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isLocalUri
import it.fast4x.riplay.utils.isMusicVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
internal fun PlayerService.createLocalDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->

        Timber.d("PlayerService createLocalDataSourceFactory dataSpec: uri=${dataSpec.uri} key=${dataSpec.key}")

        var song: Song? = null

        // Se key è null, non facciamo la query
        if (dataSpec.key != null) {
            // Blocchiamo il thread corrente finché la query non è finita
            song = runBlocking {
                Database.getSong(cleanPrefix(dataSpec.key.toString()))
            }
            Timber.d("PlayerService createLocalDataSourceFactory get song from db $song")
        }

        Timber.d("PlayerService createLocalDataSourceFactory after get song from db $song")

        when {
            dataSpec.isMusicVault || song?.isMusicVault == true -> {
                // Se qui ti serve usare 'song', ora non sarà più null (se trovato nel DB)
                Timber.d("PlayerService createLocalDataSourceFactory file as tree > song $song")
                return@Factory dataSpec
            }
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