package it.fast4x.riplay.ui.screens.player.unified

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppearanceSettings
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Info
import it.fast4x.riplay.data.models.UiMedia
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.services.playback.PlayerState
import it.fast4x.riplay.ui.screens.player.unified.components.controls.UnifiedInfoAlbumAndArtistEssential
import it.fast4x.riplay.ui.screens.player.unified.components.controls.UnifiedInfoAlbumAndArtistModern
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.isLandscape
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalMaterial3Api
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun UnifiedControls(
    navController: NavController,
    onCollapse: () -> Unit,
    onBlurScaleChange: (Float) -> Unit,
    expandedplayer: Boolean,
    titleExpanded: Boolean,
    timelineExpanded: Boolean,
    controlsExpanded: Boolean,
    isShowingLyrics: Boolean,
    media: UiMedia,
    title: String?,
    artist: String?,
    artistIds: List<Info>?,
    albumId: String?,
    isExplicit: Boolean,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onSeekTo: (Float) -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onToggleRepeatMode: () -> Unit = {},
    onToggleShuffleMode: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    playerState: PlayerState,
) {

    val appearanceSettingsVieModel = LocalAppearanceSettings.current
    val appearanceSettings = appearanceSettingsVieModel.activeSettings.collectAsState().value

    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    //var disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    val disableScrollingText = appearanceSettings.disableScrollingText

    val mediaItem = playerState.mediaInfo?.mediaItem ?: return

    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
    }


//    var playerTimelineSize by rememberPreference(
//        PLAYER_TIMELINE_SIZE.key,
//        PlayerTimelineSize.Biggest
//    )
    val playerTimelineSize = appearanceSettings.playerTimelineSize


    //val playerInfoType by rememberPreference(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential)
    val playerInfoType = appearanceSettings.playerInfoType
//    var playerSwapControlsWithTimeline by rememberPreference(
//        PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
//        false
//    )
    val playerSwapControlsWithTimeline = appearanceSettings.playerSwapControlsWithTimeline
    //var showlyricsthumbnail by rememberPreference(SHOW_LYRICS_THUMBNAIL.key, false)
    val showlyricsthumbnail = appearanceSettings.showLyricsThumbnail
