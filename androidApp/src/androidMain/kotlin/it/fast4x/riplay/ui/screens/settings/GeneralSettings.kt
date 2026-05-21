package it.fast4x.riplay.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.enums.MusicAnimationType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PauseBetweenSongs
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.styling.DefaultDarkColorPalette
import it.fast4x.riplay.ui.styling.DefaultLightColorPalette
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.RestartActivity
import it.fast4x.riplay.utils.RestartPlayerService
import it.fast4x.riplay.extensions.preferences.PreferenceKey.AUTO_LOAD_SONGS_IN_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLOSE_WITH_BACK_BUTTON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLOSE_BACKGROUND_PLAYER
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
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_PICTURE_IN_PICTURE_AUTO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_PICTURE_IN_PICTURE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXCLUDE_SONGS_WITH_DURATION_LIMIT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXO_PLAYER_MIN_TIME_FOR_EVENT
import it.fast4x.riplay.utils.isAtLeastAndroid12
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_PAUSE_ON_VOLUME_ZERO_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.JUMP_PREVIOUS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LANGUAGE_APP
import it.fast4x.riplay.utils.languageDestinationName
import it.fast4x.riplay.extensions.preferences.PreferenceKey.LOUDNESS_BASE_GAIN
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MAX_SONGS_IN_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINIMUM_SILENCE_DURATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NAVIGATION_BAR_POSITION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NOW_PLAYING_INDICATOR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PAUSE_BETWEEN_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PAUSE_LISTEN_HISTORY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PERSISTENT_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PIP_MODULE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYBACK_FADE_AUDIO_DURATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYLIST_INDICATOR
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.RESUME_PLAYBACK_ON_START
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHAKE_EVENT_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SKIP_MEDIA_ON_ERROR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SKIP_SILENCE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VOLUME_NORMALIZATION
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.extensions.preferences.PreferenceKey.AUDIO_REVERB_PRESET
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BASSBOOST_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BASSBOOST_LEVEL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_DNS_OVER_HTTPS_SERVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DNS_OVER_HTTPS_TYPE
import it.fast4x.riplay.utils.getSystemlanguage
import it.fast4x.riplay.extensions.preferences.PreferenceKey.HANDLE_AUDIO_FOCUS_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_CONNECTION_METERED_ENABLED
import it.fast4x.riplay.utils.isIgnoringBatteryOptimizations
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_KEEP_SCREEN_ON_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.IS_PROXY_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PROXY_HOSTNAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PROXY_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PROXY_PORT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VOLUME_BOOST_LEVEL
import java.net.Proxy
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.enums.CheckUpdateState
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.EqualizerType
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAST_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CHECK_UPDATE_STATE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLOSE_PLAYER_SERVICE_AFTER_MINUTES
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_AUDIO_DRC
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_VOICE_INPUT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EQUALIZER_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXCLUDE_SONG_IF_IS_VIDEO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FILTER_CONTENT_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PARENTAL_CONTROL_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PAUSE_SEARCH_HISTORY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.RESUME_OR_PAUSE_PLAYBACK_WHEN_CALL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.RESUME_OR_PAUSE_PLAYBACK_WHEN_DEVICE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ALL_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_FAVORITES_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_GRID_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_IN_LIBRARY_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MONTHLY_PLAYLISTS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ON_DEVICE_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PINNED_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PODCAST_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SHUFFLE_SONGS_AA
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_SONGS_AA
import it.fast4x.riplay.extensions.updater.UpdateDialog
import it.fast4x.riplay.services.playback.PlayerMediaBrowserService
import it.fast4x.riplay.services.helpers.AudioDRCHelper
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem
import it.fast4x.riplay.utils.CheckForNewVersion
import it.fast4x.riplay.utils.LazyListContainer
import kotlinx.coroutines.flow.distinctUntilChanged


