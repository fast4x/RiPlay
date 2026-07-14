package it.fast4x.riplay.extensions.experimental.appsettings.models

import it.fast4x.riplay.enums.AlbumSwipeAction
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.HomeScreenTabs
import it.fast4x.riplay.enums.Languages
import it.fast4x.riplay.enums.MaxStatisticsItems
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NavigationBarType
import it.fast4x.riplay.enums.PipModule
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.PlaylistSwipeAction
import it.fast4x.riplay.enums.QueueSwipeAction
import it.fast4x.riplay.enums.RecommendationsNumber
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.utils.getSystemlanguage
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val languageApp: Languages = getSystemlanguage(),
    val otherLanguageApp: Languages = Languages.English,
    val pipModule: PipModule = PipModule.Cover,
    val isSnowEffectEnabled: Boolean = false,
    val ytCookey: String = "",
    val ytVisitorData: String = "",
    val ytDataSyncId: String = "",
    val ytPageId: String = "",
    val ytAuthUser: String = "",
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
    val playlistSwipeLeftAction: PlaylistSwipeAction = PlaylistSwipeAction.Favourite,
    val playlistSwipeRightAction: PlaylistSwipeAction = PlaylistSwipeAction.PlayNext,
    val albumSwipeLeftAction: AlbumSwipeAction = AlbumSwipeAction.PlayNext,
    val albumSwipeRightAction: AlbumSwipeAction = AlbumSwipeAction.Bookmark,
    val usePlaceholderInImageLoader: Boolean = true,
    val isEnabledFullScreen: Boolean = false,
    val showSnowfallEffect: Boolean = false,
    val showListenerLevels: Boolean = true,
    val uiType: UiType = UiType.RiPlay,

    )

