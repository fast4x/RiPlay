package it.fast4x.riplay.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.NOTIFICATION_CHANNEL
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8

/**
 * Endless Service is a service that runs in the background to keep the webview running.
 * Same service will be used as tips service
 */
class EndlessService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Returns the instance of the service
     */
    inner class LocalBinder : Binder() {
        val serviceInstance: EndlessService
            get() = this@EndlessService
    }

    private val mBinder: IBinder = LocalBinder() // IBinder


    override fun onCreate() {
        super.onCreate()
        // PARTIAL_WAKELOCK
        val powerManager: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RIPLAY:wakelock"
        )
        println("EndlessService onCreate")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, this.notification)
        println("EndlessService onStartCommand")
        return START_STICKY //START_NOT_STICKY
    }

    @SuppressLint("WakelockTimeout")
    override fun onBind(intent: Intent?): IBinder {
        if (wakeLock != null && !wakeLock!!.isHeld) {
            wakeLock!!.acquire()
        }
        println("EndlessService onBind")
        return mBinder
    }

    override fun onDestroy() {
        // PARTIAL_WAKELOCK
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
        }
        println("EndlessService onDestroy")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, EndlessService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT)
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
        println("EndlessService onTaskRemoved schedule restart service")
    }

    fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            NOTIFICATION_ID.toString(),
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(CHANNEL_ID)
            .setShowBadge(false)
            .build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private val notification: Notification
        @OptIn(UnstableApi::class)
        @RequiresApi(Build.VERSION_CODES.O)
        get() {

            val startIntent = Intent(appContext(), MainActivity::class.java)
            startIntent.action = Intent.ACTION_MAIN
            startIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            val contentIntent: PendingIntent? =
                PendingIntent.getActivity(appContext(), 1, startIntent, if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)


            return if (isAtLeastAndroid8) {
                NotificationCompat.Builder(appContext(), NOTIFICATION_CHANNEL)
            } else {
                NotificationCompat.Builder(appContext())
            }
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("RiPlay Tips")
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText("Tips will be displayed here, for now can disable notification permission in app settings")
                .setContentIntent(contentIntent)
                .setSilent(true)
                .build()



        }

    companion object {
        private const val NOTIFICATION_ID = 10 // The id of the notification
        private const val CHANNEL_ID = "EndlessServiceChannel" // The id of the channel
    }
}
