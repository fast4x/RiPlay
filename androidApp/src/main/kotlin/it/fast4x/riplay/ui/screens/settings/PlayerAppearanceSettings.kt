package it.fast4x.riplay.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.RestartPlayerService
import it.fast4x.riplay.utils.isAtLeastAndroid7
import it.fast4x.riplay.utils.isLandscape
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.AudioVisualizerStyle
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.appearancesettings.AppearancePreferences
import it.fast4x.riplay.extensions.appearancesettings.ui.AppearancePresetDialogHost
import it.fast4x.riplay.extensions.appearancesettings.models.AppearanceSettings
import it.fast4x.riplay.extensions.appearancesettings.utils.fromShareString
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.utils.RestartActivity
import it.fast4x.riplay.ui.components.CustomModalBottomSheet
import it.fast4x.riplay.utils.LazyListContainer
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import it.fast4x.riplay.ui.components.themed.settingsHeaderItem
import it.fast4x.riplay.ui.components.themed.settingsGroupHeader
import it.fast4x.riplay.ui.components.themed.settingsGroupCard
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun PlayerAppearanceSettings(
    navController: NavController,
) {

    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val showthumbnail = appearanceSettings.showThumbnail

    val transparentbar = appearanceSettings.transparentBar
    val blackgradient = appearanceSettings.blackgradient
    val showlyricsthumbnail = appearanceSettings.showLyricsThumbnail
    //val expandedplayer = appearanceSettings.expandedPlayer

    val playerPlayButtonType = appearanceSettings.playerPlayButtonType
    val bottomgradient = appearanceSettings.bottomGradient
    val textoutline = appearanceSettings.textoutline

    val disablePlayerHorizontalSwipe = appearanceSettings.disablePlayerHorizontalSwipe

    val disableScrollingText = appearanceSettings.disableScrollingText

    val showLikeButtonBackgroundPlayer = appearanceSettings.showLikeButtonBackgroundPlayer

    val playerTimelineType = appearanceSettings.playerTimelineType

    val playerThumbnailSize = appearanceSettings.playerThumbnailSize

    val playerThumbnailSizeL = appearanceSettings.playerThumbnailSizeL

    val playerTimelineSize = appearanceSettings.playerTimelineSize

    val seekWithTap = appearanceSettings.seekWithTap

    val thumbnailTapEnabled = appearanceSettings.thumbnailTapEnabled

    val showButtonPlayerAddToPlaylist = appearanceSettings.showButtonPlayerAddToPlaylist
    val showButtonPlayerArrow = appearanceSettings.showButtonPlayerArrow
    val showButtonPlayerLoop = appearanceSettings.showButtonPlayerLoop
    val showButtonPlayerLyrics = appearanceSettings.showButtonPlayerLyrics
    val expandedplayertoggle = appearanceSettings.expandedPlayerToggle
    val showButtonPlayerShuffle = appearanceSettings.showButtonPlayerShuffle
    val showButtonPlayerSleepTimer = appearanceSettings.showButtonPlayerSleepTimer
    val showButtonPlayerMenu = appearanceSettings.showButtonPlayerMenu
    val showButtonPlayerStartradio = appearanceSettings.showButtonPlayerStartRadio

    val showButtonPlayerSystemEqualizer = appearanceSettings.showButtonPlayerSystemEqualizer
    val showButtonPlayerDiscover = appearanceSettings.showButtonPlayerDiscover
    val showButtonPlayerVideo = appearanceSettings.showButtonPlayerVideo

    val navigationBarPosition = appSettings.navigationBarPosition

    val showTotalTimeQueue = appearanceSettings.showTotalTimeQueue

    val backgroundProgress = appearanceSettings.backgroundProgress

    val showNextSongsInPlayer = appearanceSettings.showNextSongsInPlayer
    val showRemainingSongTime = appearanceSettings.showRemainingSongTime
    val clickLyricsText = appearanceSettings.clickLyricsText
    val showBackgroundLyrics = appearanceSettings.showBackgroundLyrics

    val search = Search.init(alwaysVisible = true)

    val thumbnailRoundness = appearanceSettings.thumbnailRoundness

    val miniPlayerType = appearanceSettings.miniPlayerType

    val playerBackgroundColors = appearanceSettings.playerBackgroundColors

    val showTopActionsBar = appearanceSettings.showTopActionsBar
    val showPlayerActionsBar = appearanceSettings.showPlayerActionsBar

    val playerControlsType = appearanceSettings.playerControlsType
    val playerInfoType = appearanceSettings.playerInfoType

    val transparentBackgroundActionBarPlayer = appearanceSettings.transparentBackgroundActionBarPlayer
    val iconLikeType = appearanceSettings.iconLikeType

    val playerSwapControlsWithTimeline = appearanceSettings.playerSwapControlsWithTimeline

    val playerEnableLyricsPopupMessage = appearanceSettings.playerEnableLyricsPopupMessage
    val actionspacedevenly = appearanceSettings.actionsSpacedEvenly
    val thumbnailType = appearanceSettings.thumbnailType
    val showvisthumbnail = appearanceSettings.showvisthumbnail
    val buttonzoomout = appearanceSettings.buttonzoomout
    val thumbnailpause = appearanceSettings.thumbnailpause
    val showsongs = appearanceSettings.showsongs
    val showalbumcover = appearanceSettings.showalbumcover
    val prevNextSongs = appearanceSettings.prevNextSongs
    val tapqueue = appearanceSettings.tapqueue
    val swipeUpQueue = appearanceSettings.swipeUpQueue
    val statsfornerds = appearanceSettings.statsfornerds

    val playerType = appearanceSettings.playerType
    val queueType = appearanceSettings.queueType
    val noblur = appearanceSettings.noblur
    val fadingedge = appearanceSettings.fadingedge
    val carousel = appearanceSettings.carousel
    val carouselSize = appearanceSettings.carouselSize
    val keepPlayerMinimized = appearanceSettings.keepPlayerMinimized
    val playerInfoShowIcons = appearanceSettings.playerInfoShowIcons
    val queueDurationExpanded = appearanceSettings.queueDurationExpanded
    val titleExpanded = appearanceSettings.titleExpanded
    val timelineExpanded = appearanceSettings.timelineExpanded
    val controlsExpanded = appearanceSettings.controlsExpanded
    val miniQueueExpanded = appearanceSettings.miniQueueExpanded
    val statsExpanded = appearanceSettings.statsExpanded
    val actionExpanded = appearanceSettings.actionExpanded
    var restartService by rememberSaveable { mutableStateOf(false) }
    var restartActivity by rememberSaveable { mutableStateOf(false) }
    val showCoverThumbnailAnimation = appearanceSettings.showCoverThumbnailAnimation
    val coverThumbnailAnimation = appearanceSettings.coverThumbnailAnimation

    val notificationPlayerFirstIcon = appSettings.notificationPlayerFirstIcon
    val notificationPlayerSecondIcon = appSettings.notificationPlayerSecondIcon
    val enableWallpaper = appSettings.enableWallpaper
    val wallpaperType = appSettings.wallpaperType
    val topPadding = appearanceSettings.topPadding
    val animatedGradient = appearanceSettings.animatedGradient
    var appearanceChooser by remember{ mutableStateOf(false)}
    val albumCoverRotation = appearanceSettings.albumCoverRotation

    val blurStrength = appearanceSettings.blurStrength
    val thumbnailFadeEx = appearanceSettings.thumbnailFadeEx
    val thumbnailFade = appearanceSettings.thumbnailFade
    val thumbnailSpacing = appearanceSettings.thumbnailSpacing
    val colorPaletteName = appearanceSettings.colorPaletteName
    val colorPaletteMode = appearanceSettings.colorPaletteMode
    val swipeAnimationNoThumbnail = appearanceSettings.swipeAnimationNoThumbnail

    val playerTransitionsEnabled = appearanceSettings.playerTransitionsEnabled
    val playerTransitionAnimation = appearanceSettings.playerTransitionAnimation

    val audioVisualizerStyleInPlayer = appearanceSettings.audioVisualizerStyleInPlayer
    val audioVisualizerStyleInMiniPlayer = appearanceSettings.audioVisualizerStyleInMiniPlayer
    val audioVisualizerStyleInEqualizer = appearanceSettings.audioVisualizerStyleInEqualizer


    val micPermission = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { isGranted ->
            if (!isGranted) return@rememberPermissionState
        }
    )

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
                        //writeRow("Appearance", appearanceFilename, "expandedplayer", expandedplayer)
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

    // Used to import old appearance settings from file
    val preferences = remember { AppearancePreferences.getInstance(context) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            //requestPermission(activity, "Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED")

            context.applicationContext.contentResolver.openInputStream(uri)
                ?.use { inputStream ->
                    // NEW FEATURE THEMES REPO
                    runCatching {
                        val encoded = inputStream.bufferedReader().readText()
                        AppearanceSettings.fromShareString(encoded)
                    }
                        .onSuccess { settings -> preferences.applyFrom(settings) }
                        .onFailure { Timber.e("PlayerAppearanceSettings failed to load appearance from file") }

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

        LaunchedEffect(Unit, playerBackgroundColors, audioVisualizerStyleInPlayer, showthumbnail, playerType) {
            if (playerBackgroundColors != PlayerBackgroundColors.BlurredCoverColor) {
                val new = appearanceSettingsManager.activeSettings.value.copy(showThumbnail = true)
                appearanceSettingsManager.updatePreset(new)
            }

            if (audioVisualizerStyleInPlayer == AudioVisualizerStyle.NONE) {
                val new = appearanceSettingsManager.activeSettings.value.copy(showvisthumbnail = false)
                appearanceSettingsManager.updatePreset(new)
            }

            if (!showthumbnail) {
                val new =
                    appearanceSettingsManager.activeSettings.value.copy(showvisthumbnail = false, showLyricsThumbnail = false)
                appearanceSettingsManager.updatePreset(new)
            }
            if (playerType == PlayerType.Modern) {
                val new = appearanceSettingsManager.activeSettings.value.copy(
                    showLyricsThumbnail = false,
                    showvisthumbnail = false,
                    thumbnailpause = false
                )
                appearanceSettingsManager.updatePreset(new)
            }
        }

        // NEW FEATURE THEMES REPO
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

        val coroutineScope = rememberCoroutineScope()

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
                        title = stringResource(R.string.player_appearance),
                        iconId = R.drawable.color_palette,
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

                if (search.matchesAny(R.string.player_transitions_enabled, R.string.player_transitions_enabled_description, R.string.player_transition_animation, R.string.player_transition_animation_description, R.string.show_player_top_actions_bar, R.string.blankspace, R.string.playertype, R.string.queuetype, R.string.miniplayertype, R.string.player_swap_controls_with_timeline, R.string.pcontrols_type, R.string.disable_scrolling_text, R.string.scrolling_text_is_used_for_long_texts, R.string.disable_horizontal_swipe, R.string.disable_vertical_swipe, R.string.disable_vertical_swipe_secondary, R.string.disable_song_switching_via_swipe)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_player_layout))


                settingsGroupCard {
                    if (search.matchesAny(R.string.player_transitions_enabled)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_transitions_enabled),
                            text = stringResource(R.string.player_transitions_enabled_description),
                            isChecked = playerTransitionsEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerTransitionsEnabled = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.slow_motion,
                        )

                        AnimatedVisibility(visible = playerTransitionsEnabled) {
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.player_transition_animation),
                                    titleSecondary = stringResource(R.string.player_transition_animation_description),
                                    selectedValue = playerTransitionAnimation,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    playerTransitionAnimation = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    },
                                    valueText = { it.name }, icon = R.drawable.slow_motion,
                                )
                            }
                        }
                    }
                    if (search.matchesAny(R.string.show_player_top_actions_bar))
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_player_top_actions_bar),
                            text = "",
                            isChecked = showTopActionsBar,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        showTopActionsBar = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.horizontal_bold_line,
                        )
                    if (!showTopActionsBar) {
                        if (search.matchesAny(R.string.blankspace))
                            SwitchSettingEntry(
                                title = stringResource(R.string.blankspace),
                                text = "",
                                isChecked = topPadding,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                topPadding = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.resize,
                            )
                    }
                    if (search.matchesAny(R.string.playertype))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.playertype),
                            selectedValue = playerType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.play,
                        )
                    if (search.matchesAny(R.string.queuetype))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.queuetype),
                            selectedValue = queueType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        queueType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.list,
                        )
                    if (search.matchesAny(R.string.miniplayertype))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.miniplayertype),
                            selectedValue = miniPlayerType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        miniPlayerType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.player_mini_widget,
                        )
                    if (search.matchesAny(R.string.player_swap_controls_with_timeline))
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_swap_controls_with_timeline),
                            text = "",
                            isChecked = playerSwapControlsWithTimeline,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new =
                                        appearanceSettingsManager.activeSettings.value.copy(
                                            playerSwapControlsWithTimeline = it
                                        )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.reorder,
                        )
                    if (search.matchesAny(R.string.pcontrols_type))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.pcontrols_type),
                            selectedValue = playerControlsType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerControlsType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.play,
                        )
                    if (search.matchesAny(R.string.disable_scrolling_text))
                        SwitchSettingEntry(
                            title = stringResource(R.string.disable_scrolling_text),
                            text = stringResource(R.string.scrolling_text_is_used_for_long_texts),
                            isChecked = disableScrollingText,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        disableScrollingText = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.text,
                        )
                    if (search.matches(stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_horizontal_swipe else R.string.disable_vertical_swipe)))
                        SwitchSettingEntry(
                            title = stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_vertical_swipe else R.string.disable_horizontal_swipe),
                            text = stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_vertical_swipe_secondary else R.string.disable_song_switching_via_swipe),
                            isChecked = disablePlayerHorizontalSwipe,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new =
                                        appearanceSettingsManager.activeSettings.value.copy(
                                            disablePlayerHorizontalSwipe = it
                                        )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }
                        )
                }
                }


