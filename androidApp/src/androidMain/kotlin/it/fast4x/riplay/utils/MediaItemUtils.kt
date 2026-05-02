package it.fast4x.riplay.utils

import android.content.ContentUris
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.services.playback.MediaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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
                        EXPLICIT_BUNDLE_TAG to song.title.startsWith(EXPLICIT_PREFIX, true ),
                        "isVideo" to (song.isAudioOnly != 1),
                        "isPodcast" to (song.isPodcast == 1)
                    )
                )
                .build()
        )
        .setMediaId(song.id)
        .setUri(
            if (song.isLocal) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else song.id.toUri()
        )
        .setCustomCacheKey(song.id)
        .build()

val MediaItem.asSong: Song
    @UnstableApi
    get() = Song (
        id = mediaId,
        title = mediaMetadata.title.toString(),
        artistsText = mediaMetadata.artist.toString(),
        durationText = mediaMetadata.extras?.getString("durationText"),
        thumbnailUrl = mediaMetadata.artworkUri.toString(),
        isAudioOnly = if (mediaMetadata.extras?.getBoolean("isVideo") == true) 0 else 1
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
