package it.fast4x.riplay.ui.screens.ondevice

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.compose.persist.persist
import it.fast4x.riplay.Database
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.ui.components.navigation.header.AppHeader
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.playerPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.transitionEffectKey


@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "SimpleDateFormat")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun OnDeviceAlbumScreen(
    navController: NavController,
    albumId: String,
    modifier: Modifier = Modifier,
    miniPlayer: @Composable () -> Unit = {}
) {

    val saveableStateHolder = rememberSaveableStateHolder()

    var album by persist<Album?>("album/$albumId/album")

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    //PersistMapCleanup(tagPrefix = "album/$albumId/")

    LaunchedEffect(Unit) {
        Database
            .album(albumId).collect { currentAlbum ->
                println("AlbumScreen collect ${currentAlbum?.title}")
                album = currentAlbum
            }

    }


//    val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit =
//        { textButton ->
//            if (album?.timestamp == null) {
//                HeaderPlaceholder(
//                    modifier = Modifier
//                        .shimmer()
//                )
//            } else {
//                val context = LocalContext.current
//
//                Header(
//                    title = "",
//                    modifier = Modifier.padding(horizontal = 12.dp),
//                    actionsContent = {
//                        textButton?.invoke()
//
//
//                        Spacer(
//                            modifier = Modifier
//                                .weight(1f)
//                        )
//
////                        HeaderIconButton(
////                            icon = if (album?.bookmarkedAt == null) {
////                                R.drawable.bookmark_outline
////                            } else {
////                                R.drawable.bookmark
////                            },
////                            color = colorPalette().accent,
////                            onClick = {
////                                val bookmarkedAt =
////                                    if (album?.bookmarkedAt == null) System.currentTimeMillis() else null
////
////                                Database.asyncTransaction {
////                                    album?.copy(bookmarkedAt = bookmarkedAt)
////                                        ?.let(::update)
////                                }
////                            }
////                        )
////
////                        HeaderIconButton(
////                            icon = R.drawable.share_social,
////                            color = colorPalette().text,
////                            onClick = {
////                                album?.shareUrl?.let { url ->
////                                    val sendIntent = Intent().apply {
////                                        Intent.setAction = Intent.ACTION_SEND
////                                        Intent.setType = "text/plain"
////                                        putExtra(Intent.EXTRA_TEXT, url)
////                                    }
////
////                                    context.startActivity(
////                                        Intent.createChooser(
////                                            sendIntent,
////                                            null
////                                        )
////                                    )
////                                }
////                            }
////                        )
//                    },
//                    disableScrollingText = disableScrollingText
//                )
//            }
//        }

//    val thumbnailContent =
//        adaptiveThumbnailContent(
//            album?.timestamp == null,
//            album?.thumbnailUrl,
//            showIcon = false, //albumPage?.otherVersions?.isNotEmpty(),
//            onOtherVersionAvailable = {
//                //println("mediaItem Click other version")
//            },
//            //shape = thumbnailRoundness.shape()
//            onClick = { changeShape = !changeShape },
//            shape = if (changeShape) CircleShape else thumbnailRoundness.shape(),
//        )

    val transitionEffect by rememberPreference(transitionEffectKey, TransitionEffect.Scale)
    val playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)

    Scaffold(
        modifier = Modifier,
        containerColor = colorPalette().background0,
        topBar = {
            if (UiType.RiPlay.isCurrent())
                AppHeader(navController).Draw()
        }
    ) {
        //**
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                val topPadding = if (UiType.ViMusic.isCurrent()) 30.dp else 0.dp

                AnimatedContent(
                    targetState = 0,
                    transitionSpec = {
                        when (transitionEffect) {
                            TransitionEffect.None -> EnterTransition.None togetherWith ExitTransition.None
                            TransitionEffect.Expand -> expandIn(
                                animationSpec = tween(
                                    350,
                                    easing = LinearOutSlowInEasing
                                ), expandFrom = Alignment.BottomStart
                            ).togetherWith(
                                shrinkOut(
                                    animationSpec = tween(
                                        350,
                                        easing = FastOutSlowInEasing
                                    ), shrinkTowards = Alignment.CenterStart
                                )
                            )

                            TransitionEffect.Fade -> fadeIn(animationSpec = tween(350)).togetherWith(
                                fadeOut(animationSpec = tween(350))
                            )

                            TransitionEffect.Scale -> scaleIn(animationSpec = tween(350)).togetherWith(
                                scaleOut(animationSpec = tween(350))
                            )

                            TransitionEffect.SlideHorizontal, TransitionEffect.SlideVertical -> {
                                val slideDirection = when (targetState > initialState) {
                                    true -> {
                                        if (transitionEffect == TransitionEffect.SlideHorizontal)
                                            AnimatedContentTransitionScope.SlideDirection.Left
                                        else AnimatedContentTransitionScope.SlideDirection.Up
                                    }

                                    false -> {
                                        if (transitionEffect == TransitionEffect.SlideHorizontal)
                                            AnimatedContentTransitionScope.SlideDirection.Right
                                        else AnimatedContentTransitionScope.SlideDirection.Down
                                    }
                                }

                                val animationSpec = spring(
                                    dampingRatio = 0.9f,
                                    stiffness = Spring.StiffnessLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )

                                slideIntoContainer(
                                    slideDirection,
                                    animationSpec
                                ) togetherWith
                                        slideOutOfContainer(slideDirection, animationSpec)
                            }
                        }
                    },
                    label = "",
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = topPadding)
                ) { currentTabIndex ->
                   // saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                        when (currentTabIndex) {
                            0 -> OnDeviceAlbumDetails(
                                navController = navController,
                                albumId = albumId,
                                onSearchClick = {
                                    navController.navigate(NavRoutes.search.name)
                                },
                                onSettingsClick = {
                                    navController.navigate(NavRoutes.settings.name)
                                }
                            )

                        }
                    //}
                }
            }

            //**
            Box(
                modifier = modifier
                    .padding(vertical = 5.dp)
                    .align(
                        if (playerPosition == PlayerPosition.Top) Alignment.TopCenter else Alignment.BottomCenter
                    )
            ) {
                miniPlayer.invoke()
            }
        }
    }

}
