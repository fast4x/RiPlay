package it.fast4x.riplay.utils


import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.musicvault.MusicVaultState
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.isVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds


const val LOCAL_KEY_PREFIX = "local:"
const val MUSIC_VAULT_KEY_PREFIX = "musicvault:"
const val SPOTIFY_TRACK_KEY_PREFIX = "spotify:track:"
const val DEEZER_TRACK_KEY_PREFIX = "deezer:track:"

const val WEBDAV_KEY_PREFIX = "webdav:"

val String.isSpotifyTrack: Boolean
        get() = this.startsWith(SPOTIFY_TRACK_KEY_PREFIX)
val Song.isSpotifyTrack: Boolean
    get() = this.id.startsWith(SPOTIFY_TRACK_KEY_PREFIX)

val Song.isDeezerTrack: Boolean
    get() = this.id.startsWith(DEEZER_TRACK_KEY_PREFIX)
val String.isDeezerTrack: Boolean
    get() = this.startsWith(DEEZER_TRACK_KEY_PREFIX)


val DataSpec.isLocal
    @OptIn(UnstableApi::class)
    get() = key?.startsWith(LOCAL_KEY_PREFIX) == true
            || uri.toString().startsWith(LOCAL_KEY_PREFIX)


val DataSpec.isMusicVault
    @OptIn(UnstableApi::class)
    get() = uri.toString().startsWith("content://com.android.externalstorage.documents/tree")

@get:OptIn(UnstableApi::class)
val DataSpec.isLocalUri get() = uri.toString().startsWith("content://")

@get:OptIn(UnstableApi::class)
val DataSpec.isWebDav
    get() =  key?.startsWith(WEBDAV_KEY_PREFIX) == true
            || uri.toString().startsWith(WEBDAV_KEY_PREFIX)

@get:OptIn(UnstableApi::class)
val MediaItem.isLocal get() = mediaId.startsWith(LOCAL_KEY_PREFIX)
        || mediaMetadata.extras?.getString("musicVaultState") == MusicVaultState.COMPLETED.name
        || mediaId.startsWith(WEBDAV_KEY_PREFIX)

@get:OptIn(UnstableApi::class)
val MediaItem.isMusicVault get() =
    mediaMetadata.extras?.getString("musicVaultState") == MusicVaultState.COMPLETED.name

@get:OptIn(UnstableApi::class)
val MediaItem.isWebDav get() = mediaId.startsWith(WEBDAV_KEY_PREFIX)

val Song.isLocal get() = id.startsWith(LOCAL_KEY_PREFIX)
        || musicVaultState == MusicVaultState.COMPLETED
        || id.startsWith(WEBDAV_KEY_PREFIX)

val Song.isExclusivelyLocal get() = id.startsWith(LOCAL_KEY_PREFIX)


val Song.isMusicVault get() = musicVaultState == MusicVaultState.COMPLETED

val String.isLocal get() = this.startsWith(LOCAL_KEY_PREFIX)

val Song.isWebDav get() = this.id.startsWith(WEBDAV_KEY_PREFIX)

fun Player.isNowPlaying(mediaId: String): Boolean {
    return mediaId == currentMediaItem?.mediaId
}

val Player.currentWindow: Timeline.Window?
    get() = if (mediaItemCount == 0) null else currentTimeline.getWindow(currentMediaItemIndex, Timeline.Window())

val Timeline.mediaItems: List<MediaItem>
    get() = List(windowCount) {
        getWindow(it, Timeline.Window()).mediaItem
    }

inline val Timeline.windows: List<Timeline.Window>
    get() = List(windowCount) {
        getWindow(it, Timeline.Window())
    }

val Player.shouldBePlaying: Boolean
    get() = !(playbackState == Player.STATE_ENDED || !playWhenReady)

fun Player.removeMediaItems(range: IntRange) = removeMediaItems(range.first, range.last + 1)

