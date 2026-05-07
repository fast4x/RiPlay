package it.fast4x.riplay.extensions.experimental.appearancepreset.utils

import android.content.Context
import android.net.Uri
import androidx.work.impl.model.Preference
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
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.preferences.actionExpandedKey
import it.fast4x.riplay.extensions.preferences.actionspacedevenlyKey
import it.fast4x.riplay.extensions.preferences.albumCoverRotationKey
import it.fast4x.riplay.extensions.preferences.animatedGradientKey
import it.fast4x.riplay.extensions.preferences.blackgradientKey
import it.fast4x.riplay.extensions.preferences.blurStrengthKey
import it.fast4x.riplay.extensions.preferences.bottomgradientKey
import it.fast4x.riplay.extensions.preferences.buttonzoomoutKey
import it.fast4x.riplay.extensions.preferences.carouselKey
import it.fast4x.riplay.extensions.preferences.carouselSizeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.extensions.preferences.controlsExpandedKey
import it.fast4x.riplay.extensions.preferences.coverThumbnailAnimationKey
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.expandedplayerKey
import it.fast4x.riplay.extensions.preferences.expandedplayertoggleKey
import it.fast4x.riplay.extensions.preferences.fadingedgeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.getEnumNew
import it.fast4x.riplay.extensions.preferences.iconLikeTypeKey
import it.fast4x.riplay.extensions.preferences.keepPlayerMinimizedKey
import it.fast4x.riplay.extensions.preferences.miniQueueExpandedKey
import it.fast4x.riplay.extensions.preferences.noblurKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerControlsTypeKey
import it.fast4x.riplay.extensions.preferences.playerInfoShowIconsKey
import it.fast4x.riplay.extensions.preferences.playerInfoTypeKey
import it.fast4x.riplay.extensions.preferences.playerPlayButtonTypeKey
import it.fast4x.riplay.extensions.preferences.playerSwapControlsWithTimelineKey
import it.fast4x.riplay.extensions.preferences.playerThumbnailSizeLKey
import it.fast4x.riplay.extensions.preferences.playerTimelineSizeKey
import it.fast4x.riplay.extensions.preferences.playerTimelineTypeKey
import it.fast4x.riplay.extensions.preferences.playerTypeKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.prevNextSongsKey
import it.fast4x.riplay.extensions.preferences.queueDurationExpandedKey
import it.fast4x.riplay.extensions.preferences.queueTypeKey
import it.fast4x.riplay.extensions.preferences.showBackgroundLyricsKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerAddToPlaylistKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerArrowKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerDiscoverKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerLoopKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerLyricsKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerMenuKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerShuffleKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerSleepTimerKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerStartRadioKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerSystemEqualizerKey
import it.fast4x.riplay.extensions.preferences.showButtonPlayerVideoKey
import it.fast4x.riplay.extensions.preferences.showCoverThumbnailAnimationKey
import it.fast4x.riplay.extensions.preferences.showLikeButtonBackgroundPlayerKey
import it.fast4x.riplay.extensions.preferences.showNextSongsInPlayerKey
import it.fast4x.riplay.extensions.preferences.showPlayerActionsBarKey
import it.fast4x.riplay.extensions.preferences.showRemainingSongTimeKey
import it.fast4x.riplay.extensions.preferences.showTopActionsBarKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.extensions.preferences.showalbumcoverKey
import it.fast4x.riplay.extensions.preferences.showlyricsthumbnailKey
import it.fast4x.riplay.extensions.preferences.showsongsKey
import it.fast4x.riplay.extensions.preferences.showthumbnailKey
import it.fast4x.riplay.extensions.preferences.showvisthumbnailKey
import it.fast4x.riplay.extensions.preferences.statsExpandedKey
import it.fast4x.riplay.extensions.preferences.statsfornerdsKey
import it.fast4x.riplay.extensions.preferences.swipeUpQueueKey
import it.fast4x.riplay.extensions.preferences.tapqueueKey
import it.fast4x.riplay.extensions.preferences.textoutlineKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeExKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.thumbnailSpacingKey
import it.fast4x.riplay.extensions.preferences.thumbnailTypeKey
import it.fast4x.riplay.extensions.preferences.thumbnailpauseKey
import it.fast4x.riplay.extensions.preferences.timelineExpandedKey
import it.fast4x.riplay.extensions.preferences.titleExpandedKey
import it.fast4x.riplay.extensions.preferences.topPaddingKey
import it.fast4x.riplay.extensions.preferences.transparentBackgroundPlayerActionBarKey
import it.fast4x.riplay.extensions.preferences.transparentbarKey
import it.fast4x.riplay.extensions.preferences.visualizerEnabledKey
import kotlin.Boolean

