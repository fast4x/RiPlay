package it.fast4x.riplay.utils

import android.content.Context
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.VideoOrSongInfo
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.AlbumPage
import it.fast4x.environment.requests.artistPage
import it.fast4x.environment.requests.nextPage
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.from
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.extensions.preferences.artistScreenTabIndexKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.items.VideoItemPlaceholder
import it.fast4x.riplay.ui.screens.searchresult.ItemsPage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.secondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@UnstableApi
data class OnlineRadio (
    private val videoId: String? = null,
    private var playlistId: String? = null,
    private var playlistSetVideoId: String? = null,
    private var parameters: String? = null,
    private val isDiscoverEnabled: Boolean = false,
    private val context: Context,
    private val binder: PlayerService.Binder? = null,
    private val coroutineScope: CoroutineScope
) {
    private var nextContinuation: String? = null

    suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        nextContinuation = withContext(Dispatchers.IO) {
            val continuation = nextContinuation

            if (continuation == null) {
                Environment.nextPage(
                    NextBody(
                        videoId = videoId,
                        playlistId = playlistId,
                        params = parameters,
                        playlistSetVideoId = playlistSetVideoId
                    )
                )?.map { nextResult ->
                    playlistId = nextResult.playlistId
                    parameters = nextResult.params
                    playlistSetVideoId = nextResult.playlistSetVideoId

                    nextResult.itemsPage
                }
            } else {
                Environment.nextPage(ContinuationBody(continuation = continuation))
            }?.getOrNull()?.let { songsPage ->
                mediaItems = songsPage.items?.map(Environment.SongItem::asMediaItem)
                songsPage.continuation?.takeUnless { nextContinuation == it }
            }

        }
            //coroutineScope.launch(Dispatchers.Main) {

        fun songsInQueue(mediaId: String): String? {
            var mediaIdFound = false
            runBlocking {
                withContext(Dispatchers.Main) {
                    for (i in 0 until (binder?.player?.mediaItemCount ?: 0) - 1) {
                        if (mediaId == binder?.player?.getMediaItemAt(i)?.mediaId) {
                            mediaIdFound = true
                            return@withContext
                        }
                    }
                }
            }
            if(mediaIdFound){
                return mediaId
            }
            return null
        }


            if (isDiscoverEnabled) {
                var listMediaItems = mutableListOf<MediaItem>()
                withContext(Dispatchers.IO) {
                    mediaItems?.forEach {
                        val songInPlaylist = Database.songUsedInPlaylists(it.mediaId)
                        val songIsLiked = (Database.getLikedAt(it.mediaId) !in listOf(-1L,null))
                        val sIQ = songsInQueue(it.mediaId)
                        if (songInPlaylist == 0 && !songIsLiked && (it.mediaId != sIQ)) {
                            listMediaItems.add(it)
                        }
                    }
                }

                SmartMessage(
                    context.resources.getString(R.string.discover_has_been_applied_to_radio).format(
                        mediaItems?.size?.minus(listMediaItems.size) ?: 0
                    ), PopupType.Success, context = context
                )

                mediaItems = listMediaItems
            }

        withContext(Dispatchers.IO) {
            mediaItems = mediaItems?.filter {
                (Database.getLikedAt(it.mediaId) != -1L)
            }?.distinct()
        }

        return mediaItems ?: emptyList()
    }
}

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun SearchOnlineEntity (
    navController: NavController,
    onDismiss: () -> Unit,
    query: String,
    filter: Environment.SearchFilter = Environment.SearchFilter.Video,
    disableScrollingText: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current
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

    var filterContentType by remember { mutableStateOf(it.fast4x.riplay.enums.ContentType.Official) }

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
                                it.fast4x.riplay.enums.ContentType.entries.forEach {
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
                        ContentType.All -> ContentType.All.textName
                        ContentType.Official -> ContentType.Official.textName
                        ContentType.UserGenerated -> ContentType.UserGenerated.textName

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
                                                        onDismiss = menuState::hide,
                                                        mediaItem = media.asMediaItem,
                                                        disableScrollingText = disableScrollingText,
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
                                                binder?.player?.forcePlay(media.asMediaItem)
                                                binder?.setupRadio(media.info?.endpoint)
                                                //fastPlay(media.asMediaItem, binder, withReplace = true)
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
                                    //disableScrollingText = disableScrollingText,
                                    //isNowPlaying = false,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        navController = rememberNavController(),
                                                        onDismiss = menuState::hide,
                                                        mediaItem = media.asMediaItem,
                                                        disableScrollingText = disableScrollingText,
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
                                                binder?.player?.forcePlay(media.asMediaItem)
                                                binder?.setupRadio(media.info?.endpoint)
                                                //fastPlay(media.asMediaItem, binder, withReplace = true)
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

@Composable
fun UpdateOnlineArtist(browseId: String) {

    var artistPage by persist<Environment.ArtistInfoPage?>("artist/$browseId/artistPage")
    var artist by persist<Artist?>("artist/$browseId/artist")
    val tabIndex by rememberPreference(artistScreenTabIndexKey, defaultValue = 0)

    LaunchedEffect(browseId) {
        Database
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && (currentArtist?.timestamp == null || mustFetch)) {
                    withContext(Dispatchers.IO) {
                        Environment.artistPage(BrowseBody(browseId = browseId))
                            ?.onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage

                                Database.upsert(
                                    Artist(
                                        id = browseId,
                                        name = currentArtistPage.name,
                                        thumbnailUrl = currentArtistPage.thumbnail?.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = currentArtist?.bookmarkedAt
                                    )
                                )
                            }
                    }
                }
            }
    }

}