fun Player.seamlessPlay(mediaItem: MediaItem) {
    if (mediaItem.mediaId == currentMediaItem?.mediaId) {
        if (currentMediaItemIndex > 0) removeMediaItems(0 until currentMediaItemIndex)
        if (currentMediaItemIndex < mediaItemCount - 1)
            removeMediaItems(currentMediaItemIndex + 1 until mediaItemCount)
    } else forcePlay(mediaItem)
    Timber.d("PlayerService-seamlessPlay mediaItem: ${mediaItem.mediaId}")
}

fun Player.seamlessQueue(mediaItem: MediaItem) {
    if (mediaItem.mediaId == currentMediaItem?.mediaId) {
        if (currentMediaItemIndex > 0) removeMediaItems(0 until currentMediaItemIndex)
        if (currentMediaItemIndex < mediaItemCount - 1)
            removeMediaItems(currentMediaItemIndex + 1 until mediaItemCount)
    }
}


fun Player.shuffleQueue() {
    val mediaItems = currentTimeline.mediaItems.toMutableList().apply { removeAt(currentMediaItemIndex) }
    if (currentMediaItemIndex > 0) removeMediaItems(0, currentMediaItemIndex)
    if (currentMediaItemIndex < mediaItemCount - 1) removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
    addMediaItems(mediaItems.shuffled())
}

fun Player.forcePlay(mediaItem: MediaItem, replace: Boolean = false) {
    if (excludeMediaItem(mediaItem, globalContext())) return

    if (!replace)
        setMediaItem(mediaItem, true)
    else
        replaceMediaItem(currentMediaItemIndex, mediaItem)

    //restoreGlobalVolume()
    playWhenReady = true
    prepare()
    //Timber.d("PlayerService-forcePlay withReplace $replace mediaItem: ${mediaItem.mediaId} currentMediaItemIndex: $currentMediaItemIndex shuffleModeEnabled $shuffleModeEnabled repeatMode $repeatMode")
}

fun Player.playAtIndex(mediaItemIndex: Int) {
    if (excludeMediaItem(getMediaItemAt(mediaItemIndex), globalContext())) return

    seekToDefaultPosition(mediaItemIndex)

    //restoreGlobalVolume()
    playWhenReady = true
    prepare()

}

@SuppressLint("Range")
@UnstableApi
fun Player.forcePlayAtIndex(mediaItems: List<MediaItem>, mediaItemIndex: Int) {
    // Disabilita shuffle per assicurarti che l'indice sia rispettato
    shuffleModeEnabled = false

    setMediaItems(mediaItems, mediaItemIndex, C.TIME_UNSET)

    //restoreGlobalVolume()
    playWhenReady = true
    prepare()
}

@UnstableApi
fun Player.forcePlayFromBeginning(mediaItems: List<MediaItem>) =
    CoroutineScope(Dispatchers.Main).launch {
        forcePlayAtIndex(mediaItems, 0)
    }

fun Player.forceSeekToPrevious() {
    val prevIndex = previousMediaItemIndex
    if (prevIndex != C.INDEX_UNSET) {
        seekToDefaultPosition(prevIndex)
    }

}

fun Player.forceSeekToNext() {
    seekToNext()
}

fun Player.playNext() {
    forceSeekToNext()
}

fun Player.playPrevious() {
    forceSeekToPrevious()
}

@UnstableApi
fun Player.addNext(mediaItem: MediaItem, context: Context? = null, queue: Queues) {
    if (context != null && excludeMediaItem(mediaItem, context)) return

    val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
    if (itemIndex >= 0) removeMediaItem(itemIndex)

    if (!canAddedToQueue(mediaItem, queue)) return

    mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
    println("mediaItem-addNext extras: ${mediaItem.mediaMetadata.extras}")

    addMediaItem(currentMediaItemIndex + 1, mediaItem)
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
}

