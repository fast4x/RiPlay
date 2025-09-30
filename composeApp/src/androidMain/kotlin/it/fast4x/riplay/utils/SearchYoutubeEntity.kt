package it.fast4x.riplay.utils

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.from
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.items.VideoItemPlaceholder
import it.fast4x.riplay.ui.screens.searchresult.ItemsPage
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.models.defaultQueue
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.screens.player.fastPlay
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun SearchYoutubeEntity (
    navController: NavController,
    onDismiss: () -> Unit,
    query: String,
    filter: Environment.SearchFilter = Environment.SearchFilter.Video,
    disableScrollingText: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    val selectedQueue = LocalSelectedQueue.current
    val thumbnailHeightDp = 72.dp
    val thumbnailWidthDp = 128.dp
    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val emptyItemsText = stringResource(R.string.no_results_found)
    val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
//        Title(
//            title = stringResource(id = R.string.videos),
//            modifier = Modifier.padding(bottom = 12.dp)
//        )
    }

    var filterContentType by remember { mutableStateOf(ContentType.Official) }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .systemBarsPadding(),

        ) {
            Title(
                title = stringResource(id = if (filter == Environment.SearchFilter.Video) R.string.videos
                else R.string.songs),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                modifier = Modifier.background(colorPalette().accent.copy(alpha = 0.15f))
            ) {
                Title2Actions(
                    title = "Filter content type",
                    onClick1 = {
                        menuState.display {
                            Menu {
                                ContentType.entries.forEach {
                                    MenuEntry(
                                        icon = it.icon,
                                        text = it.textName,
                                        onClick = {
                                            filterContentType = it
                                            menuState.hide()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                BasicText(
                    text = when (filterContentType) {
                        ContentType.All -> "All"
                        ContentType.Official -> "Official"
                        ContentType.UserGenerated -> "User Generated"

                    },
                    style = typography().xxs.secondary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
            }

            ItemsPage(
                tag = "searchYTEntity/$query/entities",
                itemsPageProvider = { continuation ->
                    if (continuation == null) {
                        Environment.searchPage(
                            body = SearchBody(
                                query = query,
                                params = filter.value
                            ),
                            fromMusicShelfRendererContent = if (filter == Environment.SearchFilter.Video) Environment.VideoItem::from
                            else Environment.SongItem::from
                        )
                    } else {
                        Environment.searchPage(
                            body = ContinuationBody(continuation = continuation),
                            fromMusicShelfRendererContent = if (filter == Environment.SearchFilter.Video) Environment.VideoItem::from
                            else Environment.SongItem::from
                        )
                    }
                },
                emptyItemsText = emptyItemsText,
                headerContent = headerContent,
                itemContent = { media ->
                    if (media is Environment.VideoItem || media is Environment.SongItem) {
                        SwipeablePlaylistItem(
                            mediaItem = when (media) {
                                is Environment.VideoItem -> media.asMediaItem
                                is Environment.SongItem -> media.asMediaItem
                                else -> throw IllegalArgumentException("Unknown media type")
                            },
                            onPlayNext = {
                                binder?.player?.addNext(
                                    when (media) {
                                        is Environment.VideoItem -> media.asMediaItem
                                        is Environment.SongItem -> media.asMediaItem
                                        else -> throw IllegalArgumentException("Unknown media type")
                                    },
                                    queue = selectedQueue ?: defaultQueue()
                                )
                            },
                            onEnqueue = {
                                binder?.player?.enqueue(when (media) {
                                    is Environment.VideoItem -> media.asMediaItem
                                    is Environment.SongItem -> media.asMediaItem
                                    else -> throw IllegalArgumentException("Unknown media type")
                                }, queue = it)
                            }
                        ) {
                            if (media is Environment.VideoItem) {
                                VideoItem(
                                    video = media,
                                    thumbnailWidthDp = thumbnailWidthDp,
                                    thumbnailHeightDp = thumbnailHeightDp,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        navController = rememberNavController(),
                                                        mediaItem = media.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        disableScrollingText = disableScrollingText
                                                    )
                                                };
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                            },
                                            onClick = {
                                                binder?.stopRadio()
        //                                        if (isVideoEnabled)
        //                                            binder?.player?.playOnline(video.asMediaItem)
        //                                        else
        //                                            binder?.player?.forcePlay(video.asMediaItem)
        //binder?.setupRadio(video.info?.endpoint)
                                                fastPlay(media.asMediaItem, binder, withReplace = true)
                                                onDismiss()
                                            }
                                        ),
                                    disableScrollingText = disableScrollingText
                                )
                            }
                            if (media is Environment.SongItem) {
                                SongItem(
                                    song = media,
                                    thumbnailSizePx = songThumbnailSizePx,
                                    thumbnailSizeDp = songThumbnailSizeDp,
                                    disableScrollingText = disableScrollingText,
                                    isNowPlaying = false,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        navController = rememberNavController(),
                                                        mediaItem = media.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        disableScrollingText = disableScrollingText
                                                    )
                                                };
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                            },
                                            onClick = {
                                                binder?.stopRadio()
    //                                        if (isVideoEnabled)
    //                                            binder?.player?.playOnline(video.asMediaItem)
    //                                        else
    //                                            binder?.player?.forcePlay(video.asMediaItem)
    //binder?.setupRadio(video.info?.endpoint)
                                                fastPlay(media.asMediaItem, binder, withReplace = true)
                                                onDismiss()
                                            }
                                        )
                                )
                            }
                        }
                    }
                },
                itemPlaceholderContent = {
                    VideoItemPlaceholder(
                        thumbnailHeightDp = thumbnailHeightDp,
                        thumbnailWidthDp = thumbnailWidthDp
                    )
                },
                filterContentType = filterContentType
            )
        }
    }
}