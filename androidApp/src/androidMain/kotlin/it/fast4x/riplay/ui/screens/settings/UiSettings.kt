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
import androidx.compose.runtime.collectAsState
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
import it.fast4x.riplay.LocalAppSettings
import it.fast4x.riplay.LocalAppearanceSettings
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
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_DISLIKED_PLAYLIST
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

    val appearanceSettingsVieModel = LocalAppearanceSettings.current
    val appearanceSettings = appearanceSettingsVieModel.activeSettings.collectAsState().value

    val appSettingsViewModel = LocalAppSettings.current
    val appSettings = appSettingsViewModel.activeSettings.collectAsState().value

    //var recommendationsNumber by rememberPreference(RECOMMENDATIONS_NUMBER.key,   RecommendationsNumber.`5`)
    val recommendationsNumber = appSettings.recommendationsNumber

    //var keepPlayerMinimized by rememberPreference(KEEP_PLAYER_MINIMIZED.key,   false)
    //val keepPlayerMinimized = appSettings.keepPlayerMinimized

    //var disableIconButtonOnTop by rememberPreference(DISABLE_ICON_BUTTON_ON_TOP.key, false)
    val disableIconButtonOnTop = appSettings.disableIconButtonOnTop
    //var lastPlayerTimelineType by rememberPreference(LAST_PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    val lastPlayerTimelineType = appearanceSettings.lastPlayerTimelineType
    //var lastPlayerThumbnailSize by rememberPreference(LAST_PLAYER_THUMBNAIL_SIZE.key, PlayerThumbnailSize.Medium)
    val lastPlayerThumbnailSize = appearanceSettings.lastPlayerThumbnailSize
    //var disablePlayerHorizontalSwipe by rememberPreference(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, false)
    val disablePlayerHorizontalSwipe = appearanceSettings.disablePlayerHorizontalSwipe

    //var lastPlayerPlayButtonType by rememberPreference(LAST_PLAYER_PLAY_BUTTON_TYPE.key, PlayerPlayButtonType.Rectangular)
    val lastPlayerPlayButtonType = appearanceSettings.lastPlayerPlayButtonType

    //var colorPaletteName by rememberPreference(COLOR_PALETTE_NAME.key, ColorPaletteName.Dynamic)
    val colorPaletteName = appearanceSettings.colorPaletteName
    //var colorPaletteMode by rememberPreference(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
    val colorPaletteMode = appearanceSettings.colorPaletteMode
//    var indexNavigationTab by rememberPreference(
//        INDEX_NAVIGATION_TAB.key,
//        HomeScreenTabs.Default
//    )
    val indexNavigationTab = appSettings.indexNavigationTab
    //var fontType by rememberPreference(FONT_TYPE.key, FontType.Rubik)
    val fontType = appSettings.fontType
    //var useSystemFont by rememberPreference(USE_SYSTEM_FONT.key, false)
    val useSystemFont = appSettings.useSystemFont
    //var applyFontPadding by rememberPreference(APPLY_FONT_PADDING.key, false)
    val applyFontPadding = appSettings.applyFontPadding
    //var isSwipeToActionEnabled by rememberPreference(IS_SWIPE_TO_ACTION_ENABLED.key, true)
    val isSwipeToActionEnabled = appSettings.isSwipeToActionEnabled
    //var showSearchTab by rememberPreference(SHOW_SEARCH_TAB.key, false)
    val showSearchTab = appSettings.showSearchTab
    //var showStatsInNavbar by rememberPreference(SHOW_STATS_IN_NAVBAR.key, false)
    val showStatsInNavbar = appSettings.showStatsInNavbar

//    var maxStatisticsItems by rememberPreference(
//        MAX_STATISTICS_ITEMS.key,
//        MaxStatisticsItems.`10`
//    )
    val maxStatisticsItems = appSettings.maxStatisticsItems

    //var showStatsListeningTime by rememberPreference(SHOW_STATS_LISTENING_TIME.key,   true)
    val showStatsListeningTime = appSettings.showStatsListeningTime

