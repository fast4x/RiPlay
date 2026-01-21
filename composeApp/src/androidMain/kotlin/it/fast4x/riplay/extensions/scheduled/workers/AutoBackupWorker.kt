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
import it.fast4x.riplay.extensions.preferences.actionExpandedKey
import it.fast4x.riplay.extensions.preferences.actionspacedevenlyKey
import it.fast4x.riplay.extensions.preferences.albumCoverRotationKey
import it.fast4x.riplay.extensions.preferences.animatedGradientKey
import it.fast4x.riplay.extensions.preferences.autoBackupFolderKey
import it.fast4x.riplay.extensions.preferences.backgroundProgressKey
import it.fast4x.riplay.extensions.preferences.blackgradientKey
import it.fast4x.riplay.extensions.preferences.blurStrengthKey
import it.fast4x.riplay.extensions.preferences.bottomgradientKey
import it.fast4x.riplay.extensions.preferences.buttonzoomoutKey
import it.fast4x.riplay.extensions.preferences.carouselKey
import it.fast4x.riplay.extensions.preferences.carouselSizeKey
import it.fast4x.riplay.extensions.preferences.clickOnLyricsTextKey
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.extensions.preferences.controlsExpandedKey
import it.fast4x.riplay.extensions.preferences.coverThumbnailAnimationKey
import it.fast4x.riplay.extensions.preferences.effectRotationKey
import it.fast4x.riplay.extensions.preferences.enableWallpaperKey
import it.fast4x.riplay.extensions.preferences.expandedplayerKey
import it.fast4x.riplay.extensions.preferences.fadingedgeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.iconLikeTypeKey
import it.fast4x.riplay.extensions.preferences.keepPlayerMinimizedKey
import it.fast4x.riplay.extensions.preferences.miniQueueExpandedKey
import it.fast4x.riplay.extensions.preferences.noblurKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerFirstIconKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerSecondIconKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerControlsTypeKey
import it.fast4x.riplay.extensions.preferences.playerEnableLyricsPopupMessageKey
import it.fast4x.riplay.extensions.preferences.playerInfoShowIconsKey
import it.fast4x.riplay.extensions.preferences.playerInfoTypeKey
import it.fast4x.riplay.extensions.preferences.playerPlayButtonTypeKey
import it.fast4x.riplay.extensions.preferences.playerSwapControlsWithTimelineKey
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
import it.fast4x.riplay.extensions.preferences.showRemainingSongTimeKey
import it.fast4x.riplay.extensions.preferences.showTopActionsBarKey
import it.fast4x.riplay.extensions.preferences.showTotalTimeQueueKey
import it.fast4x.riplay.extensions.preferences.showalbumcoverKey
import it.fast4x.riplay.extensions.preferences.showlyricsthumbnailKey
import it.fast4x.riplay.extensions.preferences.showsongsKey
import it.fast4x.riplay.extensions.preferences.showthumbnailKey
import it.fast4x.riplay.extensions.preferences.statsExpandedKey
import it.fast4x.riplay.extensions.preferences.statsfornerdsKey
import it.fast4x.riplay.extensions.preferences.swipeAnimationsNoThumbnailKey
import it.fast4x.riplay.extensions.preferences.swipeUpQueueKey
import it.fast4x.riplay.extensions.preferences.tapqueueKey
import it.fast4x.riplay.extensions.preferences.textoutlineKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeExKey
import it.fast4x.riplay.extensions.preferences.thumbnailFadeKey
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.extensions.preferences.thumbnailSpacingKey
import it.fast4x.riplay.extensions.preferences.thumbnailTapEnabledKey
import it.fast4x.riplay.extensions.preferences.thumbnailTypeKey
import it.fast4x.riplay.extensions.preferences.thumbnailpauseKey
import it.fast4x.riplay.extensions.preferences.timelineExpandedKey
import it.fast4x.riplay.extensions.preferences.titleExpandedKey
import it.fast4x.riplay.extensions.preferences.topPaddingKey
import it.fast4x.riplay.extensions.preferences.transparentBackgroundPlayerActionBarKey
import it.fast4x.riplay.extensions.preferences.transparentbarKey
import it.fast4x.riplay.extensions.preferences.visualizerEnabledKey
import it.fast4x.riplay.extensions.preferences.wallpaperTypeKey
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

            val selectedFolderUri = context.preferences.getString(autoBackupFolderKey, "")
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

            val albumCoverRotation = context.preferences.getBoolean(albumCoverRotationKey, false)
            val showthumbnail = context.preferences.getBoolean(showthumbnailKey, true)
            val playerBackgroundColors = context.preferences.getEnum(playerBackgroundColorsKey, PlayerBackgroundColors.BlurredCoverColor)
            val thumbnailRoundness = context.preferences.getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)
            val playerType = context.preferences.getEnum(playerTypeKey, PlayerType.Modern)
            val queueType = context.preferences.getEnum(queueTypeKey, QueueType.Modern)
            val noblur = context.preferences.getBoolean(noblurKey, true)
            val fadingedge = context.preferences.getBoolean(fadingedgeKey, false)
            val carousel = context.preferences.getBoolean(carouselKey, true)
            val keepPlayerMinimized = context.preferences.getBoolean(keepPlayerMinimizedKey, false)
            val playerInfoShowIcons = context.preferences.getBoolean(playerInfoShowIconsKey, true)
            val showTopActionsBar = context.preferences.getBoolean(showTopActionsBarKey, true)
            val carouselSize = context.preferences.getEnum(carouselSizeKey, CarouselSize.Biggest)
            val playerControlsType = context.preferences.getEnum(playerControlsTypeKey, PlayerControlsType.Essential)
            val playerInfoType = context.preferences.getEnum(playerInfoTypeKey, PlayerType.Modern)
            val transparentBackgroundActionBarPlayer = context.preferences.getBoolean(
                transparentBackgroundPlayerActionBarKey,
                false
            )
            val iconLikeType = context.preferences.getEnum(iconLikeTypeKey, IconLikeType.Essential)
            val playerSwapControlsWithTimeline = context.preferences.getBoolean(
                playerSwapControlsWithTimelineKey,
                false
            )
            val playerEnableLyricsPopupMessage = context.preferences.getBoolean(
                playerEnableLyricsPopupMessageKey,
                true
            )
            val actionspacedevenly = context.preferences.getBoolean(actionspacedevenlyKey, false)
            val thumbnailType = context.preferences.getEnum(thumbnailTypeKey, ThumbnailType.Modern)
            val showvisthumbnail = context.preferences.getBoolean(showthumbnailKey, true)
            val buttonzoomout = context.preferences.getBoolean(buttonzoomoutKey, false)
            val thumbnailpause = context.preferences.getBoolean(thumbnailpauseKey, false)
            val showsongs = context.preferences.getEnum(showsongsKey, SongsNumber.`2`)
            val showalbumcover = context.preferences.getBoolean(showalbumcoverKey, true)
            val prevNextSongs = context.preferences.getEnum(prevNextSongsKey, PrevNextSongs.twosongs)
            val tapqueue = context.preferences.getBoolean(tapqueueKey, true)
            val swipeUpQueue = context.preferences.getBoolean(swipeUpQueueKey, true)
            val statsfornerds = context.preferences.getBoolean(statsfornerdsKey, false)
            val transparentbar = context.preferences.getBoolean(transparentbarKey, true)
            val showlyricsthumbnail = context.preferences.getBoolean(showlyricsthumbnailKey, false)
            val blackgradient = context.preferences.getBoolean(blackgradientKey, false)
            val expandedplayer = context.preferences.getBoolean(expandedplayerKey, true)
            val playerPlayButtonType = context.preferences.getEnum(playerPlayButtonTypeKey, PlayerPlayButtonType.Disabled)
            val bottomgradient = context.preferences.getBoolean(bottomgradientKey, false)
            val textoutline = context.preferences.getBoolean(textoutlineKey, false)
            val effectRotationEnabled = context.preferences.getBoolean(effectRotationKey, true)
            val thumbnailTapEnabled = context.preferences.getBoolean(thumbnailTapEnabledKey, true)
            val showButtonPlayerAddToPlaylist = context.preferences.getBoolean(showButtonPlayerAddToPlaylistKey, true)
            val showButtonPlayerArrow = context.preferences.getBoolean(showButtonPlayerArrowKey, true)
            val showButtonPlayerLoop = context.preferences.getBoolean(showButtonPlayerLoopKey, true)
            val showButtonPlayerLyrics = context.preferences.getBoolean(showButtonPlayerLyricsKey, true)
            val expandedplayertoggle = context.preferences.getBoolean(expandedplayerKey, true)
            val showButtonPlayerShuffle = context.preferences.getBoolean(showButtonPlayerShuffleKey, true)
            val showButtonPlayerSleepTimer = context.preferences.getBoolean(showButtonPlayerSleepTimerKey, true)
            val showButtonPlayerMenu = context.preferences.getBoolean(showButtonPlayerMenuKey, true)
            val showButtonPlayerStartradio = context.preferences.getBoolean(showButtonPlayerStartRadioKey, true)
            val showButtonPlayerSystemEqualizer = context.preferences.getBoolean(showButtonPlayerSystemEqualizerKey, true)
            val showButtonPlayerDiscover = context.preferences.getBoolean(showButtonPlayerDiscoverKey, true)
            val showButtonPlayerVideo = context.preferences.getBoolean(showButtonPlayerVideoKey, true)
            val showBackgroundLyrics = context.preferences.getBoolean(showBackgroundLyricsKey, false)
            val showTotalTimeQueue = context.preferences.getBoolean(showTotalTimeQueueKey, true)
            val backgroundProgress = context.preferences.getEnum(backgroundProgressKey, BackgroundProgress.MiniPlayer)
            val showNextSongsInPlayer = context.preferences.getBoolean(showNextSongsInPlayerKey, true)
            val showRemainingSongTime = context.preferences.getBoolean(showRemainingSongTimeKey, true)
            val clickLyricsText = context.preferences.getBoolean(clickOnLyricsTextKey, false)
            val queueDurationExpanded = context.preferences.getBoolean(queueDurationExpandedKey, true)
            val titleExpanded = context.preferences.getBoolean(titleExpandedKey, true)
            val timelineExpanded = context.preferences.getBoolean(timelineExpandedKey, true)
            val controlsExpanded = context.preferences.getBoolean(controlsExpandedKey, true)
            val miniQueueExpanded = context.preferences.getBoolean(miniQueueExpandedKey, true)
            val statsExpanded = context.preferences.getBoolean(statsExpandedKey, true)
            val actionExpanded = context.preferences.getBoolean(actionExpandedKey, true)
            val showCoverThumbnailAnimation = context.preferences.getBoolean(showCoverThumbnailAnimationKey, false)
            val coverThumbnailAnimation = context.preferences.getEnum(coverThumbnailAnimationKey, ThumbnailCoverType.Vinyl)
            val notificationPlayerFirstIcon = context.preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
            val notificationPlayerSecondIcon = context.preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)
            val enableWallpaper = context.preferences.getBoolean(enableWallpaperKey, false)
            val wallpaperType = context.preferences.getEnum(wallpaperTypeKey, WallpaperType.Lockscreen)
            val topPadding = context.preferences.getBoolean(topPaddingKey, true)
            val animatedGradient = context.preferences.getEnum(animatedGradientKey, AnimatedGradient.Linear)
            val blurStrength = context.preferences.getFloat(blurStrengthKey, 25f)
            val thumbnailFadeEx = context.preferences.getFloat(thumbnailFadeExKey, 5f)
            val thumbnailFade = context.preferences.getFloat(thumbnailFadeKey, 5f)
            val thumbnailSpacing = context.preferences.getFloat(thumbnailSpacingKey, 0f)
            val colorPaletteName = context.preferences.getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
            val colorPaletteMode = context.preferences.getEnum(colorPaletteModeKey, ColorPaletteMode.Dark)
            val swipeAnimationNoThumbnail = context.preferences.getEnum(swipeAnimationsNoThumbnailKey, SwipeAnimationNoThumbnail.Sliding)
            val showLikeButtonBackgroundPlayer = context.preferences.getBoolean(showLikeButtonBackgroundPlayerKey, true)
            val visualizerEnabled = context.preferences.getBoolean(visualizerEnabledKey, false)

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