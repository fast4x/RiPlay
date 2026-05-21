package it.fast4x.riplay.extensions.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import it.fast4x.environment.Environment
import it.fast4x.environment.requests.HomePage
import it.fast4x.riplay.cast.ritune.models.RiTuneDevices
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.SongSortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber


val Context.preferences: SharedPreferences
    get() = getSharedPreferences("preferences", Context.MODE_PRIVATE)


//lastPlayerThumbnailSizeKey = "lastPlayerThumbnailSize"
//const val lastPlayerPlayButtonTypeKey = "lastPlayerPlayButtonType"
//const val lastPlayerTimelineTypeKey = "lastPlayerTimelineType"
//const val lastPlayerVisualizerTypeKey = "lastPlayerVisualizerType"
//const val playerPlayButtonTypeKey = "playerPlayButtonType"
//const val playerTimelineTypeKey = "playerTimelineType"
//const val playerVisualizerTypeKey = "playerVisualizerType"
//const val currentVisualizerKey = "currentVisualizer"
//const val visualizerEnabledKey = "visualizerEnabled"
//const val thumbnailTapEnabledKey = "thumbnailTapEnabled"
//const val wavedPlayerTimelineKey = "wavedPlayerTimeline"
//const val languageAppKey = "languageApp"
//const val otherLanguageAppKey = "otherLanguageApp"
//const val indexNavigationTabKey = "indexNavigationTab"
//const val effectRotationKey = "effectRotation"
//const val playerThumbnailSizeKey = "playerThumbnailSize"
//const val playerThumbnailSizeLKey = "playerThumbnailSizeL"
//const val playerTimelineSizeKey = "playerTimelineSize"
//const val colorPaletteNameKey = "colorPaletteName"
//const val colorPaletteModeKey = "colorPaletteMode"
//const val thumbnailRoundnessKey = "thumbnailRoundness"
//const val coilDiskCacheMaxSizeKey = "coilDiskCacheMaxSize"
//const val exoPlayerDiskCacheMaxSizeKey = "exoPlayerDiskCacheMaxSize"
//const val exoPlayerDiskDownloadCacheMaxSizeKey = "exoPlayerDiskDownloadCacheMaxSize"
//const val exoPlayerMinTimeForEventKey = "exoPlayerMinTimeForEvent"
//const val exoPlayerAlternateCacheLocationKey = "exoPlayerAlternateCacheLocation"
//const val exoPlayerCacheLocationKey = "exoPlayerCacheLocationKey"
//const val isInvincibilityEnabledKey = "isInvincibilityEnabled"
//const val useSystemFontKey = "useSystemFont"
//const val applyFontPaddingKey = "applyFontPadding"
//const val songSortOrderKey = "songSortOrder"
//const val songSortByKey = "songSortBy"
//const val onDeviceSongSortByKey = "onDeviceSongSortBy"
//const val onDeviceFolderSortByKey = "onDeviceFolderSortBy"
//const val playlistSortOrderKey = "playlistSortOrder"
//const val playlistSortByKey = "playlistSortBy"
//const val albumSortOrderKey = "albumSortOrder"
//const val albumSortByKey = "albumSortBy"
//const val artistSortOrderKey = "artistSortOrder"
//const val artistSortByKey = "artistSortBy"
//const val queueLoopTypeKey = "queueLoopType"
//const val reorderInQueueEnabledKey = "reorderInQueueEnabled"
//const val skipSilenceKey = "skipSilence"
//const val skipMediaOnErrorKey = "skipMediaOnError"
//const val volumeNormalizationKey = "volumeNormalization"
//const val persistentQueueKey = "persistentQueue"
//const val resumePlaybackOnStartKey = "resumePlaybackOnStart"
//const val closebackgroundPlayerKey = "closebackgroundPlayer"
//const val closeWithBackButtonKey = "closeWithBackButton"
//const val isShowingSynchronizedLyricsKey = "isShowingSynchronizedLyrics"
//const val isShowingThumbnailInLockscreenKey = "isShowingThumbnailInLockscreen"
//const val homeScreenTabIndexKey = "homeScreenTabIndex"
//const val searchResultScreenTabIndexKey = "searchResultScreenTabIndex"
//const val artistScreenTabIndexKey = "artistScreenTabIndex"
//const val pauseSearchHistoryKey = "pauseSearchHistory"
//const val UiTypeKey = "UiType"
//const val disablePlayerHorizontalSwipeKey = "disablePlayerHorizontalSwipe"
//const val disableIconButtonOnTopKey = "disableIconButtonOnTop"
//const val exoPlayerCustomCacheKey = "exoPlayerCustomCache"
//const val coilCustomDiskCacheKey = "exoPlayerCustomCache"
//const val disableScrollingTextKey = "disableScrollingText"
//const val audioQualityFormatKey = "audioQualityFormat"
//const val showLikeButtonBackgroundPlayerKey = "showLikeButtonBackgroundPlayer"
//const val playEventsTypeKey = "playEventsType"
//const val fontTypeKey = "fontType"
//const val playlistSongSortByKey = "playlistSongSortBy"
//const val showTipsKey = "showTips"
//const val showChartsKey = "showCharts"
//const val showRelatedAlbumsKey = "showRelatedAlbums"
//const val showSimilarArtistsKey = "showSimilarArtists"
//const val showNewAlbumsArtistsKey = "showNewAlbumsArtists"
//const val showNewAlbumsKey = "showNewAlbums"
//const val showPlaylistMightLikeKey = "showPlaylistMightLike"
//const val showMoodsAndGenresKey = "showMoodsAndGenres"
//const val maxStatisticsItemsKey = "maxStatisticsItems"
//const val showStatsListeningTimeKey = "showStatsListeningTime"
//const val isProxyEnabledKey = "isProxyEnabled"
//const val proxyHostnameKey = "proxyHostname"
//const val proxyPortKey = "proxyPort"
//const val proxyModeKey = "ProxyMode"
//const val isRecommendationEnabledKey = "isRecommendationEnabled"
//const val showButtonPlayerAddToPlaylistKey = "showButtonPlayerAddToPlaylist"
//const val showButtonPlayerArrowKey = "showButtonPlayerArrow"
//const val showButtonPlayerDownloadKey = "showButtonPlayerDownload"
//const val showButtonPlayerLoopKey = "showButtonPlayerLoop"
//const val showButtonPlayerLyricsKey = "showButtonPlayerLyrics"
//const val showButtonPlayerShuffleKey = "showButtonPlayerShuffle"
//const val isKeepScreenOnEnabledKey = "isKeepScreenOnEnabled"
//const val recommendationsNumberKey = "recommendationsNumber"
//const val checkUpdateStateKey = "checkUpdateState"
//const val showButtonPlayerSleepTimerKey = "showButtonPlayerSleepTimer"
//const val keepPlayerMinimizedKey = "keepPlayerMinimized"
//const val isSwipeToActionEnabledKey = "isSwipeToActionEnabled"
//const val showButtonPlayerMenuKey = "showButtonPlayerMenu"
//const val showButtonPlayerStartRadioKey = "showButtonPlayerStartRadio"
//const val showButtonPlayerSystemEqualizerKey = "showButtonPlayerSystemEqualizer"
//const val showButtonPlayerDiscoverKey = "showButtonPlayerDiscover"
//const val showButtonPlayerVideoKey = "showButtonPlayerVideo"
//const val disableClosingPlayerSwipingDownKey = "disableClosingPlayerSwipingDown"
//const val showSearchTabKey = "showSearchTab"
//const val MaxTopPlaylistItemsKey = "MaxTopPlaylistItems"
//const val topPlaylistPeriodKey = "topPlaylistPeriod"
//const val navigationBarPositionKey = "navigationBarPosition"
//const val navigationBarTypeKey = "navigationBarType"
//const val pauseBetweenSongsKey = "pauseBetweenSongs"
//const val showFavoritesPlaylistKey = "showFavoritesPlaylist"
//const val showDislikedPlaylistKey = "showDislikedPlaylistKey"
//const val showCachedPlaylistKey = "showCachedPlaylist"
//const val showMyTopPlaylistKey = "showMyTopPlaylist"
//const val showDownloadedPlaylistKey = "showDownloadedPlaylist"
//const val showOnDevicePlaylistKey = "showOnDevicePlaylist"
//const val showPlaylistsKey = "showPlaylists"
//const val isGradientBackgroundEnabledKey = "isGradientBackgroundEnabled"
//const val playbackSpeedKey = "playbackSpeed"
//const val playbackPitchKey = "playbackPitch"
//const val playbackVolumeKey = "playbackVolume"
//const val playbackDeviceVolumeKey = "playbackDeviceVolume"
//const val playbackDurationKey = "playbackDuration"
//const val blurStrengthKey = "blurScale"
//const val blurDarkenFactorKey = "blurDarkenFactor"
//const val playbackFadeDurationKey = "playbackFadeDuration"
//const val playbackFadeAudioDurationKey = "playbackFadeAudioDuration"
//const val showTotalTimeQueueKey = "showTotalTimeQueue"
//const val backgroundProgressKey = "backgroundProgress"
//const val maxSongsInQueueKey = "maxSongsInQueue"
//const val showFoldersOnDeviceKey = "showFoldersOnDevice"
//const val showNextSongsInPlayerKey = "showNextSongsInPlayer"
//const val showRemainingSongTimeKey = "showRemainingSongTime"
//const val lyricsFontSizeKey = "lyricsFontSize"
//const val showBackgroundLyricsKey = "showBackgroundLyrics"
//const val includeLocalSongsKey = "includeLocalSongs"
//const val clickOnLyricsTextKey = "clickOnLyricsText"
//const val defaultFolderKey = "defaultFolder"
//const val menuStyleKey = "menuStyle"
//const val shakeEventEnabledKey = "shakeEventEnabled"
//const val useVolumeKeysToChangeSongKey = "useVolumeKeysToChangeSong"
//const val showStatsInNavbarKey = "showStatsInNavbar"
//const val showActionsBarKey = "showActionsBar"
//const val floatActionIconOffsetXkey = "floatActionIconOffsetX"
//const val floatActionIconOffsetYkey = "floatActionIconOffsetY"
//const val multiFloatActionIconOffsetXkey = "multiFloatActionIconOffsetX"
//const val multiFloatActionIconOffsetYkey = "multiFloatActionIconOffsetY"
//const val showBuiltinPlaylistsKey = "showBuiltinPlaylists"
//const val showPinnedPlaylistsKey = "showPinnedPlaylists"
//const val showPlaylistsListKey = "showPlaylistsList"
//const val showPlaylistsGeneralKey = "showPlaylistsGeneral"
//const val libraryItemSizeKey = "libraryItemSize"
//const val albumsItemSizeKey = "albumsItemSize"
//const val artistsItemSizeKey = "artistsItemSize"
//const val showFloatingIconKey = "showFloatingIcon"
//const val transitionEffectKey = "transitionEffect"
//const val showMonthlyPlaylistsKey = "showMonthlyPlaylists"
//const val showPipedPlaylistsKey = "showPipedPlaylists"
//const val showMonthlyPlaylistInQuickPicksKey = "showMonthlyPlaylistInQuickPicks"
//const val showMonthlyPlaylistInLibraryKey = "showMonthlyPlaylistInLibrary"
//const val enableQuickPicksPageKey = "enableQuickPicksPage"
//const val playerBackgroundColorsKey = "playerBackgroundColors"
//const val animatedGradientKey = "animatedGradient"
//const val playerControlsTypeKey = "playerControlsType"
//const val playerInfoTypeKey = "playerInfoType"
//const val showTopActionsBarKey = "showTopActionsBar"
//const val transparentBackgroundPlayerActionBarKey = "transparentBackgroundPlayerActionBar"
//const val enableCreateMonthlyPlaylistsKey = "enableCreateMonthlyPlaylists"
//const val autoShuffleKey = "autoShuffle"
//const val builtInPlaylistKey = "builtInPlaylist"
//const val playlistTypeKey = "playlistType"
//const val iconLikeTypeKey = "iconLikeType"
//const val playerSwapControlsWithTimelineKey = "playerSwapControlsWithTimeline"
//const val playerEnableLyricsPopupMessageKey = "playerEnableLyricsPopupMessage"
//const val historyTypeKey = "historyType"
///**** CUSTOM THEME **** */
//const val customThemeLight_Background0Key = "customThemeLight_Background0"
//const val customThemeLight_Background1Key = "customThemeLight_Background1"
//const val customThemeLight_Background2Key = "customThemeLight_Background2"
//const val customThemeLight_Background3Key = "customThemeLight_Background3"
//const val customThemeLight_Background4Key = "customThemeLight_Background4"
//const val customThemeLight_TextKey = "customThemeLight_Text"
//const val customThemeLight_textSecondaryKey = "customThemeLight_textSecondary"
//const val customThemeLight_textDisabledKey = "customThemeLight_textDisabled"
//const val customThemeLight_iconButtonPlayerKey = "customThemeLight_iconButtonPlayer"
//const val customThemeLight_accentKey = "customThemeLight_accent"
//
//const val customThemeDark_Background0Key = "customThemeDark_Background0"
//const val customThemeDark_Background1Key = "customThemeDark_Background1"
//const val customThemeDark_Background2Key = "customThemeDark_Background2"
//const val customThemeDark_Background3Key = "customThemeDark_Background3"
//const val customThemeDark_Background4Key = "customThemeDark_Background4"
//const val customThemeDark_TextKey = "customThemeDark_Text"
//const val customThemeDark_textSecondaryKey = "customThemeDark_textSecondary"
//const val customThemeDark_textDisabledKey = "customThemeDark_textDisabled"
//const val customThemeDark_iconButtonPlayerKey = "customThemeDark_iconButtonPlayer"
//const val customThemeDark_accentKey = "customThemeDark_accent"
///**** CUSTOM THEME **** */
//
//const val showthumbnailKey = "showthumbnail"
//const val showlyricsthumbnailKey = "showlyricsthumbnail"
//const val miniPlayerTypeKey = "miniPlayerType"
//const val lyricsColorKey = "lyricsColor"
//const val lyricsOutlineKey = "lyricsOutline"
//const val transparentbarKey = "transparentbar"
//const val isShowingLyricsKey = "isShowingLyrics"
//const val actionspacedevenlyKey = "actionspacedevenly"
//const val expandedplayerKey = "expandedplayer"
//const val expandedplayertoggleKey = "expandedplayertoggle"
//const val blackgradientKey = "blackgradient"
//const val bottomgradientKey = "bottomgradient"
//const val textoutlineKey = "textoutline"
//const val thumbnailTypeKey = "thumbnailType"
//const val showvisthumbnailKey = "showvisthumbnail"
//const val extraspaceKey = "extraspace"
//const val expandedlyricsKey = "expandedlyrics"
//const val lyricsHighlightKey = "lyricsHighlight"
//const val buttonStateKey = "buttonState"
//const val buttonzoomoutKey = "buttonzoomout"
//const val thumbnailpauseKey = "thumbnailpause"
//const val showsongsKey = "showsongs"
//const val showalbumcoverKey = "showalbumcover"
//const val lyricsBackgroundKey = "lyricsBackground"
//const val lyricsAlignmentKey = "lyricsAlignment"
//const val hideprevnextKey = "hideprevnext"
//const val prevNextSongsKey = "prevNextSongs"
//const val tapqueueKey = "tapqueue"
//const val swipeUpQueueKey = "swipeUpQueue"
//const val playlistindicatorKey = "playlistindicator"
//const val nowPlayingIndicatorKey = "nowPlayingIndicator"
//const val statsfornerdsKey = "statsfornerds"
//const val statsfornerdsfullKey = "statsfornerdsfull"
//const val discoverKey = "discover"
//const val playerTypeKey = "playerType"
//const val noblurKey = "noblur"
//const val fadingedgeKey = "fadingedge"
//const val thumbnailOffsetKey = "thumbnailOffset"
//const val thumbnailFadeKey = "thumbnailFade"
//const val thumbnailFadeExKey = "thumbnailFadeEx"
//const val carouselKey = "carousel"
//const val carouselSizeKey = "carouselSize"
//const val thumbnailSpacingKey = "thumbnailSpacing"
//const val thumbnailSpacingLKey = "thumbnailSpacingL"
//const val autosyncKey = "autosync"
//const val queueTypeKey = "queueType"


