package it.fast4x.riplay.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.HomePage
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.smoothloader.Loader
import it.fast4x.riplay.ui.screens.settings.isYtLoggedIn
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.Encore
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.HomeDataCache
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class HomeFilter(val label: String) { All("All"), Music("Music"), Podcasts("Podcasts") }

/**
 * Inicio estilo Spotify:
 *  [Todo] [Música] [Podcasts]
 *  ┌──────┬──────┐  cuadrícula 2 × 4 de lo escuchado recientemente
 *  └──────┴──────┘
 *  Secciones con carruseles de carátulas cuadradas (140dp) – artistas en círculo.
 */
@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
fun SpotifyHome(navController: NavController) {
    val binder = LocalPlayerServiceBinder.current

    var homePage by remember { mutableStateOf<HomePage?>(HomeDataCache.homePage) }
    LaunchedEffect(Unit) {
        if (homePage != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            var attempt = 0
            while (homePage == null && attempt < 3) {
                homePage = runCatching { EnvironmentExt.getHomePage(setLogin = isYtLoggedIn()).getOrNull() }.getOrNull()
                attempt++
                if (homePage == null) delay(500L * attempt)
            }
            HomeDataCache.homePage = homePage
        }
    }

    val recent by remember { Database.lastPlayed(40) }.collectAsState(initial = emptyList(), context = Dispatchers.IO)
    val recentDistinct = remember(recent) { recent.distinctBy { it.id } }

    var filter by rememberSaveable { mutableIntStateOf(HomeFilter.All.ordinal) }

    fun playSong(song: Song) {
        binder?.stopRadio()
        binder?.hybridPlayer?.forcePlay(song.asMediaItem)
        binder?.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = song.id))
    }

    fun playItem(item: Environment.SongItem) {
        binder?.stopRadio()
        binder?.hybridPlayer?.forcePlay(item.asMediaItem)
        binder?.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = item.key))
    }

    fun openItem(item: Environment.Item) {
        when (item) {
            is Environment.SongItem -> playItem(item)
            is Environment.VideoItem -> {
                binder?.stopRadio()
                binder?.hybridPlayer?.forcePlay(item.asMediaItem)
            }
            is Environment.AlbumItem -> navController.navigate("${NavRoutes.album.name}/${item.key}")
            is Environment.ArtistItem -> navController.navigate("${NavRoutes.artist.name}/${item.key}")
            is Environment.PlaylistItem -> navController.navigate("${NavRoutes.playlist.name}/${item.key}")
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
            .verticalScroll(rememberScrollState())
    ) {
        // ---------- Chips ----------
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            HomeFilter.entries.forEach { f ->
                val selected = f.ordinal == filter
                BasicText(
                    text = f.label,
                    style = typography().xs.semiBold.copy(
                        color = if (selected) Encore.OnGreen else Color.White
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) Encore.Green else Encore.SurfaceElevated)
                        .clickable { filter = f.ordinal }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }

        // ---------- Cuadrícula 2 × 4 ----------
        if (filter != HomeFilter.Podcasts.ordinal && recentDistinct.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                recentDistinct.take(8).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { song ->
                            RecentTile(song, Modifier.weight(1f)) { playSong(song) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ---------- Escuchado recientemente ----------
        if (filter != HomeFilter.Podcasts.ordinal && recentDistinct.size > 8) {
            SectionTitle("Recently played")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentDistinct.drop(8).take(15), key = { it.id }) { song ->
                    CoverCard(
                        imageUrl = song.thumbnailUrl.toThumbnail(300),
                        title = song.title,
                        subtitle = song.artistsText,
                        onClick = { playSong(song) }
                    )
                }
            }
        }

        // ---------- Secciones de YouTube Music ----------
        val page = homePage
        if (page == null) {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) { Loader() }
        } else {
            page.sections
                .filter { it.items.isNotEmpty() }
                .filter {
                    when (HomeFilter.entries[filter]) {
                        HomeFilter.Podcasts -> it.title.contains("podcast", ignoreCase = true)
                        HomeFilter.Music -> !it.title.contains("podcast", ignoreCase = true)
                        HomeFilter.All -> true
                    }
                }
                .forEach { section ->
                    SectionTitle(section.title)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(section.items.filterNotNull()) { item ->
                            val subtitle = when (item) {
                                is Environment.SongItem -> item.authors?.joinToString { it.name ?: "" }
                                is Environment.VideoItem -> item.authors?.joinToString { it.name ?: "" }
                                is Environment.AlbumItem -> item.authors?.joinToString { it.name ?: "" }
                                is Environment.PlaylistItem -> item.channel?.name
                                is Environment.ArtistItem -> "Artist"
                                else -> null
                            }
                            CoverCard(
                                imageUrl = item.thumbnail?.size(300),
                                title = item.title ?: "",
                                subtitle = subtitle,
                                shape = if (item is Environment.ArtistItem) CircleShape else RoundedCornerShape(4.dp),
                                onClick = { openItem(item) }
                            )
                        }
                    }
                }
        }

        Spacer(Modifier.height(Dimensions.bottomSpacer))
    }
}

@Composable
private fun SectionTitle(title: String) {
    BasicText(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = typography().xl.bold.copy(color = Color.White),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp)
    )
}

@Composable
private fun RecentTile(song: Song, modifier: Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Encore.SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = song.thumbnailUrl.toThumbnail(160),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp)
        )
        BasicText(
            text = song.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = typography().xxs.bold.copy(color = Color.White),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun CoverCard(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    shape: Shape = RoundedCornerShape(4.dp),
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(shape)
                .background(Encore.SurfaceElevated)
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography().xs.semiBold.copy(color = Color.White)
        )
        if (!subtitle.isNullOrBlank())
            BasicText(
                text = subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = typography().xxs.copy(color = Encore.TextSecondary)
            )
    }
}
