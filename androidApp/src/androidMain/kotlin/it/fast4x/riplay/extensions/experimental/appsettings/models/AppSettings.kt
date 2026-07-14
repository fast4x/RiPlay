package it.fast4x.riplay.extensions.experimental.appsettings.models

import it.fast4x.riplay.enums.AlbumSwipeAction
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.enums.CoilDiskCacheMaxSize
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.HomePagetype
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.Languages
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.enums.MaxStatisticsItems
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NavigationBarType
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.PlaylistSwipeAction
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.QueueSwipeAction
import it.fast4x.riplay.enums.RecommendationsNumber
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.utils.getSystemlanguage
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val languageApp: Languages = getSystemlanguage(),
    val otherLanguageApp: Languages = Languages.English,
    val pipModule: PipModule = PipModule.Cover,
    val isSnowEffectEnabled: Boolean = false,
    val ytCookie: String = "",
    val ytVisitorData: String = "",
    val ytDataSyncId: String = "",
    val ytPageId: String = "",
    val ytAuthUser: String = "",
    val ytAccountName: String = "",
    val ytAccountEmail: String = "",
    val ytAccountChannelHandle: String = "",
    val ytAccountThumbnail: String = "",
    val ytCachedAccounts: String = "",
    val customDnsOverHttpsServer: String = "",
    val recommendationsNumber: RecommendationsNumber = RecommendationsNumber.`5`,
    val keepPlayerMinimized: Boolean = false,
    val disableIconButtonOnTop: Boolean = false,
    val disablePlayerHorizontalSwipe: Boolean = false,
    val indexNavigationTab: HomeScreenTabs = HomeScreenTabs.Default,
    val fontType: FontType = FontType.Rubik,
    val useSystemFont: Boolean = false,
    val applyFontPadding: Boolean = false,
    val isSwipeToActionEnabled: Boolean = true,
    val showSearchTab: Boolean = false,
    val showStatsInNavbar: Boolean = false,
    val maxStatisticsItems: MaxStatisticsItems = MaxStatisticsItems.`10`,
    val showStatsListeningTime: Boolean = true,
    val maxTopPlaylistItems: MaxTopPlaylistItems = MaxTopPlaylistItems.`10`,
    val navigationBarPosition: NavigationBarPosition = NavigationBarPosition.Bottom,
    val navigationBarType: NavigationBarType = NavigationBarType.IconAndText,
    val showFavoritesPlaylist: Boolean = true,
    val showMyTopPlaylist: Boolean = true,
    val showOnDevicePlaylist: Boolean = true,
    val showDislikedPlaylist: Boolean = false,
    val shakeEventEnabled: Boolean = false,
    val useVolumeKeysToChangeSong: Boolean = false,
    val showFloatingIcon: Boolean = false,
    val menuStyle: MenuStyle = MenuStyle.List,
    val transitionEffect: TransitionEffect = TransitionEffect.SlideHorizontal,
    val enableCreateMonthlyPlaylists: Boolean = true,
    val showPipedPlaylists: Boolean = true,
    val showPinnedPlaylists: Boolean = true,
    val showMonthlyPlaylists: Boolean = true,
    val playerPosition: PlayerPosition = PlayerPosition.Bottom,
    val messageType: MessageType = MessageType.Modern,
    val queueSwipeLeftAction: QueueSwipeAction = QueueSwipeAction.RemoveFromQueue,
    val queueSwipeRightAction: QueueSwipeAction = QueueSwipeAction.PlayNext,
    val queueLoopType: QueueLoopType                 = QueueLoopType.Default,
    val playlistSwipeLeftAction: PlaylistSwipeAction = PlaylistSwipeAction.Favourite,
    val playlistSwipeRightAction: PlaylistSwipeAction = PlaylistSwipeAction.PlayNext,
    val albumSwipeLeftAction: AlbumSwipeAction = AlbumSwipeAction.PlayNext,
    val albumSwipeRightAction: AlbumSwipeAction = AlbumSwipeAction.Bookmark,
    val usePlaceholderInImageLoader: Boolean = true,
    val isEnabledFullScreen: Boolean = false,
    val showSnowfallEffect: Boolean = false,
    val showListenerLevels: Boolean = true,
    val uiType: UiType = UiType.RiPlay,
    val enableMusicIdentifier: Boolean = false,
    val musicIdentifierApi: String = "",
    val musicIdentifierProvider: MusicIdentifierProvider = MusicIdentifierProvider.AudioTagInfo,
    val enableYtLogin: Boolean = false,
    val enableYtSync: Boolean = true,
    val isEnabledLastFM: Boolean = false,
    val lastFMSessionToken: String = "",
    val lastFmScrobbleType: LastFmScrobbleType = LastFmScrobbleType.Simple,
    val isDiscordPresenceEnabled: Boolean = false,
    val discordPersonalAccessToken: String = "",
    val discordAccountName: String = "",
    val coilDiskCacheMaxSize: CoilDiskCacheMaxSize = CoilDiskCacheMaxSize.`128MB`,
    val coilCustomDiskCache: Int = 32,
    val folderPath: String = "/",
    val minTimeForEvent: MinTimeForEvent = MinTimeForEvent.`20s`,
    val persistentQueue: Boolean = true,
    val resumePlaybackOnStart: Boolean = false,
    val closeBackgroundPlayerAfterMinutes: DurationInMinutes = DurationInMinutes.Disabled,
    val closeWithBackButton: Boolean = true,
    val resumeOrPausePlaybackWhenDeviceBt: Boolean = false,
    val resumeOrPausePlaybackWhenDeviceWired: Boolean = false,
    val resumeOrPausePlaybackWhenCall: Boolean = false,
    val showTips: Boolean = true,
    val showRelatedAlbums: Boolean = true,
    val showSimilarArtists: Boolean = true,
    val showNewAlbumsArtists: Boolean = true,
    val showNewAlbums: Boolean = true,
    val showPlaylistMightLike: Boolean = true,
    val showMoodAndGenres: Boolean = true,
    val showMonthlyPlaylistInQuickPicks: Boolean = true,
    val showCharts: Boolean = true,
    val homePageType: HomePagetype = HomePagetype.Classic,
    val logDebugEnabled: Boolean = false,
    val musicVaultEnabled: Boolean = false,
    val musicVaultDisclaimerAccepted: Boolean = false,
    val isShowingThumbnailInLockscreen: Boolean = true,
    val playbackDuration: Float = 0f,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val audioQualityFormat: AudioQualityFormat = AudioQualityFormat.Auto,
    val castType: CastType = CastType.RITUNECAST,
    val skipSilenceEnabled: Boolean = false,
    val stateDuration: Float = 0f,
    val stateMediaId: String = "",
    val isPauseOnVolumeZeroEnabled: Boolean = false,
    val isPauseListenHistoryEnabled: Boolean = false,
    val autoLoadSongsInQueue: Boolean = true,
    val volumeNormalizationEnabled: Boolean = false,
    val loudnessBaseGain: Float = 5.00f,
    val volumeBoostLevel: Float = 0.00f,
    val disableAudioDrc: Boolean = false,
    val notificationPlayerFirstIcon: NotificationButtons = NotificationButtons.Repeat,
    val notificationPlayerSecondIcon: NotificationButtons = NotificationButtons.Favorites,
    val playbackFadeAudioDuration: DurationInMilliseconds = DurationInMilliseconds.Disabled,
    val bassBoostEnabled: Boolean = false,
    val bassBoostLevel: Float = 0.5f,
    val audioReverbPreset: PresetsReverb = PresetsReverb.NONE,
    val minimumSilenceDuration: Long = 2_000_000L,
    val enableWallpaper: Boolean               = false,
    val wallpaperType: WallpaperType           = WallpaperType.Lockscreen,
    val timerEndTime: Long                      = 0L,
    val discoverIsEnabled: Boolean                 = false,
    val filterContentType: ContentType = ContentType.All,

    )