//const val parentalControlEnabledKey = "parentalControlEnabled"
//const val playerPositionKey = "playerPosition"
//const val excludeSongsWithDurationLimitKey = "excludeSongsWithDurationLimit"
//const val logDebugEnabledKey = "logDebugEnabled"
//
//const val messageTypeKey = "messageType"
//const val isPauseOnVolumeZeroEnabledKey = "isPauseOnVolumeZeroEnabled"
//const val playerInfoShowIconsKey = "playerInfoShowIcons"
//const val minimumSilenceDurationKey = "minimumSilenceDuration"
//const val pauseListenHistoryKey = "pauseListenHistory"
//const val selectedCountryCodeKey = "selectedCountryCode"
//
//const val lastVideoIdKey = "lastVideoId"
//const val lastVideoSecondsKey = "lastVideoSeconds"
//const val isDiscordPresenceEnabledKey = "isDiscordPresenceEnabled"
//const val loudnessBaseGainKey = "loudnessBaseGain"
//const val statisticsCategoryKey = "statisticsCategory"
//const val queueDurationExpandedKey = "queueDurationExpanded"
//const val titleExpandedKey = "titleExpanded"
//const val timelineExpandedKey = "timelineExpanded"
//const val controlsExpandedKey = "controlsExpanded"
//const val miniQueueExpandedKey = "miniQueueExpanded"
//const val statsExpandedKey = "statsExpanded"
//const val actionExpandedKey = "actionExpanded"
//const val showCoverThumbnailAnimationKey = "showCoverThumbnailAnimation"
//const val coverThumbnailAnimationKey = "coverThumbnailAnimation"
//
//const val restartActivityKey = "restartActivity"
//const val enableYouTubeLoginKey = "enableYoutubeLogin"
//const val enableYouTubeSyncKey = "enableYoutubeSync"
//const val useYtLoginOnlyForBrowseKey = "useYtLoginOnlyForBrowse"
//
//const val autoLoadSongsInQueueKey = "autoLoadSongsInQueue"
//const val showSecondLineKey = "showSecondLine"
//const val VinylSizeKey = "VinylSize"
//const val romanizationKey = "romanization"
//
//const val quickPicsTrendingSongKey = "quickPicsTrendingSong"
//const val quickPicsRelatedPageKey = "quickPicsRelatedPage"
//const val quickPicsChartsPageKey = "quickPicsChartsPage"
//const val quickPicsDiscoverPageKey = "quickPicsDiscoverPage"
//const val quickPicsHomePageKey = "quickPicsHomePage"
//const val loadedDataKey = "loadedData"
//
//const val enablePictureInPictureKey = "enablePicturInPicture"
//const val enablePictureInPictureAutoKey = "enablePicturInPictureAuto"
//const val pipModuleKey = "pipModule"
//
//const val notificationPlayerFirstIconKey = "notificationPlayerFirstIcon"
//const val notificationPlayerSecondIconKey = "notificationPlayerSecondIcon"
//const val jumpPreviousKey = "jumpPrevious"
//
//const val artistTypeKey = "artistType"
//const val albumTypeKey = "albumType"
//
//const val enableWallpaperKey = "enableWallpaper"
//const val wallpaperTypeKey = "wallpaperType"
//
//const val topPaddingKey = "topPadding"
//
//const val queueSwipeLeftActionKey = "queueSwipeLeftAction"
//const val queueSwipeRightActionKey = "queueSwipeRightAction"
//const val playlistSwipeLeftActionKey = "playlistSwipeLeftAction"
//const val playlistSwipeRightActionKey = "playlistSwipeRightAction"
//const val albumSwipeLeftActionKey = "albumSwipeLeftAction"
//const val albumSwipeRightActionKey = "albumSwipeRightAction"
//const val customColorKey = "customColor"
//const val lyricsSizeAnimateKey = "lyricsSizeAnimate"
//const val lyricsSizeKey = "lyricsSize"
//const val lyricsSizeLKey = "lyricsSizeL"
//const val ytAccountNameKey = "ytAccountName"
//const val ytAccountEmailKey = "ytAccountEmail"
//const val albumCoverRotationKey = "albumCoverRotation"
//const val isConnectionMeteredEnabledKey = "isConnectionMeteredEnabled"
//const val landscapeControlsKey = "landscapeControls"
//const val swipeAnimationsNoThumbnailKey = "swipeAnimationsNoThumbnail"
//const val playlistSongsTypeFilterKey = "playlistSongsTypeFilter"
//
//const val ytVisitorDataKey = "ytVisitorData"
//const val ytCookieKey = "ytCookie"
//const val ytAccountChannelHandleKey = "ytAccountChannelHandle"
//const val ytAccountThumbnailKey = "ytAccountThumbnail"
//const val ytDataSyncIdKey = "ytDataSyncId"
//const val filterByKey = "filterBy"
//
//const val bassboostEnabledKey = "bassboostEnabled"
//const val bassboostLevelKey = "bassboostLevel"
//const val audioReverbPresetKey = "audioReverbPreset"
//const val handleAudioFocusEnabledKey = "handleAudioFocusEnabled"
//
//const val discordPersonalAccessTokenKey = "DiscordPersonalAccessToken"
//const val discordAccountNameKey = "DiscordAccountName"
//const val viewTypeKey = "viewType"
//const val volumeBoostLevelKey = "volumeBoostLevel"
//const val dnsOverHttpsTypeKey = "dnsOverHttpsType"
//const val customDnsOverHttpsServerKey = "customDnsOverHttpsServer"
//const val enablePreCacheKey = "enablePreCache"
//const val streamingPlayerTypeKey = "streamingPlayerType"
//
////const val castToRiTuneDeviceEnabledKey = "castToRiTuneDeviceEnabled"
//const val usePlaceholderInImageLoaderKey = "usePlaceholderInImageLoader"