fun AppearanceSettings.fromCurrentSettings(context: Context): AppearanceSettings {
    val prefs = context.preferences
    return AppearanceSettings(
        showTopActionsBar = prefs.getBoolean(showTopActionsBarKey, true),
        showThumbnail = prefs.getBoolean(showthumbnailKey, true),
        playerBackgroundColors = prefs.getEnum(playerBackgroundColorsKey, PlayerBackgroundColors.BlurredCoverColor),
        blurStrength = prefs.getFloat(blurStrengthKey, 50f),
        thumbnailRoundness = prefs.getEnum(thumbnailRoundnessKey, ThumbnailRoundness.None),
        thumbnailType = prefs.getEnum(thumbnailTypeKey, ThumbnailType.Modern),
        playerThumbnailSize = prefs.getEnum(playerThumbnailSizeLKey, PlayerThumbnailSize.Big),
        transparentBar = prefs.getBoolean(transparentbarKey, true),
        bottomGradient = prefs.getBoolean(bottomgradientKey, true),
        expandedPlayer = prefs.getBoolean(expandedplayerKey, true),
        showLyricsThumbnail = prefs.getBoolean(showlyricsthumbnailKey, false),
        playerInfoType = prefs.getEnum(playerInfoTypeKey, PlayerInfoType.Essential),
        playerType = prefs.getEnum(playerTypeKey, PlayerType.Essential),
        playerTimelineType = prefs.getEnum(playerTimelineTypeKey, PlayerTimelineType.ThinBar),
        playerTimelineSize = prefs.getEnum(playerTimelineSizeKey, PlayerTimelineSize.Biggest),
        playerControlsType = prefs.getEnum(playerControlsTypeKey, PlayerControlsType.Essential),
        playerPlayButtonType = prefs.getEnum(playerPlayButtonTypeKey, PlayerPlayButtonType.Disabled),
        showTotalTimeQueue = prefs.getBoolean(showTotalTimeQueueKey, false),
        showRemainingSongTime = prefs.getBoolean(showRemainingSongTimeKey, true),
        showNextSongsInPlayer = prefs.getBoolean(showNextSongsInPlayerKey, false),
        colorPaletteName = prefs.getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic),
        colorPaletteMode = prefs.getEnum(colorPaletteModeKey, ColorPaletteMode.System),
        transparentBackgroundActionBarPlayer = prefs.getBoolean(transparentBackgroundPlayerActionBarKey, true),
        actionsSpacedEvenly = prefs.getBoolean(actionspacedevenlyKey, true),
        showButtonPlayerVideo = prefs.getBoolean(showButtonPlayerVideoKey, false),
        showButtonPlayerDiscover = prefs.getBoolean(showButtonPlayerDiscoverKey, false),
        showButtonPlayerAddToPlaylist = prefs.getBoolean(showButtonPlayerAddToPlaylistKey, true),
        showButtonPlayerLoop = prefs.getBoolean(showButtonPlayerLoopKey, false),
        showButtonPlayerShuffle = prefs.getBoolean(showButtonPlayerShuffleKey, true),
        showButtonPlayerLyrics = prefs.getBoolean(showButtonPlayerLyricsKey, false),
        expandedPlayerToggle = prefs.getBoolean(expandedplayertoggleKey, false),
        showButtonPlayerSleepTimer = prefs.getBoolean(showButtonPlayerSleepTimerKey, false),
        visualizerEnabled = prefs.getBoolean(visualizerEnabledKey, false),
        showButtonPlayerArrow = prefs.getBoolean(showButtonPlayerArrowKey, false),
        showButtonPlayerStartRadio = prefs.getBoolean(showButtonPlayerStartRadioKey, false),
        showButtonPlayerMenu = prefs.getBoolean(showButtonPlayerMenuKey, true),
        showButtonPlayerSystemEqualizer = prefs.getBoolean(showButtonPlayerSystemEqualizerKey, false),
        showBackgroundLyrics = prefs.getBoolean(showBackgroundLyricsKey, false),
        showPlayerActionsBar = prefs.getBoolean(showPlayerActionsBarKey, true),
        iconLikeType = prefs.getEnum(iconLikeTypeKey, IconLikeType.Essential),
        playerSwapControlsWithTimeline = prefs.getBoolean(playerSwapControlsWithTimelineKey, false),
        showvisthumbnail = prefs.getBoolean(showvisthumbnailKey, false),
        buttonzoomout = prefs.getBoolean(buttonzoomoutKey, false),
        thumbnailpause = prefs.getBoolean(thumbnailpauseKey, false),
        showsongs = prefs.getEnum(showsongsKey, SongsNumber.`2`),
        showalbumcover = prefs.getBoolean(showalbumcoverKey, true),
        prevNextSongs = prefs.getEnum(prevNextSongsKey, PrevNextSongs.twosongs),
        tapqueue = prefs.getBoolean(tapqueueKey, true),
        swipeUpQueue = prefs.getBoolean(swipeUpQueueKey, true),
        statsfornerds = prefs.getBoolean(statsfornerdsKey, false),
        queueType = prefs.getEnum(queueTypeKey, QueueType.Modern),
        noblur = prefs.getBoolean(noblurKey, true),
        fadingedge = prefs.getBoolean(fadingedgeKey, false),
        carousel = prefs.getBoolean(carouselKey, true),
        carouselSize = prefs.getEnum(carouselSizeKey, CarouselSize.Biggest),
        keepPlayerMinimized = prefs.getBoolean(keepPlayerMinimizedKey, false),
        playerInfoShowIcons = prefs.getBoolean(playerInfoShowIconsKey, true),
        queueDurationExpanded = prefs.getBoolean(queueDurationExpandedKey, true),
        titleExpanded = prefs.getBoolean(titleExpandedKey, true),
        timelineExpanded = prefs.getBoolean(timelineExpandedKey, true),
        controlsExpanded = prefs.getBoolean(controlsExpandedKey, true),
        miniQueueExpanded = prefs.getBoolean(miniQueueExpandedKey, true),
        statsExpanded = prefs.getBoolean(statsExpandedKey, true),
        actionExpanded = prefs.getBoolean(actionExpandedKey, true),
        showCoverThumbnailAnimation = prefs.getBoolean(showCoverThumbnailAnimationKey, false),
        coverThumbnailAnimation = prefs.getEnum(coverThumbnailAnimationKey, ThumbnailCoverType.Vinyl),
        topPadding = prefs.getBoolean(topPaddingKey, true),
        animatedGradient = prefs.getEnum(animatedGradientKey, AnimatedGradient.Linear),
        albumCoverRotation = prefs.getBoolean(albumCoverRotationKey, false),
        thumbnailFadeEx = prefs.getFloat(thumbnailFadeExKey, 5f),
        thumbnailFade = prefs.getFloat(thumbnailFadeKey, 5f),
        thumbnailSpacing = prefs.getFloat(thumbnailSpacingKey, 0f),
        blackgradient = prefs.getBoolean(blackgradientKey, false),
        textoutline = prefs.getBoolean(textoutlineKey, false),
        disablePlayerHorizontalSwipe = prefs.getBoolean(disablePlayerHorizontalSwipeKey, false),
        showLikeButtonBackgroundPlayer = prefs.getBoolean(showLikeButtonBackgroundPlayerKey, true)
    )
}

