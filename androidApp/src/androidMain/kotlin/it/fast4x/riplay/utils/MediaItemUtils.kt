package it.fast4x.riplay.utils

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.extensions.experimental.musicvalt.MusicVaultRepository
import it.fast4x.riplay.extensions.experimental.musicvalt.MusicVaultState
import it.fast4x.riplay.services.playback.MediaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


val MediaItem.isVideo: Boolean
    get() = mediaMetadata.extras?.getBoolean("isVideo") == true
val MediaItem.isPodcast: Boolean
    get() = mediaMetadata.extras?.getBoolean("isPodcast") == true
val MediaItem.isRelated: Boolean
    get() = mediaMetadata.extras?.getBoolean("isRelated") == true

val MediaItem.isExplicit: Boolean
    get() {
        val isTitleContain = mediaMetadata.title?.startsWith(EXPLICIT_PREFIX, true )
        val isBundleContain = mediaMetadata.extras?.getBoolean( EXPLICIT_BUNDLE_TAG )

        return isTitleContain == true || isBundleContain == true
    }

val MediaItem.isOfficialContent: Boolean
    get() = mediaMetadata.extras?.getBoolean("isOfficialMusicVideo") == true
            || mediaMetadata.extras?.getBoolean("isOfficialUploadByArtistContent") == true

val MediaItem.isUserGeneratedContent: Boolean
    get() = mediaMetadata.extras?.getBoolean("isUserGeneratedContent") == true


@OptIn(UnstableApi::class)
fun mediaItemToggleLike( mediaItem: MediaItem) {
    CoroutineScope(Dispatchers.IO).launch {
        Database.asyncTransaction {
            if (songExist(mediaItem.mediaId) == 0)
                insert(mediaItem)
            if (getLikedAt(mediaItem.mediaId) in listOf(null, -1L))
                like(
                    mediaItem.mediaId,
                    System.currentTimeMillis()
                )
            else like(
                mediaItem.mediaId,
                null
            )

        }
    }
}

@UnstableApi
fun mediaItemSetLiked( mediaItem: MediaItem ) {
    Database.asyncTransaction {
        if (songExist(mediaItem.mediaId) == 0)
            insert(mediaItem)
        if (getLikedAt(mediaItem.mediaId) in listOf(null, -1L))
            like(
                mediaItem.mediaId,
                System.currentTimeMillis()
            )

    }
}

val Song.asMediaItem: MediaItem
    @UnstableApi
    get() {

        Timber.d("createLocalDataSourceFactory Song.asMediaItem id=${this.id} mediaId=${this.mediaId} musicVaultState=${this.musicVaultState.name} musicVaultFileName ${this.musicVaultFileName}")

        return MediaItem.Builder()
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artistsText)
                    .setArtworkUri(thumbnailUrl?.toUri())
                    .setExtras(
                        bundleOf(
                            "durationText" to durationText,
                            EXPLICIT_BUNDLE_TAG to title.startsWith(EXPLICIT_PREFIX, true),
                            "isVideo" to (isAudioOnly != 1),
                            "isPodcast" to (isPodcast == 1),
                            "isDisliked" to (likedAt?.toInt() == -1),
                            "isLiked" to ((likedAt?.toInt() ?: 0) > 0),
                            "mediaId" to mediaId,
                            // MusicVault
                            "musicVaultState" to musicVaultState.name,
                            "musicVaultFileName" to musicVaultFileName,
                            "musicVaultThumbnailFileName" to musicVaultThumbnailFileName
                        )
                    )
                    .build()
            )
            .setMediaId(id)
            .setUri(
                when {
                    // Canzone MusicVault — usa URI SAF o path fisico
                    musicVaultState == MusicVaultState.COMPLETED -> {
                        val fileName = musicVaultFileName ?: id.toUri().toString()
                        if (fileName.startsWith("content://")) {
                            fileName.toUri()
                        } else {
                            File(
                                MusicVaultRepository.getOutputDir(),
                                fileName
                            ).toUri()
                        }
                    }
                    // Canzone locale da MediaStore
                    isLocal -> ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.substringAfter(LOCAL_KEY_PREFIX).toLong()
                    )
                    // Canzone online
                    else -> id.toUri()
                }
            )
            .setCustomCacheKey(
                when {
                    musicVaultState == MusicVaultState.COMPLETED -> "$MUSIC_VAULT_KEY_PREFIX$id"
                    //id.startsWith(LOCAL_KEY_PREFIX) -> id
                    else -> id
                }
            )
            .build()
    }

