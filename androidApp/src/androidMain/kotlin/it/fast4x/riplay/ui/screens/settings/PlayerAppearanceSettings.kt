package it.fast4x.riplay.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.CarouselSize
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerTimelineSize
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.PrevNextSongs
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.SongsNumber
import it.fast4x.riplay.enums.ThumbnailCoverType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.RestartPlayerService
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTIONS_SPACED_EVENLY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BACKGROUND_PROGRESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLACK_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BOTTOM_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BUTTON_ZOOM_OUT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLICK_ON_LYRICS_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CONTROLS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_PLAYER_HORIZONTAL_SWIPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_WALLPAPER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER_TOGGLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FADING_EDGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ICON_LIKE_TYPE
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_SHOWING_THUMBNAIL_IN_LOCKSCREEN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LAST_PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINI_PLAYER_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINI_QUEUE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_POSITION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NO_BLUR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NOTIFICATION_PLAYER_FIRST_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NOTIFICATION_PLAYER_SECOND_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_BACKGROUND_COLORS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_CONTROLS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_ENABLE_LYRICS_POPUP_MESSAGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_SHOW_ICONS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_SWAP_CONTROLS_WITH_TIMELINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_THUMBNAIL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PREV_NEXT_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_DURATION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_TYPE
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BACKGROUND_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ARROW
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LOOP
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_MENU
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SHUFFLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SLEEP_TIMER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_START_RADIO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_VIDEO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LIKE_BUTTON_BACKGROUND_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEXT_SONGS_IN_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_REMAINING_SONG_TIME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOTAL_TIME_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ALBUM_COVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LYRICS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_VIS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_FOR_NERDS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_UP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TAP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TEXT_OUTLINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TAP_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_PAUSE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TIMELINE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TITLE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VISUALIZER_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.WALLPAPER_TYPE
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.AnimatedGradient
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.SwipeAnimationNoThumbnail
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.experimental.appearancepreset.AppearancePreferences
import it.fast4x.riplay.utils.getUiType
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.AppearancePresetDialog
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem
import it.fast4x.riplay.utils.RestartActivity
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ALBUM_COVER_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ANIMATED_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLUR_SCALE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_THUMBNAIL_SIZE_L
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SEEK_WITH_TAP
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PLAYER_ACTIONS_BAR
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_ANIMATIONS_NO_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE_EX
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_SPACING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TOP_PADDING
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.isAtLeastAndroid13
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun DefaultPlayerAppearanceSettings() {
    var isShowingThumbnailInLockscreen by rememberPreference(
        IS_SHOWING_THUMBNAIL_IN_LOCKSCREEN.key,
        true
    )
    isShowingThumbnailInLockscreen = true
    var showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    showthumbnail = true
    var transparentbar by rememberPreference(TRANSPARENT_BAR.key, true)
    transparentbar = true
    var blackgradient by rememberPreference(BLACK_GRADIENT.key, false)
    blackgradient = false
    var showlyricsthumbnail by rememberPreference(SHOW_LYRICS_THUMBNAIL.key, false)
    showlyricsthumbnail = false
    var playerPlayButtonType by rememberPreference(
        PLAYER_PLAY_BUTTON_TYPE.key,
        PlayerPlayButtonType.Disabled
    )
    playerPlayButtonType = PlayerPlayButtonType.Disabled
    var bottomgradient by rememberPreference(BOTTOM_GRADIENT.key, false)
    bottomgradient = false
    var textoutline by rememberPreference(TEXT_OUTLINE.key, false)
    textoutline = false
    var lastPlayerPlayButtonType by rememberPreference(
        LAST_PLAYER_PLAY_BUTTON_TYPE.key,
        PlayerPlayButtonType.Rectangular
    )
    lastPlayerPlayButtonType = PlayerPlayButtonType.Rectangular
    var disablePlayerHorizontalSwipe by rememberPreference(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, false)
    disablePlayerHorizontalSwipe = false
    var disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    disableScrollingText = false
    var showLikeButtonBackgroundPlayer by rememberPreference(
        SHOW_LIKE_BUTTON_BACKGROUND_PLAYER.key,
        true
    )
    showLikeButtonBackgroundPlayer = true

    var visualizerEnabled by rememberPreference(VISUALIZER_ENABLED.key, false)
    visualizerEnabled = false
    var playerTimelineType by rememberPreference(PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    playerTimelineType = PlayerTimelineType.Default
    var playerThumbnailSize by rememberPreference(
        PLAYER_THUMBNAIL_SIZE.key,
        PlayerThumbnailSize.Biggest
    )
    playerThumbnailSize = PlayerThumbnailSize.Biggest
    var playerTimelineSize by rememberPreference(
        PLAYER_TIMELINE_SIZE.key,
        PlayerTimelineSize.Biggest
    )
    playerTimelineSize = PlayerTimelineSize.Biggest
    var thumbnailTapEnabled by rememberPreference(THUMBNAIL_TAP_ENABLED.key, true)
    thumbnailTapEnabled = true
    var showButtonPlayerAddToPlaylist by rememberPreference(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true)
    showButtonPlayerAddToPlaylist = true
    var showButtonPlayerArrow by rememberPreference(SHOW_BUTTON_PLAYER_ARROW.key, true)
    showButtonPlayerArrow = false
//    var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey.key, true)
//    showButtonPlayerDownload = true
    var showButtonPlayerLoop by rememberPreference(SHOW_BUTTON_PLAYER_LOOP.key, true)
    showButtonPlayerLoop = true
    var showButtonPlayerLyrics by rememberPreference(SHOW_BUTTON_PLAYER_LYRICS.key, true)
    showButtonPlayerLyrics = true
    var expandedplayertoggle by rememberPreference(EXPANDED_PLAYER_TOGGLE.key, true)
    expandedplayertoggle = true
    var showButtonPlayerShuffle by rememberPreference(SHOW_BUTTON_PLAYER_SHUFFLE.key, true)
    showButtonPlayerShuffle = true
    var showButtonPlayerSleepTimer by rememberPreference(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key, false)
    showButtonPlayerSleepTimer = false
    var showButtonPlayerMenu by rememberPreference(SHOW_BUTTON_PLAYER_MENU.key, false)
    showButtonPlayerMenu = false
    var showButtonPlayerSystemEqualizer by rememberPreference(
        SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key,
        false
    )
    showButtonPlayerSystemEqualizer = false
    var showButtonPlayerDiscover by rememberPreference(SHOW_BUTTON_PLAYER_DISCOVER.key, false)
    showButtonPlayerDiscover = false
    var showButtonPlayerVideo by rememberPreference(SHOW_BUTTON_PLAYER_VIDEO.key, true)
    showButtonPlayerVideo = false
    var navigationBarPosition by rememberPreference(
        NAVIGATION_BAR_POSITION.key,
        NavigationBarPosition.Bottom
    )
    if (getUiType()==UiType.RiPlay)
        navigationBarPosition = NavigationBarPosition.Bottom
    else
        navigationBarPosition = NavigationBarPosition.Left

    var showTotalTimeQueue by rememberPreference(SHOW_TOTAL_TIME_QUEUE.key, true)
    showTotalTimeQueue = true
    var backgroundProgress by rememberPreference(
        BACKGROUND_PROGRESS.key,
        BackgroundProgress.MiniPlayer
    )
    backgroundProgress = BackgroundProgress.MiniPlayer
    var showNextSongsInPlayer by rememberPreference(SHOW_NEXT_SONGS_IN_PLAYER.key, false)
    showNextSongsInPlayer = false
    var showRemainingSongTime by rememberPreference(SHOW_REMAINING_SONG_TIME.key, true)
    showRemainingSongTime = true
    var clickLyricsText by rememberPreference(CLICK_ON_LYRICS_TEXT.key, true)
    clickLyricsText = true
    var showBackgroundLyrics by rememberPreference(SHOW_BACKGROUND_LYRICS.key, false)
    showBackgroundLyrics = false
    var thumbnailRoundness by rememberPreference(
        THUMBNAIL_ROUNDNESS.key,
        ThumbnailRoundness.Light
    )
    thumbnailRoundness = ThumbnailRoundness.Light
    var miniPlayerType by rememberPreference(
        MINI_PLAYER_TYPE.key,
        MiniPlayerType.Modern
    )
    miniPlayerType = MiniPlayerType.Modern
    var playerBackgroundColors by rememberPreference(
        PLAYER_BACKGROUND_COLORS.key,
        PlayerBackgroundColors.BlurredCoverColor
    )
    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
    var showTopActionsBar by rememberPreference(SHOW_TOP_ACTIONS_BAR.key, true)
    showTopActionsBar = true
    var playerControlsType by rememberPreference(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
    playerControlsType = PlayerControlsType.Modern
    var playerInfoType by rememberPreference(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential)
    playerInfoType = PlayerInfoType.Modern
    var transparentBackgroundActionBarPlayer by rememberPreference(
        TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
        true
    )
    transparentBackgroundActionBarPlayer = false
    var iconLikeType by rememberPreference(ICON_LIKE_TYPE.key, IconLikeType.Essential)
    iconLikeType = IconLikeType.Essential
    var playerSwapControlsWithTimeline by rememberPreference(
        PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
        false
    )
    playerSwapControlsWithTimeline = false
    var playerEnableLyricsPopupMessage by rememberPreference(
        PLAYER_ENABLE_LYRICS_POPUP_MESSAGE.key,
        true
    )
    playerEnableLyricsPopupMessage = true
    var actionspacedevenly by rememberPreference(ACTIONS_SPACED_EVENLY.key, false)
    actionspacedevenly = false
    var thumbnailType by rememberPreference(THUMBNAIL_TYPE.key, ThumbnailType.Modern)
    thumbnailType = ThumbnailType.Modern
    var showvisthumbnail by rememberPreference(SHOW_VIS_THUMBNAIL.key, false)
    showvisthumbnail = false
    var buttonzoomout by rememberPreference(BUTTON_ZOOM_OUT.key, false)
    buttonzoomout = false
    var thumbnailpause by rememberPreference(THUMBNAIL_PAUSE.key, false)
    thumbnailpause = false
    var showsongs by rememberPreference(SHOW_SONGS.key, SongsNumber.`2`)
    showsongs = SongsNumber.`2`
    var showalbumcover by rememberPreference(SHOW_ALBUM_COVER.key, true)
    showalbumcover = true
    var prevNextSongs by rememberPreference(PREV_NEXT_SONGS.key, PrevNextSongs.twosongs)
    prevNextSongs = PrevNextSongs.twosongs
    var tapqueue by rememberPreference(TAP_QUEUE.key, true)
    tapqueue = true
    var swipeUpQueue by rememberPreference(SWIPE_UP_QUEUE.key, true)
    swipeUpQueue = true
    var statsfornerds by rememberPreference(STATS_FOR_NERDS.key, false)
    statsfornerds = false
    var playerType by rememberPreference(PLAYER_TYPE.key, PlayerType.Modern)
    playerType = PlayerType.Modern
    var queueType by rememberPreference(QUEUE_TYPE.key, QueueType.Modern)
    queueType = QueueType.Modern
    var noblur by rememberPreference(NO_BLUR.key, true)
    noblur = true
    var fadingedge by rememberPreference(FADING_EDGE.key, false)
    fadingedge = false
    var carousel by rememberPreference(CAROUSEL.key, true)
    carousel = true
    var carouselSize by rememberPreference(CAROUSEL_SIZE.key, CarouselSize.Biggest)
    carouselSize = CarouselSize.Biggest
    var keepPlayerMinimized by rememberPreference(KEEP_PLAYER_MINIMIZED.key,false)
    keepPlayerMinimized = false
    var playerInfoShowIcons by rememberPreference(PLAYER_INFO_SHOW_ICONS.key, true)
    playerInfoShowIcons = true
}

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun PlayerAppearanceSettings(
    navController: NavController,
) {

    var isShowingThumbnailInLockscreen by rememberPreference(
        IS_SHOWING_THUMBNAIL_IN_LOCKSCREEN.key,
        true
    )

    var showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    var transparentbar by rememberPreference(TRANSPARENT_BAR.key, true)
    var blackgradient by rememberPreference(BLACK_GRADIENT.key, false)
    var showlyricsthumbnail by rememberPreference(SHOW_LYRICS_THUMBNAIL.key, false)
    var expandedplayer by rememberPreference(EXPANDED_PLAYER.key, false)
    var playerPlayButtonType by rememberPreference(
        PLAYER_PLAY_BUTTON_TYPE.key,
        PlayerPlayButtonType.Disabled
    )
    var bottomgradient by rememberPreference(BOTTOM_GRADIENT.key, false)
    var textoutline by rememberPreference(TEXT_OUTLINE.key, false)

    var lastPlayerPlayButtonType by rememberPreference(
        LAST_PLAYER_PLAY_BUTTON_TYPE.key,
        PlayerPlayButtonType.Rectangular
    )
    var disablePlayerHorizontalSwipe by rememberPreference(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, false)

    var disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    var showLikeButtonBackgroundPlayer by rememberPreference(
        SHOW_LIKE_BUTTON_BACKGROUND_PLAYER.key,
        true
    )

    var visualizerEnabled by rememberPreference(VISUALIZER_ENABLED.key, false)
    /*
    var playerVisualizerType by rememberPreference(
        playerVisualizerTypeKey.key,
        PlayerVisualizerType.Disabled
    )
    */
    var playerTimelineType by rememberPreference(PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    var playerThumbnailSize by rememberPreference(
        PLAYER_THUMBNAIL_SIZE.key,
        PlayerThumbnailSize.Biggest
    )
    var playerThumbnailSizeL by rememberPreference(
        PLAYER_THUMBNAIL_SIZE_L.key,
        PlayerThumbnailSize.Biggest
    )
    var playerTimelineSize by rememberPreference(
        PLAYER_TIMELINE_SIZE.key,
        PlayerTimelineSize.Biggest
    )

    var seekWithTap by rememberPreference(
        SEEK_WITH_TAP.key,
        true
    )
    //


    var thumbnailTapEnabled by rememberPreference(THUMBNAIL_TAP_ENABLED.key, true)


    var showButtonPlayerAddToPlaylist by rememberPreference(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true)
    var showButtonPlayerArrow by rememberPreference(SHOW_BUTTON_PLAYER_ARROW.key, true)
    //var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey.key, true)
    var showButtonPlayerLoop by rememberPreference(SHOW_BUTTON_PLAYER_LOOP.key, true)
    var showButtonPlayerLyrics by rememberPreference(SHOW_BUTTON_PLAYER_LYRICS.key, true)
    var expandedplayertoggle by rememberPreference(EXPANDED_PLAYER_TOGGLE.key, true)
    var showButtonPlayerShuffle by rememberPreference(SHOW_BUTTON_PLAYER_SHUFFLE.key, true)
    var showButtonPlayerSleepTimer by rememberPreference(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key, false)
    var showButtonPlayerMenu by rememberPreference(SHOW_BUTTON_PLAYER_MENU.key, false)
    var showButtonPlayerStartradio by rememberPreference(SHOW_BUTTON_PLAYER_START_RADIO.key, false)
    var showButtonPlayerSystemEqualizer by rememberPreference(
        SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key,
        false
    )
    var showButtonPlayerDiscover by rememberPreference(SHOW_BUTTON_PLAYER_DISCOVER.key, false)
    var showButtonPlayerVideo by rememberPreference(SHOW_BUTTON_PLAYER_VIDEO.key, true)

    val navigationBarPosition by rememberPreference(
        NAVIGATION_BAR_POSITION.key,
        NavigationBarPosition.Bottom
    )

    //var isGradientBackgroundEnabled by rememberPreference(isGradientBackgroundEnabledKey.key, false)
    var showTotalTimeQueue by rememberPreference(SHOW_TOTAL_TIME_QUEUE.key, true)
    var backgroundProgress by rememberPreference(
        BACKGROUND_PROGRESS.key,
        BackgroundProgress.MiniPlayer
    )
    var showNextSongsInPlayer by rememberPreference(SHOW_NEXT_SONGS_IN_PLAYER.key, false)
    var showRemainingSongTime by rememberPreference(SHOW_REMAINING_SONG_TIME.key, true)
    var clickLyricsText by rememberPreference(CLICK_ON_LYRICS_TEXT.key, true)
    var showBackgroundLyrics by rememberPreference(SHOW_BACKGROUND_LYRICS.key, false)

    val search = Search.init()

    var thumbnailRoundness by rememberPreference(
        THUMBNAIL_ROUNDNESS.key,
        ThumbnailRoundness.Light
    )

    var miniPlayerType by rememberPreference(
        MINI_PLAYER_TYPE.key,
        MiniPlayerType.Modern
    )
    var playerBackgroundColors by rememberPreference(
        PLAYER_BACKGROUND_COLORS.key,
        PlayerBackgroundColors.BlurredCoverColor
    )

    var showTopActionsBar by rememberPreference(SHOW_TOP_ACTIONS_BAR.key, true)
    var showPlayerActionsBar by rememberPreference(SHOW_PLAYER_ACTIONS_BAR.key, true)

    var playerControlsType by rememberPreference(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
    var playerInfoType by rememberPreference(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential)
    var transparentBackgroundActionBarPlayer by rememberPreference(
        TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
        true
    )
    var iconLikeType by rememberPreference(ICON_LIKE_TYPE.key, IconLikeType.Essential)
    var playerSwapControlsWithTimeline by rememberPreference(
        PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
        false
    )
    var playerEnableLyricsPopupMessage by rememberPreference(
        PLAYER_ENABLE_LYRICS_POPUP_MESSAGE.key,
        true
    )
    var actionspacedevenly by rememberPreference(ACTIONS_SPACED_EVENLY.key, false)
    var thumbnailType by rememberPreference(THUMBNAIL_TYPE.key, ThumbnailType.Modern)
    var showvisthumbnail by rememberPreference(SHOW_VIS_THUMBNAIL.key, false)
    var buttonzoomout by rememberPreference(BUTTON_ZOOM_OUT.key, false)
    var thumbnailpause by rememberPreference(THUMBNAIL_PAUSE.key, false)
    var showsongs by rememberPreference(SHOW_SONGS.key, SongsNumber.`2`)
    var showalbumcover by rememberPreference(SHOW_ALBUM_COVER.key, true)
    var prevNextSongs by rememberPreference(PREV_NEXT_SONGS.key, PrevNextSongs.twosongs)
    var tapqueue by rememberPreference(TAP_QUEUE.key, true)
    var swipeUpQueue by rememberPreference(SWIPE_UP_QUEUE.key, true)
    var statsfornerds by rememberPreference(STATS_FOR_NERDS.key, false)

    var playerType by rememberPreference(PLAYER_TYPE.key, PlayerType.Modern)
    var queueType by rememberPreference(QUEUE_TYPE.key, QueueType.Modern)
    var noblur by rememberPreference(NO_BLUR.key, true)
    var fadingedge by rememberPreference(FADING_EDGE.key, false)
    var carousel by rememberPreference(CAROUSEL.key, true)
    var carouselSize by rememberPreference(CAROUSEL_SIZE.key, CarouselSize.Biggest)
    var keepPlayerMinimized by rememberPreference(KEEP_PLAYER_MINIMIZED.key,false)
    var playerInfoShowIcons by rememberPreference(PLAYER_INFO_SHOW_ICONS.key, true)
    var queueDurationExpanded by rememberPreference(QUEUE_DURATION_EXPANDED.key, true)
    var titleExpanded by rememberPreference(TITLE_EXPANDED.key, true)
    var timelineExpanded by rememberPreference(TIMELINE_EXPANDED.key, true)
    var controlsExpanded by rememberPreference(CONTROLS_EXPANDED.key, true)
    var miniQueueExpanded by rememberPreference(MINI_QUEUE_EXPANDED.key, true)
    var statsExpanded by rememberPreference(STATS_EXPANDED.key, true)
    var actionExpanded by rememberPreference(ACTION_EXPANDED.key, true)
    var restartService by rememberSaveable { mutableStateOf(false) }
    var restartActivity by rememberSaveable { mutableStateOf(false) }
    var showCoverThumbnailAnimation by rememberPreference(SHOW_COVER_THUMBNAIL_ANIMATION.key, false)
    var coverThumbnailAnimation by rememberPreference(COVER_THUMBNAIL_ANIMATION.key, ThumbnailCoverType.Vinyl)

    var notificationPlayerFirstIcon by rememberPreference(NOTIFICATION_PLAYER_FIRST_ICON.key, NotificationButtons.Repeat)
    var notificationPlayerSecondIcon by rememberPreference(NOTIFICATION_PLAYER_SECOND_ICON.key, NotificationButtons.Favorites)
    var enableWallpaper by rememberPreference(ENABLE_WALLPAPER.key, false)
    var wallpaperType by rememberPreference(WALLPAPER_TYPE.key, WallpaperType.Lockscreen)
    var topPadding by rememberPreference(TOP_PADDING.key, true)
    var animatedGradient by rememberPreference(
        ANIMATED_GRADIENT.key,
        AnimatedGradient.Linear
    )
    var appearanceChooser by remember{ mutableStateOf(false)}
    var albumCoverRotation by rememberPreference(ALBUM_COVER_ROTATION.key, false)

    var blurStrength by rememberPreference(BLUR_SCALE.key, 25f)
    var thumbnailFadeEx  by rememberPreference(THUMBNAIL_FADE_EX.key, 5f)
    var thumbnailFade  by rememberPreference(THUMBNAIL_FADE.key, 5f)
    var thumbnailSpacing  by rememberPreference(THUMBNAIL_SPACING.key, 0f)
    var colorPaletteName by rememberPreference(COLOR_PALETTE_NAME.key, ColorPaletteName.Dynamic)
    var colorPaletteMode by rememberPreference(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
    var swipeAnimationNoThumbnail by rememberPreference(SWIPE_ANIMATIONS_NO_THUMBNAIL.key, SwipeAnimationNoThumbnail.Sliding)

    var appearanceFilename by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            context.applicationContext.contentResolver.openOutputStream(uri)
                ?.use { outputStream ->
                    /* new feature themes repo
                    outputStream.bufferedWriter().use { writer ->
                        writer.write(AppearanceSettings.fromCurrentSettings(context).toShareString())
                    }
                     */

                    csvWriter().open(outputStream){
                        writeRow("SettingsType", "Name", "Parameter", "Value")
                        writeRow("Appearance", appearanceFilename, "albumCoverRotation", albumCoverRotation)
                        writeRow("Appearance", appearanceFilename, "showthumbnail", showthumbnail)
                        writeRow("Appearance", appearanceFilename, "playerBackgroundColors", playerBackgroundColors.ordinal)
                        writeRow("Appearance", appearanceFilename, "thumbnailRoundness", thumbnailRoundness.ordinal)
                        writeRow("Appearance", appearanceFilename, "playerType", playerType.ordinal)
                        writeRow("Appearance", appearanceFilename, "queueType", queueType.ordinal)
                        writeRow("Appearance", appearanceFilename, "noblur", noblur)
                        writeRow("Appearance", appearanceFilename, "fadingedge", fadingedge)
                        writeRow("Appearance", appearanceFilename, "carousel", carousel)
                        writeRow("Appearance", appearanceFilename, "carouselSize", carouselSize.ordinal)
                        writeRow("Appearance", appearanceFilename, "keepPlayerMinimized", keepPlayerMinimized)
                        writeRow("Appearance", appearanceFilename, "playerInfoShowIcons", playerInfoShowIcons)
                        writeRow("Appearance", appearanceFilename, "showTopActionsBar", showTopActionsBar)
                        writeRow("Appearance", appearanceFilename, "playerControlsType", playerControlsType.ordinal)
                        writeRow("Appearance", appearanceFilename, "playerInfoType", playerInfoType.ordinal)
                        writeRow("Appearance", appearanceFilename, "transparentBackgroundActionBarPlayer", transparentBackgroundActionBarPlayer)
                        writeRow("Appearance", appearanceFilename, "iconLikeType", iconLikeType.ordinal)
                        writeRow("Appearance", appearanceFilename, "playerSwapControlsWithTimeline", playerSwapControlsWithTimeline)
                        writeRow("Appearance", appearanceFilename, "playerEnableLyricsPopupMessage", playerEnableLyricsPopupMessage)
                        writeRow("Appearance", appearanceFilename, "actionspacedevenly", actionspacedevenly)
                        writeRow("Appearance", appearanceFilename, "thumbnailType", thumbnailType.ordinal)
                        writeRow("Appearance", appearanceFilename, "showvisthumbnail", showvisthumbnail)
                        writeRow("Appearance", appearanceFilename, "buttonzoomout", buttonzoomout)
                        writeRow("Appearance", appearanceFilename, "thumbnailpause", thumbnailpause)
                        writeRow("Appearance", appearanceFilename, "showsongs", showsongs.ordinal)
                        writeRow("Appearance", appearanceFilename, "showalbumcover", showalbumcover)
                        writeRow("Appearance", appearanceFilename, "prevNextSongs", prevNextSongs.ordinal)
                        writeRow("Appearance", appearanceFilename, "tapqueue", tapqueue)
                        writeRow("Appearance", appearanceFilename, "swipeUpQueue", swipeUpQueue)
                        writeRow("Appearance", appearanceFilename, "statsfornerds", statsfornerds)
                        writeRow("Appearance", appearanceFilename, "transparentbar", transparentbar)
                        writeRow("Appearance", appearanceFilename, "blackgradient", blackgradient)
                        writeRow("Appearance", appearanceFilename, "showlyricsthumbnail", showlyricsthumbnail)
                        writeRow("Appearance", appearanceFilename, "expandedplayer", expandedplayer)
                        writeRow("Appearance", appearanceFilename, "playerPlayButtonType", playerPlayButtonType.ordinal)
                        writeRow("Appearance", appearanceFilename, "bottomgradient", bottomgradient)
                        writeRow("Appearance", appearanceFilename, "textoutline", textoutline)
                        writeRow("Appearance", appearanceFilename, "thumbnailTapEnabled", thumbnailTapEnabled)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerAddToPlaylist", showButtonPlayerAddToPlaylist)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerArrow", showButtonPlayerArrow)
                        //writeRow("Appearance", appearanceFilename, "showButtonPlayerDownload", showButtonPlayerDownload)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerLoop", showButtonPlayerLoop)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerLyrics", showButtonPlayerLyrics)
                        writeRow("Appearance", appearanceFilename, "expandedplayertoggle", expandedplayertoggle)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerShuffle", showButtonPlayerShuffle)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerSleepTimer", showButtonPlayerSleepTimer)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerMenu", showButtonPlayerMenu)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerStartradio", showButtonPlayerStartradio)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerSystemEqualizer", showButtonPlayerSystemEqualizer)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerDiscover", showButtonPlayerDiscover)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerVideo", showButtonPlayerVideo)
                        writeRow("Appearance", appearanceFilename, "showBackgroundLyrics", showBackgroundLyrics)
                        writeRow("Appearance", appearanceFilename, "showTotalTimeQueue", showTotalTimeQueue)
                        writeRow("Appearance", appearanceFilename, "backgroundProgress", backgroundProgress.ordinal)
                        writeRow("Appearance", appearanceFilename, "showNextSongsInPlayer", showNextSongsInPlayer)
                        writeRow("Appearance", appearanceFilename, "showRemainingSongTime", showRemainingSongTime)
                        writeRow("Appearance", appearanceFilename, "clickLyricsText", clickLyricsText)
                        writeRow("Appearance", appearanceFilename, "queueDurationExpanded", queueDurationExpanded)
                        writeRow("Appearance", appearanceFilename, "titleExpanded", titleExpanded)
                        writeRow("Appearance", appearanceFilename, "timelineExpanded", timelineExpanded)
                        writeRow("Appearance", appearanceFilename, "controlsExpanded", controlsExpanded)
                        writeRow("Appearance", appearanceFilename, "miniQueueExpanded", miniQueueExpanded)
                        writeRow("Appearance", appearanceFilename, "statsExpanded", statsExpanded)
                        writeRow("Appearance", appearanceFilename, "actionExpanded", actionExpanded)
                        writeRow("Appearance", appearanceFilename, "showCoverThumbnailAnimation", showCoverThumbnailAnimation)
                        writeRow("Appearance", appearanceFilename, "coverThumbnailAnimation", coverThumbnailAnimation.ordinal)
                        writeRow("Appearance", appearanceFilename, "notificationPlayerFirstIcon", notificationPlayerFirstIcon.ordinal)
                        writeRow("Appearance", appearanceFilename, "notificationPlayerSecondIcon", notificationPlayerSecondIcon.ordinal)
                        writeRow("Appearance", appearanceFilename, "enableWallpaper", enableWallpaper)
                        writeRow("Appearance", appearanceFilename, "wallpaperType", wallpaperType.ordinal)
                        writeRow("Appearance", appearanceFilename, "topPadding", topPadding)
                        writeRow("Appearance", appearanceFilename, "animatedGradient", animatedGradient.ordinal)
                        writeRow("Appearance", appearanceFilename, "albumCoverRotation", albumCoverRotation)
                        writeRow("Appearance", appearanceFilename, "blurStrength", blurStrength)
                        writeRow("Appearance", appearanceFilename, "thumbnailFadeEx", thumbnailFadeEx)
                        writeRow("Appearance", appearanceFilename, "thumbnailFade", thumbnailFade)
                        writeRow("Appearance", appearanceFilename, "thumbnailSpacing", thumbnailSpacing)
                        writeRow("Appearance", appearanceFilename, "colorPaletteName", colorPaletteName.ordinal)
                        writeRow("Appearance", appearanceFilename, "colorPaletteMode", colorPaletteMode.ordinal)
                        writeRow("Appearance", appearanceFilename, "swipeAnimationNoThumbnail", swipeAnimationNoThumbnail.ordinal)
                        writeRow("Appearance", appearanceFilename, "showLikeButtonBackgroundPlayer", showLikeButtonBackgroundPlayer)
                        writeRow("Appearance", appearanceFilename, "visualizerEnabled", visualizerEnabled)
                    }

                }

        }

    var isExporting by rememberSaveable {
        mutableStateOf(false)
    }


    if (isExporting) {
        InputTextDialog(
            onDismiss = {
                isExporting = false
            },
            title = "Enter the name of settings export",
            value = "RP_Appearance",
            placeholder = "Enter the name of settings export",
            setValue = { text ->
                appearanceFilename = text
                try {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    exportLauncher.launch("RPAppearance_${text.take(20)}_${dateFormat.format(
                        Date()
                    )}")
                } catch (e: ActivityNotFoundException) {
                    SmartMessage("Couldn't find an application to create documents",
                        type = PopupType.Warning, context = context)
                }
            }
        )
    }

    val preferences = remember { AppearancePreferences.getInstance(context) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            //requestPermission(activity, "Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED")

            context.applicationContext.contentResolver.openInputStream(uri)
                ?.use { inputStream ->
                    /* NEW FEATURE THEMES REPO
                    runCatching {
                        val encoded = inputStream.bufferedReader().readText()
                        AppearanceSettings.fromShareString(encoded)
                    }
                        .onSuccess { settings -> preferences.applyFrom(settings) }
                        .onFailure { Timber.e("PlayerAppearanceSettings failed to load appearance from file") }

                     */

                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().forEachIndexed { index, row: Map<String, String> ->
                            if (row["SettingsType"] == "Appearance") {
                                println("Import appearance settings parameter ${row["Parameter"]}")
                                when (row["Parameter"]) {
                                    "showthumbnail" -> {
                                        showthumbnail = row["Value"].toBoolean()
                                    }
                                    "playerBackgroundColors" -> {
                                        playerBackgroundColors = PlayerBackgroundColors.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "thumbnailRoundness" -> {
                                        thumbnailRoundness = ThumbnailRoundness.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "playerType" -> {
                                        playerType = PlayerType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "queueType" -> {
                                        queueType = QueueType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "noblur" -> {
                                        noblur = row["Value"].toBoolean()
                                    }
                                    "fadingedge" -> {
                                        fadingedge = row["Value"].toBoolean()
                                    }
                                    "carousel" -> {
                                        carousel = row["Value"].toBoolean()
                                    }
                                    "carouselSize" -> {
                                        carouselSize =
                                            CarouselSize.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "keepPlayerMinimized" -> {
                                        keepPlayerMinimized = row["Value"].toBoolean()
                                    }
                                    "playerInfoShowIcons" -> {
                                        playerInfoShowIcons = row["Value"].toBoolean()
                                    }
                                    "showTopActionsBar" -> {
                                        showTopActionsBar = row["Value"].toBoolean()
                                    }
                                    "playerControlsType" -> {
                                        playerControlsType = PlayerControlsType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "playerInfoType" -> {
                                        playerInfoType = PlayerInfoType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "transparentBackgroundActionBarPlayer" -> {
                                        transparentBackgroundActionBarPlayer = row["Value"].toBoolean()
                                    }
                                    "iconLikeType" -> {
                                        iconLikeType = IconLikeType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "playerSwapControlsWithTimeline" -> {
                                        playerSwapControlsWithTimeline = row["Value"].toBoolean()
                                    }
                                    "playerEnableLyricsPopupMessage" -> {
                                        playerEnableLyricsPopupMessage = row["Value"].toBoolean()
                                    }
                                    "actionspacedevenly" -> {
                                        actionspacedevenly = row["Value"].toBoolean()
                                    }
                                    "thumbnailType" -> {
                                        thumbnailType = ThumbnailType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "showvisthumbnail" -> {
                                        showvisthumbnail = row["Value"].toBoolean()
                                    }
                                    "buttonzoomout" -> {
                                        buttonzoomout = row["Value"].toBoolean()
                                    }
                                    "thumbnailpause" -> {
                                        thumbnailpause = row["Value"].toBoolean()
                                    }
                                    "showsongs" -> {
                                        showsongs = SongsNumber.entries.toTypedArray()[row["Value"]!!.toInt()]
                                    }
                                    "showalbumcover" -> {
                                        showalbumcover = row["Value"].toBoolean()
                                    }
                                    "prevNextSongs" -> {
                                        prevNextSongs = PrevNextSongs.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "tapqueue" -> {
                                        tapqueue = row["Value"].toBoolean()
                                    }
                                    "swipeUpQueue" -> {
                                        swipeUpQueue = row["Value"].toBoolean()
                                    }
                                    "statsfornerds" -> {
                                        statsfornerds = row["Value"].toBoolean()
                                    }
                                    "transparentbar" -> {
                                        transparentbar = row["Value"].toBoolean()
                                    }
                                    "blackgradient" -> {
                                        blackgradient = row["Value"].toBoolean()
                                    }
                                    "showlyricsthumbnail" -> {
                                        showlyricsthumbnail = row["Value"].toBoolean()
                                    }
                                    "expandedplayer" -> {
                                        expandedplayer = row["Value"].toBoolean()
                                    }
                                    "playerPlayButtonType" -> {
                                        playerPlayButtonType = PlayerPlayButtonType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "bottomgradient" -> {
                                        bottomgradient = row["Value"].toBoolean()
                                    }
                                    "textoutline" -> {
                                        textoutline = row["Value"].toBoolean()
                                    }
                                    "thumbnailTapEnabled" -> {
                                        thumbnailTapEnabled = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerAddToPlaylist" -> {
                                        showButtonPlayerAddToPlaylist = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerArrow" -> {
                                        showButtonPlayerArrow = row["Value"].toBoolean()
                                    }
//                                    "showButtonPlayerDownload" -> {
//                                        showButtonPlayerDownload = row["Value"].toBoolean()
//                                    }
                                    "showButtonPlayerLoop" -> {
                                        showButtonPlayerLoop = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerLyrics" -> {
                                        showButtonPlayerLyrics = row["Value"].toBoolean()
                                    }
                                    "expandedplayertoggle" -> {
                                        expandedplayertoggle = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerShuffle" -> {
                                        showButtonPlayerShuffle = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerSleepTimer" -> {
                                        showButtonPlayerSleepTimer = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerMenu" -> {
                                        showButtonPlayerMenu = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerStartradio" -> {
                                        showButtonPlayerStartradio = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerSystemEqualizer" -> {
                                        showButtonPlayerSystemEqualizer = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerDiscover" -> {
                                        showButtonPlayerDiscover = row["Value"].toBoolean()
                                    }
                                    "showButtonPlayerVideo" -> {
                                        showButtonPlayerVideo = row["Value"].toBoolean()
                                    }
                                    "showBackgroundLyrics" -> {
                                        showBackgroundLyrics = row["Value"].toBoolean()
                                    }
                                    "showTotalTimeQueue" -> {
                                        showTotalTimeQueue = row["Value"].toBoolean()
                                    }
                                    "backgroundProgress" -> {
                                        backgroundProgress = BackgroundProgress.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "showNextSongsInPlayer" -> {
                                        showNextSongsInPlayer = row["Value"].toBoolean()
                                    }
                                    "showRemainingSongTime" -> {
                                        showRemainingSongTime = row["Value"].toBoolean()
                                    }
                                    "clickLyricsText" -> {
                                        clickLyricsText = row["Value"].toBoolean()
                                    }
                                    "queueDurationExpanded" -> {
                                        queueDurationExpanded = row["Value"].toBoolean()
                                    }
                                    "titleExpanded" -> {
                                        titleExpanded = row["Value"].toBoolean()
                                    }
                                    "timelineExpanded" -> {
                                        timelineExpanded = row["Value"].toBoolean()
                                    }
                                    "controlsExpanded" -> {
                                        controlsExpanded = row["Value"].toBoolean()
                                    }
                                    "miniQueueExpanded" -> {
                                        miniQueueExpanded = row["Value"].toBoolean()
                                    }
                                    "statsExpanded" -> {
                                        statsExpanded = row["Value"].toBoolean()
                                    }
                                    "actionExpanded" -> {
                                        actionExpanded = row["Value"].toBoolean()
                                    }
                                    "showCoverThumbnailAnimation" -> {
                                        showCoverThumbnailAnimation = row["Value"].toBoolean()
                                    }
                                    "coverThumbnailAnimation" -> {
                                        coverThumbnailAnimation =
                                            ThumbnailCoverType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "notificationPlayerFirstIcon" -> {
                                        notificationPlayerFirstIcon =
                                            NotificationButtons.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "notificationPlayerSecondIcon" -> {
                                        notificationPlayerSecondIcon =
                                            NotificationButtons.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "enableWallpaper" -> {
                                        enableWallpaper = row["Value"].toBoolean()
                                    }
                                    "wallpaperType" -> {
                                        wallpaperType = WallpaperType.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "topPadding" -> {
                                        topPadding = row["Value"].toBoolean()
                                    }
                                    "animatedGradient" -> {
                                        animatedGradient = AnimatedGradient.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "albumCoverRotation" -> {
                                        albumCoverRotation = row["Value"].toBoolean()
                                    }
                                    "blurStrength" -> {
                                        blurStrength = row["Value"]?.toFloat() ?: 0f
                                    }
                                    "thumbnailFadeEx" -> {
                                        thumbnailFadeEx = row["Value"]?.toFloat() ?: 0f
                                    }
                                    "thumbnailFade" -> {
                                        thumbnailFade = row["Value"]?.toFloat() ?: 0f
                                    }
                                    "thumbnailSpacing" -> {
                                        thumbnailSpacing = row["Value"]?.toFloat() ?: 0f
                                    }
                                    "colorPaletteName" -> {
                                        colorPaletteName =
                                            ColorPaletteName.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "colorPaletteMode" -> {
                                        colorPaletteMode =
                                            ColorPaletteMode.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "swipeAnimationNoThumbnail" -> {
                                        swipeAnimationNoThumbnail =
                                            SwipeAnimationNoThumbnail.entries.toTypedArray()[row["Value"]?.toInt() ?: 0]
                                    }
                                    "showLikeButtonBackgroundPlayer" -> {
                                        showLikeButtonBackgroundPlayer = row["Value"].toBoolean()
                                    }
                                    "visualizerEnabled" -> {
                                        visualizerEnabled = row["Value"].toBoolean()
                                    }




                                }
                            }

                        }
                    }

                }
        }

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
            //.verticalScroll(rememberScrollState())
        /*
        .padding(
            LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues()
        )

         */
    ) {

        if (playerBackgroundColors != PlayerBackgroundColors.BlurredCoverColor)
            showthumbnail = true
        if (!visualizerEnabled) showvisthumbnail = false
        if (!showthumbnail) {
            showlyricsthumbnail = false; showvisthumbnail = false
        }
        if (playerType == PlayerType.Modern) {
            showlyricsthumbnail = false
            showvisthumbnail = false
            thumbnailpause = false
            //keepPlayerMinimized = false
        }

        /* NEW FEATURE THEMES REPO
        CustomModalBottomSheet(
            showSheet = appearanceChooser,
            onDismissRequest = { appearanceChooser = false },
            containerColor = colorPalette().background0,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                ) {}
            },
            shape = thumbnailRoundness.shape(),
        ) {
            AppearancePresetDialogHost(context) { appearanceChooser = false }
        }
         */


        if (appearanceChooser) {

            AppearancePresetDialog(
                onDismiss = { appearanceChooser = false },
                onClick0 = {
                    showTopActionsBar = true
                    showthumbnail = true
                    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                    blurStrength = 50f
                    thumbnailRoundness = ThumbnailRoundness.None
                    playerInfoType = PlayerInfoType.Essential
                    playerTimelineType = PlayerTimelineType.ThinBar
                    playerTimelineSize = PlayerTimelineSize.Biggest
                    playerControlsType = PlayerControlsType.Essential
                    playerPlayButtonType = PlayerPlayButtonType.Disabled
                    transparentbar = true
                    playerType = PlayerType.Essential
                    showlyricsthumbnail = false
                    expandedplayer = true
                    thumbnailType = ThumbnailType.Modern
                    playerThumbnailSize = PlayerThumbnailSize.Big
                    showTotalTimeQueue = false
                    bottomgradient = true
                    showRemainingSongTime = true
                    showNextSongsInPlayer = false
                    colorPaletteName = ColorPaletteName.Dynamic
                    colorPaletteMode = ColorPaletteMode.System
                    ///////ACTION BAR BUTTONS////////////////
                    transparentBackgroundActionBarPlayer = true
                    actionspacedevenly = true
                    showButtonPlayerVideo = false
                    showButtonPlayerDiscover = false
                    //showButtonPlayerDownload = false
                    showButtonPlayerAddToPlaylist = true
                    showButtonPlayerLoop = false
                    showButtonPlayerShuffle = true
                    showButtonPlayerLyrics = false
                    expandedplayertoggle = false
                    showButtonPlayerSleepTimer = false
                    visualizerEnabled = false
                    appearanceChooser = false
                    showButtonPlayerArrow = false
                    showButtonPlayerStartradio = false
                    showButtonPlayerMenu = true
                    ///////////////////////////
                    appearanceChooser = false
                },
                onClick1 = {
                    showTopActionsBar = true
                    showthumbnail = true
                    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                    blurStrength = 50f
                    playerInfoType = PlayerInfoType.Essential
                    playerPlayButtonType = PlayerPlayButtonType.Disabled
                    playerTimelineType = PlayerTimelineType.ThinBar
                    playerControlsType = PlayerControlsType.Essential
                    transparentbar = true
                    playerType = PlayerType.Modern
                    expandedplayer = true
                    fadingedge = true
                    thumbnailFadeEx = 4f
                    thumbnailSpacing = -32f
                    thumbnailType = ThumbnailType.Essential
                    carouselSize = CarouselSize.Big
                    playerThumbnailSize = PlayerThumbnailSize.Biggest
                    showTotalTimeQueue = false
                    transparentBackgroundActionBarPlayer = true
                    showRemainingSongTime = true
                    bottomgradient = true
                    showlyricsthumbnail = false
                    thumbnailRoundness = ThumbnailRoundness.Medium
                    showNextSongsInPlayer = true
                    colorPaletteName = ColorPaletteName.Dynamic
                    colorPaletteMode = ColorPaletteMode.System
                    ///////ACTION BAR BUTTONS////////////////
                    transparentBackgroundActionBarPlayer = true
                    actionspacedevenly = true
                    showButtonPlayerVideo = false
                    showButtonPlayerDiscover = false
                    //showButtonPlayerDownload = false
                    showButtonPlayerAddToPlaylist = true
                    showButtonPlayerLoop = false
                    showButtonPlayerShuffle = false
                    showButtonPlayerLyrics = false
                    expandedplayertoggle = true
                    showButtonPlayerSleepTimer = false
                    visualizerEnabled = false
                    appearanceChooser = false
                    showButtonPlayerArrow = false
                    showButtonPlayerStartradio = false
                    showButtonPlayerMenu = true
                    ///////////////////////////
                    appearanceChooser = false
                },
                onClick2 = {
                    showTopActionsBar = false
                    showthumbnail = false
                    noblur = true
                    topPadding = false
                    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                    blurStrength = 50f
                    playerPlayButtonType = PlayerPlayButtonType.Disabled
                    playerInfoType = PlayerInfoType.Modern
                    playerInfoShowIcons = false
                    playerTimelineType = PlayerTimelineType.ThinBar
                    playerControlsType = PlayerControlsType.Essential
                    transparentbar = true
                    playerType = PlayerType.Modern
                    expandedplayer = true
                    showTotalTimeQueue = false
                    transparentBackgroundActionBarPlayer = true
                    showRemainingSongTime = true
                    bottomgradient = true
                    showlyricsthumbnail = false
                    showNextSongsInPlayer = false
                    colorPaletteName = ColorPaletteName.Dynamic
                    colorPaletteMode = ColorPaletteMode.System
                    ///////ACTION BAR BUTTONS////////////////
                    transparentBackgroundActionBarPlayer = true
                    actionspacedevenly = true
                    showButtonPlayerVideo = false
                    showButtonPlayerDiscover = false
                    //showButtonPlayerDownload = false
                    showButtonPlayerAddToPlaylist = false
                    showButtonPlayerLoop = false
                    showButtonPlayerShuffle = false
                    showButtonPlayerLyrics = false
                    expandedplayertoggle = false
                    showButtonPlayerSleepTimer = false
                    visualizerEnabled = false
                    appearanceChooser = false
                    showButtonPlayerArrow = false
                    showButtonPlayerStartradio = false
                    showButtonPlayerMenu = true
                    ///////////////////////////
                    appearanceChooser = false
                },
                onClick3 = {
                    showTopActionsBar = false
                    topPadding = false
                    showthumbnail = true
                    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                    blurStrength = 50f
                    playerInfoType = PlayerInfoType.Essential
                    playerTimelineType = PlayerTimelineType.FakeAudioBar
                    playerTimelineSize = PlayerTimelineSize.Biggest
                    playerControlsType = PlayerControlsType.Modern
                    playerPlayButtonType = PlayerPlayButtonType.Disabled
                    colorPaletteName = ColorPaletteName.PureBlack
                    transparentbar = false
                    playerType = PlayerType.Essential
                    expandedplayer = false
                    playerThumbnailSize = PlayerThumbnailSize.Expanded
                    showTotalTimeQueue = false
                    transparentBackgroundActionBarPlayer = true
                    showRemainingSongTime = true
                    bottomgradient = true
                    showlyricsthumbnail = false
                    thumbnailType = ThumbnailType.Essential
                    thumbnailRoundness = ThumbnailRoundness.Light
                    playerType = PlayerType.Modern
                    fadingedge = true
                    thumbnailFade = 5f
                    showNextSongsInPlayer = false
                    ///////ACTION BAR BUTTONS////////////////
                    transparentBackgroundActionBarPlayer = true
                    actionspacedevenly = true
                    showButtonPlayerVideo = false
                    showButtonPlayerDiscover = false
                    //showButtonPlayerDownload = false
                    showButtonPlayerAddToPlaylist = false
                    showButtonPlayerLoop = true
                    showButtonPlayerShuffle = true
                    showButtonPlayerLyrics = false
                    expandedplayertoggle = false
                    showButtonPlayerSleepTimer = false
                    visualizerEnabled = false
                    appearanceChooser = false
                    showButtonPlayerArrow = true
                    showButtonPlayerStartradio = false
                    showButtonPlayerMenu = true
                    ///////////////////////////
                    appearanceChooser = false
                },
                onClick4 = {
                    showTopActionsBar = false
                    topPadding = true
                    showthumbnail = true
                    playerBackgroundColors = PlayerBackgroundColors.AnimatedGradient
                    animatedGradient = AnimatedGradient.Linear
                    playerInfoType = PlayerInfoType.Essential
                    playerTimelineType = PlayerTimelineType.PinBar
                    playerTimelineSize = PlayerTimelineSize.Biggest
                    playerControlsType = PlayerControlsType.Essential
                    playerPlayButtonType = PlayerPlayButtonType.Square
                    colorPaletteName = ColorPaletteName.Dynamic
                    colorPaletteMode = ColorPaletteMode.PitchBlack
                    transparentbar = false
                    playerType = PlayerType.Modern
                    expandedplayer = false
                    playerThumbnailSize = PlayerThumbnailSize.Biggest
                    showTotalTimeQueue = false
                    transparentBackgroundActionBarPlayer = true
                    showRemainingSongTime = true
                    showlyricsthumbnail = false
                    thumbnailType = ThumbnailType.Modern
                    thumbnailRoundness = ThumbnailRoundness.Light
                    fadingedge = true
                    thumbnailFade = 0f
                    thumbnailFadeEx = 5f
                    thumbnailSpacing = -32f
                    showNextSongsInPlayer = false
                    ///////ACTION BAR BUTTONS////////////////
                    transparentBackgroundActionBarPlayer = true
                    actionspacedevenly = true
                    showButtonPlayerVideo = false
                    showButtonPlayerDiscover = false
                    //showButtonPlayerDownload = true
                    showButtonPlayerAddToPlaylist = false
                    showButtonPlayerLoop = false
                    showButtonPlayerShuffle = false
                    showButtonPlayerLyrics = false
                    expandedplayertoggle = true
                    showButtonPlayerSleepTimer = false
                    visualizerEnabled = false
                    appearanceChooser = false
                    showButtonPlayerArrow = false
                    showButtonPlayerStartradio = false
                    showButtonPlayerMenu = true
                    ///////////////////////////
                    appearanceChooser = false
                },
                onClick5 = {
                    showTopActionsBar = true
                    showthumbnail = true
                    playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
                    playerInfoType = PlayerInfoType.Essential
                    playerTimelineType = PlayerTimelineType.Wavy
                    playerTimelineSize = PlayerTimelineSize.Biggest
                    playerControlsType = PlayerControlsType.Essential
                    playerPlayButtonType = PlayerPlayButtonType.CircularRibbed
                    colorPaletteName = ColorPaletteName.Dynamic
                    colorPaletteMode = ColorPaletteMode.System
                    transparentbar = false
                    playerType = PlayerType.Essential
                    expandedplayer = true
                    playerThumbnailSize = PlayerThumbnailSize.Big
                    showTotalTimeQueue = false
                    transparentBackgroundActionBarPlayer = true
                    showRemainingSongTime = true
                    showlyricsthumbnail = false
                    thumbnailType = ThumbnailType.Modern
                    thumbnailRoundness = ThumbnailRoundness.Light
                    showNextSongsInPlayer = false
                    ///////ACTION BAR BUTTONS////////////////
                    transparentBackgroundActionBarPlayer = true
                    actionspacedevenly = true
                    showButtonPlayerVideo = false
                    showButtonPlayerDiscover = false
                    //showButtonPlayerDownload = false
                    showButtonPlayerAddToPlaylist = false
                    showButtonPlayerLoop = false
                    showButtonPlayerShuffle = true
                    showButtonPlayerLyrics = true
                    expandedplayertoggle = false
                    showButtonPlayerSleepTimer = false
                    visualizerEnabled = false
                    appearanceChooser = false
                    showButtonPlayerArrow = false
                    showButtonPlayerStartradio = false
                    showButtonPlayerMenu = true
                    ///////////////////////////
                    appearanceChooser = false
                }
            )
        }


        val state = rememberLazyListState()
        LazyListContainer(
            state = state
        ) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
            ) {
                settingsItem {
                    HeaderWithIcon(
                        title = stringResource(R.string.player_appearance),
                        iconId = R.drawable.color_palette,
                        enabled = false,
                        showIcon = true,
                        modifier = Modifier,
                        onClick = {}
                    )
                }

                settingsSearchBarItem {
                    search.ToolBarButton()
                    search.SearchBar(this)
                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsEntryGroupText(title = stringResource(R.string.player))
                }

                settingsItem {
                    if (!isLandscape) {
                        Column {
                            BasicText(
                                text = stringResource(R.string.appearancepresets),
                                style = typography().m.semiBold.copy(color = colorPalette().text),
                                modifier = Modifier
                                    .padding(all = 12.dp)
                                    .clickable(onClick = { appearanceChooser = true })
                            )
                            BasicText(
                                text = stringResource(R.string.appearancepresetssecondary),
                                style = typography().xs.semiBold.copy(color = colorPalette().textSecondary),
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .padding(bottom = 10.dp)
                            )
                        }

                        if (search.input.isBlank() || stringResource(R.string.show_player_top_actions_bar).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.show_player_top_actions_bar),
                                text = "",
                                isChecked = showTopActionsBar,
                                onCheckedChange = { showTopActionsBar = it }
                            )

                        if (!showTopActionsBar) {
                            if (search.input.isBlank() || stringResource(R.string.blankspace).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.blankspace),
                                    text = "",
                                    isChecked = topPadding,
                                    onCheckedChange = { topPadding = it }
                                )
                        }
                    }
                }


                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.playertype).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.playertype),
                            selectedValue = playerType,
                            onValueSelected = {
                                playerType = it
                            },
                            valueText = {
                                when (it) {
                                    PlayerType.Modern -> stringResource(R.string.pcontrols_modern)
                                    PlayerType.Essential -> stringResource(R.string.pcontrols_essential)
                                }
                            },
                        )

                    if (search.input.isBlank() || stringResource(R.string.queuetype).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.queuetype),
                            selectedValue = queueType,
                            onValueSelected = {
                                queueType = it
                            },
                            valueText = {
                                when (it) {
                                    QueueType.Modern -> stringResource(R.string.pcontrols_modern)
                                    QueueType.Essential -> stringResource(R.string.pcontrols_essential)
                                }
                            },
                        )

                    if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) {
                        if (search.input.isBlank() || stringResource(R.string.show_thumbnail).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.show_thumbnail),
                                text = "",
                                isChecked = showthumbnail,
                                onCheckedChange = { showthumbnail = it },
                            )
                    }
                    AnimatedVisibility(visible = !showthumbnail && playerType == PlayerType.Modern && !isLandscape) {
                        if (search.input.isBlank() || stringResource(R.string.swipe_Animation_No_Thumbnail).contains(
                                search.input,
                                true
                            )
                        )
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.swipe_Animation_No_Thumbnail),
                                selectedValue = swipeAnimationNoThumbnail,
                                onValueSelected = { swipeAnimationNoThumbnail = it },
                                valueText = {
                                    when (it) {
                                        SwipeAnimationNoThumbnail.Sliding -> stringResource(R.string.te_slide_vertical)
                                        SwipeAnimationNoThumbnail.Fade -> stringResource(R.string.te_fade)
                                        SwipeAnimationNoThumbnail.Scale -> stringResource(R.string.te_scale)
                                        SwipeAnimationNoThumbnail.Carousel -> stringResource(R.string.carousel)
                                        SwipeAnimationNoThumbnail.Circle -> stringResource(R.string.vt_circular)
                                    }
                                },
                                modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                            )
                    }
                    AnimatedVisibility(visible = showthumbnail) {
                        Column {
                            if (playerType == PlayerType.Modern) {
                                if (search.input.isBlank() || stringResource(R.string.fadingedge).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.fadingedge),
                                        text = "",
                                        isChecked = fadingedge,
                                        onCheckedChange = { fadingedge = it },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )
                            }

                            if (playerType == PlayerType.Modern && !isLandscape && (expandedplayertoggle || expandedplayer)) {
                                if (search.input.isBlank() || stringResource(R.string.carousel).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.carousel),
                                        text = "",
                                        isChecked = carousel,
                                        onCheckedChange = { carousel = it },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )

                                if (search.input.isBlank() || stringResource(R.string.carouselsize).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    EnumValueSelectorSettingsEntry(
                                        title = stringResource(R.string.carouselsize),
                                        selectedValue = carouselSize,
                                        onValueSelected = { carouselSize = it },
                                        valueText = {
                                            when (it) {
                                                CarouselSize.Small -> stringResource(R.string.small)
                                                CarouselSize.Medium -> stringResource(R.string.medium)
                                                CarouselSize.Big -> stringResource(R.string.big)
                                                CarouselSize.Biggest -> stringResource(R.string.biggest)
                                                CarouselSize.Expanded -> stringResource(R.string.expanded)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )
                            }
                            if (playerType == PlayerType.Essential) {

                                if (search.input.isBlank() || stringResource(R.string.thumbnailpause).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.thumbnailpause),
                                        text = "",
                                        isChecked = thumbnailpause,
                                        onCheckedChange = { thumbnailpause = it },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )

                                if (search.input.isBlank() || stringResource(R.string.show_lyrics_thumbnail).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.show_lyrics_thumbnail),
                                        text = "",
                                        isChecked = showlyricsthumbnail,
                                        onCheckedChange = { showlyricsthumbnail = it },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )
                                if (visualizerEnabled) {
                                    if (search.input.isBlank() || stringResource(R.string.showvisthumbnail).contains(
                                            search.input,
                                            true
                                        )
                                    )
                                        SwitchSettingEntry(
                                            title = stringResource(R.string.showvisthumbnail),
                                            text = "",
                                            isChecked = showvisthumbnail,
                                            onCheckedChange = { showvisthumbnail = it },
                                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                        )
                                }
                            }

                            if (search.input.isBlank() || stringResource(R.string.show_cover_thumbnail_animation).contains(
                                    search.input,
                                    true
                                )
                            ) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.show_cover_thumbnail_animation),
                                    text = "",
                                    isChecked = showCoverThumbnailAnimation,
                                    onCheckedChange = { showCoverThumbnailAnimation = it },
                                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                )
                                AnimatedVisibility(visible = showCoverThumbnailAnimation) {
                                    Column {
                                        EnumValueSelectorSettingsEntry(
                                            title = stringResource(R.string.cover_thumbnail_animation_type),
                                            selectedValue = coverThumbnailAnimation,
                                            onValueSelected = { coverThumbnailAnimation = it },
                                            valueText = { it.textName },
                                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 24.dp else 12.dp)
                                        )
                                    }
                                }
                            }

                            if (isLandscape) {
                                if (search.input.isBlank() || stringResource(R.string.player_thumbnail_size).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    EnumValueSelectorSettingsEntry(
                                        title = stringResource(R.string.player_thumbnail_size),
                                        selectedValue = playerThumbnailSizeL,
                                        onValueSelected = { playerThumbnailSizeL = it },
                                        valueText = {
                                            when (it) {
                                                PlayerThumbnailSize.Small -> stringResource(R.string.small)
                                                PlayerThumbnailSize.Medium -> stringResource(R.string.medium)
                                                PlayerThumbnailSize.Big -> stringResource(R.string.big)
                                                PlayerThumbnailSize.Biggest -> stringResource(R.string.biggest)
                                                PlayerThumbnailSize.Expanded -> stringResource(R.string.expanded)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )
                            } else {
                                if (search.input.isBlank() || stringResource(R.string.player_thumbnail_size).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    EnumValueSelectorSettingsEntry(
                                        title = stringResource(R.string.player_thumbnail_size),
                                        selectedValue = playerThumbnailSize,
                                        onValueSelected = { playerThumbnailSize = it },
                                        valueText = {
                                            when (it) {
                                                PlayerThumbnailSize.Small -> stringResource(R.string.small)
                                                PlayerThumbnailSize.Medium -> stringResource(R.string.medium)
                                                PlayerThumbnailSize.Big -> stringResource(R.string.big)
                                                PlayerThumbnailSize.Biggest -> stringResource(R.string.biggest)
                                                PlayerThumbnailSize.Expanded -> stringResource(R.string.expanded)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                    )
                            }
                            if (search.input.isBlank() || stringResource(R.string.thumbnailtype).contains(
                                    search.input,
                                    true
                                )
                            )
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.thumbnailtype),
                                    selectedValue = thumbnailType,
                                    onValueSelected = {
                                        thumbnailType = it
                                    },
                                    valueText = {
                                        when (it) {
                                            ThumbnailType.Modern -> stringResource(R.string.pcontrols_modern)
                                            ThumbnailType.Essential -> stringResource(R.string.pcontrols_essential)
                                        }
                                    },
                                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                )

                            if (search.input.isBlank() || stringResource(R.string.thumbnail_roundness).contains(
                                    search.input,
                                    true
                                )
                            )
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.thumbnail_roundness),
                                    selectedValue = thumbnailRoundness,
                                    onValueSelected = { thumbnailRoundness = it },
                                    trailingContent = {
                                        Spacer(
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = colorPalette().accent,
                                                    shape = thumbnailRoundness.shape()
                                                )
                                                .background(
                                                    color = colorPalette().background1,
                                                    shape = thumbnailRoundness.shape()
                                                )
                                                .size(36.dp)
                                        )
                                    },
                                    valueText = {
                                        when (it) {
                                            ThumbnailRoundness.None -> stringResource(R.string.none)
                                            ThumbnailRoundness.Light -> stringResource(R.string.light)
                                            ThumbnailRoundness.Heavy -> stringResource(R.string.heavy)
                                            ThumbnailRoundness.Medium -> stringResource(R.string.medium)
                                        }
                                    },
                                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp)
                                )
                        }
                    }

                    if (!showthumbnail) {
                        if (search.input.isBlank() || stringResource(R.string.noblur).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.noblur),
                                text = "",
                                isChecked = noblur,
                                onCheckedChange = { noblur = it }
                            )


                    }

                    if (!(showthumbnail && playerType == PlayerType.Essential)) {
                        if (search.input.isBlank() || stringResource(R.string.statsfornerdsplayer).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.statsfornerdsplayer),
                                text = "",
                                isChecked = statsfornerds,
                                onCheckedChange = { statsfornerds = it }
                            )
                    }

                    if (search.input.isBlank() || stringResource(R.string.pinfo_type).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.pinfo_type),
                            selectedValue = playerInfoType,
                            onValueSelected = {
                                playerInfoType = it
                            },
                            valueText = {
                                when (it) {
                                    PlayerInfoType.Modern -> stringResource(R.string.pcontrols_modern)
                                    PlayerInfoType.Essential -> stringResource(R.string.pcontrols_essential)
                                }
                            },
                        )
                        SettingsDescription(text = stringResource(R.string.pinfo_album_and_artist_name))

                        AnimatedVisibility(visible = playerInfoType == PlayerInfoType.Modern) {
                            Column {
                                if (search.input.isBlank() || stringResource(R.string.pinfo_show_icons).contains(
                                        search.input,
                                        true
                                    )
                                )
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.pinfo_show_icons),
                                        text = "",
                                        isChecked = playerInfoShowIcons,
                                        onCheckedChange = { playerInfoShowIcons = it },
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                    )
                            }
                        }

                    }



                    if (search.input.isBlank() || stringResource(R.string.miniplayertype).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.miniplayertype),
                            selectedValue = miniPlayerType,
                            onValueSelected = {
                                miniPlayerType = it
                            },
                            valueText = {
                                when (it) {
                                    MiniPlayerType.Modern -> stringResource(R.string.pcontrols_modern)
                                    MiniPlayerType.Essential -> stringResource(R.string.pcontrols_essential)
                                }
                            },
                        )

                    if (search.input.isBlank() || stringResource(R.string.player_swap_controls_with_timeline).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_swap_controls_with_timeline),
                            text = "",
                            isChecked = playerSwapControlsWithTimeline,
                            onCheckedChange = { playerSwapControlsWithTimeline = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.timeline).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.timeline),
                            selectedValue = playerTimelineType,
                            onValueSelected = {
                                playerTimelineType = it
                                restartActivity = true // applied also for online player
                            },
                            valueText = { it.textName }
                        )
                        RestartActivity(restartActivity, onRestart = { restartActivity = false })
                    }

                    if (search.input.isBlank() || stringResource(R.string.transparentbar).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.transparentbar),
                            text = "",
                            isChecked = transparentbar,
                            onCheckedChange = { transparentbar = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.timelinesize).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.timelinesize),
                            selectedValue = playerTimelineSize,
                            onValueSelected = { playerTimelineSize = it },
                            valueText = {
                                when (it) {
                                    PlayerTimelineSize.Small -> stringResource(R.string.small)
                                    PlayerTimelineSize.Medium -> stringResource(R.string.medium)
                                    PlayerTimelineSize.Big -> stringResource(R.string.big)
                                    PlayerTimelineSize.Biggest -> stringResource(R.string.biggest)
                                    PlayerTimelineSize.Expanded -> stringResource(R.string.expanded)
                                }
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.seek_with_tap).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.seek_with_tap),
                            text = "",
                            isChecked = seekWithTap,
                            onCheckedChange = { seekWithTap = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.pcontrols_type).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.pcontrols_type),
                            selectedValue = playerControlsType,
                            onValueSelected = {
                                playerControlsType = it
                            },
                            valueText = {
                                when (it) {
                                    PlayerControlsType.Modern -> stringResource(R.string.pcontrols_modern)
                                    PlayerControlsType.Essential -> stringResource(R.string.pcontrols_essential)
                                }
                            },
                        )


                    if (search.input.isBlank() || stringResource(R.string.play_button).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.play_button),
                            selectedValue = playerPlayButtonType,
                            onValueSelected = {
                                playerPlayButtonType = it
                                lastPlayerPlayButtonType = it
                            },
                            valueText = {
                                when (it) {
                                    PlayerPlayButtonType.Disabled -> stringResource(R.string.vt_disabled)
                                    PlayerPlayButtonType.Default -> stringResource(R.string._default)
                                    PlayerPlayButtonType.Rectangular -> stringResource(R.string.rectangular)
                                    PlayerPlayButtonType.Square -> stringResource(R.string.square)
                                    PlayerPlayButtonType.CircularRibbed -> stringResource(R.string.circular_ribbed)
                                    PlayerPlayButtonType.Circle -> stringResource(R.string.circle)
                                }
                            },
                        )

                    if (search.input.isBlank() || stringResource(R.string.buttonzoomout).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.buttonzoomout),
                            text = "",
                            isChecked = buttonzoomout,
                            onCheckedChange = { buttonzoomout = it }
                        )


                    if (search.input.isBlank() || stringResource(R.string.play_button).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.icon_like_button),
                            selectedValue = iconLikeType,
                            onValueSelected = {
                                iconLikeType = it
                            },
                            valueText = {
                                when (it) {
                                    IconLikeType.Essential -> stringResource(R.string.pcontrols_essential)
                                    IconLikeType.Apple -> stringResource(R.string.icon_like_apple)
                                    IconLikeType.Breaked -> stringResource(R.string.icon_like_breaked)
                                    IconLikeType.Gift -> stringResource(R.string.icon_like_gift)
                                    IconLikeType.Shape -> stringResource(R.string.icon_like_shape)
                                    IconLikeType.Striped -> stringResource(R.string.icon_like_striped)
                                    IconLikeType.Brilliant -> stringResource(R.string.icon_like_brilliant)
                                }
                            },
                        )

                    /*

            if (filter.isNullOrBlank() || stringResource(R.string.use_gradient_background).contains(filterCharSequence,true))
                SwitchSettingEntry(
                    title = stringResource(R.string.use_gradient_background),
                    text = "",
                    isChecked = isGradientBackgroundEnabled,
                    onCheckedChange = { isGradientBackgroundEnabled = it }
                )
             */

                    if (search.input.isBlank() || stringResource(R.string.background_colors).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.background_colors),
                            selectedValue = playerBackgroundColors,
                            onValueSelected = {
                                playerBackgroundColors = it
                            },
                            valueText = {
                                when (it) {
                                    PlayerBackgroundColors.CoverColor -> stringResource(R.string.bg_colors_background_from_cover)
                                    PlayerBackgroundColors.ThemeColor -> stringResource(R.string.bg_colors_background_from_theme)
                                    PlayerBackgroundColors.CoverColorGradient -> stringResource(R.string.bg_colors_gradient_background_from_cover)
                                    PlayerBackgroundColors.ThemeColorGradient -> stringResource(R.string.bg_colors_gradient_background_from_theme)
                                    PlayerBackgroundColors.BlurredCoverColor -> stringResource(R.string.bg_colors_blurred_cover_background)
                                    PlayerBackgroundColors.ColorPalette -> stringResource(R.string.colorpalette)
                                    PlayerBackgroundColors.AnimatedGradient -> stringResource(R.string.animatedgradient)
                                    PlayerBackgroundColors.MidnightOdyssey -> stringResource(R.string.midnightodyssey)
                                }
                            },
                        )

                    AnimatedVisibility(visible = playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) {
                        if (search.input.isBlank() || stringResource(R.string.gradienttype).contains(
                                search.input,
                                true
                            )
                        )
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.gradienttype),
                                selectedValue = animatedGradient,
                                onValueSelected = {
                                    animatedGradient = it
                                },
                                valueText = {
                                    when (it) {
                                        AnimatedGradient.FluidThemeColorGradient -> stringResource(R.string.bg_colors_fluid_gradient_background_from_theme)
                                        AnimatedGradient.FluidCoverColorGradient -> stringResource(R.string.bg_colors_fluid_gradient_background_from_cover)
                                        AnimatedGradient.Linear -> stringResource(R.string.linear)
                                        AnimatedGradient.Mesh -> stringResource(R.string.mesh)
                                        AnimatedGradient.MesmerizingLens -> stringResource(R.string.mesmerizinglens)
                                        AnimatedGradient.GlossyGradients -> stringResource(R.string.glossygradient)
                                        AnimatedGradient.GradientFlow -> stringResource(R.string.gradientflow)
                                        AnimatedGradient.PurpleLiquid -> stringResource(R.string.purpleliquid)
                                        AnimatedGradient.Stage -> stringResource(R.string.stage)
                                        AnimatedGradient.InkFlow -> stringResource(R.string.inkflow)
                                        AnimatedGradient.GoldenMagma -> stringResource(R.string.goldenmagma)
                                        AnimatedGradient.OilFlow -> stringResource(R.string.oilflow)
                                        AnimatedGradient.IceReflection -> stringResource(R.string.icereflection)
                                        AnimatedGradient.BlackCherryCosmos -> stringResource(R.string.blackcherrycosmos)
                                        AnimatedGradient.Random -> stringResource(R.string.random)
                                    }
                                },
                                modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) 12.dp else 0.dp)
                            )
                    }

                    if ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient))
                        if (search.input.isBlank() || stringResource(R.string.blackgradient).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.blackgradient),
                                text = "",
                                isChecked = blackgradient,
                                onCheckedChange = { blackgradient = it }
                            )

                    if ((playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) && (playerType == PlayerType.Modern))
                        if (search.input.isBlank() || stringResource(R.string.albumCoverRotation).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.albumCoverRotation),
                                text = "",
                                isChecked = albumCoverRotation,
                                onCheckedChange = { albumCoverRotation = it },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                            )

                    if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor)
                        if (search.input.isBlank() || stringResource(R.string.bottomgradient).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.bottomgradient),
                                text = "",
                                isChecked = bottomgradient,
                                onCheckedChange = { bottomgradient = it }
                            )
                    if (search.input.isBlank() || stringResource(R.string.textoutline).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.textoutline),
                            text = "",
                            isChecked = textoutline,
                            onCheckedChange = { textoutline = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.show_total_time_of_queue).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_total_time_of_queue),
                            text = "",
                            isChecked = showTotalTimeQueue,
                            onCheckedChange = { showTotalTimeQueue = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.show_remaining_song_time).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_remaining_song_time),
                            text = "",
                            isChecked = showRemainingSongTime,
                            onCheckedChange = { showRemainingSongTime = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.show_next_songs_in_player).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_next_songs_in_player),
                            text = "",
                            isChecked = showNextSongsInPlayer,
                            onCheckedChange = { showNextSongsInPlayer = it }
                        )
                    AnimatedVisibility(visible = showNextSongsInPlayer) {
                        Column {
                            if (search.input.isBlank() || stringResource(R.string.showtwosongs).contains(
                                    search.input,
                                    true
                                )
                            )
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.songs_number_to_show),
                                    selectedValue = showsongs,
                                    onValueSelected = {
                                        showsongs = it
                                    },
                                    valueText = {
                                        it.name
                                    },
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                )


                            if (search.input.isBlank() || stringResource(R.string.showalbumcover).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.showalbumcover),
                                    text = "",
                                    isChecked = showalbumcover,
                                    onCheckedChange = { showalbumcover = it },
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.disable_scrolling_text).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.disable_scrolling_text),
                            text = stringResource(R.string.scrolling_text_is_used_for_long_texts),
                            isChecked = disableScrollingText,
                            onCheckedChange = { disableScrollingText = it }
                        )

                    if (search.input.isBlank() || stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_horizontal_swipe else R.string.disable_vertical_swipe).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_vertical_swipe else R.string.disable_horizontal_swipe),
                            text = stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_vertical_swipe_secondary else R.string.disable_song_switching_via_swipe),
                            isChecked = disablePlayerHorizontalSwipe,
                            onCheckedChange = { disablePlayerHorizontalSwipe = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.toggle_lyrics).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.toggle_lyrics),
                            text = stringResource(R.string.by_tapping_on_the_thumbnail),
                            isChecked = thumbnailTapEnabled,
                            onCheckedChange = { thumbnailTapEnabled = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.click_lyrics_text).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.click_lyrics_text),
                            text = "",
                            isChecked = clickLyricsText,
                            onCheckedChange = { clickLyricsText = it }
                        )
                    if (showlyricsthumbnail)
                        if (search.input.isBlank() || stringResource(R.string.show_background_in_lyrics).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.show_background_in_lyrics),
                                text = "",
                                isChecked = showBackgroundLyrics,
                                onCheckedChange = { showBackgroundLyrics = it }
                            )

                    if (search.input.isBlank() || stringResource(R.string.player_enable_lyrics_popup_message).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_enable_lyrics_popup_message),
                            text = "",
                            isChecked = playerEnableLyricsPopupMessage,
                            onCheckedChange = { playerEnableLyricsPopupMessage = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.background_progress_bar).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.background_progress_bar),
                            selectedValue = backgroundProgress,
                            onValueSelected = {
                                backgroundProgress = it
                            },
                            valueText = {
                                when (it) {
                                    BackgroundProgress.Player -> stringResource(R.string.player)
                                    BackgroundProgress.MiniPlayer -> stringResource(R.string.minimized_player)
                                    BackgroundProgress.Both -> stringResource(R.string.both)
                                    BackgroundProgress.Disabled -> stringResource(R.string.vt_disabled)
                                }
                            },
                        )


                    if (search.input.isBlank() || stringResource(R.string.visualizer).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.visualizer),
                            text = "",
                            isChecked = visualizerEnabled,
                            onCheckedChange = { visualizerEnabled = it }
                        )
                        /*
                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.visualizer),
                    selectedValue = playerVisualizerType,
                    onValueSelected = { playerVisualizerType = it },
                    valueText = {
                        when (it) {
                            PlayerVisualizerType.Fancy -> stringResource(R.string.vt_fancy)
                            PlayerVisualizerType.Circular -> stringResource(R.string.vt_circular)
                            PlayerVisualizerType.Disabled -> stringResource(R.string.vt_disabled)
                            PlayerVisualizerType.Stacked -> stringResource(R.string.vt_stacked)
                            PlayerVisualizerType.Oneside -> stringResource(R.string.vt_one_side)
                            PlayerVisualizerType.Doubleside -> stringResource(R.string.vt_double_side)
                            PlayerVisualizerType.DoublesideCircular -> stringResource(R.string.vt_double_side_circular)
                            PlayerVisualizerType.Full -> stringResource(R.string.vt_full)
                        }
                    }
                )
                */
                        ImportantSettingsDescription(text = stringResource(R.string.visualizer_require_mic_permission))
                    }

                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.player_action_bar))
                }

                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.player_action_bar).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_action_bar),
                            text = "",
                            isChecked = showPlayerActionsBar,
                            onCheckedChange = { showPlayerActionsBar = it }
                        )
                }


                settingsItem {
                    AnimatedVisibility(visible = showPlayerActionsBar) {
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            if (search.input.isBlank() || stringResource(R.string.action_bar_transparent_background).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_transparent_background),
                                    text = "",
                                    isChecked = transparentBackgroundActionBarPlayer,
                                    onCheckedChange = { transparentBackgroundActionBarPlayer = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.actionspacedevenly).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.actionspacedevenly),
                                    text = "",
                                    isChecked = actionspacedevenly,
                                    onCheckedChange = { actionspacedevenly = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.tapqueue).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.tapqueue),
                                    text = "",
                                    isChecked = tapqueue,
                                    onCheckedChange = { tapqueue = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.swipe_up_to_open_the_queue).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.swipe_up_to_open_the_queue),
                                    text = "",
                                    isChecked = swipeUpQueue,
                                    onCheckedChange = { swipeUpQueue = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_video_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_video_button),
                                    text = "",
                                    isChecked = showButtonPlayerVideo,
                                    onCheckedChange = { showButtonPlayerVideo = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_discover_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_discover_button),
                                    text = "",
                                    isChecked = showButtonPlayerDiscover,
                                    onCheckedChange = { showButtonPlayerDiscover = it }
                                )

