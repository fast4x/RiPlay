package it.fast4x.riplay.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.AlbumSwipeAction
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.CarouselSize
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.enums.MaxStatisticsItems
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NavigationBarType
import it.fast4x.riplay.enums.PauseBetweenSongs
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerTimelineSize
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.PlaylistSwipeAction
import it.fast4x.riplay.enums.QueueSwipeAction
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.RecommendationsNumber
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.DefaultDarkColorPalette
import it.fast4x.riplay.ui.styling.DefaultLightColorPalette
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MAX_TOP_PLAYLIST_ITEMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.UI_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTIONS_SPACED_EVENLY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ALBUM_SWIPE_LEFT_ACTION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ALBUM_SWIPE_RIGHT_ACTION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.APPLY_FONT_PADDING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BACKGROUND_PROGRESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLACK_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BUTTON_ZOOM_OUT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLICK_ON_LYRICS_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLOSE_WITH_BACK_BUTTON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLOSE_BACKGROUND_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_0
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_1
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_2
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_3
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_4
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_ACCENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_ICON_BUTTON_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_TEXT_DISABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_TEXT_SECONDARY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_0
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_1
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_2
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_3
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_4
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_ACCENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_ICON_BUTTON_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_TEXT_DISABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_TEXT_SECONDARY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_CLOSING_PLAYER_SWIPING_DOWN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_ICON_BUTTON_ON_TOP
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_PLAYER_HORIZONTAL_SWIPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_SCROLLING_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EFFECT_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_CREATE_MONTHLY_PLAYLISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXCLUDE_SONGS_WITH_DURATION_LIMIT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXO_PLAYER_MIN_TIME_FOR_EVENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER_TOGGLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FADING_EDGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FONT_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ICON_LIKE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.INDEX_NAVIGATION_TAB
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_PAUSE_ON_VOLUME_ZERO_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_SWIPE_TO_ACTION_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LAST_PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LAST_PLAYER_THUMBNAIL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LAST_PLAYER_TIMELINE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MAX_SONGS_IN_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MAX_STATISTICS_ITEMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MENU_STYLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MESSAGE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINI_PLAYER_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINIMUM_SILENCE_DURATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_POSITION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PAUSE_BETWEEN_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PAUSE_LISTEN_HISTORY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYBACK_FADE_AUDIO_DURATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_BACKGROUND_COLORS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_CONTROLS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_ENABLE_LYRICS_POPUP_MESSAGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_SHOW_ICONS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_POSITION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_SWAP_CONTROLS_WITH_TIMELINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_THUMBNAIL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYLIST_SWIPE_LEFT_ACTION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYLIST_SWIPE_RIGHT_ACTION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYLIST_INDICATOR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_SWIPE_LEFT_ACTION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_SWIPE_RIGHT_ACTION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.RECOMMENDATIONS_NUMBER
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.RESUME_PLAYBACK_ON_START
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHAKE_EVENT_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ARROW
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LOOP
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_MENU
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SHUFFLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SLEEP_TIMER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_DOWNLOADED_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FAVORITES_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FLOATING_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MONTHLY_PLAYLISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MY_TOP_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEXT_SONGS_IN_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ON_DEVICE_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PINNED_PLAYLISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PIPED_PLAYLISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_REMAINING_SONG_TIME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SEARCH_TAB
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_STATS_IN_NAVBAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_STATS_LISTENING_TIME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOTAL_TIME_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SKIP_MEDIA_ON_ERROR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_UP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TAP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TAP_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSITION_EFFECT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.USE_SYSTEM_FONT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.USE_VOLUME_KEYS_TO_CHANGE_SONG
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VISUALIZER_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VOLUME_NORMALIZATION
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_COLOR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_ENABLED_FULLSCREEN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_DISLIKED_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LISTENER_LEVELS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SNOWFALL_EFFECT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.USE_PLACEHOLDER_IN_IMAGE_LOADER
import it.fast4x.riplay.utils.LazyListContainer

