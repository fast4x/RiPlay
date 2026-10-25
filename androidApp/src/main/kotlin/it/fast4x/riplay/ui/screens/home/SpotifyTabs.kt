package it.fast4x.riplay.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi
import kotlinx.serialization.ExperimentalSerializationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.requests.discoverPage
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.data.models.toUiMood
import it.fast4x.riplay.enums.BuiltInPlaylist
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.Encore
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.launch
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.ui.components.tab.ImportSongsFromSpotifyCSV
import it.fast4x.riplay.extensions.spotifyimport.SpotifyMatcher
import it.fast4x.riplay.ui.components.tab.ImportPlaylistFromSpotifyUrlDialog

/* =================================================================================================
 *  BUSCAR
 * ================================================================================================= */

@Composable
fun SpotifySearchTab(navController: NavController) {
    val appSettings = LocalAppSettingsManager.current.activeSettings.collectAsStateWithLifecycle().value

    var discoverPage by persist<Result<Environment.DiscoverPage>>("home/discoveryMoods")
    LaunchedEffect(Unit) {
        if (discoverPage?.isSuccess != true)
            discoverPage = Environment.discoverPage(appSettings.languageApp.code, appSettings.contentCountry.code)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (LocalDensity.current.fontScale > 1.3f) 180.dp else 140.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BasicText(
                text = stringResource(R.string.mobile_home_search),
                style = typography().xxl.bold.copy(color = colorPalette().text),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }

        // Barra blanca "What do you want to listen to?"
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .mobileHomeClickable(onClick = { navController.navigate(NavRoutes.search.name) })
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF121212)),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                BasicText(
                    text = stringResource(R.string.mobile_home_search_prompt),
                    style = typography().s.semiBold.copy(color = Color(0xFF535353)),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            BasicText(
                text = stringResource(R.string.mobile_home_browse_all),
                style = typography().m.bold.copy(color = colorPalette().text),
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
            )
        }

        val moods = discoverPage?.getOrNull()?.moods.orEmpty()
        items(moods, key = { it.endpoint.params ?: it.title }) { mood ->
            GenreCard(
                title = mood.title,
                color = Color(mood.stripeColor),
                onClick = {
                    mood.endpoint.browseId?.let {
                        navController.currentBackStackEntry?.savedStateHandle?.set("mood", mood.toUiMood())
                        navController.navigate(NavRoutes.mood.name)
                    }
                }
            )
        }

    }
}