if (search.matchesAny(R.string.show_thumbnail, R.string.swipe_Animation_No_Thumbnail, R.string.fadingedge, R.string.carousel, R.string.carouselsize, R.string.thumbnailpause, R.string.show_lyrics_thumbnail, R.string.showvisthumbnail, R.string.show_cover_thumbnail_animation, R.string.cover_thumbnail_animation_type, R.string.player_thumbnail_size, R.string.thumbnailtype, R.string.thumbnail_roundness, R.string.noblur, R.string.albumCoverRotation)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_thumbnail))


                settingsGroupCard {
                    if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) {
                        if (search.matchesAny(R.string.show_thumbnail))
                            SwitchSettingEntry(
                                title = stringResource(R.string.show_thumbnail),
                                text = "",
                                isChecked = showthumbnail,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                showThumbnail = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.picture,
                            )
                    }
                    AnimatedVisibility(visible = !showthumbnail && playerType == PlayerType.Modern && !isLandscape) {
                        if (search.matchesAny(R.string.swipe_Animation_No_Thumbnail))
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.swipe_Animation_No_Thumbnail),
                                selectedValue = swipeAnimationNoThumbnail,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                swipeAnimationNoThumbnail = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                },
                                valueText = { it.displayName },
                                modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.flip,
                            )
                    }
                    AnimatedVisibility(visible = showthumbnail) {
                        Column {
                            if (playerType == PlayerType.Modern) {
                                if (search.matchesAny(R.string.fadingedge))
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.fadingedge),
                                        text = "",
                                        isChecked = fadingedge,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        fadingedge = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.slow_motion,
                                    )
                            }

                            if (playerType == PlayerType.Modern && !isLandscape
                                //&& (expandedplayertoggle || expandedplayer)
                                )
                            {
                                if (search.matchesAny(R.string.carousel))
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.carousel),
                                        text = "",
                                        isChecked = carousel,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        carousel = it,
                                                        expandedPlayerToggle = it,
                                                        expandedPlayer = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.cd,
                                    )

                                if (search.matchesAny(R.string.carouselsize))
                                    EnumValueSelectorSettingsEntry(
                                        title = stringResource(R.string.carouselsize),
                                        selectedValue = carouselSize,
                                        onValueSelected = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        carouselSize = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        valueText = { it.displayName },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.resize,
                                    )
                            }
                            if (playerType == PlayerType.Essential) {

                                if (search.matchesAny(R.string.thumbnailpause))
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.thumbnailpause),
                                        text = "",
                                        isChecked = thumbnailpause,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        thumbnailpause = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.pause,
                                    )

                                if (search.matchesAny(R.string.show_lyrics_thumbnail))
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.show_lyrics_thumbnail),
                                        text = "",
                                        isChecked = showlyricsthumbnail,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        showLyricsThumbnail = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.musical_notes,
                                    )
                                if (audioVisualizerStyleInPlayer != AudioVisualizerStyle.NONE) {
                                    if (search.matchesAny(R.string.showvisthumbnail))
                                        SwitchSettingEntry(
                                            title = stringResource(R.string.showvisthumbnail),
                                            text = "",
                                            isChecked = showvisthumbnail,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val new =
                                                        appearanceSettingsManager.activeSettings.value.copy(
                                                            showvisthumbnail = it
                                                        )
                                                    appearanceSettingsManager.updatePreset(new)
                                                }
                                            },
                                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.soundwave,
                                        )
                                }
                            }

                            if (search.matchesAny(R.string.show_cover_thumbnail_animation)) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.show_cover_thumbnail_animation),
                                    text = "",
                                    isChecked = showCoverThumbnailAnimation,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    showCoverThumbnailAnimation = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    },
                                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.picture,
                                )
                                AnimatedVisibility(visible = showCoverThumbnailAnimation) {
                                    Column {
                                        EnumValueSelectorSettingsEntry(
                                            title = stringResource(R.string.cover_thumbnail_animation_type),
                                            selectedValue = coverThumbnailAnimation,
                                            onValueSelected = {
                                                coroutineScope.launch {
                                                    val new =
                                                        appearanceSettingsManager.activeSettings.value.copy(
                                                            coverThumbnailAnimation = it
                                                        )
                                                    appearanceSettingsManager.updatePreset(new)
                                                }
                                            },
                                            valueText = { it.textName },
                                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 24.dp else 12.dp), icon = R.drawable.picture,
                                        )
                                    }
                                }
                            }

                            if (isLandscape) {
                                if (search.matchesAny(R.string.player_thumbnail_size))
                                    EnumValueSelectorSettingsEntry(
                                        title = stringResource(R.string.player_thumbnail_size),
                                        selectedValue = playerThumbnailSizeL,
                                        onValueSelected = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        playerThumbnailSizeL = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        valueText = { it.displayName },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.resize,
                                    )
                            } else {
                                if (search.matchesAny(R.string.player_thumbnail_size))
                                    EnumValueSelectorSettingsEntry(
                                        title = stringResource(R.string.player_thumbnail_size),
                                        selectedValue = playerThumbnailSize,
                                        onValueSelected = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        playerThumbnailSize = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        valueText = { it.displayName },
                                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.resize,
                                    )
                            }
                            if (search.matchesAny(R.string.thumbnailtype))
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.thumbnailtype),
                                    selectedValue = thumbnailType,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    thumbnailType = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    },
                                    valueText = { it.displayName },
                                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.picture,
                                )

                            if (search.matchesAny(R.string.thumbnail_roundness))
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.thumbnail_roundness),
                                    selectedValue = thumbnailRoundness,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    thumbnailRoundness = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    },
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
                                    valueText = { it.displayName },
                                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 12.dp else 0.dp), icon = R.drawable.shapes,
                                )
                        }
                    }
                    if (!showthumbnail) {
                        if (search.matchesAny(R.string.noblur))
                            SwitchSettingEntry(
                                title = stringResource(R.string.noblur),
                                text = "",
                                isChecked = noblur,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                noblur = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.eye_off,
                            )


                    }
                    if ((playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) && (playerType == PlayerType.Modern))
                        if (search.matchesAny(R.string.albumCoverRotation))
                            SwitchSettingEntry(
                                title = stringResource(R.string.albumCoverRotation),
                                text = "",
                                isChecked = albumCoverRotation,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                albumCoverRotation = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp), icon = R.drawable.cd,
                            )
                }
                }


