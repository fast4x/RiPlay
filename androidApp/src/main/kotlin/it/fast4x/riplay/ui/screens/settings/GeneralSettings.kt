package it.fast4x.riplay.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.RestartActivity
import it.fast4x.riplay.utils.RestartPlayerService
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.languageToName
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.ui.components.themed.Search
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.CheckUpdateState
import it.fast4x.riplay.enums.Countries
import it.fast4x.riplay.enums.EqualizerType
import it.fast4x.riplay.extensions.updater.UpdateDialog
import it.fast4x.riplay.services.helpers.AudioDRCHelper
import it.fast4x.riplay.services.playback.MediaLibraryServiceCallback
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.utils.CheckForNewVersion
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.isIgnoringBatteryOptimizations
import it.fast4x.riplay.utils.restartApp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import it.fast4x.riplay.ui.components.themed.settingsHeaderItem
import it.fast4x.riplay.ui.components.themed.settingsGroupHeader
import it.fast4x.riplay.ui.components.themed.settingsGroupCard
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem


@ExperimentalAnimationApi
@UnstableApi
@Composable
fun GeneralSettings(
    navController: NavController
) {
    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val binder = LocalPlayerServiceBinder.current

    //val systemLocale = LocalLocale.current.platformLocale
    val languageApp = appSettings.languageApp
    val contentCountry = appSettings.contentCountry

    var restartService by rememberSaveable { mutableStateOf(false) }
    var restartActivity by rememberSaveable { mutableStateOf(false) }

    val minTimeForEvent = appSettings.minTimeForEvent
    val persistentQueue = appSettings.persistentQueue
    val resumePlaybackOnStart = appSettings.resumePlaybackOnStart
    val closeBackgroundPlayerAfterMinutes = appSettings.closeBackgroundPlayerAfterMinutes
    val closeWithBackButton = appSettings.closeWithBackButton
    val resumeOrPausePlaybackWhenDeviceBt = appSettings.resumeOrPausePlaybackWhenDeviceBt
    val resumeOrPausePlaybackWhenDeviceWired = appSettings.resumeOrPausePlaybackWhenDeviceWired

    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        coroutineScope.launch {
            if (isGranted) {
                val new = appSettingsManager.activeSettings.value.copy(resumeOrPausePlaybackWhenCall = true)
                appSettingsManager.updateSettings(new)
                restartService = true
            } else {
                val new = appSettingsManager.activeSettings.value.copy(resumeOrPausePlaybackWhenCall = false)
                appSettingsManager.updateSettings(new)
            }
        }
    }

    val skipSilence = appSettings.skipSilenceEnabled
    val skipMediaOnError = appSettings.skipMediaOnError
    val volumeNormalization = appSettings.volumeNormalizationEnabled

    val useDnsOverHttpsType = appSettings.dnsOverHttpsType

    val keepPlayerMinimized = appSettings.keepPlayerMinimized

    val disableClosingPlayerSwipingDown = appSettings.disableClosingPlayerSwipingDown

    val navigationBarPosition = appSettings.navigationBarPosition
    val pauseBetweenSongs = appSettings.pauseBetweenSongs
    val maxSongsInQueue = appSettings.maxSongsInQueue
    val filterContentType = appSettings.filterContentType

    val search = Search.init(alwaysVisible = true)

    val shakeEventEnabled = appSettings.shakeEventEnabled
    val crossfadeDuration = appSettings.crossfadeDuration
    val excludeSongWithDurationLimit = appSettings.excludeSongWithDurationLimit
    val videoContentMode = appSettings.videoContentMode
    val playlistindicator = appearanceSettings.playlistIndicator
    val nowPlayingIndicator = appSettings.nowPlayingIndicator
    val discoverIsEnabled = appSettings.discoverIsEnabled
    val isPauseOnVolumeZeroEnabled = appSettings.isPauseOnVolumeZeroEnabled

    val equalizerType = appSettings.equalizerType

    val minimumSilenceDuration = appSettings.minimumSilenceDuration

    val pauseListenHistory = appSettings.isPauseListenHistoryEnabled


    val loudnessBaseGain = appSettings.loudnessBaseGain
    val autoLoadSongsInQueue = appSettings.autoLoadSongsInQueue

    val bassboostEnabled = appSettings.bassBoostEnabled
    val bassboostLevel = appSettings.bassBoostLevel
    val volumeBoostLevel = appSettings.volumeBoostLevel
    val audioReverb = appSettings.audioReverbPreset
    val audioFocusEnabled = appSettings.handleAudioFocusEnabled

    val enablePictureInPicture = appSettings.enablePictureInPicture
    val enablePictureInPictureAuto = appSettings.enablePictureInPictureAuto
    val enableFloatingLyrics = appSettings.enableFloatingLyricsOverlay
    val pipModule = appSettings.pipModule
    val rewindThresholdDuration = appSettings.rewindThresholdDuration

    val isProxyEnabled = appSettings.proxyEnabled
    val proxyHost = appSettings.proxyHostname
    val proxyPort = appSettings.proxyPort
    val proxyMode = appSettings.proxyMode
    val customDnsOverHttpsServer = appSettings.customDnsOverHttpsServer
    val context = LocalContext.current

    val isAndroidAutoEnabled = appSettings.isAndroidAutoEnabled

