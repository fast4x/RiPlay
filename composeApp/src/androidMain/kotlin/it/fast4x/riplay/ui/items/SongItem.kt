package it.fast4x.riplay.ui.items

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.thumbnailShape
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.themed.AddToPlaylistPlayerMenu
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.TextPlaceholder
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.favoritesOverlay
import it.fast4x.riplay.ui.styling.shimmer
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.getLikeState
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isVideo
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.extensions.preferences.playlistindicatorKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.isNowPlaying
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.shimmerEffect
import it.fast4x.riplay.utils.thumbnail


@UnstableApi
@Composable
fun SongItem(
    song: Environment.SongItem,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    thumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    //disableScrollingText: Boolean,
    //isNowPlaying: Boolean = false,
    //isLocal: Boolean = false,
    //forceRecompose: Boolean = false
) {
    SongItem(
        thumbnailUrl = song.thumbnail?.size(thumbnailSizePx),
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        mediaItem = song.asMediaItem,
        onThumbnailContent = thumbnailContent,
        //disableScrollingText = disableScrollingText,
        //isNowPlaying = isNowPlaying,
        //isLocal = isLocal,
        //forceRecompose = forceRecompose
    )
}

@UnstableApi
@Composable
fun SongItem(
    song: MediaItem,
    thumbnailSizeDp: Dp,
    thumbnailSizePx: Int,
    modifier: Modifier = Modifier,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isRecommended: Boolean = false,
    //disableScrollingText: Boolean,
    //isNowPlaying: Boolean = false,
    //isLocal: Boolean = false,
    //forceRecompose: Boolean = false
) {
    SongItem(
        thumbnailUrl = song.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx)?.toString(),
        thumbnailSizeDp = thumbnailSizeDp,
        onThumbnailContent = onThumbnailContent,
        trailingContent = trailingContent,
        modifier = modifier,
        isRecommended = isRecommended,
        mediaItem = song,
        //disableScrollingText = disableScrollingText,
        //isNowPlaying = isNowPlaying,
        //isLocal = isLocal,
        //forceRecompose = forceRecompose
    )
}

@UnstableApi
@Composable
fun SongItem(
    song: Song,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    //disableScrollingText: Boolean,
    //isNowPlaying: Boolean = false,
    //isLocal: Boolean = false,
    //forceRecompose: Boolean = false
) {
    SongItem(
        thumbnailUrl = song.thumbnailUrl?.thumbnail(thumbnailSizePx),
        thumbnailSizeDp = thumbnailSizeDp,
        onThumbnailContent = onThumbnailContent,
        trailingContent = trailingContent,
        modifier = modifier,
        mediaItem = song.asMediaItem,
        //disableScrollingText = disableScrollingText,
        //isNowPlaying = isNowPlaying,
        //isLocal = isLocal,
        //forceRecompose = forceRecompose
    )
}