@UnstableApi
fun Player.addNext(mediaItems: List<MediaItem>, context: Context? = null, queue: Queues) {
    val filteredMediaItems = if (context != null) excludeMediaItems(mediaItems, context)
    else mediaItems

    filteredMediaItems.forEach { mediaItem ->
        val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
        if (itemIndex >= 0) removeMediaItem(itemIndex)

        if (canAddedToQueue(mediaItem, queue)) {
            mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
            println("mediaItems-addNext extras: ${mediaItem.mediaMetadata.extras}")
        }
    }

    addMediaItems(currentMediaItemIndex + 1, filteredMediaItems)
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
}


fun Player.enqueue(mediaItem: MediaItem, context: Context? = null, queue: Queues) {
     if (context != null && excludeMediaItem(mediaItem, context)) return

    if (!canAddedToQueue(mediaItem, queue)) return

    mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
    println("mediaItem-enqueue extras: ${mediaItem.mediaMetadata.extras}")

    addMediaItem(mediaItemCount, mediaItem)
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())

}


@UnstableApi
fun Player.enqueue(
    mediaItems: List<MediaItem>,
    context: Context? = null,
) {
    val filteredMediaItems = if (context != null) excludeMediaItems(mediaItems, context)
    else mediaItems

    addMediaItems(mediaItemCount, filteredMediaItems)
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
}

/**
 * Rimuove in modo sicuro tutti i video dalla coda
 */
fun Player.removeVideoMediaItems() {
    var modificationsMade = false

    // Iterazione inversa per evitare problemi di indice
    for (i in this.mediaItemCount - 1 downTo 0) {
        val item = this.getMediaItemAt(i)

        if (item.isVideo) {
            this.removeMediaItem(i)
            modificationsMade = true
        }
    }

    // Se la playlist diventa vuota, metto in pausa che il player sia in pausa
//    if (modificationsMade && this.mediaItemCount == 0) {
//        this.pause()
//    }
}

fun Player.canAddedToQueue(mediaItem: MediaItem, queue: Queues): Boolean {
    if (mediaItem.isVideo && !queue.acceptVideo) {
        SmartMessage("Queue not accept video", type = PopupType.Warning, context = globalContext())
        return false
    }
    if (!mediaItem.isVideo && !queue.acceptSong) {
        SmartMessage("Queue not accept song", type = PopupType.Warning, context = globalContext())
        return false
    }
    if (mediaItem.isPodcast && !queue.acceptPodcast) {
        SmartMessage("Queue not accept podcast", type = PopupType.Warning, context = globalContext())
        return false
    }

    return true
}

fun Player.findMediaItemIndexById(mediaId: String): Int {
    for (i in currentMediaItemIndex until mediaItemCount) {
        if (getMediaItemAt(i).mediaId == mediaId) {
            return i
        }
    }
    return -1
}


fun Player.excludeMediaItems(mediaItems: List<MediaItem>, context: Context): List<MediaItem> {
    return try {
        val appSettings = getAppSettings()

        var filteredMediaItems = mediaItems

        // --- Escludi Video ---
        val excludeIfIsVideo = appSettings.excludeIfIsVideo
        if (excludeIfIsVideo) {
            filteredMediaItems = filteredMediaItems.filter { !it.isVideo }
        }

        // --- Escludi per Durata ---
        val excludeSongWithDurationLimit = appSettings.excludeSongWithDurationLimit

        if (excludeSongWithDurationLimit != DurationInMinutes.Disabled) {
            filteredMediaItems = filteredMediaItems.filter { item ->
                try {
                    val durationMillis = item.mediaMetadata.extras
                        ?.getString("durationText")
                        ?.let { durationTextToMillis(it) }
                        ?: 0L

                    durationMillis < excludeSongWithDurationLimit.milliSeconds
                } catch (e: Exception) {
                    Timber.w(e, "Errore parsing durata per ${item.mediaId}")
                    false
                }
            }
        }

        // --- Blacklist (DB) ---
        filteredMediaItems = filteredMediaItems.filter { item ->
            try {
                if (item.mediaId.isEmpty()) return@filter true

                val isBlacklisted = runBlocking(Dispatchers.IO) {
                    Database.blacklisted(item.mediaId)
                } != 0L

                // Mantieni l'item se NON è blacklistato
                !isBlacklisted
            } catch (e: Exception) {
                Timber.e(e, "Errore DB blacklist per ${item.mediaId}")
                true
            }
        }

        // --- FEEDBACK VISIVO ---
        val excludedSongs = mediaItems.size - filteredMediaItems.size
        if (excludedSongs > 0) {
            showExcludedMessage(context, R.string.message_excluded_s_songs, excludedSongs)
        }

        filteredMediaItems
    } catch (e: Exception) {
        Timber.e(e, "Errore generico in excludeMediaItems")
        mediaItems
    }
}

