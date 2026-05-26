package it.fast4x.riplay.extensions.experimental.ui.screens.artist

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.requests.ArtistPage
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.fastshare.FastShare
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.extensions.appviewmodel.isNetworkConnected
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.fadingEdge
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PARENTAL_CONTROL_ENABLED
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FLOATING_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.ui.components.themed.FastPlayActionsBar
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.screens.artist.ArtistLibrarySongs
import it.fast4x.riplay.ui.screens.artist.ArtistOverviewItems
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.forcePlayFromBeginning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.random.Random

// ─── Piccolo composable helper: icona azione con background pill quando attiva ───
@Composable
private fun ActionIconButton(
    icon: Int,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Int = 24,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val accent = colorPalette().accent
    val tint = when {
        !enabled -> colorPalette().textDisabled
        active   -> accent
        else     -> colorPalette().text
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size((size + 12).dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (onLongClick != null)
                    Modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                else
                    Modifier.clickable(enabled = enabled, onClick = onClick)
            )
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .size(size.dp)
                .alpha(if (enabled) 1f else 0.4f)
        )
    }
}

@ExperimentalSerializationApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun ArtistOverviewNew(
    navController: NavController,
    browseId: String?,
    disableScrollingText: Boolean,
    onNavigateTo: () -> Unit
) {

    if (browseId == null) return

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

    val thumbnailRoundness by rememberPreference(THUMBNAIL_ROUNDNESS.key, ThumbnailRoundness.Light)

    val context = LocalContext.current

    var artist by persist<Artist?>("artist/$browseId/artist")
    var artistPage by persist<ArtistPage?>("artist/$browseId/artistPage")

    var itemsBrowseId by remember { mutableStateOf("") }
    var itemsParams by remember { mutableStateOf("") }
    var itemsSectionName by remember { mutableStateOf("") }
    var showArtistItems by rememberSaveable { mutableStateOf(false) }
    var songsBrowseId by remember { mutableStateOf("") }
    var songsParams by remember { mutableStateOf("") }

    var showArtistSongsInLibrary by rememberSaveable { mutableStateOf(false) }

    val hapticFeedback = LocalHapticFeedback.current
    val parentalControlEnabled by rememberPreference(PARENTAL_CONTROL_ENABLED.key, false)
    val menuState = LocalGlobalSheetState.current

    var readMore by remember { mutableStateOf(false) }
    var showFastShare by remember { mutableStateOf(false) }

    LoaderScreen(show = artistPage == null)

    LaunchedEffect(Unit) {
        Database.artist(browseId).distinctUntilChanged().collect { currentArtist ->
            artist = currentArtist

            if (artistPage == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    EnvironmentExt.getArtistPage(browseId = browseId)
                        .onSuccess { currentArtistPage ->
                            artistPage = currentArtistPage

                            Database.upsert(
                                Artist(
                                    id = browseId,
                                    name = currentArtistPage.artist.info?.name,
                                    thumbnailUrl = currentArtistPage.artist.thumbnail?.url,
                                    timestamp = System.currentTimeMillis(),
                                    bookmarkedAt = currentArtist?.bookmarkedAt,
                                    isYoutubeArtist = currentArtist?.isYoutubeArtist == true
                                )
                            )
                        }
                }
            }
        }
    }

    FastShare(
        showFastShare,
        onDismissRequest = { showFastShare = false },
        content = artist ?: return
    )

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
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
        ) {

            // ─── HERO SECTION (Image + Name) ────────────────────────────────
            item {
                val modifierArt = Modifier.fillMaxWidth()

                Box(modifier = modifierArt) {
                    if (!isLandscape)
                        Box {
                            AsyncImage(
                                model = artistPage?.artist?.thumbnail?.url?.toThumbnail(1200),
                                contentDescription = "loading...",
                                contentScale = ContentScale.Crop,
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
                                    painter = painterResource(R.drawable.internet),
                                    colorFilter = ColorFilter.tint(
                                        Color.Red.copy(0.75f).compositeOver(Color.White)
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(all = 5.dp)
                                        .offset(10.dp, 10.dp),
                                    contentDescription = "Background Image",
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                    AutoResizeText(
                        text = artistPage?.artist?.info?.name ?: "",
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
                                basicMarquee(iterations = Int.MAX_VALUE)
                            }
                    )

                    FastPlayActionsBar(
                        modifier = Modifier.fillMaxWidth(.5f).align(Alignment.BottomCenter).padding(bottom = 50.dp),
                        onPlayNowClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                artistPage?.sections?.firstOrNull { sec -> sec.items.firstOrNull() is Environment.SongItem }.let {
                                    songsBrowseId = it?.moreEndpoint?.browseId.toString()
                                    songsParams = it?.moreEndpoint?.params.toString()
                                }
                                if (songsBrowseId.isNotEmpty())
                                    EnvironmentExt.getArtistItemsPage(
                                        BrowseEndpoint(browseId = songsBrowseId, params = songsParams)
                                    ).completed().getOrNull()
                                        ?.items
                                        ?.map { it as Environment.SongItem }
                                        ?.map { it.asMediaItem }
                                        .let {
                                            if (it != null)
                                                withContext(Dispatchers.Main) {
                                                    binder?.player?.forcePlayFromBeginning(it)
                                                }
                                        }
                            }
                        },
                        onShufflePlayClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                artistPage?.sections?.firstOrNull { sec -> sec.items.firstOrNull() is Environment.SongItem }.let {
                                    songsBrowseId = it?.moreEndpoint?.browseId.toString()
                                    songsParams = it?.moreEndpoint?.params.toString()
                                }
                                if (songsBrowseId.isNotEmpty())
                                    EnvironmentExt.getArtistItemsPage(
                                        BrowseEndpoint(browseId = songsBrowseId, params = songsParams)
                                    ).completed().getOrNull()
                                        ?.items
                                        ?.map { it as Environment.SongItem }
                                        ?.map { it.asMediaItem }
                                        .let {
                                            if (it != null)
                                                withContext(Dispatchers.Main) {
                                                    binder?.player?.forcePlayFromBeginning(it.shuffled())
                                                }
                                        }
                            }
                        }
                    )
                }

                // ── SUBSCRIBERS LABEL (Stile QueueNew) ────────────────────────
                artistPage?.subscribers?.let {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        BasicText(
                            text = String.format(stringResource(R.string.artist_subscribers), it),
                            style = typography().xs.semiBold.copy(
                                color = colorPalette().textSecondary,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.W600
                            ),
                            maxLines = 1
                        )
                    }
                }

                // ── FLOATING CAPSULE: Follow + Azioni ────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colorPalette().background1)
                        .border(
                            width = 0.5.dp,
                            color = colorPalette().textDisabled.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (artist?.bookmarkedAt != null) colorPalette().accent.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .border(
                                width = 0.5.dp,
                                color = colorPalette().accent.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(50)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (isYtSyncEnabled() && !isNetworkConnected()) {
                                        SmartMessage(context.resources.getString(R.string.no_connection), context = context, type = PopupType.Error)
                                    } else {
                                        val bookmarkedAt = if (artist?.bookmarkedAt == null) System.currentTimeMillis() else null
                                        Database.asyncTransaction {
                                            artist?.copy(bookmarkedAt = bookmarkedAt)?.let(::update)
                                        }
                                        if (isYtSyncEnabled())
                                            CoroutineScope(Dispatchers.IO).launch {
                                                if (bookmarkedAt == null)
                                                    artistPage?.artist?.channelId.let {
                                                        if (it != null) {
                                                            EnvironmentExt.unsubscribeChannel(it)
                                                            artist?.let { Database.update(it.copy(isYoutubeArtist = false)) }
                                                        }
                                                    }
                                                else
                                                    artistPage?.artist?.channelId.let {
                                                        if (it != null) {
                                                            EnvironmentExt.subscribeChannel(it)
                                                            artist?.let { Database.update(it.copy(isYoutubeArtist = true)) }
                                                        }
                                                    }
                                            }
                                    }
                                },
                                onLongClick = null
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            text = if (artist?.bookmarkedAt == null) stringResource(R.string.follow) else stringResource(R.string.following),
                            style = typography().s.semiBold.copy(
                                color = if (artist?.bookmarkedAt != null) colorPalette().accent else colorPalette().text,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    artistPage?.shuffleEndpoint?.let { endpoint ->
                        ActionIconButton(
                            icon = R.drawable.shuffle,
                            size = 22,
                            onClick = { binder?.stopRadio(); binder?.playRadio(endpoint) },
                            onLongClick = { SmartMessage(context.resources.getString(R.string.info_shuffle), context = context) }
                        )
                    } ?: Spacer(Modifier.width(34.dp))

                    artistPage?.radioEndpoint?.let { endpoint ->
                        ActionIconButton(
                            icon = R.drawable.radio,
                            size = 22,
                            onClick = { binder?.stopRadio(); binder?.playRadio(endpoint) },
                            onLongClick = { SmartMessage(context.resources.getString(R.string.info_start_radio), context = context) }
                        )
                    } ?: Spacer(Modifier.width(34.dp))

                    ActionIconButton(
                        icon = R.drawable.share_social,
                        size = 22,
                        onClick = { showFastShare = true }
                    )
                }

                // ── GRADIENT DIVIDER (Stile QueueNew) ────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorPalette().accent.copy(alpha = 0.6f),
                                    colorPalette().accent.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // ─── INFORMATION SECTION ────────────────────────────────────────
            item {
                artistPage?.description?.let { description ->
                    val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                    BasicText(
                        text = stringResource(R.string.information).uppercase(),
                        style = typography().xs.semiBold.copy(
                            color = colorPalette().textSecondary,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.W600
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                        BasicText(
                            text = "\u201C",
                            style = typography().xxl.semiBold,
                            modifier = Modifier.offset(y = (-8).dp).align(Alignment.Top)
                        )

                        BasicText(
                            text = if (!readMore) {
                                description.substring(0, if (description.length >= 100) 100 else description.length).plus("...")
                            } else {
                                if (attributionsIndex == -1) description else description.substring(0, attributionsIndex)
                            },
                            style = typography().xxs.secondary.align(TextAlign.Justify),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .weight(1f)
                                .clickable { readMore = !readMore }
                        )

                        BasicText(
                            text = "\u201D",
                            style = typography().xxl.semiBold,
                            modifier = Modifier.offset(y = 4.dp).align(Alignment.Bottom)
                        )
                    }

                    if (attributionsIndex != -1) {
                        BasicText(
                            text = stringResource(R.string.from_wikipedia_cca),
                            style = typography().xxs.color(colorPalette().textDisabled).align(TextAlign.Start),
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // ─── LIBRARY SECTION ────────────────────────────────────────────
            item {
                BasicText(
                    text = stringResource(R.string.library).uppercase(),
                    style = typography().xs.semiBold.copy(
                        color = colorPalette().text,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.W600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showArtistSongsInLibrary = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ─── DYNAMIC SECTIONS ───────────────────────────────────────────
            artistPage?.sections?.forEach { section ->
                item {
                    if (section.items.firstOrNull() is Environment.SongItem) {
                        BasicText(
                            text = section.title,
                            style = typography().xs.semiBold.copy(
                                color = colorPalette().text,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.W600
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (section.moreEndpoint?.browseId != null)
                                        Modifier.clickable {
                                            itemsBrowseId = section.moreEndpoint?.browseId.toString()
                                            itemsParams = section.moreEndpoint?.params.toString()
                                            itemsSectionName = section.title
                                            showArtistItems = true
                                        }
                                    else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else {
                        Title2Actions(
                            title = section.title,
                            enableClick = section.moreEndpoint?.browseId != null,
                            onClick1 = {
                                if (section.moreEndpoint?.browseId != null) {
                                    itemsBrowseId = section.moreEndpoint?.browseId.toString()
                                    itemsParams = section.moreEndpoint?.params.toString()
                                    itemsSectionName = section.title
                                    showArtistItems = true
                                }
                            },
                            icon2 = R.drawable.dice,
                            onClick2 = {
                                if (section.items.isEmpty()) return@Title2Actions
                                val idItem = section.items.get(
                                    if (section.items.size > 1) Random(System.currentTimeMillis()).nextInt(0, section.items.size - 1) else 0
                                ).key
                                onNavigateTo()
                                navController.navigate(route = "${NavRoutes.album.name}/${idItem}")
                            }
                        )
                    }
                }

                if (section.items.firstOrNull() is Environment.SongItem) {
                    items(section.items) { item ->
                        when (item) {
                            is Environment.SongItem -> {
                                if (parentalControlEnabled && item.explicit) return@items
                                SwipeablePlaylistItem(
                                    mediaItem = item.asMediaItem,
                                    onPlayNext = { binder?.player?.addNext(item.asMediaItem, queue = selectedQueue ?: defaultQueue()) },
                                    onEnqueue = { binder?.player?.enqueue(item.asMediaItem, queue = selectedQueue ?: defaultQueue()) }
                                ) {
                                    SongItem(
                                        song = item,
                                        thumbnailSizePx = songThumbnailSizePx,
                                        thumbnailSizeDp = songThumbnailSizeDp,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            navController = navController,
                                                            onDismiss = { menuState.hide() },
                                                            mediaItem = item.asMediaItem,
                                                            onInfo = {
                                                                onNavigateTo()
                                                                navController.navigate("${NavRoutes.videoOrSongInfo.name}/${item.key}")
                                                            },
                                                            disableScrollingText = disableScrollingText,
                                                        )
                                                    }
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onClick = {
                                                    binder?.stopRadio()
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        artistPage?.sections?.firstOrNull { sec -> sec.items.firstOrNull() is Environment.SongItem }.let {
                                                            songsBrowseId = it?.moreEndpoint?.browseId.toString()
                                                            songsParams = it?.moreEndpoint?.params.toString()
                                                        }
                                                        if (songsBrowseId.isNotEmpty())
                                                            BrowseEndpoint(browseId = songsBrowseId, params = songsParams).let { endpoint ->
                                                                val artistSongs = EnvironmentExt.getArtistItemsPage(endpoint)
                                                                    .completed().getOrNull()?.items?.map { it as Environment.SongItem }?.map { it.asMediaItem }
                                                                val filteredArtistSongs = artistSongs?.filter { it.mediaId != Database.songDisliked(it.mediaId) }
                                                                withContext(Dispatchers.Main) {
                                                                    binder?.player?.forcePlay(item.asMediaItem)
                                                                    if (filteredArtistSongs != null) {
                                                                        binder?.player?.addMediaItems(filteredArtistSongs.filterNot { it.mediaId == item.key })
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                            )
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                } else {
                    item {
                        LazyRow(contentPadding = endPaddingValues) {
                            items(section.items) { item ->
                                when (item) {
                                    is Environment.SongItem -> {}
                                    is Environment.AlbumItem -> {
                                        var albumById by remember { mutableStateOf<Album?>(null) }
                                        LaunchedEffect(item) { CoroutineScope(Dispatchers.IO).launch { albumById = Database.album(item.key).firstOrNull() } }
                                        AlbumItem(
                                            album = item, alternative = true, thumbnailSizePx = albumThumbnailSizePx, thumbnailSizeDp = albumThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText, isYoutubeAlbum = albumById?.isYoutubeAlbum == true,
                                            modifier = Modifier.clickable { onNavigateTo(); navController.navigate("${NavRoutes.album.name}/${item.key}") }
                                        )
                                    }
                                    is Environment.ArtistItem -> {
                                        var artistById by remember { mutableStateOf<Artist?>(null) }
                                        LaunchedEffect(item) { CoroutineScope(Dispatchers.IO).launch { artistById = Database.artist(item.key).firstOrNull() } }
                                        ArtistItem(
                                            artist = item, thumbnailSizePx = artistThumbnailSizePx, thumbnailSizeDp = artistThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText, isYoutubeArtist = artistById?.isYoutubeArtist == true,
                                            modifier = Modifier.clickable { onNavigateTo(); navController.navigate("${NavRoutes.artist.name}/${item.key}") }
                                        )
                                    }
                                    is Environment.PlaylistItem -> {
                                        var playlistById by remember { mutableStateOf<Playlist?>(null) }
                                        LaunchedEffect(item) { CoroutineScope(Dispatchers.IO).launch { playlistById = Database.playlist(item.key.substringAfter("VL")).firstOrNull() } }
                                        PlaylistItem(
                                            playlist = item, alternative = true, thumbnailSizePx = playlistThumbnailSizePx, thumbnailSizeDp = playlistThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText, isYoutubePlaylist = playlistById?.isYoutubePlaylist == true,
                                            modifier = Modifier.clickable { onNavigateTo(); navController.navigate("${NavRoutes.playlist.name}/${item.key}") }
                                        )
                                    }
                                    is Environment.VideoItem -> {
                                        VideoItem(
                                            video = item, thumbnailHeightDp = playlistThumbnailSizeDp, thumbnailWidthDp = playlistThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }
        }

        val showFloatingIcon by rememberPreference(SHOW_FLOATING_ICON.key, false)
        if (UiType.ViMusic.isCurrent() && showFloatingIcon)
            artistPage?.radioEndpoint?.let { endpoint ->
                MultiFloatingActionsContainer(
                    iconId = R.drawable.radio,
                    onClick = { binder?.stopRadio(); binder?.playRadio(endpoint) },
                    onClickSettings = { onNavigateTo(); navController.navigate(NavRoutes.search.name) },
                    onClickSearch = { onNavigateTo(); navController.navigate(NavRoutes.settings.name) }
                )
            }

        CustomModalBottomSheet(
            showSheet = showArtistItems,
            onDismissRequest = { showArtistItems = false },
            containerColor = colorPalette().background2,
            contentColor = colorPalette().background2,
            modifier = Modifier.fillMaxWidth(),
            dragHandle = { Surface(modifier = Modifier.padding(vertical = 0.dp), color = colorPalette().background0, shape = thumbnailShape()) {} },
            shape = thumbnailRoundness.shape()
        ) {
            ArtistOverviewItems(
                navController, artistName = cleanPrefix(artist?.name ?: ""), sectionName = itemsSectionName,
                browseId = itemsBrowseId, params = itemsParams, disableScrollingText = false,
                onDismiss = { showArtistItems = false }
            )
        }

        CustomModalBottomSheet(
            showSheet = showArtistSongsInLibrary,
            onDismissRequest = { showArtistSongsInLibrary = false },
            containerColor = colorPalette().background2,
            contentColor = colorPalette().background2,
            modifier = Modifier.fillMaxWidth(),
            dragHandle = { Surface(modifier = Modifier.padding(vertical = 0.dp), color = colorPalette().background0, shape = thumbnailShape()) {} },
            shape = thumbnailRoundness.shape()
        ) {
            ArtistLibrarySongs(
                navController = navController, browseId = browseId, artistName = cleanPrefix(artist?.name ?: ""),
                onDismiss = { showArtistSongsInLibrary = false }
            )
        }
    }
}