//    var maxTopPlaylistItems by rememberPreference(
//        MAX_TOP_PLAYLIST_ITEMS.key,
//        MaxTopPlaylistItems.`10`
//    )
    val maxTopPlaylistItems = appSettings.maxTopPlaylistItems

    //var navigationBarPosition by rememberPreference(NAVIGATION_BAR_POSITION.key, NavigationBarPosition.Bottom)
    val navigationBarPosition = appSettings.navigationBarPosition
    //var navigationBarType by rememberPreference(NAVIGATION_BAR_TYPE.key, NavigationBarType.IconAndText)
    val navigationBarType = appSettings.navigationBarType
    val search = Search.init()

    //var showFavoritesPlaylist by rememberPreference(SHOW_FAVORITES_PLAYLIST.key, true)
    val showFavoritesPlaylist = appSettings.showFavoritesPlaylist
    //var showMyTopPlaylist by rememberPreference(SHOW_MY_TOP_PLAYLIST.key, true)
    val showMyTopPlaylist = appSettings.showMyTopPlaylist
    //var showOnDevicePlaylist by rememberPreference(SHOW_ON_DEVICE_PLAYLIST.key, true)
    val showOnDevicePlaylist = appSettings.showOnDevicePlaylist
    //var showDislikedPlaylist by rememberPreference(SHOW_DISLIKED_PLAYLIST.key, false)
    val showDislikedPlaylist = appSettings.showDislikedPlaylist
    //var showFloatingIcon by rememberPreference(SHOW_FLOATING_ICON.key, false)
    val showFloatingIcon = appSettings.showFloatingIcon
    //var menuStyle by rememberPreference(MENU_STYLE.key, MenuStyle.List)
    val menuStyle = appSettings.menuStyle
    //var transitionEffect by rememberPreference(TRANSITION_EFFECT.key, TransitionEffect.SlideHorizontal)
    val transitionEffect = appSettings.transitionEffect
    //var enableCreateMonthlyPlaylists by rememberPreference(ENABLE_CREATE_MONTHLY_PLAYLISTS.key, true)
    val enableCreateMonthlyPlaylists = appSettings.enableCreateMonthlyPlaylists
    //var showPinnedPlaylists by rememberPreference(SHOW_PINNED_PLAYLISTS.key, true)
    val showPinnedPlaylists = appSettings.showPinnedPlaylists
    //var showMonthlyPlaylists by rememberPreference(SHOW_MONTHLY_PLAYLISTS.key, true)
    val showMonthlyPlaylists = appSettings.showMonthlyPlaylists

    //var customThemeLight_Background0 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_0.key, DefaultLightColorPalette.background0.hashCode())
    val customThemeLight_Background0 = appearanceSettings.customThemeLight_Background0
    //var customThemeLight_Background1 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_1.key, DefaultLightColorPalette.background1.hashCode())
    val customThemeLight_Background1 = appearanceSettings.customThemeLight_Background1
    //var customThemeLight_Background2 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_2.key, DefaultLightColorPalette.background2.hashCode())
    val customThemeLight_Background2 = appearanceSettings.customThemeLight_Background2
    //var customThemeLight_Background3 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_3.key, DefaultLightColorPalette.background3.hashCode())
    val customThemeLight_Background3 = appearanceSettings.customThemeLight_Background3
    //var customThemeLight_Background4 by rememberPreference(CUSTOM_THEME_LIGHT_BACKGROUND_4.key, DefaultLightColorPalette.background4.hashCode())
    val customThemeLight_Background4 = appearanceSettings.customThemeLight_Background4
    //var customThemeLight_Text by rememberPreference(CUSTOM_THEME_LIGHT_TEXT.key, DefaultLightColorPalette.text.hashCode())
    val customThemeLight_Text = appearanceSettings.customThemeLight_Text
    //var customThemeLight_TextSecondary by rememberPreference(CUSTOM_THEME_LIGHT_TEXT_SECONDARY.key, DefaultLightColorPalette.textSecondary.hashCode())
    val customThemeLight_TextSecondary = appearanceSettings.customThemeLight_TextSecondary
    //var customThemeLight_TextDisabled by rememberPreference(CUSTOM_THEME_LIGHT_TEXT_DISABLED.key, DefaultLightColorPalette.textDisabled.hashCode())
    val customThemeLight_TextDisabled = appearanceSettings.customThemeLight_TextDisabled
    //var customThemeLight_IconButtonPlayer by rememberPreference(CUSTOM_THEME_LIGHT_ICON_BUTTON_PLAYER.key, DefaultLightColorPalette.iconButtonPlayer.hashCode())
    val customThemeLight_IconButtonPlayer = appearanceSettings.customThemeLight_IconButtonPlayer
    //var customThemeLight_Accent by rememberPreference(CUSTOM_THEME_LIGHT_ACCENT.key, DefaultLightColorPalette.accent.hashCode())
    val customThemeLight_Accent = appearanceSettings.customThemeLight_Accent

    //var customThemeDark_Background0 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_0.key, DefaultDarkColorPalette.background0.hashCode())
    val customThemeDark_Background0 = appearanceSettings.customThemeDark_Background0
    //var customThemeDark_Background1 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_1.key, DefaultDarkColorPalette.background1.hashCode())
    val customThemeDark_Background1 = appearanceSettings.customThemeDark_Background1
    //var customThemeDark_Background2 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_2.key, DefaultDarkColorPalette.background2.hashCode())
    val customThemeDark_Background2 = appearanceSettings.customThemeDark_Background2
    //var customThemeDark_Background3 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_3.key, DefaultDarkColorPalette.background3.hashCode())
    val customThemeDark_Background3 = appearanceSettings.customThemeDark_Background3
    //var customThemeDark_Background4 by rememberPreference(CUSTOM_THEME_DARK_BACKGROUND_4.key, DefaultDarkColorPalette.background4.hashCode())
    val customThemeDark_Background4 = appearanceSettings.customThemeDark_Background4
    //var customThemeDark_Text by rememberPreference(CUSTOM_THEME_DARK_TEXT.key, DefaultDarkColorPalette.text.hashCode())
    val customThemeDark_Text = appearanceSettings.customThemeDark_Text
    //var customThemeDark_TextSecondary by rememberPreference(CUSTOM_THEME_DARK_TEXT_SECONDARY.key, DefaultDarkColorPalette.textSecondary.hashCode())
    val customThemeDark_TextSecondary = appearanceSettings.customThemeDark_TextSecondary
    //var customThemeDark_TextDisabled by rememberPreference(CUSTOM_THEME_DARK_TEXT_DISABLED.key, DefaultDarkColorPalette.textDisabled.hashCode())
    val customThemeDark_TextDisabled = appearanceSettings.customThemeDark_TextDisabled
    //var customThemeDark_IconButtonPlayer by rememberPreference(CUSTOM_THEME_DARK_ICON_BUTTON_PLAYER.key, DefaultDarkColorPalette.iconButtonPlayer.hashCode())
    val customThemeDark_IconButtonPlayer = appearanceSettings.customThemeDark_IconButtonPlayer
    //var customThemeDark_Accent by rememberPreference(CUSTOM_THEME_DARK_ACCENT.key, DefaultDarkColorPalette.accent.hashCode())
    val customThemeDark_Accent = appearanceSettings.customThemeDark_Accent

    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }
    //var playerPosition by rememberPreference(PLAYER_POSITION.key, PlayerPosition.Bottom)
    val playerPosition = appSettings.playerPosition

    //var messageType by rememberPreference(MESSAGE_TYPE.key, MessageType.Modern)
    val messageType = appSettings.messageType


    /*  ViMusic Mode Settings  */
    //var showTopActionsBar by rememberPreference(SHOW_TOP_ACTIONS_BAR.key, true)
    //val showTopActionsBar = appearanceSettings.showTopActionsBar
    //var playerControlsType by rememberPreference(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
    //val playerControlsType = appearanceSettings.playerControlsType
    //var playerInfoType by rememberPreference(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential)
    //var playerType by rememberPreference(PLAYER_TYPE.key, PlayerType.Modern)
    //val playerType = appearanceSettings.playerType
    //var queueType by rememberPreference(QUEUE_TYPE.key, QueueType.Modern)
    //val queueType = appearanceSettings.queueType
    //var fadingedge by rememberPreference(FADING_EDGE.key, false)
    //val fadingedge = appearanceSettings.fadingedge
    //var carousel by rememberPreference(CAROUSEL.key, true)
    //val carousel = appearanceSettings.carousel
    //var carouselSize by rememberPreference(CAROUSEL_SIZE.key, CarouselSize.Biggest)
    //val carouselSize = appearanceSettings.carouselSize
    //var thumbnailType by rememberPreference(THUMBNAIL_TYPE.key, ThumbnailType.Modern)
    //val thumbnailType = appearanceSettings.thumbnailType
    //var playerTimelineType by rememberPreference(PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.Default)
    //val playerTimelineType = appearanceSettings.playerTimelineType
