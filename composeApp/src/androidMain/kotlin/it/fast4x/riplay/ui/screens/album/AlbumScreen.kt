package it.fast4x.riplay.ui.screens.album

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.valentinilk.shimmer.shimmer
import it.fast4x.riplay.extensions.persist.PersistMapCleanup
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.AlbumPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.ui.components.themed.Header
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.HeaderPlaceholder
import it.fast4x.riplay.ui.components.themed.adaptiveThumbnailContent
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.items.AlbumItemPlaceholder
import it.fast4x.riplay.ui.screens.searchresult.ItemsPage
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.playerPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.transitionEffectKey
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.ui.components.PageContainer
import it.fast4x.riplay.utils.asMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber

@ExperimentalSerializationApi
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "SimpleDateFormat")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun AlbumScreen(
    navController: NavController,
    browseId: String,
    miniPlayer: @Composable () -> Unit = {}
) {

    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Light
    )
    var changeShape by remember {
        mutableStateOf(false)
    }

    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<AlbumPage?>("album/$browseId/albumPage")

    LaunchedEffect(Unit) {
        Database
            .album(browseId).collect { currentAlbum ->
                println("AlbumScreen collect ${currentAlbum?.title}")
                album = currentAlbum
                CoroutineScope(Dispatchers.IO).launch {
                    if (albumPage == null)
                        EnvironmentExt.getAlbum(browseId)
                            .onSuccess { currentAlbumPage ->
                                albumPage = currentAlbumPage

                                println("AlbumScreen otherVersion ${currentAlbumPage.otherVersions}")
                                Database.upsert(
                                    Album(
                                        id = browseId,
                                        title = album?.title ?: currentAlbumPage.album.title,
                                        thumbnailUrl = if (album?.thumbnailUrl?.startsWith(
                                                MODIFIED_PREFIX
                                            ) == true
                                        ) album?.thumbnailUrl else currentAlbumPage.album.thumbnail?.url,
                                        year = currentAlbumPage.album.year,
                                        authorsText = if (album?.authorsText?.startsWith(
                                                MODIFIED_PREFIX
                                            ) == true
                                        ) album?.authorsText else currentAlbumPage.album.authors
                                            ?.joinToString(", ") { it.name ?: "" },
                                        shareUrl = currentAlbumPage.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt,
                                        isYoutubeAlbum = album?.isYoutubeAlbum == true
                                    ),
                                    currentAlbumPage
                                        .songs.distinct()
                                        .map(Environment.SongItem::asMediaItem)
                                        .onEach(Database::insert)
                                        .mapIndexed { position, mediaItem ->
                                            SongAlbumMap(
                                                songId = mediaItem.mediaId,
                                                albumId = browseId,
                                                position = position
                                            )
                                        }
                                )
                            }
                            .onFailure {
                                Timber.e("AlbumScreen error ${it.stackTraceToString()}")
                            }
                }
            }

    }

    val thumbnailContent =
        adaptiveThumbnailContent(
            album?.timestamp == null,
            album?.thumbnailUrl,
            showIcon = false,
            onOtherVersionAvailable = {},
            //shape = thumbnailRoundness.shape()
            onClick = { changeShape = !changeShape },
            shape = if (changeShape) CircleShape else thumbnailRoundness.shape(),
        )

    PageContainer(
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        AlbumDetails(
            navController = navController,
            browseId = browseId,
            albumPage = albumPage,
            thumbnailContent = thumbnailContent,
            onSearchClick = {
                navController.navigate(NavRoutes.search.name)
            },
            onSettingsClick = {
                navController.navigate(NavRoutes.settings.name)
            }
        )
    }

}