@UnstableApi
@Composable
fun UpdateOnlineAlbum (
    browseId: String,
    onFetch: ((album: Album?, albumPage: AlbumPage?) -> Unit)? = null
) {
    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<AlbumPage?>("album/$browseId/albumPage")
    LaunchedEffect(browseId) {
        Database
            .album(browseId).collect { currentAlbum ->
                Timber.d("UpdateYoutubeAlbum collect ${currentAlbum?.title}")
                album = currentAlbum
                CoroutineScope(Dispatchers.IO).launch {
                    if (albumPage == null)
                        EnvironmentExt.getAlbum(browseId)
                            .onSuccess { currentAlbumPage ->
                                albumPage = currentAlbumPage

                                Timber.d("UpdateYoutubeAlbum otherVersion ${currentAlbumPage.otherVersions}")
                                Database.upsert(
                                    Album(
                                        id = browseId,
                                        title = album?.title ?: currentAlbumPage.album.title,
                                        thumbnailUrl = if (album?.thumbnailUrl?.startsWith(
                                                MODIFIED_PREFIX
                                            ) == true
                                        ) album?.thumbnailUrl else currentAlbumPage.album.thumbnail?.url,
                                        year = currentAlbumPage.album.year,
                                        authorsText = if (album?.authorsText?.startsWith(
                                                MODIFIED_PREFIX
                                            ) == true
                                        ) album?.authorsText else currentAlbumPage.album.authors
                                            ?.joinToString(", ") { it.name ?: "" },
                                        shareUrl = currentAlbumPage.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt,
                                        isYoutubeAlbum = album?.isYoutubeAlbum == true
                                    ),
                                    currentAlbumPage
                                        .songs.distinct()
                                        .map(Environment.SongItem::asMediaItem)
                                        .onEach(Database::insert)
                                        .mapIndexed { position, mediaItem ->
                                            SongAlbumMap(
                                                songId = mediaItem.mediaId,
                                                albumId = browseId,
                                                position = position
                                            )
                                        }
                                )
                            }
                            .onFailure {
                                Timber.e("AlbumScreen error ${it.stackTraceToString()}")
//                            if (it.message?.contains("NOT_FOUND") == true) {
//                                // This album no longer exists in YouTube Music
//                                Database.asyncTransaction {
//                                    album?.let(::delete)
//                                }
//                            }
                            }
                }
            }
//        Database
//            .album(browseId)
//            .combine(snapshotFlow { tabIndex }) { album, tabIndex -> album to tabIndex }
//            .collect { (currentAlbum, tabIndex) ->
//                album = currentAlbum
//
//                if (albumPage == null && (currentAlbum?.timestamp == null || tabIndex == 1)) {
//                    withContext(Dispatchers.IO) {
//                        Environment.albumPage(BrowseBody(browseId = browseId))
//                            ?.onSuccess { currentAlbumPage ->
//                                albumPage = currentAlbumPage
//
//                                Database.clearAlbum(browseId)
//
//                                Database.upsert(
//                                    Album(
//                                        id = browseId,
//                                        title = currentAlbumPage?.title,
//                                        thumbnailUrl = currentAlbumPage?.thumbnail?.url,
//                                        year = currentAlbumPage?.year,
//                                        authorsText = currentAlbumPage?.authors
//                                            ?.joinToString("") { it.name ?: "" },
//                                        shareUrl = currentAlbumPage?.url,
//                                        timestamp = System.currentTimeMillis(),
//                                        bookmarkedAt = album?.bookmarkedAt
//                                    ),
//                                    currentAlbumPage
//                                        ?.songsPage
//                                        ?.items
//                                        ?.map(Environment.SongItem::asMediaItem)
//                                        ?.onEach(Database::insert)
//                                        ?.mapIndexed { position, mediaItem ->
//                                            SongAlbumMap(
//                                                songId = mediaItem.mediaId,
//                                                albumId = browseId,
//                                                position = position
//                                            )
//                                        } ?: emptyList()
//                                )
//                            }
//                    }
//
//                }
//            }
        onFetch?.invoke(album, albumPage)
    }

}

