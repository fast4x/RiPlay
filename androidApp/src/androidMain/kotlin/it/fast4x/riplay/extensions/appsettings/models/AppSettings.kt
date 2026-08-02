package it.fast4x.riplay.extensions.appsettings.models

import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.AlbumSwipeAction
import it.fast4x.riplay.enums.AlbumsType
import it.fast4x.riplay.enums.AndroidAutoPlaylistLimit
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.ArtistsType
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.BuiltInPlaylist
import it.fast4x.riplay.enums.CastType
import it.fast4x.riplay.enums.CheckUpdateState
import it.fast4x.riplay.enums.CoilDiskCacheMaxSize
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.Countries
import it.fast4x.riplay.enums.DnsOverHttpsType
import it.fast4x.riplay.enums.DurationInMilliseconds
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.EqualizerType
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.HistoryType
import it.fast4x.riplay.enums.HomeItemSize
import it.fast4x.riplay.enums.HomePagetype
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.HomeType
import it.fast4x.riplay.enums.ImportPlaylistType
import it.fast4x.riplay.enums.Languages
import it.fast4x.riplay.enums.LastFmScrobbleType
import it.fast4x.riplay.enums.LocalPlayerCacheLocation
import it.fast4x.riplay.enums.LocalPlayerDiskCacheMaxSize
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.enums.MaxStatisticsItems
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.MinTimeForEvent
import it.fast4x.riplay.enums.MusicAnimationType
import it.fast4x.riplay.enums.MusicIdentifierProvider
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NavigationBarType
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.OnDeviceFolderSortBy
import it.fast4x.riplay.enums.OnDeviceSongSortBy
import it.fast4x.riplay.enums.PauseBetweenSongs
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.enums.PlayEventsType
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PlaylistSongsTypeFilter
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.PlaylistSwipeAction
import it.fast4x.riplay.enums.PlaylistType
import it.fast4x.riplay.enums.PresetsReverb
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.QueueSwipeAction
import it.fast4x.riplay.enums.RecommendationsNumber
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.enums.StatisticsCategory
import it.fast4x.riplay.enums.TopPlaylistPeriod
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.ViewType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.getDeviceVolume
import it.fast4x.riplay.utils.getSystemlanguage
import kotlinx.serialization.Serializable
import java.net.Proxy

