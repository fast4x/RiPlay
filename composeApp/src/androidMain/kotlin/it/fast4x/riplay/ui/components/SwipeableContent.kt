package it.fast4x.riplay.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import it.fast4x.environment.Environment
import it.fast4x.riplay.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.enums.AlbumSwipeAction
import it.fast4x.riplay.enums.PlaylistSwipeAction
import it.fast4x.riplay.enums.QueueSwipeAction
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.albumSwipeLeftActionKey
import it.fast4x.riplay.utils.albumSwipeRightActionKey
import it.fast4x.riplay.utils.isSwipeToActionEnabledKey
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.playlistSwipeLeftActionKey
import it.fast4x.riplay.utils.playlistSwipeRightActionKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.queueSwipeLeftActionKey
import it.fast4x.riplay.utils.queueSwipeRightActionKey
import kotlinx.coroutines.flow.distinctUntilChanged
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.QueuesDialog
import it.fast4x.riplay.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.riplay.utils.addToYtLikedSong
import org.dailyislam.android.utilities.isNetworkConnected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SwipeableContent(
    swipeToLeftIcon: Int? = null,
    swipeToRightIcon: Int? = null,
    onSwipeToLeft: () -> Unit,
    onSwipeToRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance: Float -> distance * 0.25f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {onSwipeToRight();hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)}
            else if (value == SwipeToDismissBoxValue.EndToStart) {onSwipeToLeft();hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)}

            return@rememberSwipeToDismissBoxState false
        }
    )
    val isSwipeToActionEnabled by rememberPreference(isSwipeToActionEnabledKey, true)

    val current = LocalViewConfiguration.current
    CompositionLocalProvider(LocalViewConfiguration provides object : ViewConfiguration by current{
        override val touchSlop: Float
            get() = current.touchSlop * 5f
    }) {
        SwipeToDismissBox(
            gesturesEnabled = isSwipeToActionEnabled,
            modifier = modifier,
            //.padding(horizontal = 16.dp)
            //.clip(RoundedCornerShape(12.dp)),
            state = dismissState,
            backgroundContent = {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        //.background(colorPalette.background1)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                        SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                        SwipeToDismissBoxValue.Settled -> Arrangement.Center
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> if (swipeToRightIcon == null) null else ImageVector.vectorResource(
                            swipeToRightIcon
                        )

                        SwipeToDismissBoxValue.EndToStart -> if (swipeToLeftIcon == null) null else ImageVector.vectorResource(
                            swipeToLeftIcon
                        )

                        SwipeToDismissBoxValue.Settled -> null
                    }
                    if (icon != null)
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colorPalette().accent,
                        )
                }
            }
        ) {
            content()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SwipeableQueueItem(
    mediaItem: MediaItem,
    onPlayNext: (() -> Unit) = {},
    onDownload: (() -> Unit) = {},
    onRemoveFromQueue: (() -> Unit) = {},
    onEnqueue: ((Long) -> Unit) = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current


    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
    }
    val onFavourite: () -> Unit = {
        if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
            SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
        } else if (!isYouTubeSyncEnabled()){
            mediaItemToggleLike(mediaItem)
            val message: String
            val mTitle: String = cleanPrefix(mediaItem.mediaMetadata.title?.toString() ?: "")
            val mArtist: String = mediaItem.mediaMetadata.artist?.toString() ?: ""
            if (likedAt == -1L) {
                message =
                    "\"$mTitle - $mArtist\" ${context.resources.getString(R.string.removed_from_disliked)}"
            } else if (likedAt != null) {
                message =
                    "\"$mTitle - $mArtist\" ${context.resources.getString(R.string.removed_from_favorites)}"
            } else
                message = context.resources.getString(R.string.added_to_favorites)

            SmartMessage(
                message,
                durationLong = likedAt != null,
                context = context
            )
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                addToYtLikedSong(mediaItem)
            }
        }
    }

    val queueSwipeLeftAction by rememberPreference(queueSwipeLeftActionKey, QueueSwipeAction.RemoveFromQueue)
    val queueSwipeRightAction by rememberPreference(queueSwipeRightActionKey, QueueSwipeAction.PlayNext)
    var isViewingQueues by remember { mutableStateOf(false) }

    fun getActionCallback(actionName: QueueSwipeAction): () -> Unit {
        return when (actionName) {
            QueueSwipeAction.PlayNext -> onPlayNext
            QueueSwipeAction.Favourite -> onFavourite
            QueueSwipeAction.RemoveFromQueue -> onRemoveFromQueue
            QueueSwipeAction.Enqueue -> ({ isViewingQueues = true })
            else -> ({})
        }
    }

    if (isViewingQueues) {
        QueuesDialog(
            onSelect = {
                onEnqueue.invoke(it.id)
            },
            onDismiss = { isViewingQueues = false }
        )
    }


    val swipeLeftCallback = getActionCallback(queueSwipeLeftAction)
    val swipeRighCallback = getActionCallback(queueSwipeRightAction)

    SwipeableContent(
        swipeToLeftIcon = null,
        swipeToRightIcon = null,
        onSwipeToLeft = swipeLeftCallback,
        onSwipeToRight = swipeRighCallback,
        modifier = modifier
    ) {
        content()
    }

}