if (search.matchesAny(R.string.statsfornerdsplayer, R.string.pinfo_type, R.string.pinfo_album_and_artist_name, R.string.pinfo_show_icons, R.string.textoutline, R.string.show_total_time_of_queue, R.string.show_remaining_song_time, R.string.show_next_songs_in_player, R.string.showtwosongs, R.string.songs_number_to_show, R.string.showalbumcover)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_song_info))


                settingsGroupCard {
                    if (!(showthumbnail && playerType == PlayerType.Essential)) {
                        if (search.matchesAny(R.string.statsfornerdsplayer))
                            SwitchSettingEntry(
                                title = stringResource(R.string.statsfornerdsplayer),
                                text = "",
                                isChecked = statsfornerds,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                statsfornerds = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.query_stats,
                            )
                    }
                    if (search.matchesAny(R.string.pinfo_type)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.pinfo_type),
                            selectedValue = playerInfoType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerInfoType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.text,
                        )
                        SettingsDescription(text = stringResource(R.string.pinfo_album_and_artist_name))

                        AnimatedVisibility(visible = playerInfoType == PlayerInfoType.Modern) {
                            Column {
                                if (search.matchesAny(R.string.pinfo_show_icons))
                                    SwitchSettingEntry(
                                        title = stringResource(R.string.pinfo_show_icons),
                                        text = "",
                                        isChecked = playerInfoShowIcons,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val new =
                                                    appearanceSettingsManager.activeSettings.value.copy(
                                                        playerInfoShowIcons = it
                                                    )
                                                appearanceSettingsManager.updatePreset(new)
                                            }
                                        },
                                        modifier = Modifier
                                            .padding(start = 12.dp), icon = R.drawable.information_circle,
                                    )
                            }
                        }

                    }
                    if (search.matchesAny(R.string.textoutline))
                        SwitchSettingEntry(
                            title = stringResource(R.string.textoutline),
                            text = "",
                            isChecked = textoutline,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        textoutline = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.text,
                        )
                    if (search.matchesAny(R.string.show_total_time_of_queue))
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_total_time_of_queue),
                            text = "",
                            isChecked = showTotalTimeQueue,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        showTotalTimeQueue = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.time,
                        )
                    if (search.matchesAny(R.string.show_remaining_song_time))
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_remaining_song_time),
                            text = "",
                            isChecked = showRemainingSongTime,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        showRemainingSongTime = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.time,
                        )
                    if (search.matchesAny(R.string.show_next_songs_in_player))
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_next_songs_in_player),
                            text = "",
                            isChecked = showNextSongsInPlayer,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        showNextSongsInPlayer = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.addqueue,
                        )
                    AnimatedVisibility(visible = showNextSongsInPlayer) {
                        Column {
                            if (search.matchesAny(R.string.showtwosongs))
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.songs_number_to_show),
                                    selectedValue = showsongs,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    showsongs = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    },
                                    valueText = {
                                        it.name
                                    },
                                    modifier = Modifier
                                        .padding(start = 12.dp), icon = R.drawable.list,
                                )


                            if (search.matchesAny(R.string.showalbumcover))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.showalbumcover),
                                    text = "",
                                    isChecked = showalbumcover,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    showalbumcover = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    },
                                    modifier = Modifier.padding(start = 12.dp), icon = R.drawable.picture,
                                )
                        }
                    }
                }
                }


