package it.fast4x.riplay.extensions.experimental.appearancepreset.models

import android.R
import it.fast4x.riplay.enums.AnimatedGradient
import it.fast4x.riplay.enums.CarouselSize
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerTimelineSize
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.PrevNextSongs
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.SongsNumber
import it.fast4x.riplay.enums.ThumbnailCoverType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType

// ── 1. PlayerSettings: snapshot tipizzato di TUTTE le variabili ───────────────
//    Questo è il "corpo" del preset. Ogni campo corrisponde esattamente
//    a una variabile Preferences del player.

data class PlayerSettings(

    val showTopActionsBar: Boolean             = true,
    val showThumbnail: Boolean                 = true,
    val playerBackgroundColors: PlayerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor,
    val blurStrength: Float                    = 50f,
    val thumbnailRoundness: ThumbnailRoundness = ThumbnailRoundness.None,
    val thumbnailType: ThumbnailType = ThumbnailType.Modern,
    val playerThumbnailSize: PlayerThumbnailSize = PlayerThumbnailSize.Big,
    val transparentBar: Boolean                = true,
    val bottomGradient: Boolean                = true,
    val expandedPlayer: Boolean                = true,
    val showLyricsThumbnail: Boolean           = false,

    val playerInfoType: PlayerInfoType = PlayerInfoType.Essential,
    val playerType: PlayerType = PlayerType.Essential,

    val playerTimelineType: PlayerTimelineType = PlayerTimelineType.ThinBar,
    val playerTimelineSize: PlayerTimelineSize = PlayerTimelineSize.Biggest,

    val playerControlsType: PlayerControlsType = PlayerControlsType.Essential,
    val playerPlayButtonType: PlayerPlayButtonType = PlayerPlayButtonType.Disabled,

    val showTotalTimeQueue: Boolean            = false,
    val showRemainingSongTime: Boolean         = true,
    val showNextSongsInPlayer: Boolean         = false,

    val colorPaletteName: ColorPaletteName = ColorPaletteName.Dynamic,
    val colorPaletteMode: ColorPaletteMode = ColorPaletteMode.System,

    val transparentBackgroundActionBarPlayer: Boolean = true,
    val actionsSpacedEvenly: Boolean           = true,
    val showButtonPlayerVideo: Boolean         = false,
    val showButtonPlayerDiscover: Boolean      = false,
    val showButtonPlayerAddToPlaylist: Boolean = true,
    val showButtonPlayerLoop: Boolean          = false,
    val showButtonPlayerShuffle: Boolean       = true,
    val showButtonPlayerLyrics: Boolean        = false,
    val expandedPlayerToggle: Boolean          = false,
    val showButtonPlayerSleepTimer: Boolean    = false,
    val visualizerEnabled: Boolean             = false,
    val showButtonPlayerArrow: Boolean         = false,
    val showButtonPlayerStartRadio: Boolean    = false,
    val showButtonPlayerMenu: Boolean          = true,
    val showButtonPlayerSystemEqualizer: Boolean = false,
    val showBackgroundLyrics: Boolean          = false,
    val showPlayerActionsBar: Boolean          = true,
    val iconLikeType: IconLikeType             = IconLikeType.Essential,
    val playerSwapControlsWithTimeline: Boolean = false,
    val showvisthumbnail: Boolean              = false,
    val buttonzoomout: Boolean                 = false,
    val thumbnailpause: Boolean                = false,
    val showsongs: SongsNumber                 = SongsNumber.`2`,
    val showalbumcover: Boolean                = true,
    val prevNextSongs: PrevNextSongs           = PrevNextSongs.twosongs,
    val tapqueue: Boolean                      = true,
    val swipeUpQueue: Boolean                  = true,
    val statsfornerds: Boolean                 = false,
    val queueType: QueueType                   = QueueType.Modern,
    val noblur: Boolean                        = true,
    val fadingedge: Boolean                    = false,
    val carousel: Boolean                      = true,
    val carouselSize: CarouselSize             = CarouselSize.Biggest,
    val keepPlayerMinimized: Boolean           = false,
    val playerInfoShowIcons: Boolean           = true,
    val queueDurationExpanded: Boolean         = true,
    val titleExpanded: Boolean                 = true,
    val timelineExpanded: Boolean              = true,
    val controlsExpanded: Boolean              = true,
    val miniQueueExpanded: Boolean             = true,
    val statsExpanded: Boolean                 = true,
    val actionExpanded: Boolean                = true,
    val showCoverThumbnailAnimation: Boolean   = false,
    val coverThumbnailAnimation: ThumbnailCoverType = ThumbnailCoverType.Vinyl,
    val topPadding: Boolean                    = true,
    val animatedGradient: AnimatedGradient     = AnimatedGradient.Linear,
    val albumCoverRotation: Boolean            = false,
    val thumbnailFadeEx: Float                 = 5f,
    val thumbnailFade: Float                   = 5f,
    val thumbnailSpacing: Float                = 0f,
    val blackgradient: Boolean                 = false,
    val textoutline: Boolean                   = false,
    val disablePlayerHorizontalSwipe: Boolean  = false,
    val showLikeButtonBackgroundPlayer: Boolean = true

) {
    companion object {

        val Modern = PlayerSettings(
            playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor,
            blurStrength = 50f,
            thumbnailType = ThumbnailType.Modern,
            playerThumbnailSize = PlayerThumbnailSize.Big,
            playerTimelineType = PlayerTimelineType.ThinBar,
            playerControlsType = PlayerControlsType.Essential,
            expandedPlayer = true,
            bottomGradient = true
        )
        val Minimal = PlayerSettings(
            showTopActionsBar = false,
            showThumbnail = false,
            playerBackgroundColors = PlayerBackgroundColors.ThemeColor,
            playerTimelineType = PlayerTimelineType.ThinBar,
            playerTimelineSize = PlayerTimelineSize.Small,
            playerControlsType = PlayerControlsType.Essential,
            playerPlayButtonType = PlayerPlayButtonType.Disabled,
            expandedPlayer = false,
            bottomGradient = false,
            actionsSpacedEvenly = false
        )

    }
}