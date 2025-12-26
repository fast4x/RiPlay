package it.fast4x.riplay.ui.screens.moodandchip

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.valentinilk.shimmer.shimmer
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.HomePage
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.Chip
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.ShimmerHost
import it.fast4x.riplay.ui.components.themed.HeaderPlaceholder
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.TextPlaceholder
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlay
import timber.log.Timber

@OptIn(UnstableApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ChipList(
    navController: NavController,
    chip: Chip
) {
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val binder = LocalPlayerServiceBinder.current
    val browseId = chip.browseId ?: "FEmusic_home"
    var chipPage by persist<Result<HomePage>>("playlist/$browseId${chip.params?.let { "/$it" } ?: ""}")

    LoaderScreen(show = chipPage == null)

    LaunchedEffect(Unit) {
        chipPage =
            EnvironmentExt.getHomePage(params = chip.params) //Environment.browse(BrowseBodyWithLocale(browseId = browseId, params = mood.params))
        Timber.d("MoodList chipPage $chipPage")
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    Column (
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
        chipPage?.getOrNull()?.let { moodResult ->
            LazyListContainer(
                state = lazyListState,
            ) {
                LazyColumn(
                    state = lazyListState,
                    //contentPadding = LocalPlayerAwareWindowInsets.current
                    //    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                    modifier = Modifier
                        .background(colorPalette().background0)
                        .fillMaxSize()
                ) {
                    item(
                        key = "header",
                        contentType = 0
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HeaderWithIcon(
                                title = chip.name,
                                iconId = R.drawable.internet,
                                enabled = true,
                                showIcon = true,
                                modifier = Modifier,
                                onClick = {}
                            )
                        }
                    }

                    moodResult.sections.forEach {
                        if (it.items.isEmpty() || it.items.firstOrNull()?.key == null) return@forEach
                        item {
                            TitleMiniSection(
                                it.label ?: "", modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 14.dp, bottom = 4.dp)
                            )

                            BasicText(
                                text = it.title,
                                style = typography().l.semiBold.color(colorPalette().text),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(vertical = 4.dp)
                            )
                        }
                        item {
                            LazyRow(contentPadding = endPaddingValues) {
                                items(it.items) { item ->
                                    when (item) {
                                        is Environment.SongItem -> {
                                            println("Innertube homePage SongItem: ${item.info?.name}")
                                            SongItem(
                                                song = item,
                                                thumbnailSizePx = albumThumbnailSizePx,
                                                thumbnailSizeDp = albumThumbnailSizeDp,
                                                //disableScrollingText = disableScrollingText,
                                                //isNowPlaying = false,
                                                modifier = Modifier.clickable(onClick = {
                                                    binder?.player?.forcePlay(item.asMediaItem)
                                                    //fastPlay(item.asMediaItem, binder)
                                                })
                                            )
                                        }

                                        is Environment.AlbumItem -> {
                                            println("Innertube homePage AlbumItem: ${item.info?.name}")
                                            AlbumItem(
                                                album = item,
                                                alternative = true,
                                                thumbnailSizePx = albumThumbnailSizePx,
                                                thumbnailSizeDp = albumThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    navController.navigate("${NavRoutes.album.name}/${item.key}")
                                                })

                                            )
                                        }

                                        is Environment.ArtistItem -> {
                                            println("Innertube homePage ArtistItem: ${item.info?.name}")
                                            ArtistItem(
                                                artist = item,
                                                thumbnailSizePx = artistThumbnailSizePx,
                                                thumbnailSizeDp = artistThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    navController.navigate("${NavRoutes.artist.name}/${item.key}")
                                                })
                                            )
                                        }

                                        is Environment.PlaylistItem -> {
                                            println("Innertube homePage PlaylistItem: ${item.info?.name}")
                                            PlaylistItem(
                                                playlist = item,
                                                alternative = true,
                                                thumbnailSizePx = playlistThumbnailSizePx,
                                                thumbnailSizeDp = playlistThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    navController.navigate("${NavRoutes.playlist.name}/${item.key}")
                                                })
                                            )
                                        }

                                        is Environment.VideoItem -> {
                                            println("Innertube homePage VideoItem: ${item.info?.name}")
                                            VideoItem(
                                                video = item,
                                                thumbnailHeightDp = playlistThumbnailSizeDp,
                                                thumbnailWidthDp = playlistThumbnailSizeDp,
                                                disableScrollingText = disableScrollingText,
                                                modifier = Modifier.clickable(onClick = {
                                                    binder?.stopRadio()
//                                                if (isVideoEnabled())
//                                                    binder?.player?.playOnline(item.asMediaItem)
//                                                else
                                                    binder?.player?.forcePlay(item.asMediaItem)
                                                    //fastPlay(item.asMediaItem, binder)
                                                })
                                            )
                                        }

                                        null -> {}
                                    }

                                }
                            }
                        }
                    }

                    item(key = "bottom") {
                        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                    }

                }
            }
        } ?: chipPage?.exceptionOrNull()?.let {
            BasicText(
                text = stringResource(R.string.page_not_been_loaded),
                style = typography().s.secondary.center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(all = 16.dp)
            )
        } ?: ShimmerHost {
            HeaderPlaceholder(modifier = Modifier.shimmer())
            repeat(4) {
                TextPlaceholder(modifier = sectionTextModifier)
                Row {
                    repeat(6) {
                        AlbumItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }
            }
        }
    }
}