@ExperimentalAnimationApi
@UnstableApi
@Composable
fun GeneralSettings(
    navController: NavController
) {
    val binder = LocalPlayerServiceBinder.current

    val systemLocale = LocaleListCompat.getDefault().get(0).toString()
    var languageApp  by rememberPreference(LANGUAGE_APP.key, getSystemlanguage())

    var restartService by rememberSaveable { mutableStateOf(false) }
    var restartActivity by rememberSaveable { mutableStateOf(false) }

    var minTimeForEvent by rememberPreference(
        EXO_PLAYER_MIN_TIME_FOR_EVENT.key,
        MinTimeForEvent.`20s`
    )
    var persistentQueue by rememberPreference(PERSISTENT_QUEUE.key, true)
    var resumePlaybackOnStart by rememberPreference(RESUME_PLAYBACK_ON_START.key, false)
    var closebackgroundPlayer by rememberPreference(CLOSE_BACKGROUND_PLAYER.key, false)
    var closeBackgroundPlayerAfterMinutes by rememberPreference(
        CLOSE_PLAYER_SERVICE_AFTER_MINUTES.key,
        DurationInMinutes.Disabled
    )

//    var closePlayerWhenPausedAfterMinutes by rememberPreference(
//        closePlayerServiceWhenPausedAfterMinutesKey.key,
//        DurationInMinutes.Disabled
//    )

    var closeWithBackButton by rememberPreference(CLOSE_WITH_BACK_BUTTON.key, true)
    var resumeOrPausePlaybackWhenDevice by rememberPreference(
        RESUME_OR_PAUSE_PLAYBACK_WHEN_DEVICE.key,
        false
    )

    var resumeOrPausePlaybackWhenCall by rememberPreference(
        RESUME_OR_PAUSE_PLAYBACK_WHEN_CALL.key,
        false
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            resumeOrPausePlaybackWhenCall = true
            restartService = true
        } else {
            resumeOrPausePlaybackWhenCall = false
        }
    }

    var skipSilence by rememberPreference(SKIP_SILENCE.key, false)
    var skipMediaOnError by rememberPreference(SKIP_MEDIA_ON_ERROR.key, false)
    var volumeNormalization by rememberPreference(VOLUME_NORMALIZATION.key, false)
    var isConnectionMeteredEnabled by rememberPreference(IS_CONNECTION_METERED_ENABLED.key, true)

    var useDnsOverHttpsType by rememberPreference(DNS_OVER_HTTPS_TYPE.key, DnsOverHttpsType.None)


    var keepPlayerMinimized by rememberPreference(KEEP_PLAYER_MINIMIZED.key,   false)

    var disableClosingPlayerSwipingDown by rememberPreference(DISABLE_CLOSING_PLAYER_SWIPING_DOWN.key, false)

    val navigationBarPosition by rememberPreference(NAVIGATION_BAR_POSITION.key, NavigationBarPosition.Bottom)
    //var navigationBarType by rememberPreference(navigationBarTypeKey.key, NavigationBarType.IconAndText)
    var pauseBetweenSongs  by rememberPreference(PAUSE_BETWEEN_SONGS.key, PauseBetweenSongs.`0`)
    var maxSongsInQueue  by rememberPreference(MAX_SONGS_IN_QUEUE.key, MaxSongs.`500`)
    var filterContentType by rememberPreference(FILTER_CONTENT_TYPE.key, ContentType.All)

    val search = Search.init()

    var shakeEventEnabled by rememberPreference(SHAKE_EVENT_ENABLED.key, false)
    //var useVolumeKeysToChangeSong by rememberPreference(useVolumeKeysToChangeSongKey.key, false)

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
    var playbackFadeAudioDuration by rememberPreference(PLAYBACK_FADE_AUDIO_DURATION.key, DurationInMilliseconds.Disabled)
    var excludeSongWithDurationLimit by rememberPreference(EXCLUDE_SONGS_WITH_DURATION_LIMIT.key, DurationInMinutes.Disabled)
    var excludeSongsIfAreVideos by rememberPreference(EXCLUDE_SONG_IF_IS_VIDEO.key, false)
    var playlistindicator by rememberPreference(PLAYLIST_INDICATOR.key, false)
    var nowPlayingIndicator by rememberPreference(NOW_PLAYING_INDICATOR.key, MusicAnimationType.Bubbles)
    var discoverIsEnabled by rememberPreference(DISCOVER.key, false)
    var isPauseOnVolumeZeroEnabled by rememberPreference(IS_PAUSE_ON_VOLUME_ZERO_ENABLED.key, false)


