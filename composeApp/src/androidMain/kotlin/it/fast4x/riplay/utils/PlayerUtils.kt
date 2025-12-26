package it.fast4x.riplay.utils


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.preferences.excludeSongsWithDurationLimitKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.extensions.preferences.excludeSongIfIsVideoKey
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.components.themed.SmartMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.ArrayDeque

var GlobalVolume: Float = 0.5f

fun Player.restoreGlobalVolume() {
    volume = GlobalVolume
}

fun Player.setGlobalVolume(v: Float) {
    GlobalVolume = v
}

fun Player.getGlobalVolume(): Float {
    return GlobalVolume
}

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
        setMediaItem(mediaItem.cleaned, true)
    else
        replaceMediaItem(currentMediaItemIndex, mediaItem.cleaned)

    restoreGlobalVolume()
    playWhenReady = true
    prepare()
}

fun Player.playAtIndex(mediaItemIndex: Int) {
    if (excludeMediaItem(getMediaItemAt(mediaItemIndex), globalContext())) return

    seekToDefaultPosition(mediaItemIndex)

    restoreGlobalVolume()
    playWhenReady = true
    prepare()

}

@SuppressLint("Range")
@UnstableApi
fun Player.forcePlayAtIndex(mediaItems: List<MediaItem>, mediaItemIndex: Int) {
    if (excludeMediaItems(mediaItems, globalContext()).isEmpty()) return

    setMediaItems(mediaItems.map { it.cleaned }, mediaItemIndex, C.TIME_UNSET)

    restoreGlobalVolume()
    playWhenReady = true
    prepare()
}
@UnstableApi
fun Player.forcePlayFromBeginning(mediaItems: List<MediaItem>) =
    forcePlayAtIndex(mediaItems, 0)

fun Player.forceSeekToPrevious() {
    seekToPrevious()
}

fun Player.forceSeekToNext() {
    seekToNext()
}

fun Player.playNext() {
    restoreGlobalVolume()
    forceSeekToNext()
}

fun Player.playPrevious() {
    restoreGlobalVolume()
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

    addMediaItem(currentMediaItemIndex + 1, mediaItem.cleaned)
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

    addMediaItems(currentMediaItemIndex + 1, filteredMediaItems.map { it.cleaned })
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
}


fun Player.enqueue(mediaItem: MediaItem, context: Context? = null, queue: Queues) {
     if (context != null && excludeMediaItem(mediaItem, context)) return

    if (!canAddedToQueue(mediaItem, queue)) return

    mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
    println("mediaItem-enqueue extras: ${mediaItem.mediaMetadata.extras}")

    addMediaItem(mediaItemCount, mediaItem.cleaned)
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())

}


