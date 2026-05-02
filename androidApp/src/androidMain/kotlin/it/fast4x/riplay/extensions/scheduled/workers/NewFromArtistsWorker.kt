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
import it.fast4x.environment.Environment
import it.fast4x.environment.requests.discoverPage
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import kotlinx.coroutines.flow.first
import timber.log.Timber

class NewFromArtistsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "checkNewFromArtists"
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        try {
            Timber.d("NewFromArtistsWorker: Start...")

            val preferitesArtists = Database.preferitesArtistsByName().first()

            Timber.d("NewFromArtistsWorker: Found ${preferitesArtists.size} favorites artists.")
            preferitesArtists.forEach { Timber.d("NewFromArtistsWorker: DB Artist: ${it.name}") }

            if (preferitesArtists.isEmpty()) {
                Timber.d("NewFromArtistsWorker: No favorites artists, end.")
                return Result.success()
            }

            val discoverPage = Environment.discoverPage().getOrNull()
            Timber.d("NewFromArtistsWorker: Result API: $discoverPage")


            if (discoverPage == null) {
                Timber.e("NewFromArtistsWorker: discoverPage is NULL (Error API)")
                return Result.retry()
            }

            Timber.d("NewFromArtistsWorker: API result ${discoverPage.newReleaseAlbums.size} album.")

            val newReleaseAlbumsFiltered = mutableListOf<Environment.AlbumItem>()
            val preferredNames = preferitesArtists.map { it.name }.toSet()

            discoverPage.newReleaseAlbums.forEach { album ->

                val apiAuthorsNames = album.authors?.map { it.name } ?: emptyList()

                val match = apiAuthorsNames.any { apiName ->

                    preferredNames.any { dbName ->
                        apiName?.contains(dbName.toString(), ignoreCase = true) == true
                    }
                }

                Timber.d("NewFromArtistsWorker: Album by api: ${album.title} (${apiAuthorsNames})")

                if (match) {
                    newReleaseAlbumsFiltered.add(album)
                    Timber.d("NewFromArtistsWorker: Found MATCH: ${album.title} (${apiAuthorsNames})")
                }
            }

            Timber.d("NewFromArtistsWorker: Filter end. Found ${newReleaseAlbumsFiltered.size} album.")


            if (newReleaseAlbumsFiltered.isEmpty()) {
                Timber.d("NewFromArtistsWorker: No match found within DB and API.")
                return Result.success()
            }

            val message = buildString {
                appendLine("New from your artists: ${newReleaseAlbumsFiltered.size}")
                newReleaseAlbumsFiltered.forEach { album ->
                    val authors = album.authors?.joinToString { it.name.toString() } ?: "Unknown"
                    appendLine("- ${album.title} by $authors")
                }
            }

            showNotification(context, message)

            return Result.success()

        } catch (e: Exception) {
            Timber.e(e, "NewFromArtistsWorker: Error generic: ${e.message}")
            return Result.retry()
        }
    }

    private fun showNotification(context: Context, message: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scheduled"
            val descriptionText = "Check new from artists"
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

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(message)
            .setBigContentTitle("New releases")
            .setSummaryText("Click to see details")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("New releases")
            .setContentText("Completed...")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}