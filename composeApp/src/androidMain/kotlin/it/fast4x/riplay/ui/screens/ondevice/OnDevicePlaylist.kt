package it.fast4x.riplay.ui.screens.ondevice


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.relatedSongs
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.LocalOnDeviceViewModel
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.Database.Companion.songAlbumInfo
import it.fast4x.riplay.data.Database.Companion.songArtistInfo
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.RecommendationsNumber
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SwipeableQueueItem
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.IconInfo
import it.fast4x.riplay.ui.components.themed.InPlaylistMediaItemMenu
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.Playlist
import it.fast4x.riplay.ui.components.themed.PlaylistsItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.SortMenu
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.onOverlay
import it.fast4x.riplay.ui.styling.overlay
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.extensions.preferences.UiTypeKey
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.formatAsTime
import it.fast4x.riplay.extensions.preferences.isRecommendationEnabledKey
import it.fast4x.riplay.extensions.preferences.maxSongsInQueueKey
import it.fast4x.riplay.extensions.preferences.navigationBarPositionKey
import it.fast4x.riplay.extensions.preferences.playlistSongSortByKey
import it.fast4x.riplay.extensions.preferences.recommendationsNumberKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.reorderInQueueEnabledKey
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import it.fast4x.riplay.extensions.preferences.songSortOrderKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.commonutils.PIPED_PREFIX
import it.fast4x.riplay.commonutils.YTP_PREFIX
import it.fast4x.riplay.commonutils.thumbnail
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.PlaylistSongsTypeFilter
import it.fast4x.riplay.extensions.fastshare.FastShare
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.screens.settings.isSyncEnabled
import it.fast4x.riplay.utils.checkFileExists
import it.fast4x.riplay.utils.deleteFileIfExists
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.saveImageToInternalStorage
import kotlinx.coroutines.CoroutineScope
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.enums.OnDeviceSongSortBy
import it.fast4x.riplay.extensions.ondevice.OnDeviceViewModel
import it.fast4x.riplay.service.LOCAL_KEY_PREFIX
import it.fast4x.riplay.ui.components.PullToRefreshBox
import it.fast4x.riplay.ui.components.themed.FilterMenu
import it.fast4x.riplay.ui.components.themed.InProgressDialog
import it.fast4x.riplay.utils.addToYtLikedSongs
import it.fast4x.riplay.utils.addToYtPlaylist
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.formatAsDuration
import it.fast4x.riplay.utils.getAlbumVersionFromVideo
import it.fast4x.riplay.utils.isExplicit
import org.dailyislam.android.utilities.isNetworkConnected
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.move
import it.fast4x.riplay.extensions.preferences.playlistSongsTypeFilterKey
import it.fast4x.riplay.ui.components.themed.FastPlayActionsBar
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.removeYTSongFromPlaylist
import it.fast4x.riplay.utils.unlikeYtVideoOrSong
import it.fast4x.riplay.utils.updateLocalPlaylist
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.riplay.extensions.preferences.onDeviceSongSortByKey
import it.fast4x.riplay.ui.items.PlaylistItem
import timber.log.Timber

