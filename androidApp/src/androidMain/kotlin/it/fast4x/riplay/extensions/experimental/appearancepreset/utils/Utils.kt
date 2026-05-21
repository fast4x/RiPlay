package it.fast4x.riplay.extensions.experimental.appearancepreset.utils

import android.content.Context
import android.net.Uri
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
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTIONS_SPACED_EVENLY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ALBUM_COVER_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ANIMATED_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLACK_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLUR_SCALE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BOTTOM_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BUTTON_ZOOM_OUT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CONTROLS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.DISABLE_PLAYER_HORIZONTAL_SWIPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER_TOGGLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FADING_EDGE
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ICON_LIKE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINI_QUEUE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NO_BLUR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_BACKGROUND_COLORS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_CONTROLS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_SHOW_ICONS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_SWAP_CONTROLS_WITH_TIMELINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_THUMBNAIL_SIZE_L
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TIMELINE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_TYPE
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PREV_NEXT_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_DURATION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.QUEUE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BACKGROUND_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_ARROW
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_DISCOVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LOOP
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_LYRICS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_MENU
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SHUFFLE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SLEEP_TIMER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_START_RADIO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_BUTTON_PLAYER_VIDEO
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LIKE_BUTTON_BACKGROUND_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEXT_SONGS_IN_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PLAYER_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_REMAINING_SONG_TIME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOTAL_TIME_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ALBUM_COVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LYRICS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_VIS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_FOR_NERDS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_UP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TAP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TEXT_OUTLINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE_EX
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_SPACING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_PAUSE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TIMELINE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TITLE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TOP_PADDING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VISUALIZER_ENABLED
import kotlin.Boolean