//const val seekWithTapKey = "seekWithTap"
//
//const val filterContentTypeKey = "filterContentType"
//const val isEnabledFullscreenKey = "isEnabledFullscreen"
//
//const val appIsRunningKey = "appIsRunning"
//
//const val lastMediaItemWasLocalKey = "lastMediaItemWasLocal"
//
//const val notifyTipsKey = "notifyTips"
//const val notifyAndroidAutoTipsKey = "notifyAandroidAutoTips"
//
//const val resumeOrPausePlaybackWhenDeviceKey = "resumeOrPausePlaybackWhenDevice"
//
//const val showShuffleSongsAAKey = "shuffleSongsAAEnabled"
//const val showMonthlyPlaylistsAAKey = "showMonthlyPlaylistsAA"
//const val showInLibraryAAKey = "showInLibraryAA"
//const val showOnDeviceAAKey = "showOnDeviceAA"
//const val showFavoritesSongsAAKey = "showFavoritesSongsAA"
//const val showTopSongsAAKey = "showTopSongsAA"
//const val showGridAAKey = "showGridAA"
//
//const val showPlayerActionsBarKey = "showPlayerActionsBar"
//
//const val showAutostartPermissionDialogKey = "showAutostartPermissionDialog"
//
//const val currentQueuePositionKey = "currentQueuePosition" // todo maybe not needed
//
//const val excludeSongIfIsVideoKey = "excludeSongIfIsVideo"
//
//const val enableVoiceInputKey = "enableVoiceInput"
//const val enableMusicIdentifierKey = "enableMusicIdentifier"
//const val musicIdentifierProviderKey = "musicIdentifierProvider"
//const val musicIdentifierApiKey = "musicIdentifierApiKey"
//
//const val closePlayerServiceAfterMinutesKey = "closeBackgroundPlayerAfterMinutes"
//const val closePlayerServiceWhenPausedAfterMinutesKey = "closePlayerServiceWhenPausedAfterMinutes"
//const val checkVolumeLevelKey = "checkVolumeLevel"
//const val showListenerLevelsKey = "showListenerLevels"
//const val homeTypeKey = "homeType"
//const val showSnowfallEffectKey = "showSnowfallEffect"
//
//const val isEnabledLastfmKey = "isEnabledLastfm"
//const val lastfmSessionTokenKey = "lastfmSessionToken"
//const val lastfmScrobbleTypeKey = "lastfmScrobbleType"
//
//const val shortOnDeviceFolderNameKey = "shortOnDeviceFolderName"
//
//const val autoBackupFolderKey = "autoBackupFolder"
//const val eqEnabledKey = "eqEnabled"
//const val eqPresetKey = "eqPreset"
//const val eqBandsKey = "eqBands"
//
//const val equalizerTypeKey = "equalizerType"
//
//const val homePageTypeKey = "homePageType"
//
//const val resumeOrPausePlaybackWhenCallKey = "resumeOrPausePlaybackWhenCall"
//
//const val stateMediaIdKey = "stateMediaId"
//const val statePositionKey = "statePosition"
//const val stateDurationKey = "stateDuration"
//const val stateIsPlayingKey = "stateIsPlaying"
//
//const val isShowingSynchronizedWordByWordLyricsKey = "isShowingSynchronizedWordByWorkLyrics"
//
//const val showAllSongsAAKey = "showAllSongsAA"
//const val showPodcastAAKey = "showPodcastAA"
//const val showPinnedAAKey = "showPinnedAA"
//
//const val showOnBoardingScreenKey = "onBoardingScreen"
//
//const val timerEndTimeKey = "timerEndTime"
//
//const val disableAudioDRCKey = "disableAudioDRC"
//
//const val castTypeKey = "castType"
//
//const val activeAppearancePresetIdKey = "activeAppearancePresetId"
//
//const val musicVaultPathKey = "musicVaultPath"
//const val musicVaultEnabledKey = "musicVaultEnabled"
//const val musicVaultDisclaimerAcceptedKey = "musicVaultDisclaimerAccepted"