//    var isAndroidAutoEnabled by remember {
//        val component = ComponentName(context, PlayerService::class.java)
//        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
//        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
//
//        mutableStateOf(
//            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
//            policy = object : SnapshotMutationPolicy<Boolean> {
//                override fun equivalent(a: Boolean, b: Boolean): Boolean {
//                    context.packageManager.setComponentEnabledSetting(
//                        component,
//                        if (b) enabledFlag else disabledFlag,
//                        PackageManager.DONT_KILL_APP
//                    )
//                    return a == b
//                }
//            }
//        )
//    }

    val showShuffleSongsAA = appSettings.showShuffleSongsAA
    val showMonthlyPlaylistsAA = appSettings.showMonthlyPlaylistAA
    val showInLibraryAA = appSettings.showInLibraryAA
    val showOnDeviceAA = appSettings.showOnDeviceAA
    val showTopSongsAA = appSettings.showTopSongsAA
    val showAllSongsAA = appSettings.showAllSongsAA
    val showPodcastAA = appSettings.showPodcastAA
    val showPinnedAA = appSettings.showPinnedAA
    val showGridAA = appSettings.showGridAA
    val androidAutoPlaylistLimit = appSettings.androidAutoPlaylistLimit

    val isEnabledVoiceInput = appSettings.enableVoiceInput
    val isVoiceHandsFree = appSettings.voiceCommandsHandsFree

    val parentalControlEnabled = appSettings.parentalControlEnabled

    val castType = appSettings.castType

    val disableAudioDRC = appSettings.disableAudioDrc


    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)
    var clearEvents by remember { mutableStateOf(false) }
    if (clearEvents) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_all_playback_events),
            onDismiss = { clearEvents = false },
            onConfirm = { coroutineScope.launch {  Database.clearEvents() }  }
        )
    }
    val unplayableTracksCount by remember {
        Database.unplayableTracksDao().countAll().distinctUntilChanged()
    }.collectAsState(initial = 0)
    var clearUnplayableTracks by remember { mutableStateOf(false) }
    if (clearUnplayableTracks) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_clear_all_unplayable_tracks),
            onDismiss = { clearUnplayableTracks = false },
            onConfirm = { coroutineScope.launch {  Database.unplayableTracksDao().clearAll() }  }
        )
    }

    val pauseSearchHistory = appSettings.pauseSearchHistory

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    val checkUpdateState = appSettings.checkUpdateState

    val internalEqualizer = LocalPlayerServiceBinder.current?.equalizer

    val enabledQrCodeActions = appSettings.qrCodeToActions

    LaunchedEffect(Unit) {
        AudioDRCHelper.init(context)
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
    ) {


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
                        title = stringResource(R.string.tab_general),
                        iconId = R.drawable.app_icon,
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

                if (BuildConfig.FLAVOR == "full") {
                    if (search.matchesAny(R.string.info_no_update_available, R.string.enable_check_for_update, R.string.when_enabled_a_new_version_is_checked_and_notified_during_startup, R.string.check_update, R.string.info_check_update_now)) {
                    settingsGroupHeader(title = context.resources.getString(R.string.check_update))


                    settingsGroupCard {

                        var checkUpdateNow by remember { mutableStateOf(false) }
                        if (checkUpdateNow) {
                            if (checkUpdateState == CheckUpdateState.Enabled)
                                UpdateDialog(onClose = { checkUpdateNow = false })

                            if (checkUpdateState == CheckUpdateState.OnlyCheck)
                                CheckForNewVersion(
                                    onDismiss = { checkUpdateNow = false },
                                    onNoUpdateAvailable = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_no_update_available),
                                                type = PopupType.Info,
                                                context = context
                                            )
                                    },
                                    onClose = { checkUpdateNow = false }
                                )
                        }

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.enable_check_for_update),
                            selectedValue = checkUpdateState,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(checkUpdateState = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.update,
                        )
                        SettingsDescription(text = stringResource(R.string.when_enabled_a_new_version_is_checked_and_notified_during_startup))
                        AnimatedVisibility(visible = checkUpdateState != CheckUpdateState.Disabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SettingsDescription(
                                    text = stringResource(R.string.check_update),
                                    important = true,
                                    modifier = Modifier.weight(1f)
                                )

                                SecondaryTextButton(
                                    text = stringResource(R.string.info_check_update_now),
                                    onClick = { checkUpdateNow = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    
                }
                    }

                }


                if (search.matchesAny(R.string.system_language, R.string.app_language)) {
                settingsGroupHeader(title = context.resources.getString(R.string.languages))


                settingsGroupCard {

                    //SettingsDescription(text = stringResource(R.string.system_language) + ": $systemLocale")

                    if (search.matchesAny(R.string.app_language)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.app_language),
                            selectedValue = languageApp,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new =
                                        appSettingsManager.activeSettings.value.copy(languageApp = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = {
                                languageToName(it)
                            }, icon = R.drawable.globe,
                        )

                        EnumValueSelectorSettingsEntry(
                            title = "Country language",
                            selectedValue = contentCountry,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new =
                                        appSettingsManager.activeSettings.value.copy(contentCountry = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { if (it == Countries.XX) "System Country" else it.countryName }
                        )
                    }
                
                }
                }


                if (search.matchesAny(R.string.title_qrcode_to_actions, R.string.enable_create_and_read_qrcode, R.string.info_create_and_read_qr_code_for_artist_album_playlist_song)) {
                settingsGroupHeader(title = context.resources.getString(R.string.title_qrcode_to_actions))

                settingsGroupCard {

                    if (search.matchesAny(R.string.title_qrcode_to_actions)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_create_and_read_qrcode),
                            text = stringResource(R.string.info_create_and_read_qr_code_for_artist_album_playlist_song),
                            isChecked = enabledQrCodeActions,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(qrCodeToActions = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.qr_code_reader,
                        )
                    }
                
                }
                }


                if (search.matchesAny(R.string.use_alternative_dns, R.string.use_dns_over_https_title, R.string.custom_dns_over_https_server, R.string.info_aternative_dns_server, R.string.enable_proxy, R.string.restarting_riplay_is_required, R.string.proxy_mode, R.string.proxy_host, R.string.proxy_port)) {
                settingsGroupHeader(title = context.resources.getString(R.string.title_network))





//        if (search.input.isBlank() || stringResource(R.string.enable_connection_metered).contains(search.input,true))
//            SwitchSettingEntry(
//                title = stringResource(R.string.enable_connection_metered),
//                text = stringResource(R.string.info_enable_connection_metered),
//                isChecked = isConnectionMeteredEnabled,
//                onCheckedChange = {
//                    isConnectionMeteredEnabled = it
//                    if (it)
//                        audioQualityFormat = AudioQualityFormat.Auto
//                }
//, icon = R.drawable.datamobile,            )

                settingsGroupCard {

                    if (search.matchesAny(R.string.use_alternative_dns)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.use_dns_over_https_title),
                            selectedValue = useDnsOverHttpsType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(dnsOverHttpsType = it)
                                    appSettingsManager.updateSettings(new)
                                }
                                restartActivity = true
                            },
                            valueText = { it.textName }, icon = R.drawable.shield_checkmark,
                        )

                        AnimatedVisibility(visible = useDnsOverHttpsType == DnsOverHttpsType.Custom) {
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.custom_dns_over_https_server),
                                    text = customDnsOverHttpsServer,
                                    currentText = customDnsOverHttpsServer,
                                    onTextSave = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(customDnsOverHttpsServer = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                        restartActivity = true
                                    },
                                    validationType = ValidationType.Url, icon = R.drawable.server,
                                )
                                RestartActivity(
                                    restartActivity,
                                    onRestart = { restartActivity = false })
                            }

                        }

                        SettingsDescription(text = stringResource(R.string.info_aternative_dns_server))
                        if (useDnsOverHttpsType != DnsOverHttpsType.Custom)
                            RestartActivity(
                                restartActivity,
                                onRestart = { restartActivity = false })


                    }

                    if (search.matchesAny(R.string.enable_proxy)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_proxy),
                            text = "",
                            isChecked = isProxyEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(proxyEnabled = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.cloud,
                        )
                        SettingsDescription(text = stringResource(R.string.restarting_riplay_is_required))

                        AnimatedVisibility(visible = isProxyEnabled) {
                            Column {
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.proxy_mode),
                                    selectedValue = proxyMode,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(proxyMode = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    },
                                    valueText = { it.name }, icon = R.drawable.sort,
                                )
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.proxy_host),
                                    text = proxyHost,
                                    currentText = proxyHost,
                                    onTextSave = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(proxyHostname = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    },
                                    validationType = ValidationType.Ip, icon = R.drawable.server,
                                )
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.proxy_port),
                                    text = proxyPort.toString(),
                                    currentText = proxyPort.toString(),
                                    onTextSave = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(proxyPort = it.toIntOrNull() ?: 1080)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    }, icon = R.drawable.server,
                                )
                            }
                        }
                    }

                
                }
                }


                if (search.matchesAny(R.string.keep_screen_on, R.string.prevents_screen_timeout, R.string.ignore_battery_optimizations, R.string.battery_optimizations_applied, R.string.is_android12, R.string.not_find_battery_optimization_settings, R.string.already_unrestricted, R.string.disable_background_restrictions, R.string.enable_voice_input, R.string.require_mic_permission)) {
                settingsGroupHeader(title = context.resources.getString(R.string.service_lifetime))


                settingsGroupCard {

                    val context = LocalContext.current
                    val isKeepScreenOnEnabled = appSettings.keepScreenEnabled
                    var isIgnoringBatteryOptimizations by remember {
                        mutableStateOf(context.isIgnoringBatteryOptimizations())
                    }
                    val activityResultLauncher =
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations()
                        }

                    if (search.matchesAny(R.string.keep_screen_on)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.keep_screen_on),
                            text = stringResource(R.string.prevents_screen_timeout),
                            isChecked = isKeepScreenOnEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(keepScreenEnabled = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.eye,
                        )
                    }
                    if (search.matchesAny(R.string.ignore_battery_optimizations)) {
                        ImportantSettingsDescription(text = stringResource(R.string.battery_optimizations_applied))

                        if (isAtLeastAndroid12) {
                            SettingsDescription(text = stringResource(R.string.is_android12))
                        }

                        val msgNoBatteryOptim =
                            stringResource(R.string.not_find_battery_optimization_settings)

                        SettingsEntry(
                            title = stringResource(R.string.ignore_battery_optimizations),
                            isEnabled = !isIgnoringBatteryOptimizations,
                            text = if (isIgnoringBatteryOptimizations) {
                                stringResource(R.string.already_unrestricted)
                            } else {
                                stringResource(R.string.disable_background_restrictions)
                            },
                            onClick = {
                                if (!isAtLeastAndroid6) return@SettingsEntry

                                try {
                                    activityResultLauncher.launch(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = "package:${context.packageName}".toUri()
                                        }
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    try {
                                        activityResultLauncher.launch(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        )
                                    } catch (e: ActivityNotFoundException) {
                                        SmartMessage(
                                            "$msgNoBatteryOptim RiPlay",
                                            type = PopupType.Info,
                                            context = context
                                        )
                                    }
                                }
                            }, icon = R.drawable.battery_charging,
                        )
                    }


                    if (search.matchesAny(R.string.enable_voice_input)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_voice_input),
                            text = stringResource(R.string.require_mic_permission),
                            isChecked = isEnabledVoiceInput,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(enableVoiceInput = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.mic,
                        )
                    }

                    if (isEnabledVoiceInput && (search.input.isBlank() || stringResource(R.string.voice_hands_free).contains(
                            search.input,
                            true
                        ))
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.voice_hands_free),
                            text = stringResource(R.string.voice_hands_free_description),
                            isChecked = isVoiceHandsFree,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(voiceCommandsHandsFree = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )
                    }

                }
                }


                if (search.matchesAny(R.string.close_background_player, R.string.when_app_swipe_out_from_task_manager)) {
                settingsGroupHeader(context.resources.getString(R.string.self_closing))


                settingsGroupCard {


                    if (search.matchesAny(R.string.close_background_player)) {

                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.when_app_swipe_out_from_task_manager),
                                selectedValue = closeBackgroundPlayerAfterMinutes,
                                onValueSelected = {
                                    coroutineScope.launch {
                                        val new =
                                            appSettingsManager.activeSettings.value.copy(closeBackgroundPlayerAfterMinutes = it)
                                        appSettingsManager.updateSettings(new)
                                    }
                                },
                                valueText = { it.displayName }, icon = R.drawable.exit,
                            )


                    }
                
                }
                }


                if (search.matchesAny(R.string.video_content_mode_title, R.string.video_content_mode_description, R.string.jump_previous_title, R.string.jump_previous_description, R.string.min_listening_time, R.string.is_min_list_time_for_tips_or_quick_pics, R.string.exclude_songs_with_duration_limit, R.string.exclude_songs_with_duration_limit_description, R.string.pause_between_songs, R.string.player_pause_on_volume_zero, R.string.info_pauses_player_when_volume_zero, R.string.player_auto_load_songs_in_queue, R.string.player_auto_load_songs_in_queue_description, R.string.max_songs_in_queue, R.string.filter_content_type, R.string.persistent_queue, R.string.save_and_restore_playing_songs, R.string.resume_playback_on_start, R.string.resume_automatically_when_app_opens, R.string.close_app_with_back_button, R.string.when_you_use_the_back_button_from_the_home_page, R.string.skip_media_on_error, R.string.skip_media_on_error_description, R.string.skip_silence, R.string.skip_silent_parts_during_playback, R.string.minimum_silence_length, R.string.minimum_silence_length_description, R.string.format_ms, R.string.parental_control, R.string.info_prevent_play_songs_with_age_limitation, R.string.event_shake, R.string.shake_to_change_song)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_playback))


                settingsGroupCard {
                    if (search.matchesAny(R.string.video_content_mode_title)) {

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.video_content_mode_title),
                            titleSecondary = stringResource(R.string.video_content_mode_description),
                            selectedValue = videoContentMode,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new =
                                        appSettingsManager.activeSettings.value.copy(videoContentMode = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.textName }, icon = R.drawable.video,
                        )

                    }
                    if (search.matchesAny(R.string.jump_previous_title)) {

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.jump_previous_title),
                            titleSecondary = stringResource(R.string.jump_previous_description),
                            selectedValue = rewindThresholdDuration,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new =
                                        appSettingsManager.activeSettings.value.copy(rewindThresholdDuration = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.textName }, icon = R.drawable.arrow_left,
                        )

                    }
                    if (search.matchesAny(R.string.min_listening_time)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.min_listening_time),
                            selectedValue = minTimeForEvent,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(minTimeForEvent = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.time,
                        )
                        SettingsDescription(text = stringResource(R.string.is_min_list_time_for_tips_or_quick_pics))
                    }
                    if (search.matchesAny(R.string.min_listening_time)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.exclude_songs_with_duration_limit),
                            selectedValue = excludeSongWithDurationLimit,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(excludeSongWithDurationLimit = if (it == DurationInMinutes.`0`) DurationInMinutes.`1` else it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.music,
                        )
                        SettingsDescription(text = stringResource(R.string.exclude_songs_with_duration_limit_description))
                    }


                    if (search.matchesAny(R.string.pause_between_songs))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.pause_between_songs),
                            selectedValue = pauseBetweenSongs,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(pauseBetweenSongs = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.pause,
                        )

                    if (search.matchesAny(R.string.player_pause_on_volume_zero))
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_pause_on_volume_zero),
                            text = stringResource(R.string.info_pauses_player_when_volume_zero),
                            isChecked = isPauseOnVolumeZeroEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(isPauseOnVolumeZeroEnabled = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.volume_up,
                        )

                    if (search.matchesAny(R.string.player_auto_load_songs_in_queue)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_auto_load_songs_in_queue),
                            text = stringResource(R.string.player_auto_load_songs_in_queue_description),
                            isChecked = autoLoadSongsInQueue,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(autoLoadSongsInQueue = it)
                                    appSettingsManager.updateSettings(new)
                                }
                                restartService = true
                            }, icon = R.drawable.addqueue,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                    if (search.matchesAny(R.string.max_songs_in_queue))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.max_songs_in_queue),
                            selectedValue = maxSongsInQueue,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(maxSongsInQueue = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.displayName }, icon = R.drawable.list,
                        )
                    if (search.matchesAny(R.string.filter_content_type))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.filter_content_type),
                            selectedValue = filterContentType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(filterContentType = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = {
                                it.textName
                            }, icon = R.drawable.shapes,
                        )
                    if (search.matchesAny(R.string.persistent_queue)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.persistent_queue),
                            text = stringResource(R.string.save_and_restore_playing_songs),
                            isChecked = persistentQueue,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(persistentQueue = it)
                                    appSettingsManager.updateSettings(new)

                                    if(it) binder?.loadQueue() // try to load last known queue now
                                }
                            }, icon = R.drawable.library,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })

                        AnimatedVisibility(visible = persistentQueue) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.resume_playback_on_start),
                                    text = stringResource(R.string.resume_automatically_when_app_opens),
                                    isChecked = resumePlaybackOnStart,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(resumePlaybackOnStart = it)
                                            appSettingsManager.updateSettings(new)
                                        }

                                        //restartService = true
                                    }, icon = R.drawable.play,
                                )
                                RestartPlayerService(
                                    restartService,
                                    onRestart = { restartService = false })
                            }
                        }
                    }
                    if (search.matchesAny(R.string.close_app_with_back_button)) {
                        SwitchSettingEntry(
                            isEnabled = Build.VERSION.SDK_INT >= 33,
                            title = stringResource(R.string.close_app_with_back_button),
                            text = stringResource(R.string.when_you_use_the_back_button_from_the_home_page),
                            isChecked = closeWithBackButton,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(closeWithBackButton = it)
                                    appSettingsManager.updateSettings(new)
                                }

                                restartActivity = true
                            }, icon = R.drawable.exit,
                        )
                    }
                    if (search.matchesAny(R.string.skip_media_on_error)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.skip_media_on_error),
                            text = stringResource(R.string.skip_media_on_error_description),
                            isChecked = skipMediaOnError,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            skipMediaOnError = it
                                        )
                                    )
                                }
                                restartService = true
                            }, icon = R.drawable.alert_circle,
                        )

                        RestartPlayerService(restartService, onRestart = { restartService = false })

                    }
                    if (search.matchesAny(R.string.skip_silence)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.skip_silence),
                            text = stringResource(R.string.skip_silent_parts_during_playback),
                            isChecked = skipSilence,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            skipSilenceEnabled = it
                                        )
                                    )
                                }
                            }, icon = R.drawable.slow_motion,
                        )

                        AnimatedVisibility(visible = skipSilence) {
                            val initialValue by remember { derivedStateOf { minimumSilenceDuration.toFloat() / 1000L } }
                            var newValue by remember(initialValue) {
                                mutableFloatStateOf(
                                    initialValue
                                )
                            }


                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                SliderSettingsEntry(
                                    title = stringResource(R.string.minimum_silence_length),
                                    text = stringResource(R.string.minimum_silence_length_description),
                                    state = newValue,
                                    onSlide = { newValue = it },
                                    onSlideComplete = {
                                        coroutineScope.launch {
                                            appSettingsManager.updateSettings(
                                                appSettingsManager.activeSettings.value.copy(
                                                    minimumSilenceDuration = newValue.toLong() * 1000L
                                                )
                                            )
                                        }

                                        restartService = true
                                    },
                                    toDisplay = { stringResource(R.string.format_ms, it.toLong()) },
                                    range = 1.00f..2000.000f, icon = R.drawable.slow_motion,
                                )

                                RestartPlayerService(
                                    restartService,
                                    onRestart = { restartService = false })
                            }
                        }

                    }
                    if (search.matchesAny(R.string.parental_control))
                        SwitchSettingEntry(
                            title = stringResource(R.string.parental_control),
                            text = stringResource(R.string.info_prevent_play_songs_with_age_limitation),
                            isChecked = parentalControlEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            parentalControlEnabled = it
                                        )
                                    )
                                }
                            }, icon = R.drawable.shield_checkmark,
                        )
                    if (search.matchesAny(R.string.event_shake)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.event_shake),
                            text = stringResource(R.string.shake_to_change_song),
                            isChecked = shakeEventEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            shakeEventEnabled = it
                                        )
                                    )
                                }
                                restartService = true
                            }, icon = R.drawable.smart_shuffle,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                }
                }


                if (search.matchesAny(R.string.player_keep_minimized, R.string.when_click_on_a_song_player_start_minimized, R.string.player_collapsed_disable_swiping_down, R.string.avoid_closing_the_player_cleaning_queue_by_swiping_down, R.string.discover, R.string.discoverinfo, R.string.playlistindicator, R.string.playlistindicatorinfo, R.string.now_playing_indicator)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_player_interface))


                settingsGroupCard {
                    if (search.matchesAny(R.string.player_keep_minimized))
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_keep_minimized),
                            text = stringResource(R.string.when_click_on_a_song_player_start_minimized),
                            isChecked = keepPlayerMinimized,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(keepPlayerMinimized = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.minmax,
                        )

                    if (search.matchesAny(R.string.player_collapsed_disable_swiping_down))
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_collapsed_disable_swiping_down),
                            text = stringResource(R.string.avoid_closing_the_player_cleaning_queue_by_swiping_down),
                            isChecked = disableClosingPlayerSwipingDown,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(disableClosingPlayerSwipingDown = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.arrow_down,
                        )
                    if (search.matchesAny(R.string.discover))
                        SwitchSettingEntry(
                            title = stringResource(R.string.discover),
                            text = stringResource(R.string.discoverinfo),
                            isChecked = discoverIsEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(discoverIsEnabled = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }, icon = R.drawable.trending,
                        )
                    if (search.matchesAny(R.string.playlistindicator))
                        SwitchSettingEntry(
                            title = stringResource(R.string.playlistindicator),
                            text = stringResource(R.string.playlistindicatorinfo),
                            isChecked = playlistindicator,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appearanceSettingsManager.activeSettings.value.copy(playlistIndicator = it)
                                    appearanceSettingsManager.updatePreset(new)
                                }
                            }, icon = R.drawable.playlist,
                        )
                    if (search.matchesAny(R.string.now_playing_indicator))
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.now_playing_indicator),
                            selectedValue = nowPlayingIndicator,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(nowPlayingIndicator = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = {
                                it.textName
                            }, icon = R.drawable.musical_note,
                        )

                }
                }


                if (search.matchesAny(R.string.settings_enable_pip, R.string.settings_pip_module, R.string.settings_enable_pip_auto, R.string.pip_info_from_android_12_pip_can_be_automatically_enabled, R.string.settings_floating_lyrics, R.string.settings_floating_lyrics_info, R.string.floating_lyrics_permission_missing)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_group_screen_windows))


                settingsGroupCard {
                    if (search.matchesAny(R.string.settings_enable_pip)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_enable_pip),
                            text = "",
                            isChecked = enablePictureInPicture,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            enablePictureInPicture = it
                                        )
                                    )
                                }
                                restartActivity = true
                            }, icon = R.drawable.picture,
                        )
                        RestartActivity(restartActivity, onRestart = { restartActivity = false })
                        AnimatedVisibility(visible = enablePictureInPicture) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {

                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.settings_pip_module),
                                    selectedValue = pipModule,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            appSettingsManager.updateSettings(
                                                appSettingsManager.activeSettings.value.copy(
                                                    pipModule = it
                                                )
                                            )
                                        }
                                        restartActivity = true
                                    },
                                    valueText = { it.displayName }, icon = R.drawable.film,
                                )

                                SwitchSettingEntry(
                                    isEnabled = isAtLeastAndroid12,
                                    title = stringResource(R.string.settings_enable_pip_auto),
                                    text = stringResource(R.string.pip_info_from_android_12_pip_can_be_automatically_enabled),
                                    isChecked = enablePictureInPictureAuto,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            appSettingsManager.updateSettings(
                                                appSettingsManager.activeSettings.value.copy(
                                                    enablePictureInPictureAuto = it
                                                )
                                            )
                                        }
                                        restartActivity = true
                                    }, icon = R.drawable.picture,
                                )
                                RestartActivity(
                                    restartActivity,
                                    onRestart = { restartActivity = false })
                            }

                        }
                    }
                    if (search.matchesAny(R.string.settings_floating_lyrics)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_floating_lyrics),
                            text = stringResource(R.string.settings_floating_lyrics_info),
                            isChecked = enableFloatingLyrics,
                            onCheckedChange = { checked ->
                                if (checked && !Settings.canDrawOverlays(context)) {
                                    SmartMessage(
                                    context.resources.getString(R.string.floating_lyrics_permission_missing),
                                        type = PopupType.Warning,
                                        context = context
                                    )
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                "package:${context.packageName}".toUri()
                                            )
                                        )
                                    }
                                } else {
                                    coroutineScope.launch {
                                        appSettingsManager.updateSettings(
                                            appSettingsManager.activeSettings.value.copy(
                                                enableFloatingLyricsOverlay = checked
                                            )
                                        )
                                    }
                                }
                            }, icon = R.drawable.musical_notes,
                        )
                    }

                }
                }


                if (search.matchesAny(R.string.effect_title_crossfade, R.string.effect_subtitle_crossfade_fade_between_tracks_in_queue, R.string.loudness_normalization, R.string.autoadjust_the_volume, R.string.settings_loudness_base_gain, R.string.settings_target_gain_loudness_info, R.string.loudness_boost_level, R.string.loudness_boost_level_info, R.string.settings_audio_bass_boost, R.string.settings_bass_boost_level, R.string.settings_audio_reverb, R.string.settings_audio_reverb_info_apply_a_depth_effect_to_the_audio, R.string.settings_audio_focus, R.string.settings_audio_focus_info, R.string.settings_disable_audio_drc, R.string.equalizer)) {
                settingsGroupHeader(title = context.resources.getString(R.string.audio))


                settingsGroupCard {


                    if (search.matchesAny(R.string.effect_title_crossfade)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.effect_title_crossfade),
                            titleSecondary = stringResource(R.string.effect_subtitle_crossfade_fade_between_tracks_in_queue),
                            selectedValue = crossfadeDuration,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(crossfadeDuration = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.title }, icon = R.drawable.shuffle,
                        )

                    }

                    if (search.matchesAny(R.string.loudness_normalization)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.loudness_normalization),
                            text = stringResource(R.string.autoadjust_the_volume),
                            isChecked = volumeNormalization,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            volumeNormalizationEnabled = it
                                        )
                                    )
                                }
                            }, icon = R.drawable.music_equalizer,
                        )
                        AnimatedVisibility(visible = volumeNormalization) {
                            val initialValue by remember { derivedStateOf { loudnessBaseGain } }
                            var newValue by remember(initialValue) {
                                mutableFloatStateOf(
                                    initialValue
                                )
                            }

                            val initialValueVolume by remember { derivedStateOf { volumeBoostLevel } }
                            var newValueVolume by remember(initialValue) {
                                mutableFloatStateOf(
                                    initialValueVolume
                                )
                            }


                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                SliderSettingsEntry(
                                    title = stringResource(R.string.settings_loudness_base_gain),
                                    text = stringResource(R.string.settings_target_gain_loudness_info),
                                    state = newValue,
                                    onSlide = { newValue = it },
                                    onSlideComplete = {
                                        coroutineScope.launch {
                                            appSettingsManager.updateSettings(
                                                appSettingsManager.activeSettings.value.copy(
                                                    loudnessBaseGain = newValue
                                                )
                                            )
                                        }
                                    },
                                    toDisplay = {
                                        "%.1f dB".format(loudnessBaseGain).replace(",", ".")
                                    },
                                    range = -20f..20f, icon = R.drawable.music_equalizer,
                                )

                                SliderSettingsEntry(
                                    title = stringResource(R.string.loudness_boost_level),
                                    text = stringResource(R.string.loudness_boost_level_info),
                                    state = newValueVolume,
                                    onSlide = { newValueVolume = it },
                                    onSlideComplete = {
                                        coroutineScope.launch {
                                            appSettingsManager.updateSettings(
                                                appSettingsManager.activeSettings.value.copy(
                                                    volumeBoostLevel = newValueVolume
                                                )
                                            )
                                        }
                                    },
                                    toDisplay = {
                                        "%.2f dB".format(volumeBoostLevel).replace(",", ".")
                                    },
                                    range = -20f..20f, icon = R.drawable.music_equalizer,
                                )
                            }
                        }
                    }

                    if (search.matchesAny(R.string.settings_audio_bass_boost)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_audio_bass_boost),
                            text = "",
                            isChecked = bassboostEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            bassBoostEnabled = it
                                        )
                                    )
                                }
                            }, icon = R.drawable.music_equalizer,
                        )
                        AnimatedVisibility(visible = bassboostEnabled) {
                            val initialValue by remember { derivedStateOf { bassboostLevel } }
                            var newValue by remember(initialValue) {
                                mutableFloatStateOf(
                                    initialValue
                                )
                            }


                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                SliderSettingsEntry(
                                    title = stringResource(R.string.settings_bass_boost_level),
                                    text = "",
                                    state = newValue,
                                    onSlide = { newValue = it },
                                    onSlideComplete = {
                                        coroutineScope.launch {
                                            appSettingsManager.updateSettings(
                                                appSettingsManager.activeSettings.value.copy(
                                                    bassBoostLevel = newValue
                                                )
                                            )
                                        }
                                    },
                                    toDisplay = { "%.1f".format(bassboostLevel).replace(",", ".") },
                                    range = 0f..1f, icon = R.drawable.music_equalizer,
                                )
                            }
                        }
                    }

                    if (search.matchesAny(R.string.settings_audio_reverb)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.settings_audio_reverb),
                            titleSecondary = stringResource(R.string.settings_audio_reverb_info_apply_a_depth_effect_to_the_audio),
                            selectedValue = audioReverb,
                            onValueSelected = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            audioReverbPreset = it
                                        )
                                    )
                                }
                                restartService = true
                            },
                            valueText = {
                                it.textName
                            }, icon = R.drawable.music_equalizer,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.matchesAny(R.string.settings_audio_focus)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_audio_focus),
                            text = stringResource(R.string.settings_audio_focus_info),
                            isChecked = audioFocusEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            handleAudioFocusEnabled = it
                                        )
                                    )
                                }
                            }, icon = R.drawable.master_volume,
                        )
                    }

                    if (search.matchesAny(R.string.settings_disable_audio_drc)) {
                        SwitchSettingEntry(
                            isEnabled = AudioDRCHelper.hasDRCSupport(),
                            title = stringResource(R.string.settings_disable_audio_drc),
                            text = if (AudioDRCHelper.hasDRCSupport()) "" else "Not supported by device",
                            isChecked = disableAudioDRC,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            disableAudioDrc = it
                                        )
                                    )
                                }
                            }, icon = R.drawable.music_equalizer,
                        )
                    }

                

                    if (search.matchesAny(R.string.equalizer)) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.equalizer),
                            selectedValue = equalizerType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            equalizerType = it
                                        )
                                    )
                                }

                                if (it == EqualizerType.System)
                                    internalEqualizer?.setEnabled(false)
                            },
                            valueText = { it.textName }, icon = R.drawable.equalizer,
                        )

                    }
                }
                }


                 // cast to complete in the future
                if (search.matchesAny(R.string.cast, R.string.enable_ritune_cast, R.string.ritune_cast_info, R.string.cast_type)) {
                settingsGroupHeader(title = context.resources.getString(R.string.cast))


                settingsGroupCard {

                    if (search.matchesAny(R.string.cast))
//                        SwitchSettingEntry(
//                            title = stringResource(R.string.enable_ritune_cast),
//                            text = stringResource(R.string.ritune_cast_info),
//                            isChecked = castToRiTuneDeviceEnabled,
//                            onCheckedChange = {
//                                castToRiTuneDeviceEnabled = it
//                            }
//, icon = R.drawable.cast_connected,                        )

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.cast_type),
                            selectedValue = castType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(castType = it)
                                    appSettingsManager.updateSettings(new)
                                }
                                restartActivity = true
                            },
                            valueText = { stringResource(it.title) }, icon = R.drawable.cast_disconnected,
                        )
                        RestartActivity(
                            restartActivity,
                            onRestart = { restartActivity = false })

                
                }
                }


                if (search.matchesAny(R.string.settings_automatic_playback, R.string.settings_bt_title_bluetooth_audio_devices, R.string.settings_bt_info_resume_playback_when_connected_pause_when_disconnected, R.string.settings_wired_title_wired_audio_devices, R.string.settings_wired_info_resume_playback_when_plugged_pause_when_unplugged)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_automatic_playback))


                settingsGroupCard {

                    if (search.matchesAny(R.string.settings_automatic_playback)) {
                        if (isAtLeastAndroid6) {
                            SwitchSettingEntry(
                                title = stringResource(R.string.settings_bt_title_bluetooth_audio_devices),
                                text = stringResource(R.string.settings_bt_info_resume_playback_when_connected_pause_when_disconnected),
                                isChecked = resumeOrPausePlaybackWhenDeviceBt,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appSettingsManager.activeSettings.value.copy(resumeOrPausePlaybackWhenDeviceBt = it)
                                        appSettingsManager.updateSettings(new)
                                    }

                                    restartService = true
                                }, icon = R.drawable.bluetooth,
                            )
                            SwitchSettingEntry(
                                title = stringResource(R.string.settings_wired_title_wired_audio_devices),
                                text = stringResource(R.string.settings_wired_info_resume_playback_when_plugged_pause_when_unplugged),
                                isChecked = resumeOrPausePlaybackWhenDeviceWired,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        val new =
                                            appSettingsManager.activeSettings.value.copy(resumeOrPausePlaybackWhenDeviceWired = it)
                                        appSettingsManager.updateSettings(new)
                                    }

                                    restartService = true
                                }, icon = R.drawable.headset,
                            )
                            RestartPlayerService(
                                restartService,
                                onRestart = { restartService = false })
                        }
                    }
                
                }
                }


                if (search.matchesAny(R.string.player_pause_listen_history, R.string.player_pause_listen_history_info, R.string.reset_playback_events, R.string.delete_playback_events, R.string.no_playback_events, R.string.reset_unplayable_tracks, R.string.clear_unplayable_tracks_summary, R.string.clear_unplayable_tracks, R.string.no_unplayable_tracks)) {
                settingsGroupHeader(title = context.resources.getString(R.string.playback_events))

                settingsGroupCard {


                    if (search.matchesAny(R.string.player_pause_listen_history)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_pause_listen_history),
                            text = stringResource(R.string.player_pause_listen_history_info),
                            isChecked = pauseListenHistory,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    appSettingsManager.updateSettings(
                                        appSettingsManager.activeSettings.value.copy(
                                            isPauseListenHistoryEnabled = it
                                        )
                                    )
                                }
                                restartService = true
                            }, icon = R.drawable.history,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })

                        SettingsEntry(
                            title = stringResource(R.string.reset_playback_events),
                            text = if (eventsCount > 0) {
                                stringResource(R.string.delete_playback_events, eventsCount)
                            } else {
                                stringResource(R.string.no_playback_events)
                            },
                            isEnabled = eventsCount > 0,
                            onClick = { clearEvents = true }, icon = R.drawable.refresh,
                        )

                        SettingsEntry(
                            title = stringResource(R.string.reset_unplayable_tracks),
                            titleSecondary = stringResource(R.string.clear_unplayable_tracks_summary),
                            text = if (unplayableTracksCount > 0) {
                                stringResource(R.string.clear_unplayable_tracks, unplayableTracksCount)
                            } else {
                                stringResource(R.string.no_unplayable_tracks)
                            },
                            isEnabled = unplayableTracksCount > 0,
                            onClick = { clearUnplayableTracks = true }, icon = R.drawable.alert_circle,
                        )
                    }
                
                }
                }


                if (search.matchesAny(R.string.pause_search_history, R.string.neither_save_new_searched_query, R.string.clear_search_history, R.string.delete, R.string.search_queries, R.string.history_is_empty)) {
                settingsGroupHeader(title = context.resources.getString(R.string.search_history))

                settingsGroupCard {

                    SwitchSettingEntry(
                        title = stringResource(R.string.pause_search_history),
                        text = stringResource(R.string.neither_save_new_searched_query),
                        isChecked = pauseSearchHistory,
                        onCheckedChange = {
                            coroutineScope.launch {
                                appSettingsManager.updateSettings(
                                    appSettingsManager.activeSettings.value.copy(
                                        pauseSearchHistory = it
                                    )
                                )
                            }
                            restartService = true
                        }, icon = R.drawable.pause,
                    )
                    RestartPlayerService(restartService, onRestart = { restartService = false } )

                    SettingsEntry(
                        title = stringResource(R.string.clear_search_history),
                        text = if (queriesCount > 0) {
                            "${stringResource(R.string.delete)} " + queriesCount + stringResource(R.string.search_queries)
                        } else {
                            stringResource(R.string.history_is_empty)
                        },
                        isEnabled = queriesCount > 0,
                        onClick = { Database.asyncTransaction( Database::clearQueries ) }, icon = R.drawable.trash,
                    )
                
                }
                }


                if (search.matchesAny(R.string.android_auto_1, R.string.enable_android_auto_support, R.string.aa_show_list_as_grid, R.string.aa_playlist_song_limit, R.string.aa_show_monthly_playlists, R.string.aa_info_show_monthly_playlists_in_playlists_screen, R.string.aa_show_podcast_playlists, R.string.aa_show_pinned_playlists, R.string.aa_show_in_library, R.string.aa_info_show_in_library_in_artists_and_albums_screen, R.string.aa_show_on_device, R.string.aa_info_show_on_device_in_artists_and_albums_screen, R.string.aa_show_top_songs, R.string.aa_info_show_top_playlist_in_playlists_screen, R.string.aa_show_all_songs, R.string.aa_info_show_favorites_playlists_in_playlists_screen, R.string.aa_show_shuffle_in_songs)) {
                settingsGroupHeader(title = context.resources.getString(R.string.android_auto))


                settingsGroupCard {

                    if (search.matchesAny(R.string.android_auto_1)) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.android_auto_1),
                            text = stringResource(R.string.enable_android_auto_support),
                            isChecked = isAndroidAutoEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(isAndroidAutoEnabled = it)
                                    appSettingsManager.updateSettings(new)

                                    //binder?.notifyAndroidAutoChildrenChanged(MediaLibraryServiceCallback.MediaId.ROOT)
                                    restartApp(context)
                                }
                            }, icon = R.drawable.ic_android_auto,
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                    AnimatedVisibility(visible = isAndroidAutoEnabled) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {

                            if (search.matchesAny(R.string.aa_show_list_as_grid)) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_list_as_grid),
                                    text = "",
                                    isChecked = showGridAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showGridAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.let { service ->
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.SONGS
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.ARTISTS_FAVORITES
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.ALBUMS_FAVORITES
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                                )
                                            }
                                        }
                                    }, icon = R.drawable.grid_view,
                                )
                            }

                            if (search.matchesAny(R.string.aa_playlist_song_limit)) {
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.aa_playlist_song_limit),
                                    selectedValue = androidAutoPlaylistLimit,
                                    onValueSelected = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(androidAutoPlaylistLimit = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                        binder?.let { service ->
                                            service.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.SONGS
                                            )
                                            service.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.ARTISTS_FAVORITES
                                            )
                                            service.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.ALBUMS_FAVORITES
                                            )
                                            service.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                            )
                                        }
                                    },
                                    valueText = { it.displayName }, icon = R.drawable.list,
                                )
                            }

                            if (search.matchesAny(R.string.aa_show_monthly_playlists))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_monthly_playlists),
                                    text = "", //stringResource(R.string.aa_info_show_monthly_playlists_in_playlists_screen),
                                    isChecked = showMonthlyPlaylistsAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showMonthlyPlaylistAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                            )
                                        }
                                    }, icon = R.drawable.calendar,
                                )

                            if (search.matchesAny(R.string.aa_show_podcast_playlists))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_podcast_playlists),
                                    text = "", //stringResource(R.string.aa_info_show_monthly_playlists_in_playlists_screen),
                                    isChecked = showPodcastAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showPodcastAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                            )
                                        }
                                    }, icon = R.drawable.podcast,
                                )

                            if (search.matchesAny(R.string.aa_show_pinned_playlists))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_pinned_playlists),
                                    text = "", //stringResource(R.string.aa_info_show_monthly_playlists_in_playlists_screen),
                                    isChecked = showPinnedAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showPinnedAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                            )
                                        }
                                    }, icon = R.drawable.pin,
                                )

                            if (search.matchesAny(R.string.aa_show_in_library))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_in_library),
                                    text = "", //stringResource(R.string.aa_info_show_in_library_in_artists_and_albums_screen),
                                    isChecked = showInLibraryAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showInLibraryAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.let { service ->
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.SONGS
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.ARTISTS_FAVORITES
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.ALBUMS_FAVORITES
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                                )
                                            }
                                        }
                                    }, icon = R.drawable.library,
                                )

                            if (search.matchesAny(R.string.aa_show_on_device))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_on_device),
                                    text = "", //stringResource(R.string.aa_info_show_on_device_in_artists_and_albums_screen),
                                    isChecked = showOnDeviceAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showOnDeviceAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.let { service ->
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.SONGS
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.ARTISTS_FAVORITES
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.ALBUMS_FAVORITES
                                                )
                                                service.notifyAndroidAutoChildrenChanged(
                                                    MediaLibraryServiceCallback.MediaId.PLAYLISTS
                                                )
                                            }
                                        }
                                    }, icon = R.drawable.folder,
                                )

                            if (search.matchesAny(R.string.aa_show_top_songs))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_top_songs),
                                    text = "", //stringResource(R.string.aa_info_show_top_playlist_in_playlists_screen),
                                    isChecked = showTopSongsAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showTopSongsAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.SONGS
                                            )
                                        }
                                    }, icon = R.drawable.trophy,
                                )

                            if (search.matchesAny(R.string.aa_show_all_songs))
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_all_songs),
                                    text = "", // stringResource(R.string.aa_info_show_favorites_playlists_in_playlists_screen),
                                    isChecked = showAllSongsAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showAllSongsAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.SONGS
                                            )
                                        }
                                    }, icon = R.drawable.musical_notes,
                                )

                            if (search.matchesAny(R.string.aa_show_shuffle_in_songs)) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_shuffle_in_songs),
                                    text = "",
                                    isChecked = showShuffleSongsAA,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showShuffleSongsAA = it)
                                            appSettingsManager.updateSettings(new)
                                            binder?.notifyAndroidAutoChildrenChanged(
                                                MediaLibraryServiceCallback.MediaId.SONGS
                                            )
                                        }
                                    }, icon = R.drawable.shuffle,
                                )
                            }

                        }
                    }

                


//            SettingsGroupSpacer(
//                modifier = Modifier.height(Dimensions.bottomSpacer)
//            )

            }
                }
        }
    }
        }
}