fun AppearanceSettings.Companion.fromCurrentSettings(context: Context): AppearanceSettings {
    val prefs = context.preferences
    return AppearanceSettings(
        showTopActionsBar = prefs.getBoolean(SHOW_TOP_ACTIONS_BAR.key, true),
        showThumbnail = prefs.getBoolean(SHOW_THUMBNAIL.key, true),
        playerBackgroundColors = prefs.getEnum(PLAYER_BACKGROUND_COLORS.key, PlayerBackgroundColors.BlurredCoverColor),
        blurStrength = prefs.getFloat(BLUR_SCALE.key, 50f),
        thumbnailRoundness = prefs.getEnum(THUMBNAIL_ROUNDNESS.key, ThumbnailRoundness.None),
        thumbnailType = prefs.getEnum(THUMBNAIL_TYPE.key, ThumbnailType.Modern),
        playerThumbnailSize = prefs.getEnum(PLAYER_THUMBNAIL_SIZE_L.key, PlayerThumbnailSize.Big),
        transparentBar = prefs.getBoolean(TRANSPARENT_BAR.key, true),
        bottomGradient = prefs.getBoolean(BOTTOM_GRADIENT.key, true),
        expandedPlayer = prefs.getBoolean(EXPANDED_PLAYER.key, true),
        showLyricsThumbnail = prefs.getBoolean(SHOW_LYRICS_THUMBNAIL.key, false),
        playerInfoType = prefs.getEnum(PLAYER_INFO_TYPE.key, PlayerInfoType.Essential),
        playerType = prefs.getEnum(PLAYER_TYPE.key, PlayerType.Essential),
        playerTimelineType = prefs.getEnum(PLAYER_TIMELINE_TYPE.key, PlayerTimelineType.ThinBar),
        playerTimelineSize = prefs.getEnum(PLAYER_TIMELINE_SIZE.key, PlayerTimelineSize.Biggest),
        playerControlsType = prefs.getEnum(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential),
        playerPlayButtonType = prefs.getEnum(PLAYER_PLAY_BUTTON_TYPE.key, PlayerPlayButtonType.Disabled),
        showTotalTimeQueue = prefs.getBoolean(SHOW_TOTAL_TIME_QUEUE.key, false),
        showRemainingSongTime = prefs.getBoolean(SHOW_REMAINING_SONG_TIME.key, true),
        showNextSongsInPlayer = prefs.getBoolean(SHOW_NEXT_SONGS_IN_PLAYER.key, false),
        colorPaletteName = prefs.getEnum(COLOR_PALETTE_NAME.key, ColorPaletteName.Dynamic),
        colorPaletteMode = prefs.getEnum(COLOR_PALETTE_MODE.key, ColorPaletteMode.System),
        transparentBackgroundActionBarPlayer = prefs.getBoolean(TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key, true),
        actionsSpacedEvenly = prefs.getBoolean(ACTIONS_SPACED_EVENLY.key, true),
        showButtonPlayerVideo = prefs.getBoolean(SHOW_BUTTON_PLAYER_VIDEO.key, false),
        showButtonPlayerDiscover = prefs.getBoolean(SHOW_BUTTON_PLAYER_DISCOVER.key, false),
        showButtonPlayerAddToPlaylist = prefs.getBoolean(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true),
        showButtonPlayerLoop = prefs.getBoolean(SHOW_BUTTON_PLAYER_LOOP.key, false),
        showButtonPlayerShuffle = prefs.getBoolean(SHOW_BUTTON_PLAYER_SHUFFLE.key, true),
        showButtonPlayerLyrics = prefs.getBoolean(SHOW_BUTTON_PLAYER_LYRICS.key, false),
        expandedPlayerToggle = prefs.getBoolean(EXPANDED_PLAYER_TOGGLE.key, false),
        showButtonPlayerSleepTimer = prefs.getBoolean(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key, false),
        visualizerEnabled = prefs.getBoolean(VISUALIZER_ENABLED.key, false),
        showButtonPlayerArrow = prefs.getBoolean(SHOW_BUTTON_PLAYER_ARROW.key, false),
        showButtonPlayerStartRadio = prefs.getBoolean(SHOW_BUTTON_PLAYER_START_RADIO.key, false),
        showButtonPlayerMenu = prefs.getBoolean(SHOW_BUTTON_PLAYER_MENU.key, true),
        showButtonPlayerSystemEqualizer = prefs.getBoolean(SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key, false),
        showBackgroundLyrics = prefs.getBoolean(SHOW_BACKGROUND_LYRICS.key, false),
        showPlayerActionsBar = prefs.getBoolean(SHOW_PLAYER_ACTIONS_BAR.key, true),
        iconLikeType = prefs.getEnum(ICON_LIKE_TYPE.key, IconLikeType.Essential),
        playerSwapControlsWithTimeline = prefs.getBoolean(PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key, false),
        showvisthumbnail = prefs.getBoolean(SHOW_VIS_THUMBNAIL.key, false),
        buttonzoomout = prefs.getBoolean(BUTTON_ZOOM_OUT.key, false),
        thumbnailpause = prefs.getBoolean(THUMBNAIL_PAUSE.key, false),
        showsongs = prefs.getEnum(SHOW_SONGS.key, SongsNumber.`2`),
        showalbumcover = prefs.getBoolean(SHOW_ALBUM_COVER.key, true),
        prevNextSongs = prefs.getEnum(PREV_NEXT_SONGS.key, PrevNextSongs.twosongs),
        tapqueue = prefs.getBoolean(TAP_QUEUE.key, true),
        swipeUpQueue = prefs.getBoolean(SWIPE_UP_QUEUE.key, true),
        statsfornerds = prefs.getBoolean(STATS_FOR_NERDS.key, false),
        queueType = prefs.getEnum(QUEUE_TYPE.key, QueueType.Modern),
        noblur = prefs.getBoolean(NO_BLUR.key, true),
        fadingedge = prefs.getBoolean(FADING_EDGE.key, false),
        carousel = prefs.getBoolean(CAROUSEL.key, true),
        carouselSize = prefs.getEnum(CAROUSEL_SIZE.key, CarouselSize.Biggest),
        keepPlayerMinimized = prefs.getBoolean(KEEP_PLAYER_MINIMIZED.key, false),
        playerInfoShowIcons = prefs.getBoolean(PLAYER_INFO_SHOW_ICONS.key, true),
        queueDurationExpanded = prefs.getBoolean(QUEUE_DURATION_EXPANDED.key, true),
        titleExpanded = prefs.getBoolean(TITLE_EXPANDED.key, true),
        timelineExpanded = prefs.getBoolean(TIMELINE_EXPANDED.key, true),
        controlsExpanded = prefs.getBoolean(CONTROLS_EXPANDED.key, true),
        miniQueueExpanded = prefs.getBoolean(MINI_QUEUE_EXPANDED.key, true),
        statsExpanded = prefs.getBoolean(STATS_EXPANDED.key, true),
        actionExpanded = prefs.getBoolean(ACTION_EXPANDED.key, true),
        showCoverThumbnailAnimation = prefs.getBoolean(SHOW_COVER_THUMBNAIL_ANIMATION.key, false),
        coverThumbnailAnimation = prefs.getEnum(COVER_THUMBNAIL_ANIMATION.key, ThumbnailCoverType.Vinyl),
        topPadding = prefs.getBoolean(TOP_PADDING.key, true),
        animatedGradient = prefs.getEnum(ANIMATED_GRADIENT.key, AnimatedGradient.Linear),
        albumCoverRotation = prefs.getBoolean(ALBUM_COVER_ROTATION.key, false),
        thumbnailFadeEx = prefs.getFloat(THUMBNAIL_FADE_EX.key, 5f),
        thumbnailFade = prefs.getFloat(THUMBNAIL_FADE.key, 5f),
        thumbnailSpacing = prefs.getFloat(THUMBNAIL_SPACING.key, 0f),
        blackgradient = prefs.getBoolean(BLACK_GRADIENT.key, false),
        textoutline = prefs.getBoolean(TEXT_OUTLINE.key, false),
        disablePlayerHorizontalSwipe = prefs.getBoolean(DISABLE_PLAYER_HORIZONTAL_SWIPE.key, false),
        showLikeButtonBackgroundPlayer = prefs.getBoolean(SHOW_LIKE_BUTTON_BACKGROUND_PLAYER.key, true)
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
        append("showButtonPlayerMenu=$showButtonPlayerMenu&")
        append("showButtonPlayerSystemEqualizer=$showButtonPlayerSystemEqualizer&")
        append("showBackgroundLyrics=$showBackgroundLyrics&")
        append("showPlayerActionsBar=$showPlayerActionsBar&")
        append("iconLikeType=$iconLikeType&")
        append("playerSwapControlsWithTimeline=$playerSwapControlsWithTimeline&")
        append("showvisthumbnail=$showvisthumbnail&")
        append("buttonzoomout=$buttonzoomout&")
        append("thumbnailpause=$thumbnailpause&")
        append("showsongs=$showsongs&")
        append("showalbumcover=$showalbumcover&")
        append("prevNextSongs=$prevNextSongs&")
        append("tapqueue=$tapqueue&")
        append("swipeUpQueue=$swipeUpQueue&")
        append("statsfornerds=$statsfornerds&")
        append("queueType=$queueType&")
        append("noblur=$noblur&")
        append("fadingedge=$fadingedge&")
        append("carousel=$carousel&")
        append("carouselSize=$carouselSize&")
        append("keepPlayerMinimized=$keepPlayerMinimized&")
        append("playerInfoShowIcons=$playerInfoShowIcons&")
        append("queueDurationExpanded=$queueDurationExpanded&")
        append("titleExpanded=$titleExpanded&")
        append("timelineExpanded=$timelineExpanded&")
        append("controlsExpanded=$controlsExpanded&")
        append("miniQueueExpanded=$miniQueueExpanded&")
        append("statsExpanded=$statsExpanded&")
        append("actionExpanded=$actionExpanded&")
        append("showCoverThumbnailAnimation=$showCoverThumbnailAnimation&")
        append("coverThumbnailAnimation=$coverThumbnailAnimation&")
        append("topPadding=$topPadding&")
        append("animatedGradient=$animatedGradient&")
        append("albumCoverRotation=$albumCoverRotation&")
        append("thumbnailFadeEx=$thumbnailFadeEx&")
        append("thumbnailFade=$thumbnailFade&")
        append("thumbnailSpacing=$thumbnailSpacing&")
        append("blackgradient=$blackgradient&")
        append("textoutline=$textoutline&")
        append("disablePlayerHorizontalSwipe=$disablePlayerHorizontalSwipe&")
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