@Composable
fun rememberPreference(key: String, defaultValue: RiTuneDevices?): MutableState<RiTuneDevices?> {
    val context = LocalContext.current
    val json = Json.encodeToString(defaultValue)
    return remember {
        mutableStatePreferenceOf(
            try {
                context.preferences.getString(key, json)
                    ?.let { Json.decodeFromString<RiTuneDevices>(it) }
            } catch (e: Exception) {
                Timber.e("RememberPreference LinkDevices Error: ${ e.message }")
                null
            }
        ) {
            context.preferences.edit { putString(
                key,
                Json.encodeToString(it)
            ) }
        }
    }
}

@Composable
fun rememberPreference(key: String, defaultValue: Song?): MutableState<Song?> {
    val context = LocalContext.current
    val json = Json.encodeToString(defaultValue)
    return remember {
        mutableStatePreferenceOf(
            try {
                context.preferences.getString(key, json)
                    ?.let { Json.decodeFromString<Song>(it) }
            } catch (e: Exception) {
                Timber.e("RememberPreference Song Error: ${ e.message }")
                null
            }
        ) {
            context.preferences.edit { putString(
                key,
                Json.encodeToString(it)
            ) }
        }
    }
}

@Composable
fun rememberPreference(key: String, defaultValue: Environment.DiscoverPage?): MutableState<Environment.DiscoverPage?> {
    val context = LocalContext.current
    val json = Json.encodeToString(defaultValue)
    return remember {
        mutableStatePreferenceOf(
            try {
                context.preferences.getString(key, json)
                    ?.let { Json.decodeFromString<Environment.DiscoverPage>(it) }
            } catch (e: Exception) {
                Timber.e("RememberPreference DiscoverPage Error: ${ e.message }")
                null
            }
        ) {
            context.preferences.edit { putString(
                key,
                Json.encodeToString(it)
            ) }
        }
    }
}

