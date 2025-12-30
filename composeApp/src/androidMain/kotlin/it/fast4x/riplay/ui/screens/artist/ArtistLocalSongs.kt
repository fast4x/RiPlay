package it.fast4x.riplay.ui.screens.artist

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.ShimmerHost
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.SongItemPlaceholder
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.LazyListContainer

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun ArtistLocalSongs(
    navController: NavController,
    browseId: String,
    artistName: String,
    onDismiss: () -> Unit
//    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
//    thumbnailContent: @Composable () -> Unit,
//    onSearchClick: () -> Unit,
//    onSettingsClick: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    var songs by persist<List<Song>?>("artist/$browseId/localSongs")

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    LaunchedEffect(Unit) {
        Database.artistSongs(browseId).collect { songs = it }
/*
        val items = songs?.map { it.id }
        downloader.downloads.collect { downloads ->
            if (items != null) {
                downloadState =
                    if (items.all { downloads[it]?.state == Download.STATE_COMPLETED })
                        Download.STATE_COMPLETED
                    else if (items.all {
                            downloads[it]?.state == Download.STATE_QUEUED
                                    || downloads[it]?.state == Download.STATE_DOWNLOADING
                                    || downloads[it]?.state == Download.STATE_COMPLETED
                        })
                        Download.STATE_DOWNLOADING
                    else
                        Download.STATE_STOPPED
            }
        }

 */

    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    var showConfirmDeleteDownloadDialog by remember {
        mutableStateOf(false)
    }

    var showConfirmDownloadAllDialog by remember {
        mutableStateOf(false)
    }

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
    //LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                //.fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth(
                    if( NavigationBarPosition.Right.isCurrent() )
                        Dimensions.contentWidthRightBar
                    else
                        1f
                )
        ) {
            LazyListContainer(
                state = lazyListState,
            ) {
                LazyColumn(
                    state = lazyListState,
                    //contentPadding = LocalPlayerAwareWindowInsets.current
                    //.only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                    modifier = Modifier
                        .background(colorPalette().background0)
                        .fillMaxSize()
                ) {
//                    item(
//                        key = "header",
//                        contentType = 0
//                    ) {
//                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                            headerContent {
//
//                                HeaderIconButton(
//                                    icon = R.drawable.enqueue,
//                                    enabled = !songs.isNullOrEmpty(),
//                                    color = if (!songs.isNullOrEmpty()) colorPalette().text else colorPalette().textDisabled,
//                                    onClick = { },
//                                    modifier = Modifier
//                                        .combinedClickable(
//                                            onClick = {
//                                                binder?.player?.enqueue(
//                                                    songs!!.map(Song::asMediaItem),
//                                                    context
//                                                )
//                                            },
//                                            onLongClick = {
//                                                SmartMessage(
//                                                    context.resources.getString(R.string.info_enqueue_songs),
//                                                    context = context
//                                                )
//                                            }
//                                        )
//                                )
//                                HeaderIconButton(
//                                    icon = R.drawable.shuffle,
//                                    enabled = !songs.isNullOrEmpty(),
//                                    color = if (!songs.isNullOrEmpty()) colorPalette().text else colorPalette().textDisabled,
//                                    onClick = {},
//                                    modifier = Modifier
//                                        .combinedClickable(
//                                            onClick = {
//                                                songs?.let { songs ->
//                                                    if (songs.isNotEmpty()) {
//                                                        binder?.stopRadio()
//                                                        binder?.player?.forcePlayFromBeginning(
//                                                            songs.shuffled().map(Song::asMediaItem)
//                                                        )
//                                                    }
//                                                }
//                                            },
//                                            onLongClick = {
//                                                SmartMessage(
//                                                    context.resources.getString(R.string.info_shuffle),
//                                                    context = context
//                                                )
//                                            }
//                                        )
//                                )
//                            }
//
//                            thumbnailContent()
//                        }
//                    }

                    item {
                        Title(
                            title = artistName,
                            modifier = sectionTextModifier,
                            icon = R.drawable.chevron_down,
                            onClick = onDismiss
                        )
                        TitleSection(
                            title = stringResource(R.string.library),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }

                    songs?.let { songs ->
                        itemsIndexed(
                            items = songs,
                            key = { _, song -> song.id }
                        ) { index, song ->

                            SongItem(
                                song = song,
                                thumbnailSizeDp = songThumbnailSizeDp,
                                thumbnailSizePx = songThumbnailSizePx,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    navController = navController,
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    disableScrollingText = disableScrollingText,
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(
                                                songs.map(Song::asMediaItem),
                                                index
                                            )
                                        }
                                    ),
                                //disableScrollingText = disableScrollingText,
                                //isNowPlaying = binder?.player?.isNowPlaying(song.id) ?: false
                            )
                        }
                    } ?: item(key = "loading") {
                        ShimmerHost {
                            repeat(4) {
                                SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song)
                            }
                        }
                    }
                }
            }


        }
    //}
}