if (search.matchesAny(R.string.timeline, R.string.transparentbar, R.string.timelinesize, R.string.seek_with_tap, R.string.background_progress_bar)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_timeline))


                settingsGroupCard {
                    if (search.matchesAny(R.string.timeline)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.timeline),
                            selectedValue = playerTimelineType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerTimelineType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }

                                restartActivity = true // applied also for online player
                            },
                            valueText = { it.textName }, icon = R.drawable.horizontal_bold_line,
                        )
                        RestartActivity(restartActivity, onRestart = { restartActivity = false })
                    }
                    if (search.matchesAny(R.string.transparentbar))
                        SwitchSettingEntry(
                            title = stringResource(R.string.transparentbar),
                            text = "",
                            isChecked = transparentbar,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        transparentBar = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.eye_off,
                        )
                    if (search.matchesAny(R.string.timelinesize))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.timelinesize),
                            selectedValue = playerTimelineSize,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerTimelineSize = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.resize,
                        )
                    if (search.matchesAny(R.string.seek_with_tap))
                        SwitchSettingEntry(
                            title = stringResource(R.string.seek_with_tap),
                            text = "",
                            isChecked = seekWithTap,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        seekWithTap = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.left_and_right_arrows,
                        )
                    if (search.matchesAny(R.string.background_progress_bar))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.background_progress_bar),
                            selectedValue = backgroundProgress,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        backgroundProgress = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.horizontal_bold_line,
                        )
                }
                }


