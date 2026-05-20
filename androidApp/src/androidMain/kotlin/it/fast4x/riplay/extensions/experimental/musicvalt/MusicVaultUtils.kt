package it.fast4x.riplay.extensions.experimental.musicvalt

import android.content.Context
import android.os.Environment
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import it.fast4x.riplay.data.models.Song
import java.io.File

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