@UnstableApi
fun Player.enqueue(
    mediaItems: List<MediaItem>,
    context: Context? = null,
    //queue: Queues
) {
    val filteredMediaItems = if (context != null) excludeMediaItems(mediaItems, context)
    else mediaItems

    addMediaItems(mediaItemCount, filteredMediaItems.map { it.cleaned })
    SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
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

fun Player.findNextMediaItemById(mediaId: String): MediaItem? = runCatching {
    for (i in currentMediaItemIndex until mediaItemCount) {
        if (getMediaItemAt(i).mediaId == mediaId) return getMediaItemAt(i)
    }
    return null
}.getOrNull()

fun Player.findMediaItemIndexById(mediaId: String): Int {
    for (i in currentMediaItemIndex until mediaItemCount) {
        if (getMediaItemAt(i).mediaId == mediaId) {
            return i
        }
    }
    return -1
}

fun Player.excludeMediaItems(mediaItems: List<MediaItem>, context: Context): List<MediaItem> {
    var filteredMediaItems = mediaItems
    runCatching {
        val preferences = context.preferences
        val excludeIfIsVideo = preferences.getBoolean(excludeSongIfIsVideoKey, false)
        if (excludeIfIsVideo) {
            filteredMediaItems = mediaItems.filter { !it.isVideo }
        }
        val excludedVideos = mediaItems.size - filteredMediaItems.size

        if (excludedVideos > 0)
            CoroutineScope(Dispatchers.Main).launch {
                SmartMessage(context.resources.getString(R.string.message_excluded_videos).format(excludedVideos), context = context)
            }

        val excludeSongWithDurationLimit =
            preferences.getEnum(excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled)

        if (excludeSongWithDurationLimit != DurationInMinutes.Disabled) {
            filteredMediaItems = mediaItems.filter {
                it.mediaMetadata.extras?.getString("durationText")?.let { it1 ->
                    durationTextToMillis(it1)
                }!! < excludeSongWithDurationLimit.minutesInMilliSeconds
            }

            val excludedSongs = mediaItems.size - filteredMediaItems.size
            if (excludedSongs > 0)
                CoroutineScope(Dispatchers.Main).launch {
                        SmartMessage(context.resources.getString(R.string.message_excluded_s_songs).format(excludedSongs), context = context)
                }
        }
    }.onFailure {
        Timber.e(it.message)
    }

    return filteredMediaItems
}
fun Player.excludeMediaItem(mediaItem: MediaItem, context: Context): Boolean {
    runCatching {
        val preferences = context.preferences
        val excludeIfIsVideo = preferences.getBoolean(excludeSongIfIsVideoKey, false)
        if (excludeIfIsVideo && mediaItem.isVideo) {
            CoroutineScope(Dispatchers.Main).launch {
                SmartMessage(context.resources.getString(R.string.message_excluded_videos).format(1), context = context)
            }
            return true
        }

        val excludeSongWithDurationLimit =
            preferences.getEnum(excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled)
        if (excludeSongWithDurationLimit != DurationInMinutes.Disabled) {
            val excludedSong = mediaItem.mediaMetadata.extras?.getString("durationText")?.let { it1 ->
                durationTextToMillis(it1)
                }!! <= excludeSongWithDurationLimit.minutesInMilliSeconds

            if (excludedSong)
                CoroutineScope(Dispatchers.Main).launch {
                    SmartMessage(context.resources.getString(R.string.message_excluded_s_songs).format(1), context = context)
                }

            return excludedSong
        }
    }.onFailure {
        Timber.e(it.message)
        return false
    }

    return false

}

val Player.mediaItems: List<MediaItem>
    get() = object : AbstractList<MediaItem>() {
        override val size: Int
            get() = mediaItemCount

        override fun get(index: Int): MediaItem = getMediaItemAt(index)
    }

fun Player.getCurrentQueueIndex(): Int {
    if (currentTimeline.isEmpty) {
        return -1
    }
    var index = 0
    var currentMediaItemIndex = currentMediaItemIndex
    while (currentMediaItemIndex != C.INDEX_UNSET) {
        currentMediaItemIndex = currentTimeline.getPreviousWindowIndex(currentMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
        if (currentMediaItemIndex != C.INDEX_UNSET) {
            index++
        }
    }
    return index
}

fun Player.togglePlayPause() {
    if (!playWhenReady && playbackState == Player.STATE_IDLE) {
        prepare()
    }
    playWhenReady = !playWhenReady
}

fun Player.toggleRepeatMode() {
    repeatMode = when (repeatMode) {
        REPEAT_MODE_OFF -> REPEAT_MODE_ALL
        REPEAT_MODE_ALL -> REPEAT_MODE_ONE
        REPEAT_MODE_ONE -> REPEAT_MODE_OFF
        else -> throw IllegalStateException()
    }
}

fun Player.toggleShuffleMode() {
    shuffleModeEnabled = !shuffleModeEnabled
}

fun Player.getQueueWindows(): List<Timeline.Window> {
    val timeline = currentTimeline
    if (timeline.isEmpty) {
        return emptyList()
    }
    val queue = ArrayDeque<Timeline.Window>()
    val queueSize = timeline.windowCount

    val currentMediaItemIndex: Int = currentMediaItemIndex
    queue.add(timeline.getWindow(currentMediaItemIndex, Timeline.Window()))

    var firstMediaItemIndex = currentMediaItemIndex
    var lastMediaItemIndex = currentMediaItemIndex
    val shuffleModeEnabled = shuffleModeEnabled
    while ((firstMediaItemIndex != C.INDEX_UNSET || lastMediaItemIndex != C.INDEX_UNSET) && queue.size < queueSize) {
        if (lastMediaItemIndex != C.INDEX_UNSET) {
            lastMediaItemIndex = timeline.getNextWindowIndex(lastMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
            if (lastMediaItemIndex != C.INDEX_UNSET) {
                queue.add(timeline.getWindow(lastMediaItemIndex, Timeline.Window()))
            }
        }
        if (firstMediaItemIndex != C.INDEX_UNSET && queue.size < queueSize) {
            firstMediaItemIndex = timeline.getPreviousWindowIndex(firstMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
            if (firstMediaItemIndex != C.INDEX_UNSET) {
                queue.addFirst(timeline.getWindow(firstMediaItemIndex, Timeline.Window()))
            }
        }
    }
    return queue.toList()
}

fun Player.saveMasterQueue() {
    if (!isPersistentQueueEnabled()) return

    CoroutineScope(Dispatchers.Main).launch {
        val mediaItems = currentTimeline.mediaItems
        val mediaItemIndex = currentMediaItemIndex
        val mediaItemPosition = currentPosition

        Timber.d("SaveMasterQueue savePersistentQueue mediaItems ${mediaItems.size} mediaItemIndex $mediaItemIndex mediaItemPosition $mediaItemPosition")

        if (mediaItems.isEmpty()) return@launch

        withContext(Dispatchers.IO) {

            mediaItems.mapIndexed { index, mediaItem ->
                QueuedMediaItem(
                    mediaItem = mediaItem,
                    mediaId = mediaItem.mediaId,
                    //position = if (index == mediaItemIndex) mediaItemPosition else null,
                    position = if (index == mediaItemIndex) mediaItemIndex.toLong() else -1,
                    idQueue = mediaItem.mediaMetadata.extras?.getLong("idQueue", defaultQueueId())
                )
            }.let { queuedMediaItems ->
                if (queuedMediaItems.isEmpty()) return@let

                Database.asyncTransaction {
                    clearQueuedMediaItems()
                    insert(queuedMediaItems)
                    Timber.d("SaveMasterQueue QueuePersistentEnabled Saved mediaItems ${queuedMediaItems.size}")
//                    queuedMediaItems.forEach {
//                        insert(it)
//                        Timber.d("SaveMasterQueue QueuePersistentEnabled Save mediaItem ${it.mediaId}")
//                    }
                }

            }
        }
    }
}

@OptIn(UnstableApi::class)
fun Player.loadMasterQueue() {
    Timber.d("LoadMasterQueue loadPersistentQueue is enabled, called")
    if (!isPersistentQueueEnabled()) return

    println("LoadMasterQueue loadPersistentQueue is enabled, processing")
    Database.asyncQuery {
        val queuedSong = queuedMediaItems()

        if (queuedSong.isEmpty()) return@asyncQuery

        //val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)
        val index = queuedSong.indexOfFirst { (it.position ?: -1) >= 0L }.coerceAtLeast(0)

        runBlocking(Dispatchers.Main) {
            setMediaItems(
                queuedSong.map { mediaItem ->
                    mediaItem.mediaItem.buildUpon()
                        .setUri(mediaItem.mediaItem.mediaId)
                        .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                        .build().apply {
                            mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            mediaMetadata.extras?.putLong("idQueue", mediaItem.idQueue ?: defaultQueueId())
                        }
                },
                index,
                0 //queuedSong[index].position ?: C.TIME_UNSET
            )
            prepare()
        }
    }
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
fun Player.positionAndDurationStateFlow(
    scope: CoroutineScope,
    binder: PlayerService.Binder?
): StateFlow<Pair<Long, Long>> {

    val initialValue = if (currentMediaItem?.isLocal == true) {
        currentPosition to duration
    } else {
        (binder?.onlinePlayerCurrentSecond?.toLong() ?: 0L) to
                (binder?.onlinePlayerCurrentDuration?.toLong() ?: 0L)
    }

    return callbackFlow {
        var isSeeking = false

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isSeeking = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newValue = if (mediaItem?.isLocal == true) {
                    currentPosition to duration
                } else {
                    (binder?.onlinePlayerCurrentSecond?.toLong() ?: 0L) to
                            (binder?.onlinePlayerCurrentDuration?.toLong() ?: 0L)
                }
                trySend(newValue)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK && currentMediaItem?.isLocal == true) {
                    isSeeking = true
                    trySend(currentPosition to duration)
                }
            }
        }

        addListener(listener)

        // Job per il polling continuo della posizione
        val pollJob = launch {
            while (isActive) {
                delay(500) // Aggiorna ogni 500ms
                if (!isSeeking) {
                    val newValue = if (currentMediaItem?.isLocal == true) {
                        currentPosition to duration
                    } else {
                        (binder?.onlinePlayerCurrentSecond?.toLong() ?: 0L) to
                                (binder?.onlinePlayerCurrentDuration?.toLong() ?: 0L)
                    }
                    trySend(newValue)
                }
            }
        }


        awaitClose {
            removeListener(listener)
            pollJob.cancel()
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = initialValue
    )
}

@UnstableApi
class PlayerViewModel (
    private val binder: PlayerService.Binder?
) : ViewModel() {
    val positionAndDuration: StateFlow<Pair<Long, Long>> =
        binder?.player?.positionAndDurationStateFlow(viewModelScope, binder)
            ?: flowOf(0L to 0L).stateIn(viewModelScope, SharingStarted.Eagerly, 0L to 0L)
}

@UnstableApi
class PlayerViewModelFactory(
    private val binder: PlayerService.Binder?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(binder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun rememberEqualizerLauncher(
    audioSessionId: () -> Int?,
    contentType: Int = AudioEffect.CONTENT_TYPE_MUSIC
): State<() -> Unit> {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    return rememberUpdatedState {
        try {
            launcher.launch(
                Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    replaceExtras(EqualizerIntentBundleAccessor.bundle {
                        audioSessionId()?.let { audioSession = it }
                        packageName = context.packageName
                        this.contentType = contentType
                    })
                }
            )
        } catch (e: ActivityNotFoundException) {
            SmartMessage(
                context.resources.getString(R.string.info_not_find_application_audio),
                type = PopupType.Warning,
                context = context
            )
        }
    }
}