@UnstableApi
@Composable
fun SongItem(
    thumbnailUrl: String?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isRecommended: Boolean = false,
    mediaItem: MediaItem,
    //disableScrollingText: Boolean,
    //isNowPlaying: Boolean = false,
    //isLocal: Boolean = false,
    //forceRecompose: Boolean = false
) {
    val binder = LocalPlayerServiceBinder.current

    SongItem(
        thumbnailSizeDp = thumbnailSizeDp,
        thumbnailContent = {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(thumbnailShape())
                    .fillMaxSize()
            )

            onThumbnailContent?.invoke(this)

            NowPlayingSongIndicator(
                mediaId = mediaItem.mediaId,
                player = binder?.player
            )
        },
        modifier = modifier,
        trailingContent = trailingContent,
        isRecommended = isRecommended,
        //isLocal = mediaItem.isLocal,
        mediaItem = mediaItem,
        //disableScrollingText = disableScrollingText,
        //isNowPlaying = isNowPlaying,
        //forceRecompose = forceRecompose
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class,
    ExperimentalAnimationApi::class
)
@UnstableApi
@Composable
fun SongItem(
    thumbnailContent: @Composable BoxScope.() -> Unit,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    isRecommended: Boolean = false,
    //isLocal: Boolean,
    mediaItem: MediaItem,
    //disableScrollingText: Boolean,
    //isNowPlaying: Boolean = false,
    isLocalSong: Boolean = false,
    //forceRecompose: Boolean = false
) {

    val title = mediaItem.mediaMetadata.title.toString()
    val authors = mediaItem.mediaMetadata.artist.toString()
    val duration = mediaItem.mediaMetadata.extras?.getString("durationText")

    val playlistindicator by rememberPreference(playlistindicatorKey,false)
    var songPlaylist: State<Int> = remember {
        mutableIntStateOf(0)
    }
    val colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)

    if (playlistindicator)
        songPlaylist = Database.songUsedInPlaylistsAsFlow(mediaItem.mediaId).collectAsState(initial = 0)

    val binder = LocalPlayerServiceBinder.current
    val isNowPlaying = binder?.player?.isNowPlaying(mediaItem.mediaId)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val context = LocalContext.current
    val colorPalette = LocalAppearance.current.colorPalette

    ItemContainer(
        alternative = false,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .applyIf(isNowPlaying == true){
                background(colorPalette.favoritesOverlay)
            }

    ) {
        Box(
            modifier = Modifier
                .size(thumbnailSizeDp)
        ) {
            thumbnailContent()


            var likedAt by remember {
                mutableStateOf<Long?>(null)
            }
            LaunchedEffect(Unit, mediaItem.mediaId) {
                Database.likedAt(mediaItem.mediaId).collect { likedAt = it }
            }
            if (likedAt != null)
                HeaderIconButton(
                    onClick = {},
                    icon = getLikeState(mediaItem.mediaId),
                    color = colorPalette().favoritesIcon,
                    iconSize = 12.dp,
                    modifier = Modifier
                        //.padding(start = 4.dp)
                        .align(Alignment.BottomStart)
                        .absoluteOffset(-8.dp, 0.dp)

                )
            if (mediaItem.isVideo)
                HeaderIconButton(
                    onClick = {},
                    icon = R.drawable.video,
                    color = colorPalette().favoritesIcon,
                    iconSize = 12.dp,
                    modifier = Modifier
                        //.padding(start = 4.dp)
                        .align(Alignment.BottomEnd)
                        .absoluteOffset(8.dp, 0.dp)

                )
            /*
            if (totalPlayTimeMs != null) {
                if (totalPlayTimeMs <= 0 ) {
                    HeaderIconButton(
                        onClick = {},
                        icon = R.drawable.noteslashed,
                        color = colorPalette().favoritesIcon,
                        iconSize = 12.dp,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .align(Alignment.BottomStart)
                    )
                }
            }
             */

            /*
            BasicText(
                text = totalPlayTimeMs.toString() ?: "",
                style = typography().xs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(all = 16.dp)
            )
             */
        }

        ItemInfoContainer {
            trailingContent?.let {
                val menuState = LocalMenuState.current
                val navController = rememberNavController()
                val binder = LocalPlayerServiceBinder.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecommended)
                        IconButton(
                            icon = R.drawable.smart_shuffle,
                            color = colorPalette().accent,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier
                                .size(18.dp)
                        )

                    if (playlistindicator && (songPlaylist.value > 0)) {
                        IconButton(
                            icon = R.drawable.add_in_playlist,
                            color = if (colorPaletteName == ColorPaletteName.PureBlack) Color.Black else colorPalette().text,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier
                                .size(14.dp)
                                .background(colorPalette().accent, CircleShape)
                                .padding(all = 3.dp)
                                .combinedClickable(onClick = {
                                    menuState.display {
                                        if (binder != null) {
                                            AddToPlaylistPlayerMenu(
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.hide()
//                                                    Database.asyncTransaction {
//                                                        songPlaylist = songUsedInPlaylists(mediaItem.mediaId)
//                                                    }
                                                },
                                                mediaItem = mediaItem,
                                                binder = binder,
                                                onClosePlayer = {},
                                            )
                                        }
                                    }
                                }, onLongClick = {
                                    SmartMessage(
                                        context.resources.getString(R.string.playlistindicatorinfo2),
                                        context = context
                                    )
                                })
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                    }

                    if ( mediaItem.isExplicit )
                        IconButton(
                            icon = R.drawable.explicit,
                            color = colorPalette().text,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier
                                .size(18.dp)
                        )

                    BasicText(
                        text = cleanPrefix(title),
                        style = typography().xs.semiBold,
                        /*
                        style = TextStyle(
                            color = if (isRecommended) colorPalette().accent else colorPalette().text,
                            fontStyle = typography().xs.semiBold.fontStyle,
                            fontSize = typography().xs.semiBold.fontSize
                        ),
                         */
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )

                    /*
                    if (playlistindicator && (songPlaylist > 0)) {
                        IconButton(
                            icon = R.drawable.add_in_playlist,
                            color = colorPalette().text,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier
                                .size(18.dp)
                                .background(colorPalette().accent, CircleShape)
                                .padding(all = 3.dp)
                                .combinedClickable(onClick = {}, onLongClick = {
                                    SmartMessage(context.resources.getString(R.string.playlistindicatorinfo2), context = context)
                                })
                        )
                    }
                     */

                    it()
                }
            } ?: Row(verticalAlignment = Alignment.CenterVertically) {
                val menuState = LocalMenuState.current
                val navController = rememberNavController()
                val binder = LocalPlayerServiceBinder.current

                if (isRecommended)
                    IconButton(
                        icon = R.drawable.smart_shuffle,
                        color = colorPalette().accent,
                        enabled = true,
                        onClick = {},
                        modifier = Modifier
                            .size(18.dp)
                    )

                    if ( mediaItem.isExplicit )
                        IconButton(
                            icon = R.drawable.explicit,
                            color = colorPalette().text,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier
                                .size(18.dp)
                        )
                    BasicText(
                        text = cleanPrefix(title),
                        style = typography().xs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                            .weight(1f)
                    )
                if (playlistindicator && (songPlaylist.value > 0)) {
                    IconButton(
                        icon = R.drawable.add_in_playlist,
                        color = if (colorPaletteName == ColorPaletteName.PureBlack) Color.Black else colorPalette().text,
                        enabled = true,
                        onClick = {},
                        modifier = Modifier
                            .size(18.dp)
                            .background(colorPalette().accent, CircleShape)
                            .padding(all = 3.dp)
                            .combinedClickable(onClick = {
                                menuState.display {
                                    if (binder != null) {
                                        AddToPlaylistPlayerMenu(
                                            navController = navController,
                                            onDismiss = {
                                                menuState.hide()
//                                                Database.asyncTransaction {
//                                                    songPlaylist = songUsedInPlaylists(mediaItem.mediaId)
//                                                }
                                            },
                                            mediaItem = mediaItem,
                                            binder = binder,
                                            onClosePlayer = {},
                                        )
                                    }
                                }
                            }, onLongClick = {
                                SmartMessage(
                                    context.resources.getString(R.string.playlistindicatorinfo2),
                                    context = context
                                )
                            })
                    )
                }
            }


            Row(verticalAlignment = Alignment.CenterVertically) {

                BasicText(
                    text = authors,
                    style = typography().xs.semiBold.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f)
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                )

                duration?.let {
                    BasicText(
                        text = duration,
                        style = typography().xxs.secondary.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                if (mediaItem.isLocal)
                    IconButton(
                        onClick = {},
                        icon = R.drawable.folder,
                        color = colorPalette().text,
                        modifier = Modifier
                            .size(20.dp)
                    )

            }
        }
    }
}