//    var transparentBackgroundActionBarPlayer by rememberPreference(
//        TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
//        true
//    )
    val transparentBackgroundActionBarPlayer = appearanceSettings.transparentBackgroundActionBarPlayer
    //var playerControlsType by rememberPreference(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
    val playerControlsType = appearanceSettings.playerControlsType
    //var playerPlayButtonType by rememberPreference(PLAYER_PLAY_BUTTON_TYPE.key, PlayerPlayButtonType.Disabled)
    val playerPlayButtonType = appearanceSettings.playerPlayButtonType
    //var showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    val showthumbnail = appearanceSettings.showThumbnail
    //var playerType by rememberPreference(PLAYER_TYPE.key, PlayerType.Modern)
    val playerType = appearanceSettings.playerType
    val expandedlandscape = (isLandscape && playerType == PlayerType.Modern) || (expandedplayer && !showthumbnail)

    Box(
        modifier = Modifier
            .animateContentSize()
    ) {
        if ((!isLandscape) and ((expandedplayer || isShowingLyrics) && !showlyricsthumbnail))
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .padding(horizontal = playerTimelineSize.size.dp)
            ) {

                if (!isShowingLyrics || titleExpanded) {
                    if (playerInfoType == PlayerInfoType.Modern)
                        UnifiedInfoAlbumAndArtistModern(
                            navController = navController,
                            media = media,
                            title = title,
                            albumId = albumId,
                            mediaItem = mediaItem,
                            likedAt = likedAt,
                            onCollapse = onCollapse,
                            disableScrollingText = disableScrollingText,
                            artist = artist,
                            artistIds = artistIds,
                            isExplicit = isExplicit
                        )

                    if (playerInfoType == PlayerInfoType.Essential)
                        UnifiedInfoAlbumAndArtistEssential(
                            navController = navController,
                            albumId = albumId,
                            title = title,
                            likedAt = likedAt,
                            artistIds = artistIds,
                            artist = artist,
                            isExplicit = isExplicit,
                            onCollapse = onCollapse,
                            disableScrollingText = disableScrollingText,
                            mediaItem = mediaItem
                        )
                    Spacer(
                        modifier = Modifier
                            .height(10.dp)
                    )
                }
                if (!isShowingLyrics || timelineExpanded) {
                    UnifiedGetSeekBar(
                        mediaId = mediaItem.mediaId,
                        onSeekTo = onSeekTo,
                        onPlay = onPlay,
                        onPause = onPause,
                    )
                    Spacer(
                        modifier = Modifier
                            .height(if (playerPlayButtonType != PlayerPlayButtonType.Disabled) 10.dp else 5.dp)
                    )
                }
                if (!isShowingLyrics || controlsExpanded) {
                    UnifiedGetControls(
                        likedAt = likedAt,
                        onBlurScaleChange = onBlurScaleChange,
                        onPlay = onPlay,
                        onPause = onPause,
                        onSeekTo = onSeekTo,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleRepeatMode = onToggleRepeatMode,
                        onToggleShuffleMode = onToggleShuffleMode,
                        playerState = playerState
                    )
                    Spacer(
                        modifier = Modifier
                            .height(5.dp)
                    )
                }
                if (((playerControlsType == PlayerControlsType.Modern) || (!transparentBackgroundActionBarPlayer)) && (playerPlayButtonType != PlayerPlayButtonType.Disabled)) {
                    Spacer(
                        modifier = Modifier
                            .height(10.dp)
                    )
                }
            }
        else if (!isLandscape)
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = playerTimelineSize.size.dp)

            ) {

                if (playerInfoType == PlayerInfoType.Modern)
                    UnifiedInfoAlbumAndArtistModern(
                        navController = navController,
                        media = media,
                        title = title,
                        albumId = albumId,
                        mediaItem = mediaItem,
                        likedAt = likedAt,
                        onCollapse = onCollapse,
                        disableScrollingText = disableScrollingText,
                        artist = artist,
                        artistIds = artistIds,
                        isExplicit = isExplicit
                    )

                if (playerInfoType == PlayerInfoType.Essential)
                    UnifiedInfoAlbumAndArtistEssential(
                        navController = navController,
                        title = title,
                        albumId = albumId,
                        mediaItem = mediaItem,
                        likedAt = likedAt,
                        onCollapse = onCollapse,
                        disableScrollingText = disableScrollingText,
                        artist = artist,
                        artistIds = artistIds,
                        isExplicit = isExplicit
                    )

                Spacer(
                    modifier = Modifier
                        .height(25.dp)
                )

                if (!playerSwapControlsWithTimeline) {
                    UnifiedGetSeekBar(
                        mediaId = mediaItem.mediaId,
                        onSeekTo = onSeekTo,
                        onPlay = onPlay,
                        onPause = onPause,
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(0.4f)
                    )
                    UnifiedGetControls(
                        likedAt = likedAt,
                        onBlurScaleChange = onBlurScaleChange,
                        onPlay = onPlay,
                        onPause = onPause,
                        onSeekTo = onSeekTo,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleRepeatMode = onToggleRepeatMode,
                        onToggleShuffleMode = onToggleShuffleMode,
                        playerState = playerState
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(0.5f)
                    )
                } else {
                    UnifiedGetControls(
                        likedAt = likedAt,
                        onBlurScaleChange = onBlurScaleChange,
                        onPlay = onPlay,
                        onPause = onPause,
                        onSeekTo = onSeekTo,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleRepeatMode = onToggleRepeatMode,
                        onToggleShuffleMode = onToggleShuffleMode,
                        playerState = playerState
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(0.5f)
                    )
                    UnifiedGetSeekBar(
                        mediaId = mediaItem.mediaId,
                        onSeekTo = onSeekTo,
                        onPlay = onPlay,
                        onPause = onPause,
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(0.4f)
                    )
                }

            }

    }

    if (isLandscape)
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Bottom,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = playerTimelineSize.size.dp)
        ) {

            if (playerInfoType == PlayerInfoType.Modern)
                UnifiedInfoAlbumAndArtistModern(
                    navController = navController,
                    media = media,
                    title = title,
                    albumId = albumId,
                    mediaItem = mediaItem,
                    likedAt = likedAt,
                    onCollapse = onCollapse,
                    disableScrollingText = disableScrollingText,
                    artist = artist,
                    artistIds = artistIds,
                    isExplicit = isExplicit
                )

            if (playerInfoType == PlayerInfoType.Essential)
                UnifiedInfoAlbumAndArtistEssential(
                    navController = navController,
                    title = title,
                    albumId = albumId,
                    mediaItem = mediaItem,
                    likedAt = likedAt,
                    onCollapse = onCollapse,
                    disableScrollingText = disableScrollingText,
                    artist = artist,
                    artistIds = artistIds,
                    isExplicit = isExplicit
                )

            Spacer(
                modifier = Modifier
                    .height(if (expandedlandscape) 10.dp else 25.dp)
            )

            if (!playerSwapControlsWithTimeline) {
                UnifiedGetSeekBar(
                    mediaId = mediaItem.mediaId,
                    onSeekTo = onSeekTo,
                    onPlay = onPlay,
                    onPause = onPause,
                )
                Spacer(
                    modifier = Modifier
                        .animateContentSize()
                        .applyIf(!expandedlandscape) { weight(0.4f) }
                        .applyIf(expandedlandscape) { height(15.dp) }
                )
                UnifiedGetControls(
                    likedAt = likedAt,
                    onBlurScaleChange = onBlurScaleChange,
                    onPlay = onPlay,
                    onPause = onPause,
                    onSeekTo = onSeekTo,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleRepeatMode = onToggleRepeatMode,
                    onToggleShuffleMode = onToggleShuffleMode,
                    playerState = playerState
                )
                Spacer(
                    modifier = Modifier
                        .animateContentSize()
                        .applyIf(!expandedlandscape) { weight(0.5f) }
                        .applyIf(expandedlandscape) { height(15.dp) }
                )
            } else {
                UnifiedGetControls(
                    likedAt = likedAt,
                    onBlurScaleChange = onBlurScaleChange,
                    onPlay = onPlay,
                    onPause = onPause,
                    onSeekTo = onSeekTo,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleRepeatMode = onToggleRepeatMode,
                    onToggleShuffleMode = onToggleShuffleMode,
                    playerState = playerState,
                )
                Spacer(
                    modifier = Modifier
                        .animateContentSize()
                        .applyIf(!expandedlandscape) { weight(0.5f) }
                        .applyIf(expandedlandscape) { height(15.dp) }
                )
                UnifiedGetSeekBar(
                    mediaId = mediaItem.mediaId,
                    onSeekTo = onSeekTo,
                    onPlay = onPlay,
                    onPause = onPause,
                )
                Spacer(
                    modifier = Modifier
                        .animateContentSize()
                        .applyIf(!expandedlandscape) { weight(0.4f) }
                        .applyIf(expandedlandscape) { height(15.dp) }
                )
            }
        }
}