@UnstableApi
@Composable
fun ShowVideoOrSongInfo(
    videoId: String,
) {

    if (videoId.isBlank()) return

    val thumbnailRoundness by rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<VideoOrSongInfo?>(null)
    }

    LaunchedEffect(Unit, videoId) {
        info = EnvironmentExt.getVideOrSongInfo(videoId).getOrNull()
        //println("ShowVideoOrSongInfo: ${info?.description}")
    }

    //if (info == null) return


    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(
                windowInsets
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .asPaddingValues()
            )
            .background(colorPalette().background0)
            .fillMaxSize()
    ) {
        item(contentType = "InfoTitlePage") {
            Title(
                title = stringResource(R.string.information),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
                icon = R.drawable.chevron_down,
                onClick = {},
                enableClick = true
            )
        }
        if (info != null) {
            item(contentType = "InfoTitle") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                ) {
                    TitleMiniSection(
                        title = stringResource(R.string.title)
                    )
                    BasicText(
                        text = "" + info?.title,
                        style = typography().xs.color(colorPalette().text)
                            .align(TextAlign.Start),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )
                }
            }
            item(contentType = "InfoAuthor") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                ) {
                    TitleMiniSection(
                        title = stringResource(R.string.artists)
                    )
                    BasicText(
                        text = "" + info?.author,
                        style = typography().xs.color(colorPalette().text)
                            .align(TextAlign.Start),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )
                }
            }
            item(contentType = "InfoDescription") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    TitleMiniSection(
                        title = stringResource(R.string.description)
                    )
                    BasicText(
                        text = info?.description ?: "",
                        style = typography().xs.color(colorPalette().text)
                            .align(TextAlign.Start),
                        modifier = Modifier
                            .padding(all = 16.dp)
                    )
                }
            }
            item(contentType = "InfoNumbers") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    TitleMiniSection(
                        title = stringResource(R.string.numbers)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column {
                            BasicText(
                                text = stringResource(R.string.subscribers),
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier
                            )
                            BasicText(
                                text = info?.subscribers ?: "",
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.views),
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier
                            )
                            BasicText(
                                text = "" + info?.viewCount?.toInt()
                                    ?.let { numberFormatter(it) },
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.likes),
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier
                            )
                            BasicText(
                                text = "" + info?.like?.toInt()?.let { numberFormatter(it) },
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.dislikes),
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier
                            )
                            BasicText(
                                text = "" + info?.dislike?.toInt()?.let { numberFormatter(it) },
                                style = typography().xs.color(colorPalette().text)
                                    .align(TextAlign.Start),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                    }

                }
            }

        } else {
            item(contentType = "InfoLoader") {
                Loader()
            }
        }
    }


}