@OptIn(UnstableApi::class)
@Composable
fun SwipeablePlaylistItem(
    mediaItem: MediaItem,
    onPlayNext: (() -> Unit) = {},
    onDownload: (() -> Unit) = {},
    onEnqueue: ((Long) -> Unit) = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
    }
    val onFavourite: () -> Unit = {
        if (!isNetworkConnected(appContext()) && isYouTubeSyncEnabled()) {
            SmartMessage(appContext().resources.getString(R.string.no_connection), context = appContext(), type = PopupType.Error)
        } else if (!isYouTubeSyncEnabled()){
            mediaItemToggleLike(mediaItem)
            val message: String
            val mTitle: String = cleanPrefix(mediaItem.mediaMetadata.title?.toString() ?: "")
            val mArtist: String = mediaItem.mediaMetadata.artist?.toString() ?: ""
            if (likedAt == -1L) {
                message =
                    "\"$mTitle - $mArtist\" ${context.resources.getString(R.string.removed_from_disliked)}"
            } else if (likedAt != null) {
                message =
                    "\"$mTitle - $mArtist\" ${context.resources.getString(R.string.removed_from_favorites)}"
            } else
                message = context.resources.getString(R.string.added_to_favorites)

            SmartMessage(
                message,
                durationLong = likedAt != null,
                context = context
            )
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                addToYtLikedSong(mediaItem)
            }
        }
    }

    val playlistSwipeLeftAction by rememberPreference(playlistSwipeLeftActionKey, PlaylistSwipeAction.Favourite)
    val playlistSwipeRightAction by rememberPreference(playlistSwipeRightActionKey, PlaylistSwipeAction.PlayNext)
    var isViewingQueues by remember { mutableStateOf(false) }

    fun getActionCallback(actionName: PlaylistSwipeAction): () -> Unit {
        return when (actionName) {
            PlaylistSwipeAction.PlayNext -> onPlayNext
            PlaylistSwipeAction.Download -> onDownload
            PlaylistSwipeAction.Favourite -> onFavourite
            PlaylistSwipeAction.Enqueue -> ({ isViewingQueues = true })
            else -> ({})
        }
    }

    if (isViewingQueues) {
        QueuesDialog(
            onSelect = {
                onEnqueue.invoke(it.id)
            },
            onDismiss = { isViewingQueues = false }
        )
    }

    val swipeLeftCallback = getActionCallback(playlistSwipeLeftAction)
    val swipeRighCallback = getActionCallback(playlistSwipeRightAction)


    SwipeableContent(
        swipeToLeftIcon =  null,
        swipeToRightIcon =  null,
        onSwipeToLeft = swipeLeftCallback,
        onSwipeToRight = swipeRighCallback
    ) {
        content()
    }

}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun SwipeableAlbumItem(
    albumItem: Environment.AlbumItem,
    onPlayNext: () -> Unit,
    onEnqueue: () -> Unit,
    onBookmark: () -> Unit,
    content: @Composable () -> Unit
) {
    var bookmarkedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    LaunchedEffect(albumItem.key) {
        Database.albumBookmarkedAt(albumItem.key).distinctUntilChanged().collect { bookmarkedAt = it }
    }

    val albumSwipeLeftAction by rememberPreference(albumSwipeLeftActionKey, AlbumSwipeAction.PlayNext)
    val albumSwipeRightAction by rememberPreference(albumSwipeRightActionKey, AlbumSwipeAction.Bookmark)

    fun getActionCallback(actionName: AlbumSwipeAction): () -> Unit {
        return when (actionName) {
            AlbumSwipeAction.PlayNext -> onPlayNext
            AlbumSwipeAction.Bookmark -> onBookmark
            AlbumSwipeAction.Enqueue -> onEnqueue
            else -> ({})
        }
    }
    val swipeLeftCallback = getActionCallback(albumSwipeLeftAction)
    val swipeRighCallback = getActionCallback(albumSwipeRightAction)

    SwipeableContent(
        swipeToLeftIcon =  albumSwipeLeftAction.getStateIcon(bookmarkedAt),
        swipeToRightIcon =  albumSwipeRightAction.getStateIcon(bookmarkedAt),
        onSwipeToLeft = swipeLeftCallback,
        onSwipeToRight = swipeRighCallback
    ) {
        content()
    }

}