//    var playerThumbnailSize by rememberPreference(
//        PLAYER_THUMBNAIL_SIZE.key,
//        PlayerThumbnailSize.Biggest
//    )
    //val playerThumbnailSize = appearanceSettings.playerThumbnailSize
//    var playerTimelineSize by rememberPreference(
//        PLAYER_TIMELINE_SIZE.key,
//        PlayerTimelineSize.Biggest
//    )
    //val playerTimelineSize = appearanceSettings.playerTimelineSize
    //var playerInfoShowIcons by rememberPreference(PLAYER_INFO_SHOW_ICONS.key, true)
    //val playerInfoShowIcons = appearanceSettings.playerInfoShowIcons
//    var miniPlayerType by rememberPreference(
//        MINI_PLAYER_TYPE.key,
//        MiniPlayerType.Modern
//    )
    //val miniPlayerType = appearanceSettings.miniPlayerType
//    var playerSwapControlsWithTimeline by rememberPreference(
//        PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
//        false
//    )
    //val playerSwapControlsWithTimeline = appearanceSettings.playerSwapControlsWithTimeline
//    var playerPlayButtonType by rememberPreference(
//        PLAYER_PLAY_BUTTON_TYPE.key,
//        PlayerPlayButtonType.Disabled
//    )
    //val playerPlayButtonType = appearanceSettings.playerPlayButtonType
    //var buttonzoomout by rememberPreference(BUTTON_ZOOM_OUT.key, false)
    //val buttonzoomout = appearanceSettings.buttonzoomout
    //var iconLikeType by rememberPreference(ICON_LIKE_TYPE.key, IconLikeType.Essential)
    //val iconLikeType = appearanceSettings.iconLikeType
