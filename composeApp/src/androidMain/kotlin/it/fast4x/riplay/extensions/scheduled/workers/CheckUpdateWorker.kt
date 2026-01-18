package it.fast4x.riplay.extensions.scheduled.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.getAvailableUpdateInfo
import it.fast4x.riplay.utils.getVersionCode
import timber.log.Timber
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class CheckUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "checkUpdate"
        const val NOTIFICATION_ID = 2
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        return try {
            Timber.d("CheckUpdateWorker: Start...")

            val client = OkHttpClient()
            val urlVersionCode =
                "https://raw.githubusercontent.com/fast4x/RiPlay/main/updatedVersion/updatedVersionCode.ver"

            val request = Request.Builder().url(urlVersionCode).build()
            val response: Response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.e("CheckUpdateWorker: Download failed ${response.code}")
                return Result.retry()
            }

            val responseData = response.body?.string()

            if (responseData != null) {
                try {
                    val file = File(context.filesDir, "UpdatedVersionCode.ver")
                    file.writeText(responseData)
                    Timber.d("CheckUpdateWorker: File updated successfully with new data")
                } catch (e: Exception) {
                    Timber.e(e, "CheckUpdateWorker: Error writing file")
                    return Result.failure()
                }
            } else {
                Timber.e("CheckUpdateWorker: Response body is null")
                return Result.retry()
            }

            val (updatedProductName, updatedVersionName, updatedVersionCode) = getAvailableUpdateInfo()

            Timber.d("CheckUpdateWorker: updatedVersionName $updatedVersionName updatedProductName $updatedProductName updatedVersionCode $updatedVersionCode")


            if (updatedVersionCode <= getVersionCode()) {
                Timber.d("CheckUpdateWorker: No new version available")
                return Result.success()
            }


            val message = buildString {
                appendLine("New version available: $updatedVersionName")
            }

            showNotification(context, message)

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "CheckUpdateWorker: Error generic: ${e.message}")
            Result.retry()
        }
    }

    private fun showNotification(context: Context, message: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scheduled"
            val descriptionText = "Check update"
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
            .setContentTitle("Check update")
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