/*
val Song.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        EXPLICIT_BUNDLE_TAG to title.startsWith(EXPLICIT_PREFIX, true ),
                        "isVideo" to (isAudioOnly != 1),
                        "isPodcast" to (isPodcast == 1),
                        "isDisliked" to (likedAt?.toInt() == -1),
                        "isLiked" to ((likedAt?.toInt() ?: 0) > 0),
                        "mediaId" to mediaId
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(
            if (isLocal) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else id.toUri()
        )
        .setCustomCacheKey(id)
        .build()
*/

val Song.asVideoMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        EXPLICIT_BUNDLE_TAG to title.startsWith(EXPLICIT_PREFIX, true ),
                        "isVideo" to (isAudioOnly != 1),
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(
            if (isLocal) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else id.toUri()
        )
        .setCustomCacheKey(id)
        .build()

val SongEntity.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artistsText)
                .setAlbumTitle(albumTitle)
                .setArtworkUri(song.thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to song.durationText,
                        EXPLICIT_BUNDLE_TAG to song.title.startsWith(EXPLICIT_PREFIX, true),
                        "isVideo" to (song.isAudioOnly != 1),
                        "isPodcast" to (song.isPodcast == 1),
                        // MusicVault
                        "musicVaultState" to song.musicVaultState.name,
                        "musicVaultFileName" to song.musicVaultFileName,
                        "musicVaultThumbnailFileName" to song.musicVaultThumbnailFileName
                    )
                )
                .build()
        )
        .setMediaId(song.id)
        .setUri(
            when {
                // Canzone MusicVault
                song.isMusicVault -> {
                    val fileName = song.musicVaultFileName ?: song.id.toUri().toString()
                    if (fileName.startsWith("content://")) {
                        Uri.parse(fileName)
                    } else {
                        File(
                            MusicVaultRepository.getOutputDir(),
                            fileName
                        ).toUri()
                    }
                }
                // Canzone locale da MediaStore
                song.isLocal -> ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    song.id.substringAfter(LOCAL_KEY_PREFIX).toLong()
                )
                // Canzone online
                else -> song.id.toUri()
            }
        )
        .setCustomCacheKey(
            when {
                song.isMusicVault -> "$MUSIC_VAULT_KEY_PREFIX${song.id}"
                else -> song.id
            }
        )
        .build()

/*
val SongEntity.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artistsText)
                .setAlbumTitle(albumTitle)
                .setArtworkUri(song.thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to song.durationText,
                        EXPLICIT_BUNDLE_TAG to song.title.startsWith(EXPLICIT_PREFIX, true ),
                        "isVideo" to (song.isAudioOnly != 1),
                        "isPodcast" to (song.isPodcast == 1)
                    )
                )
                .build()
        )
        .setMediaId(song.id)
        .setUri(
            if (song.isLocal && !song.isMusicVault) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else song.id.toUri()
        )
        .setCustomCacheKey(song.id)
        .build()
*/

val MediaItem.asSong: Song
    @UnstableApi
    get() = Song(
        id = mediaId,
        title = mediaMetadata.title.toString(),
        artistsText = mediaMetadata.artist.toString(),
        durationText = mediaMetadata.extras?.getString("durationText"),
        thumbnailUrl = mediaMetadata.artworkUri.toString(),
        isAudioOnly = if (mediaMetadata.extras?.getBoolean("isVideo") == true) 0 else 1,
        // MusicVault
        musicVaultState = mediaMetadata.extras?.getString("musicVaultState")
            ?.let { MusicVaultState.valueOf(it) } ?: MusicVaultState.NONE,
        musicVaultFileName = mediaMetadata.extras?.getString("musicVaultFileName"),
        musicVaultThumbnailFileName = mediaMetadata.extras?.getString("musicVaultThumbnailFileName")
    )

val Song.asMediaInfo: MediaInfo
        get() = MediaInfo(
            mediaItem = this.asMediaItem
        )

val MediaItem.asRelated: MediaItem
    get() {
        val mediaItem = this
        mediaItem.mediaMetadata.extras?.putBoolean("isRelated", true)
        return mediaItem
    }

val MediaItem.origin: String
    get() = when {
        this.isMusicVault -> "MUSIC VAULT"
        this.isLocal -> appContext().resources.getString(R.string.local_now_playing_title)
        else -> appContext().resources.getString(R.string.online_now_playing_title)
    }