//    var playerBackgroundColors by rememberPreference(
//        PLAYER_BACKGROUND_COLORS.key,
//        PlayerBackgroundColors.BlurredCoverColor
//    )
    //val playerBackgroundColors = appearanceSettings.playerBackgroundColors
    //var blackgradient by rememberPreference(BLACK_GRADIENT.key, false)
    //val blackgradient = appearanceSettings.blackgradient
    //var showTotalTimeQueue by rememberPreference(SHOW_TOTAL_TIME_QUEUE.key, true)
    //val showTotalTimeQueue = appearanceSettings.showTotalTimeQueue
    //var showNextSongsInPlayer by rememberPreference(SHOW_NEXT_SONGS_IN_PLAYER.key, false)
    //val showNextSongsInPlayer = appearanceSettings.showNextSongsInPlayer
    //var showRemainingSongTime by rememberPreference(SHOW_REMAINING_SONG_TIME.key, true)
    //val showRemainingSongTime = appearanceSettings.showRemainingSongTime
    //var disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    //val disableScrollingText = appearanceSettings.disableScrollingText
    //var thumbnailTapEnabled by rememberPreference(THUMBNAIL_TAP_ENABLED.key, true)
    //val thumbnailTapEnabled = appearanceSettings.thumbnailTapEnabled
    //var clickLyricsText by rememberPreference(CLICK_ON_LYRICS_TEXT.key, true)
    //val clickLyricsText = appearanceSettings.clickLyricsText
//    var backgroundProgress by rememberPreference(
//        BACKGROUND_PROGRESS.key,
//        BackgroundProgress.MiniPlayer
//    )
    //val backgroundProgress = appearanceSettings.backgroundProgress
//    var transparentBackgroundActionBarPlayer by rememberPreference(
//        TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
//        true
//    )
    //val transparentBackgroundActionBarPlayer = appearanceSettings.transparentBackgroundActionBarPlayer
    //var actionspacedevenly by rememberPreference(ACTIONS_SPACED_EVENLY.key, false)
    //val actionspacedevenly = appearanceSettings.actionsSpacedEvenly
    //var tapqueue by rememberPreference(TAP_QUEUE.key, true)
    //val tapqueue = appearanceSettings.tapqueue
    //var swipeUpQueue by rememberPreference(SWIPE_UP_QUEUE.key, true)
    //val swipeUpQueue = appearanceSettings.swipeUpQueue
    //var showButtonPlayerAddToPlaylist by rememberPreference(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true)
    //val showButtonPlayerAddToPlaylist = appearanceSettings.showButtonPlayerAddToPlaylist
    //var showButtonPlayerArrow by rememberPreference(SHOW_BUTTON_PLAYER_ARROW.key, true)
    //val showButtonPlayerArrow = appearanceSettings.showButtonPlayerArrow
    //var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey.key, true)
    //var showButtonPlayerLoop by rememberPreference(SHOW_BUTTON_PLAYER_LOOP.key, true)
    //val showButtonPlayerLoop = appearanceSettings.showButtonPlayerLoop
    //var showButtonPlayerLyrics by rememberPreference(SHOW_BUTTON_PLAYER_LYRICS.key, true)
    //val showButtonPlayerLyrics = appearanceSettings.showButtonPlayerLyrics
    //var expandedplayertoggle by rememberPreference(EXPANDED_PLAYER_TOGGLE.key, true)
    //val expandedplayertoggle = appearanceSettings.expandedPlayerToggle
    //var showButtonPlayerShuffle by rememberPreference(SHOW_BUTTON_PLAYER_SHUFFLE.key, true)
    //val showButtonPlayerShuffle = appearanceSettings.showButtonPlayerShuffle
    //var showButtonPlayerSleepTimer by rememberPreference(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key, false)
    //val showButtonPlayerSleepTimer = appearanceSettings.showButtonPlayerSleepTimer
    //var showButtonPlayerMenu by rememberPreference(SHOW_BUTTON_PLAYER_MENU.key, false)
    //val showButtonPlayerMenu = appearanceSettings.showButtonPlayerMenu
