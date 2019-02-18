package com.raywenderlich.android.photouploader.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raywenderlich.android.photouploader.ImageUtils

private val LOG_TAG by lazy { CleanFilesWorker::class.java.simpleName }

class CleanFilesWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    override fun doWork(): Result = try {
        // Sleep for debugging purposes
        Thread.sleep(1000)
        Log.d(LOG_TAG, "Cleaning files!")

        // Makes a notification when the work starts
        ImageUtils.makeStatusNotification("Cleaning Files", applicationContext)

        ImageUtils.cleanFiles(applicationContext)

        Log.d(LOG_TAG, "Success!")
       Result.success()
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: ${e.message}", e)
        Result.failure()
    }
}