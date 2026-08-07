package it.fast4x.riplay.services.playback.deprecated

import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext

@UnstableApi
class NotificationProvider(
    private val customChannelId: String,
    private val customNotificationId: Int
) : MediaNotification.Provider {

    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {

        val context = appContext()
        val metadata = mediaSession.player.currentMediaItem?.mediaMetadata

        val builder = NotificationCompat.Builder(context, customChannelId)
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(metadata?.title)
            .setContentText(metadata?.artist)

        for (commandButton in mediaButtonPreferences) {
            val icon: IconCompat = IconCompat.createWithResource(context, commandButton.iconResId)

            val action = actionFactory.createMediaAction(
                mediaSession,
                icon,
                commandButton.sessionCommand.toString(),
                0
            )
            builder.addAction(action)
        }

        val notification = MediaNotification(customNotificationId, builder.build())
        onNotificationChangedCallback.onNotificationChanged(notification)

        return notification
    }

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return MediaNotification.Provider.NotificationChannelInfo(
            customChannelId,
            appContext().getString(R.string.now_playing_title)
        )
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        // Non gestiamo comandi custom via notifica per ora
        return false
    }
}