//    val launchEqualizer by rememberEqualizerLauncher(audioSessionId = {
//        //binder?.player?.audioSessionId
//        0
//    })
    var equalizerType by rememberPreference(EQUALIZER_TYPE.key, EqualizerType.Internal)

    var minimumSilenceDuration by rememberPreference(MINIMUM_SILENCE_DURATION.key, 2_000_000L)

    var pauseListenHistory by rememberPreference(PAUSE_LISTEN_HISTORY.key, false)


    var loudnessBaseGain by rememberPreference(LOUDNESS_BASE_GAIN.key, 5.00f)
    var autoLoadSongsInQueue by rememberPreference(AUTO_LOAD_SONGS_IN_QUEUE.key, true)

    var bassboostEnabled by rememberPreference(BASSBOOST_ENABLED.key,false)
    var bassboostLevel by rememberPreference(BASSBOOST_LEVEL.key, 0.5f)
    var volumeBoostLevel by rememberPreference(VOLUME_BOOST_LEVEL.key, 0f)
    var audioReverb by rememberPreference(AUDIO_REVERB_PRESET.key,   PresetsReverb.NONE)
    var audioFocusEnabled by rememberPreference(HANDLE_AUDIO_FOCUS_ENABLED.key, true)

    var enablePictureInPicture by rememberPreference(ENABLE_PICTURE_IN_PICTURE.key, false)
    var enablePictureInPictureAuto by rememberPreference(ENABLE_PICTURE_IN_PICTURE_AUTO.key, false)
    var pipModule by rememberPreference(PIP_MODULE.key, PipModule.Cover)
    var jumpPrevious by rememberPreference(JUMP_PREVIOUS.key,"3")

    var isProxyEnabled by rememberPreference(IS_PROXY_ENABLED.key, false)
    var proxyHost by rememberPreference(PROXY_HOSTNAME.key, "")
    var proxyPort by rememberPreference(PROXY_PORT.key, 1080)
    var proxyMode by rememberPreference(PROXY_MODE.key, Proxy.Type.HTTP)
    var customDnsOverHttpsServer by rememberPreference(CUSTOM_DNS_OVER_HTTPS_SERVER.key, "")
    val context = LocalContext.current

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var showShuffleSongsAA by rememberPreference(SHOW_SHUFFLE_SONGS_AA.key, true)
    var showMonthlyPlaylistsAA by rememberPreference(SHOW_MONTHLY_PLAYLISTS_AA.key, true)
    var showInLibraryAA by rememberPreference(SHOW_IN_LIBRARY_AA.key, true)
    var showOnDeviceAA by rememberPreference(SHOW_ON_DEVICE_AA.key, true)
    var showFavoritesSongsAA by rememberPreference(SHOW_FAVORITES_SONGS_AA.key, true)
    var showTopSongsAA by rememberPreference(SHOW_TOP_SONGS_AA.key, true)
    var showAllSongsAA by rememberPreference(SHOW_ALL_SONGS_AA.key, true)
    var showPodcastAA by rememberPreference(SHOW_PODCAST_AA.key, true)
    var showPinnedAA by rememberPreference(SHOW_PINNED_AA.key, true)
    var showGridAA by rememberPreference(SHOW_GRID_AA.key, true)

    var isEnabledVoiceInput by rememberPreference(
        ENABLE_VOICE_INPUT.key,
        true
    )

    //var checkVolumeLevel by rememberPreference(checkVolumeLevelKey.key, true)
    var parentalControlEnabled by rememberPreference(PARENTAL_CONTROL_ENABLED.key, false)

    //var castToRiTuneDeviceEnabled by rememberPreference(castToRiTuneDeviceEnabledKey.key, false )

    var castType by rememberPreference(CAST_TYPE.key, CastType.RITUNECAST)

    var disableAudioDRC by rememberPreference(DISABLE_AUDIO_DRC.key, false)


    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)
    var clearEvents by remember { mutableStateOf(false) }
    if (clearEvents) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_all_playback_events),
            onDismiss = { clearEvents = false },
            onConfirm = { Database.asyncTransaction( Database::clearEvents ) }
        )
    }

    var pauseSearchHistory by rememberPreference(PAUSE_SEARCH_HISTORY.key, false)

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    var checkUpdateState by rememberPreference(CHECK_UPDATE_STATE.key, CheckUpdateState.Enabled)

    val internalEqualizer = LocalPlayerServiceBinder.current?.equalizer

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
            //.verticalScroll(rememberScrollState())
            /*
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
             */
    ) {

//
//        if (resetCustomLightThemeDialog) {
//            ConfirmationDialog(
//                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_light_theme_colors),
//                onDismiss = { resetCustomLightThemeDialog = false },
//                onConfirm = {
//                    resetCustomLightThemeDialog = false
//                    customThemeLight_Background0 = DefaultLightColorPalette.background0.hashCode()
//                    customThemeLight_Background1 = DefaultLightColorPalette.background1.hashCode()
//                    customThemeLight_Background2 = DefaultLightColorPalette.background2.hashCode()
//                    customThemeLight_Background3 = DefaultLightColorPalette.background3.hashCode()
//                    customThemeLight_Background4 = DefaultLightColorPalette.background4.hashCode()
//                    customThemeLight_Text = DefaultLightColorPalette.text.hashCode()
//                    customThemeLight_TextSecondary =
//                        DefaultLightColorPalette.textSecondary.hashCode()
//                    customThemeLight_TextDisabled = DefaultLightColorPalette.textDisabled.hashCode()
//                    customThemeLight_IconButtonPlayer =
//                        DefaultLightColorPalette.iconButtonPlayer.hashCode()
//                    customThemeLight_Accent = DefaultLightColorPalette.accent.hashCode()
//                }
//            )
//        }
//
//        if (resetCustomDarkThemeDialog) {
//            ConfirmationDialog(
//                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_dark_theme_colors),
//                onDismiss = { resetCustomDarkThemeDialog = false },
//                onConfirm = {
//                    resetCustomDarkThemeDialog = false
//                    customThemeDark_Background0 = DefaultDarkColorPalette.background0.hashCode()
//                    customThemeDark_Background1 = DefaultDarkColorPalette.background1.hashCode()
//                    customThemeDark_Background2 = DefaultDarkColorPalette.background2.hashCode()
//                    customThemeDark_Background3 = DefaultDarkColorPalette.background3.hashCode()
//                    customThemeDark_Background4 = DefaultDarkColorPalette.background4.hashCode()
//                    customThemeDark_Text = DefaultDarkColorPalette.text.hashCode()
//                    customThemeDark_TextSecondary = DefaultDarkColorPalette.textSecondary.hashCode()
//                    customThemeDark_TextDisabled = DefaultDarkColorPalette.textDisabled.hashCode()
//                    customThemeDark_IconButtonPlayer =
//                        DefaultDarkColorPalette.iconButtonPlayer.hashCode()
//                    customThemeDark_Accent = DefaultDarkColorPalette.accent.hashCode()
//                }
//            )
//        }

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
                        title = stringResource(R.string.tab_general),
                        iconId = R.drawable.app_icon,
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

                if (BuildConfig.FLAVOR == "full") {
                    settingsItem(
                        isHeader = true
                    ) {
                        SettingsEntryGroupText(title = stringResource(R.string.check_update))
                    }

                    settingsItem {
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
                            onValueSelected = { checkUpdateState = it },
                            valueText = {
                                when (it) {
                                    CheckUpdateState.Disabled -> stringResource(R.string.vt_disabled)
                                    CheckUpdateState.Enabled -> stringResource(R.string.enabled)
                                    CheckUpdateState.OnlyCheck -> stringResource(R.string.only_check_update)
                                    //CheckUpdateState.Ask -> stringResource(R.string.ask)
                                }

                            }
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


                settingsItem(
                    isHeader = true
                ) {
                    SettingsEntryGroupText(title = stringResource(R.string.languages))
                }

                settingsItem {
                    SettingsDescription(text = stringResource(R.string.system_language) + ": $systemLocale")

                    if (search.input.isBlank() || stringResource(R.string.app_language).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.app_language),
                            selectedValue = languageApp,
                            onValueSelected = { languageApp = it },
                            valueText = {
                                languageDestinationName(it)
                            }
                        )
                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsEntryGroupText(title = stringResource(R.string.title_network))
                }


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
//            )

                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.use_alternative_dns).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.use_dns_over_https_title),
                            selectedValue = useDnsOverHttpsType,
                            onValueSelected = {
                                useDnsOverHttpsType = it
                                restartActivity = true
                            },
                            valueText = { it.textName }
                        )

                        AnimatedVisibility(visible = useDnsOverHttpsType == DnsOverHttpsType.Custom) {
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.custom_dns_over_https_server),
                                    text = customDnsOverHttpsServer,
                                    currentText = customDnsOverHttpsServer,
                                    onTextSave = {
                                        customDnsOverHttpsServer = it
                                        restartActivity = true
                                    },
                                    validationType = ValidationType.Url
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

                    if (search.input.isBlank() || stringResource(R.string.enable_proxy).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_proxy),
                            text = "",
                            isChecked = isProxyEnabled,
                            onCheckedChange = { isProxyEnabled = it }
                        )
                        SettingsDescription(text = stringResource(R.string.restarting_riplay_is_required))

                        AnimatedVisibility(visible = isProxyEnabled) {
                            Column {
                                EnumValueSelectorSettingsEntry(
                                    title = stringResource(R.string.proxy_mode),
                                    selectedValue = proxyMode,
                                    onValueSelected = { proxyMode = it },
                                    valueText = { it.name }
                                )
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.proxy_host),
                                    text = proxyHost,
                                    currentText = proxyHost,
                                    onTextSave = { proxyHost = it },
                                    validationType = ValidationType.Ip
                                )
                                TextDialogSettingEntry(
                                    title = stringResource(R.string.proxy_port),
                                    text = proxyPort.toString(),
                                    currentText = proxyPort.toString(),
                                    onTextSave = { proxyPort = it.toIntOrNull() ?: 1080 })
                            }
                        }
                    }

                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.service_lifetime))
                }

                settingsItem {
                    val context = LocalContext.current
                    var isKeepScreenOnEnabled by rememberPreference(IS_KEEP_SCREEN_ON_ENABLED.key, false)
                    var isIgnoringBatteryOptimizations by remember {
                        mutableStateOf(context.isIgnoringBatteryOptimizations())
                    }
                    val activityResultLauncher =
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations()
                        }

                    if (search.input.isBlank() || stringResource(R.string.keep_screen_on).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.keep_screen_on),
                            text = stringResource(R.string.prevents_screen_timeout),
                            isChecked = isKeepScreenOnEnabled,
                            onCheckedChange = { isKeepScreenOnEnabled = it }
                        )
                    }
                    if (search.input.isBlank() || stringResource(R.string.ignore_battery_optimizations).contains(
                            search.input,
                            true
                        )
                    ) {
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
                            }
                        )
                    }


                    if (search.input.isBlank() || stringResource(R.string.enable_voice_input).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.enable_voice_input),
                            text = stringResource(R.string.require_mic_permission),
                            isChecked = isEnabledVoiceInput,
                            onCheckedChange = {
                                isEnabledVoiceInput = it
                            }
                        )
                    }

                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(stringResource(R.string.self_closing))
                }

                settingsItem {

                    if (search.input.isBlank() || stringResource(R.string.close_background_player).contains(
                            search.input,
                            true
                        )
                    ) {

                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.when_app_swipe_out_from_task_manager),
                                selectedValue = closeBackgroundPlayerAfterMinutes,
                                onValueSelected = { closeBackgroundPlayerAfterMinutes = it },
                                valueText = {
                                    when (it) {
                                        DurationInMinutes.Disabled -> stringResource(R.string.vt_disabled)
                                        DurationInMinutes.`0` -> "0m"
                                        DurationInMinutes.`1` -> "1m"
                                        DurationInMinutes.`3` -> "3m"
                                        DurationInMinutes.`5` -> "5m"
                                        DurationInMinutes.`10` -> "10m"
                                        DurationInMinutes.`15` -> "15m"
                                        DurationInMinutes.`20` -> "20m"
                                        DurationInMinutes.`25` -> "25m"
                                        DurationInMinutes.`30` -> "30m"
                                        DurationInMinutes.`60` -> "60m"
                                        DurationInMinutes.`90` -> "90m"
                                        DurationInMinutes.`120` -> "120m"
                                        DurationInMinutes.`150` -> "150m"
                                        DurationInMinutes.`180` -> "180m"

                                    }
                                }
                            )


                    }
                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(stringResource(R.string.player))
                }

                settingsItem {

                    if (search.input.isBlank() || stringResource(R.string.jump_previous).contains(
                            search.input,
                            true
                        )
                    ) {
                        SettingsEntryGroup() {
                            BasicText(
                                text = stringResource(R.string.jump_previous),
                                style = typography().xs.semiBold.copy(color = colorPalette().text),
                            )
                            BasicText(
                                text = stringResource(R.string.jump_previous_blank),
                                style = typography().xxs.semiBold.copy(color = colorPalette().textDisabled),
                            )
                            TextField(
                                value = jumpPrevious,
                                onValueChange = {
                                    if (it.isDigitsOnly())
                                        jumpPrevious = it
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = TextFieldDefaults.textFieldColors(
                                    textColor = colorPalette().text,
                                    unfocusedIndicatorColor = colorPalette().text
                                ),
                            )
                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.min_listening_time).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.min_listening_time),
                            selectedValue = minTimeForEvent,
                            onValueSelected = { minTimeForEvent = it },
                            valueText = {
                                when (it) {
                                    MinTimeForEvent.`10s` -> "10s"
                                    MinTimeForEvent.`15s` -> "15s"
                                    MinTimeForEvent.`20s` -> "20s"
                                    MinTimeForEvent.`30s` -> "30s"
                                    MinTimeForEvent.`40s` -> "40s"
                                    MinTimeForEvent.`60s` -> "60s"
                                }
                            }
                        )
                        SettingsDescription(text = stringResource(R.string.is_min_list_time_for_tips_or_quick_pics))
                    }

                    if (search.input.isBlank() || stringResource(R.string.min_listening_time).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.exclude_songs_with_duration_limit),
                            selectedValue = excludeSongWithDurationLimit,
                            onValueSelected = {
                                excludeSongWithDurationLimit = if (it == DurationInMinutes.`0`) DurationInMinutes.`1` else it
                            },
                            valueText = {
                                when (it) {
                                    DurationInMinutes.Disabled -> stringResource(R.string.vt_disabled)
                                    DurationInMinutes.`0` -> "0m"
                                    DurationInMinutes.`1` -> "1m"
                                    DurationInMinutes.`3` -> "3m"
                                    DurationInMinutes.`5` -> "5m"
                                    DurationInMinutes.`10` -> "10m"
                                    DurationInMinutes.`15` -> "15m"
                                    DurationInMinutes.`20` -> "20m"
                                    DurationInMinutes.`25` -> "25m"
                                    DurationInMinutes.`30` -> "30m"
                                    DurationInMinutes.`60` -> "60m"
                                    DurationInMinutes.`90` -> "90m"
                                    DurationInMinutes.`120` -> "120m"
                                    DurationInMinutes.`150` -> "150m"
                                    DurationInMinutes.`180` -> "180m"
                                }
                            }
                        )
                        SettingsDescription(text = stringResource(R.string.exclude_songs_with_duration_limit_description))
                    }

                    if (search.input.isBlank() || stringResource(R.string.exclude_song_if_is_video).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.exclude_song_if_is_video),
                            text = "",
                            isChecked = excludeSongsIfAreVideos,
                            onCheckedChange = {
                                excludeSongsIfAreVideos = it
                                restartService = true
                            }
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.input.isBlank() || stringResource(R.string.pause_between_songs).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.pause_between_songs),
                            selectedValue = pauseBetweenSongs,
                            onValueSelected = { pauseBetweenSongs = it },
                            valueText = {
                                when (it) {
                                    PauseBetweenSongs.`0` -> "0s"
                                    PauseBetweenSongs.`5` -> "5s"
                                    PauseBetweenSongs.`10` -> "10s"
                                    PauseBetweenSongs.`15` -> "15s"
                                    PauseBetweenSongs.`20` -> "20s"
                                    PauseBetweenSongs.`30` -> "30s"
                                    PauseBetweenSongs.`40` -> "40s"
                                    PauseBetweenSongs.`50` -> "50s"
                                    PauseBetweenSongs.`60` -> "60s"
                                }
                            }
                        )


                    if (search.input.isBlank() || stringResource(R.string.player_pause_on_volume_zero).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_pause_on_volume_zero),
                            text = stringResource(R.string.info_pauses_player_when_volume_zero),
                            isChecked = isPauseOnVolumeZeroEnabled,
                            onCheckedChange = {
                                isPauseOnVolumeZeroEnabled = it
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.effect_fade_audio).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.effect_fade_audio),
                            selectedValue = playbackFadeAudioDuration,
                            onValueSelected = { playbackFadeAudioDuration = it },
                            valueText = {
                                when (it) {
                                    DurationInMilliseconds.Disabled -> stringResource(R.string.vt_disabled)
                                    else -> {
                                        it.toString()
                                    }
                                }
                            }
                        )
                        SettingsDescription(text = stringResource(R.string.effect_fade_audio_description))
                    }


                    if (search.input.isBlank() || stringResource(R.string.player_keep_minimized).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_keep_minimized),
                            text = stringResource(R.string.when_click_on_a_song_player_start_minimized),
                            isChecked = keepPlayerMinimized,
                            onCheckedChange = {
                                keepPlayerMinimized = it
                            }
                        )


                    if (search.input.isBlank() || stringResource(R.string.player_collapsed_disable_swiping_down).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_collapsed_disable_swiping_down),
                            text = stringResource(R.string.avoid_closing_the_player_cleaning_queue_by_swiping_down),
                            isChecked = disableClosingPlayerSwipingDown,
                            onCheckedChange = {
                                disableClosingPlayerSwipingDown = it
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.player_auto_load_songs_in_queue).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_auto_load_songs_in_queue),
                            text = stringResource(R.string.player_auto_load_songs_in_queue_description),
                            isChecked = autoLoadSongsInQueue,
                            onCheckedChange = {
                                autoLoadSongsInQueue = it
                                restartService = true
                            }
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.input.isBlank() || stringResource(R.string.max_songs_in_queue).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.max_songs_in_queue),
                            selectedValue = maxSongsInQueue,
                            onValueSelected = { maxSongsInQueue = it },
                            valueText = {
                                when (it) {
                                    MaxSongs.Unlimited -> stringResource(R.string.unlimited)
                                    MaxSongs.`50` -> MaxSongs.`50`.name
                                    MaxSongs.`100` -> MaxSongs.`100`.name
                                    MaxSongs.`200` -> MaxSongs.`200`.name
                                    MaxSongs.`300` -> MaxSongs.`300`.name
                                    MaxSongs.`500` -> MaxSongs.`500`.name
                                    MaxSongs.`1000` -> MaxSongs.`1000`.name
                                    MaxSongs.`2000` -> MaxSongs.`2000`.name
                                    MaxSongs.`3000` -> MaxSongs.`3000`.name
                                }
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.filter_content_type).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.filter_content_type),
                            selectedValue = filterContentType,
                            onValueSelected = { filterContentType = it },
                            valueText = {
                                it.textName
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.discover).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.discover),
                            text = stringResource(R.string.discoverinfo),
                            isChecked = discoverIsEnabled,
                            onCheckedChange = { discoverIsEnabled = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.playlistindicator).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.playlistindicator),
                            text = stringResource(R.string.playlistindicatorinfo),
                            isChecked = playlistindicator,
                            onCheckedChange = {
                                playlistindicator = it
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.now_playing_indicator).contains(
                            search.input,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.now_playing_indicator),
                            selectedValue = nowPlayingIndicator,
                            onValueSelected = { nowPlayingIndicator = it },
                            valueText = {
                                it.textName
                            }
                        )

                    if (search.input.isBlank() || stringResource(R.string.persistent_queue).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.persistent_queue),
                            text = stringResource(R.string.save_and_restore_playing_songs),
                            isChecked = persistentQueue,
                            onCheckedChange = {
                                persistentQueue = it
                                if(it) binder?.loadQueue() // try to load last known queue now
                                //restartService = true
                            }
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
                                        resumePlaybackOnStart = it
                                        //restartService = true
                                    }
                                )
                                RestartPlayerService(
                                    restartService,
                                    onRestart = { restartService = false })
                            }
                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.close_app_with_back_button).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            isEnabled = Build.VERSION.SDK_INT >= 33,
                            title = stringResource(R.string.close_app_with_back_button),
                            text = stringResource(R.string.when_you_use_the_back_button_from_the_home_page),
                            isChecked = closeWithBackButton,
                            onCheckedChange = {
                                closeWithBackButton = it
                                restartActivity = true
                            }
                        )
                        RestartActivity(restartActivity, onRestart = { restartActivity = false })
                    }

                    if (search.input.isBlank() || stringResource(R.string.skip_media_on_error).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.skip_media_on_error),
                            text = stringResource(R.string.skip_media_on_error_description),
                            isChecked = skipMediaOnError,
                            onCheckedChange = {
                                skipMediaOnError = it
                                restartService = true
                            }
                        )

                        RestartPlayerService(restartService, onRestart = { restartService = false })

                    }

                    if (search.input.isBlank() || stringResource(R.string.skip_silence).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.skip_silence),
                            text = stringResource(R.string.skip_silent_parts_during_playback),
                            isChecked = skipSilence,
                            onCheckedChange = {
                                skipSilence = it
                            }
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
                                        minimumSilenceDuration = newValue.toLong() * 1000L
                                        restartService = true
                                    },
                                    toDisplay = { stringResource(R.string.format_ms, it.toLong()) },
                                    range = 1.00f..2000.000f
                                )

                                RestartPlayerService(
                                    restartService,
                                    onRestart = { restartService = false })
                            }
                        }

                    }

                    if (search.input.isBlank() || stringResource(R.string.parental_control).contains(
                            search.input,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.parental_control),
                            text = stringResource(R.string.info_prevent_play_songs_with_age_limitation),
                            isChecked = parentalControlEnabled,
                            onCheckedChange = { parentalControlEnabled = it }
                        )

                    if (search.input.isBlank() || stringResource(R.string.event_shake).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.event_shake),
                            text = stringResource(R.string.shake_to_change_song),
                            isChecked = shakeEventEnabled,
                            onCheckedChange = {
                                shakeEventEnabled = it
                                restartService = true
                            }
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.input.isBlank() || stringResource(R.string.settings_enable_pip).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_enable_pip),
                            text = "",
                            isChecked = enablePictureInPicture,
                            onCheckedChange = {
                                enablePictureInPicture = it
                                restartActivity = true
                            }
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
                                        pipModule = it
                                        restartActivity = true
                                    },
                                    valueText = {
                                        when (it) {
                                            PipModule.Cover -> stringResource(R.string.pipmodule_cover)
                                        }
                                    }
                                )

                                SwitchSettingEntry(
                                    isEnabled = isAtLeastAndroid12,
                                    title = stringResource(R.string.settings_enable_pip_auto),
                                    text = stringResource(R.string.pip_info_from_android_12_pip_can_be_automatically_enabled),
                                    isChecked = enablePictureInPictureAuto,
                                    onCheckedChange = {
                                        enablePictureInPictureAuto = it
                                        restartActivity = true
                                    }
                                )
                                RestartActivity(
                                    restartActivity,
                                    onRestart = { restartActivity = false })
                            }

                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.equalizer).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.equalizer),
                            selectedValue = equalizerType,
                            onValueSelected = {
                                equalizerType = it

                                if (it == EqualizerType.System)
                                    internalEqualizer?.setEnabled(false)
                            },
                            valueText = { it.textName }
                        )

                    }

                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.audio))
                }

                settingsItem {

                    if (search.input.isBlank() || stringResource(R.string.loudness_normalization).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.loudness_normalization),
                            text = stringResource(R.string.autoadjust_the_volume),
                            isChecked = volumeNormalization,
                            onCheckedChange = {
                                volumeNormalization = it
                            }
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
                                        loudnessBaseGain = newValue
                                    },
                                    toDisplay = {
                                        "%.1f dB".format(loudnessBaseGain).replace(",", ".")
                                    },
                                    range = -20f..20f
                                )

                                SliderSettingsEntry(
                                    title = stringResource(R.string.loudness_boost_level),
                                    text = stringResource(R.string.loudness_boost_level_info),
                                    state = newValueVolume,
                                    onSlide = { newValueVolume = it },
                                    onSlideComplete = {
                                        volumeBoostLevel = newValueVolume
                                    },
                                    toDisplay = {
                                        "%.2f dB".format(volumeBoostLevel).replace(",", ".")
                                    },
                                    range = -20f..20f
                                )
                            }
                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.settings_audio_bass_boost).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_audio_bass_boost),
                            text = "",
                            isChecked = bassboostEnabled,
                            onCheckedChange = {
                                bassboostEnabled = it
                            }
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
                                        bassboostLevel = newValue
                                    },
                                    toDisplay = { "%.1f".format(bassboostLevel).replace(",", ".") },
                                    range = 0f..1f
                                )
                            }
                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.settings_audio_reverb).contains(
                            search.input,
                            true
                        )
                    ) {
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.settings_audio_reverb),
                            text = stringResource(R.string.settings_audio_reverb_info_apply_a_depth_effect_to_the_audio),
                            selectedValue = audioReverb,
                            onValueSelected = {
                                audioReverb = it
                                restartService = true
                            },
                            valueText = {
                                it.textName
                            }
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }

                    if (search.input.isBlank() || stringResource(R.string.settings_audio_focus).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.settings_audio_focus),
                            text = stringResource(R.string.settings_audio_focus_info),
                            isChecked = audioFocusEnabled,
                            onCheckedChange = {
                                audioFocusEnabled = it
                            }
                        )
                    }

                    if (search.input.isBlank() || stringResource(R.string.settings_disable_audio_drc).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            isEnabled = AudioDRCHelper.hasDRCSupport(),
                            title = stringResource(R.string.settings_disable_audio_drc),
                            text = if (AudioDRCHelper.hasDRCSupport()) "" else "Not supported by device",
                            isChecked = disableAudioDRC,
                            onCheckedChange = {
                                disableAudioDRC = it
                            }
                        )
                    }

                }

                 // cast to complete in the future
                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.cast))
                }

                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.cast).contains(
                            search.input,
                            true
                        )
                    )
