package it.fast4x.riplay.extensions.appsettings

import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.enums.AudioVisualizerBaseStyle
import it.fast4x.riplay.enums.AudioVisualizerStyle
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.CheckUpdateState
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.DurationInMinutes
import it.fast4x.riplay.enums.ExoPlayerCacheLocation
import it.fast4x.riplay.enums.ExoPlayerDiskCacheMaxSize
import it.fast4x.riplay.enums.FontType
import it.fast4x.riplay.enums.Languages
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.MiniPlayerType
import it.fast4x.riplay.enums.MusicAnimationType
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.NavigationBarType
import it.fast4x.riplay.enums.PauseBetweenSongs
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerPosition
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.enums.VideoContentMode
import it.fast4x.riplay.extensions.appearancesettings.models.AppearanceSettings
import it.fast4x.riplay.extensions.appsettings.models.AppSettings

/**
 * "Zero-Config": valores que la app impone SIEMPRE, por encima de lo que haya guardado en la
 * base de datos (instalaciones anteriores, backups restaurados o cambios desde Ajustes).
 *
 * Sólo se fuerzan los campos que definen el comportamiento/diseño. El estado del usuario
 * (cuenta, cola, historial, toggles de letras, ordenaciones, etc.) no se toca.
 */
object ZeroConfig {

    fun enforce(s: AppSettings): AppSettings = s.copy(
        // ---------- Language: Spotifree is English-only ----------
        languageApp = Languages.English,
        otherLanguageApp = Languages.English,

        // ---------- Reproducción ----------
        audioQualityFormat = AudioQualityFormat.High,
        skipSilenceEnabled = false,
        volumeNormalizationEnabled = true,
        pauseBetweenSongs = PauseBetweenSongs.`0`,
        handleAudioFocusEnabled = true,
        isPauseOnVolumeZeroEnabled = false,
        persistentQueue = true,
        videoContentMode = VideoContentMode.AudioOnly,
        closeBackgroundPlayerAfterMinutes = DurationInMinutes.Disabled, // segundo plano siempre vivo
        exoPlayerDiskCacheMaxSize = ExoPlayerDiskCacheMaxSize.`4GB`,
        exoPlayerCacheLocation = ExoPlayerCacheLocation.System,

        // ---------- Diseño fijo ----------
        uiType = UiType.RiPlay,
        useSystemFont = false,
        applyFontPadding = false,
        fontType = FontType.Poppins,                         // ignorado por typographyOf (Encore)
        navigationBarPosition = NavigationBarPosition.Bottom,
        navigationBarType = NavigationBarType.IconAndText,
        playerPosition = PlayerPosition.Bottom,
        transitionEffect = TransitionEffect.Fade,
        messageType = MessageType.Modern,
        menuStyle = MenuStyle.List,
        nowPlayingIndicator = MusicAnimationType.Bars,
        keepPlayerMinimized = false,
        showUpNextSongInMiniPlayer = false,
        showFloatingIcon = false,
        showStatsInNavbar = false,
        showListenerLevels = false,
        isSnowEffectEnabled = false,
        isEnabledFullScreen = false,

        // ---------- Sin fricción al abrir ----------
        showOnboardingScreen = false,
        showAutostartPermissionDialog = false,
        showTips = false,
        checkUpdateState = CheckUpdateState.Disabled,        // build propia: no ofrecer el APK oficial
    )

    fun enforce(a: AppearanceSettings): AppearanceSettings {
        val spotify = AppearanceSettings.Spotify
        return a.copy(
            colorPaletteName = ColorPaletteName.Default,
            colorPaletteMode = ColorPaletteMode.Dark,
            thumbnailRoundness = ThumbnailRoundness.Light,

            playerType = PlayerType.Essential,
            playerInfoType = PlayerInfoType.Essential,
            playerControlsType = PlayerControlsType.Essential,
            playerPlayButtonType = PlayerPlayButtonType.Circle,
            playerTimelineType = PlayerTimelineType.ThinBar,
            playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient,
            thumbnailType = ThumbnailType.Essential,
            queueType = QueueType.Essential,
            miniPlayerType = MiniPlayerType.Modern,       // deslizar = anterior/siguiente
            backgroundProgress = BackgroundProgress.MiniPlayer,

            showTopActionsBar = true,
            showThumbnail = true,
            showRemainingSongTime = false,
            showNextSongsInPlayer = false,
            showTotalTimeQueue = false,
            carousel = false,
            noblur = true,
            blackgradient = false,
            textoutline = false,
            isGradientBackgroundEnabled = false,
            showCoverThumbnailAnimation = false,
            albumCoverRotation = false,
            statsfornerds = false,
            disableScrollingText = true,              // Spotify trunca con "…"
            playerSwapControlsWithTimeline = false,

            showButtonPlayerShuffle = true,
            showButtonPlayerLoop = true,
            showButtonPlayerAddToPlaylist = true,
            showButtonPlayerVideo = false,
            showButtonPlayerDiscover = false,
            showButtonPlayerSleepTimer = false,
            showButtonPlayerArrow = false,
            showButtonPlayerStartRadio = false,
            showButtonPlayerSystemEqualizer = false,

            audioVisualizerStyleInPlayer = AudioVisualizerStyle.NONE,
            audioVisualizerStyleInMiniPlayer = AudioVisualizerBaseStyle.NONE,
            audioVisualizerStyleInEqualizer = AudioVisualizerBaseStyle.NONE,

            customColor = spotify.customColor,
        )
    }
}