@Serializable
data class AppSettings(
    val languageApp: Languages = Languages.English,
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
    val musicVaultPath: String = "",
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
    val showAutostartPermissionDialog: Boolean = true,
    val keepScreenEnabled: Boolean = false,
    val proxyEnabled: Boolean = false,
    val proxyHostname: String = "",
    val proxyPort: Int = 8080,
    val proxyMode: Proxy.Type = Proxy.Type.HTTP,
    val parentalControlEnabled: Boolean = false,
    val checkUpdateState: CheckUpdateState = CheckUpdateState.Enabled,
    val offlineModeEnabled: Boolean = false,
    val excludeIfIsVideo: Boolean = false,
    val excludeSongWithDurationLimit: DurationInMinutes = DurationInMinutes.Disabled,
    val initialSetupWorkerDone: Boolean = false,
    val currentVisualizer: Int = 0,
    val enablePictureInPicture: Boolean = false,
    val enablePictureInPictureAuto: Boolean = false,
    val qrCodeToActions: Boolean = true,
    val autoBackupFolder: String = "",
    val showOnboardingScreen: Boolean = true,
    val eqEnabled: Boolean = false,
    val eqPreset: String = "Flat",
    val eqBands: String = "",
    val songSortBy: SongSortBy = SongSortBy.DateAdded,
    val songSortOrder: SortOrder = SortOrder.Descending,
    val artistSortOrder: SortOrder = SortOrder.Descending,
    val artistSortBy: ArtistSortBy = ArtistSortBy.DateAdded,
    val albumSortOrder: SortOrder = SortOrder.Descending,
    val albumSortBy: AlbumSortBy = AlbumSortBy.DateAdded,
    val playlistSortBy: PlaylistSortBy = PlaylistSortBy.DateAdded,
    val playlistSongsSortBy: PlaylistSongSortBy = PlaylistSongSortBy.DateAdded,
    val playlistSortOrder: SortOrder = SortOrder.Descending,
    val onDeviceSongSortBy: OnDeviceSongSortBy = OnDeviceSongSortBy.DateAdded,
    val onDeviceFolderSortBy: OnDeviceFolderSortBy = OnDeviceFolderSortBy.Title,
    val androidAutoPlaylistLimit: AndroidAutoPlaylistLimit = AndroidAutoPlaylistLimit.Unlimited,
    val albumsItemSize: HomeItemSize = HomeItemSize.BIG,
    val songsItemSize: HomeItemSize = HomeItemSize.BIG,
    val artistsItemSize: HomeItemSize = HomeItemSize.BIG,
    val multiFloatActionIconOffsetX: Float = 0f,
    val multiFloatActionIconOffsetY: Float = 0f,
    val nowPlayingIndicator: MusicAnimationType = MusicAnimationType.Bubbles,
    val equalizerType: EqualizerType = EqualizerType.Internal,
    val maxSongsInQueue: MaxSongs = MaxSongs.`500`,
    val exoPlayerDiskCacheMaxSize: LocalPlayerDiskCacheMaxSize = LocalPlayerDiskCacheMaxSize.`2GB`,
    val exoPlayerCustomCache: Int = 32,
    val exoPlayerCacheLocation: LocalPlayerCacheLocation = LocalPlayerCacheLocation.System,
    val playbackVolume: Float = 0.5f,
    val playbackDeviceVolume: Float = getDeviceVolume(appContext()),
    val historyType: HistoryType = HistoryType.History,
    val albumType: AlbumsType = AlbumsType.Favorites,
    val artistType: ArtistsType = ArtistsType.Favorites,
    val playlistType: PlaylistType = PlaylistType.Playlist,
    val importPlaylistType: ImportPlaylistType = ImportPlaylistType.Riplay,
    val shortOnDeviceFolderName: Boolean = false,
    val homeScreenTabIndex: Int = 0,
    val builtInPlaylist: BuiltInPlaylist = BuiltInPlaylist.Favorites,
    val includeLocalSongs: Boolean = true,
    val autoShuffle: Boolean = false,
    val topPlaylistPeriod: TopPlaylistPeriod = TopPlaylistPeriod.PastWeek,
    val showFoldersOnDevice: Boolean = true,
    val defaultFolder: String = "/",
    val playEventsType: PlayEventsType = PlayEventsType.MostPlayed,
    val showMoodsAndGenres: Boolean = true,
    val selectedCountryCode: Countries = Countries.ZZ,
    val homeType: HomeType = HomeType.Tabbed,
    val playlistSongsTypeFilter: PlaylistSongsTypeFilter = PlaylistSongsTypeFilter.All,
    val isRecommendationEnabled: Boolean = false,
    val reorderInQueueEnabled: Boolean = true,
    val pauseBetweenSongs: PauseBetweenSongs = PauseBetweenSongs.`0`,
    val disableClosingPlayerSwipingDown: Boolean = false,
    val enableVoiceInput: Boolean = true,
    val skipMediaOnError: Boolean = false,
    val dnsOverHttpsType: DnsOverHttpsType = DnsOverHttpsType.None,
    val handleAudioFocusEnabled: Boolean = true,
    val showShuffleSongsAA: Boolean = true,
    val showMonthlyPlaylistAA: Boolean = true,
    val showInLibraryAA: Boolean = true,
    val showOnDeviceAA: Boolean = true,
    val showTopSongsAA: Boolean = true,
    val showAllSongsAA: Boolean = true,
    val showPodcastAA: Boolean = true,
    val showPinnedAA: Boolean = true,
    val showGridAA: Boolean = true,
    val pauseSearchHistory: Boolean = false,
    val statisticsCategory: StatisticsCategory = StatisticsCategory.Songs,
    val viewType: ViewType = ViewType.Grid,
    val isReorderDisabled: Boolean = false,
    val isAndroidAutoEnabled: Boolean = true,

    )