//                        SwitchSettingEntry(
//                            title = stringResource(R.string.enable_ritune_cast),
//                            text = stringResource(R.string.ritune_cast_info),
//                            isChecked = castToRiTuneDeviceEnabled,
//                            onCheckedChange = {
//                                castToRiTuneDeviceEnabled = it
//                            }
//                        )

                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.cast_type),
                        selectedValue = castType,
                        onValueSelected = {
                            castType = it
                            restartActivity = true
                        },
                        valueText = {
                            context.resources.getString(it.title)
                        }
                    )
                    RestartActivity(
                        restartActivity,
                        onRestart = { restartActivity = false })

                }


                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.playback_events))
                }
                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.resume_or_pause_playback).contains(
                            search.input,
                            true
                        )
                    ) {
                        if (isAtLeastAndroid6) {
                            SwitchSettingEntry(
                                title = stringResource(R.string.play_or_pause_when_device_is_connected_or_disconnected),
                                text = "", //stringResource(R.string.resume_or_pause_playback),
                                isChecked = resumeOrPausePlaybackWhenDevice,
                                onCheckedChange = {
                                    resumeOrPausePlaybackWhenDevice = it
                                    restartService = true
                                }
                            )
                            RestartPlayerService(
                                restartService,
                                onRestart = { restartService = false })
                        }
                    }

                    if (search.input.isBlank() || stringResource(R.string.player_pause_listen_history).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.player_pause_listen_history),
                            text = stringResource(R.string.player_pause_listen_history_info),
                            isChecked = pauseListenHistory,
                            onCheckedChange = {
                                pauseListenHistory = it
                                restartService = true
                            }
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
                            onClick = { clearEvents = true }
                        )
                    }
                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.search_history))
                }
                settingsItem {
                    SwitchSettingEntry(
                        title = stringResource(R.string.pause_search_history),
                        text = stringResource(R.string.neither_save_new_searched_query),
                        isChecked = pauseSearchHistory,
                        onCheckedChange = {
                            pauseSearchHistory = it
                            restartService = true
                        }
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
                        onClick = { Database.asyncTransaction( Database::clearQueries ) }
                    )
                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsGroupSpacer()
                    SettingsEntryGroupText(title = stringResource(R.string.android_auto))
                }

                settingsItem {
                    if (search.input.isBlank() || stringResource(R.string.android_auto_1).contains(
                            search.input,
                            true
                        )
                    ) {
                        SwitchSettingEntry(
                            title = stringResource(R.string.android_auto_1),
                            text = stringResource(R.string.enable_android_auto_support),
                            isChecked = isAndroidAutoEnabled,
                            onCheckedChange = {
                                isAndroidAutoEnabled = it
                                restartService = true
                            }
                        )
                        RestartPlayerService(restartService, onRestart = { restartService = false })
                    }
                    AnimatedVisibility(visible = isAndroidAutoEnabled) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {

                            if (search.input.isBlank() || stringResource(R.string.aa_show_list_as_grid).contains(
                                    search.input,
                                    true
                                )
                            ) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_list_as_grid),
                                    text = "",
                                    isChecked = showGridAA,
                                    onCheckedChange = {
                                        showGridAA = it
                                    }
                                )
                            }

                            if (search.input.isBlank() || stringResource(R.string.aa_show_monthly_playlists).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_monthly_playlists),
                                    text = "", //stringResource(R.string.aa_info_show_monthly_playlists_in_playlists_screen),
                                    isChecked = showMonthlyPlaylistsAA,
                                    onCheckedChange = { showMonthlyPlaylistsAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_podcast_playlists).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_podcast_playlists),
                                    text = "", //stringResource(R.string.aa_info_show_monthly_playlists_in_playlists_screen),
                                    isChecked = showPodcastAA,
                                    onCheckedChange = { showPodcastAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_pinned_playlists).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_pinned_playlists),
                                    text = "", //stringResource(R.string.aa_info_show_monthly_playlists_in_playlists_screen),
                                    isChecked = showPinnedAA,
                                    onCheckedChange = { showPinnedAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_in_library).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_in_library),
                                    text = "", //stringResource(R.string.aa_info_show_in_library_in_artists_and_albums_screen),
                                    isChecked = showInLibraryAA,
                                    onCheckedChange = { showInLibraryAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_on_device).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_on_device),
                                    text = "", //stringResource(R.string.aa_info_show_on_device_in_artists_and_albums_screen),
                                    isChecked = showOnDeviceAA,
                                    onCheckedChange = { showOnDeviceAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_top_songs).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_top_songs),
                                    text = "", //stringResource(R.string.aa_info_show_top_playlist_in_playlists_screen),
                                    isChecked = showTopSongsAA,
                                    onCheckedChange = { showTopSongsAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_all_songs).contains(
                                    search.input,
                                    true
                                )
                            )
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_all_songs),
                                    text = "", // stringResource(R.string.aa_info_show_favorites_playlists_in_playlists_screen),
                                    isChecked = showAllSongsAA,
                                    onCheckedChange = { showAllSongsAA = it }
                                )

                            if (search.input.isBlank() || stringResource(R.string.aa_show_shuffle_in_songs).contains(
                                    search.input,
                                    true
                                )
                            ) {
                                SwitchSettingEntry(
                                    title = stringResource(R.string.aa_show_shuffle_in_songs),
                                    text = "",
                                    isChecked = showShuffleSongsAA,
                                    onCheckedChange = {
                                        showShuffleSongsAA = it
                                    }
                                )
                            }

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