@KotlinCsvExperimental
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun OnDevicePlaylist(
    navController: NavController,
    folder: String,
) {
    val context = LocalContext.current
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
    val selectedQueue = LocalSelectedQueue.current
    val uiType by rememberPreference(UiTypeKey, UiType.RiPlay)

    val folder by remember(folder) { mutableStateOf(folder.replace("$","/")) }

    var playlistAllSongs by persistList<SongEntity>("localPlaylist/$folder/songs")
    var songsInTheToPlaylist by persistList<SongEntity>("")
    var playlistSongs by persistList<SongEntity>("localPlaylist/$folder/songs")
    var playlistPreview by persist<PlaylistPreview?>("localPlaylist/playlist")
    val thumbnailUrl = remember { mutableStateOf("") }


    var sortBy by rememberPreference(onDeviceSongSortByKey, OnDeviceSongSortBy.DateAdded)
    var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)

    var filter: String? by rememberSaveable { mutableStateOf(null) }

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val onDeviceViewModel = LocalOnDeviceViewModel.current
    LaunchedEffect(Unit, filter, sortOrder, sortBy) {
        onDeviceViewModel.sortBy = sortBy
        onDeviceViewModel.sortOrder = sortOrder
        onDeviceViewModel.loadAudioFiles()
        onDeviceViewModel.audioFilesFromFolder(folder).collect {
            playlistAllSongs = it
            playlistSongs = it
            playlistPreview = PlaylistPreview(
                playlist = Playlist(name = folder, isEditable = false, ),
                songCount = it.size,
                isOnDevice = true,
                folder = folder
            )
        }

        Timber.d("OnDevicePlaylist playlistAllSongs ${playlistAllSongs.size}")
    }

    var filterCharSequence: CharSequence
    filterCharSequence = filter.toString()

    if (!filter.isNullOrBlank())
        playlistSongs =
            playlistSongs.filter { songItem ->
                songItem.song.title.contains(
                    filterCharSequence,
                    true
                ) ?: false
                        || songItem.song.artistsText?.contains(
                    filterCharSequence,
                    true
                ) ?: false
                        || songItem.albumTitle?.contains(
                    filterCharSequence,
                    true
                ) ?: false
            }

    var searching by rememberSaveable { mutableStateOf(false) }

    var totalPlayTimes = 0L
    playlistSongs.forEach {
        totalPlayTimes += it.song.durationText?.let { it1 ->
            durationTextToMillis(it1)
        }?.toLong() ?: 0
    }


    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
    )

    val lazyListState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()

    fun sync() {

        playlistPreview?.let { playlistPreview ->

        }
    }


    val playlistThumbnailSizeDp = Dimensions.thumbnails.playlist
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val rippleIndication = ripple(bounded = false)

    val uriHandler = LocalUriHandler.current

    var scrollToNowPlaying by remember {
        mutableStateOf(false)
    }

    var nowPlayingItem by remember {
        mutableStateOf(-1)
    }

    var listMediaItems = remember {
        mutableListOf<MediaItem>()
    }

    var selectItems by remember {
        mutableStateOf(false)
    }

    var plistId by remember {
        mutableStateOf(0L)
    }
    var plistName by remember {
        mutableStateOf(playlistPreview?.playlist?.name)
    }


    var position by remember {
        mutableIntStateOf(0)
    }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            coroutineScope.launch (Dispatchers.IO){
                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        csvWriter().open(outputStream) {
                            writeRow(
                                "PlaylistBrowseId",
                                "PlaylistName",
                                "MediaId",
                                "Title",
                                "Artists",
                                "Duration",
                                "ThumbnailUrl",
                                "AlbumId",
                                "AlbumTitle",
                                "ArtistIds"
                            )
                            if (listMediaItems.isEmpty()) {
                                playlistSongs.forEach {
                                    val artistInfos = songArtistInfo(it.asMediaItem.mediaId)
                                    val albumInfo = songAlbumInfo(it.asMediaItem.mediaId)
                                    writeRow(
                                        playlistPreview?.playlist?.browseId,
                                        plistName,
                                        it.song.id,
                                        it.song.title,
                                        artistInfos.joinToString(",") { it.name ?: "" },
                                        it.song.durationText,
                                        it.song.thumbnailUrl,
                                        albumInfo?.id,
                                        albumInfo?.name,
                                        artistInfos.joinToString(",") { it.id }
                                    )
                                }
                            } else {
                                listMediaItems.forEach {
                                    val artistInfos = songArtistInfo(it.mediaId)
                                    val albumInfo = songAlbumInfo(it.mediaId)
                                    writeRow(
                                        playlistPreview?.playlist?.browseId,
                                        plistName,
                                        it.mediaId,
                                        it.mediaMetadata.title,
                                        artistInfos.joinToString(",") { it.name ?: "" },
                                        it.asSong.durationText,
                                        it.mediaMetadata.artworkUri,
                                        albumInfo?.id,
                                        albumInfo?.name,
                                        artistInfos.joinToString(",") { it.id }
                                    )
                                }
                            }
                        }
                    }
                }
        }

    var isExporting by rememberSaveable {
        mutableStateOf(false)
    }

    if (isExporting) {
        InputTextDialog(
            onDismiss = {
                isExporting = false
            },
            title = stringResource(R.string.enter_the_playlist_name),
            value = playlistPreview?.playlist?.name?.let { cleanPrefix(it) } ?: "",
            placeholder = stringResource(R.string.enter_the_playlist_name),
            setValue = { text ->
                if (isExporting) {
                    plistName = text
                    try {
                        @SuppressLint("SimpleDateFormat")
                        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                        exportLauncher.launch("RMPlaylist_${text.take(20)}_${dateFormat.format(Date())}")
                    } catch (e: ActivityNotFoundException) {
                        SmartMessage(
                            context.resources.getString(R.string.info_not_find_app_create_doc),
                            type = PopupType.Warning, context = context
                        )
                    }
                }

            }
        )
    }

    val navigationBarPosition by rememberPreference(
        navigationBarPositionKey,
        NavigationBarPosition.Bottom
    )
    val maxSongsInQueue by rememberPreference(maxSongsInQueueKey, MaxSongs.`500`)

    val thumbnails = playlistSongs.map { it.song }
        .takeWhile { it.thumbnailUrl?.isNotEmpty() ?: false }
        .take(4)
        .map { it.thumbnailUrl.thumbnail(thumbnailSizePx / 2) }

    val hapticFeedback = LocalHapticFeedback.current

    var refreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    fun refresh() {
        if (refreshing) return
        refreshScope.launch(Dispatchers.IO) {
            refreshing = true
            sync()
            delay(500)
            refreshing = false
        }
    }

    var showFastShare by remember { mutableStateOf(false) }

    FastShare(
        showFastShare,
        onDismissRequest = { showFastShare = false},
        content = playlistPreview?.playlist ?: return
    )

    PullToRefreshBox(
        refreshing = refreshing,
        onRefresh = { refresh() }
    ) {
        Box(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxHeight()
                .fillMaxWidth(
                    if (navigationBarPosition == NavigationBarPosition.Left ||
                        navigationBarPosition == NavigationBarPosition.Top ||
                        navigationBarPosition == NavigationBarPosition.Bottom
                    ) 1f
                    else Dimensions.contentWidthRightBar
                ),

        ) {
            LazyListContainer(
                state = lazyListState
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .background(colorPalette.background0)
                        .fillMaxSize()
                ) {
                    item(
                        key = "header",
                        contentType = 0
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {

                            HeaderWithIcon(
                                title = playlistPreview?.playlist?.name?.let { name ->
                                    cleanPrefix(name)
                                } ?: "Unknown",
                                iconId = R.drawable.playlist,
                                enabled = true,
                                showIcon = false,
                                modifier = Modifier
                                    .padding(bottom = 8.dp),
                                onClick = {}
                            )

                        }

                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                //.background(colorPalette.background4)
                                //.fillMaxSize(0.99F)
                                .fillMaxWidth()
                                .background(
                                    color = colorPalette.background1,
                                    shape = thumbnailRoundness.shape()
                                )
                        ) {

                            playlistPreview?.let {
                                PlaylistItem(
                                    thumbnailContent = {
                                        if (thumbnails.toSet().size == 1) {
                                            AsyncImage(
                                                model = thumbnails.first().thumbnail(thumbnailSizePx),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                            ) {
                                                listOf(
                                                    Alignment.TopStart,
                                                    Alignment.TopEnd,
                                                    Alignment.BottomStart,
                                                    Alignment.BottomEnd
                                                ).forEachIndexed { index, alignment ->
                                                    AsyncImage(
                                                        model = thumbnails.getOrNull(index),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .align(alignment)
                                                            .size(playlistThumbnailSizeDp / 2)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    songCount = playlistSongs.size,
                                    thumbnailSizeDp = playlistThumbnailSizeDp,
                                    name = it.playlist.name,
                                    channelName = null,
                                    alternative = true,
                                    showName = false,
                                    modifier = Modifier
                                        .padding(top = 14.dp),
                                    disableScrollingText = disableScrollingText,

                                )
                            }


                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    //.fillMaxHeight()
                                    .padding(end = 10.dp)
                                    //.fillMaxWidth(if (isLandscape) 0.90f else 0.80f)
                                    .fillMaxWidth()
                            ) {
                                Spacer(modifier = Modifier.height(10.dp))
                                IconInfo(
                                    title = playlistSongs.size.toString(),
                                    icon = painterResource(R.drawable.musical_notes)
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                IconInfo(
                                    title = formatAsTime(totalPlayTimes),
                                    icon = painterResource(R.drawable.time)
                                )
                                Spacer(modifier = Modifier.height(30.dp))

                                FastPlayActionsBar(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    onPlayNowClick = {
                                        if (playlistSongs.any { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }) {
                                            playlistSongs.filter { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }
                                                .let { songs ->
                                                    if (songs.isNotEmpty()) {
                                                        val itemsLimited =
                                                            if (songs.size > maxSongsInQueue.number) songs
                                                                .take(maxSongsInQueue.number.toInt()) else songs
                                                        binder?.stopRadio()
                                                        binder?.player?.forcePlayFromBeginning(
                                                            itemsLimited
                                                                .map(SongEntity::asMediaItem)
                                                        )
                                                        //fastPlay(binder = binder, mediaItems = itemsLimited.map(SongEntity::asMediaItem), withShuffle = true)
                                                    }
                                                }
                                        } else {
                                            SmartMessage(
                                                context.resources.getString(R.string.disliked_this_collection),
                                                type = PopupType.Error,
                                                context = context
                                            )
                                        }
                                        //fastPlay(binder = binder, mediaItems = playlistSongs.map(SongEntity::asMediaItem))
                                    },
                                    onShufflePlayClick = {
                                        if (playlistSongs.any { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }) {
                                            playlistSongs.filter { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }
                                                .let { songs ->
                                                    if (songs.isNotEmpty()) {
                                                        val itemsLimited =
                                                            if (songs.size > maxSongsInQueue.number) songs.shuffled()
                                                                .take(maxSongsInQueue.number.toInt()) else songs
                                                        binder?.stopRadio()
                                                        binder?.player?.forcePlayFromBeginning(
                                                            itemsLimited.shuffled()
                                                                .map(SongEntity::asMediaItem)
                                                        )
                                                        //fastPlay(binder = binder, mediaItems = itemsLimited.map(SongEntity::asMediaItem), withShuffle = true)
                                                    }
                                                }
                                        } else {
                                            SmartMessage(
                                                context.resources.getString(R.string.disliked_this_collection),
                                                type = PopupType.Error,
                                                context = context
                                            )
                                        }
                                    },
                                )

                            }

                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .fillMaxWidth()
                        ) {
                            HeaderIconButton(
                                onClick = {},
                                icon = R.drawable.search,
                                color = colorPalette.text,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            searching = !searching
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.search),
                                                context = context
                                            )
                                        }
                                    )
                            )

                            HeaderIconButton(
                                icon = R.drawable.ellipsis_horizontal,
                                color = colorPalette.text, //if (playlistWithSongs?.songs?.isNotEmpty() == true) colorPalette.text else colorPalette.textDisabled,
                                enabled = true, //playlistWithSongs?.songs?.isNotEmpty() == true,
                                modifier = Modifier
                                    .padding(end = 4.dp),
                                onClick = {
                                    menuState.display {
                                        playlistPreview?.let { playlistPreview ->
                                            PlaylistsItemMenu(
                                                navController = navController,
                                                onDismiss = menuState::hide,
                                                onSelectUnselect = {
                                                    selectItems = !selectItems
                                                    if (!selectItems) {
                                                        listMediaItems.clear()
                                                    }
                                                },
                                                playlist = playlistPreview,
                                                onEnqueue = {
                                                    if (listMediaItems.isEmpty()) {
                                                        if (playlistSongs.any { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }) {
                                                            binder?.player?.enqueue(playlistSongs.filter { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }
                                                                .map(SongEntity::asMediaItem),
                                                                context)
                                                        } else {
                                                            SmartMessage(
                                                                context.resources.getString(R.string.disliked_this_collection),
                                                                type = PopupType.Error,
                                                                context = context
                                                            )
                                                        }
                                                    } else {
                                                        binder?.player?.enqueue(
                                                            listMediaItems,
                                                            context
                                                        )
                                                        listMediaItems.clear()
                                                        selectItems = false
                                                    }
                                                },
                                                onPlayNext = {
                                                    if (listMediaItems.isEmpty()) {
                                                        if (playlistSongs.any { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }) {
                                                            binder?.player?.addNext(playlistSongs.filter { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }
                                                                .map(SongEntity::asMediaItem),
                                                                context,
                                                                selectedQueue ?: defaultQueue()
                                                            )
                                                        } else {
                                                            SmartMessage(
                                                                context.resources.getString(R.string.disliked_this_collection),
                                                                type = PopupType.Error,
                                                                context = context
                                                            )
                                                        }
                                                    } else {
                                                        binder?.player?.addNext(
                                                            listMediaItems,
                                                            context,
                                                            selectedQueue ?: defaultQueue()
                                                        )
                                                        listMediaItems.clear()
                                                        selectItems = false
                                                    }
                                                },
                                                showOnSyncronize = false,
                                                showLinkUnlink = false,
                                                onAddToPlaylist = { toPlaylistPreview ->
                                                    position = toPlaylistPreview.songCount.minus(1)
                                                    //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                                    if (position > 0) position++ else position = 0
                                                    //Log.d("mediaItem", "next initial pos ${position}")
                                                    if (listMediaItems.isEmpty()) {
                                                        val filteredPLSongs =
                                                            playlistSongs.filterNot {
                                                                it.asMediaItem.mediaId.startsWith(
                                                                    LOCAL_KEY_PREFIX
                                                                ) || it.song.thumbnailUrl == ""
                                                            }
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            songsInTheToPlaylist =
                                                                withContext(Dispatchers.IO) {
                                                                    Database.sortSongsPlaylistByPositionNoFlow(
                                                                        toPlaylistPreview.playlist.id
                                                                    )
                                                                }
                                                            var distinctSongs =
                                                                filteredPLSongs.filterNot { it in songsInTheToPlaylist }

                                                            if ((distinctSongs.size + toPlaylistPreview.songCount) > 5000 && toPlaylistPreview.playlist.isYoutubePlaylist && isSyncEnabled()) {
                                                                SmartMessage(
                                                                    context.resources.getString(
                                                                        R.string.yt_playlist_limited
                                                                    ),
                                                                    context = context,
                                                                    type = PopupType.Error
                                                                )
                                                            } else if (!isSyncEnabled() || !toPlaylistPreview.playlist.isYoutubePlaylist) {
                                                                playlistSongs.forEachIndexed { index, song ->
                                                                    Database.asyncTransaction {
                                                                        Database.insert(song.asMediaItem)
                                                                        Database.insert(
                                                                            SongPlaylistMap(
                                                                                songId = song.asMediaItem.mediaId,
                                                                                playlistId = toPlaylistPreview.playlist.id,
                                                                                position = position + index
                                                                            ).default()
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                    if (playlistPreview.playlist.isYoutubePlaylist) {
                                                                        EnvironmentExt.addPlaylistToPlaylist(
                                                                            cleanPrefix(
                                                                                toPlaylistPreview.playlist.browseId
                                                                                    ?: ""
                                                                            ),
                                                                            cleanPrefix(
                                                                                playlistPreview.playlist.browseId
                                                                                    ?: ""
                                                                            )
                                                                        ).onSuccess {
                                                                            playlistSongs.forEachIndexed { index, song ->
                                                                                Database.asyncTransaction {
                                                                                    Database.insert(
                                                                                        song.asMediaItem
                                                                                    )
                                                                                    Database.insert(
                                                                                        SongPlaylistMap(
                                                                                            songId = song.asMediaItem.mediaId,
                                                                                            playlistId = toPlaylistPreview.playlist.id,
                                                                                            position = position + index
                                                                                        ).default()
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    } else if (distinctSongs.isNotEmpty()) {
                                                                        addToYtPlaylist(
                                                                            toPlaylistPreview.playlist.id,
                                                                            position,
                                                                            toPlaylistPreview.playlist.browseId
                                                                                ?: "",
                                                                            distinctSongs.map { it.asMediaItem })
                                                                    }
                                                                }
                                                            }

                                                        }
                                                    } else {
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            val filteredListMediaItems =
                                                                listMediaItems.filterNot {
                                                                    it.mediaId.startsWith(
                                                                        LOCAL_KEY_PREFIX
                                                                    ) || it.mediaMetadata.artworkUri.toString() == ""
                                                                }
                                                            songsInTheToPlaylist =
                                                                withContext(Dispatchers.IO) {
                                                                    Database.sortSongsPlaylistByPositionNoFlow(
                                                                        toPlaylistPreview.playlist.id
                                                                    )
                                                                }

                                                            val distinctSongs =
                                                                filteredListMediaItems.filter { item -> item !in songsInTheToPlaylist.map { it.asMediaItem } }
                                                            if ((distinctSongs.size + toPlaylistPreview.songCount) > 5000 && toPlaylistPreview.playlist.isYoutubePlaylist && isSyncEnabled()) {
                                                                SmartMessage(
                                                                    context.resources.getString(
                                                                        R.string.yt_playlist_limited
                                                                    ),
                                                                    context = context,
                                                                    type = PopupType.Error
                                                                )
                                                            } else if (!isSyncEnabled() || !toPlaylistPreview.playlist.isYoutubePlaylist) {
                                                                listMediaItems.forEachIndexed { index, song ->
                                                                    Database.asyncTransaction {
                                                                        Database.insert(song)
                                                                        Database.insert(
                                                                            SongPlaylistMap(
                                                                                songId = song.mediaId,
                                                                                playlistId = toPlaylistPreview.playlist.id,
                                                                                position = position + index
                                                                            ).default()
                                                                        )
                                                                    }
                                                                }
                                                            } else if (distinctSongs.isNotEmpty()) {
                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                    addToYtPlaylist(
                                                                        toPlaylistPreview.playlist.id,
                                                                        position,
                                                                        toPlaylistPreview.playlist.browseId
                                                                            ?: "",
                                                                        distinctSongs
                                                                    )
                                                                }
                                                            }
                                                            println("pipedInfo mediaitemmenu uuid ${playlistPreview.playlist.browseId}")


                                                            listMediaItems.clear()
                                                            selectItems = false
                                                        }
                                                    }
                                                },
                                                onAddToPreferites = {
                                                        if (listMediaItems.isEmpty()) {
                                                            playlistSongs.forEachIndexed { index, song ->
                                                                if (song.song.likedAt in listOf(
                                                                        -1L,
                                                                        null
                                                                    )
                                                                ) {
                                                                    mediaItemToggleLike(song.asMediaItem)
                                                                }
                                                            }
                                                        } else {
                                                            Database.asyncTransaction {
                                                                listMediaItems.forEachIndexed { index, song ->
                                                                    if (Database.getLikedAt(song.mediaId) !in listOf(
                                                                            -1L,
                                                                            null
                                                                        )
                                                                    ) {
                                                                        mediaItemToggleLike(song)
                                                                    }
                                                                }
                                                            }
                                                        }

                                                },
                                                showonListenToYT = false,
                                                onExport = {
                                                    isExporting = true
                                                },
                                                disableScrollingText = disableScrollingText,
                                            )
                                        }

                                    }
                                }
                            )

                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        /*        */
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .fillMaxWidth()
                        ) {

                            HeaderIconButton(
                                icon = R.drawable.arrow_up,
                                color = colorPalette.text,
                                onClick = {
                                    sortOrder = !sortOrder
                                },
                                modifier = Modifier
                                    .graphicsLayer { rotationZ = sortOrderIconRotation }
                            )

                            BasicText(
                                text = when (sortBy) {
                                    OnDeviceSongSortBy.DateAdded -> stringResource(R.string.sort_date_added)
                                    OnDeviceSongSortBy.Title -> stringResource(R.string.sort_title)
                                    OnDeviceSongSortBy.Album -> stringResource(R.string.sort_album)
                                    OnDeviceSongSortBy.Artist -> stringResource(R.string.sort_artist)
                                    OnDeviceSongSortBy.Duration -> stringResource(R.string.sort_duration)
                                },
                                style = typography.xs.semiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clickable {
                                        menuState.display {
                                            SortMenu(
                                                title = stringResource(R.string.sorting_order),
                                                onDismiss = menuState::hide,
                                                onTitle = {
                                                    sortBy =
                                                        OnDeviceSongSortBy.Title
                                                },
                                                onDateAdded = {
                                                    sortBy =
                                                        OnDeviceSongSortBy.DateAdded
                                                },
                                                onArtist = {
                                                    sortBy =
                                                        OnDeviceSongSortBy.Artist
                                                },
                                                onAlbum = {
                                                    sortBy =
                                                        OnDeviceSongSortBy.Album
                                                },
                                            )
                                        }

                                    }
                            )

                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                HeaderIconButton(
                                    modifier = Modifier
                                        .padding(horizontal = 5.dp)
                                        .combinedClickable(
                                            onClick = {
                                                nowPlayingItem = -1
                                                scrollToNowPlaying = false
                                                playlistSongs
                                                    .forEachIndexed { index, song ->
                                                        if (song.asMediaItem.mediaId == binder?.player?.currentMediaItem?.mediaId)
                                                            nowPlayingItem = index
                                                    }

                                                if (nowPlayingItem > -1)
                                                    scrollToNowPlaying = true
                                            },
                                            onLongClick = {
                                                SmartMessage(
                                                    context.resources.getString(R.string.info_find_the_song_that_is_playing),
                                                    context = context
                                                )
                                            }
                                        ),
                                    icon = R.drawable.locate,
                                    enabled = playlistSongs.isNotEmpty(),
                                    color = if (playlistSongs.isNotEmpty()) colorPalette.text else colorPalette.textDisabled,
                                    onClick = {}
                                )
                                LaunchedEffect(scrollToNowPlaying) {
                                    if (scrollToNowPlaying)
                                        lazyListState.scrollToItem(nowPlayingItem, 1)
                                    scrollToNowPlaying = false
                                }

                            }

                        }


                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .padding(all = 10.dp)
                                .fillMaxWidth()
                        ) {
                            AnimatedVisibility(visible = searching) {
                                val focusRequester = remember { FocusRequester() }
                                val focusManager = LocalFocusManager.current
                                val keyboardController = LocalSoftwareKeyboardController.current

                                LaunchedEffect(searching) {
                                    focusRequester.requestFocus()
                                }

                                BasicTextField(
                                    value = filter ?: "",
                                    onValueChange = { filter = it },
                                    textStyle = typography.xs.semiBold,
                                    singleLine = true,
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (filter.isNullOrBlank()) filter = ""
                                        focusManager.clearFocus()
                                    }),
                                    cursorBrush = SolidColor(colorPalette.text),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 10.dp)
                                        ) {
                                            IconButton(
                                                onClick = {},
                                                icon = R.drawable.search,
                                                color = colorPalette.favoritesIcon,
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .size(16.dp)
                                            )
                                        }
                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 30.dp)
                                        ) {
                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier
                                        .height(30.dp)
                                        .fillMaxWidth()
                                        .background(
                                            colorPalette.background4,
                                            shape = thumbnailRoundness.shape()
                                        )
                                        .focusRequester(focusRequester)
                                        .onFocusChanged {
                                            if (!it.hasFocus) {
                                                keyboardController?.hide()
                                                if (filter?.isBlank() == true) {
                                                    filter = null
                                                    searching = false
                                                }
                                            }
                                        }
                                )
                            }
                        }


                    }

                    itemsIndexed(
                        items = playlistSongs.distinctBy { it.song.id },
                        key = { _, song -> song.song.id },
                        contentType = { _, song -> song },
                    ) { index, song ->

                            val interactionSource = remember { MutableInteractionSource() }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()

                            ) {
                                val checkedState = rememberSaveable { mutableStateOf(false) }

                                SwipeableQueueItem(
                                    mediaItem = song.asMediaItem,
                                    onPlayNext = {
                                        binder?.player?.addNext(
                                            song.asMediaItem,
                                            queue = selectedQueue ?: defaultQueue()
                                        )
                                    }
                                ) {
                                    //var forceRecompose by remember { mutableStateOf(false) }
                                    SongItem(
                                        song = song.song,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        trailingContent = {
                                            if (selectItems)
                                                Checkbox(
                                                    checked = checkedState.value,
                                                    onCheckedChange = {
                                                        checkedState.value = it
                                                        if (it) listMediaItems.add(song.asMediaItem) else
                                                            listMediaItems.remove(song.asMediaItem)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = colorPalette.accent,
                                                        uncheckedColor = colorPalette.text
                                                    ),
                                                    modifier = Modifier
                                                        .scale(0.7f)
                                                )
                                            else checkedState.value = false

                                        },
                                        onThumbnailContent = {
                                            if (nowPlayingItem > -1)
                                                NowPlayingSongIndicator(
                                                    song.asMediaItem.mediaId,
                                                    binder?.player
                                                )
                                        },
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        InPlaylistMediaItemMenu(
                                                            onInfo = {
                                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.song.id}")
                                                            },
                                                            navController = navController,
                                                            playlist = playlistPreview,
                                                            playlistId = 0,
                                                            positionInPlaylist = index,
                                                            song = song.song,
                                                            onDismiss = menuState::hide,
                                                            disableScrollingText = disableScrollingText,
                                                        )
                                                    }
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                },
                                                onClick = {
                                                    if (!selectItems) {
                                                        if (song.song.likedAt != -1L) {
                                                            searching = false
                                                            filter = null
                                                            playlistSongs.filter { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }
                                                                .map(SongEntity::asMediaItem)
                                                                .let { mediaItems ->
                                                                    binder?.stopRadio()
                                                                    binder?.player?.forcePlayAtIndex(
                                                                        mediaItems,
                                                                        mediaItems.indexOf(song.asMediaItem)
                                                                    )
                                                                }
                                                        } else {
                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                SmartMessage(
                                                                    context.resources.getString(R.string.disliked_this_song),
                                                                    type = PopupType.Error,
                                                                    context = context
                                                                )
                                                            }
                                                        }
                                                    } else checkedState.value = !checkedState.value
                                                }
                                            )

                                            .background(color = colorPalette.background0),
                                    )
                                }
                            }

                    }

                    item(
                        key = "footer",
                        contentType = 0,
                    ) {
                        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if (uiType == UiType.ViMusic || showFloatingIcon)
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = lazyListState,
                    iconId = R.drawable.shuffle,
                    onClick = {
                        if (playlistSongs.any { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }) {
                            playlistSongs.filter { it.song.thumbnailUrl != "" && it.song.likedAt != -1L }
                                .let { songs ->
                                    if (songs.isNotEmpty()) {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayFromBeginning(
                                            songs.shuffled().map(SongEntity::asMediaItem)
                                        )
                                    }
                                }
                        } else {
                            SmartMessage(
                                context.resources.getString(R.string.disliked_this_collection),
                                type = PopupType.Error,
                                context = context
                            )
                        }
                }
            )


        }

    }
}