//        if (search.input.isBlank() || stringResource(R.string.action_bar_show_download_button).contains(
//                search.input,
//                true
//            )
//        )
//            SwitchSettingEntry(
//                title = stringResource(R.string.action_bar_show_download_button),
//                text = "",
//                isChecked = showButtonPlayerDownload,
//                onCheckedChange = { showButtonPlayerDownload = it }
//            )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_add_to_playlist_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_add_to_playlist_button),
                                    text = "",
                                    isChecked = showButtonPlayerAddToPlaylist,
                                    onCheckedChange = { showButtonPlayerAddToPlaylist = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_loop_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_loop_button),
                                    text = "",
                                    isChecked = showButtonPlayerLoop,
                                    onCheckedChange = { showButtonPlayerLoop = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_shuffle_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_shuffle_button),
                                    text = "",
                                    isChecked = showButtonPlayerShuffle,
                                    onCheckedChange = { showButtonPlayerShuffle = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_lyrics_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_lyrics_button),
                                    text = "",
                                    isChecked = showButtonPlayerLyrics,
                                    onCheckedChange = { showButtonPlayerLyrics = it }
                                )
                            if (!isLandscape || !showthumbnail) {
                                if (!showlyricsthumbnail) {
                                    if (search.input.isBlank() || stringResource(R.string.expandedplayer).contains(
                                            search.input,
                                            true
                                        )
                                    )
                                        SwitchSettingEntry(
                                            title = stringResource(R.string.expandedplayer),
                                            text = "",
                                            isChecked = expandedplayertoggle,
                                            onCheckedChange = { expandedplayertoggle = it }
                                        )
                                }
                            }

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_sleep_timer_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_sleep_timer_button),
                                    text = "",
                                    isChecked = showButtonPlayerSleepTimer,
                                    onCheckedChange = { showButtonPlayerSleepTimer = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.show_equalizer).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.show_equalizer),
                                    text = "",
                                    isChecked = showButtonPlayerSystemEqualizer,
                                    onCheckedChange = { showButtonPlayerSystemEqualizer = it }
                                )

                            /*
                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_arrow_button_to_open_queue).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_arrow_button_to_open_queue),
                                    text = "",
                                    isChecked = showButtonPlayerArrow,
                                    onCheckedChange = { showButtonPlayerArrow = it }
                                )
                            */

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_start_radio_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_start_radio_button),
                                    text = "",
                                    isChecked = showButtonPlayerStartradio,
                                    onCheckedChange = { showButtonPlayerStartradio = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.action_bar_show_menu_button).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_menu_button),
                                    text = "",
                                    isChecked = showButtonPlayerMenu,
                                    onCheckedChange = { showButtonPlayerMenu = it }
                                )
                        }
                    }
                }

                if (!showlyricsthumbnail) {
                    settingsItem(
                        isHeader = true
                    ) {
                        SettingsGroupSpacer()
                        SettingsEntryGroupText(title = stringResource(R.string.full_screen_lyrics_components))
                    }

                    settingsItem {
                        if (showTotalTimeQueue) {
                            if (search.input.isBlank() || stringResource(R.string.show_total_time_of_queue).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.show_total_time_of_queue),
                                    text = "",
                                    isChecked = queueDurationExpanded,
                                    onCheckedChange = { queueDurationExpanded = it }
                                )
                        }

                        if (search.input.isBlank() || stringResource(R.string.titleartist).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.titleartist),
                                text = "",
                                isChecked = titleExpanded,
                                onCheckedChange = { titleExpanded = it }
                            )

                        if (search.input.isBlank() || stringResource(R.string.timeline).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.timeline),
                                text = "",
                                isChecked = timelineExpanded,
                                onCheckedChange = { timelineExpanded = it }
                            )

                        if (search.input.isBlank() || stringResource(R.string.controls).contains(
                                search.input,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.controls),
                                text = "",
                                isChecked = controlsExpanded,
                                onCheckedChange = { controlsExpanded = it }
                            )

                        if (statsfornerds && (!(showthumbnail && playerType == PlayerType.Essential))) {
                            if (search.input.isBlank() || stringResource(R.string.statsfornerds).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.statsfornerds),
                                    text = "",
                                    isChecked = statsExpanded,
                                    onCheckedChange = { statsExpanded = it }
                                )
                        }

                        if (
                        //showButtonPlayerDownload ||
                            showButtonPlayerAddToPlaylist ||
                            showButtonPlayerLoop ||
                            showButtonPlayerShuffle ||
                            showButtonPlayerLyrics ||
                            showButtonPlayerSleepTimer ||
                            showButtonPlayerSystemEqualizer ||
                            showButtonPlayerArrow ||
                            showButtonPlayerMenu ||
                            expandedplayertoggle ||
                            showButtonPlayerDiscover ||
                            showButtonPlayerVideo
                        ) {
                            if (search.input.isBlank() || stringResource(R.string.actionbar).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.actionbar),
                                    text = "",
                                    isChecked = actionExpanded,
                                    onCheckedChange = {
                                        actionExpanded = it
                                    }
                                )
                        }
                        if (showNextSongsInPlayer && actionExpanded) {
                            if (search.input.isBlank() || stringResource(R.string.miniqueue).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.miniqueue),
                                    text = "",
                                    isChecked = miniQueueExpanded,
                                    onCheckedChange = { miniQueueExpanded = it }
                                )
                        }
                    }

                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.notification_player))
                }

                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.notification_player).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.notificationPlayerFirstIcon),
                            selectedValue = notificationPlayerFirstIcon,
                            onValueSelected = {
                                notificationPlayerFirstIcon = it
                                restartService = true
                            },
                            valueText = {
                                it.displayName
                            },
                        )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.notificationPlayerSecondIcon),
                            selectedValue = notificationPlayerSecondIcon,
                            onValueSelected = {
                                notificationPlayerSecondIcon = it
                                restartService = true
                            },
                            valueText = {
                                it.displayName
                            },
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }


        if (search.input.isBlank() || stringResource(R.string.show_song_cover).contains(
                search.input,
                true
            )
        )
            if (!isAtLeastAndroid13) {
                SettingsGroupSpacer()

                SettingsEntryGroupText(title = stringResource(R.string.lockscreen))

                SwitchSettingEntry(
                    title = stringResource(R.string.show_song_cover),
                    text = stringResource(R.string.use_song_cover_on_lockscreen),
                    isChecked = isShowingThumbnailInLockscreen,
                    onCheckedChange = { isShowingThumbnailInLockscreen = it }
                )
            }

                }

                if (isAtLeastAndroid7) {
                    settingsItem(
                        isHeader = true
                    ) {
                        SettingsGroupSpacer()
                        SettingsEntryGroupText(title = stringResource(R.string.wallpaper))
                    }
                    settingsItem {
                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_wallpaper),
                            text = "",
                            isChecked = enableWallpaper,
                            onCheckedChange = { enableWallpaper = it }
                        )
                        AnimatedVisibility(visible = enableWallpaper) {
                            Column {
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.set_cover_thumbnail_as_wallpaper),
                                    selectedValue = wallpaperType,
                                    onValueSelected = {
                                        wallpaperType = it
                                        restartService = true
                                    },
                                    valueText = {
                                        it.displayName
                                    },
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                                RestartPlayerService(
                                    restartService,
                                    onRestart = { restartService = false })
                            }
                        }
                    }

                }

                settingsItem {
                    SettingsGroupSpacer()
                    var resetToDefault by remember { mutableStateOf(false) }

                    ButtonBarSettingEntry(
                        title = stringResource(R.string.settings_reset),
                        text = stringResource(R.string.settings_restore_default_settings),
                        icon = R.drawable.refresh,
                        iconColor = colorPalette().text,
                        onClick = { resetToDefault = true },
                    )
                    if (resetToDefault) {
                        DefaultPlayerAppearanceSettings()
                        resetToDefault = false
                        navController.popBackStack()
                        SmartMessage(stringResource(R.string.done), context = context)
                    }

                    SettingsGroupSpacer()
                    ButtonBarSettingEntry(
                        title = stringResource(R.string.export_appearance_settings),
                        text = stringResource(R.string.info_backup_or_share_appearance_settings),
                        icon = R.drawable.export,
                        iconColor = colorPalette().text,
                        onClick = { isExporting = true },
                    )

                    ButtonBarSettingEntry(
                        title = stringResource(R.string.import_appearance_settings),
                        text = stringResource(R.string.info_restore_backup_or_shared_appearance_settings),
                        icon = R.drawable.resource_import,
                        iconColor = colorPalette().text,
                        onClick = {
                            try {
                                importLauncher.launch(
                                    arrayOf(
                                        "text/*"
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
                                SmartMessage(
                                    context.resources.getString(R.string.info_not_find_app_open_doc),
                                    type = PopupType.Warning, context = context
                                )
                            }
                        },
                    )
                }


//            SettingsGroupSpacer(
//                modifier = Modifier.height(Dimensions.bottomSpacer)
//            )
            }
        }
    }

}
