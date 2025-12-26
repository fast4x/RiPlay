package it.fast4x.riplay.service

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.defaultQueueId
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.preferences.excludeSongIfIsVideoKey
import it.fast4x.riplay.extensions.preferences.excludeSongsWithDurationLimitKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.cleaned
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.utils.isPersistentQueueEnabled
import it.fast4x.riplay.utils.isPodcast
import it.fast4x.riplay.utils.isVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@UnstableApi
class PlayerServiceQueueViewModel(
    //private val initialQueue: MutableList<MediaItem>
) : ViewModel(), ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerServiceQueueViewModel::class.java)) {
            return PlayerServiceQueueViewModel() as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private var playerController: PlayerService.Binder? = null

    val queue: MutableList<MediaItem> = mutableListOf() //= initialQueue

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _nowPlaying = MutableStateFlow(false)
    val nowPlaying: StateFlow<Boolean> = _nowPlaying.asStateFlow()

    private val _playerState = MutableStateFlow(Player.STATE_IDLE)
    val playerState: StateFlow<Int> = _playerState.asStateFlow()

    fun linkController(controller: PlayerService.Binder) {
        this.playerController = controller
        if (queue.isNotEmpty()) {
            loadCurrentMedia()
        }
    }

    fun onPlayerStateChanged(isPlaying: Boolean, playbackState: Int) {
        _nowPlaying.value = isPlaying
        _playerState.value = playbackState

        if (playbackState == Player.STATE_ENDED) {
            playNext()
        }
    }

    fun add(mediaItem: MediaItem, index: Int) {
        queue.add(index, mediaItem)
    }

    fun add(mediaItem: MediaItem) {
        queue.add(mediaItem)
    }

    fun add(mediaItems: List<MediaItem>, index: Int) {
        queue.addAll(index, mediaItems)
    }

    fun add(mediaItems: List<MediaItem>) {
        queue.addAll(mediaItems)
    }

    fun remove(index: Int) {
        queue.removeAt(index)
    }

    fun remove(mediaItem: MediaItem) {
        queue.remove(mediaItem)
    }

    fun remove(mediaItems: List<MediaItem>) {
        queue.removeAll(mediaItems)
    }

    fun exclude(mediaItem: MediaItem, context: Context): Boolean {
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

    fun exclude(mediaItems: List<MediaItem>, context: Context): List<MediaItem> {
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

    fun count(): Int {
        return queue.size
    }

    fun clear() {
        queue.clear()
    }

    fun save() {
        if (!isPersistentQueueEnabled()) return

        CoroutineScope(Dispatchers.Main).launch {

            if (queue.isEmpty()) return@launch

            withContext(Dispatchers.IO) {

                queue.mapIndexed { index, mediaItem ->
                    QueuedMediaItem(
                        mediaItem = mediaItem,
                        mediaId = mediaItem.mediaId,
                        //position = if (index == mediaItemIndex) mediaItemPosition else null,
                        position = if (index == _currentIndex.value) _currentIndex.value.toLong() else -1,
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

    fun load() {
        Timber.d("LoadMasterQueue loadPersistentQueue is enabled, called")
        if (!isPersistentQueueEnabled()) return

        println("LoadMasterQueue loadPersistentQueue is enabled, processing")
        Database.asyncQuery {
            val queuedSong = queuedMediaItems()

            if (queuedSong.isEmpty()) return@asyncQuery

            val index = queuedSong.indexOfFirst { (it.position ?: -1) >= 0L }.coerceAtLeast(0)
            queue.clear()
            runBlocking(Dispatchers.Main) {
                add(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                                mediaMetadata.extras?.putLong("idQueue", mediaItem.idQueue ?: defaultQueueId())
                            }
                    },
                    index
                )

            }
        }
    }

    fun canAddedToQueue(mediaItem: MediaItem, queue: Queues): Boolean {
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

    fun enqueue(
        mediaItems: List<MediaItem>,
        context: Context? = null,
    ) {
        val filteredMediaItems = if (context != null) exclude(mediaItems, context)
        else mediaItems

        add(filteredMediaItems.map { it.cleaned })
        SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
    }

    fun enqueue(mediaItem: MediaItem, context: Context? = null, queue: Queues) {
        if (context != null && exclude(mediaItem, context)) return

        if (!canAddedToQueue(mediaItem, queue)) return

        mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
        println("mediaItem-enqueue extras: ${mediaItem.mediaMetadata.extras}")

        add(mediaItem.cleaned)
        SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())

    }

    fun addNext(mediaItems: List<MediaItem>, context: Context? = null, queue: Queues) {
        val filteredMediaItems = if (context != null) exclude(mediaItems, context)
        else mediaItems

        filteredMediaItems.forEach { mediaItem ->
            val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
            if (itemIndex >= 0) remove(itemIndex)

            if (canAddedToQueue(mediaItem, queue)) {
                mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
                println("mediaItems-addNext extras: ${mediaItem.mediaMetadata.extras}")
            }
        }

        add(filteredMediaItems.map { it.cleaned }, _currentIndex.value + 1)
        SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
    }

    fun addNext(mediaItem: MediaItem, context: Context? = null, queue: Queues) {
        if (context != null && exclude(mediaItem, context)) return

        val itemIndex = findMediaItemIndexById(mediaItem.mediaId)
        if (itemIndex >= 0) remove(itemIndex)

        if (!canAddedToQueue(mediaItem, queue)) return

        mediaItem.mediaMetadata.extras?.putLong("idQueue", queue.id)
        println("mediaItem-addNext extras: ${mediaItem.mediaMetadata.extras}")

        add(mediaItem.cleaned, _currentIndex.value + 1)
        SmartMessage(globalContext().resources.getString(R.string.done), context = globalContext())
    }

    private fun findMediaItemIndexById(mediaId: String): Int {
        for (i in _currentIndex.value until count()) {
            if (queue[_currentIndex.value].mediaId == mediaId) {
                return i
            }
        }
        return -1
    }

    private fun loadCurrentMedia() {
        val controller = playerController ?: return
        if (queue.isNotEmpty()) {
            val currentMediaItem = queue[_currentIndex.value]
            if (currentMediaItem.isLocal)
                controller.player.forcePlay(currentMediaItem)
            else
                controller.onlinePlayer?.loadVideo(currentMediaItem.mediaId, 0f)
        }
    }

    fun playPrevious() {
        if (_currentIndex.value < queue.size - 1) {
            _currentIndex.value += 1
            loadCurrentMedia()
            //play()
        } else {
            _nowPlaying.value = false
        }
    }

    fun playNext() {
        if (_currentIndex.value > 0) {
            _currentIndex.value -= 1
            loadCurrentMedia()
            //play()
        }
    }

    fun playTo(indice: Int) {
        if (indice in queue.indices) {
            _currentIndex.value = indice
            loadCurrentMedia()
            //play()
        }
    }

}