@Composable
fun SongItemPlaceholder(
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier
) {
    ItemContainer(
        alternative = false,
        thumbnailSizeDp =thumbnailSizeDp,
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .background(color = colorPalette().shimmer, shape = thumbnailShape())
                .size(thumbnailSizeDp)
        )

        ItemInfoContainer {
            TextPlaceholder()
            TextPlaceholder()
        }
    }
}

/**
 * New component is more resemble to the final
 * SongItem that's currently being used.
 */
@Composable
fun SongItemPlaceholder( thumbnailSizeDp: Dp ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy( 12.dp ),
        modifier = Modifier.fillMaxWidth()
                           .padding(
                               vertical = 8.dp,
                               horizontal = 16.dp
                           )
    ) {
        Box(
            Modifier.size( thumbnailSizeDp )
                    .clip( RoundedCornerShape(12.dp) )
                    .shimmerEffect()
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth( .7f )
            ) {
                BasicText(
                    text = "",
                    style = typography().xs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight( 1f ).shimmerEffect()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box( Modifier.weight( 1f ).fillMaxWidth() ) {
                    BasicText(
                        text = "",
                        style = typography().xs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth( .3f ).shimmerEffect()
                    )
                }

                BasicText(
                    text = "0:00",
                    style = typography().xxs.secondary.medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding( top = 4.dp )
                )

                Spacer(modifier = Modifier.padding( horizontal = 4.dp ))

                IconButton(
                    onClick = {},
                    icon = R.drawable.download,
                    color = colorPalette().textDisabled,
                    modifier = Modifier.size( 20.dp ),
                    enabled = false
                )
            }
        }
    }
}