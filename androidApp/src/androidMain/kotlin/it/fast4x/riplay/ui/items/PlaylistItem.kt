package it.fast4x.riplay.ui.items

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.fast4x.environment.Environment
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.ui.components.themed.TextPlaceholder
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.commonutils.PIPED_PREFIX
import it.fast4x.riplay.ui.styling.onOverlay
import it.fast4x.riplay.ui.styling.overlay
import it.fast4x.riplay.ui.styling.shimmer
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.utils.checkFileExists
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.ui.styling.youtubeItemTintColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import timber.log.Timber

private val quadrantAlignments = listOf(
    Alignment.TopStart,
    Alignment.TopEnd,
    Alignment.BottomStart,
    Alignment.BottomEnd
)

private const val LIKED_MUSIC_THUMBNAIL_URL =
    "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-music-@1200.png"

@Composable
fun PlaylistItem(
    @DrawableRes icon: Int,
    colorTint: Color,
    name: String?,
    songCount: Int?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    showName: Boolean = true,
    iconSize: Dp = 34.dp,
    disableScrollingText: Boolean,
    isYoutubePlaylist : Boolean = false,
    isEditable : Boolean = false,
    isPodcast : Boolean = false
) {
    PlaylistItem(
        thumbnailContent = {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorTint),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(iconSize)
            )
        },
        songCount = songCount,
        name = name,
        channelName = null,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        showName = showName,
        disableScrollingText = disableScrollingText,
        isYoutubePlaylist = isYoutubePlaylist,
        isEditable = isEditable,
        isPodcast = isPodcast
    )
}