//    var showButtonPlayerSystemEqualizer by rememberPreference(
//        SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key,
//        false
//    )
    //val showButtonPlayerSystemEqualizer = appearanceSettings.showButtonPlayerSystemEqualizer
    //var showButtonPlayerDiscover by rememberPreference(SHOW_BUTTON_PLAYER_DISCOVER.key, false)
    //val showButtonPlayerDiscover = appearanceSettings.showButtonPlayerDiscover
//    var playerEnableLyricsPopupMessage by rememberPreference(
//        PLAYER_ENABLE_LYRICS_POPUP_MESSAGE.key,
//        true
//    )
    //val playerEnableLyricsPopupMessage = appearanceSettings.playerEnableLyricsPopupMessage
    //var visualizerEnabled by rememberPreference(VISUALIZER_ENABLED.key, false)
    //val visualizerEnabled = appearanceSettings.visualizerEnabled
    //var showthumbnail by rememberPreference(SHOW_THUMBNAIL.key, true)
    //val showthumbnail = appearanceSettings.showThumbnail
    /*  ViMusic Mode Settings  */

//    var queueSwipeLeftAction by rememberPreference(
//        QUEUE_SWIPE_LEFT_ACTION.key,
//        QueueSwipeAction.RemoveFromQueue
//    )
    val queueSwipeLeftAction = appSettings.queueSwipeLeftAction
//    var queueSwipeRightAction by rememberPreference(
//        QUEUE_SWIPE_RIGHT_ACTION.key,
//        QueueSwipeAction.PlayNext
//
    val queueSwipeRightAction = appSettings.queueSwipeRightAction
//    var playlistSwipeLeftAction by rememberPreference(
//        PLAYLIST_SWIPE_LEFT_ACTION.key,
//        PlaylistSwipeAction.Favourite
//    )
    val playlistSwipeLeftAction = appSettings.playlistSwipeLeftAction
//    var playlistSwipeRightAction by rememberPreference(
//        PLAYLIST_SWIPE_RIGHT_ACTION.key,
//        PlaylistSwipeAction.PlayNext
//    )
    val playlistSwipeRightAction = appSettings.playlistSwipeRightAction
//    var albumSwipeLeftAction by rememberPreference(
//        ALBUM_SWIPE_LEFT_ACTION.key,
//        AlbumSwipeAction.PlayNext
//    )
    val albumSwipeLeftAction = appSettings.albumSwipeLeftAction
