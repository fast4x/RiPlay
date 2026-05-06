package it.fast4x.riplay.extensions.experimental.appearancepreset.utils

import android.net.Uri
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerInfoType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerThumbnailSize
import it.fast4x.riplay.enums.PlayerTimelineSize
import it.fast4x.riplay.enums.PlayerTimelineType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PlayerSettings

fun PlayerSettings.toShareString(): String =
    buildString {
        append("showTopActionsBar=$showTopActionsBar&")
        append("showThumbnail=$showThumbnail&")
        append("playerBackgroundColors=${playerBackgroundColors.name}&")
        append("blurStrength=$blurStrength&")
        append("thumbnailRoundness=${thumbnailRoundness.name}&")
        append("thumbnailType=${thumbnailType.name}&")
        append("playerThumbnailSize=${playerThumbnailSize.name}&")
        append("transparentBar=$transparentBar&")
        append("bottomGradient=$bottomGradient&")
        append("expandedPlayer=$expandedPlayer&")
        append("showLyricsThumbnail=$showLyricsThumbnail&")
        append("playerInfoType=${playerInfoType.name}&")
        append("playerType=${playerType.name}&")
        append("playerTimelineType=${playerTimelineType.name}&")
        append("playerTimelineSize=${playerTimelineSize.name}&")
        append("playerControlsType=${playerControlsType.name}&")
        append("playerPlayButtonType=${playerPlayButtonType.name}&")
        append("showTotalTimeQueue=$showTotalTimeQueue&")
        append("showRemainingSongTime=$showRemainingSongTime&")
        append("showNextSongsInPlayer=$showNextSongsInPlayer&")
        append("colorPaletteName=${colorPaletteName.name}&")
        append("colorPaletteMode=${colorPaletteMode.name}&")
        append("transparentBackgroundActionBarPlayer=$transparentBackgroundActionBarPlayer&")
        append("actionsSpacedEvenly=$actionsSpacedEvenly&")
        append("showButtonPlayerVideo=$showButtonPlayerVideo&")
        append("showButtonPlayerDiscover=$showButtonPlayerDiscover&")
        append("showButtonPlayerAddToPlaylist=$showButtonPlayerAddToPlaylist&")
        append("showButtonPlayerLoop=$showButtonPlayerLoop&")
        append("showButtonPlayerShuffle=$showButtonPlayerShuffle&")
        append("showButtonPlayerLyrics=$showButtonPlayerLyrics&")
        append("expandedPlayerToggle=$expandedPlayerToggle&")
        append("showButtonPlayerSleepTimer=$showButtonPlayerSleepTimer&")
        append("visualizerEnabled=$visualizerEnabled&")
        append("showButtonPlayerArrow=$showButtonPlayerArrow&")
        append("showButtonPlayerStartRadio=$showButtonPlayerStartRadio&")
        append("showButtonPlayerMenu=$showButtonPlayerMenu")
    }.let { Uri.encode(it) }

private inline fun <reified T : Enum<T>> Map<String, String>.enum(
    key: String,
    default: T
): T = get(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

fun PlayerSettings.Companion.fromShareString(encoded: String): PlayerSettings {
    val params = Uri.decode(encoded)
        .split("&")
        .mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx == -1) null else pair.substring(0, idx) to pair.substring(idx + 1)
        }
        .toMap()

    fun bool(key: String, default: Boolean) =
        params[key]?.toBooleanStrictOrNull() ?: default
    fun float(key: String, default: Float) =
        params[key]?.toFloatOrNull() ?: default

    return PlayerSettings(
        showTopActionsBar = bool("showTopActionsBar", true),
        showThumbnail = bool("showThumbnail", true),
        playerBackgroundColors = params.enum(
            "playerBackgroundColors",
            PlayerBackgroundColors.BlurredCoverColor
        ),
        blurStrength = float("blurStrength", 50f),
        thumbnailRoundness = params.enum("thumbnailRoundness", ThumbnailRoundness.None),
        thumbnailType = params.enum("thumbnailType", ThumbnailType.Modern),
        playerThumbnailSize = params.enum("playerThumbnailSize", PlayerThumbnailSize.Big),
        transparentBar = bool("transparentBar", true),
        bottomGradient = bool("bottomGradient", true),
        expandedPlayer = bool("expandedPlayer", true),
        showLyricsThumbnail = bool("showLyricsThumbnail", false),
        playerInfoType = params.enum("playerInfoType", PlayerInfoType.Essential),
        playerType = params.enum("playerType", PlayerType.Essential),
        playerTimelineType = params.enum("playerTimelineType", PlayerTimelineType.ThinBar),
        playerTimelineSize = params.enum("playerTimelineSize", PlayerTimelineSize.Biggest),
        playerControlsType = params.enum("playerControlsType", PlayerControlsType.Essential),
        playerPlayButtonType = params.enum("playerPlayButtonType", PlayerPlayButtonType.Disabled),
        showTotalTimeQueue = bool("showTotalTimeQueue", false),
        showRemainingSongTime = bool("showRemainingSongTime", true),
        showNextSongsInPlayer = bool("showNextSongsInPlayer", false),
        colorPaletteName = params.enum("colorPaletteName", ColorPaletteName.Dynamic),
        colorPaletteMode = params.enum("colorPaletteMode", ColorPaletteMode.System),
        transparentBackgroundActionBarPlayer = bool("transparentBackgroundActionBarPlayer", true),
        actionsSpacedEvenly = bool("actionsSpacedEvenly", true),
        showButtonPlayerVideo = bool("showButtonPlayerVideo", false),
        showButtonPlayerDiscover = bool("showButtonPlayerDiscover", false),
        showButtonPlayerAddToPlaylist = bool("showButtonPlayerAddToPlaylist", true),
        showButtonPlayerLoop = bool("showButtonPlayerLoop", false),
        showButtonPlayerShuffle = bool("showButtonPlayerShuffle", true),
        showButtonPlayerLyrics = bool("showButtonPlayerLyrics", false),
        expandedPlayerToggle = bool("expandedPlayerToggle", false),
        showButtonPlayerSleepTimer = bool("showButtonPlayerSleepTimer", false),
        visualizerEnabled = bool("visualizerEnabled", false),
        showButtonPlayerArrow = bool("showButtonPlayerArrow", false),
        showButtonPlayerStartRadio = bool("showButtonPlayerStartRadio", false),
        showButtonPlayerMenu = bool("showButtonPlayerMenu", true),
    )
}