@Composable
fun DefaultUiSettings() {
    var minTimeForEvent by rememberPreference(
        EXO_PLAYER_MIN_TIME_FOR_EVENT.key,
        MinTimeForEvent.`20s`
    )
    minTimeForEvent = MinTimeForEvent.`20s`
    var resumePlaybackOnStart by rememberPreference(RESUME_PLAYBACK_ON_START.key, false)
    resumePlaybackOnStart = false
    var closebackgroundPlayer by rememberPreference(CLOSE_BACKGROUND_PLAYER.key, false)
    closebackgroundPlayer = false
    var closeWithBackButton by rememberPreference(CLOSE_WITH_BACK_BUTTON.key, true)
    closeWithBackButton = true
    var skipMediaOnError by rememberPreference(SKIP_MEDIA_ON_ERROR.key, false)
    skipMediaOnError = false
    var volumeNormalization by rememberPreference(VOLUME_NORMALIZATION.key, false)
    volumeNormalization = false
    var recommendationsNumber by rememberPreference(RECOMMENDATIONS_NUMBER.key,   RecommendationsNumber.`5`)
    recommendationsNumber = RecommendationsNumber.`5`
    var keepPlayerMinimized by rememberPreference(KEEP_PLAYER_MINIMIZED.key,   false)
    keepPlayerMinimized = false
    var disableIconButtonOnTop by rememberPreference(DISABLE_ICON_BUTTON_ON_TOP.key, false)
    disableIconButtonOnTop = false
    var lastPlayerTimelineType by rememberPreference(LAST_PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    lastPlayerTimelineType = PlayerTimelineType.Default
    var lastPlayerThumbnailSize by rememberPreference(LAST_PLAYER_THUMBNAIL_SIZE.key, PlayerThumbnailSize.Medium)
    lastPlayerThumbnailSize = PlayerThumbnailSize.Medium
    var uiType  by rememberPreference(UI_TYPE.key, UiType.RiPlay)
    uiType = UiType.RiPlay
    var disablePlayerHorizontalSwipe by rememberPreference(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, false)
    disablePlayerHorizontalSwipe = false
    var lastPlayerPlayButtonType by rememberPreference(LAST_PLAYER_PLAY_BUTTON_TYPE.key, PlayerPlayButtonType.Rectangular)
    lastPlayerPlayButtonType = PlayerPlayButtonType.Rectangular
    var colorPaletteName by rememberPreference(COLOR_PALETTE_NAME.key, ColorPaletteName.Dynamic)
    colorPaletteName = ColorPaletteName.Dynamic
    var colorPaletteMode by rememberPreference(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
    colorPaletteMode = ColorPaletteMode.Dark
    var indexNavigationTab by rememberPreference(
        INDEX_NAVIGATION_TAB.key,
        HomeScreenTabs.Default
    )
    indexNavigationTab = HomeScreenTabs.Default
    var fontType by rememberPreference(FONT_TYPE.key, FontType.Rubik)
    fontType = FontType.Rubik
    var useSystemFont by rememberPreference(USE_SYSTEM_FONT.key, false)
    useSystemFont = false
    var applyFontPadding by rememberPreference(APPLY_FONT_PADDING.key, false)
    applyFontPadding = false
    var isSwipeToActionEnabled by rememberPreference(IS_SWIPE_TO_ACTION_ENABLED.key, true)
    isSwipeToActionEnabled = true
    var disableClosingPlayerSwipingDown by rememberPreference(DISABLE_CLOSING_PLAYER_SWIPING_DOWN.key, false)
    disableClosingPlayerSwipingDown = false
    var showSearchTab by rememberPreference(SHOW_SEARCH_TAB.key, false)
    showSearchTab = false
    var showStatsInNavbar by rememberPreference(SHOW_STATS_IN_NAVBAR.key, false)
    showStatsInNavbar = false
    var maxStatisticsItems by rememberPreference(
        MAX_STATISTICS_ITEMS.key,
        MaxStatisticsItems.`10`
    )
    maxStatisticsItems = MaxStatisticsItems.`10`
    var showStatsListeningTime by rememberPreference(SHOW_STATS_LISTENING_TIME.key,   true)
    showStatsListeningTime = true
    var maxTopPlaylistItems by rememberPreference(
        MAX_TOP_PLAYLIST_ITEMS.key,
        MaxTopPlaylistItems.`10`
    )
    maxTopPlaylistItems = MaxTopPlaylistItems.`10`
    var navigationBarPosition by rememberPreference(NAVIGATION_BAR_POSITION.key, NavigationBarPosition.Bottom)
    navigationBarPosition = NavigationBarPosition.Bottom
    var navigationBarType by rememberPreference(NAVIGATION_BAR_TYPE.key, NavigationBarType.IconAndText)
    navigationBarType = NavigationBarType.IconAndText
    var pauseBetweenSongs  by rememberPreference(PAUSE_BETWEEN_SONGS.key, PauseBetweenSongs.`0`)
    pauseBetweenSongs = PauseBetweenSongs.`0`
    var maxSongsInQueue  by rememberPreference(MAX_SONGS_IN_QUEUE.key, MaxSongs.`500`)
    maxSongsInQueue = MaxSongs.`500`
    var thumbnailRoundness by rememberPreference(
        THUMBNAIL_ROUNDNESS.key,
        ThumbnailRoundness.Light
    )
    thumbnailRoundness = ThumbnailRoundness.Light
    var showFavoritesPlaylist by rememberPreference(SHOW_FAVORITES_PLAYLIST.key, true)
    showFavoritesPlaylist = true
    var showMyTopPlaylist by rememberPreference(SHOW_MY_TOP_PLAYLIST.key, true)
    showMyTopPlaylist = true
    var showDownloadedPlaylist by rememberPreference(SHOW_DOWNLOADED_PLAYLIST.key, true)
    showDownloadedPlaylist = true
    var showOnDevicePlaylist by rememberPreference(SHOW_ON_DEVICE_PLAYLIST.key, true)
    showOnDevicePlaylist = true
    var showDislikedPlaylist by rememberPreference(SHOW_DISLIKED_PLAYLIST.key, false)
    showDislikedPlaylist = false
    var shakeEventEnabled by rememberPreference(SHAKE_EVENT_ENABLED.key, false)
    shakeEventEnabled = false
    var useVolumeKeysToChangeSong by rememberPreference(USE_VOLUME_KEYS_TO_CHANGE_SONG.key, false)
    useVolumeKeysToChangeSong = false
    var showFloatingIcon by rememberPreference(SHOW_FLOATING_ICON.key, false)
    showFloatingIcon = false
    var menuStyle by rememberPreference(MENU_STYLE.key, MenuStyle.List)
    menuStyle = MenuStyle.List
    var transitionEffect by rememberPreference(TRANSITION_EFFECT.key, TransitionEffect.SlideHorizontal)
    transitionEffect = TransitionEffect.Scale
    var enableCreateMonthlyPlaylists by rememberPreference(ENABLE_CREATE_MONTHLY_PLAYLISTS.key, true)
    enableCreateMonthlyPlaylists = true
    var showPipedPlaylists by rememberPreference(SHOW_PIPED_PLAYLISTS.key, true)
    showPipedPlaylists = true
    var showPinnedPlaylists by rememberPreference(SHOW_PINNED_PLAYLISTS.key, true)
    showPinnedPlaylists = true
    var showMonthlyPlaylists by rememberPreference(SHOW_MONTHLY_PLAYLISTS.key, true)
    showMonthlyPlaylists = true
    var customThemeLight_Background0 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_0.key, DefaultLightColorPalette.background0.hashCode())
    customThemeLight_Background0 = DefaultLightColorPalette.background0.hashCode()
    var customThemeLight_Background1 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_1.key, DefaultLightColorPalette.background1.hashCode())
    customThemeLight_Background1 = DefaultLightColorPalette.background1.hashCode()
    var customThemeLight_Background2 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_2.key, DefaultLightColorPalette.background2.hashCode())
    customThemeLight_Background2 = DefaultLightColorPalette.background2.hashCode()
    var customThemeLight_Background3 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_3.key, DefaultLightColorPalette.background3.hashCode())
    customThemeLight_Background3 = DefaultLightColorPalette.background3.hashCode()
    var customThemeLight_Background4 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_4.key, DefaultLightColorPalette.background4.hashCode())
    customThemeLight_Background4 = DefaultLightColorPalette.background4.hashCode()
    var customThemeLight_Text by rememberPreference(CUSTOM_THEME_LIGHT_TEXT.key, DefaultLightColorPalette.text.hashCode())
    customThemeLight_Text = DefaultLightColorPalette.text.hashCode()
    var customThemeLight_TextSecondary by rememberPreference(CUSTOM_THEME_LIGHT_TEXT_SECONDARY.key, DefaultLightColorPalette.textSecondary.hashCode())
    customThemeLight_TextSecondary = DefaultLightColorPalette.textSecondary.hashCode()
    var customThemeLight_TextDisabled by rememberPreference(CUSTOM_THEME_LIGHT_TEXT_DISABLED.key, DefaultLightColorPalette.textDisabled.hashCode())
    customThemeLight_TextDisabled = DefaultLightColorPalette.textDisabled.hashCode()
    var customThemeLight_IconButtonPlayer by rememberPreference(CUSTOM_THEME_LIGHT_ICON_BUTTON_PLAYER.key, DefaultLightColorPalette.iconButtonPlayer.hashCode())
    customThemeLight_IconButtonPlayer = DefaultLightColorPalette.iconButtonPlayer.hashCode()
    var customThemeLight_Accent by rememberPreference(CUSTOM_THEME_LIGHT_ACCENT.key, DefaultLightColorPalette.accent.hashCode())
    customThemeLight_Accent = DefaultLightColorPalette.accent.hashCode()
    var customThemeDark_Background0 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_0.key, DefaultDarkColorPalette.background0.hashCode())
    customThemeDark_Background0 = DefaultDarkColorPalette.background0.hashCode()
    var customThemeDark_Background1 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_1.key, DefaultDarkColorPalette.background1.hashCode())
    customThemeDark_Background1 = DefaultDarkColorPalette.background1.hashCode()
    var customThemeDark_Background2 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_2.key, DefaultDarkColorPalette.background2.hashCode())
    customThemeDark_Background2 = DefaultDarkColorPalette.background2.hashCode()
    var customThemeDark_Background3 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_3.key, DefaultDarkColorPalette.background3.hashCode())
    customThemeDark_Background3 = DefaultDarkColorPalette.background3.hashCode()
    var customThemeDark_Background4 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_4.key, DefaultDarkColorPalette.background4.hashCode())
    customThemeDark_Background4 = DefaultDarkColorPalette.background4.hashCode()
    var customThemeDark_Text by rememberPreference(CUSTOM_THEME_DARK_TEXT.key, DefaultDarkColorPalette.text.hashCode())
    customThemeDark_Text = DefaultDarkColorPalette.text.hashCode()
    var customThemeDark_TextSecondary by rememberPreference(CUSTOM_THEME_DARK_TEXT_SECONDARY.key, DefaultDarkColorPalette.textSecondary.hashCode())
    customThemeDark_TextSecondary = DefaultDarkColorPalette.textSecondary.hashCode()
    var customThemeDark_TextDisabled by rememberPreference(CUSTOM_THEME_DARK_TEXT_DISABLED.key, DefaultDarkColorPalette.textDisabled.hashCode())
    customThemeDark_TextDisabled = DefaultDarkColorPalette.textDisabled.hashCode()
    var customThemeDark_IconButtonPlayer by rememberPreference(CUSTOM_THEME_DARK_ICON_BUTTON_PLAYER.key, DefaultDarkColorPalette.iconButtonPlayer.hashCode())
    customThemeDark_IconButtonPlayer = DefaultDarkColorPalette.iconButtonPlayer.hashCode()
    var customThemeDark_Accent by rememberPreference(CUSTOM_THEME_DARK_ACCENT.key, DefaultDarkColorPalette.accent.hashCode())
    customThemeDark_Accent = DefaultDarkColorPalette.accent.hashCode()
    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    resetCustomLightThemeDialog = false
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }
    resetCustomDarkThemeDialog = false
    var playbackFadeAudioDuration by rememberPreference(PLAYBACK_FADE_AUDIO_DURATION.key, DurationInMilliseconds.Disabled)
    playbackFadeAudioDuration = DurationInMilliseconds.Disabled
    var playerPosition by rememberPreference(PLAYER_POSITION.key, PlayerPosition.Bottom)
    playerPosition = PlayerPosition.Bottom
    var excludeSongWithDurationLimit by rememberPreference(EXCLUDE_SONGS_WITH_DURATION_LIMIT.key, DurationInMinutes.Disabled)
    excludeSongWithDurationLimit = DurationInMinutes.Disabled
    var playlistindicator by rememberPreference(PLAYLIST_INDICATOR.key, false)
    playlistindicator = false
    var discoverIsEnabled by rememberPreference(DISCOVER.key, false)
    discoverIsEnabled = false
    var isPauseOnVolumeZeroEnabled by rememberPreference(IS_PAUSE_ON_VOLUME_ZERO_ENABLED.key, false)
    isPauseOnVolumeZeroEnabled = false
    var messageType by rememberPreference(MESSAGE_TYPE.key, MessageType.Modern)
    messageType = MessageType.Modern
    var minimumSilenceDuration by rememberPreference(MINIMUM_SILENCE_DURATION.key, 2_000_000L)
    minimumSilenceDuration = 2_000_000L
    var pauseListenHistory by rememberPreference(PAUSE_LISTEN_HISTORY.key, false)
    pauseListenHistory = false
    var showTopActionsBar by rememberPreference(SHOW_TOP_ACTIONS_BAR.key, true)
    showTopActionsBar = true
    var playerControlsType by rememberPreference(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
    playerControlsType = PlayerControlsType.Modern
    var playerInfoType by rememberPreference(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential)
    playerInfoType = PlayerInfoType.Modern
    var playerType by rememberPreference(PLAYER_TYPE.key, PlayerType.Modern)
    playerType = PlayerType.Modern
    var queueType by rememberPreference(QUEUE_TYPE.key, QueueType.Modern)
    queueType = QueueType.Modern
    var fadingedge by rememberPreference(FADING_EDGE.key, false)
    fadingedge = false
    var carousel by rememberPreference(CAROUSEL.key, true)
    carousel = true
    var carouselSize by rememberPreference(CAROUSEL_SIZE.key, CarouselSize.Biggest)
    carouselSize = CarouselSize.Biggest
    var thumbnailType by rememberPreference(THUMBNAIL_TYPE.key, ThumbnailType.Modern)
    thumbnailType = ThumbnailType.Modern
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
    var playerInfoShowIcons by rememberPreference(PLAYER_INFO_SHOW_ICONS.key, true)
    playerInfoShowIcons = true
    var miniPlayerType by rememberPreference(
        MINI_PLAYER_TYPE.key,
        MiniPlayerType.Modern
    )
    miniPlayerType = MiniPlayerType.Modern
    var playerSwapControlsWithTimeline by rememberPreference(
        PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
        false
    )
    playerSwapControlsWithTimeline = false
    var playerPlayButtonType by rememberPreference(
        PLAYER_PLAY_BUTTON_TYPE.key,
        PlayerPlayButtonType.Disabled
    )
    playerPlayButtonType = PlayerPlayButtonType.Disabled
    var buttonzoomout by rememberPreference(BUTTON_ZOOM_OUT.key, false)
    buttonzoomout = false
    var iconLikeType by rememberPreference(ICON_LIKE_TYPE.key, IconLikeType.Essential)
    iconLikeType = IconLikeType.Essential
    var playerBackgroundColors by rememberPreference(
        PLAYER_BACKGROUND_COLORS.key,
        PlayerBackgroundColors.BlurredCoverColor
    )
    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
    var blackgradient by rememberPreference(BLACK_GRADIENT.key, false)
    blackgradient = false
    var showTotalTimeQueue by rememberPreference(SHOW_TOTAL_TIME_QUEUE.key, true)
    showTotalTimeQueue = true
    var showNextSongsInPlayer by rememberPreference(SHOW_NEXT_SONGS_IN_PLAYER.key, false)
    showNextSongsInPlayer = false
    var showRemainingSongTime by rememberPreference(SHOW_REMAINING_SONG_TIME.key, true)
    showRemainingSongTime = true
    var disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    disableScrollingText = false
    var effectRotationEnabled by rememberPreference(EFFECT_ROTATION.key, true)
    effectRotationEnabled = true
    var thumbnailTapEnabled by rememberPreference(THUMBNAIL_TAP_ENABLED.key, true)
    thumbnailTapEnabled = true
    var clickLyricsText by rememberPreference(CLICK_ON_LYRICS_TEXT.key, true)
    clickLyricsText = true
    var backgroundProgress by rememberPreference(
        BACKGROUND_PROGRESS.key,
        BackgroundProgress.MiniPlayer
    )
    backgroundProgress = BackgroundProgress.MiniPlayer
    var transparentBackgroundActionBarPlayer by rememberPreference(
        TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
        true
    )
    transparentBackgroundActionBarPlayer = false
    var actionspacedevenly by rememberPreference(ACTIONS_SPACED_EVENLY.key, false)
    actionspacedevenly = false
    var tapqueue by rememberPreference(TAP_QUEUE.key, true)
    tapqueue = true
    var swipeUpQueue by rememberPreference(SWIPE_UP_QUEUE.key, true)
    swipeUpQueue = true
    var showButtonPlayerAddToPlaylist by rememberPreference(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true)
    showButtonPlayerAddToPlaylist = true
    var showButtonPlayerArrow by rememberPreference(SHOW_BUTTON_PLAYER_ARROW.key, true)
    showButtonPlayerArrow = false
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
    var queueSwipeLeftAction by rememberPreference(QUEUE_SWIPE_LEFT_ACTION.key, QueueSwipeAction.RemoveFromQueue)
    queueSwipeLeftAction = QueueSwipeAction.RemoveFromQueue
    var queueSwipeRightAction by rememberPreference(QUEUE_SWIPE_RIGHT_ACTION.key, QueueSwipeAction.PlayNext)
    queueSwipeRightAction = QueueSwipeAction.PlayNext

    var playlistSwipeLeftAction by rememberPreference(PLAYLIST_SWIPE_LEFT_ACTION.key, PlaylistSwipeAction.Favourite)
    playlistSwipeLeftAction = PlaylistSwipeAction.Favourite
    var playlistSwipeRightAction by rememberPreference(PLAYLIST_SWIPE_RIGHT_ACTION.key, PlaylistSwipeAction.PlayNext)
    playlistSwipeRightAction = PlaylistSwipeAction.PlayNext

    var albumSwipeLeftAction by rememberPreference(ALBUM_SWIPE_LEFT_ACTION.key, AlbumSwipeAction.PlayNext)
    albumSwipeLeftAction = AlbumSwipeAction.PlayNext
    var albumSwipeRightAction by rememberPreference(ALBUM_SWIPE_RIGHT_ACTION.key, AlbumSwipeAction.Bookmark)
    albumSwipeRightAction = AlbumSwipeAction.Bookmark

    var showButtonPlayerDiscover by rememberPreference(SHOW_BUTTON_PLAYER_DISCOVER.key, false)
    showButtonPlayerDiscover = false
    var playerEnableLyricsPopupMessage by rememberPreference(
        PLAYER_ENABLE_LYRICS_POPUP_MESSAGE.key,
        true
    )
    playerEnableLyricsPopupMessage = true
    var visualizerEnabled by rememberPreference(VISUALIZER_ENABLED.key, false)
    visualizerEnabled = false
    var showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    showthumbnail = true
}

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun UiSettings(
    navController: NavController
) {
    var recommendationsNumber by rememberPreference(RECOMMENDATIONS_NUMBER.key,   RecommendationsNumber.`5`)

    var keepPlayerMinimized by rememberPreference(KEEP_PLAYER_MINIMIZED.key,   false)

    var disableIconButtonOnTop by rememberPreference(DISABLE_ICON_BUTTON_ON_TOP.key, false)
    var lastPlayerTimelineType by rememberPreference(LAST_PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    var lastPlayerThumbnailSize by rememberPreference(LAST_PLAYER_THUMBNAIL_SIZE.key, PlayerThumbnailSize.Medium)
    var disablePlayerHorizontalSwipe by rememberPreference(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, false)

    var lastPlayerPlayButtonType by rememberPreference(LAST_PLAYER_PLAY_BUTTON_TYPE.key, PlayerPlayButtonType.Rectangular)

    var colorPaletteName by rememberPreference(COLOR_PALETTE_NAME.key, ColorPaletteName.Dynamic)
    var colorPaletteMode by rememberPreference(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
    var indexNavigationTab by rememberPreference(
        INDEX_NAVIGATION_TAB.key,
        HomeScreenTabs.Default
    )
    var fontType by rememberPreference(FONT_TYPE.key, FontType.Rubik)
    var useSystemFont by rememberPreference(USE_SYSTEM_FONT.key, false)
    var applyFontPadding by rememberPreference(APPLY_FONT_PADDING.key, false)
    var isSwipeToActionEnabled by rememberPreference(IS_SWIPE_TO_ACTION_ENABLED.key, true)
    var showSearchTab by rememberPreference(SHOW_SEARCH_TAB.key, false)
    var showStatsInNavbar by rememberPreference(SHOW_STATS_IN_NAVBAR.key, false)

    var maxStatisticsItems by rememberPreference(
        MAX_STATISTICS_ITEMS.key,
        MaxStatisticsItems.`10`
    )

    var showStatsListeningTime by rememberPreference(SHOW_STATS_LISTENING_TIME.key,   true)

    var maxTopPlaylistItems by rememberPreference(
        MAX_TOP_PLAYLIST_ITEMS.key,
        MaxTopPlaylistItems.`10`
    )

    var navigationBarPosition by rememberPreference(NAVIGATION_BAR_POSITION.key, NavigationBarPosition.Bottom)
    var navigationBarType by rememberPreference(NAVIGATION_BAR_TYPE.key, NavigationBarType.IconAndText)
    val search = Search.init()

    var showFavoritesPlaylist by rememberPreference(SHOW_FAVORITES_PLAYLIST.key, true)
    var showMyTopPlaylist by rememberPreference(SHOW_MY_TOP_PLAYLIST.key, true)
    var showOnDevicePlaylist by rememberPreference(SHOW_ON_DEVICE_PLAYLIST.key, true)
    var showDislikedPlaylist by rememberPreference(SHOW_DISLIKED_PLAYLIST.key, false)
    var showFloatingIcon by rememberPreference(SHOW_FLOATING_ICON.key, false)
    var menuStyle by rememberPreference(MENU_STYLE.key, MenuStyle.List)
    var transitionEffect by rememberPreference(TRANSITION_EFFECT.key, TransitionEffect.SlideHorizontal)
    var enableCreateMonthlyPlaylists by rememberPreference(ENABLE_CREATE_MONTHLY_PLAYLISTS.key, true)
    var showPinnedPlaylists by rememberPreference(SHOW_PINNED_PLAYLISTS.key, true)
    var showMonthlyPlaylists by rememberPreference(SHOW_MONTHLY_PLAYLISTS.key, true)

    var customThemeLight_Background0 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_0.key, DefaultLightColorPalette.background0.hashCode())
    var customThemeLight_Background1 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_1.key, DefaultLightColorPalette.background1.hashCode())
    var customThemeLight_Background2 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_2.key, DefaultLightColorPalette.background2.hashCode())
    var customThemeLight_Background3 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_3.key, DefaultLightColorPalette.background3.hashCode())
    var customThemeLight_Background4 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_4.key, DefaultLightColorPalette.background4.hashCode())
    var customThemeLight_Text by rememberPreference(CUSTOM_THEME_LIGHT_TEXT.key, DefaultLightColorPalette.text.hashCode())
    var customThemeLight_TextSecondary by rememberPreference(CUSTOM_THEME_LIGHT_TEXT_SECONDARY.key, DefaultLightColorPalette.textSecondary.hashCode())
    var customThemeLight_TextDisabled by rememberPreference(CUSTOM_THEME_LIGHT_TEXT_DISABLED.key, DefaultLightColorPalette.textDisabled.hashCode())
    var customThemeLight_IconButtonPlayer by rememberPreference(CUSTOM_THEME_LIGHT_ICON_BUTTON_PLAYER.key, DefaultLightColorPalette.iconButtonPlayer.hashCode())
    var customThemeLight_Accent by rememberPreference(CUSTOM_THEME_LIGHT_ACCENT.key, DefaultLightColorPalette.accent.hashCode())

    var customThemeDark_Background0 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_0.key, DefaultDarkColorPalette.background0.hashCode())
    var customThemeDark_Background1 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_1.key, DefaultDarkColorPalette.background1.hashCode())
    var customThemeDark_Background2 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_2.key, DefaultDarkColorPalette.background2.hashCode())
    var customThemeDark_Background3 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_3.key, DefaultDarkColorPalette.background3.hashCode())
    var customThemeDark_Background4 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_4.key, DefaultDarkColorPalette.background4.hashCode())
    var customThemeDark_Text by rememberPreference(CUSTOM_THEME_DARK_TEXT.key, DefaultDarkColorPalette.text.hashCode())
    var customThemeDark_TextSecondary by rememberPreference(CUSTOM_THEME_DARK_TEXT_SECONDARY.key, DefaultDarkColorPalette.textSecondary.hashCode())
    var customThemeDark_TextDisabled by rememberPreference(CUSTOM_THEME_DARK_TEXT_DISABLED.key, DefaultDarkColorPalette.textDisabled.hashCode())
    var customThemeDark_IconButtonPlayer by rememberPreference(CUSTOM_THEME_DARK_ICON_BUTTON_PLAYER.key, DefaultDarkColorPalette.iconButtonPlayer.hashCode())
    var customThemeDark_Accent by rememberPreference(CUSTOM_THEME_DARK_ACCENT.key, DefaultDarkColorPalette.accent.hashCode())

    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }
    var playerPosition by rememberPreference(PLAYER_POSITION.key, PlayerPosition.Bottom)

    var messageType by rememberPreference(MESSAGE_TYPE.key, MessageType.Modern)


    /*  ViMusic Mode Settings  */
    var showTopActionsBar by rememberPreference(SHOW_TOP_ACTIONS_BAR.key, true)
    var playerControlsType by rememberPreference(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
    var playerInfoType by rememberPreference(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential)
    var playerType by rememberPreference(PLAYER_TYPE.key, PlayerType.Modern)
    var queueType by rememberPreference(QUEUE_TYPE.key, QueueType.Modern)
    var fadingedge by rememberPreference(FADING_EDGE.key, false)
    var carousel by rememberPreference(CAROUSEL.key, true)
    var carouselSize by rememberPreference(CAROUSEL_SIZE.key, CarouselSize.Biggest)
    var thumbnailType by rememberPreference(THUMBNAIL_TYPE.key, ThumbnailType.Modern)
    var playerTimelineType by rememberPreference(PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    var playerThumbnailSize by rememberPreference(
        PLAYER_THUMBNAIL_SIZE.key,
        PlayerThumbnailSize.Biggest
    )
    var playerTimelineSize by rememberPreference(
        PLAYER_TIMELINE_SIZE.key,
        PlayerTimelineSize.Biggest
    )
    var playerInfoShowIcons by rememberPreference(PLAYER_INFO_SHOW_ICONS.key, true)
    var miniPlayerType by rememberPreference(
        MINI_PLAYER_TYPE.key,
        MiniPlayerType.Modern
    )
    var playerSwapControlsWithTimeline by rememberPreference(
        PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
        false
    )
    var playerPlayButtonType by rememberPreference(
        PLAYER_PLAY_BUTTON_TYPE.key,
        PlayerPlayButtonType.Disabled
    )
    var buttonzoomout by rememberPreference(BUTTON_ZOOM_OUT.key, false)
    var iconLikeType by rememberPreference(ICON_LIKE_TYPE.key, IconLikeType.Essential)
    var playerBackgroundColors by rememberPreference(
        PLAYER_BACKGROUND_COLORS.key,
        PlayerBackgroundColors.BlurredCoverColor
    )
    var blackgradient by rememberPreference(BLACK_GRADIENT.key, false)
    var showTotalTimeQueue by rememberPreference(SHOW_TOTAL_TIME_QUEUE.key, true)
    var showNextSongsInPlayer by rememberPreference(SHOW_NEXT_SONGS_IN_PLAYER.key, false)
    var showRemainingSongTime by rememberPreference(SHOW_REMAINING_SONG_TIME.key, true)
    var disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    var effectRotationEnabled by rememberPreference(EFFECT_ROTATION.key, true)
    var thumbnailTapEnabled by rememberPreference(THUMBNAIL_TAP_ENABLED.key, true)
    var clickLyricsText by rememberPreference(CLICK_ON_LYRICS_TEXT.key, true)
    var backgroundProgress by rememberPreference(
        BACKGROUND_PROGRESS.key,
        BackgroundProgress.MiniPlayer
    )
    var transparentBackgroundActionBarPlayer by rememberPreference(
        TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
        true
    )
    var actionspacedevenly by rememberPreference(ACTIONS_SPACED_EVENLY.key, false)
    var tapqueue by rememberPreference(TAP_QUEUE.key, true)
    var swipeUpQueue by rememberPreference(SWIPE_UP_QUEUE.key, true)
    var showButtonPlayerAddToPlaylist by rememberPreference(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true)
    var showButtonPlayerArrow by rememberPreference(SHOW_BUTTON_PLAYER_ARROW.key, true)
    //var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey.key, true)
    var showButtonPlayerLoop by rememberPreference(SHOW_BUTTON_PLAYER_LOOP.key, true)
    var showButtonPlayerLyrics by rememberPreference(SHOW_BUTTON_PLAYER_LYRICS.key, true)
    var expandedplayertoggle by rememberPreference(EXPANDED_PLAYER_TOGGLE.key, true)
    var showButtonPlayerShuffle by rememberPreference(SHOW_BUTTON_PLAYER_SHUFFLE.key, true)
    var showButtonPlayerSleepTimer by rememberPreference(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key, false)
    var showButtonPlayerMenu by rememberPreference(SHOW_BUTTON_PLAYER_MENU.key, false)
    var showButtonPlayerSystemEqualizer by rememberPreference(
        SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key,
        false
    )
    var showButtonPlayerDiscover by rememberPreference(SHOW_BUTTON_PLAYER_DISCOVER.key, false)
    var playerEnableLyricsPopupMessage by rememberPreference(
        PLAYER_ENABLE_LYRICS_POPUP_MESSAGE.key,
        true
    )
    var visualizerEnabled by rememberPreference(VISUALIZER_ENABLED.key, false)
    var showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    /*  ViMusic Mode Settings  */

    var queueSwipeLeftAction by rememberPreference(
        QUEUE_SWIPE_LEFT_ACTION.key,
        QueueSwipeAction.RemoveFromQueue
    )
    var queueSwipeRightAction by rememberPreference(
        QUEUE_SWIPE_RIGHT_ACTION.key,
        QueueSwipeAction.PlayNext
    )
    var playlistSwipeLeftAction by rememberPreference(
        PLAYLIST_SWIPE_LEFT_ACTION.key,
        PlaylistSwipeAction.Favourite
    )
    var playlistSwipeRightAction by rememberPreference(
        PLAYLIST_SWIPE_RIGHT_ACTION.key,
        PlaylistSwipeAction.PlayNext
    )
    var albumSwipeLeftAction by rememberPreference(
        ALBUM_SWIPE_LEFT_ACTION.key,
        AlbumSwipeAction.PlayNext
    )
    var albumSwipeRightAction by rememberPreference(
        ALBUM_SWIPE_RIGHT_ACTION.key,
        AlbumSwipeAction.Bookmark
    )

    var customColor by rememberPreference(CUSTOM_COLOR.key, Color.Green.hashCode())

    var usePlaceholder by rememberPreference(USE_PLACEHOLDER_IN_IMAGE_LOADER.key, true)

    var isEnabledFullscreen by rememberPreference(IS_ENABLED_FULLSCREEN.key, false)

    var isSnowEffectEnabled by rememberPreference(SHOW_SNOWFALL_EFFECT.key, false)

    var showListenerLevels by rememberPreference(SHOW_LISTENER_LEVELS.key, true)

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
    ) {

        if (resetCustomLightThemeDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_light_theme_colors),
                onDismiss = { resetCustomLightThemeDialog = false },
                onConfirm = {
                    resetCustomLightThemeDialog = false
                    customThemeLight_Background0 = DefaultLightColorPalette.background0.hashCode()
                    customThemeLight_Background1 = DefaultLightColorPalette.background1.hashCode()
                    customThemeLight_Background2 = DefaultLightColorPalette.background2.hashCode()
                    customThemeLight_Background3 = DefaultLightColorPalette.background3.hashCode()
                    customThemeLight_Background4 = DefaultLightColorPalette.background4.hashCode()
                    customThemeLight_Text = DefaultLightColorPalette.text.hashCode()
                    customThemeLight_TextSecondary = DefaultLightColorPalette.textSecondary.hashCode()
                    customThemeLight_TextDisabled = DefaultLightColorPalette.textDisabled.hashCode()
                    customThemeLight_IconButtonPlayer = DefaultLightColorPalette.iconButtonPlayer.hashCode()
                    customThemeLight_Accent = DefaultLightColorPalette.accent.hashCode()
                }
            )
        }

        if (resetCustomDarkThemeDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_dark_theme_colors),
                onDismiss = { resetCustomDarkThemeDialog = false },
                onConfirm = {
                    resetCustomDarkThemeDialog = false
                    customThemeDark_Background0 = DefaultDarkColorPalette.background0.hashCode()
                    customThemeDark_Background1 = DefaultDarkColorPalette.background1.hashCode()
                    customThemeDark_Background2 = DefaultDarkColorPalette.background2.hashCode()
                    customThemeDark_Background3 = DefaultDarkColorPalette.background3.hashCode()
                    customThemeDark_Background4 = DefaultDarkColorPalette.background4.hashCode()
                    customThemeDark_Text = DefaultDarkColorPalette.text.hashCode()
                    customThemeDark_TextSecondary = DefaultDarkColorPalette.textSecondary.hashCode()
                    customThemeDark_TextDisabled = DefaultDarkColorPalette.textDisabled.hashCode()
                    customThemeDark_IconButtonPlayer = DefaultDarkColorPalette.iconButtonPlayer.hashCode()
                    customThemeDark_Accent = DefaultDarkColorPalette.accent.hashCode()
                }
            )
        }
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
                    title = stringResource(R.string.user_interface),
                    iconId = R.drawable.ui,
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
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.user_interface))
            }

            settingsItem {

                if (search.input.isBlank() || stringResource(R.string.enable_fullscreen).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.enable_fullscreen),
                        text = stringResource(R.string.enable_fullscreen_info),
                        isChecked = isEnabledFullscreen,
                        onCheckedChange = { isEnabledFullscreen = it }
                    )

                var uiType by rememberPreference(UI_TYPE.key, UiType.RiPlay)
                if (search.input.isBlank() || stringResource(R.string.interface_in_use).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.interface_in_use),
                        selectedValue = uiType,
                        onValueSelected = {
                            uiType = it
                            if (uiType == UiType.ViMusic) {
                                disablePlayerHorizontalSwipe = true
                                disableIconButtonOnTop = true
                                playerTimelineType = PlayerTimelineType.Default
                                visualizerEnabled = false
                                playerThumbnailSize = PlayerThumbnailSize.Medium
                                thumbnailTapEnabled = true
                                showSearchTab = true
                                showStatsInNavbar = true
                                navigationBarPosition = NavigationBarPosition.Left
                                showTopActionsBar = false
                                playerType = PlayerType.Modern
                                queueType = QueueType.Modern
                                fadingedge = false
                                carousel = true
                                carouselSize = CarouselSize.Medium
                                thumbnailType = ThumbnailType.Essential
                                playerTimelineSize = PlayerTimelineSize.Medium
                                playerInfoShowIcons = true
                                miniPlayerType = MiniPlayerType.Modern
                                playerSwapControlsWithTimeline = false
                                transparentBackgroundActionBarPlayer = false
                                playerControlsType = PlayerControlsType.Essential
                                playerPlayButtonType = PlayerPlayButtonType.Disabled
                                buttonzoomout = true
                                iconLikeType = IconLikeType.Essential
                                playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
                                blackgradient = true
                                showTotalTimeQueue = false
                                showRemainingSongTime = false
                                showNextSongsInPlayer = false
                                disableScrollingText = false
                                effectRotationEnabled = true
                                clickLyricsText = true
                                playerEnableLyricsPopupMessage = true
                                backgroundProgress = BackgroundProgress.MiniPlayer
                                transparentBackgroundActionBarPlayer = true
                                actionspacedevenly = false
                                tapqueue = false
                                swipeUpQueue = true
                                showButtonPlayerDiscover = false
                                //showButtonPlayerDownload = false
                                showButtonPlayerAddToPlaylist = false
                                showButtonPlayerLoop = false
                                showButtonPlayerShuffle = false
                                showButtonPlayerLyrics = false
                                expandedplayertoggle = false
                                showButtonPlayerSleepTimer = false
                                showButtonPlayerSystemEqualizer = false
                                showButtonPlayerArrow = false
                                showButtonPlayerShuffle = false
                                showButtonPlayerMenu = true
                                showthumbnail = true
                                keepPlayerMinimized = false
                            } else {
                                disablePlayerHorizontalSwipe = false
                                disableIconButtonOnTop = false
                                playerTimelineType = lastPlayerTimelineType
                                playerThumbnailSize = lastPlayerThumbnailSize
                                playerPlayButtonType = lastPlayerPlayButtonType

                            }

                        },
                        valueText = {
                            it.name
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.theme).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.theme),
                        selectedValue = colorPaletteName,
                        onValueSelected = {
                            colorPaletteName = it
                            when (it) {
                                ColorPaletteName.PureBlack,
                                ColorPaletteName.ModernBlack -> colorPaletteMode =
                                    ColorPaletteMode.System

                                else -> {}
                            }
                        },
                        valueText = {
                            when (it) {
                                ColorPaletteName.Default -> stringResource(R.string._default)
                                ColorPaletteName.Dynamic -> stringResource(R.string.dynamic)
                                ColorPaletteName.PureBlack -> stringResource(R.string.theme_pure_black)
                                ColorPaletteName.ModernBlack -> stringResource(R.string.theme_modern_black)
                                ColorPaletteName.MaterialYou -> stringResource(R.string.theme_material_you)
                                ColorPaletteName.Customized -> stringResource(R.string.theme_customized)
                                ColorPaletteName.CustomColor -> stringResource(R.string.customcolor)
                            }
                        }
                    )

                AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.CustomColor) {
                    Column {
                        ColorSettingEntry(
                            title = stringResource(R.string.customcolor),
                            text = "",
                            color = Color(customColor),
                            onColorSelected = {
                                customColor = it.hashCode()
                            },
                            modifier = Modifier
                                .padding(start = 12.dp)
                        )
                        ImportantSettingsDescription(
                            text = stringResource(R.string.restarting_riplay_is_required),
                            modifier = Modifier
                                .padding(start = 12.dp)
                        )
                    }
                }
                AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.Customized) {
                    Column {
                        SettingsEntryGroupText(stringResource(R.string.title_customized_light_theme_colors))
                        ButtonBarSettingEntry(
                            title = stringResource(R.string.title_reset_customized_light_colors),
                            text = stringResource(R.string.info_click_to_reset_default_light_colors),
                            icon = R.drawable.trash,
                            onClick = { resetCustomLightThemeDialog = true }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_1),
                            text = "",
                            color = Color(customThemeLight_Background0),
                            onColorSelected = {
                                customThemeLight_Background0 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_2),
                            text = "",
                            color = Color(customThemeLight_Background1),
                            onColorSelected = {
                                customThemeLight_Background1 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_3),
                            text = "",
                            color = Color(customThemeLight_Background2),
                            onColorSelected = {
                                customThemeLight_Background2 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_4),
                            text = "",
                            color = Color(customThemeLight_Background3),
                            onColorSelected = {
                                customThemeLight_Background3 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_5),
                            text = "",
                            color = Color(customThemeLight_Background4),
                            onColorSelected = {
                                customThemeLight_Background4 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text),
                            text = "",
                            color = Color(customThemeLight_Text),
                            onColorSelected = {
                                customThemeLight_Text = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_secondary),
                            text = "",
                            color = Color(customThemeLight_TextSecondary),
                            onColorSelected = {
                                customThemeLight_TextSecondary = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_disabled),
                            text = "",
                            color = Color(customThemeLight_TextDisabled),
                            onColorSelected = {
                                customThemeLight_TextDisabled = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_icon_button_player),
                            text = "",
                            color = Color(customThemeLight_IconButtonPlayer),
                            onColorSelected = {
                                customThemeLight_IconButtonPlayer = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_accent),
                            text = "",
                            color = Color(customThemeLight_Accent),
                            onColorSelected = {
                                customThemeLight_Accent = it.hashCode()
                            }
                        )

                        SettingsEntryGroupText(stringResource(R.string.title_customized_dark_theme_colors))
                        ButtonBarSettingEntry(
                            title = stringResource(R.string.title_reset_customized_dark_colors),
                            text = stringResource(R.string.click_to_reset_default_dark_colors),
                            icon = R.drawable.trash,
                            onClick = { resetCustomDarkThemeDialog = true }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_1),
                            text = "",
                            color = Color(customThemeDark_Background0),
                            onColorSelected = {
                                customThemeDark_Background0 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_2),
                            text = "",
                            color = Color(customThemeDark_Background1),
                            onColorSelected = {
                                customThemeDark_Background1 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_3),
                            text = "",
                            color = Color(customThemeDark_Background2),
                            onColorSelected = {
                                customThemeDark_Background2 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_4),
                            text = "",
                            color = Color(customThemeDark_Background3),
                            onColorSelected = {
                                customThemeDark_Background3 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_5),
                            text = "",
                            color = Color(customThemeDark_Background4),
                            onColorSelected = {
                                customThemeDark_Background4 = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text),
                            text = "",
                            color = Color(customThemeDark_Text),
                            onColorSelected = {
                                customThemeDark_Text = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_secondary),
                            text = "",
                            color = Color(customThemeDark_TextSecondary),
                            onColorSelected = {
                                customThemeDark_TextSecondary = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_disabled),
                            text = "",
                            color = Color(customThemeDark_TextDisabled),
                            onColorSelected = {
                                customThemeDark_TextDisabled = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_icon_button_player),
                            text = "",
                            color = Color(customThemeDark_IconButtonPlayer),
                            onColorSelected = {
                                customThemeDark_IconButtonPlayer = it.hashCode()
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_accent),
                            text = "",
                            color = Color(customThemeDark_Accent),
                            onColorSelected = {
                                customThemeDark_Accent = it.hashCode()
                            }
                        )
                    }
                }

                if (search.input.isBlank() || stringResource(R.string.theme_mode).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.theme_mode),
                        selectedValue = colorPaletteMode,
                        isEnabled = when (colorPaletteName) {
                            ColorPaletteName.PureBlack -> false
                            ColorPaletteName.ModernBlack -> false
                            else -> {
                                true
                            }
                        },
                        onValueSelected = {
                            colorPaletteMode = it
                            //if (it == ColorPaletteMode.PitchBlack) colorPaletteName = ColorPaletteName.ModernBlack
                        },
                        valueText = {
                            when (it) {
                                ColorPaletteMode.Dark -> stringResource(R.string.dark)
                                ColorPaletteMode.Light -> stringResource(R.string._light)
                                ColorPaletteMode.System -> stringResource(R.string.system)
                                ColorPaletteMode.PitchBlack -> stringResource(R.string.theme_mode_pitch_black)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.navigation_bar_position).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.navigation_bar_position),
                        selectedValue = navigationBarPosition,
                        onValueSelected = { navigationBarPosition = it },
                        // As of version 0.6.53, changing navigation bar to top or bottom
                        // while using ViMusic theme breaks the UI
                        isEnabled = uiType != UiType.ViMusic,
                        valueText = {
                            when (it) {
                                NavigationBarPosition.Left -> stringResource(R.string.direction_left)
                                NavigationBarPosition.Right -> stringResource(R.string.direction_right)
                                NavigationBarPosition.Top -> stringResource(R.string.direction_top)
                                NavigationBarPosition.Bottom -> stringResource(R.string.direction_bottom)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.navigation_bar_type).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.navigation_bar_type),
                        selectedValue = navigationBarType,
                        onValueSelected = { navigationBarType = it },
                        valueText = {
                            when (it) {
                                NavigationBarType.IconAndText -> stringResource(R.string.icon_and_text)
                                NavigationBarType.IconOnly -> stringResource(R.string.only_icon)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.player_position).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.player_position),
                        selectedValue = playerPosition,
                        onValueSelected = { playerPosition = it },
                        valueText = {
                            when (it) {
                                PlayerPosition.Top -> stringResource(R.string.position_top)
                                PlayerPosition.Bottom -> stringResource(R.string.position_bottom)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.menu_style).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.menu_style),
                        selectedValue = menuStyle,
                        onValueSelected = { menuStyle = it },
                        valueText = {
                            when (it) {
                                MenuStyle.Grid -> stringResource(R.string.style_grid)
                                MenuStyle.List -> stringResource(R.string.style_list)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.message_type).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.message_type),
                        selectedValue = messageType,
                        onValueSelected = { messageType = it },
                        valueText = {
                            when (it) {
                                MessageType.Modern -> stringResource(R.string.message_type_modern)
                                MessageType.Essential -> stringResource(R.string.message_type_essential)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.default_page).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.default_page),
                        selectedValue = indexNavigationTab,
                        onValueSelected = { indexNavigationTab = it },
                        valueText = {
                            when (it) {
                                HomeScreenTabs.Default -> stringResource(R.string._default)
                                HomeScreenTabs.Home -> stringResource(R.string.home)
                                //HomeScreenTabs.LocalSongs -> stringResource(R.string.on_device)
                                HomeScreenTabs.Songs -> stringResource(R.string.songs)
                                HomeScreenTabs.Albums -> stringResource(R.string.albums)
                                HomeScreenTabs.Artists -> stringResource(R.string.artists)
                                HomeScreenTabs.Playlists -> stringResource(R.string.playlists)
                                HomeScreenTabs.Search -> stringResource(R.string.search)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.transition_effect).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.transition_effect),
                        selectedValue = transitionEffect,
                        onValueSelected = { transitionEffect = it },
                        valueText = {
                            when (it) {
                                TransitionEffect.None -> stringResource(R.string.none)
                                TransitionEffect.Expand -> stringResource(R.string.te_expand)
                                TransitionEffect.Fade -> stringResource(R.string.te_fade)
                                TransitionEffect.Scale -> stringResource(R.string.te_scale)
                                TransitionEffect.SlideVertical -> stringResource(R.string.te_slide_vertical)
                                TransitionEffect.SlideHorizontal -> stringResource(R.string.te_slide_horizontal)
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.snow_effect).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.snow_effect),
                        text = "",
                        isChecked = isSnowEffectEnabled,
                        onCheckedChange = { isSnowEffectEnabled = it }
                    )

                if (UiType.ViMusic.isCurrent()) {
                    if (search.input.isBlank() || stringResource(R.string.vimusic_show_search_button_in_navigation_bar).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.vimusic_show_search_button_in_navigation_bar),
                            text = stringResource(R.string.vismusic_only_in_left_right_navigation_bar),
                            isChecked = showSearchTab,
                            onCheckedChange = { showSearchTab = it }
                        )



                    if (search.input.isBlank() || stringResource(R.string.show_statistics_in_navigation_bar).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_statistics_in_navigation_bar),
                            text = "",
                            isChecked = showStatsInNavbar,
                            onCheckedChange = { showStatsInNavbar = it }
                        )
                }

                if (uiType == UiType.ViMusic) {
                    if (search.input.isBlank() || stringResource(R.string.show_floating_icon).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_floating_icon),
                            text = "",
                            isChecked = showFloatingIcon,
                            onCheckedChange = { showFloatingIcon = it }
                        )
                } else showFloatingIcon = false



                if (search.input.isBlank() || stringResource(R.string.settings_use_font_type).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_use_font_type),
                        selectedValue = fontType,
                        onValueSelected = { fontType = it },
                        valueText = {
                            when (it) {
                                FontType.Rubik -> FontType.Rubik.name
                                FontType.Poppins -> FontType.Poppins.name
                            }
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.use_system_font).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.use_system_font),
                        text = stringResource(R.string.use_font_by_the_system),
                        isChecked = useSystemFont,
                        onCheckedChange = { useSystemFont = it }
                    )

                if (search.input.isBlank() || stringResource(R.string.apply_font_padding).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.apply_font_padding),
                        text = stringResource(R.string.add_spacing_around_texts),
                        isChecked = applyFontPadding,
                        onCheckedChange = { applyFontPadding = it }
                    )


                if (search.input.isBlank() || stringResource(R.string.swipe_to_action).contains(
                        search.input,
                        true
                    )
                ) {
                    SwitchSettingEntry(
                        title = stringResource(R.string.swipe_to_action),
                        text = stringResource(R.string.activate_the_action_menu_by_swiping_the_song_left_or_right),
                        isChecked = isSwipeToActionEnabled,
                        onCheckedChange = { isSwipeToActionEnabled = it }
                    )

                    AnimatedVisibility(visible = isSwipeToActionEnabled) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            EnumValueSelectorSettingsEntry<QueueSwipeAction>(
                                title = stringResource(R.string.queue_and_local_playlists_left_swipe),
                                selectedValue = queueSwipeLeftAction,
                                onValueSelected = {
                                    queueSwipeLeftAction = it
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<QueueSwipeAction>(
                                title = stringResource(R.string.queue_and_local_playlists_right_swipe),
                                selectedValue = queueSwipeRightAction,
                                onValueSelected = {
                                    queueSwipeRightAction = it
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<PlaylistSwipeAction>(
                                title = stringResource(R.string.playlist_left_swipe),
                                selectedValue = playlistSwipeLeftAction,
                                onValueSelected = {
                                    playlistSwipeLeftAction = it
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<PlaylistSwipeAction>(
                                title = stringResource(R.string.playlist_right_swipe),
                                selectedValue = playlistSwipeRightAction,
                                onValueSelected = {
                                    playlistSwipeRightAction = it
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<AlbumSwipeAction>(
                                title = stringResource(R.string.album_left_swipe),
                                selectedValue = albumSwipeLeftAction,
                                onValueSelected = {
                                    albumSwipeLeftAction = it
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<AlbumSwipeAction>(
                                title = stringResource(R.string.album_right_swipe),
                                selectedValue = albumSwipeRightAction,
                                onValueSelected = {
                                    albumSwipeRightAction = it
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                        }
                    }
                }

                if (search.input.isBlank() || stringResource(R.string.use_placeholder_in_imageloader).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.use_placeholder_in_imageloader),
                        text = stringResource(R.string.use_placeholder_in_imageloader_info),
                        isChecked = usePlaceholder,
                        onCheckedChange = { usePlaceholder = it }
                    )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(title = stringResource(R.string.songs).uppercase())
            }

            settingsItem {
                if (search.input.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.favorites)}".contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.favorites)}",
                        text = "",
                        isChecked = showFavoritesPlaylist,
                        onCheckedChange = { showFavoritesPlaylist = it }
                    )

                if (search.input.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.my_playlist_top)}".contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${
                            stringResource(R.string.my_playlist_top).format(
                                maxTopPlaylistItems
                            )
                        }",
                        text = "",
                        isChecked = showMyTopPlaylist,
                        onCheckedChange = { showMyTopPlaylist = it }
                    )
                if (search.input.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.on_device)}".contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.on_device)}",
                        text = "",
                        isChecked = showOnDevicePlaylist,
                        onCheckedChange = { showOnDevicePlaylist = it }
                    )

                if (search.input.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.disliked)}".contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.disliked)}",
                        text = "",
                        isChecked = showDislikedPlaylist,
                        onCheckedChange = { showDislikedPlaylist = it }
                    )

            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(title = stringResource(R.string.playlists).uppercase())
            }

            settingsItem {

                if (search.input.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.pinned_playlists)}".contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.pinned_playlists)}",
                        text = "",
                        isChecked = showPinnedPlaylists,
                        onCheckedChange = { showPinnedPlaylists = it }
                    )

                if (search.input.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}".contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}",
                        text = "",
                        isChecked = showMonthlyPlaylists,
                        onCheckedChange = { showMonthlyPlaylists = it }
                    )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.monthly_playlists).uppercase())
            }

            settingsItem {
                if (search.input.isBlank() || stringResource(R.string.monthly_playlists).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.enable_monthly_playlists_creation),
                        text = "",
                        isChecked = enableCreateMonthlyPlaylists,
                        onCheckedChange = {
                            enableCreateMonthlyPlaylists = it
                        }
                    )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.smart_recommendations))
            }

            settingsItem {
                if (search.input.isBlank() || stringResource(R.string.statistics_max_number_of_items).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        selectedValue = recommendationsNumber,
                        onValueSelected = { recommendationsNumber = it },
                        valueText = {
                            it.number.toString()
                        }
                    )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.statistics))
            }

            settingsItem {
                if (search.input.isBlank() || stringResource(R.string.statistics_max_number_of_items).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        selectedValue = maxStatisticsItems,
                        onValueSelected = { maxStatisticsItems = it },
                        valueText = {
                            it.number.toString()
                        }
                    )

                if (search.input.isBlank() || stringResource(R.string.listening_time).contains(
                        search.input,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.listening_time),
                        text = stringResource(R.string.shows_the_number_of_songs_heard_and_their_listening_time),
                        isChecked = showStatsListeningTime,
                        onCheckedChange = {
                            showStatsListeningTime = it
                        }
                    )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.playlist_top))
            }

            settingsItem {
                if (search.input.isBlank() || stringResource(R.string.statistics_max_number_of_items).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        selectedValue = maxTopPlaylistItems,
                        onValueSelected = { maxTopPlaylistItems = it },
                        valueText = {
                            it.number.toString()
                        }
                    )


            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.listener_levels))
            }

            settingsItem {
                SwitchSettingEntry(
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.listener_levels)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.listener_levels
                    ),
                    isChecked = showListenerLevels,
                    onCheckedChange = {
                        showListenerLevels = it
                    }
                )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsGroupSpacer()
                SettingsEntryGroupText(stringResource(R.string.settings_reset))
            }

            settingsItem {
                var resetToDefault by remember { mutableStateOf(false) }
                val context = LocalContext.current
                ButtonBarSettingEntry(
                    title = stringResource(R.string.settings_reset),
                    text = stringResource(R.string.settings_restore_default_settings),
                    icon = R.drawable.refresh,
                    iconColor = colorPalette().text,
                    onClick = { resetToDefault = true },
                )
                if (resetToDefault) {
                    DefaultUiSettings()
                    resetToDefault = false
                    navController.popBackStack()
                    SmartMessage(stringResource(R.string.done), context = context)
                }
            }

        }
    }
}