fun AppearanceSettings.toShareString(): String =
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
        append("showButtonPlayerSystemEqualizer=$showButtonPlayerSystemEqualizer")
        append("showBackgroundLyrics=$showBackgroundLyrics")
        append("showPlayerActionsBar=$showPlayerActionsBar")
        append("iconLikeType=$iconLikeType")
        append("playerSwapControlsWithTimeline=$playerSwapControlsWithTimeline")
        append("showvisthumbnail=$showvisthumbnail")
        append("buttonzoomout=$buttonzoomout")
        append("thumbnailpause=$thumbnailpause")
        append("showsongs=$showsongs")
        append("showalbumcover=$showalbumcover")
        append("prevNextSongs=$prevNextSongs")
        append("tapqueue=$tapqueue")
        append("swipeUpQueue=$swipeUpQueue")
        append("statsfornerds=$statsfornerds")
        append("queueType=$queueType")
        append("noblur=$noblur")
        append("fadingedge=$fadingedge")
        append("carousel=$carousel")
        append("carouselSize=$carouselSize")
        append("keepPlayerMinimized=$keepPlayerMinimized")
        append("playerInfoShowIcons=$playerInfoShowIcons")
        append("queueDurationExpanded=$queueDurationExpanded")
        append("titleExpanded=$titleExpanded")
        append("timelineExpanded=$timelineExpanded")
        append("controlsExpanded=$controlsExpanded")
        append("miniQueueExpanded=$miniQueueExpanded")
        append("statsExpanded=$statsExpanded")
        append("actionExpanded=$actionExpanded")
        append("showCoverThumbnailAnimation=$showCoverThumbnailAnimation")
        append("coverThumbnailAnimation=$coverThumbnailAnimation")
        append("topPadding=$topPadding")
        append("animatedGradient=$animatedGradient")
        append("albumCoverRotation=$albumCoverRotation")
        append("thumbnailFadeEx=$thumbnailFadeEx")
        append("thumbnailFade=$thumbnailFade")
        append("thumbnailSpacing=$thumbnailSpacing")
        append("blackgradient=$blackgradient")
        append("textoutline=$textoutline")
        append("disablePlayerHorizontalSwipe=$disablePlayerHorizontalSwipe")
        append("showLikeButtonBackgroundPlayer=$showLikeButtonBackgroundPlayer")
    }.let { Uri.encode(it) }

