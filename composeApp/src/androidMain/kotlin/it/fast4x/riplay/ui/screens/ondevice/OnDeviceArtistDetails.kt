package it.fast4x.riplay.ui.screens.ondevice

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.compose.persist.persist
import it.fast4x.compose.persist.persistList
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.ArtistSection
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.models.Album
import it.fast4x.riplay.models.Artist
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.models.defaultQueue
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.screens.player.fastPlay
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.parentalControlEnabledKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.resize
import it.fast4x.riplay.utils.semiBold
import it.fast4x.riplay.utils.showFloatingIconKey
import it.fast4x.riplay.utils.thumbnailRoundnessKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun OnDeviceArtistDetails(
    navController: NavController,
    artistId: String?,
//    artistPage: ArtistPage?,
    onItemsPageClick: (ArtistSection) -> Unit,
    disableScrollingText: Boolean
) {

    if (artistId == null) return

    val binder = LocalPlayerServiceBinder.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val selectedQueue = LocalSelectedQueue.current

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val thumbnailRoundness by rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current

    var showConfirmDeleteDownloadDialog by remember {
        mutableStateOf(false)
    }

    var showConfirmDownloadAllDialog by remember {
        mutableStateOf(false)
    }

    var translateEnabled by remember {
        mutableStateOf(false)
    }

    val listMediaItems = remember { mutableListOf<MediaItem>() }

    val artist by remember {
        Database.artist(artistId)
    }.collectAsState(initial = null, context = Dispatchers.IO)
    println("OnDeviceArtistDetails artistId: $artistId artist: $artist")
    val topSongs by remember {
        Database.artistTopSongs(artistId)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)
    println("OnDeviceArtistDetails topSongs: $topSongs")
    val albums by remember {
        Database.artistOnDeviceAlbums(artist?.name.toString())
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)
    println("OnDeviceArtistDetails albums: $albums")

    val artistSongsMapped by remember {
        Database.artistSongMap(artistId)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)
    println("OnDeviceArtistDetails artistSongsMapped: $artistSongsMapped")


    var itemsBrowseId by remember { mutableStateOf("") }
    var itemsParams by remember { mutableStateOf("") }
    var itemsSectionName by remember { mutableStateOf("") }
    var showArtistItems by rememberSaveable { mutableStateOf(false) }
    var songsBrowseId by remember { mutableStateOf("") }
    var songsParams by remember { mutableStateOf("") }

    val hapticFeedback = LocalHapticFeedback.current
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val menuState = LocalMenuState.current

    var readMore by remember { mutableStateOf(false) }

    var scrollToNowPlaying by remember {
        mutableStateOf(false)
    }

    var nowPlayingItem by remember {
        mutableStateOf(-1)
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(scrollToNowPlaying) {
        if (scrollToNowPlaying)
            lazyListState.scrollToItem(nowPlayingItem, 1)
        scrollToNowPlaying = false
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        ) {

            item {
                val modifierArt = Modifier.fillMaxWidth()

                Box(
                    modifier = modifierArt
                ) {
                    //if (artistPage != null) {
                    if (!isLandscape)
                        Box {
                            AsyncImage(
                                model = artist?.thumbnailUrl?.resize(
                                    1200,
                                    1200
                                ),
                                contentDescription = "loading...",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .fadingEdge(
                                        top = WindowInsets.systemBars
                                            .asPaddingValues()
                                            .calculateTopPadding() + Dimensions.fadeSpacingTop,
                                        bottom = Dimensions.fadeSpacingBottom
                                    )
                            )
                            if (artist?.isYoutubeArtist == true) {
                                Image(
                                    painter = painterResource(R.drawable.ytmusic),
                                    colorFilter = ColorFilter.tint(
                                        Color.Red.copy(0.75f).compositeOver(Color.White)
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(all = 5.dp)
                                        .offset(10.dp,10.dp),
                                    contentDescription = "Background Image",
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                    AutoResizeText(
                        text = artist?.name ?: "",
                        style = typography().l.semiBold,
                        fontSizeRange = FontSizeRange(32.sp, 38.sp),
                        fontWeight = typography().l.semiBold.fontWeight,
                        fontFamily = typography().l.semiBold.fontFamily,
                        color = typography().l.semiBold.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 30.dp)
                            .applyIf(!disableScrollingText) {
                                basicMarquee(
                                    iterations = Int.MAX_VALUE
                                )
                            }

                    )

                }


                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {

                    HeaderIconButton(
                        icon = R.drawable.shuffle,
                        enabled = topSongs.any { it.likedAt != -1L },
                        color = if (topSongs.any { it.likedAt != -1L }) colorPalette().text else colorPalette().textDisabled,
                        onClick = {},
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .combinedClickable(
                                onClick = {
                                    if (topSongs.any { it.likedAt != -1L }) {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayFromBeginning(
                                            topSongs.filter { it.likedAt != -1L }
                                                .shuffled()
                                                .map(Song::asMediaItem)
                                        )
                                    } else {
                                        SmartMessage(
                                            context.resources.getString(R.string.disliked_this_collection),
                                            type = PopupType.Error,
                                            context = context
                                        )
                                    }
                                },
                                onLongClick = {
                                    SmartMessage(
                                        context.resources.getString(R.string.info_shuffle),
                                        context = context
                                    )
                                }
                            )
                    )

                    HeaderIconButton(
                        icon = R.drawable.radio,
                        enabled = true,
                        color = if (topSongs.any { it.likedAt != -1L }) colorPalette().text else colorPalette().textDisabled,
                        onClick = {},
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .combinedClickable(
                                onClick = {
                                    if (topSongs.any { it.likedAt != -1L }) {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayFromBeginning(topSongs.filter { it.likedAt != -1L }
                                            .map(Song::asMediaItem))
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(
                                                videoId = topSongs.first { it.likedAt != -1L }.id
                                            )
                                        )
                                    } else {
                                        SmartMessage(
                                            context.resources.getString(R.string.disliked_this_collection),
                                            type = PopupType.Error,
                                            context = context
                                        )
                                    }
                                },
                                onLongClick = {
                                    SmartMessage(
                                        context.resources.getString(R.string.info_start_radio),
                                        context = context
                                    )
                                }
                            )
                    )

                    HeaderIconButton(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .combinedClickable(
                                onClick = {
                                    nowPlayingItem = -1
                                    scrollToNowPlaying = false
                                    topSongs
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
                        enabled = topSongs.isNotEmpty(),
                        color = if (topSongs.isNotEmpty()) colorPalette().text else colorPalette().textDisabled,
                        onClick = {}


                    )

                }
            }

//            item {
//                artistPage.description?.let { description ->
//                    val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")
//
//                    Title(
//                        title = stringResource(R.string.information),
//                        icon = if (readMore) R.drawable.chevron_up else R.drawable.chevron_down,
//                        onClick = {
//                            readMore = !readMore
//                        }
//                    )
//
//                    Row(
//                        modifier = Modifier
//                            .padding(vertical = 16.dp, horizontal = 8.dp)
//                    ) {
//                        BasicText(
//                            text = "“",
//                            style = typography().xxl.semiBold,
//                            modifier = Modifier
//                                .offset(y = (-8).dp)
//                                .align(Alignment.Top)
//                        )
//
//                        if (!readMore)
//                            BasicText(
//                                text = description.substring(0,
//                                    if (description.length >= 100) 100 else description.length
//                                ).plus("..."),
//                                style = typography().xxs.secondary.align(TextAlign.Justify),
//                                modifier = Modifier
//                                    .padding(horizontal = 8.dp)
//                                    .weight(1f)
//                                    .clickable {
//                                        readMore = !readMore
//                                    }
//                            )
//
//                        if (readMore)
//                            BasicText(
//                                text = if (attributionsIndex == -1) {
//                                    description
//                                } else {
//                                    description.substring(0, attributionsIndex)
//                                },
//                                style = typography().xxs.secondary.align(TextAlign.Justify),
//                                modifier = Modifier
//                                    .padding(horizontal = 8.dp)
//                                    .weight(1f)
//                                    .clickable {
//                                        readMore = !readMore
//                                    }
//                            )
//
//
//                        BasicText(
//                            text = "„",
//                            style = typography().xxl.semiBold,
//                            modifier = Modifier
//                                .offset(y = 4.dp)
//                                .align(Alignment.Bottom)
//                        )
//                    }
//
//                    if (attributionsIndex != -1) {
//                        BasicText(
//                            text = stringResource(R.string.from_wikipedia_cca),
//                            style = typography().xxs.color(colorPalette().textDisabled)
//                                .align(TextAlign.Start),
//                            modifier = Modifier
//                                .padding(horizontal = 16.dp)
//                                .padding(bottom = 16.dp)
//                        )
//                    }
//
//                }
//            }

            item {
                Title2Actions(
                            title = stringResource(R.string.songs),
                            enableClick = topSongs.isNotEmpty(),
                            onClick1 = {
                                //println("ArtistOverviewModern onClick: browseId: ${it.moreEndpoint?.browseId} params: ${it.moreEndpoint?.params}")
//                                if (it.moreEndpoint?.browseId != null) {
//                                    itemsBrowseId = it.moreEndpoint!!.browseId!!
//                                    itemsParams = it.moreEndpoint!!.params.toString()
//                                    itemsSectionName = it.title
//                                    showArtistItems = true
//                                }

                            },
                            icon2 = R.drawable.dice,
                            onClick2 = {
//                                if (it.items.isEmpty()) return@Title2Actions
//                                val idItem = it.items.get(
//                                    if (it.items.size > 1)
//                                        Random(System.currentTimeMillis()).nextInt(0, it.items.size-1)
//                                    else 0
//                                ).key
//                                navController.navigate(route = "${NavRoutes.album.name}/${idItem}")
                            }
                        )
            }
            items(
                count = topSongs.size
            ) { index ->
                val item = topSongs[index]
                if (parentalControlEnabled && item.asMediaItem.isExplicit) return@items
                SwipeablePlaylistItem(
                                    mediaItem = item.asMediaItem,
                                    onPlayNext = {
                                        binder?.player?.addNext(item.asMediaItem, queue = selectedQueue ?: defaultQueue())
                                    },
                                    onDownload = {},
                                    onEnqueue = {
                                        binder?.player?.enqueue(item.asMediaItem, queue = it)
                                    }
                                ) {
                                    var forceRecompose by remember { mutableStateOf(false) }
                                    SongItem(
                                        song = item,
                                        thumbnailSizePx = songThumbnailSizePx,
                                        thumbnailSizeDp = songThumbnailSizeDp,
                                        disableScrollingText = disableScrollingText,
                                        isNowPlaying = false,
                                        forceRecompose = forceRecompose,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            navController = navController,
                                                            onDismiss = {
                                                                menuState.hide()
                                                                forceRecompose = true
                                                            },
                                                            onInfo = {
                                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${item.id}")
                                                            },
                                                            mediaItem = item.asMediaItem,
                                                            disableScrollingText = disableScrollingText
                                                        )
                                                    };
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                },
                                                onClick = {
                                                    //binder?.stopRadio()
                                                    fastPlay(item.asMediaItem, binder)
                                                }
                                            )
                                    )
                                }
            }
            item {
                Title2Actions(
                            title = stringResource(R.string.albums),
                            enableClick = albums.isNotEmpty(),
                            onClick1 = {
                                //println("ArtistOverviewModern onClick: browseId: ${it.moreEndpoint?.browseId} params: ${it.moreEndpoint?.params}")
//                                if (it.moreEndpoint?.browseId != null) {
//                                    itemsBrowseId = it.moreEndpoint!!.browseId!!
//                                    itemsParams = it.moreEndpoint!!.params.toString()
//                                    itemsSectionName = it.title
//                                    showArtistItems = true
//                                }

                            },
                            icon2 = R.drawable.dice,
                            onClick2 = {
//                                if (it.items.isEmpty()) return@Title2Actions
//                                val idItem = it.items.get(
//                                    if (it.items.size > 1)
//                                        Random(System.currentTimeMillis()).nextInt(0, it.items.size-1)
//                                    else 0
//                                ).key
//                                navController.navigate(route = "${NavRoutes.album.name}/${idItem}")
                            }
                        )
            }
            item {
                LazyRow(contentPadding = endPaddingValues) {
                    items(albums.size) { index ->
                        val item = albums[index]
                        AlbumItem(
                            album = item,
                            thumbnailSizePx = albumThumbnailSizePx,
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            disableScrollingText = disableScrollingText,
                            isYoutubeAlbum = false,
                        )
                    }
                }
            }
//            artistPage.sections.forEach() { it ->
//                //println("ArtistOverviewModern title: ${it.title} browseId: ${it.moreEndpoint?.browseId} params: ${it.moreEndpoint?.params}")
//                item {
//                    if (it.items.firstOrNull() is Environment.SongItem) {
//                        Title(
//                            title = it.title,
//                            enableClick = it.moreEndpoint?.browseId != null,
//                            onClick = {
//                                //println("ArtistOverviewModern onClick: browseId: ${it.moreEndpoint?.browseId} params: ${it.moreEndpoint?.params}")
//                                if (it.moreEndpoint?.browseId != null) {
//                                    itemsBrowseId = it.moreEndpoint!!.browseId!!
//                                    itemsParams = it.moreEndpoint!!.params.toString()
//                                    itemsSectionName = it.title
//                                    showArtistItems = true
//                                }
//
//                            },
//                        )
//                    } else {
//                        Title2Actions(
//                            title = it.title,
//                            enableClick = it.moreEndpoint?.browseId != null,
//                            onClick1 = {
//                                //println("ArtistOverviewModern onClick: browseId: ${it.moreEndpoint?.browseId} params: ${it.moreEndpoint?.params}")
//                                if (it.moreEndpoint?.browseId != null) {
//                                    itemsBrowseId = it.moreEndpoint!!.browseId!!
//                                    itemsParams = it.moreEndpoint!!.params.toString()
//                                    itemsSectionName = it.title
//                                    showArtistItems = true
//                                }
//
//                            },
//                            icon2 = R.drawable.dice,
//                            onClick2 = {
//                                if (it.items.isEmpty()) return@Title2Actions
//                                val idItem = it.items.get(
//                                    if (it.items.size > 1)
//                                        Random(System.currentTimeMillis()).nextInt(0, it.items.size-1)
//                                    else 0
//                                ).key
//                                navController.navigate(route = "${NavRoutes.album.name}/${idItem}")
//                            }
//                        )
//                    }
//                }
//                if (it.items.firstOrNull() is Environment.SongItem) {
//                    items(it.items) { item ->
//                        when (item) {
//                            is Environment.SongItem -> {
//                                if (parentalControlEnabled && item.explicit) return@items
//
//                                println("Innertube artistmodern SongItem: ${item.info?.name}")
//                                SwipeablePlaylistItem(
//                                    mediaItem = item.asMediaItem,
//                                    onPlayNext = {
//                                        binder?.player?.addNext(item.asMediaItem, queue = selectedQueue ?: defaultQueue())
//                                    },
//                                    onDownload = {},
//                                    onEnqueue = {
//                                        binder?.player?.enqueue(item.asMediaItem, queue = it)
//                                    }
//                                ) {
//                                    var forceRecompose by remember { mutableStateOf(false) }
//                                    SongItem(
//                                        song = item,
//                                        thumbnailSizePx = songThumbnailSizePx,
//                                        thumbnailSizeDp = songThumbnailSizeDp,
//                                        disableScrollingText = disableScrollingText,
//                                        isNowPlaying = false,
//                                        forceRecompose = forceRecompose,
//                                        modifier = Modifier
//                                            .combinedClickable(
//                                                onLongClick = {
//                                                    menuState.display {
//                                                        NonQueuedMediaItemMenu(
//                                                            navController = navController,
//                                                            onDismiss = {
//                                                                menuState.hide()
//                                                                forceRecompose = true
//                                                            },
//                                                            onInfo = {
//                                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${item.key}")
//                                                            },
//                                                            mediaItem = item.asMediaItem,
//                                                            disableScrollingText = disableScrollingText
//                                                        )
//                                                    };
//                                                    hapticFeedback.performHapticFeedback(
//                                                        HapticFeedbackType.LongPress
//                                                    )
//                                                },
//                                                onClick = {
//                                                    binder?.stopRadio()
//                                                    CoroutineScope(Dispatchers.IO).launch {
//                                                        artistPage.sections.firstOrNull{sec -> sec.items.firstOrNull() is Environment.SongItem}.let {
//                                                            songsBrowseId = it?.moreEndpoint?.browseId.toString()
//                                                            songsParams = it?.moreEndpoint?.params.toString()
//                                                        }
//                                                        if (songsBrowseId.isNotEmpty())
//                                                            BrowseEndpoint(
//                                                                browseId = songsBrowseId,
//                                                                params = songsParams
//                                                            ).let { endpoint ->
//                                                                val artistSongs = EnvironmentExt.getArtistItemsPage(endpoint)
//                                                                    .completed()
//                                                                    .getOrNull()
//                                                                    ?.items
//                                                                    ?.map{ it as Environment.SongItem }
//                                                                    ?.map { it.asMediaItem }
//
//                                                                    val filteredArtistSongs = artistSongs
//                                                                    ?.filter { it.mediaId != Database.songDisliked(it.mediaId) }
//
//                                                                    //if (artistSongs?.contains(item.asMediaItem) == false){
//                                                                        withContext(Dispatchers.Main) {
//                                                                            //binder?.player?.forcePlay(item.asMediaItem)
//                                                                            fastPlay(item.asMediaItem, binder)
//                                                                            if (filteredArtistSongs != null) {
//                                                                                binder?.player?.addMediaItems(filteredArtistSongs.filterNot { it.mediaId == item.key })
//                                                                            }
//                                                                        }
//    //                                                                } else {
//    //                                                                    SmartMessage(context.resources.getString(R.string.disliked_this_song),type = PopupType.Error, context = context)
//    //                                                                }
//
//                                                            }
//                                                    }
//                                                }
//                                            )
//                                    )
//                                }
//                            }
//
//                            else -> {}
//                        }
//                    }
//                } else {
//                    item {
//                        LazyRow(contentPadding = endPaddingValues) {
//                            items(it.items) { item ->
//                                when (item) {
//                                    is Environment.SongItem -> {}
//
//                                    is Environment.AlbumItem -> {
//                                        println("Innertube artistmodern AlbumItem: ${item.info?.name}")
//                                        var albumById by remember { mutableStateOf<Album?>(null) }
//                                        LaunchedEffect(item) {
//                                            CoroutineScope(Dispatchers.IO).launch {
//                                                albumById = Database.album(item.key).firstOrNull()
//                                            }
//                                        }
//                                        AlbumItem(
//                                            album = item,
//                                            alternative = true,
//                                            thumbnailSizePx = albumThumbnailSizePx,
//                                            thumbnailSizeDp = albumThumbnailSizeDp,
//                                            disableScrollingText = disableScrollingText,
//                                            isYoutubeAlbum = albumById?.isYoutubeAlbum == true,
//                                            modifier = Modifier.clickable(onClick = {
//                                                navController.navigate("${NavRoutes.album.name}/${item.key}")
//                                            })
//
//                                        )
//                                    }
//
//                                    is Environment.ArtistItem -> {
//                                        println("Innertube v ArtistItem: ${item.info?.name}")
//                                        var artistById by remember { mutableStateOf<Artist?>(null) }
//                                        LaunchedEffect(item) {
//                                            CoroutineScope(Dispatchers.IO).launch {
//                                                artistById = Database.artist(item.key).firstOrNull()
//                                            }
//                                        }
//                                        ArtistItem(
//                                            artist = item,
//                                            thumbnailSizePx = artistThumbnailSizePx,
//                                            thumbnailSizeDp = artistThumbnailSizeDp,
//                                            disableScrollingText = disableScrollingText,
//                                            isYoutubeArtist = artistById?.isYoutubeArtist == true,
//                                            modifier = Modifier.clickable(onClick = {
//                                                navController.navigate("${NavRoutes.artist.name}/${item.key}")
//                                            })
//                                        )
//                                    }
//
//                                    is Environment.PlaylistItem -> {
//                                        println("Innertube v PlaylistItem: ${item.info?.name}")
//                                        var playlistById by remember { mutableStateOf<Playlist?>(null) }
//                                        LaunchedEffect(item) {
//                                            CoroutineScope(Dispatchers.IO).launch {
//                                                playlistById = Database.playlist(item.key.substringAfter("VL")).firstOrNull()
//                                            }
//                                        }
//                                        PlaylistItem(
//                                            playlist = item,
//                                            alternative = true,
//                                            thumbnailSizePx = playlistThumbnailSizePx,
//                                            thumbnailSizeDp = playlistThumbnailSizeDp,
//                                            disableScrollingText = disableScrollingText,
//                                            isYoutubePlaylist = playlistById?.isYoutubePlaylist == true,
//                                            modifier = Modifier.clickable(onClick = {
//                                                navController.navigate("${NavRoutes.playlist.name}/${item.key}")
//                                            })
//                                        )
//                                    }
//
//                                    is Environment.VideoItem -> {
//                                        println("Innertube v VideoItem: ${item.info?.name}")
//                                        VideoItem(
//                                            video = item,
//                                            thumbnailHeightDp = playlistThumbnailSizeDp,
//                                            thumbnailWidthDp = playlistThumbnailSizeDp,
//                                            disableScrollingText = disableScrollingText
//                                        )
//                                    }
//                                }
//
//                            }
//                        }
//                    }
//                }
//            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }

        }

        val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
//        if (UiType.ViMusic.isCurrent() && showFloatingIcon)
//            artistPage?.radioEndpoint?.let { endpoint ->
//
//                MultiFloatingActionsContainer(
//                    iconId = R.drawable.radio,
//                    onClick = {
//                        binder?.stopRadio()
//                        binder?.playRadio(endpoint)
//                    },
//                    onClickSettings = { navController.navigate(NavRoutes.search.name) },
//                    onClickSearch = { navController.navigate(NavRoutes.settings.name) }
//                )
//
//            }


//        CustomModalBottomSheet(
//            showSheet = showArtistItems,
//            onDismissRequest = { showArtistItems = false },
//            containerColor = colorPalette().background2,
//            contentColor = colorPalette().background2,
//            modifier = Modifier
//                .fillMaxWidth(),
//            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
//            dragHandle = {
//                Surface(
//                    modifier = Modifier.padding(vertical = 0.dp),
//                    color = colorPalette().background0,
//                    shape = thumbnailShape()
//                ) {}
//            },
//            shape = thumbnailRoundness.shape()
//        ) {
//            ArtistOverviewItems(
//                navController,
//                artistName = cleanPrefix(artist?.name ?: ""),
//                sectionName = itemsSectionName,
//                browseId = itemsBrowseId,
//                params = itemsParams,
//                disableScrollingText = false,
//                onDismiss = { showArtistItems = false }
//            )
//        }


    }

}
