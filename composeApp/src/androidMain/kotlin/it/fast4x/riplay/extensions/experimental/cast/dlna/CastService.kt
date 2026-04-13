package it.fast4x.riplay.extensions.experimental.cast.dlna

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CastService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var httpServer: AudioStreamServer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val SAMPLE_RATE = 44100
    private val CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO
    private val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING) * 4

    companion object {
        const val ACTION_STOP = "action_stop_cast"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, buildNotification())

        val projectionCode = intent?.getIntExtra("projection_code", -1) ?: return START_NOT_STICKY
        val projectionData = intent.getParcelableExtra<Intent>("projection_data") ?: return START_NOT_STICKY

        val mpManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = mpManager.getMediaProjection(projectionCode, projectionData)

        startAudioCapture()
        return START_STICKY
    }

    private fun startAudioCapture() {
        // Configura la cattura dell'audio di sistema (non microfono)
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)  // cattura solo media (YouTube)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_MASK)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(captureConfig)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(BUFFER_SIZE)
            .build()

        // Avvia il server HTTP che servirà il PCM come WAV chunked
        httpServer = AudioStreamServer(port = 8765).also { it.start() }

        audioRecord!!.startRecording()

        // Loop di lettura PCM → push al server
        scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive) {
                val read = audioRecord!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    httpServer?.pushAudio(buffer.copyOf(read))
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        httpServer?.stop()
        mediaProjection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification(): Notification {
        val channelId = "cast_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cast audio in corso",
                NotificationManager.IMPORTANCE_LOW  // LOW = nessun suono, ma visibile
            ).apply {
                description = "Notifica attiva durante il cast verso l'amplificatore"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Intent per fermare il cast toccando la notifica
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, CastService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Cast audio attivo")
            .setContentText("Streaming verso l'amplificatore DLNA")
            .setSmallIcon(R.drawable.ic_media_play)
            .setOngoing(true)          // non eliminabile con swipe
            .setSilent(true)
            .addAction(
                R.drawable.ic_media_pause,
                "Stop cast",
                stopIntent
            )
            .build()
    }
}