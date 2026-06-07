package it.fast4x.riplay.extensions.scheduled.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.utils.ArtistDiscographyType
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.ArtistDiscography
import it.fast4x.riplay.utils.asAlbum
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber

class NewFromArtistsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "checkNewFromArtists"
        const val NOTIFICATION_ID = 1
    }

    data class ArtistReleases(val artistName: String, val albums: List<Album>, val singles: List<Album>)

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun doWork(): Result {
        val context = applicationContext

        try {
            Timber.d("NewFromArtistsWorker: Start...")

            val preferitesArtists = Database.preferitesArtistsByName().first()

            //Timber.d("NewFromArtistsWorker: Found ${preferitesArtists.size} favorites artists.")
            //preferitesArtists.forEach { Timber.d("NewFromArtistsWorker: DB Artist: ${it.name}") }

            if (preferitesArtists.isEmpty()) {
                Timber.d("NewFromArtistsWorker: No favorites artists, end.")
                return Result.success()
            }

            val newReleasesMap = mutableMapOf<String, ArtistReleases>()

            preferitesArtists.forEach { artist ->
                val onlineAlbumsDiscography = EnvironmentExt.getArtistDiscography(artist.id).getOrNull()
                Timber.d("NewFromArtistsWorker: onlineAlbumsDiscography: $onlineAlbumsDiscography")

                val onlineSinglesDiscography = EnvironmentExt.getArtistDiscography(artist.id,
                    ArtistDiscographyType.Single).getOrNull()
                Timber.d("NewFromArtistsWorker: onlineSinglesDiscography: $onlineSinglesDiscography")

                // Salta al prossimo se non ci sono album nè singoli
                if (onlineAlbumsDiscography == null && onlineSinglesDiscography == null) {
                    return@forEach
                }

                val localArtistDiscography = Database.getArtistDiscography(artist.id).first()
                    ?: ArtistDiscography(artist.id, emptyList(), emptyList())
                Timber.d("NewFromArtistsWorker: localArtistDiscography: $localArtistDiscography")

                // Se la chiamata online fallisce (null), mantengo i dati locali. Altrimenti mappo quelli online.
                val currentAlbums = if (onlineAlbumsDiscography != null) {
                    onlineAlbumsDiscography.items?.filterIsInstance<Environment.AlbumItem>()?.map { it.asAlbum } ?: emptyList()
                } else {
                    localArtistDiscography.albums // Conservo i locali!
                }

                val currentSingles = if (onlineSinglesDiscography != null) {
                    onlineSinglesDiscography.items?.filterIsInstance<Environment.AlbumItem>()?.map { it.asAlbum } ?: emptyList()
                } else {
                    localArtistDiscography.singles // Conservo i locali!
                }

                // Calcolo le novità REALI (solo se la chiamata è andata a buon fine)
                val newAlbums = currentAlbums subtract localArtistDiscography.albums.toSet()
                val newSingles = currentSingles subtract localArtistDiscography.singles.toSet()

                if (newAlbums.isNotEmpty() || newSingles.isNotEmpty()) {
                    newReleasesMap[artist.id] = ArtistReleases(
                        artistName = artist.name.toString(),
                        albums = newAlbums.toList(),
                        singles = newSingles.toList()
                    )
                }

                // Salvo su database la lista aggiornata di album e singoli
                Database.insert(ArtistDiscography(artist.id, currentAlbums, currentSingles))

            }

            Timber.d("NewFromArtistsWorker: newReleasesMap: $newReleasesMap")

            if (newReleasesMap.isNotEmpty()) {
                //showGroupedNotifications(context, newReleasesMap)
                showNotification(context, newReleasesMap)
            }

            return Result.success()

        } catch (e: Exception) {
            Timber.e(e, "NewFromArtistsWorker: Error generic: ${e.message}")
            return Result.retry()
        }
    }

    private fun showNotification(
        context: Context,
        newReleasesMap: Map<String, ArtistReleases>
    ) {
        // 1. Controllo permessi per Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Timber.w("NewFromArtistsWorker: Notifications permission not granted, skipping.")
                return
            }
        }

        // 2. Creazione del Canale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.resources.getString(R.string.event_notify_channel_name_favorites_artists_updates)
            val descriptionText = context.resources.getString(R.string.event_notify_channel_desc_check_new_releases_from_favorite_artists)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Intent per aprire l'app al click
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. Calcoli per i testi
        val totalReleases = newReleasesMap.values.sumOf { it.albums.size + it.singles.size }
        val totalArtists = newReleasesMap.size

        val bigTextMessage = buildString {
            newReleasesMap.forEach { (_, releases) ->
                // Intestazione dell'artista
                appendLine("🎤 ${releases.artistName}:")

                releases.albums.forEach { album ->
                    appendLine("↳ 🎧 ${album.title}")
                }

                releases.singles.forEach { single ->
                    appendLine("↳ 🎵 ${single.title}")
                }

                appendLine() // Spazio vuoto prima del prossimo artista
            }
        }.trimEnd()

        // Testo breve per la notifica collassata
        val contentText = context.getString(
            R.string.event_total_new_releases_from_artists,
            totalReleases,
            totalArtists
        )

        // 6. Impostiamo lo stile BigTextStyle
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bigTextMessage)
            .setBigContentTitle(context.resources.getString(R.string.event_new_release))
            .setSummaryText(context.resources.getString(R.string.event_from_your_favorites_artists))

        // 7. Costruiamo la notifica
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(context.resources.getString(R.string.event_new_release))
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // 8. Lancio della notifica
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun showGroupedNotifications(
        context: Context,
        newReleasesMap: Map<String, ArtistReleases>
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                Timber.w("NewFromArtistsWorker: Notifications permission not granted, skipping notification.")
                return // Esci senza mostrare la notifica
            }
        }

        val GROUP_KEY_NEW_RELEASES = "it.fast4x.riplay.new_releases_group"
        var notificationIdCounter = 1001 // ID dinamici per ogni notifica

        // 1. Crea il Canale (se non esiste)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Artist Updates",
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH per farle apparire come Heads-up
            ).apply {
                description = "Notifications for new releases from your favorite artists"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }


        // 2. Cicla per ogni artista e crea una notifica singola
        newReleasesMap.forEach { (artistId, releases) ->

            // Costruisci il testo per questo specifico artista
            val artistMessage = buildString {
                if (releases.albums.isNotEmpty()) {
                    appendLine("🎧 Albums:")
                    releases.albums.forEach { appendLine("• ${it.title}") }
                }
                if (releases.singles.isNotEmpty()) {
                    if (isNotEmpty()) appendLine() // Spazio
                    appendLine("🎵 Singles:")
                    releases.singles.forEach { appendLine("• ${it.title}") }
                }
            }.trimEnd()

            // INTENTO SPECIFICO: Apre la schermata di QUESTO artista!
            // Devi creare un'Activity che sappia gestire questo Intent (es. ArtistDetailActivity)
            val artistIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("ARTIST_ID", artistId) // Passo l'ID
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val artistPendingIntent = PendingIntent.getActivity(
                context,
                notificationIdCounter, // Request code unico per ogni artista
                artistIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Notifica individuale
            val individualBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("New from ${releases.artistName}") // Titolo: Nome Artista
                .setContentText("${releases.albums.size + releases.singles.size} new releases")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(artistMessage)
                        .setBigContentTitle("New from ${releases.artistName}")
                )
                .setContentIntent(artistPendingIntent) // Apre l'artista al click!
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_NEW_RELEASES) // FONDAMENTALE: Assegna al gruppo!
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY) // Suonerà il sommario

            // Lancio la notifica individuale
            NotificationManagerCompat.from(context)
                .notify(notificationIdCounter, individualBuilder.build())

            notificationIdCounter++ // Incremento l'ID per la prossima
        }

        // 3. Crea la Notifica Sommario (Summary) - OBBLIGATORIA per il raggruppamento!
        // Su Android 12- questa è la notifica visibile di default. Su 13+ è quella che si espande.

        val totalReleases = newReleasesMap.values.sumOf { it.albums.size + it.singles.size }
        val totalArtists = newReleasesMap.size

        // Intent generico per il sommario (apre la Home)
        val summaryIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val summaryPendingIntent = PendingIntent.getActivity(
            context,
            0,
            summaryIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summaryBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("New Releases")
            .setContentText("$totalReleases new releases from $totalArtists artists")
            .setContentIntent(summaryPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_NEW_RELEASES) // Stesso gruppo
            .setGroupSummary(true) // FONDAMENTALE: Dice ad Android che questa è il sommario!
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

        NotificationManagerCompat.from(context)
            .notify(0, summaryBuilder.build()) // ID 0 fisso per il sommario
    }

    private fun showNotification(context: Context, message: String, allNewQuantity: Int) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                Timber.w("NewFromArtistsWorker: Notifications permission not granted, skipping notification.")
                return // Esci senza mostrare la notifica
            }
        }

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
            .setContentText("$allNewQuantity new releases available")
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