package it.fast4x.riplay.extensions.chromecast.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleObserver
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import it.fast4x.riplay.R
import timber.log.Timber

class NotificationManager(
    private val context: Context,
    private val notificationHostActivity: Class<*>?
) : AbstractYouTubePlayerListener(), LifecycleObserver {
    private val notificationId = 101
    private val channelId = "CHANNEL_ID"

    private val notificationBuilder: NotificationCompat.Builder

    init {
        initNotificationChannel()
        notificationBuilder = initNotificationBuilder()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "chromecast-youtube-player",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "sample-app"
            val notificationManager = context.getSystemService<NotificationManager?>(
                NotificationManager::class.java
            )
            if (notificationManager != null) notificationManager.createNotificationChannel(channel)
            else Timber.e(javaClass.simpleName, "Can't create notification channel")
        }
    }

    private fun initNotificationBuilder(): NotificationCompat.Builder {
        val openActivityExplicitIntent =
            Intent(context.applicationContext, notificationHostActivity)
        val togglePlaybackImplicitIntent =
            Intent(PlaybackControllerBroadcastReceiver.TOGGLE_PLAYBACK)
        val stopCastSessionImplicitIntent =
            Intent(PlaybackControllerBroadcastReceiver.STOP_CAST_SESSION)

        var flags = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE
        }

        val openActivityPendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            0,
            openActivityExplicitIntent,
            flags
        )
        val togglePlaybackPendingIntent =
            PendingIntent.getBroadcast(context, 0, togglePlaybackImplicitIntent, flags)
        val stopCastSessionPendingIntent =
            PendingIntent.getBroadcast(context, 0, stopCastSessionImplicitIntent, flags)

        return NotificationCompat.Builder(context, channelId)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_cast_connected_24dp)
            .setContentIntent(openActivityPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_play_arrow_24dp,
                "Toggle Playback",
                togglePlaybackPendingIntent
            )
            .addAction(
                R.drawable.ic_cast_connected_24dp,
                "Disconnect from chromecast",
                stopCastSessionPendingIntent
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1)
            )
    }

    fun showNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun dismissNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    @SuppressLint("SwitchIntDef")
    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
        when (state) {
            PlayerConstants.PlayerState.PLAYING -> notificationBuilder.mActions.get(0).icon =
                R.drawable.ic_pause_24dp

            else -> notificationBuilder.mActions.get(0).icon = R.drawable.ic_play_arrow_24dp
        }

        showNotification()
    }

    @SuppressLint("CheckResult")
    override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {
        notificationBuilder.setContentTitle(videoId)
        showNotification()
    }
}