@Composable
fun rememberPreference(key: String, defaultValue: Environment.ChartsPage?): MutableState<Environment.ChartsPage?> {
    val context = LocalContext.current
    val json = Json.encodeToString(defaultValue)
    return remember {
        mutableStatePreferenceOf(
            try {
                context.preferences.getString(key, json)
                    ?.let { Json.decodeFromString<Environment.ChartsPage>(it) }
            } catch (e: Exception) {
                Timber.e("RememberPreference ChartsPage Error: ${ e.message }")
                null
            }
        ) {
            context.preferences.edit { putString(
                key,
                Json.encodeToString(it)
            ) }
        }
    }
}

@Composable
fun rememberPreference(key: String, defaultValue: Environment.RelatedPage?): MutableState<Environment.RelatedPage?> {
    val context = LocalContext.current
    val json = Json.encodeToString(defaultValue)
    return remember {
        mutableStatePreferenceOf(
            try {
                context.preferences.getString(key, json)
                    ?.let { Json.decodeFromString<Environment.RelatedPage>(it) }
            } catch (e: Exception) {
                Timber.e("RememberPreference RelatedPage Error: ${ e.message }")
                null
            }
        ) {
            context.preferences.edit { putString(
                key,
                Json.encodeToString(it)
            ) }
        }
    }
}

