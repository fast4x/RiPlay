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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.AlbumSwipeAction
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.CarouselSize
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerTimelineSize
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.PlaylistSwipeAction
import it.fast4x.riplay.enums.QueueSwipeAction
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.appsettings.models.AppSettings
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.DefaultDarkColorPalette
import it.fast4x.riplay.ui.styling.DefaultLightColorPalette
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.restartApp
import kotlinx.coroutines.launch
import it.fast4x.riplay.ui.components.themed.settingsHeaderItem
import it.fast4x.riplay.ui.components.themed.settingsGroupHeader
import it.fast4x.riplay.ui.components.themed.settingsGroupCard
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun UiSettings(
    navController: NavController
) {

    val context = LocalContext.current
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val recommendationsNumber = appSettings.recommendationsNumber

    val lastPlayerTimelineType = appearanceSettings.lastPlayerTimelineType
    val lastPlayerThumbnailSize = appearanceSettings.lastPlayerThumbnailSize
    val lastPlayerPlayButtonType = appearanceSettings.lastPlayerPlayButtonType
    val colorPaletteName = appearanceSettings.colorPaletteName
    val colorPaletteMode = appearanceSettings.colorPaletteMode
    val fontType = appSettings.fontType
    val useSystemFont = appSettings.useSystemFont
    val applyFontPadding = appSettings.applyFontPadding
    val isSwipeToActionEnabled = appSettings.isSwipeToActionEnabled
    val showSearchTab = appSettings.showSearchTab
    val showStatsInNavbar = appSettings.showStatsInNavbar
    val maxStatisticsItems = appSettings.maxStatisticsItems
    val showStatsListeningTime = appSettings.showStatsListeningTime
    val maxTopPlaylistItems = appSettings.maxTopPlaylistItems
    val navigationBarPosition = appSettings.navigationBarPosition
    val navigationBarType = appSettings.navigationBarType
    val search = Search.init(alwaysVisible = true)
    val showFavoritesPlaylist = appSettings.showFavoritesPlaylist
    val showMyTopPlaylist = appSettings.showMyTopPlaylist
    val showOnDevicePlaylist = appSettings.showOnDevicePlaylist
    val showDislikedPlaylist = appSettings.showDislikedPlaylist
    val showFloatingIcon = appSettings.showFloatingIcon
    val menuStyle = appSettings.menuStyle
    val transitionEffect = appSettings.transitionEffect
    val enableCreateMonthlyPlaylists = appSettings.enableCreateMonthlyPlaylists
    val showPinnedPlaylists = appSettings.showPinnedPlaylists
    val showMonthlyPlaylists = appSettings.showMonthlyPlaylists
    val customThemeLight_Background0 = appearanceSettings.customThemeLight_Background0
    val customThemeLight_Background1 = appearanceSettings.customThemeLight_Background1
    val customThemeLight_Background2 = appearanceSettings.customThemeLight_Background2
    val customThemeLight_Background3 = appearanceSettings.customThemeLight_Background3
    val customThemeLight_Background4 = appearanceSettings.customThemeLight_Background4
    val customThemeLight_Text = appearanceSettings.customThemeLight_Text
    val customThemeLight_TextSecondary = appearanceSettings.customThemeLight_TextSecondary
    val customThemeLight_TextDisabled = appearanceSettings.customThemeLight_TextDisabled
    val customThemeLight_IconButtonPlayer = appearanceSettings.customThemeLight_IconButtonPlayer
    val customThemeLight_Accent = appearanceSettings.customThemeLight_Accent
    val customThemeDark_Background0 = appearanceSettings.customThemeDark_Background0
    val customThemeDark_Background1 = appearanceSettings.customThemeDark_Background1
    val customThemeDark_Background2 = appearanceSettings.customThemeDark_Background2
    val customThemeDark_Background3 = appearanceSettings.customThemeDark_Background3
    val customThemeDark_Background4 = appearanceSettings.customThemeDark_Background4
    val customThemeDark_Text = appearanceSettings.customThemeDark_Text
    val customThemeDark_TextSecondary = appearanceSettings.customThemeDark_TextSecondary
    val customThemeDark_TextDisabled = appearanceSettings.customThemeDark_TextDisabled
    val customThemeDark_IconButtonPlayer = appearanceSettings.customThemeDark_IconButtonPlayer
    val customThemeDark_Accent = appearanceSettings.customThemeDark_Accent
    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }
    val playerPosition = appSettings.playerPosition
    val messageType = appSettings.messageType
    val queueSwipeLeftAction = appSettings.queueSwipeLeftAction
    val queueSwipeRightAction = appSettings.queueSwipeRightAction
    val playlistSwipeLeftAction = appSettings.playlistSwipeLeftAction
    val playlistSwipeRightAction = appSettings.playlistSwipeRightAction
    val albumSwipeLeftAction = appSettings.albumSwipeLeftAction
    val albumSwipeRightAction = appSettings.albumSwipeRightAction
    val customColor = appearanceSettings.customColor
    val usePlaceholderInImageLoader = appSettings.usePlaceholderInImageLoader
    val isEnabledFullscreen = appSettings.isEnabledFullScreen
    val isSnowEffectEnabled = appSettings.isSnowEffectEnabled
    val showListenerLevels = appSettings.showListenerLevels
    val showMiniPlayerInSettings = appSettings.showMiniPlayerInSettings
    val showUpNextSongInMiniPlayer = appSettings.showUpNextSongInMiniPlayer
    val loaderStyle = appSettings.loaderStyle
    val swipeToNext = appSettings.swipeToNext
    val uiType = appSettings.uiType
    val coroutineScope = rememberCoroutineScope()

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
                    coroutineScope.launch {
                        val new = appearanceSettingsManager.activeSettings.value.copy(
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
                        appearanceSettingsManager.updatePreset(new)
                    }
                }
            )
        }

        if (resetCustomDarkThemeDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_dark_theme_colors),
                onDismiss = { resetCustomDarkThemeDialog = false },
                onConfirm = {
                    resetCustomDarkThemeDialog = false
                    coroutineScope.launch {
                        val new = appearanceSettingsManager.activeSettings.value.copy(
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
                        appearanceSettingsManager.updatePreset(new)
                    }
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
            settingsHeaderItem {
                HeaderWithIcon(
                    title = stringResource(R.string.user_interface),
                    iconId = R.drawable.ui,
                    enabled = false,
                    showIcon = true,
                    modifier = Modifier,
                    onClick = {},
                    navController = navController
                )
            }


            settingsSearchBarItem {
                search.SearchBar(this)
            }


            if (search.matchesAny(R.string.enable_fullscreen, R.string.interface_in_use)) {
            settingsGroupHeader(context.resources.getString(R.string.user_interface))
            settingsGroupCard {


                if (search.matchesAny(R.string.enable_fullscreen))
                    SwitchSettingEntry(
                        title = stringResource(R.string.enable_fullscreen),
                        text = stringResource(R.string.enable_fullscreen_info),
                        isChecked = isEnabledFullscreen,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(isEnabledFullScreen = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.resize,
                    )

                val uiType = appSettings.uiType
                if (search.matchesAny(R.string.interface_in_use))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.interface_in_use))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.interface_in_use),
                        selectedValue = uiType,
                        onValueSelected = {

                            if (it == UiType.ViMusic) {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        showTopActionsBar = false,
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
                                    appearanceSettingsManager.updatePreset(new)
                                    val newsettings = appSettingsManager.activeSettings.value.copy(
                                        disablePlayerHorizontalSwipe = true,
                                        disableIconButtonOnTop = true,
                                        showSearchTab = true,
                                        showStatsInNavbar = true,
                                        navigationBarPosition = NavigationBarPosition.Left,
                                        playerPosition = PlayerPosition.Bottom,
                                        uiType = it
                                    )
                                    appSettingsManager.updateSettings(newsettings)
                                }

                            } else {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerTimelineType = lastPlayerTimelineType,
                                        playerThumbnailSize = lastPlayerThumbnailSize,
                                        playerPlayButtonType = lastPlayerPlayButtonType,
                                    )
                                    appearanceSettingsManager.updatePreset(new)

                                    val newsettings = appSettingsManager.activeSettings.value.copy(
                                        disablePlayerHorizontalSwipe = false,
                                        disableIconButtonOnTop = false,
                                        navigationBarPosition = NavigationBarPosition.Bottom,
                                        uiType = it
                                    )
                                    appSettingsManager.updateSettings(newsettings)
                                }

                            }

                            navController.navigate(NavRoutes.home.name)

                        },
                        valueText = {
                            it.name
                        },
                        icon = R.drawable.ui
                    )
                }
                }

                if (search.matchesAny(R.string.theme, R.string.customcolor, R.string.theme_mode, R.string.title_customized_light_theme_colors, R.string.title_customized_dark_theme_colors)) {
                settingsGroupHeader(context.resources.getString(R.string.theme))
                settingsGroupCard {

                if (search.matchesAny(R.string.theme))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.theme),
                        selectedValue = colorPaletteName,
                        onValueSelected = {
                            val mode = appearanceSettings.colorPaletteMode
                            coroutineScope.launch {
                                val new = appearanceSettingsManager.activeSettings.value.copy(
                                    colorPaletteName = it,
                                    colorPaletteMode = when (it) {
                                        ColorPaletteName.PureBlack,
                                        ColorPaletteName.ModernBlack ->
                                            ColorPaletteMode.System

                                        else -> mode
                                    }
                                )
                                appearanceSettingsManager.updatePreset(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.color_palette,
                    )

                AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.CustomColor) {
                    Column {
                        ColorSettingEntry(
                            title = stringResource(R.string.customcolor),
                            text = "",
                            color = Color(customColor),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(customColor = it.hashCode())
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            modifier = Modifier
                                .padding(start = 12.dp), icon = R.drawable.color_palette,
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
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Background0 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_2),
                            text = "",
                            color = Color(customThemeLight_Background1),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Background1 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_3),
                            text = "",
                            color = Color(customThemeLight_Background2),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Background2 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_4),
                            text = "",
                            color = Color(customThemeLight_Background3),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Background3 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_5),
                            text = "",
                            color = Color(customThemeLight_Background4),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Background4 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text),
                            text = "",
                            color = Color(customThemeLight_Text),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Text = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_secondary),
                            text = "",
                            color = Color(customThemeLight_TextSecondary),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_TextSecondary = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_disabled),
                            text = "",
                            color = Color(customThemeLight_TextDisabled),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_TextDisabled = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_icon_button_player),
                            text = "",
                            color = Color(customThemeLight_IconButtonPlayer),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_IconButtonPlayer = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_accent),
                            text = "",
                            color = Color(customThemeLight_Accent),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeLight_Accent = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
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
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Background0 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_2),
                            text = "",
                            color = Color(customThemeDark_Background1),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Background1 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_3),
                            text = "",
                            color = Color(customThemeDark_Background2),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Background2 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_4),
                            text = "",
                            color = Color(customThemeDark_Background3),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Background3 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_background_5),
                            text = "",
                            color = Color(customThemeDark_Background4),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Background4 = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text),
                            text = "",
                            color = Color(customThemeDark_Text),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Text = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_secondary),
                            text = "",
                            color = Color(customThemeDark_TextSecondary),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_TextSecondary = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_text_disabled),
                            text = "",
                            color = Color(customThemeDark_TextDisabled),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_TextDisabled = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_icon_button_player),
                            text = "",
                            color = Color(customThemeDark_IconButtonPlayer),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_IconButtonPlayer = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                        ColorSettingEntry(
                            title = stringResource(R.string.color_accent),
                            text = "",
                            color = Color(customThemeDark_Accent),
                            onColorSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        customThemeDark_Accent = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                    }
                }

                if (search.matchesAny(R.string.theme_mode))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.theme_mode))
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
                            coroutineScope.launch {
                                val new = appearanceSettingsManager.activeSettings.value.copy(colorPaletteMode = it)
                                appearanceSettingsManager.updatePreset(new)
                            }
                            //if (it == ColorPaletteMode.PitchBlack) colorPaletteName = ColorPaletteName.ModernBlack
                        },
                        valueText = { it.displayName },
                        icon = R.drawable.moon
                    )
                }
                }

                if (search.matchesAny(R.string.navigation_bar_position, R.string.navigation_bar_type, R.string.player_position, R.string.swipe_to_next_with_miniplayer_title, R.string.show_upnext_in_miniplayer, R.string.show_miniplayer_in_settings)) {
                settingsGroupHeader(context.resources.getString(R.string.settings_group_navigation))
                settingsGroupCard {

                if (search.matchesAny(R.string.navigation_bar_position))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.navigation_bar_position),
                        selectedValue = navigationBarPosition,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettings.copy(navigationBarPosition = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        // As of version 0.6.53, changing navigation bar to top or bottom
                        // while using ViMusic theme breaks the UI
                        //isEnabled = uiType != UiType.ViMusic,
                        valueText = { it.displayName }, icon = R.drawable.position,
                    )

                if (search.matchesAny(R.string.navigation_bar_type))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.navigation_bar_type))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.navigation_bar_type),
                        selectedValue = navigationBarType,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(navigationBarType = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.singlepage,
                    )

                if (UiType.RiPlay.isCurrent()) {
                    if (search.matchesAny(R.string.player_position))
                        SettingsGroupDivider()

                    if (search.matchesAny(R.string.player_position))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.player_position),
                            selectedValue = playerPosition,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new =
                                        appSettingsManager.activeSettings.value.copy(playerPosition = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.arrow_up,
                        )
                }

                if (search.matchesAny(R.string.swipe_to_next_with_miniplayer_title))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.swipe_to_next_with_miniplayer_title))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.swipe_to_next_with_miniplayer_title),
                        selectedValue = swipeToNext,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(swipeToNext = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.left_and_right_arrows,
                    )

                if (search.matchesAny(R.string.show_upnext_in_miniplayer))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.show_upnext_in_miniplayer))
                    SwitchSettingEntry(
                        title = stringResource(R.string.show_upnext_in_miniplayer),
                        text = "",
                        isChecked = showUpNextSongInMiniPlayer,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showUpNextSongInMiniPlayer = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.addqueue,
                    )

                if (search.matchesAny(R.string.show_miniplayer_in_settings))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.show_miniplayer_in_settings))
                    SwitchSettingEntry(
                        title = stringResource(R.string.show_miniplayer_in_settings),
                        text = "",
                        isChecked = showMiniPlayerInSettings,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showMiniPlayerInSettings = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        icon = R.drawable.player_mini_widget
                    )
                }
                }

                if (search.matchesAny(R.string.loader_style, R.string.menu_style, R.string.message_type, R.string.transition_effect, R.string.snow_effect)) {
                settingsGroupHeader(context.resources.getString(R.string.settings_group_effects))
                settingsGroupCard {

                if (search.matchesAny(R.string.loader_style))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.loader_style),
                        selectedValue = loaderStyle,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(loaderStyle = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.refresh,
                    )

                if (search.matchesAny(R.string.menu_style))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.menu_style))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.menu_style),
                        selectedValue = menuStyle,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(menuStyle = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.burger,
                    )

                if (search.matchesAny(R.string.message_type))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.message_type))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.message_type),
                        selectedValue = messageType,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(messageType = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.information_circle,
                    )

                if (search.matchesAny(R.string.transition_effect))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.transition_effect))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.transition_effect),
                        selectedValue = transitionEffect,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(transitionEffect = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.slow_motion,
                    )

                if (search.matchesAny(R.string.snow_effect))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.snow_effect))
                    SwitchSettingEntry(
                        title = stringResource(R.string.snow_effect),
                        text = "",
                        isChecked = isSnowEffectEnabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(isSnowEffectEnabled = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        icon = R.drawable.droplet
                    )

                if (search.matchesAny(R.string.use_placeholder_in_imageloader))
                    SwitchSettingEntry(
                        title = stringResource(R.string.use_placeholder_in_imageloader),
                        text = stringResource(R.string.use_placeholder_in_imageloader_info),
                        isChecked = usePlaceholderInImageLoader,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(usePlaceholderInImageLoader = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        icon = R.drawable.picture
                    )
                }
                }

                if (search.matchesAny(R.string.vimusic_show_search_button_in_navigation_bar, R.string.show_statistics_in_navigation_bar, R.string.show_floating_icon)) {
                settingsGroupHeader(context.resources.getString(R.string.settings_group_visibility))
                settingsGroupCard {

                if (UiType.ViMusic.isCurrent()) {
                    if (search.matchesAny(R.string.vimusic_show_search_button_in_navigation_bar))
                        SwitchSettingEntry(
                            title = stringResource(R.string.vimusic_show_search_button_in_navigation_bar),
                            text = stringResource(R.string.vismusic_only_in_left_right_navigation_bar),
                            isChecked = showSearchTab,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showSearchTab = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.search,
                        )



                    if (search.matchesAny(R.string.show_statistics_in_navigation_bar))
                        SettingsGroupDivider()

                    if (search.matchesAny(R.string.show_statistics_in_navigation_bar))
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_statistics_in_navigation_bar),
                            text = "",
                            isChecked = showStatsInNavbar,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showStatsInNavbar = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.query_stats,
                        )
                }

                if (uiType == UiType.ViMusic) {
                    if (search.matchesAny(R.string.show_floating_icon))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.show_floating_icon))
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_floating_icon),
                            text = "",
                            isChecked = showFloatingIcon,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showFloatingIcon = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.sparkles,
                        )
                } else {
                    LaunchedEffect(Unit) {
                        val new = appSettingsManager.activeSettings.value.copy(
                            showFloatingIcon = false
                        )
                        appSettingsManager.updateSettings(new)
                    }
                }
                }
                }

                if (search.matchesAny(R.string.settings_use_font_type, R.string.use_system_font, R.string.apply_font_padding)) {
                settingsGroupHeader(context.resources.getString(R.string.settings_group_font))
                settingsGroupCard {

                if (search.matchesAny(R.string.settings_use_font_type))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_use_font_type),
                        selectedValue = fontType,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(fontType = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = { it.displayName }, icon = R.drawable.text,
                    )

                if (search.matchesAny(R.string.use_system_font))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.use_system_font))
                    SwitchSettingEntry(
                        title = stringResource(R.string.use_system_font),
                        text = stringResource(R.string.use_font_by_the_system),
                        isChecked = useSystemFont,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(useSystemFont = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.text,
                    )

                if (search.matchesAny(R.string.apply_font_padding))
                    SettingsGroupDivider()

                if (search.matchesAny(R.string.apply_font_padding))
                    SwitchSettingEntry(
                        title = stringResource(R.string.apply_font_padding),
                        text = stringResource(R.string.add_spacing_around_texts),
                        isChecked = applyFontPadding,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(applyFontPadding = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        icon = R.drawable.text
                    )
                }
                }

                if (search.matchesAny(R.string.swipe_to_action, R.string.queue_and_local_playlists_left_swipe, R.string.queue_and_local_playlists_right_swipe, R.string.playlist_left_swipe, R.string.playlist_right_swipe, R.string.album_left_swipe, R.string.album_right_swipe)) {
                settingsGroupHeader(context.resources.getString(R.string.settings_group_gestures))
                settingsGroupCard {

                if (search.matchesAny(R.string.swipe_to_action)) {
                    SwitchSettingEntry(
                        title = stringResource(R.string.swipe_to_action),
                        text = stringResource(R.string.activate_the_action_menu_by_swiping_the_song_left_or_right),
                        isChecked = isSwipeToActionEnabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(isSwipeToActionEnabled = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.flip,
                    )

                    AnimatedVisibility(visible = isSwipeToActionEnabled) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            EnumValueSelectorSettingsEntry<QueueSwipeAction>(
                                title = stringResource(R.string.queue_and_local_playlists_left_swipe),
                                selectedValue = queueSwipeLeftAction,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            queueSwipeLeftAction = it
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<QueueSwipeAction>(
                                title = stringResource(R.string.queue_and_local_playlists_right_swipe),
                                selectedValue = queueSwipeRightAction,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            queueSwipeRightAction = it
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<PlaylistSwipeAction>(
                                title = stringResource(R.string.playlist_left_swipe),
                                selectedValue = playlistSwipeLeftAction,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            playlistSwipeLeftAction = it
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<PlaylistSwipeAction>(
                                title = stringResource(R.string.playlist_right_swipe),
                                selectedValue = playlistSwipeRightAction,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            playlistSwipeRightAction = it
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<AlbumSwipeAction>(
                                title = stringResource(R.string.album_left_swipe),
                                selectedValue = albumSwipeLeftAction,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            albumSwipeLeftAction = it
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                            EnumValueSelectorSettingsEntry<AlbumSwipeAction>(
                                title = stringResource(R.string.album_right_swipe),
                                selectedValue = albumSwipeRightAction,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new = appSettingsManager.activeSettings.value.copy(
                                            albumSwipeRightAction = it
                                        )
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = {
                                    it.displayName
                                },
                            )
                        }
                    }
                }

                }
                }


            if (search.matchesAny(R.string.show, R.string.favorites, R.string.my_playlist_top, R.string.on_device, R.string.disliked)) {
            settingsGroupHeader(title = context.resources.getString(R.string.songs).uppercase())


            settingsGroupCard {

                if (search.matches("${stringResource(R.string.show)} ${stringResource(R.string.favorites)}"))
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.favorites)}",
                        text = "",
                        isChecked = showFavoritesPlaylist,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showFavoritesPlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.heart,
                    )

                if (search.matches("${stringResource(R.string.show)} ${stringResource(R.string.my_playlist_top)}"))
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${
                            stringResource(R.string.my_playlist_top).format(
                                maxTopPlaylistItems
                            )
                        }",
                        text = "",
                        isChecked = showMyTopPlaylist,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showMyTopPlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.trophy,
                    )
                if (search.matches("${stringResource(R.string.show)} ${stringResource(R.string.on_device)}"))
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.on_device)}",
                        text = "",
                        isChecked = showOnDevicePlaylist,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showOnDevicePlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.folder,
                    )

                if (search.matches("${stringResource(R.string.show)} ${stringResource(R.string.disliked)}"))
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.disliked)}",
                        text = "",
                        isChecked = showDislikedPlaylist,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showDislikedPlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.heart_dislike,
                    )

            
            }
            }


            if (search.matchesAny(R.string.show, R.string.pinned_playlists, R.string.monthly_playlists)) {
            settingsGroupHeader(title = context.resources.getString(R.string.playlists).uppercase())


            settingsGroupCard {


                if (search.matches("${stringResource(R.string.show)} ${stringResource(R.string.pinned_playlists)}"))
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.pinned_playlists)}",
                        text = "",
                        isChecked = showPinnedPlaylists,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showPinnedPlaylists = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.pin,
                    )

                if (search.matches("${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}"))
                    SwitchSettingEntry(
                        title = "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}",
                        text = "",
                        isChecked = showMonthlyPlaylists,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(showMonthlyPlaylists = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.calendar,
                    )
            
            }
            }


            if (search.matchesAny(R.string.monthly_playlists, R.string.enable_monthly_playlists_creation)) {
            settingsGroupHeader(context.resources.getString(R.string.monthly_playlists).uppercase())


            settingsGroupCard {

                if (search.matchesAny(R.string.monthly_playlists))
                    SwitchSettingEntry(
                        title = stringResource(R.string.enable_monthly_playlists_creation),
                        text = "",
                        isChecked = enableCreateMonthlyPlaylists,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(enableCreateMonthlyPlaylists = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.calendar,
                    )
            
            }
            }


            if (search.matchesAny(R.string.statistics_max_number_of_items)) {
            settingsGroupHeader(context.resources.getString(R.string.smart_recommendations))


            settingsGroupCard {

                if (search.matchesAny(R.string.statistics_max_number_of_items))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        selectedValue = recommendationsNumber,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(recommendationsNumber = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = {
                            it.number.toString()
                        }, icon = R.drawable.query_stats,
                    )
            
            }
            }


            if (search.matchesAny(R.string.statistics_max_number_of_items, R.string.listening_time, R.string.shows_the_number_of_songs_heard_and_their_listening_time)) {
            settingsGroupHeader(context.resources.getString(R.string.statistics))


            settingsGroupCard {

                if (search.matchesAny(R.string.statistics_max_number_of_items))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        selectedValue = maxStatisticsItems,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(maxStatisticsItems = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = {
                            it.number.toString()
                        }, icon = R.drawable.query_stats,
                    )

                if (search.matchesAny(R.string.listening_time))
                    SwitchSettingEntry(
                        title = stringResource(R.string.listening_time),
                        text = stringResource(R.string.shows_the_number_of_songs_heard_and_their_listening_time),
                        isChecked = showStatsListeningTime,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(
                                    showStatsListeningTime = it
                                )
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.time,
                    )
            
            }
            }


            if (search.matchesAny(R.string.statistics_max_number_of_items)) {
            settingsGroupHeader(context.resources.getString(R.string.playlist_top))


            settingsGroupCard {

                if (search.matchesAny(R.string.statistics_max_number_of_items))
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.statistics_max_number_of_items),
                        selectedValue = maxTopPlaylistItems,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(maxTopPlaylistItems = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = {
                            it.number.toString()
                        }, icon = R.drawable.query_stats,
                    )


            
            }
            }


            if (search.matchesAny(R.string.show, R.string.listener_levels, R.string.disable_if_you_do_not_want_to_see)) {
            settingsGroupHeader(context.resources.getString(R.string.listener_levels))


            settingsGroupCard {

                SwitchSettingEntry(
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.listener_levels)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.listener_levels
                    ),
                    isChecked = showListenerLevels,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val new = appSettingsManager.activeSettings.value.copy(
                                showListenerLevels = it
                            )
                            appSettingsManager.updateSettings(new)
                        }
                    }, icon = R.drawable.soundwave,
                )
            
            }
            }


            if (search.matchesAny(R.string.settings_reset, R.string.settings_restore_default_settings, R.string.done)) {
            settingsGroupHeader(context.resources.getString(R.string.settings_reset))


            settingsGroupCard {

                var resetToDefault by remember { mutableStateOf(false) }
                val context = LocalContext.current
                ButtonBarSettingEntry(
                    title = stringResource(R.string.settings_reset),
                    text = stringResource(R.string.settings_restore_default_settings),
                    icon = R.drawable.refresh,
                    iconColor = colorPalette().text,
                    onClick = { resetToDefault = true },
                )
                LaunchedEffect(resetToDefault) {
                    if (resetToDefault) {
                        appSettingsManager.updateSettings(AppSettings())

                        resetToDefault = false
                        //navController.popBackStack()
                        restartApp(context)
                        SmartMessage(context.resources.getString(R.string.done), context = context)
                    }
                }

            

        }
            }
    }
        }
}