fun Player.excludeMediaItem(mediaItem: MediaItem, context: Context): Boolean {
    return try {
        val appSettings = getAppSettings()

        // --- CHECK VIDEO ---
        val excludeIfIsVideo = appSettings.excludeIfIsVideo
        if (excludeIfIsVideo && mediaItem.isVideo) {
            showExcludedMessage(context, R.string.message_excluded_videos, 1)
            return true
        }

        // --- CHECK DURATA ---
        val excludeSongWithDurationLimit = appSettings.excludeSongWithDurationLimit

        if (excludeSongWithDurationLimit != DurationInMinutes.Disabled) {
            val durationMillis = try {
                mediaItem.mediaMetadata.extras
                    ?.getString("durationText")
                    ?.let { durationTextToMillis(it) }
                    ?: 0L
            } catch (e: Exception) {
                Timber.w(e, "Errore nel parsing della durata mediaItem")
                0L
            }

            val excludedSong = durationMillis <= excludeSongWithDurationLimit.milliSeconds

            if (excludedSong) {
                showExcludedMessage(context, R.string.message_excluded_s_songs, 1)
            }
            return excludedSong
        }

        // --- CHECK BLACKLIST (Database) ---
        val listed = try {
            if (mediaItem.mediaId.isNotEmpty()) {
                runBlocking(Dispatchers.IO) {
                    Database.blacklisted(mediaItem.mediaId)
                } != 0L
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Errore durante il controllo della blacklist sul Database")
            false
        }

        if (listed) {
            showExcludedMessage(context, R.string.message_excluded_s_songs,1)
        }

        listed
    } catch (e: Exception) {
        Timber.e(e, "Errore critico imprevisto in excludeMediaItem")
        false
    }
}


private fun showExcludedMessage(context: Context, messageResId: Int, count: Int) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            SmartMessage(
                context.resources.getString(messageResId).format(count),
                context = context
            )
        } catch (e: Exception) {
            Timber.e(e, "Impossibile mostrare messaggio esclusione lista")
        }
    }
}

val Player.mediaItems: List<MediaItem>
    get() = object : AbstractList<MediaItem>() {
        override val size: Int
            get() = mediaItemCount

        override fun get(index: Int): MediaItem = getMediaItemAt(index)
    }


@Composable
inline fun Player.DisposableListener(crossinline listenerProvider: () -> Player.Listener) {
    DisposableEffect(this) {
        val listener = listenerProvider()
        addListener(listener)
        onDispose { removeListener(listener) }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayerPositionAndDuration(binder: PlayerService.Binder?): Pair<Long, Long> {
    val player = binder?.exoPlayer
    val default = 0L to 0L

    if (player == null) return default

    return produceState(initialValue = default, player) {
        while (isActive) {
            value = if (player.currentMediaItem?.isLocal == true) {
                player.currentPosition to player.duration
            } else {
                val onlineCurrentSecond = binder.youtubePlayerCurrentSecond.value
                val onlineCurrentDuration = binder.youtubePlayerCurrentDuration.value
                (onlineCurrentSecond.toLong() * 1000L) to (onlineCurrentDuration.toLong() * 1000L)
            }
            delay(200.milliseconds) // Aggiorna l'UI 5 volte al secondo, fluido e non pesante
        }
    }.value
}


fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}