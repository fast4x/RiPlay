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
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NavigationBarType
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
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.experimental.appsettings.models.AppSettings
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.DefaultDarkColorPalette
import it.fast4x.riplay.ui.styling.DefaultLightColorPalette
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.restartApp
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun UiSettings(
    navController: NavController
) {

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
    val indexNavigationTab = appSettings.indexNavigationTab
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
    val search = Search.init()
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
                            coroutineScope.launch {
                                val new = appSettings.copy(isEnabledFullScreen = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }
                    )

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
                            coroutineScope.launch {
                                val new = appSettings.copy(uiType = it)
                                appSettingsManager.updateSettings(new)
                            }

                            if (uiType == UiType.ViMusic) {
                                coroutineScope.launch {
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
                                    appearanceSettingsManager.updatePreset(new)
                                    val newsettings = appSettings.copy(
                                        disablePlayerHorizontalSwipe = true,
                                        disableIconButtonOnTop = true,
                                        showSearchTab = true,
                                        showStatsInNavbar = true,
                                        navigationBarPosition = NavigationBarPosition.Left
                                    )
                                    appSettingsManager.updateSettings(newsettings)
                                }

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
                                coroutineScope.launch {
                                    val new = appearanceSettings.copy(
                                        playerTimelineType = lastPlayerTimelineType,
                                        playerThumbnailSize = lastPlayerThumbnailSize,
                                        playerPlayButtonType = lastPlayerPlayButtonType,
                                    )
                                    appearanceSettingsManager.updatePreset(new)

                                    val newsettings = appSettings.copy(
                                        disablePlayerHorizontalSwipe = false,
                                        disableIconButtonOnTop = false,
                                    )
                                    appSettingsManager.updateSettings(newsettings)
                                }

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
                            coroutineScope.launch {
                                val new = appearanceSettings.copy(
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
                                coroutineScope.launch {
                                    val new = appearanceSettings.copy(customColor = it.hashCode())
                                    appearanceSettingsManager.updatePreset(new)
                                }
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
                                coroutineScope.launch {
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
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
                                    val new = appearanceSettings.copy(
                                        customThemeDark_Accent = it.hashCode()
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
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
                            coroutineScope.launch {
                                val new = appearanceSettings.copy(colorPaletteMode = it)
                                appearanceSettingsManager.updatePreset(new)
                            }
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
                            coroutineScope.launch {
                                val new = appearanceSettings.copy(navigationBarPosition = it)
                                appearanceSettingsManager.updatePreset(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(navigationBarType = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(playerPosition = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(menuStyle = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(messageType = it)
                                appSettingsManager.updateSettings(new)
                            }
                        },
                        valueText = {
                            when (it) {
                                MessageType.Modern -> stringResource(R.string.message_type_modern)
                                MessageType.Essential -> stringResource(R.string.message_type_essential)
                            }
                        }
                    )

                /*
                if (search.input.isBlank() || stringResource(R.string.default_page).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.default_page),
                        selectedValue = indexNavigationTab,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettings.copy(indexNavigationTab = it)
                                appSettingsManager.updateSettings(new)
                            }
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

                 */

                if (search.input.isBlank() || stringResource(R.string.transition_effect).contains(
                        search.input,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.transition_effect),
                        selectedValue = transitionEffect,
                        onValueSelected = {
                            coroutineScope.launch {
                                val new = appSettings.copy(transitionEffect = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(isSnowEffectEnabled = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                                coroutineScope.launch {
                                    val new = appSettings.copy(showSearchTab = it)
                                    appSettingsManager.updateSettings(new)
                                }
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
                                coroutineScope.launch {
                                    val new = appSettings.copy(showStatsInNavbar = it)
                                    appSettingsManager.updateSettings(new)
                                }
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
                                coroutineScope.launch {
                                    val new = appSettings.copy(showFloatingIcon = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )
                } else {
                    LaunchedEffect(Unit) {
                        val new = appSettings.copy(
                            showFloatingIcon = false
                        )
                        appSettingsManager.updateSettings(new)
                    }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(fontType = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(useSystemFont = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(applyFontPadding = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(isSwipeToActionEnabled = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                                    coroutineScope.launch {
                                        val new = appSettings.copy(
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
                                        val new = appSettings.copy(
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
                                        val new = appSettings.copy(
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
                                        val new = appSettings.copy(
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
                                        val new = appSettings.copy(
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
                                        val new = appSettings.copy(
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
                            coroutineScope.launch {
                                val new = appSettings.copy(usePlaceholderInImageLoader = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(showFavoritesPlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(showMyTopPlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(showOnDevicePlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(showDislikedPlaylist = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(showPinnedPlaylists = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(showMonthlyPlaylists = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(enableCreateMonthlyPlaylists = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(recommendationsNumber = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(maxStatisticsItems = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(
                                    showStatsListeningTime = it
                                )
                                appSettingsManager.updateSettings(new)
                            }
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
                            coroutineScope.launch {
                                val new = appSettings.copy(maxTopPlaylistItems = it)
                                appSettingsManager.updateSettings(new)
                            }
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
                        coroutineScope.launch {
                            val new = appSettings.copy(
                                showListenerLevels = it
                            )
                            appSettingsManager.updateSettings(new)
                        }
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
