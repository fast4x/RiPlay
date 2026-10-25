package it.fast4x.riplay.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.requests.HomePage
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.Dependencies
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.smoothloader.Loader
import it.fast4x.riplay.ui.components.mobilePressFeedback
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

private enum class HomeFilter(val label: Int) {
    All(R.string.mobile_home_all), Music(R.string.mobile_home_music), Podcasts(R.string.mobile_home_podcasts)
}

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
    val playlistRecommendations = remember { Dependencies.application.playlistRecommendationService }
    val recommendations by playlistRecommendations.state.collectAsStateWithLifecycle()

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
    ) {
        val recentColumns = if (maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f) 1 else 2
        val coverSize = (maxWidth * 0.42f).coerceIn(136.dp, 172.dp)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "filters") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HomeFilter.entries.forEach { f ->
                        MobileFilterChip(stringResource(f.label), f.ordinal == filter) { filter = f.ordinal }
                    }
                }
            }

            val taste = recommendations.profile
            if (filter != HomeFilter.Podcasts.ordinal && taste != null && taste.songCount > 0) {
                item(key = "playlist_recommendations") {
                    Column {
                        SectionTitle(stringResource(R.string.playlist_recs_title))
                        BasicText(
                            text = stringResource(
                                R.string.playlist_recs_summary,
                                pluralStringResource(R.plurals.playlist_recs_lists, taste.playlistCount, taste.playlistCount),
                                pluralStringResource(R.plurals.playlist_recs_songs, taste.songCount, taste.songCount)
                            ),
                            style = typography().xs.copy(color = Encore.TextSecondary),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (taste.topArtists.isNotEmpty()) {
                            BasicText(
                                text = stringResource(R.string.playlist_recs_artists, taste.topArtists.take(3).joinToString(", ")),
                                style = typography().xs.copy(color = Encore.TextSecondary),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        if (recommendations.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp)
                            ) {
                                Loader(size = 20.dp, expandWidth = false)
                                BasicText(
                                    text = stringResource(R.string.playlist_recs_loading),
                                    style = typography().xs.copy(color = Encore.TextSecondary),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (recommendations.items.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recommendations.items, key = { it.key }) { song ->
                                    CoverCard(
                                        imageUrl = song.thumbnail?.size(300),
                                        title = song.title.orEmpty(),
                                        subtitle = song.authors?.mapNotNull { it.name }?.joinToString(", "),
                                        coverSize = coverSize,
                                        onClick = { playItem(song) }
                                    )
                                }
                            }
                        }
                        val message = when {
                            recommendations.offline && recommendations.items.isEmpty() -> R.string.playlist_recs_offline
                            recommendations.offline -> R.string.playlist_recs_offline_cached
                            taste.seeds.isEmpty() -> R.string.playlist_recs_missing_metadata
                            !recommendations.isLoading && recommendations.items.isEmpty() -> R.string.playlist_recs_empty
                            else -> null
                        }
                        message?.let {
                            BasicText(
                                text = stringResource(it),
                                style = typography().xs.copy(color = Encore.TextSecondary),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        if (!recommendations.offline && !recommendations.isLoading && taste.seeds.isNotEmpty()) {
                            TextButton(
                                onClick = playlistRecommendations::refresh,
                                modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp)
                            ) {
                                BasicText(
                                    text = stringResource(if (recommendations.canRetry) R.string.playlist_recs_retry else R.string.playlist_recs_refresh),
                                    style = typography().xs.semiBold.copy(color = Encore.Green)
                                )
                            }
                        }
                    }
                }
            }

            if (filter != HomeFilter.Podcasts.ordinal && recentDistinct.isNotEmpty()) {
                item(key = "recent_grid") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        recentDistinct.take(8).chunked(recentColumns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { song ->
                                    RecentTile(song, Modifier.weight(1f)) { playSong(song) }
                                }
                                if (row.size < recentColumns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (filter != HomeFilter.Podcasts.ordinal && recentDistinct.size > 8) {
                item(key = "recent_carousel") {
                    Column {
                        SectionTitle(stringResource(R.string.mobile_home_recently_played))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(recentDistinct.drop(8).take(15), key = { it.id }) { song ->
                                CoverCard(
                                    imageUrl = song.thumbnailUrl.toThumbnail(300),
                                    title = song.title,
                                    subtitle = song.artistsText,
                                    coverSize = coverSize,
                                    onClick = { playSong(song) }
                                )
                            }
                        }
                    }
                }
            }

            val page = homePage
            if (page == null) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) { Loader() }
                }
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
                        item {
                            Column {
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
                                            is Environment.ArtistItem -> stringResource(R.string.mobile_home_artist)
                                            else -> null
                                        }
                                        CoverCard(
                                            imageUrl = item.thumbnail?.size(300),
                                            title = item.title ?: "",
                                            subtitle = subtitle,
                                            coverSize = coverSize,
                                            shape = if (item is Environment.ArtistItem) CircleShape else RoundedCornerShape(4.dp),
                                            onClick = { openItem(item) }
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

@Composable
private fun SectionTitle(title: String) {
    BasicText(
        text = title,
        maxLines = 2,
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
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Encore.SurfaceElevated)
            .mobileHomeClickable(onClick = onClick)
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
            style = typography().xs.semiBold.copy(color = Color.White),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CoverCard(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    coverSize: androidx.compose.ui.unit.Dp,
    shape: Shape = RoundedCornerShape(4.dp),
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(coverSize)
            .clip(RoundedCornerShape(6.dp))
            .mobileHomeClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(coverSize)
                .clip(shape)
                .background(Encore.SurfaceElevated)
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = typography().xs.semiBold.copy(color = Color.White)
        )
        if (!subtitle.isNullOrBlank())
            BasicText(
                text = subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = typography().xs.copy(color = Encore.TextSecondary)
            )
    }
}

@Composable
internal fun MobileFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = Dimensions.touchTarget, minHeight = Dimensions.touchTarget)
            .clip(CircleShape)
            .background(if (isSelected) Encore.Green else Encore.SurfaceElevated)
            .semantics { selected = isSelected }
            .mobileHomeClickable(onClick, Role.Tab)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BasicText(
            text = label,
            style = typography().xs.semiBold.copy(color = if (isSelected) Encore.OnGreen else Color.White)
        )
    }
}

@Composable
internal fun Modifier.mobileHomeClickable(onClick: () -> Unit, role: Role = Role.Button): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .mobilePressFeedback(interactionSource)
        .semantics(mergeDescendants = true) {}
        .clickable(interactionSource = interactionSource, indication = ripple(), role = role, onClick = onClick)
}