//    var albumSwipeRightAction by rememberPreference(
//        ALBUM_SWIPE_RIGHT_ACTION.key,
//        AlbumSwipeAction.Bookmark
//    )
    val albumSwipeRightAction = appSettings.albumSwipeRightAction
    //var customColor by rememberPreference(CUSTOM_COLOR.key, Color.Green.hashCode())
    val customColor = appearanceSettings.customColor

    //var usePlaceholder by rememberPreference(USE_PLACEHOLDER_IN_IMAGE_LOADER.key, true)
    val usePlaceholderInImageLoader = appSettings.usePlaceholderInImageLoader

    //var isEnabledFullscreen by rememberPreference(IS_ENABLED_FULLSCREEN.key, false)
    val isEnabledFullscreen = appSettings.isEnabledFullScreen

    //var isSnowEffectEnabled by rememberPreference(SHOW_SNOWFALL_EFFECT.key, false)
    val isSnowEffectEnabled = appSettings.showSnowfallEffect

    //var showListenerLevels by rememberPreference(SHOW_LISTENER_LEVELS.key, true)
    val showListenerLevels = appSettings.showListenerLevels

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
                    val new = appearanceSettings.copy(
                        customThemeLight_Background0 = DefaultLightColorPalette.background0.hashCode(),
                        customThemeLight_Background1 = DefaultLightColorPalette.background1.hashCode(),
                        customThemeLight_Background2 = DefaultLightColorPalette.background2.hashCode(),
                        customThemeLight_Background3 = DefaultLightColorPalette.background3.hashCode(),
                        customThemeLight_Background4 = DefaultLightColorPalette.background4.hashCode(),
                        customThemeLight_Text = DefaultLightColorPalette.text.hashCode(),
                        customThemeLight_TextSecondary = DefaultLightColorPalette.textSecondary.hashCode(),
                        customThemeLight_TextDisabled = DefaultLightColorPalette.textDisabled.hashCode(),
                        customThemeLight_IconButtonPlayer = DefaultLightColorPalette.iconButtonPlayer.hashCode(),
                        customThemeLight_Accent = DefaultLightColorPalette.accent.hashCode()
                    )
                    appearanceSettingsVieModel.updatePreset(new)
                }
            )
        }

        if (resetCustomDarkThemeDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_dark_theme_colors),
                onDismiss = { resetCustomDarkThemeDialog = false },
                onConfirm = {
                    resetCustomDarkThemeDialog = false
                    val new = appearanceSettings.copy(
                        customThemeDark_Background0 = DefaultDarkColorPalette.background0.hashCode(),
                        customThemeDark_Background1 = DefaultDarkColorPalette.background1.hashCode(),
                        customThemeDark_Background2 = DefaultDarkColorPalette.background2.hashCode(),
                        customThemeDark_Background3 = DefaultDarkColorPalette.background3.hashCode(),
                        customThemeDark_Background4 = DefaultDarkColorPalette.background4.hashCode(),
                        customThemeDark_Text = DefaultDarkColorPalette.text.hashCode(),
                        customThemeDark_TextSecondary = DefaultDarkColorPalette.textSecondary.hashCode(),
                        customThemeDark_TextDisabled = DefaultDarkColorPalette.textDisabled.hashCode(),
                        customThemeDark_IconButtonPlayer = DefaultDarkColorPalette.iconButtonPlayer.hashCode(),
                        customThemeDark_Accent = DefaultDarkColorPalette.accent.hashCode()
                    )
                    appearanceSettingsVieModel.updatePreset(new)
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
                        onCheckedChange = {
                            val new = appSettings.copy(isEnabledFullScreen = it)
                            appSettingsViewModel.updateSettings(new)
                        }
                    )

                //var uiType by rememberPreference(UI_TYPE.key, UiType.RiPlay)
                val uiType = appSettings.uiType
                if (search.input.isBlank() || stringResource(R.string.interface_in_use).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.interface_in_use),
                        selectedValue = uiType,
                        onValueSelected = {
                            val new = appSettings.copy(uiType = it)
                            appSettingsViewModel.updateSettings(new)

                            if (uiType == UiType.ViMusic) {
                                val new = appearanceSettings.copy(
                                    showTopActionsBar = false,
                                    visualizerEnabled = false,
                                    showThumbnail = true,
                                    playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient,
                                    playerTimelineType = PlayerTimelineType.Default,
                                    playerThumbnailSize = PlayerThumbnailSize.Medium,
                                    thumbnailTapEnabled = true,
                                    playerType = PlayerType.Modern,
                                    queueType = QueueType.Modern,
                                    fadingedge = false,
                                    carousel = true,
                                    carouselSize = CarouselSize.Medium,
                                    thumbnailType = ThumbnailType.Essential,
                                    playerTimelineSize = PlayerTimelineSize.Medium,
                                    playerInfoShowIcons = true,
                                    miniPlayerType = MiniPlayerType.Modern,
                                    playerSwapControlsWithTimeline = false,
                                    transparentBackgroundActionBarPlayer = false,
                                    playerControlsType = PlayerControlsType.Essential,
                                    playerPlayButtonType = PlayerPlayButtonType.Disabled,
                                    buttonzoomout = true,
                                    iconLikeType = IconLikeType.Essential,
                                    blackgradient = true,
                                    showTotalTimeQueue = false,
                                    showRemainingSongTime = false,
                                    showNextSongsInPlayer = false,
                                    disableScrollingText = false,
                                    clickLyricsText = true,
                                    playerEnableLyricsPopupMessage = true,
                                    backgroundProgress = BackgroundProgress.MiniPlayer,
                                    actionsSpacedEvenly = false,
                                    tapqueue = false,
                                    swipeUpQueue = true,
                                    showButtonPlayerDiscover = false,
                                    showButtonPlayerAddToPlaylist = false,
                                    showButtonPlayerLoop = false,
                                    showButtonPlayerShuffle = false,
                                    showButtonPlayerLyrics = false,
                                    showButtonPlayerSleepTimer = false,
                                    showButtonPlayerSystemEqualizer = false,
                                    showButtonPlayerArrow = false,
                                    showButtonPlayerMenu = true,
                                    keepPlayerMinimized = false,
                                    disablePlayerHorizontalSwipe = true
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                                val newsettings = appSettings.copy(
                                    disablePlayerHorizontalSwipe = true,
                                    disableIconButtonOnTop = true,
                                    showSearchTab = true,
                                    showStatsInNavbar = true,
                                    navigationBarPosition = NavigationBarPosition.Left
                                )
                                appSettingsViewModel.updateSettings(newsettings)

                                //showTopActionsBar = false
//                                playerType = PlayerType.Modern
//                                queueType = QueueType.Modern
//                                fadingedge = false
//                                carousel = true
//                                carouselSize = CarouselSize.Medium
//                                thumbnailType = ThumbnailType.Essential
//                                playerTimelineSize = PlayerTimelineSize.Medium
//                                playerInfoShowIcons = true
//                                miniPlayerType = MiniPlayerType.Modern
//                                playerSwapControlsWithTimeline = false
//                                transparentBackgroundActionBarPlayer = false
//                                playerControlsType = PlayerControlsType.Essential
//                                playerPlayButtonType = PlayerPlayButtonType.Disabled
//                                buttonzoomout = true
//                                iconLikeType = IconLikeType.Essential
                                //playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
//                                blackgradient = true
//                                showTotalTimeQueue = false
//                                showRemainingSongTime = false
//                                showNextSongsInPlayer = false
//                                disableScrollingText = false
//                                clickLyricsText = true
//                                playerEnableLyricsPopupMessage = true
//                                backgroundProgress = BackgroundProgress.MiniPlayer
//                                transparentBackgroundActionBarPlayer = true
//                                actionspacedevenly = false
//                                tapqueue = false
//                                swipeUpQueue = true
//                                showButtonPlayerDiscover = false
//                                showButtonPlayerAddToPlaylist = false
//                                showButtonPlayerLoop = false
//                                showButtonPlayerShuffle = false
//                                showButtonPlayerLyrics = false
//                                expandedplayertoggle = false
//                                showButtonPlayerSleepTimer = false
//                                showButtonPlayerSystemEqualizer = false
//                                showButtonPlayerArrow = false
//                                showButtonPlayerShuffle = false
//                                showButtonPlayerMenu = true
                                //showthumbnail = true
                                //keepPlayerMinimized = false
                            } else {
                                val new = appearanceSettings.copy(
                                    playerTimelineType = lastPlayerTimelineType,
                                    playerThumbnailSize = lastPlayerThumbnailSize,
                                    playerPlayButtonType = lastPlayerPlayButtonType,
                                )
                                appearanceSettingsVieModel.updatePreset(new)

                                val newsettings = appSettings.copy(
                                    disablePlayerHorizontalSwipe = false,
                                    disableIconButtonOnTop = false,
                                )
                                appSettingsViewModel.updateSettings(newsettings)

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
                            val mode = appearanceSettings.colorPaletteMode
                            val new = appearanceSettings.copy(
                                colorPaletteName = it,
                                colorPaletteMode = when (it) {
                                        ColorPaletteName.PureBlack,
                                        ColorPaletteName.ModernBlack ->
                                                    ColorPaletteMode.System
                                        else -> mode
                                    }
                                )
                            appearanceSettingsVieModel.updatePreset(new)
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
                                val new = appearanceSettings.copy(customColor = it.hashCode())
                                appearanceSettingsVieModel.updatePreset(new)
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
                                val new = appearanceSettings.copy(
                                    customThemeLight_Background0 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_2),
                            text = "",
                            color = Color(customThemeLight_Background1),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_Background1 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_3),
                            text = "",
                            color = Color(customThemeLight_Background2),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_Background2 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_4),
                            text = "",
                            color = Color(customThemeLight_Background3),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_Background3 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_5),
                            text = "",
                            color = Color(customThemeLight_Background4),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_Background4 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text),
                            text = "",
                            color = Color(customThemeLight_Text),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_Text = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_secondary),
                            text = "",
                            color = Color(customThemeLight_TextSecondary),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_TextSecondary = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_disabled),
                            text = "",
                            color = Color(customThemeLight_TextDisabled),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_TextDisabled = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_icon_button_player),
                            text = "",
                            color = Color(customThemeLight_IconButtonPlayer),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_IconButtonPlayer = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_accent),
                            text = "",
                            color = Color(customThemeLight_Accent),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeLight_Accent = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
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
                                val new = appearanceSettings.copy(
                                    customThemeDark_Background0 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_2),
                            text = "",
                            color = Color(customThemeDark_Background1),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_Background1 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_3),
                            text = "",
                            color = Color(customThemeDark_Background2),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_Background2 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_4),
                            text = "",
                            color = Color(customThemeDark_Background3),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_Background3 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_5),
                            text = "",
                            color = Color(customThemeDark_Background4),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_Background4 = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text),
                            text = "",
                            color = Color(customThemeDark_Text),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_Text = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_secondary),
                            text = "",
                            color = Color(customThemeDark_TextSecondary),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_TextSecondary = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_disabled),
                            text = "",
                            color = Color(customThemeDark_TextDisabled),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_TextDisabled = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_icon_button_player),
                            text = "",
                            color = Color(customThemeDark_IconButtonPlayer),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_IconButtonPlayer = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_accent),
                            text = "",
                            color = Color(customThemeDark_Accent),
                            onColorSelected = {
                                val new = appearanceSettings.copy(
                                    customThemeDark_Accent = it.hashCode()
                                )
                                appearanceSettingsVieModel.updatePreset(new)
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
                            val new = appearanceSettings.copy(colorPaletteMode = it)
                            appearanceSettingsVieModel.updatePreset(new)
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
                        onValueSelected = {
                            val new = appearanceSettings.copy(navigationBarPosition = it)
                            appearanceSettingsVieModel.updatePreset(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(navigationBarType = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(playerPosition = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(menuStyle = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(messageType = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(indexNavigationTab = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(transitionEffect = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onCheckedChange = {
                            val new = appSettings.copy(isSnowEffectEnabled = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                            onCheckedChange = {
                                val new = appSettings.copy(showSearchTab = it)
                                appSettingsViewModel.updateSettings(new)
                            }
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
                            onCheckedChange = {
                                val new = appSettings.copy(showStatsInNavbar = it)
                                appSettingsViewModel.updateSettings(new)
                            }
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
                            onCheckedChange = {
                                val new = appSettings.copy(showFloatingIcon = it)
                                appSettingsViewModel.updateSettings(new)
                            }
                        )
                } else {
                    val new = appSettings.copy(
                        showFloatingIcon = false
                    )
                    appSettingsViewModel.updateSettings(new)
                }



                if (search.input.isBlank() || stringResource(R.string.settings_use_font_type).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_use_font_type),
                        selectedValue = fontType,
                        onValueSelected = {
                            val new = appSettings.copy(fontType = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onCheckedChange = {
                            val new = appSettings.copy(useSystemFont = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(applyFontPadding = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(isSwipeToActionEnabled = it)
                            appSettingsViewModel.updateSettings(new)
                        }
                    )

                    AnimatedVisibility(visible = isSwipeToActionEnabled) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            EnumValueSelectorSettingsEntry<QueueSwipeAction>(
                                title = stringResource(R.string.queue_and_local_playlists_left_swipe),
                                selectedValue = queueSwipeLeftAction,
                                onValueSelected = {
                                    val new = appSettings.copy(
                                        queueSwipeLeftAction = it
                                    )
                                    appSettingsViewModel.updateSettings(new)
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<QueueSwipeAction>(
                                title = stringResource(R.string.queue_and_local_playlists_right_swipe),
                                selectedValue = queueSwipeRightAction,
                                onValueSelected = {
                                    val new = appSettings.copy(
                                        queueSwipeRightAction = it
                                    )
                                    appSettingsViewModel.updateSettings(new)
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<PlaylistSwipeAction>(
                                title = stringResource(R.string.playlist_left_swipe),
                                selectedValue = playlistSwipeLeftAction,
                                onValueSelected = {
                                    val new = appSettings.copy(
                                        playlistSwipeLeftAction = it
                                    )
                                    appSettingsViewModel.updateSettings(new)
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<PlaylistSwipeAction>(
                                title = stringResource(R.string.playlist_right_swipe),
                                selectedValue = playlistSwipeRightAction,
                                onValueSelected = {
                                    val new = appSettings.copy(
                                        playlistSwipeRightAction = it
                                    )
                                    appSettingsViewModel.updateSettings(new)
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<AlbumSwipeAction>(
                                title = stringResource(R.string.album_left_swipe),
                                selectedValue = albumSwipeLeftAction,
                                onValueSelected = {
                                    val new = appSettings.copy(
                                        albumSwipeLeftAction = it
                                    )
                                    appSettingsViewModel.updateSettings(new)
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<AlbumSwipeAction>(
                                title = stringResource(R.string.album_right_swipe),
                                selectedValue = albumSwipeRightAction,
                                onValueSelected = {
                                    val new = appSettings.copy(
                                        albumSwipeRightAction = it
                                    )
                                    appSettingsViewModel.updateSettings(new)
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
                        isChecked = usePlaceholderInImageLoader,
                        onCheckedChange = {
                            val new = appSettings.copy(usePlaceholderInImageLoader = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(showFavoritesPlaylist = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(showMyTopPlaylist = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(showOnDevicePlaylist = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(showDislikedPlaylist = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(showPinnedPlaylists = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                        onCheckedChange = {
                            val new = appSettings.copy(showMonthlyPlaylists = it)
                            appSettingsViewModel.updateSettings(new)
                        }
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
                            val new = appSettings.copy(enableCreateMonthlyPlaylists = it)
                            appSettingsViewModel.updateSettings(new)
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
                        onValueSelected = {
                            val new = appSettings.copy(recommendationsNumber = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        onValueSelected = {
                            val new = appSettings.copy(maxStatisticsItems = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                            val new = appSettings.copy(
                                showStatsListeningTime = it
                            )
                            appSettingsViewModel.updateSettings(new)
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
                        onValueSelected = {
                            val new = appSettings.copy(maxTopPlaylistItems = it)
                            appSettingsViewModel.updateSettings(new)
                        },
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
                        val new = appSettings.copy(
                            showListenerLevels = it
                        )
                        appSettingsViewModel.updateSettings(new)
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