if (search.matchesAny(R.string.play_button, R.string.buttonzoomout, R.string.icon_like_button)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_buttons))


                settingsGroupCard {
                    if (search.matchesAny(R.string.play_button))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.play_button),
                            selectedValue = playerPlayButtonType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerPlayButtonType = it,
                                        lastPlayerPlayButtonType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.play,
                        )
                    if (search.matchesAny(R.string.buttonzoomout))
                        SwitchSettingEntry(
                            title = stringResource(R.string.buttonzoomout),
                            text = "",
                            isChecked = buttonzoomout,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        buttonzoomout = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.resize,
                        )
                    if (search.matchesAny(R.string.play_button))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.icon_like_button),
                            selectedValue = iconLikeType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        iconLikeType = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.heart,
                        )
                }
                }


if (search.matchesAny(R.string.settings_themes_manager, R.string.appearancepresets, R.string.settings_themes_manager_title_swithes_between_themes, R.string.background_colors, R.string.gradienttype, R.string.blackgradient, R.string.bottomgradient)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_background))


                settingsGroupCard {
                    ButtonBarSettingEntry(
                        title = stringResource(R.string.settings_themes_manager), //stringResource(R.string.appearancepresets),
                        text = stringResource(R.string.settings_themes_manager_title_swithes_between_themes),
                        icon = R.drawable.color_palette,
                        iconColor = colorPalette().text,
                        onClick = { appearanceChooser = true },
                    )
                    if (search.matchesAny(R.string.background_colors))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.background_colors),
                            selectedValue = playerBackgroundColors,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        playerBackgroundColors = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            },
                            valueText = { it.textName }, icon = R.drawable.color_palette,
                        )
                    AnimatedVisibility(visible = playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) {
                        if (search.matchesAny(R.string.gradienttype))
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.gradienttype),
                                selectedValue = animatedGradient,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                animatedGradient = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                },
                                valueText = { it.displayName },
                                modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) 12.dp else 0.dp), icon = R.drawable.color_palette,
                            )
                    }
                    if ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient))
                        if (search.matchesAny(R.string.blackgradient))
                            SwitchSettingEntry(
                                title = stringResource(R.string.blackgradient),
                                text = "",
                                isChecked = blackgradient,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                blackgradient = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.moon,
                            )
                    if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor)
                        if (search.matchesAny(R.string.bottomgradient))
                            SwitchSettingEntry(
                                title = stringResource(R.string.bottomgradient),
                                text = "",
                                isChecked = bottomgradient,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                bottomGradient = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.color_palette,
                            )
                }
                }


