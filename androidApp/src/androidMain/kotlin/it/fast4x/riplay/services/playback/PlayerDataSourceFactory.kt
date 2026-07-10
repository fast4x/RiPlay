package it.fast4x.riplay.services.playback

import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.musicvault.MusicVaultRepository
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
import java.io.File

@OptIn(UnstableApi::class)
internal fun PlayerService.createLocalDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->

        Timber.d("PlayerService createLocalDataSourceFactory dataSpec: uri=${dataSpec.uri} key=${dataSpec.key}")

        var song: Song? = null

        if (dataSpec.key != null) {
            song = runBlocking {
                Database.getSong(cleanPrefix(dataSpec.key.toString()))
            }
            Timber.d("PlayerService createLocalDataSourceFactory get song from db $song")
        }

        when {
            dataSpec.isMusicVault || song?.isMusicVault == true -> {
                if (song == null) {
                    throw PlaybackException(
                        "Song not found in DB for Music Vault",
                        Throwable(),
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                    )
                }

                Timber.d("PlayerService createLocalDataSourceFactory file as tree > song $song")

                // 1. Chiediamo al Repository di risolvere il file (ritorna Uri o File)
                val resolvedFile = MusicVaultRepository.resolveAudioFile(song)

                // 2. Convertiamo il risultato in un URI che ExoPlayer capisce
                val playableUri = when (resolvedFile) {
                    is Uri -> resolvedFile // Cartella SAF -> content://...
                    is File -> Uri.fromFile(resolvedFile) // Cartella Default -> file:///...
                    else -> null // Il file non esiste più o permessi mancanti
                }

                if (playableUri != null) {
                    Timber.d("PlayerService MusicVault playableUri: $playableUri")
                    // 3. INIETTIAMO L'URI REALE NEL DATASPEC
                    // Manteniamo la key originale intatta per non rompere la cache di ExoPlayer
                    return@Factory dataSpec.withUri(playableUri)
                } else {
                    throw PlaybackException(
                        "MusicVault file not found or inaccessible on device",
                        Throwable(),
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                    )
                }
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