private inline fun <reified T : Enum<T>> Map<String, String>.enum(
    key: String,
    default: T
): T = get(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

fun AppearanceSettings.Companion.fromShareString(encoded: String): AppearanceSettings {
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

    return AppearanceSettings(
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
        showButtonPlayerSystemEqualizer = bool("showButtonPlayerSystemEqualizer", false),
        showBackgroundLyrics = bool("showBackgroundLyrics", false),
        showPlayerActionsBar = bool("showPlayerActionsBar", true),
        iconLikeType = params.enum("iconLikeType", IconLikeType.Essential),
        playerSwapControlsWithTimeline = bool("playerSwapControlsWithTimeline", false),
        showvisthumbnail = bool("showvisthumbnail", false),
        buttonzoomout = bool("buttonzoomout", false),
        thumbnailpause = bool("thumbnailpause", false),
        showsongs = params.enum("showsongs", SongsNumber.`2`),
        showalbumcover = bool("showalbumcover", true),
        prevNextSongs = params.enum("prevNextSongs", PrevNextSongs.twosongs),
        tapqueue = bool("tapqueue", true),
        swipeUpQueue = bool("swipeUpQueue", true),
        statsfornerds = bool("statsfornerds", false),
        queueType = params.enum("queueType", QueueType.Modern),
        noblur = bool("noblur", true),
        fadingedge = bool("fadingedge", false),
        carousel = bool("carousel", true),
        carouselSize = params.enum("carouselSize", CarouselSize.Biggest),
        keepPlayerMinimized = bool("keepPlayerMinimized", false),
        playerInfoShowIcons = bool("playerInfoShowIcons", true),
        queueDurationExpanded = bool("queueDurationExpanded", true),
        titleExpanded = bool("titleExpanded", true),
        timelineExpanded = bool("timelineExpanded", true),
        controlsExpanded = bool("controlsExpanded", true),
        miniQueueExpanded = bool("miniQueueExpanded", true),
        statsExpanded = bool("statsExpanded", true),
        actionExpanded = bool("actionExpanded", true),
        showCoverThumbnailAnimation = bool("showCoverThumbnailAnimation", false),
        coverThumbnailAnimation = params.enum("coverThumbnailAnimation", ThumbnailCoverType.Vinyl),
        topPadding = bool("topPadding", true),
        animatedGradient = params.enum("animatedGradient", AnimatedGradient.Linear),
        albumCoverRotation = bool("albumCoverRotation", false),
        thumbnailFadeEx = float("thumbnailFadeEx", 5f),
        thumbnailFade = float("thumbnailFade", 5f),
        thumbnailSpacing = float("thumbnailSpacing", 0f),
        blackgradient = bool("blackgradient", false),
        textoutline = bool("textoutline", false),
        disablePlayerHorizontalSwipe = bool("disablePlayerHorizontalSwipe", false),
        showLikeButtonBackgroundPlayer = bool("showLikeButtonBackgroundPlayer", true)
    )
}