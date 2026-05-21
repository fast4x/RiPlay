package it.fast4x.riplay.extensions.experimental.musicvalt

import android.content.Context
import android.os.Environment
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_VAULT_DISCLAIMER_ACCEPTED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_VAULT_ENABLED
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.appContext
import timber.log.Timber
import java.io.File

fun checkAndStartMusicVault(){
    val context = appContext()
    if (context.preferences.getBoolean(MUSIC_VAULT_ENABLED.key, false)
        && context.preferences.getBoolean(MUSIC_VAULT_DISCLAIMER_ACCEPTED.key, false)) {
        val result = testAndStartChaquopy()
        Timber.d("Chaquopy $result")
    }
}

private fun testAndStartChaquopy(): Triple<String, String, Boolean> {
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(appContext()))
    }

    val py = Python.getInstance()

    // Test 1: Python funziona?
    val sys = py.getModule("sys")
    val pyVersion = sys["version"].toString()
    Timber.d("Chaquopy Python version: $pyVersion")

    // Test 2: yt-dlp è installato?
    val ytdlp = py.getModule("yt_dlp")
    val ytdlpVersion = ytdlp["version"]?.get("__version__").toString()
    Timber.d("Chaquopy yt-dlp version: $ytdlpVersion")

    val ytdlpIsReady = (pyVersion.isNotEmpty() && ytdlpVersion.isNotEmpty())

    return Triple(pyVersion, ytdlpVersion, ytdlpIsReady)
}

fun Song.resolveThumbnail(context: Context): Any? {
    // Se è in MusicVault e ha la thumbnail locale, usala
    musicVaultThumbnailFileName?.let { fileName ->
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            fileName
        )
        if (file.exists()) return file
    }
    // Fallback all'URL remoto
    return thumbnailUrl
}

fun Song.resolveAudioFile(context: Context): File? {
    musicVaultFileName?.let { fileName ->
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            fileName
        )
        if (file.exists()) return file
    }
    return null
}

fun enqueueMusicVault(context: Context, song: Song) {
    val request = OneTimeWorkRequestBuilder<MusicVaultWorker>()
        .setInputData(workDataOf(
            "song_id" to song.id,
            "url"     to "https://youtube.com/watch?v=${song.id}"
        ))
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .addTag("musicvault_${song.id}")
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "musicvault_${song.id}",
        ExistingWorkPolicy.KEEP,
        request
    )
}