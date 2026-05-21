package it.fast4x.riplay.extensions.scheduled.workers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.AnimatedGradient
import it.fast4x.riplay.enums.BackgroundProgress
import it.fast4x.riplay.enums.CarouselSize
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PlayerControlsType
import it.fast4x.riplay.enums.PlayerPlayButtonType
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.enums.PrevNextSongs
import it.fast4x.riplay.enums.QueueType
import it.fast4x.riplay.enums.SongsNumber
import it.fast4x.riplay.enums.SwipeAnimationNoThumbnail
import it.fast4x.riplay.enums.ThumbnailCoverType
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.enums.ThumbnailType
import it.fast4x.riplay.enums.WallpaperType
import it.fast4x.riplay.extensions.databasebackup.DatabaseBackupManager
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTION_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ACTIONS_SPACED_EVENLY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ALBUM_COVER_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ANIMATED_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.AUTO_BACKUP_FOLDER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BACKGROUND_PROGRESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLACK_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BLUR_SCALE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BOTTOM_GRADIENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.BUTTON_ZOOM_OUT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CAROUSEL_SIZE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CLICK_ON_LYRICS_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_NAME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CONTROLS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COVER_THUMBNAIL_ANIMATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EFFECT_ROTATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_WALLPAPER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.EXPANDED_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.FADING_EDGE
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ICON_LIKE_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.KEEP_PLAYER_MINIMIZED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MINI_QUEUE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NO_BLUR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NOTIFICATION_PLAYER_FIRST_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.NOTIFICATION_PLAYER_SECOND_ICON
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_BACKGROUND_COLORS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_CONTROLS_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_ENABLE_LYRICS_POPUP_MESSAGE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_SHOW_ICONS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_INFO_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_PLAY_BUTTON_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PLAYER_SWAP_CONTROLS_WITH_TIMELINE
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
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_REMAINING_SONG_TIME
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOP_ACTIONS_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TOTAL_TIME_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_ALBUM_COVER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_LYRICS_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SONGS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATS_FOR_NERDS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_ANIMATIONS_NO_THUMBNAIL
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SWIPE_UP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TAP_QUEUE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TEXT_OUTLINE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE_EX
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_FADE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_ROUNDNESS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_SPACING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TAP_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_TYPE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.THUMBNAIL_PAUSE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TIMELINE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TITLE_EXPANDED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TOP_PADDING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSPARENT_BAR
import it.fast4x.riplay.extensions.preferences.PreferenceKey.VISUALIZER_ENABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.WALLPAPER_TYPE
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "autobackup"
        const val NOTIFICATION_ID = 2
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        return try {
            Timber.d("AutoBackupWorker: Start...")

            val selectedFolderUri = context.preferences.getString(AUTO_BACKUP_FOLDER.key, "")
            val savedUri = Uri.parse(selectedFolderUri)
            val folder = DocumentFile.fromTreeUri(context, savedUri)

            if (folder == null || !folder.exists()) {
                Timber.e("AutoBackupWorker: Folder not found")
                return Result.failure()
            }

            val backupManager = DatabaseBackupManager(context, Database)
            @SuppressLint("SimpleDateFormat")
            val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
            val dbFile = folder.createFile("application/octet-stream", "riplay_${dateFormat.format(Date())}.db")

            if (dbFile == null) {
                Timber.e("AutoBackupWorker: File Database Backup not created")
                return Result.failure()
            }

            backupManager.backupDatabase(dbFile.uri)
            Timber.e("AutoBackupWorker: Backup database completed")

            val appearanceFilename = folder.createFile("text/csv", "riplay_appearance_${dateFormat.format(Date())}.csv")

            if (appearanceFilename == null) {
                Timber.e("AutoBackupWorker: File Appearance Backup not created")
                return Result.failure()
            }

            val albumCoverRotation = context.preferences.getBoolean(ALBUM_COVER_ROTATION.key, false)
            val showthumbnail = context.preferences.getBoolean(SHOW_THUMBNAIL.key, true)
            val playerBackgroundColors = context.preferences.getEnum(PLAYER_BACKGROUND_COLORS.key, PlayerBackgroundColors.BlurredCoverColor)
            val thumbnailRoundness = context.preferences.getEnum(THUMBNAIL_ROUNDNESS.key, ThumbnailRoundness.Light)
            val playerType = context.preferences.getEnum(PLAYER_TYPE.key, PlayerType.Modern)
            val queueType = context.preferences.getEnum(QUEUE_TYPE.key, QueueType.Modern)
            val noblur = context.preferences.getBoolean(NO_BLUR.key, true)
            val fadingedge = context.preferences.getBoolean(FADING_EDGE.key, false)
            val carousel = context.preferences.getBoolean(CAROUSEL.key, true)
            val keepPlayerMinimized = context.preferences.getBoolean(KEEP_PLAYER_MINIMIZED.key, false)
            val playerInfoShowIcons = context.preferences.getBoolean(PLAYER_INFO_SHOW_ICONS.key, true)
            val showTopActionsBar = context.preferences.getBoolean(SHOW_TOP_ACTIONS_BAR.key, true)
            val carouselSize = context.preferences.getEnum(CAROUSEL_SIZE.key, CarouselSize.Biggest)
            val playerControlsType = context.preferences.getEnum(PLAYER_CONTROLS_TYPE.key, PlayerControlsType.Essential)
            val playerInfoType = context.preferences.getEnum(PLAYER_INFO_TYPE.key, PlayerType.Modern)
            val transparentBackgroundActionBarPlayer = context.preferences.getBoolean(
                TRANSPARENT_BACKGROUND_PLAYER_ACTION_BAR.key,
                true
            )
            val iconLikeType = context.preferences.getEnum(ICON_LIKE_TYPE.key, IconLikeType.Essential)
            val playerSwapControlsWithTimeline = context.preferences.getBoolean(
                PLAYER_SWAP_CONTROLS_WITH_TIMELINE.key,
                false
            )
            val playerEnableLyricsPopupMessage = context.preferences.getBoolean(
                PLAYER_ENABLE_LYRICS_POPUP_MESSAGE.key,
                true
            )
            val actionspacedevenly = context.preferences.getBoolean(ACTIONS_SPACED_EVENLY.key, false)
            val thumbnailType = context.preferences.getEnum(THUMBNAIL_TYPE.key, ThumbnailType.Modern)
            val showvisthumbnail = context.preferences.getBoolean(SHOW_THUMBNAIL.key, true)
            val buttonzoomout = context.preferences.getBoolean(BUTTON_ZOOM_OUT.key, false)
            val thumbnailpause = context.preferences.getBoolean(THUMBNAIL_PAUSE.key, false)
            val showsongs = context.preferences.getEnum(SHOW_SONGS.key, SongsNumber.`2`)
            val showalbumcover = context.preferences.getBoolean(SHOW_ALBUM_COVER.key, true)
            val prevNextSongs = context.preferences.getEnum(PREV_NEXT_SONGS.key, PrevNextSongs.twosongs)
            val tapqueue = context.preferences.getBoolean(TAP_QUEUE.key, true)
            val swipeUpQueue = context.preferences.getBoolean(SWIPE_UP_QUEUE.key, true)
            val statsfornerds = context.preferences.getBoolean(STATS_FOR_NERDS.key, false)
            val transparentbar = context.preferences.getBoolean(TRANSPARENT_BAR.key, true)
            val showlyricsthumbnail = context.preferences.getBoolean(SHOW_LYRICS_THUMBNAIL.key, false)
            val blackgradient = context.preferences.getBoolean(BLACK_GRADIENT.key, false)
            val expandedplayer = context.preferences.getBoolean(EXPANDED_PLAYER.key, true)
            val playerPlayButtonType = context.preferences.getEnum(PLAYER_PLAY_BUTTON_TYPE.key, PlayerPlayButtonType.Disabled)
            val bottomgradient = context.preferences.getBoolean(BOTTOM_GRADIENT.key, false)
            val textoutline = context.preferences.getBoolean(TEXT_OUTLINE.key, false)
            val effectRotationEnabled = context.preferences.getBoolean(EFFECT_ROTATION.key, true)
            val thumbnailTapEnabled = context.preferences.getBoolean(THUMBNAIL_TAP_ENABLED.key, true)
            val showButtonPlayerAddToPlaylist = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_ADD_TO_PLAYLIST.key, true)
            val showButtonPlayerArrow = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_ARROW.key, true)
            val showButtonPlayerLoop = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_LOOP.key, true)
            val showButtonPlayerLyrics = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_LYRICS.key, true)
            val expandedplayertoggle = context.preferences.getBoolean(EXPANDED_PLAYER.key, true)
            val showButtonPlayerShuffle = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_SHUFFLE.key, true)
            val showButtonPlayerSleepTimer = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_SLEEP_TIMER.key, true)
            val showButtonPlayerMenu = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_MENU.key, true)
            val showButtonPlayerStartradio = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_START_RADIO.key, true)
            val showButtonPlayerSystemEqualizer = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_SYSTEM_EQUALIZER.key, true)
            val showButtonPlayerDiscover = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_DISCOVER.key, true)
            val showButtonPlayerVideo = context.preferences.getBoolean(SHOW_BUTTON_PLAYER_VIDEO.key, true)
            val showBackgroundLyrics = context.preferences.getBoolean(SHOW_BACKGROUND_LYRICS.key, false)
            val showTotalTimeQueue = context.preferences.getBoolean(SHOW_TOTAL_TIME_QUEUE.key, true)
            val backgroundProgress = context.preferences.getEnum(BACKGROUND_PROGRESS.key, BackgroundProgress.MiniPlayer)
            val showNextSongsInPlayer = context.preferences.getBoolean(SHOW_NEXT_SONGS_IN_PLAYER.key, true)
            val showRemainingSongTime = context.preferences.getBoolean(SHOW_REMAINING_SONG_TIME.key, true)
            val clickLyricsText = context.preferences.getBoolean(CLICK_ON_LYRICS_TEXT.key, false)
            val queueDurationExpanded = context.preferences.getBoolean(QUEUE_DURATION_EXPANDED.key, true)
            val titleExpanded = context.preferences.getBoolean(TITLE_EXPANDED.key, true)
            val timelineExpanded = context.preferences.getBoolean(TIMELINE_EXPANDED.key, true)
            val controlsExpanded = context.preferences.getBoolean(CONTROLS_EXPANDED.key, true)
            val miniQueueExpanded = context.preferences.getBoolean(MINI_QUEUE_EXPANDED.key, true)
            val statsExpanded = context.preferences.getBoolean(STATS_EXPANDED.key, true)
            val actionExpanded = context.preferences.getBoolean(ACTION_EXPANDED.key, true)
            val showCoverThumbnailAnimation = context.preferences.getBoolean(SHOW_COVER_THUMBNAIL_ANIMATION.key, false)
            val coverThumbnailAnimation = context.preferences.getEnum(COVER_THUMBNAIL_ANIMATION.key, ThumbnailCoverType.Vinyl)
            val notificationPlayerFirstIcon = context.preferences.getEnum(NOTIFICATION_PLAYER_FIRST_ICON.key, NotificationButtons.Repeat)
            val notificationPlayerSecondIcon = context.preferences.getEnum(NOTIFICATION_PLAYER_SECOND_ICON.key, NotificationButtons.Favorites)
            val enableWallpaper = context.preferences.getBoolean(ENABLE_WALLPAPER.key, false)
            val wallpaperType = context.preferences.getEnum(WALLPAPER_TYPE.key, WallpaperType.Lockscreen)
            val topPadding = context.preferences.getBoolean(TOP_PADDING.key, true)
            val animatedGradient = context.preferences.getEnum(ANIMATED_GRADIENT.key, AnimatedGradient.Linear)
            val blurStrength = context.preferences.getFloat(BLUR_SCALE.key, 25f)
            val thumbnailFadeEx = context.preferences.getFloat(THUMBNAIL_FADE_EX.key, 5f)
            val thumbnailFade = context.preferences.getFloat(THUMBNAIL_FADE.key, 5f)
            val thumbnailSpacing = context.preferences.getFloat(THUMBNAIL_SPACING.key, 0f)
            val colorPaletteName = context.preferences.getEnum(COLOR_PALETTE_NAME.key, ColorPaletteName.Dynamic)
            val colorPaletteMode = context.preferences.getEnum(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)
            val swipeAnimationNoThumbnail = context.preferences.getEnum(SWIPE_ANIMATIONS_NO_THUMBNAIL.key, SwipeAnimationNoThumbnail.Sliding)
            val showLikeButtonBackgroundPlayer = context.preferences.getBoolean(SHOW_LIKE_BUTTON_BACKGROUND_PLAYER.key, true)
            val visualizerEnabled = context.preferences.getBoolean(VISUALIZER_ENABLED.key, false)

            context.applicationContext.contentResolver.openOutputStream(appearanceFilename.uri)
                ?.use { outputStream ->
                    csvWriter().open(outputStream){
                        writeRow("SettingsType", "Name", "Parameter", "Value")
                        writeRow("Appearance", appearanceFilename, "albumCoverRotation", albumCoverRotation)
                        writeRow("Appearance", appearanceFilename, "showthumbnail", showthumbnail)
                        writeRow("Appearance", appearanceFilename, "playerBackgroundColors", playerBackgroundColors.ordinal)
                        writeRow("Appearance", appearanceFilename, "thumbnailRoundness", thumbnailRoundness.ordinal)
                        writeRow("Appearance", appearanceFilename, "playerType", playerType.ordinal)
                        writeRow("Appearance", appearanceFilename, "queueType", queueType.ordinal)
                        writeRow("Appearance", appearanceFilename, "noblur", noblur)
                        writeRow("Appearance", appearanceFilename, "fadingedge", fadingedge)
                        writeRow("Appearance", appearanceFilename, "carousel", carousel)
                        writeRow("Appearance", appearanceFilename, "carouselSize", carouselSize.ordinal)
                        writeRow("Appearance", appearanceFilename, "keepPlayerMinimized", keepPlayerMinimized)
                        writeRow("Appearance", appearanceFilename, "playerInfoShowIcons", playerInfoShowIcons)
                        writeRow("Appearance", appearanceFilename, "showTopActionsBar", showTopActionsBar)
                        writeRow("Appearance", appearanceFilename, "playerControlsType", playerControlsType.ordinal)
                        writeRow("Appearance", appearanceFilename, "playerInfoType", playerInfoType.ordinal)
                        writeRow("Appearance", appearanceFilename, "transparentBackgroundActionBarPlayer", transparentBackgroundActionBarPlayer)
                        writeRow("Appearance", appearanceFilename, "iconLikeType", iconLikeType.ordinal)
                        writeRow("Appearance", appearanceFilename, "playerSwapControlsWithTimeline", playerSwapControlsWithTimeline)
                        writeRow("Appearance", appearanceFilename, "playerEnableLyricsPopupMessage", playerEnableLyricsPopupMessage)
                        writeRow("Appearance", appearanceFilename, "actionspacedevenly", actionspacedevenly)
                        writeRow("Appearance", appearanceFilename, "thumbnailType", thumbnailType.ordinal)
                        writeRow("Appearance", appearanceFilename, "showvisthumbnail", showvisthumbnail)
                        writeRow("Appearance", appearanceFilename, "buttonzoomout", buttonzoomout)
                        writeRow("Appearance", appearanceFilename, "thumbnailpause", thumbnailpause)
                        writeRow("Appearance", appearanceFilename, "showsongs", showsongs.ordinal)
                        writeRow("Appearance", appearanceFilename, "showalbumcover", showalbumcover)
                        writeRow("Appearance", appearanceFilename, "prevNextSongs", prevNextSongs.ordinal)
                        writeRow("Appearance", appearanceFilename, "tapqueue", tapqueue)
                        writeRow("Appearance", appearanceFilename, "swipeUpQueue", swipeUpQueue)
                        writeRow("Appearance", appearanceFilename, "statsfornerds", statsfornerds)
                        writeRow("Appearance", appearanceFilename, "transparentbar", transparentbar)
                        writeRow("Appearance", appearanceFilename, "blackgradient", blackgradient)
                        writeRow("Appearance", appearanceFilename, "showlyricsthumbnail", showlyricsthumbnail)
                        writeRow("Appearance", appearanceFilename, "expandedplayer", expandedplayer)
                        writeRow("Appearance", appearanceFilename, "playerPlayButtonType", playerPlayButtonType.ordinal)
                        writeRow("Appearance", appearanceFilename, "bottomgradient", bottomgradient)
                        writeRow("Appearance", appearanceFilename, "textoutline", textoutline)
                        writeRow("Appearance", appearanceFilename, "effectRotationEnabled", effectRotationEnabled)
                        writeRow("Appearance", appearanceFilename, "thumbnailTapEnabled", thumbnailTapEnabled)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerAddToPlaylist", showButtonPlayerAddToPlaylist)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerArrow", showButtonPlayerArrow)
                        //writeRow("Appearance", appearanceFilename, "showButtonPlayerDownload", showButtonPlayerDownload)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerLoop", showButtonPlayerLoop)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerLyrics", showButtonPlayerLyrics)
                        writeRow("Appearance", appearanceFilename, "expandedplayertoggle", expandedplayertoggle)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerShuffle", showButtonPlayerShuffle)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerSleepTimer", showButtonPlayerSleepTimer)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerMenu", showButtonPlayerMenu)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerStartradio", showButtonPlayerStartradio)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerSystemEqualizer", showButtonPlayerSystemEqualizer)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerDiscover", showButtonPlayerDiscover)
                        writeRow("Appearance", appearanceFilename, "showButtonPlayerVideo", showButtonPlayerVideo)
                        writeRow("Appearance", appearanceFilename, "showBackgroundLyrics", showBackgroundLyrics)
                        writeRow("Appearance", appearanceFilename, "showTotalTimeQueue", showTotalTimeQueue)
                        writeRow("Appearance", appearanceFilename, "backgroundProgress", backgroundProgress.ordinal)
                        writeRow("Appearance", appearanceFilename, "showNextSongsInPlayer", showNextSongsInPlayer)
                        writeRow("Appearance", appearanceFilename, "showRemainingSongTime", showRemainingSongTime)
                        writeRow("Appearance", appearanceFilename, "clickLyricsText", clickLyricsText)
                        writeRow("Appearance", appearanceFilename, "queueDurationExpanded", queueDurationExpanded)
                        writeRow("Appearance", appearanceFilename, "titleExpanded", titleExpanded)
                        writeRow("Appearance", appearanceFilename, "timelineExpanded", timelineExpanded)
                        writeRow("Appearance", appearanceFilename, "controlsExpanded", controlsExpanded)
                        writeRow("Appearance", appearanceFilename, "miniQueueExpanded", miniQueueExpanded)
                        writeRow("Appearance", appearanceFilename, "statsExpanded", statsExpanded)
                        writeRow("Appearance", appearanceFilename, "actionExpanded", actionExpanded)
                        writeRow("Appearance", appearanceFilename, "showCoverThumbnailAnimation", showCoverThumbnailAnimation)
                        writeRow("Appearance", appearanceFilename, "coverThumbnailAnimation", coverThumbnailAnimation.ordinal)
                        writeRow("Appearance", appearanceFilename, "notificationPlayerFirstIcon", notificationPlayerFirstIcon.ordinal)
                        writeRow("Appearance", appearanceFilename, "notificationPlayerSecondIcon", notificationPlayerSecondIcon.ordinal)
                        writeRow("Appearance", appearanceFilename, "enableWallpaper", enableWallpaper)
                        writeRow("Appearance", appearanceFilename, "wallpaperType", wallpaperType.ordinal)
                        writeRow("Appearance", appearanceFilename, "topPadding", topPadding)
                        writeRow("Appearance", appearanceFilename, "animatedGradient", animatedGradient.ordinal)
                        writeRow("Appearance", appearanceFilename, "albumCoverRotation", albumCoverRotation)
                        writeRow("Appearance", appearanceFilename, "blurStrength", blurStrength)
                        writeRow("Appearance", appearanceFilename, "thumbnailFadeEx", thumbnailFadeEx)
                        writeRow("Appearance", appearanceFilename, "thumbnailFade", thumbnailFade)
                        writeRow("Appearance", appearanceFilename, "thumbnailSpacing", thumbnailSpacing)
                        writeRow("Appearance", appearanceFilename, "colorPaletteName", colorPaletteName.ordinal)
                        writeRow("Appearance", appearanceFilename, "colorPaletteMode", colorPaletteMode.ordinal)
                        writeRow("Appearance", appearanceFilename, "swipeAnimationNoThumbnail", swipeAnimationNoThumbnail.ordinal)
                        writeRow("Appearance", appearanceFilename, "showLikeButtonBackgroundPlayer", showLikeButtonBackgroundPlayer)
                        writeRow("Appearance", appearanceFilename, "visualizerEnabled", visualizerEnabled)
                    }
                }

            Timber.e("AutoBackupWorker: Backup appearance settings completed")

            val message = buildString {
                appendLine("Auto backup completed")
            }

            showNotification(context, message)

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "AutoBackupWorker: Error generic: ${e.message}")
            Result.retry()
        }
    }

    private fun showNotification(context: Context, message: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scheduled"
            val descriptionText = "Auto backup"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Auto backup")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}