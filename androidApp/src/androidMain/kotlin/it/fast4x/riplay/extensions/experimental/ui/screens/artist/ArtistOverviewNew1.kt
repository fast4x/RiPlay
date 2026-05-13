package it.fast4x.riplay.extensions.experimental.ui.screens.artist

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
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
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.requests.ArtistPage
import it.fast4x.environment.utils.completed
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.LocalSelectedQueue
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.defaultQueue
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.fastshare.FastShare
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showFloatingIconKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.PillIconButton
import it.fast4x.riplay.ui.components.SwipeablePlaylistItem
import it.fast4x.riplay.ui.components.themed.AutoResizeText
import it.fast4x.riplay.ui.components.themed.BoldSectionTitle
import it.fast4x.riplay.ui.components.themed.FastPlayActionsBar
import it.fast4x.riplay.ui.components.themed.FontSizeRange
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.screens.artist.ArtistLibrarySongs
import it.fast4x.riplay.ui.screens.artist.ArtistOverviewItems
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.addNext
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.enqueue
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.isNetworkConnected
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.random.Random



@ExperimentalSerializationApi
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ArtistOverviewNew1(
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
    val thumbnailRoundness by rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Light)
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
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val menuState = LocalGlobalSheetState.current

    var readMore by remember { mutableStateOf(false) }
    var showFastShare by remember { mutableStateOf(false) }

    // Scroll state per sticky header
    val lazyListState = rememberLazyListState()
    val showStickyName by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

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
                if (NavigationBarPosition.Right.isCurrent()) Dimensions.contentWidthRightBar else 1f
            )
    ) {

        // ── Sticky name header (appare dopo scroll) ──────────────────────────
        AnimatedVisibility(
            visible = showStickyName,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(10f)  // sopra tutto
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorPalette().background0,
                                colorPalette().background0.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(
                        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
                        bottom = 8.dp
                    )
            ) {
                BasicText(
                    text = artistPage?.artist?.info?.name ?: "",
                    style = typography().m.semiBold.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp)
                )
                // Share button rimane sempre visibile in alto a destra
                PillIconButton(
                    icon = R.drawable.share_social,
                    size = 20,
                    onClick = { showFastShare = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }
        }

        // ── MAIN LIST ────────────────────────────────────────────────────────
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {

            // ── ITEM 1: Hero + nome + azioni ──────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth()) {

                    // Hero image con gradiente drammatico
                    if (!isLandscape) {
                        Box {
                            AsyncImage(
                                model = artistPage?.artist?.thumbnail?.url?.toThumbnail(1200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                            )
                            // Gradiente bottom-heavy per far emergere il nome
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0f to Color.Transparent,
                                                0.45f to Color.Transparent,
                                                0.78f to colorPalette().background0.copy(alpha = 0.7f),
                                                1f to colorPalette().background0
                                            )
                                        )
                                    )
                            )
                            // Badge YouTube artista
                            if (artist?.isYoutubeArtist == true) {
                                Box(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.internet),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(
                                                Color.Red.copy(0.85f).compositeOver(Color.White)
                                            ),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        BasicText(
                                            text = "YouTube",
                                            style = typography().xxs.semiBold.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Share button in alto a destra (visibile prima dello sticky)
                    AnimatedVisibility(
                        visible = !showStickyName,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 4.dp,
                                end = 8.dp
                            )
                    ) {
                        PillIconButton(
                            icon = R.drawable.share_social,
                            size = 22,
                            onClick = { showFastShare = true },
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Nome artista in grande sopra il gradiente
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        AutoResizeText(
                            text = artistPage?.artist?.info?.name ?: "",
                            style = typography().l.semiBold.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = colorPalette().text
                            ),
                            fontSizeRange = FontSizeRange(34.sp, 48.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = typography().l.semiBold.fontFamily,
                            color = colorPalette().text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .applyIf(!disableScrollingText) {
                                    basicMarquee(iterations = Int.MAX_VALUE)
                                }
                        )

                        // FastPlay sotto il nome
                        FastPlayActionsBar(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .padding(top = 10.dp),
                            onPlayNowClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    artistPage?.sections?.firstOrNull { sec ->
                                        sec.items.firstOrNull() is Environment.SongItem
                                    }.let {
                                        songsBrowseId = it?.moreEndpoint?.browseId.toString()
                                        songsParams = it?.moreEndpoint?.params.toString()
                                    }
                                    if (songsBrowseId.isNotEmpty())
                                        EnvironmentExt.getArtistItemsPage(
                                            BrowseEndpoint(browseId = songsBrowseId, params = songsParams)
                                        ).completed().getOrNull()
                                            ?.items?.map { it as Environment.SongItem }
                                            ?.map { it.asMediaItem }
                                            .let {
                                                if (it != null) withContext(Dispatchers.Main) {
                                                    binder?.player?.forcePlayFromBeginning(it)
                                                }
                                            }
                                }
                            },
                            onShufflePlayClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    artistPage?.sections?.firstOrNull { sec ->
                                        sec.items.firstOrNull() is Environment.SongItem
                                    }.let {
                                        songsBrowseId = it?.moreEndpoint?.browseId.toString()
                                        songsParams = it?.moreEndpoint?.params.toString()
                                    }
                                    if (songsBrowseId.isNotEmpty())
                                        EnvironmentExt.getArtistItemsPage(
                                            BrowseEndpoint(browseId = songsBrowseId, params = songsParams)
                                        ).completed().getOrNull()
                                            ?.items?.map { it as Environment.SongItem }
                                            ?.map { it.asMediaItem }
                                            .let {
                                                if (it != null) withContext(Dispatchers.Main) {
                                                    binder?.player?.forcePlayFromBeginning(it.shuffled())
                                                }
                                            }
                                }
                            }
                        )
                    }
                }

                // ── Subscriber pill + azioni follow/shuffle/radio ──────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Subscriber badge pill
                    artistPage?.subscribers?.let { subs ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colorPalette().accent.copy(alpha = 0.12f))
                                .border(
                                    0.5.dp,
                                    colorPalette().accent.copy(alpha = 0.35f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                text = String.format(stringResource(R.string.artist_subscribers), subs),
                                style = typography().xs.semiBold.copy(
                                    color = colorPalette().accent,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Follow + shuffle + radio in capsula flottante
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colorPalette().background1)
                            .border(
                                0.5.dp,
                                colorPalette().textDisabled.copy(alpha = 0.2f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        // Follow pill
                        val isFollowing = artist?.bookmarkedAt != null
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isFollowing) colorPalette().accent.copy(alpha = 0.15f)
                                    else colorPalette().background2
                                )
                                .border(
                                    0.5.dp,
                                    if (isFollowing) colorPalette().accent.copy(alpha = 0.5f)
                                    else colorPalette().textDisabled.copy(alpha = 0.3f),
                                    RoundedCornerShape(50)
                                )
                                .clickable {
                                    if (isYtSyncEnabled() && !isNetworkConnected(context)) {
                                        SmartMessage(
                                            context.resources.getString(R.string.no_connection),
                                            context = context, type = PopupType.Error
                                        )
                                    } else {
                                        val bookmarkedAt =
                                            if (artist?.bookmarkedAt == null) System.currentTimeMillis() else null
                                        Database.asyncTransaction {
                                            artist?.copy(bookmarkedAt = bookmarkedAt)?.let(::update)
                                        }
                                        if (isYtSyncEnabled())
                                            CoroutineScope(Dispatchers.IO).launch {
                                                if (bookmarkedAt == null)
                                                    artistPage?.artist?.channelId?.let {
                                                        EnvironmentExt.unsubscribeChannel(it)
                                                        artist?.let { a -> Database.update(a.copy(isYoutubeArtist = false)) }
                                                    }
                                                else
                                                    artistPage?.artist?.channelId?.let {
                                                        EnvironmentExt.subscribeChannel(it)
                                                        artist?.let { a -> Database.update(a.copy(isYoutubeArtist = true)) }
                                                    }
                                            }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            BasicText(
                                text = if (isFollowing)
                                    stringResource(R.string.following)
                                else
                                    stringResource(R.string.follow),
                                style = typography().xs.semiBold.copy(
                                    color = if (isFollowing) colorPalette().accent else colorPalette().text,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Separatore verticale
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(20.dp)
                                .background(colorPalette().textDisabled.copy(alpha = 0.3f))
                        )

                        // Shuffle endpoint
                        artistPage?.shuffleEndpoint?.let { endpoint ->
                            PillIconButton(
                                icon = R.drawable.shuffle,
                                size = 20,
                                onClick = { binder?.stopRadio(); binder?.playRadio(endpoint) },
                                onLongClick = {
                                    SmartMessage(context.resources.getString(R.string.info_shuffle), context = context)
                                }
                            )
                        }

                        // Radio endpoint
                        artistPage?.radioEndpoint?.let { endpoint ->
                            PillIconButton(
                                icon = R.drawable.radio,
                                size = 20,
                                onClick = { binder?.stopRadio(); binder?.playRadio(endpoint) },
                                onLongClick = {
                                    SmartMessage(context.resources.getString(R.string.info_start_radio), context = context)
                                }
                            )
                        }
                    }
                }
            } // end hero item

            // ── ITEM 2: Description card ─────────────────────────────────
            item {
                artistPage?.description?.let { description ->
                    val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                    // Section title con accent bar
                    BoldSectionTitle(
                        title = stringResource(R.string.information),
                        trailingContent = {
                            PillIconButton(
                                icon = if (readMore) R.drawable.chevron_up else R.drawable.chevron_down,
                                size = 18,
                                onClick = { readMore = !readMore }
                            )
                        }
                    )

                    // Card con bordo accent sinistro
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorPalette().background1)
                            .border(
                                0.5.dp,
                                colorPalette().textDisabled.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        // Accent bar sinistra
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .align(Alignment.CenterStart)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            colorPalette().accent.copy(alpha = 0.4f),
                                            colorPalette().accent,
                                            colorPalette().accent.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                        ) {
                            // Quote mark
                            BasicText(
                                text = "\u201C",
                                style = typography().xxl.semiBold.copy(
                                    color = colorPalette().accent.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp
                                ),
                                modifier = Modifier.offset(y = (-4).dp)
                            )

                            val displayText = if (!readMore)
                                description.substring(0, minOf(100, description.length)).plus("…")
                            else if (attributionsIndex == -1) description
                            else description.substring(0, attributionsIndex)

                            BasicText(
                                text = displayText,
                                style = typography().xxs.secondary.align(TextAlign.Justify).copy(
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { readMore = !readMore }
                            )

                            if (readMore && attributionsIndex != -1) {
                                Spacer(Modifier.height(8.dp))
                                BasicText(
                                    text = stringResource(R.string.from_wikipedia_cca),
                                    style = typography().xxs.color(colorPalette().textDisabled)
                                        .align(TextAlign.Start)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // ── ITEM 3: Library ──────────────────────────────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colorPalette().textDisabled.copy(alpha = 0.15f)
                )
                BoldSectionTitle(
                    title = stringResource(R.string.library),
                    trailingContent = {
                        PillIconButton(
                            icon = R.drawable.chevron_down,
                            size = 18,
                            onClick = { showArtistSongsInLibrary = true }
                        )
                    },
                    modifier = Modifier.clickable { showArtistSongsInLibrary = true }
                )
            }

            // ── Sezioni artista (canzoni, album, ecc.) ───────────────────
            artistPage?.sections?.forEach { section ->
                item {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colorPalette().textDisabled.copy(alpha = 0.12f)
                    )

                    if (section.items.firstOrNull() is Environment.SongItem) {
                        BoldSectionTitle(
                            title = section.title,
                            trailingContent = if (section.moreEndpoint?.browseId != null) {
                                {
                                    PillIconButton(
                                        icon = R.drawable.chevron_down,
                                        size = 18,
                                        onClick = {
                                            itemsBrowseId = section.moreEndpoint?.browseId.toString()
                                            itemsParams = section.moreEndpoint?.params.toString()
                                            itemsSectionName = section.title
                                            showArtistItems = true
                                        }
                                    )
                                }
                            } else null,
                            modifier = if (section.moreEndpoint?.browseId != null)
                                Modifier.clickable {
                                    itemsBrowseId = section.moreEndpoint?.browseId.toString()
                                    itemsParams = section.moreEndpoint?.params.toString()
                                    itemsSectionName = section.title
                                    showArtistItems = true
                                }
                            else Modifier
                        )
                    } else {
                        BoldSectionTitle(
                            title = section.title,
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                    if (section.moreEndpoint?.browseId != null) {
                                        PillIconButton(
                                            icon = R.drawable.chevron_down,
                                            size = 18,
                                            onClick = {
                                                itemsBrowseId = section.moreEndpoint?.browseId.toString()
                                                itemsParams = section.moreEndpoint?.params.toString()
                                                itemsSectionName = section.title
                                                showArtistItems = true
                                            }
                                        )
                                    }
                                    PillIconButton(
                                        icon = R.drawable.dice,
                                        size = 18,
                                        onClick = {
                                            if (section.items.isEmpty()) return@PillIconButton
                                            val idItem = section.items[
                                                if (section.items.size > 1)
                                                    Random(System.currentTimeMillis()).nextInt(0, section.items.size - 1)
                                                else 0
                                            ].key
                                            onNavigateTo()
                                            navController.navigate("${NavRoutes.album.name}/$idItem")
                                        }
                                    )
                                }
                            }
                        )
                    }
                }

                // Song items
                if (section.items.firstOrNull() is Environment.SongItem) {
                    items(section.items) { item ->
                        when (item) {
                            is Environment.SongItem -> {
                                if (parentalControlEnabled && item.explicit) return@items
                                SwipeablePlaylistItem(
                                    mediaItem = item.asMediaItem,
                                    onPlayNext = {
                                        binder?.player?.addNext(item.asMediaItem, queue = selectedQueue ?: defaultQueue())
                                    },
                                    onEnqueue = {
                                        binder?.player?.enqueue(item.asMediaItem, queue = it)
                                    }
                                ) {
                                    SongItem(
                                        song = item,
                                        thumbnailSizePx = songThumbnailSizePx,
                                        thumbnailSizeDp = songThumbnailSizeDp,
                                        modifier = Modifier.combinedClickable(
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
                                                    artistPage?.sections?.firstOrNull { sec ->
                                                        sec.items.firstOrNull() is Environment.SongItem
                                                    }.let {
                                                        songsBrowseId = it?.moreEndpoint?.browseId.toString()
                                                        songsParams = it?.moreEndpoint?.params.toString()
                                                    }
                                                    if (songsBrowseId.isNotEmpty())
                                                        BrowseEndpoint(browseId = songsBrowseId, params = songsParams)
                                                            .let { endpoint ->
                                                                val artistSongs = EnvironmentExt.getArtistItemsPage(endpoint)
                                                                    .completed().getOrNull()
                                                                    ?.items?.map { it as Environment.SongItem }
                                                                    ?.map { it.asMediaItem }
                                                                val filteredArtistSongs = artistSongs
                                                                    ?.filter { it.mediaId != Database.songDisliked(it.mediaId) }
                                                                withContext(Dispatchers.Main) {
                                                                    binder?.player?.forcePlay(item.asMediaItem)
                                                                    if (filteredArtistSongs != null)
                                                                        binder?.player?.addMediaItems(
                                                                            filteredArtistSongs.filterNot { it.mediaId == item.key }
                                                                        )
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
                    // Album / Artist / Playlist / Video row orizzontale
                    item {
                        LazyRow(contentPadding = endPaddingValues) {
                            items(section.items) { item ->
                                when (item) {
                                    is Environment.AlbumItem -> {
                                        var albumById by remember { mutableStateOf<Album?>(null) }
                                        LaunchedEffect(item) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                albumById = Database.album(item.key).firstOrNull()
                                            }
                                        }
                                        AlbumItem(
                                            album = item,
                                            alternative = true,
                                            thumbnailSizePx = albumThumbnailSizePx,
                                            thumbnailSizeDp = albumThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText,
                                            isYoutubeAlbum = albumById?.isYoutubeAlbum == true,
                                            modifier = Modifier.clickable {
                                                onNavigateTo()
                                                navController.navigate("${NavRoutes.album.name}/${item.key}")
                                            }
                                        )
                                    }
                                    is Environment.ArtistItem -> {
                                        var artistById by remember { mutableStateOf<Artist?>(null) }
                                        LaunchedEffect(item) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                artistById = Database.artist(item.key).firstOrNull()
                                            }
                                        }
                                        ArtistItem(
                                            artist = item,
                                            thumbnailSizePx = artistThumbnailSizePx,
                                            thumbnailSizeDp = artistThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText,
                                            isYoutubeArtist = artistById?.isYoutubeArtist == true,
                                            modifier = Modifier.clickable {
                                                onNavigateTo()
                                                navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                            }
                                        )
                                    }
                                    is Environment.PlaylistItem -> {
                                        var playlistById by remember { mutableStateOf<Playlist?>(null) }
                                        LaunchedEffect(item) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                playlistById = Database.playlist(item.key.substringAfter("VL")).firstOrNull()
                                            }
                                        }
                                        PlaylistItem(
                                            playlist = item,
                                            alternative = true,
                                            thumbnailSizePx = playlistThumbnailSizePx,
                                            thumbnailSizeDp = playlistThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText,
                                            isYoutubePlaylist = playlistById?.isYoutubePlaylist == true,
                                            modifier = Modifier.clickable {
                                                onNavigateTo()
                                                navController.navigate("${NavRoutes.playlist.name}/${item.key}")
                                            }
                                        )
                                    }
                                    is Environment.VideoItem -> {
                                        VideoItem(
                                            video = item,
                                            thumbnailHeightDp = playlistThumbnailSizeDp,
                                            thumbnailWidthDp = playlistThumbnailSizeDp,
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
            }

        } // end LazyColumn

        // ── Floating action button (ViMusic) ─────────────────────────────────
        val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
        if (UiType.ViMusic.isCurrent() && showFloatingIcon)
            artistPage?.radioEndpoint?.let { endpoint ->
                MultiFloatingActionsContainer(
                    iconId = R.drawable.radio,
                    onClick = { binder?.stopRadio(); binder?.playRadio(endpoint) },
                    onClickSettings = {
                        onNavigateTo()
                        navController.navigate(NavRoutes.search.name)
                    },
                    onClickSearch = {
                        onNavigateTo()
                        navController.navigate(NavRoutes.settings.name)
                    }
                )
            }

        // ── Bottom sheets ────────────────────────────────────────────────────
        CustomModalBottomSheet(
            showSheet = showArtistItems,
            onDismissRequest = { showArtistItems = false },
            containerColor = colorPalette().background2,
            contentColor = colorPalette().background2,
            modifier = Modifier.fillMaxWidth(),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                    shape = thumbnailShape()
                ) {}
            },
            shape = thumbnailRoundness.shape()
        ) {
            ArtistOverviewItems(
                navController,
                artistName = cleanPrefix(artist?.name ?: ""),
                sectionName = itemsSectionName,
                browseId = itemsBrowseId,
                params = itemsParams,
                disableScrollingText = false,
                onDismiss = { showArtistItems = false }
            )
        }

        CustomModalBottomSheet(
            showSheet = showArtistSongsInLibrary,
            onDismissRequest = { showArtistSongsInLibrary = false },
            containerColor = colorPalette().background2,
            contentColor = colorPalette().background2,
            modifier = Modifier.fillMaxWidth(),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                    shape = thumbnailShape()
                ) {}
            },
            shape = thumbnailRoundness.shape()
        ) {
            ArtistLibrarySongs(
                navController = navController,
                browseId = browseId,
                artistName = cleanPrefix(artist?.name ?: ""),
                onDismiss = { showArtistSongsInLibrary = false }
            )
        }

    } // end root Box
} // end ArtistOverview