@Composable
fun PlaylistItem(
    playlist: PlaylistPreview,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    homepage: Boolean = false,
    iconSize: Dp = 0.dp,
    alternative: Boolean = false,
    showName: Boolean = true,
    disableScrollingText: Boolean,
    isYoutubePlaylist: Boolean,
    isEditable: Boolean,
) {
    val context = LocalContext.current

    val thumbnailName = remember(playlist.playlist.id) {
        "thumbnail/playlist_${playlist.playlist.id}"
    }

    val playlistThumbnailUrl = remember(thumbnailName) {
        checkFileExists(context, thumbnailName)
    }

    val thumbnails by remember(playlist.playlist.id) {
        Database.playlistThumbnailUrls(playlist.playlist.id)
            .distinctUntilChanged()
            .map { urls -> urls.map { it.toThumbnail(thumbnailSizePx / 2) } }
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    // Calcolato fuori dal thumbnailContent per evitare ricalcoli dentro la lambda
    val halfThumbnailSizeDp = remember(thumbnailSizeDp) { thumbnailSizeDp / 2 }

    val isSingleThumbnail = remember(thumbnails) {
        thumbnails.isNotEmpty() && thumbnails.all { it == thumbnails.first() }
    }

    // Costruttore ImageRequest condiviso per evitare duplicazioni
    val buildImageRequest: (String?) -> ImageRequest = remember(context) {
        { data ->
            ImageRequest.Builder(context)
                .data(data)
                .setHeader("User-Agent", "Mozilla/5.0")
                .build()
        }
    }

    PlaylistItem(
        browseId = playlist.playlist.browseId,
        thumbnailContent = {
            when {
                playlistThumbnailUrl != null -> {
                    AsyncImage(
                        model = playlistThumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                playlist.playlist.browseId == "LM" -> {
                    AsyncImage(
                        model = LIKED_MUSIC_THUMBNAIL_URL,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isSingleThumbnail -> {
                    AsyncImage(
                        model = buildImageRequest(thumbnails.first()),
                        onError = { error ->
                            Timber.e("Failed AsyncImage in PlaylistItem ${error.result.throwable.stackTraceToString()}")
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        thumbnails.take(4).forEachIndexed { index, thumbnail ->
                            AsyncImage(
                                model = buildImageRequest(thumbnail),
                                onError = { error ->
                                    Timber.e("Failed AsyncImage 1 in PlaylistItem ${error.result.throwable.stackTraceToString()}")
                                },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(quadrantAlignments[index])
                                    .size(halfThumbnailSizeDp)
                            )
                        }
                    }
                }
            }
        },
        songCount = playlist.songCount,
        name = playlist.playlist.name,
        channelName = null,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        homePage = homepage,
        iconSize = iconSize,
        showName = showName,
        disableScrollingText = disableScrollingText,
        isYoutubePlaylist = isYoutubePlaylist,
        isEditable = isEditable,
        isPodcast = playlist.playlist.isPodcast
    )
}

/*
@Composable
fun PlaylistItem(
    playlist: PlaylistPreview,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    homepage: Boolean = false,
    iconSize: Dp = 0.dp,
    alternative: Boolean = false,
    showName: Boolean = true,
    disableScrollingText: Boolean,
    isYoutubePlaylist: Boolean,
    isEditable: Boolean,
) {
    val context = LocalContext.current

    val thumbnailName = "thumbnail/playlist_${playlist.playlist.id}"
    val playlistThumbnailUrl by remember { mutableStateOf(checkFileExists(context, thumbnailName)) }

    val thumbnails by remember {
        Database.playlistThumbnailUrls(playlist.playlist.id).distinctUntilChanged().map {
            it.map { url ->
                url.toThumbnail(thumbnailSizePx / 2)
            }
        }
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    //Timber.d("PlaylistItem.-: $thumbnails")

    PlaylistItem(
        browseId = playlist.playlist.browseId,
        thumbnailContent = {
            if (playlistThumbnailUrl != null) {
                //Timber.d("PlaylistItem.-: playlistThumbnailUrl not null")
                AsyncImage(
                    model = playlistThumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (playlist.playlist.browseId == "LM") {
                //Timber.d("PlaylistItem.-: playlist LM")
                AsyncImage(
                    model = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-music-@1200.png",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (thumbnails.toSet().size == 1) {
                //Timber.d("PlaylistItem.-: 1 image playlist ${playlist.playlist.name}")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnails.first())
                        .setHeader("User-Agent", "Mozilla/5.0")
                        .build(),
                    onError = {error ->
                        Timber.e("Failed AsyncImage in PlaylistItem ${error.result.throwable.stackTraceToString()}")
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                //Timber.d("PlaylistItem.-: 4 images  ${playlist.playlist.name}")
                Box(
                    modifier = Modifier // KOTLIN 2
                        .fillMaxSize()
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd
                    ).forEachIndexed { index, alignment ->
                        val thumbnail = thumbnails.getOrNull(index)
                        if (thumbnail != null)
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(thumbnail)
                                    .setHeader("User-Agent", "Mozilla/5.0")
                                    .build(),
                                onError = {error ->
                                    Timber.e("Failed AsyncImage 1 in PlaylistItem ${error.result.throwable.stackTraceToString()}")
                                },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(alignment)
                                    .size(thumbnailSizeDp / 2)
                            )
                    }
                }
            }
        },
        songCount = playlist.songCount,
        name = playlist.playlist.name,
        channelName = null,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        homePage = homepage,
        iconSize = iconSize,
        showName = showName,
        disableScrollingText = disableScrollingText,
        isYoutubePlaylist = isYoutubePlaylist,
        isEditable = isEditable,
        isPodcast = playlist.playlist.isPodcast
    )
}

 */

@Composable
fun PlaylistItem(
    playlist: Environment.PlaylistItem,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    showSongsCount: Boolean = true,
    disableScrollingText: Boolean,
    isYoutubePlaylist : Boolean = false,
    isEditable : Boolean = false
) {
    PlaylistItem(
        thumbnailUrl = playlist.thumbnail?.url,
        songCount = playlist.songCount,
        showSongsCount = showSongsCount,
        name = playlist.info?.name,
        channelName = playlist.channel?.name,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        disableScrollingText = disableScrollingText,
        isYoutubePlaylist = isYoutubePlaylist,
        isEditable = isEditable
    )
}

@Composable
fun PlaylistItem(
    thumbnailUrl: String?,
    songCount: Int?,
    name: String?,
    channelName: String?,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    showSongsCount: Boolean = true,
    disableScrollingText: Boolean,
    isYoutubePlaylist : Boolean = false,
    isEditable : Boolean = false,
    isPodcast : Boolean = false
) {
    PlaylistItem(
        thumbnailContent = {
            AsyncImage(
                model = thumbnailUrl?.toThumbnail(thumbnailSizePx),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        },
        songCount = songCount,
        showSongsCount = showSongsCount,
        name = name,
        channelName = channelName,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        disableScrollingText = disableScrollingText,
        isYoutubePlaylist = isYoutubePlaylist,
        isEditable = isEditable,
        isPodcast = isPodcast
    )
}

@Composable
fun PlaylistItem(
    browseId: String? = null,
    thumbnailContent: @Composable BoxScope.() -> Unit,
    songCount: Int?,
    name: String?,
    channelName: String?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    homePage: Boolean = false,
    iconSize: Dp = 0.dp,
    alternative: Boolean = false,
    showName: Boolean = true,
    showSongsCount: Boolean = true,
    disableScrollingText: Boolean,
    isYoutubePlaylist: Boolean = false,
    isEditable: Boolean = false,
    isPodcast: Boolean = false
) {
    val colorPalette = colorPalette()
    val thumbnailShape = thumbnailShape()

    val localIconSize = remember(homePage, iconSize) {
        if (homePage) 0.2 * iconSize else 30.dp
    }

    // ColorFilter memoizzati
    val accentFilter = remember(colorPalette.accent) {
        ColorFilter.tint(colorPalette.accent)
    }
    val textDisabledFilter = remember(colorPalette.textDisabled) {
        ColorFilter.tint(colorPalette.textDisabled)
    }
    val youtubeTintFilter = remember {
        ColorFilter.tint(youtubeItemTintColor)
    }

    // Modifier comune per le icone overlay
    val iconOverlayModifier = remember(localIconSize, colorPalette.text) {
        Modifier
            .padding(all = 5.dp)
            .background(colorPalette.text, CircleShape)
            .size(localIconSize)
            .padding(all = 5.dp)
    }

    // Prefissi memoizzati
    val isPinned = remember(name) { name?.startsWith(PINNED_PREFIX, 0, true) == true }
    val isMonthly = remember(name) { name?.startsWith(MONTHLY_PREFIX, 0, true) == true }
    val isPiped  = remember(name) { name?.startsWith(PIPED_PREFIX, 0, true) == true }

    val showInternetIcon = remember(browseId, isPiped, isYoutubePlaylist) {
        (browseId?.isNotEmpty() == true && !isPiped) || isYoutubePlaylist
    }

    val cleanName = remember(name) { name?.let { cleanPrefix(it) } }
    val songCountText = remember(songCount) { songCount?.toString() }

    val infoAlignment = remember(alternative, channelName) {
        if (alternative && channelName == null) Alignment.CenterHorizontally else Alignment.Start
    }

    ItemContainer(
        alternative = alternative,
        thumbnailSizeDp = thumbnailSizeDp,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(thumbnailShape)
                .background(color = colorPalette.background4)
                .requiredSize(thumbnailSizeDp)
        ) {
            thumbnailContent()

            if (isPinned) {
                Image(
                    painter = painterResource(R.drawable.pin),
                    colorFilter = accentFilter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = iconOverlayModifier
                )
            }

            if (isMonthly) {
                Image(
                    painter = painterResource(R.drawable.stat_month),
                    colorFilter = accentFilter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = iconOverlayModifier
                )
            }

            if (showInternetIcon) {
                Image(
                    painter = painterResource(R.drawable.internet),
                    colorFilter = if (isYoutubePlaylist) youtubeTintFilter else textDisabledFilter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = iconOverlayModifier
                )
            }

            if (isPodcast) {
                Image(
                    painter = painterResource(R.drawable.podcast),
                    colorFilter = if (isYoutubePlaylist) youtubeTintFilter else textDisabledFilter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = iconOverlayModifier.then(Modifier.align(Alignment.TopEnd))
                )
            }

            if (isYoutubePlaylist && !isEditable) {
                Image(
                    painter = painterResource(R.drawable.locked),
                    colorFilter = textDisabledFilter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = iconOverlayModifier.then(Modifier.align(Alignment.BottomStart))
                )
            }

            if (showSongsCount && songCountText != null) {
                BasicText(
                    text = songCountText,
                    style = typography().xxs.medium.color(colorPalette.onOverlay),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .background(color = colorPalette.overlay, shape = getRoundnessShape())
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        ItemInfoContainer(
            horizontalAlignment = infoAlignment,
            modifier = Modifier.fillMaxSize()
        ) {
            if (showName && cleanName != null) {
                BasicText(
                    text = cleanName,
                    style = typography().xs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                )
            }

            channelName?.let {
                BasicText(
                    text = it,
                    style = typography().xs.semiBold.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                )
            }
        }
    }
}

/*
@Composable
fun PlaylistItem(
    browseId: String? = null,
    thumbnailContent: @Composable BoxScope.() -> Unit,
    songCount: Int?,
    name: String?,
    channelName: String?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    homePage: Boolean = false,
    iconSize: Dp = 0.dp,
    alternative: Boolean = false,
    showName: Boolean = true,
    showSongsCount: Boolean = true,
    disableScrollingText: Boolean,
    isYoutubePlaylist : Boolean = false,
    isEditable : Boolean = false,
    isPodcast : Boolean = false
) {
    val localIconSize = remember { if (homePage) 0.2*iconSize else 30.dp }
    ItemContainer(
        alternative = alternative,
        thumbnailSizeDp = thumbnailSizeDp,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) { //centeredModifier ->
        Box(
            modifier = Modifier
                .clip(thumbnailShape())
                .background(color = colorPalette().background4)
                .requiredSize(thumbnailSizeDp)
        ) {
            thumbnailContent(
                /*
                modifier = Modifier
                    .fillMaxSize()

                 */
            )

            name?.let {
//                if (it.startsWith(PIPED_PREFIX,0,true)) {
//                    Image(
//                        painter = painterResource(R.drawable.piped_logo),
//                        colorFilter = ColorFilter.tint(colorPalette().red),
//                        modifier = Modifier
//                            .size(iconSize)
//                            .padding(all = 5.dp),
//                        contentDescription = "Background Image",
//                        contentScale = ContentScale.Fit
//                    )
//                }
                if (it.startsWith(PINNED_PREFIX,0,true)) {
                    Image(
                        painter = painterResource(R.drawable.pin),
                        colorFilter = ColorFilter.tint(colorPalette().accent),
                        modifier = Modifier
                            .padding(all = 5.dp)
                            .background(colorPalette().text, CircleShape)
                            .size(localIconSize)
                            .padding(all = 5.dp),
                        contentDescription = "Background Image",
                        contentScale = ContentScale.Fit
                    )
                }
                if (it.startsWith(MONTHLY_PREFIX,0,true)) {
                    Image(
                        painter = painterResource(R.drawable.stat_month),
                        colorFilter = ColorFilter.tint(colorPalette().accent),
                        modifier = Modifier
                            .padding(all = 5.dp)
                            .background(colorPalette().text, CircleShape)
                            .size(localIconSize)
                            .padding(all = 5.dp),
                        contentDescription = "Background Image",
                        contentScale = ContentScale.Fit
                    )
                }

            }
            if ((browseId?.isNotEmpty() == true && name?.startsWith(PIPED_PREFIX) == false) || isYoutubePlaylist) {
                Image(
                    painter = painterResource(R.drawable.internet),
                    colorFilter = ColorFilter.tint(if (isYoutubePlaylist) Color.Red.copy(0.75f).compositeOver(Color.White) else colorPalette().textDisabled),
                    modifier = Modifier
                        .padding(all = 5.dp)
                        .background(colorPalette().text, CircleShape)
                        .size(localIconSize)
                        .padding(all = 5.dp),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Fit
                )
            }

            if (isPodcast) {
                Image(
                    painter = painterResource(R.drawable.podcast),
                    colorFilter = ColorFilter.tint(if (isYoutubePlaylist) Color.Red.copy(0.75f).compositeOver(Color.White) else colorPalette().textDisabled),
                    modifier = Modifier
                        .padding(all = 5.dp)
                        .background(colorPalette().text, CircleShape)
                        .size(localIconSize)
                        .padding(all = 5.dp)
                        .align(Alignment.TopEnd),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Fit
                )
            }

            if (isYoutubePlaylist && !isEditable){
                Image(
                    painter = painterResource(R.drawable.locked),
                    colorFilter = ColorFilter.tint(colorPalette().textDisabled),
                    modifier = Modifier
                        .padding(all = 5.dp)
                        .background(colorPalette().text, CircleShape)
                        .padding(all = 5.dp)
                        .size(localIconSize)
                        .align(Alignment.BottomStart),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Fit
                )
            }

            if (showSongsCount)
                songCount?.let {
                    BasicText(
                        text = "$songCount",
                        style = typography().xxs.medium.color(colorPalette().onOverlay),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .background(color = colorPalette().overlay, shape = getRoundnessShape())
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .align(Alignment.BottomEnd)
                    )
                }

        }


        ItemInfoContainer(
            horizontalAlignment = if (alternative && channelName == null) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (showName)
                if (name != null) {
                    BasicText(
                        //text = name.substringAfter(PINNED_PREFIX) ?: "",
                        text = cleanPrefix(name),
                        style = typography().xs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )
                }

            channelName?.let {
                BasicText(
                    text = channelName,
                    style = typography().xs.semiBold.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                )
            }
        }
    }
}

 */

@Composable
fun PlaylistItemPlaceholder(
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
) {
    ItemContainer(
        alternative = alternative,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .background(color = colorPalette().shimmer, shape = thumbnailShape())
                .size(thumbnailSizeDp)
        )

        ItemInfoContainer(
            horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            TextPlaceholder()
            TextPlaceholder()
        }
    }
}
