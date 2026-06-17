package it.fast4x.riplay.extensions.musicbrainz.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MBAlbumsByGenreWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val fetcher = MBAlbumsByGenreFetcher()

        val result = fetcher.fetch()
        return if (result.status == "OK") Result.success() else Result.retry()
    }
}