package it.fast4x.riplay.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

class ToolsWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        Timber.d("ToolsWorker.doWork")
        return Result.success()

    }

}