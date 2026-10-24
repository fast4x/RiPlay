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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
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
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
            .padding(horizontal = 16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BasicText(
                text = "Search",
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
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .clickable { navController.navigate(NavRoutes.search.name) }
                    .padding(horizontal = 12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF121212)),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                BasicText(
                    text = "What do you want to listen to?",
                    style = typography().s.semiBold.copy(color = Color(0xFF535353))
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            BasicText(
                text = "Browse all",
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

        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(Dimensions.bottomSpacer)) }
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
            .aspectRatio(1.75f)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
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

private enum class LibraryFilter(val label: String) {
    Playlists("Playlists"), Artists("Artists"), Albums("Albums"), Songs("Songs")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
    ) {
        BasicText(
            text = "Your Library",
            style = typography().xxl.bold.copy(color = colorPalette().text),
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
        )

        // Píldoras de filtro
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            LibraryFilter.entries.forEach { f ->
                val selected = f.ordinal == filter
                BasicText(
                    text = f.label,
                    style = typography().xs.semiBold.copy(
                        color = if (selected) Encore.OnGreen else colorPalette().text
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) colorPalette().accent else colorPalette().background2)
                        .clickable { filter = f.ordinal }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // "Canciones que te gustan" fijada arriba
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    coroutineScope.launch {
                        appSettingsManager.updateSettings(
                            appSettingsManager.activeSettings.value.copy(builtInPlaylist = BuiltInPlaylist.Favorites)
                        )
                    }
                    filter = LibraryFilter.Songs.ordinal
                }
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
            Column {
                BasicText(
                    text = "Liked Songs",
                    style = typography().s.semiBold.copy(color = colorPalette().text)
                )
                BasicText(
                    text = "Playlist",
                    style = typography().xs.copy(color = colorPalette().textSecondary)
                )
            }
        }

        // "Import from Spotify link": public playlist URL (upstream RiPlay importer)
        var showUrlImport by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        if (showUrlImport) ImportPlaylistFromSpotifyUrlDialog(onDismiss = { showUrlImport = false })
        LibraryActionRow(
            iconId = R.drawable.add,
            title = "Import from Spotify link",
            subtitle = "Paste a public playlist link from Spotify",
            onClick = { showUrlImport = true }
        )

        // "Import from Spotify": exportify.net CSV → automatic YouTube Music matching
        val spotifyImport = rememberSpotifyImport()
        val matchProgress by SpotifyMatcher.progress.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { SpotifyMatcher.request() } // fixes songs left unmatched earlier
        val matchLabel = when {
            matchProgress.running -> "Matching songs… ${matchProgress.done}/${matchProgress.total}"
            matchProgress.failed > 0 -> "${matchProgress.failed} songs couldn't be found"
            else -> "CSV file from exportify.net"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { spotifyImport.onShortClick() }
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
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                BasicText(
                    text = "Import Spotify CSV",
                    style = typography().s.semiBold.copy(color = colorPalette().text)
                )
                BasicText(
                    text = matchLabel,
                    style = typography().xs.copy(color = colorPalette().textSecondary)
                )
            }
        }

        Box(Modifier.weight(1f)) {
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
            .clickable(onClick = onClick)
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
        Column {
            BasicText(text = title, style = typography().s.semiBold.copy(color = colorPalette().text))
            BasicText(text = subtitle, style = typography().xs.copy(color = colorPalette().textSecondary))
        }
    }
}
