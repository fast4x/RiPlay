package it.fast4x.riplay.ui.screens.home.sections

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.HomePage
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.Countries
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PlayEventsType
import it.fast4x.riplay.extensions.listenerlevel.HomepageListenerLevelBadges
import it.fast4x.riplay.extensions.rewind.HomepageRewind
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.ui.components.GlobalSheetState
import it.fast4x.riplay.ui.components.themed.ChipItemColored
import it.fast4x.riplay.ui.components.themed.Loader
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.MoodItemColored
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.ArtistItem
import it.fast4x.riplay.ui.items.PlaylistItem
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.items.VideoItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.asVideoMediaItem
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import it.fast4x.riplay.utils.typography
import timber.log.Timber

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
@Composable
fun HomeSectionPart(
    navController: NavController,
    showListenerLevels: Boolean,
    showTips: Boolean,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    playlistThumbnailSizeDp: Dp,
    playlistThumbnailSizePx: Int,
    disableScrollingText: Boolean,
    endPaddingValues: PaddingValues,
    menuState: GlobalSheetState,
    onPlayEventTypeClick: (PlayEventsType) -> Unit,
    binder: PlayerService.Binder?,
    trending: Song?,
    relatedInit: Environment.RelatedPage?,
    discoverPageInit: Environment.DiscoverPage?,
    playEventType: PlayEventsType,
    quickPicksLazyGridState: LazyGridState,
    songThumbnailSizeDp: Dp,
    songThumbnailSizePx: Int,
    hapticFeedback: HapticFeedback,
    itemInHorizontalGridWidth: Dp,
    preferitesArtists: List<Artist>,
    showNewAlbumsArtists: Boolean,
    showNewAlbums: Boolean,
    sectionTextModifier: Modifier,
    albumThumbnailSizeDp: Dp,
    albumThumbnailSizePx: Int,
    showRelatedAlbums: Boolean,
    showSimilarArtists: Boolean,
    artistThumbnailSizeDp: Dp,
    artistThumbnailSizePx: Int,
    showPlaylistMightLike: Boolean,


    ) {

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {

        if (showListenerLevels)
            HomepageListenerLevelBadges(navController)

        HomepageRewind(
            showIfEndOfYear = true,
            navController = navController,
            playlistThumbnailSizeDp = playlistThumbnailSizeDp,
            endPaddingValues = endPaddingValues,
            disableScrollingText = disableScrollingText
        )

        if (showTips) {
            Title2Actions(
                title = stringResource(R.string.tips),
                onClick1 = {
                    menuState.display {
                        Menu {
                            MenuEntry(
                                icon = R.drawable.chevron_up,
                                text = stringResource(R.string.by_most_played_song),
                                onClick = {
                                    onPlayEventTypeClick(PlayEventsType.MostPlayed)
                                    //playEventType = PlayEventsType.MostPlayed
                                    menuState.hide()
                                }
                            )
                            MenuEntry(
                                icon = R.drawable.chevron_down,
                                text = stringResource(R.string.by_last_played_song),
                                onClick = {
                                    onPlayEventTypeClick(PlayEventsType.LastPlayed)
                                    //playEventType = PlayEventsType.LastPlayed
                                    menuState.hide()
                                }
                            )
                            MenuEntry(
                                icon = R.drawable.random,
                                text = stringResource(R.string.by_casual_played_song),
                                onClick = {
                                    onPlayEventTypeClick(PlayEventsType.CasualPlayed)
                                    //playEventType = PlayEventsType.CasualPlayed
                                    menuState.hide()
                                }
                            )
                        }
                    }
                },
                icon2 = R.drawable.play_now,
                onClick2 = {
                    //trending?.let { fastPlay(it.asMediaItem, binder, relatedInit?.songs?.map { it.asMediaItem }) }
                    binder?.stopRadio()
                    trending?.let { binder?.player?.forcePlay(it.asMediaItem) }
                    binder?.player?.addMediaItems(relatedInit?.songs?.map { it.asMediaItem }
                        ?: emptyList())
                }

                //modifier = Modifier.fillMaxWidth(0.7f)
            )

            BasicText(
                text = when (playEventType) {
                    PlayEventsType.MostPlayed -> stringResource(R.string.by_most_played_song)
                    PlayEventsType.LastPlayed -> stringResource(R.string.by_last_played_song)
                    PlayEventsType.CasualPlayed -> stringResource(R.string.by_casual_played_song)
                },
                style = typography().xxs.secondary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )




            LazyHorizontalGrid(
                state = quickPicksLazyGridState,
                rows = GridCells.Fixed(if (relatedInit != null) 3 else 1),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                contentPadding = endPaddingValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (relatedInit != null) Dimensions.itemsVerticalPadding * 3 * 9 else Dimensions.itemsVerticalPadding * 9)
                //.height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 4)
            ) {
                trending?.let { song ->
                    item {
                        //val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                        //var forceRecompose by remember { mutableStateOf(false) }
                        SongItem(
                            song = song,
                            thumbnailSizePx = songThumbnailSizePx,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            trailingContent = {
                                Image(
                                    painter = painterResource(R.drawable.star),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette().accent),
                                    modifier = Modifier
                                        .size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.hide()
                                                    //forceRecompose = true
                                                },
                                                mediaItem = song.asMediaItem,
                                                onRemoveFromQuickPicks = {
                                                    Database.asyncTransaction {
                                                        clearEventsFor(song.id)
                                                    }
                                                },
                                                onInfo = {
                                                    navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.id}")
                                                },
                                                disableScrollingText = disableScrollingText,
                                                onBlacklist = {
                                                    insertOrUpdateBlacklist(song)
                                                },
                                            )
                                        }
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    },
                                    onClick = {

                                        val mediaItem = if (song.isAudioOnly == 1)
                                            song.asMediaItem
                                        else
                                            song.asVideoMediaItem

                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                        //binder?.player?.playOnline(mediaItem)
                                        //fastPlay(mediaItem, binder)
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    }
                                )
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null
                                )
                                .width(itemInHorizontalGridWidth),

                            )
                    }
                }

                if (relatedInit != null) {
                    items(
                        items = relatedInit?.songs?.distinctBy { it.key }
                            ?.dropLast(if (trending == null) 0 else 1)
                            ?: emptyList(),
                        key = Environment.SongItem::key
                    ) { song ->
                        Timber.d("HomePage RELATED Environment.SongItem duration ${song.durationText}")
                        SongItem(
                            song = song,
                            thumbnailSizePx = songThumbnailSizePx,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null
                                )
                                .width(itemInHorizontalGridWidth)
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.hide()
                                                    //forceRecompose = true
                                                },
                                                mediaItem = song.asMediaItem,
                                                onInfo = {
                                                    navController.navigate("${NavRoutes.videoOrSongInfo.name}/${song.key}")
                                                },
                                                disableScrollingText = disableScrollingText,
                                                onBlacklist = {
                                                    insertOrUpdateBlacklist(song.asSong)
                                                },
                                            )
                                        }
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    },
                                    onClick = {
                                        println("HomePage Clicked on song")
                                        val mediaItem = if (song.isAudioOnly)
                                            song.asMediaItem
                                        else
                                            song.asVideoMediaItem

                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                        //fastPlay(mediaItem, binder)
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    }
                                ),

                            )
                    }
                }
            }

            if (relatedInit == null) Loader()

        }


        discoverPageInit?.let { page ->

            //var newReleaseAlbumsFiltered by persistList<Environment.AlbumItem>("discovery/newalbumsartist")
            var newReleaseAlbumsFiltered by remember { mutableStateOf(emptyList<Environment.AlbumItem>()) }
            page.newReleaseAlbums.forEach { album ->
                preferitesArtists.forEach { artist ->
                    if (artist.name == album.authors?.first()?.name) {
                        newReleaseAlbumsFiltered += album
                    }
                }
            }

            if (showNewAlbumsArtists)
                if (newReleaseAlbumsFiltered.isNotEmpty() && preferitesArtists.isNotEmpty()) {

                    BasicText(
                        text = stringResource(R.string.new_albums_of_your_artists),
                        style = typography().l.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = newReleaseAlbumsFiltered.distinctBy { it.key },
                            key = { it.key }) {
                            AlbumItem(
                                album = it,
                                thumbnailSizePx = albumThumbnailSizePx,
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                alternative = true,
                                modifier = Modifier.clickable(onClick = {
                                    onAlbumClick(it.key)
                                }),
                                disableScrollingText = disableScrollingText
                            )
                        }
                    }

                }

            if (showNewAlbums) {
                Title(
                    title = stringResource(R.string.new_albums),
                    onClick = { navController.navigate(NavRoutes.newAlbums.name) },
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = page.newReleaseAlbums.distinctBy { it.key },
                        key = { it.key }) {
                        AlbumItem(
                            album = it,
                            thumbnailSizePx = albumThumbnailSizePx,
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier.clickable(onClick = {
                                onAlbumClick(it.key)
                            }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }
            }
        }

        if (showRelatedAlbums)
            relatedInit?.albums?.let { albums ->
                BasicText(
                    text = stringResource(R.string.related_albums),
                    style = typography().l.semiBold,
                    modifier = sectionTextModifier
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = albums.distinctBy { it.key },
                        key = Environment.AlbumItem::key
                    ) { album ->
                        AlbumItem(
                            album = album,
                            thumbnailSizePx = albumThumbnailSizePx,
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier
                                .clickable(onClick = { onAlbumClick(album.key) }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }
            }

        if (showSimilarArtists)
            relatedInit?.artists?.let { artists ->
                BasicText(
                    text = stringResource(R.string.similar_artists),
                    style = typography().l.semiBold,
                    modifier = sectionTextModifier
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = artists.distinctBy { it.key },
                        key = Environment.ArtistItem::key,
                    ) { artist ->
                        ArtistItem(
                            artist = artist,
                            thumbnailSizePx = artistThumbnailSizePx,
                            thumbnailSizeDp = artistThumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier
                                .clickable(onClick = { onArtistClick(artist.key) }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }
            }

        if (showPlaylistMightLike)
            relatedInit?.playlists?.let { playlists ->
                BasicText(
                    text = stringResource(R.string.playlists_you_might_like),
                    style = typography().l.semiBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp, bottom = 8.dp)
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(
                        items = playlists.distinctBy { it.key },
                        key = Environment.PlaylistItem::key,
                    ) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            thumbnailSizePx = playlistThumbnailSizePx,
                            thumbnailSizeDp = playlistThumbnailSizeDp,
                            alternative = true,
                            showSongsCount = false,
                            modifier = Modifier
                                .clickable(onClick = { onPlaylistClick(playlist.key) }),
                            disableScrollingText = disableScrollingText
                        )
                    }
                }
            }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MoodAndGenresPart(
    homePageInit: HomePage?,
    chipsLazyGridState: LazyGridState,
    endPaddingValues: PaddingValues,
    onChipClick: (Environment.Chip) -> Unit,
    showMoodsAndGenres: Boolean,
    discoverPageInit: Environment.DiscoverPage?,
    navController: NavController,
    moodAndGenresLazyGridState: LazyGridState,
    onMoodAndGenresClick: (Environment.Mood.Item) -> Unit,
    playlistThumbnailSizeDp: Dp,
    playlistThumbnailSizePx: Int,
    disableScrollingText: Boolean,
    showMonthlyPlaylistInQuickPicks: Boolean,
    localMonthlyPlaylists: List<PlaylistPreview>,
    moodAngGenresLazyGridState: LazyGridState,
    showCharts: Boolean,
    chartsPageInit: Environment.ChartsPage?,
    selectedCountryCode: Countries,
    menuState: GlobalSheetState,
    onSelectCountryCode: (Countries) -> Unit,
    onPlaylistClick: (String) -> Unit,
    chartsPageSongLazyGridState: LazyGridState,
    parentalControlEnabled: Boolean,
    songThumbnailSizeDp: Dp,
    songThumbnailSizePx: Int,
    binder: PlayerService.Binder?,
    itemWidth: Dp,
    chartsPageArtistLazyGridState: LazyGridState,
    onArtistClick: (String) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        homePageInit?.let { page ->

            if (page.chips?.isNotEmpty() == true) {
                Title(
                    title = stringResource(R.string.mood),
                    //onClick = { navController.navigate(NavRoutes.moodsPage.name) },
                    //modifier = Modifier.fillMaxWidth(0.7f)
                )

                LazyHorizontalGrid(
                    state = chipsLazyGridState,
                    rows = GridCells.Fixed(4),
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    //flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        //.height((thumbnailSizeDp + Dimensions.itemsVerticalPadding * 8) * 8)
                        .height(Dimensions.itemsVerticalPadding * 4 * 8)
                ) {
                    items(
                        items = homePageInit?.chips?.sortedBy { it.title } ?: emptyList(),
                        key = { it.endpoint?.params!! }
                    ) {
                        ChipItemColored(
                            chip = it,
                            onClick = { it.endpoint?.browseId?.let { _ -> onChipClick(it) } },
                            modifier = Modifier
                                //.width(itemWidth)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }


        if (showMoodsAndGenres)
            discoverPageInit?.let { page ->

                if (page.moods.isNotEmpty()) {

                    Title(
                        title = stringResource(R.string.moods_and_genres),
                        onClick = { navController.navigate(NavRoutes.moodsPage.name) },
                        //modifier = Modifier.fillMaxWidth(0.7f)
                    )

                    LazyHorizontalGrid(
                        state = moodAngGenresLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        //flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                        contentPadding = endPaddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            //.height((thumbnailSizeDp + Dimensions.itemsVerticalPadding * 8) * 8)
                            .height(Dimensions.itemsVerticalPadding * 4 * 8)
                    ) {
                        items(
                            items = page.moods.sortedBy { it.title },
                            key = { it.endpoint.params ?: it.title }
                        ) {
                            MoodItemColored(
                                mood = it,
                                onClick = {
                                    it.endpoint.browseId?.let { _ ->
                                        onMoodAndGenresClick(
                                            it
                                        )
                                    }
                                },
                                modifier = Modifier
                                    //.width(itemWidth)
                                    .padding(4.dp)
                            )
                        }
                    }

                }
            }

        HomepageRewind(
            navController = navController,
            playlistThumbnailSizeDp = playlistThumbnailSizeDp,
            endPaddingValues = endPaddingValues,
            disableScrollingText = disableScrollingText
        )

        if (showMonthlyPlaylistInQuickPicks)
            localMonthlyPlaylists.let { playlists ->
                if (playlists.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.monthly_playlists),
                        style = typography().l.semiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 8.dp)
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = playlists.distinctBy { it.playlist.id },
                            key = { it.playlist.id }
                        ) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                thumbnailSizeDp = playlistThumbnailSizeDp,
                                thumbnailSizePx = playlistThumbnailSizePx,
                                alternative = true,
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null
                                    )
                                    .fillMaxSize()
                                    .clickable(onClick = { navController.navigate(route = "${NavRoutes.localPlaylist.name}/${playlist.playlist.id}") }),
                                disableScrollingText = disableScrollingText,
                                isYoutubePlaylist = playlist.playlist.isYoutubePlaylist,
                                isEditable = playlist.playlist.isEditable
                            )
                        }
                    }
                }
            }


        if (showCharts) {

            chartsPageInit?.let { page ->

                Title(
                    title = "${stringResource(R.string.charts)} (${selectedCountryCode.countryName})",
                    onClick = {
                        menuState.display {
                            Menu {
                                Countries.entries.forEach { country ->
                                    MenuEntry(
                                        icon = R.drawable.arrow_right,
                                        text = country.countryName,
                                        onClick = {
                                            onSelectCountryCode(selectedCountryCode)
                                            //selectedCountryCode = country
                                            menuState.hide()
                                        }
                                    )
                                }
                            }
                        }
                    },
                )

                page.playlists?.let { playlists ->
                    /*
                       BasicText(
                           text = stringResource(R.string.playlists),
                           style = typography().l.semiBold,
                           modifier = Modifier
                               .padding(horizontal = 16.dp)
                               .padding(top = 24.dp, bottom = 8.dp)
                       )
                         */

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = playlists.distinctBy { it.key },
                            key = Environment.PlaylistItem::key,
                        ) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                thumbnailSizePx = playlistThumbnailSizePx,
                                thumbnailSizeDp = playlistThumbnailSizeDp,
                                alternative = true,
                                showSongsCount = false,
                                modifier = Modifier
                                    .clickable(onClick = { onPlaylistClick(playlist.key) }),
                                disableScrollingText = disableScrollingText
                            )
                        }
                    }
                }

                page.songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        BasicText(
                            text = stringResource(R.string.chart_top_songs),
                            style = typography().l.semiBold,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 24.dp, bottom = 8.dp)
                        )


                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(2),
                            modifier = Modifier
                                .height(130.dp)
                                .fillMaxWidth(),
                            state = chartsPageSongLazyGridState,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                        ) {
                            itemsIndexed(
                                items = if (parentalControlEnabled)
                                    songs.filter {
                                        !it.asSong.title.startsWith(
                                            EXPLICIT_PREFIX
                                        )
                                    }.distinctBy { it.key }
                                else songs.distinctBy { it.key },
                                key = { _, song -> song.key }
                            ) { index, song ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    BasicText(
                                        text = "${index + 1}",
                                        style = typography().l.bold.center.color(
                                            colorPalette().text
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    SongItem(
                                        song = song,
                                        thumbnailSizePx = songThumbnailSizePx,
                                        thumbnailSizeDp = songThumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                val mediaItem = song.asMediaItem
                                                binder?.stopRadio()
                                                binder?.player?.forcePlay(mediaItem)
                                                //fastPlay(mediaItem, binder)
                                                binder?.player?.addMediaItems(songs.map { it.asMediaItem })
                                            })
                                            .width(itemWidth),
                                        //disableScrollingText = disableScrollingText,
                                        //isNowPlaying = binder?.player?.isNowPlaying(song.key) ?: false
                                    )
                                }
                            }
                        }
                    }
                }

                page.artists?.let { artists ->
                    if (artists.isNotEmpty()) {
                        BasicText(
                            text = stringResource(R.string.chart_top_artists),
                            style = typography().l.semiBold,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 24.dp, bottom = 8.dp)
                        )


                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(2),
                            modifier = Modifier
                                .height(130.dp)
                                .fillMaxWidth(),
                            state = chartsPageArtistLazyGridState,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                        ) {
                            itemsIndexed(
                                items = artists.distinctBy { it.key },
                                key = { _, artist -> artist.key }
                            ) { index, artist ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    BasicText(
                                        text = "${index + 1}",
                                        style = typography().l.bold.center.color(
                                            colorPalette().text
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    ArtistItem(
                                        artist = artist,
                                        thumbnailSizePx = songThumbnailSizePx,
                                        thumbnailSizeDp = songThumbnailSizeDp,
                                        alternative = false,
                                        modifier = Modifier
                                            .width(200.dp)
                                            .clickable(onClick = { onArtistClick(artist.key) }),
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ForYouPart(
    homePageInit: HomePage?,
    endPaddingValues: PaddingValues,
    disableScrollingText: Boolean,
    navController: NavController,
    albumThumbnailSizeDp: Dp,
    albumThumbnailSizePx: Int,
    binder: PlayerService.Binder?,
    artistThumbnailSizeDp: Dp,
    artistThumbnailSizePx: Int,
    playlistThumbnailSizeDp: Dp,
    playlistThumbnailSizePx: Int,
    relatedPageResult: Result<Environment.RelatedPage?>?
) {

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        homePageInit?.let { page ->

            page.sections.forEach {
                if (it.items.isEmpty() || it.items.firstOrNull()?.key == null) return@forEach

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

//                ?:
//                if (!isYouTubeLoggedIn())
//                    BasicText(
//                        text = "Log in to your YTM account for more content",
//                        style = typography().xs.center,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier
//                            .padding(vertical = 32.dp)
//                            .fillMaxWidth()
//                            .clickable {
//                                navController.navigate(NavRoutes.settings.name)
//                            }
//                    )
//                else
//                {
//                    ShimmerHost {
//                        repeat(3) {
//                            SongItemPlaceholder(
//                                thumbnailSizeDp = songThumbnailSizeDp,
//                            )
//                        }
//
//                        TextPlaceholder(modifier = sectionTextModifier)
//
//                        Row {
//                            repeat(2) {
//                                AlbumItemPlaceholder(
//                                    thumbnailSizeDp = albumThumbnailSizeDp,
//                                    alternative = true
//                                )
//                            }
//                        }
//
//                        TextPlaceholder(modifier = sectionTextModifier)
//
//                        Row {
//                            repeat(2) {
//                                PlaylistItemPlaceholder(
//                                    thumbnailSizeDp = albumThumbnailSizeDp,
//                                    alternative = true
//                                )
//                            }
//                        }
//                    }
//                }


        //} ?:

        relatedPageResult?.exceptionOrNull()?.let {
            BasicText(
                text = stringResource(R.string.page_not_been_loaded),
                style = typography().s.secondary.center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(all = 16.dp)
            )
        }

        /*
            if (related == null)
                ShimmerHost {
                    repeat(3) {
                        SongItemPlaceholder(
                            thumbnailSizeDp = songThumbnailSizeDp,
                        )
                    }

                    TextPlaceholder(modifier = sectionTextModifier)

                    Row {
                        repeat(2) {
                            AlbumItemPlaceholder(
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                alternative = true
                            )
                        }
                    }

                    TextPlaceholder(modifier = sectionTextModifier)

                    Row {
                        repeat(2) {
                            ArtistItemPlaceholder(
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                alternative = true
                            )
                        }
                    }

                    TextPlaceholder(modifier = sectionTextModifier)

                    Row {
                        repeat(2) {
                            PlaylistItemPlaceholder(
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                alternative = true
                            )
                        }
                    }
                }
             */
    }
}