@Composable
fun rememberPreference(key: String, defaultValue: HomePage?): MutableState<HomePage?> {
    val context = LocalContext.current
    val json = Json.encodeToString(defaultValue)
    return remember {
        mutableStatePreferenceOf(
            try {
                context.preferences.getString(key, json)
                    ?.let { Json.decodeFromString<HomePage>(it) }
            } catch (e: Exception) {
                Timber.e("RememberPreference HomePage Error: ${ e.message }")
                null
            }
        ) {
            context.preferences.edit { putString(
                key,
                Json.encodeToString(it)
            ) }
        }
    }
}


@Composable
fun <T : Enum<T>> rememberPreference(key: String, defaultValue: T): MutableState<T> {
    val context = LocalContext.current
    val prefs = context.preferences

    @Suppress("UNCHECKED_CAST")
    val enumClass = defaultValue.javaClass as Class<T>

    fun readEnum(): T {
        return try {
            val name = prefs.getString(key, null)
            if (name != null) java.lang.Enum.valueOf(enumClass, name) else defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    val internalState = remember(key, defaultValue) {
        mutableStateOf(readEnum())
    }

    LaunchedEffect(key, defaultValue) {
        channelFlow {
            send(readEnum())

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
                if (key == k) {
                    trySend(readEnum())
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.collect { newValue ->
            if (internalState.value != newValue) {
                internalState.value = newValue
            }
        }
    }

    return remember(key, defaultValue) {
        object : MutableState<T> {
            override var value: T
                get() = internalState.value
                set(newValue) {
                    internalState.value = newValue
                    prefs.edit {
                        putString(key, newValue.name)
                    }
                }
            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}


@Composable
fun <T> rememberPreference(key: String, defaultValue: T): MutableState<T> {
    val context = LocalContext.current
    val prefs = context.preferences

    fun readValue(): T {
        @Suppress("UNCHECKED_CAST")
        return when (defaultValue) {
            is String -> prefs.getString(key, defaultValue) as T
            is Int -> prefs.getInt(key, defaultValue) as T
            is Long -> prefs.getLong(key, defaultValue) as T
            is Boolean -> prefs.getBoolean(key, defaultValue) as T
            is Float -> prefs.getFloat(key, defaultValue) as T
            is Set<*> -> prefs.getStringSet(key, defaultValue as Set<String>) as T
            else -> throw IllegalArgumentException("Type not handled")
        }
    }

    val internalState = remember(key, defaultValue) {
        mutableStateOf(readValue())
    }

    LaunchedEffect(key, defaultValue) {
        channelFlow {
            send(readValue())

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
                if (key == k) {
                    trySend(readValue())
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.collect { newValue ->
            if (internalState.value != newValue) {
                internalState.value = newValue
            }
        }
    }

    return remember(key, defaultValue) {
        object : MutableState<T> {
            override var value: T
                get() = internalState.value
                set(newValue) {
                    internalState.value = newValue
                    prefs.edit {
                        @Suppress("UNCHECKED_CAST")
                        when (newValue) {
                            is String -> putString(key, newValue)
                            is Int -> putInt(key, newValue)
                            is Long -> putLong(key, newValue)
                            is Boolean -> putBoolean(key, newValue)
                            is Float -> putFloat(key, newValue)
                            is Set<*> -> putStringSet(key, newValue as Set<String>)
                            else -> throw IllegalArgumentException("Type not handled")
                        }
                    }
                }
            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}


fun clearPreference(context: Context, key: String): Unit {
    try {
        context.preferences.edit { remove(key) }
    } catch (e: Exception) {
        Timber.e("ClearPreference Error: ${e.message}")
    }
}

suspend fun cleanUpUnusedPreferences(context: Context) = withContext(Dispatchers.IO) {
    val prefs = context.preferences

    val keysOnDisk = prefs.all.keys

    val activeKeys = PreferenceKey.allKeys

    val keysToDelete = keysOnDisk - activeKeys

    if (keysToDelete.isNotEmpty()) {
        Timber.d("PrefsCleanup Trovate ${keysToDelete.size} chiavi inutilizzate: $keysToDelete")

        prefs.edit {
            keysToDelete.forEach { key ->
                remove(key)
            }
        }
        Timber.d("PrefsCleanup Pulizia completata.")
    } else {
        Timber.d("PrefsCleanup Nessuna chiave inutilizzata trovata.")
    }
}


inline fun <T> mutableStatePreferenceOf(
    value: T,
    crossinline onStructuralInequality: (newValue: T) -> Unit
) =
    mutableStateOf(
        value = value,
        policy = object : SnapshotMutationPolicy<T> {
            override fun equivalent(a: T, b: T): Boolean {
                val areEquals = a == b
                if (!areEquals) onStructuralInequality(b)
                return areEquals
            }
        })


inline fun <reified T : Enum<T>> SharedPreferences.getEnum(
    key: String,
    defaultValue: T
): T =
    getString(key, null)?.let {
        try {
            enumValueOf<T>(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: defaultValue

inline fun <reified T : Enum<T>> SharedPreferences.Editor.putEnum(
    key: String,
    value: T
): SharedPreferences.Editor =
    putString(key, value.name)