if (search.matchesAny(R.string.toggle_lyrics, R.string.by_tapping_on_the_thumbnail, R.string.click_lyrics_text, R.string.show_background_in_lyrics, R.string.player_enable_lyrics_popup_message)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_lyrics))


                settingsGroupCard {
                    if (search.matchesAny(R.string.toggle_lyrics))
                        SwitchSettingEntry(
                            title = stringResource(R.string.toggle_lyrics),
                            text = stringResource(R.string.by_tapping_on_the_thumbnail),
                            isChecked = thumbnailTapEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        thumbnailTapEnabled = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.musical_notes,
                        )
                    if (search.matchesAny(R.string.click_lyrics_text))
                        SwitchSettingEntry(
                            title = stringResource(R.string.click_lyrics_text),
                            text = "",
                            isChecked = clickLyricsText,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(
                                        clickLyricsText = it
                                    )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.musical_notes,
                        )
                    if (showlyricsthumbnail)
                        if (search.matchesAny(R.string.show_background_in_lyrics))
                            SwitchSettingEntry(
                                title = stringResource(R.string.show_background_in_lyrics),
                                text = "",
                                isChecked = showBackgroundLyrics,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appearanceSettingsManager.activeSettings.value.copy(
                                                showBackgroundLyrics = it
                                            )
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.picture,
                            )
                    if (search.matchesAny(R.string.player_enable_lyrics_popup_message))
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_enable_lyrics_popup_message),
                            text = "",
                            isChecked = playerEnableLyricsPopupMessage,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new =
                                        appearanceSettingsManager.activeSettings.value.copy(
                                            playerEnableLyricsPopupMessage = it
                                        )
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.information_circle,
                        )
                }
                }




            


                    if (search.matchesAny(R.string.audio_visualizer, R.string.audio_visualizer_title_player_style, R.string.audio_visualizer_title_mini_player_style, R.string.audio_visualizer_title_equalizer_style, R.string.visualizer_require_mic_permission)) {
                    settingsGroupHeader(title = context.resources.getString(R.string.audio_visualizer))

                    settingsGroupCard {

                        if (search.matchesAny(R.string.audio_visualizer)) {
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.audio_visualizer_title_player_style),
                                selectedValue = audioVisualizerStyleInPlayer,
                                onValueSelected = {
                                    if (!micPermission.status.isGranted) {
                                        micPermission.launchPermissionRequest()
                                    } else {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(
                                                    audioVisualizerStyleInPlayer = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }
                                },
                                valueText = {
                                    it.displayName
                                }, icon = R.drawable.soundwave,
                            )
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.audio_visualizer_title_mini_player_style),
                                selectedValue = audioVisualizerStyleInMiniPlayer,
                                onValueSelected = {
                                    if (!micPermission.status.isGranted) {
                                        micPermission.launchPermissionRequest()
                                    } else {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager
                                                .activeSettings.value
                                                .copy(
                                                    audioVisualizerStyleInMiniPlayer = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }
                                },
                                valueText = {
                                    it.displayName
                                }, icon = R.drawable.soundwave,
                            )

                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.audio_visualizer_title_equalizer_style),
                                selectedValue = audioVisualizerStyleInEqualizer,
                                onValueSelected = {
                                    if (!micPermission.status.isGranted) {
                                        micPermission.launchPermissionRequest()
                                    } else {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager
                                                .activeSettings.value
                                                .copy(
                                                    audioVisualizerStyleInEqualizer = it
                                                )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }
                                },
                                valueText = {
                                    it.displayName
                                }, icon = R.drawable.equalizer,
                            )

                            ImportantSettingsDescription(text = stringResource(R.string.visualizer_require_mic_permission))
                        }

                    
                }
                    }


                if (search.matchesAny(R.string.player_action_bar, R.string.action_bar_transparent_background, R.string.actionspacedevenly, R.string.tapqueue, R.string.swipe_up_to_open_the_queue, R.string.action_bar_show_video_button, R.string.action_bar_show_discover_button, R.string.action_bar_show_download_button, R.string.action_bar_show_add_to_playlist_button, R.string.action_bar_show_loop_button, R.string.action_bar_show_shuffle_button, R.string.action_bar_show_lyrics_button, R.string.expandedplayer, R.string.action_bar_show_sleep_timer_button, R.string.show_equalizer, R.string.action_bar_show_arrow_button_to_open_queue, R.string.action_bar_show_start_radio_button, R.string.action_bar_show_menu_button)) {
                settingsGroupHeader(title = context.resources.getString(R.string.player_action_bar), icon = R.drawable.horizontal_bold_line,)


                settingsGroupCard {

                    if (search.matchesAny(R.string.player_action_bar))
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_action_bar),
                            text = "",
                            isChecked = showPlayerActionsBar,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(showPlayerActionsBar = it)
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.horizontal_bold_line,
                        )
                




                    AnimatedVisibility(visible = showPlayerActionsBar) {
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            if (search.matchesAny(R.string.action_bar_transparent_background))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_transparent_background),
                                    text = "",
                                    isChecked = transparentBackgroundActionBarPlayer,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(
                                                transparentBackgroundActionBarPlayer = it
                                            )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.eye_off,
                                )

                            if (search.matchesAny(R.string.actionspacedevenly))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.actionspacedevenly),
                                    text = "",
                                    isChecked = actionspacedevenly,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(actionsSpacedEvenly = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.reorder,
                                )

                            if (search.matchesAny(R.string.tapqueue))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.tapqueue),
                                    text = "",
                                    isChecked = tapqueue,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(tapqueue = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.addqueue,
                                )

                            if (search.matchesAny(R.string.swipe_up_to_open_the_queue))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.swipe_up_to_open_the_queue),
                                    text = "",
                                    isChecked = swipeUpQueue,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(swipeUpQueue = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.arrow_up,
                                )

                            if (search.matchesAny(R.string.action_bar_show_video_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_video_button),
                                    text = "",
                                    isChecked = showButtonPlayerVideo,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerVideo = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.video,
                                )

                            if (search.matchesAny(R.string.action_bar_show_discover_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_discover_button),
                                    text = "",
                                    isChecked = showButtonPlayerDiscover,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerDiscover = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.trending,
                                )

                            if (search.matchesAny(R.string.action_bar_show_add_to_playlist_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_add_to_playlist_button),
                                    text = "",
                                    isChecked = showButtonPlayerAddToPlaylist,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(
                                                showButtonPlayerAddToPlaylist = it
                                            )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.add_in_playlist,
                                )

                            if (search.matchesAny(R.string.action_bar_show_loop_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_loop_button),
                                    text = "",
                                    isChecked = showButtonPlayerLoop,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerLoop = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.repeatone,
                                )

                            if (search.matchesAny(R.string.action_bar_show_shuffle_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_shuffle_button),
                                    text = "",
                                    isChecked = showButtonPlayerShuffle,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerShuffle = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.shuffle,
                                )

                            if (search.matchesAny(R.string.action_bar_show_lyrics_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_lyrics_button),
                                    text = "",
                                    isChecked = showButtonPlayerLyrics,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerLyrics = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.musical_notes,
                                )
                            if (!isLandscape // || !showthumbnail
                                ) {
                                if (!showlyricsthumbnail) {
                                    if (search.matchesAny(R.string.expandedplayer))
                                        SwitchSettingEntry(
                                            title = stringResource(R.string.expandedplayer),
                                            text = "",
                                            isChecked = expandedplayertoggle,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val new =
                                                        appearanceSettingsManager.activeSettings.value.copy(expandedPlayerToggle = it)
                                                    appearanceSettingsManager.updatePreset(new)
                                                }
                                            }, icon = R.drawable.resize,
                                        )
                                }
                            }

                            if (search.matchesAny(R.string.action_bar_show_sleep_timer_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_sleep_timer_button),
                                    text = "",
                                    isChecked = showButtonPlayerSleepTimer,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerSleepTimer = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.sleep,
                                )

                            if (search.matchesAny(R.string.show_equalizer))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.show_equalizer),
                                    text = "",
                                    isChecked = showButtonPlayerSystemEqualizer,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(
                                                showButtonPlayerSystemEqualizer = it
                                            )
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.equalizer,
                                )

                            /*
                            if (search.matchesAny(R.string.action_bar_show_arrow_button_to_open_queue))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_arrow_button_to_open_queue),
                                    text = "",
                                    isChecked = showButtonPlayerArrow,
                                    onCheckedChange = { showButtonPlayerArrow = it }, icon = R.drawable.chevron_up,
                                )
                            */

                            if (search.matchesAny(R.string.action_bar_show_start_radio_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_start_radio_button),
                                    text = "",
                                    isChecked = showButtonPlayerStartradio,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerStartRadio = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.radio,
                                )

                            if (search.matchesAny(R.string.action_bar_show_menu_button))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.action_bar_show_menu_button),
                                    text = "",
                                    isChecked = showButtonPlayerMenu,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(showButtonPlayerMenu = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.burger,
                                )
                        }
                    }
                
                    }
                }


                if (!showlyricsthumbnail) {
                    if (search.matchesAny(R.string.show_total_time_of_queue, R.string.titleartist, R.string.timeline, R.string.controls, R.string.statsfornerds, R.string.actionbar, R.string.miniqueue)) {
                    settingsGroupHeader(title = context.resources.getString(R.string.full_screen_lyrics_components))


                    settingsGroupCard {

                        if (showTotalTimeQueue) {
                            if (search.matchesAny(R.string.show_total_time_of_queue))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.show_total_time_of_queue),
                                    text = "",
                                    isChecked = queueDurationExpanded,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(queueDurationExpanded = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.time,
                                )
                        }

                        if (search.matchesAny(R.string.titleartist))
                            SwitchSettingEntry(
                                title = stringResource(R.string.titleartist),
                                text = "",
                                isChecked = titleExpanded,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new = appearanceSettingsManager.activeSettings.value.copy(titleExpanded = it)
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.text,
                            )

                        if (search.matchesAny(R.string.timeline))
                            SwitchSettingEntry(
                                title = stringResource(R.string.timeline),
                                text = "",
                                isChecked = timelineExpanded,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new = appearanceSettingsManager.activeSettings.value.copy(timelineExpanded = it)
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.horizontal_bold_line,
                            )

                        if (search.matchesAny(R.string.controls))
                            SwitchSettingEntry(
                                title = stringResource(R.string.controls),
                                text = "",
                                isChecked = controlsExpanded,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new = appearanceSettingsManager.activeSettings.value.copy(controlsExpanded = it)
                                        appearanceSettingsManager.updatePreset(new)
                                    }
                                }, icon = R.drawable.play,
                            )

                        if (statsfornerds && (!(showthumbnail && playerType == PlayerType.Essential))) {
                            if (search.matchesAny(R.string.statsfornerds))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.statsfornerds),
                                    text = "",
                                    isChecked = statsExpanded,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(statsExpanded = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.query_stats,
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
                            if (search.matchesAny(R.string.actionbar))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.actionbar),
                                    text = "",
                                    isChecked = actionExpanded,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appearanceSettingsManager.activeSettings.value.copy(actionExpanded = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.horizontal_bold_line,
                                )
                        }
                        if (showNextSongsInPlayer && actionExpanded) {
                            if (search.matchesAny(R.string.miniqueue))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.miniqueue),
                                    text = "",
                                    isChecked = miniQueueExpanded,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appearanceSettingsManager.activeSettings.value.copy(miniQueueExpanded = it)
                                            appearanceSettingsManager.updatePreset(new)
                                        }
                                    }, icon = R.drawable.list,
                                )
                        }
                    
                }
                    }


                }

                if (search.matchesAny(R.string.notification_player, R.string.notificationPlayerFirstIcon, R.string.notificationPlayerSecondIcon, R.string.show_song_cover, R.string.lockscreen, R.string.use_song_cover_on_lockscreen)) {
                settingsGroupHeader(title = context.resources.getString(R.string.notification_player))


                settingsGroupCard {

                    if (search.matchesAny(R.string.notification_player)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.notificationPlayerFirstIcon),
                            selectedValue = notificationPlayerFirstIcon,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(notificationPlayerFirstIcon = it)
                                    appSettingsManager.updateSettings(new)
                                }
                                restartService = true
                            },
                            valueText = {
                                it.displayName
                            }, icon = R.drawable.notifications,
                        )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.notificationPlayerSecondIcon),
                            selectedValue = notificationPlayerSecondIcon,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(notificationPlayerSecondIcon = it)
                                    appSettingsManager.updateSettings(new)
                                }

                                restartService = true
                            },
                            valueText = {
                                it.displayName
                            }, icon = R.drawable.notifications,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }


                    if (search.matchesAny(R.string.show_song_cover))

                    SettingsGroupSpacer()

                    SettingsEntryGroupText(title = stringResource(R.string.lockscreen))

                    SwitchSettingEntry(
                        title = stringResource(R.string.show_song_cover),
                        text = stringResource(R.string.use_song_cover_on_lockscreen),
                        isChecked = appSettings.isShowingThumbnailInLockscreen,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val new = appSettingsManager.activeSettings.value.copy(isShowingThumbnailInLockscreen = it)
                                appSettingsManager.updateSettings(new)
                            }
                        }, icon = R.drawable.picture,
                    )


                
                    }
                }


                if (isAtLeastAndroid7) {
                    if (search.matchesAny(R.string.enable_wallpaper, R.string.set_cover_thumbnail_as_wallpaper)) {
                    settingsGroupHeader(title = context.resources.getString(R.string.wallpaper))

                    settingsGroupCard {

                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_wallpaper),
                            text = "",
                            isChecked = enableWallpaper,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(enableWallpaper = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.picture,
                        )
                        AnimatedVisibility(visible = enableWallpaper) {
                            Column {
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.set_cover_thumbnail_as_wallpaper),
                                    selectedValue = wallpaperType,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(wallpaperType = it)
                                            appSettingsManager.updateSettings(new)
                                        }

                                        restartService = true
                                    },
                                    valueText = {
                                        it.displayName
                                    },
                                    modifier = Modifier.padding(start = 12.dp), icon = R.drawable.picture,
                                )
                                RestartPlayerService(
                                    restartService,
                                    onRestart = { restartService = false })
                            }
                        }
                    
                }
                    }


                }

                if (search.matchesAny(R.string.settings_reset, R.string.settings_restore_default_settings, R.string.done, R.string.export_appearance_settings, R.string.info_backup_or_share_appearance_settings, R.string.import_appearance_settings, R.string.info_restore_backup_or_shared_appearance_settings, R.string.info_not_find_app_open_doc)) {
                settingsGroupHeader(context.resources.getString(R.string.settings_reset))


                settingsGroupCard {

                    SettingsGroupSpacer()
                    var resetToDefault by remember { mutableStateOf(false) }

                    ButtonBarSettingEntry(
                        title = stringResource(R.string.settings_reset),
                        text = stringResource(R.string.settings_restore_default_settings),
                        icon = R.drawable.refresh,
                        iconColor = colorPalette().text,
                        onClick = { resetToDefault = true },
                    )

                    LaunchedEffect(resetToDefault) {
                        if (!resetToDefault) return@LaunchedEffect

                        val new = AppearanceSettings()
                        appearanceSettingsManager.updatePreset(new)

                        resetToDefault = false
                        navController.popBackStack()
                        SmartMessage(context.resources.getString(R.string.done), context = context)
                    }


                    SettingsGroupSpacer()

//                    ButtonBarSettingEntry(
//                        title = stringResource(R.string.export_appearance_settings),
//                        text = stringResource(R.string.info_backup_or_share_appearance_settings),
//                        icon = R.drawable.export,
//                        iconColor = colorPalette().text,
//                        onClick = { isExporting = true },
//                    )
//
//                    ButtonBarSettingEntry(
//                        title = stringResource(R.string.import_appearance_settings),
//                        text = stringResource(R.string.info_restore_backup_or_shared_appearance_settings),
//                        icon = R.drawable.resource_import,
//                        iconColor = colorPalette().text,
//                        onClick = {
//                            try {
//                                importLauncher.launch(
//                                    arrayOf(
//                                        "text/*"
//                                    )
//                                )
//                            } catch (e: ActivityNotFoundException) {
//                                SmartMessage(
//                                    context.resources.getString(R.string.info_not_find_app_open_doc),
//                                    type = PopupType.Warning, context = context
//                                )
//                            }
//                        },
//                    )

                



//            SettingsGroupSpacer(
//                modifier = Modifier.height(Dimensions.bottomSpacer)
//            )
            }
                }
        }
    }

        }
}