@Composable
private fun GenreCard(title: String, color: Color, onClick: () -> Unit) {
    // Colores de YouTube Music suelen ser apagados: se aclaran un poco para parecer "vivos".
    val bg = Color(
        red = (color.red * 0.8f + 0.12f).coerceAtMost(1f),
        green = (color.green * 0.8f + 0.12f).coerceAtMost(1f),
        blue = (color.blue * 0.8f + 0.12f).coerceAtMost(1f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .mobileHomeClickable(onClick = onClick)
            .padding(12.dp)
    ) {
        BasicText(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = typography().s.bold.copy(color = Color.White),
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

/* =================================================================================================
 *  TU BIBLIOTECA
 * ================================================================================================= */

private enum class LibraryFilter(val label: Int) {
    Playlists(R.string.mobile_home_playlists), Artists(R.string.mobile_home_artists),
    Albums(R.string.mobile_home_albums), Songs(R.string.mobile_home_songs)
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalTextApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalSerializationApi::class,
    UnstableApi::class
)
@Composable
fun SpotifyLibraryTab(navController: NavController) {
    val appSettingsManager = LocalAppSettingsManager.current
    val coroutineScope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableLibraryFilter() }
    var showLibraryActions by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showUrlImport by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (showUrlImport) ImportPlaylistFromSpotifyUrlDialog(onDismiss = { showUrlImport = false })

    val spotifyImport = rememberSpotifyImport()
    val matchProgress by SpotifyMatcher.progress.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { SpotifyMatcher.request() } // fixes songs left unmatched earlier
    val matchLabel = when {
        matchProgress.running -> stringResource(R.string.mobile_home_matching, matchProgress.done, matchProgress.total)
        matchProgress.failed > 0 -> stringResource(R.string.mobile_home_unmatched, matchProgress.failed)
        else -> stringResource(R.string.mobile_home_csv_hint)
    }
    val openLikedSongs: () -> Unit = {
        coroutineScope.launch {
            appSettingsManager.updateSettings(
                appSettingsManager.activeSettings.value.copy(builtInPlaylist = BuiltInPlaylist.Favorites)
            )
        }
        filter = LibraryFilter.Songs.ordinal
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
    ) {
        val compact = maxHeight < 360.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.mobile_home_library),
                    style = typography().xxl.bold.copy(color = colorPalette().text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (compact) LibraryIcon(R.drawable.heart, stringResource(R.string.mobile_home_liked_songs), openLikedSongs)
                LibraryIcon(R.drawable.add, stringResource(R.string.mobile_home_import_music)) { showLibraryActions = true }
            }

            // Píldoras de filtro
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LibraryFilter.entries.forEach { f ->
                    val selected = f.ordinal == filter
                    MobileFilterChip(stringResource(f.label), selected) { filter = f.ordinal }
                }
            }

            // "Canciones que te gustan" fijada arriba
            if (!compact) Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .mobileHomeClickable(onClick = openLikedSongs)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.linearGradient(listOf(Encore.LikedSongsTop, Encore.LikedSongsBottom)))
                ) {
                    Image(
                        painter = painterResource(R.drawable.heart),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = stringResource(R.string.mobile_home_liked_songs),
                        style = typography().s.semiBold.copy(color = colorPalette().text)
                    )
                    BasicText(
                        text = stringResource(R.string.mobile_home_playlist),
                        style = typography().xs.copy(color = colorPalette().textSecondary)
                    )
                }
            }

            Box(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalEmbeddedLibrary provides true) {
                    when (LibraryFilter.entries[filter]) {
                        LibraryFilter.Playlists -> HomePlaylists(
                            navController = navController,
                            onPlaylistClick = {
                                if (!it.isOnDevice)
                                    navController.navigate(route = "${NavRoutes.localPlaylist.name}/${it.playlist.id}")
                                else
                                    navController.navigate(route = "${NavRoutes.onDevicePlaylist.name}/${it.folder?.replace("/", "$")}")
                            },
                            onSearchClick = { navController.navigate(NavRoutes.search.name) },
                            onSettingsClick = { navController.navigate(NavRoutes.settings.name) }
                        )

                        LibraryFilter.Artists -> HomeArtists(
                            navController = navController,
                            onArtistClick = {
                                if (!it.id.startsWith(LOCAL_KEY_PREFIX))
                                    navController.navigate(route = "${NavRoutes.artist.name}/${it.id}")
                                else navController.navigate(route = "${NavRoutes.onDeviceArtist.name}/${it.id}")
                            },
                            onSearchClick = { navController.navigate(NavRoutes.search.name) },
                            onSettingsClick = { navController.navigate(NavRoutes.settings.name) }
                        )

                        LibraryFilter.Albums -> HomeAlbums(
                            navController = navController,
                            onAlbumClick = {
                                if (!it.id.startsWith(LOCAL_KEY_PREFIX))
                                    navController.navigate(route = "${NavRoutes.album.name}/${it.id}")
                                else navController.navigate(route = "${NavRoutes.onDeviceAlbum.name}/${it.id}")
                            },
                            onSearchClick = { navController.navigate(NavRoutes.search.name) },
                            onSettingsClick = { navController.navigate(NavRoutes.settings.name) }
                        )

                        LibraryFilter.Songs -> HomeSongs(
                            navController = navController,
                            onSearchClick = { navController.navigate(NavRoutes.search.name) },
                            onSettingsClick = { navController.navigate(NavRoutes.settings.name) }
                        )
                    }
                }
            }
        }
    }

    if (showLibraryActions) {
        ModalBottomSheet(
            onDismissRequest = { showLibraryActions = false },
            containerColor = colorPalette().background1
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                BasicText(
                    text = stringResource(R.string.mobile_home_import_music),
                    style = typography().xl.bold.copy(color = colorPalette().text),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LibraryActionRow(
                    iconId = R.drawable.add,
                    title = stringResource(R.string.mobile_home_import_link),
                    subtitle = stringResource(R.string.mobile_home_import_link_hint),
                    onClick = {
                        showLibraryActions = false
                        showUrlImport = true
                    }
                )
                LibraryActionRow(
                    iconId = R.drawable.add,
                    title = stringResource(R.string.mobile_home_import_csv),
                    subtitle = matchLabel,
                    onClick = {
                        showLibraryActions = false
                        spotifyImport.onShortClick()
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun mutableLibraryFilter() = androidx.compose.runtime.mutableIntStateOf(LibraryFilter.Playlists.ordinal)

/**
 * Imports a Spotify CSV (exportify.net format) into a local playlist named after the file.
 * Matching with YouTube Music is done in background by [SpotifyMatcher].
 */
@OptIn(ExperimentalSerializationApi::class, UnstableApi::class)
@Composable
private fun rememberSpotifyImport(): ImportSongsFromSpotifyCSV {
    var playlistId by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    return ImportSongsFromSpotifyCSV.init(
        beforeTransaction = { _, row, fileName ->
            val name = row["PlaylistName"]
                ?: fileName?.substringBeforeLast('.')?.replace('_', ' ')?.trim()?.ifBlank { null }
                ?: "Spotify"
            playlistId = Database.playlistExistByName(name)
            if (playlistId == 0L)
                playlistId = Database.insert(Playlist(0L, name, row["PlaylistBrowseId"]))
        },
        afterTransaction = { index, song, _, _ ->
            if (song.id.isBlank()) return@init
            Database.insert(song)
            Database.insert(
                SongPlaylistMap(songId = song.id, playlistId = playlistId, position = index).default()
            )
            SpotifyMatcher.request()
        }
    )
}

@Composable
private fun LibraryActionRow(iconId: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .mobileHomeClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorPalette().background2)
        ) {
            Image(
                painter = painterResource(iconId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(text = title, style = typography().s.semiBold.copy(color = colorPalette().text))
            BasicText(text = subtitle, style = typography().xs.copy(color = colorPalette().textSecondary))
        }
    }
}

@Composable
private fun LibraryIcon(iconId: Int, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(Dimensions.touchTarget)) {
        Image(
            painter = painterResource(iconId),
            contentDescription = label,
            colorFilter = ColorFilter.tint(colorPalette().text),
            modifier = Modifier.size(24.dp)
        )
    }
}
