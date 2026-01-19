package it.fast4x.riplay.extensions.scheduled.workers

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
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.preferences.autoBackupFolderKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.getAvailableUpdateInfo
import it.fast4x.riplay.utils.getVersionCode
import timber.log.Timber
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

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

            if (folder != null && folder.exists()) {

            }

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