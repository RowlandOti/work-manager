package com.raywenderlich.android.photouploader.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raywenderlich.android.photouploader.ImageUtils
import com.raywenderlich.android.photouploader.KEY_ZIP_PATH

private const val LOG_TAG = "UploadWorker"

class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    override fun doWork(): Result = try {
        // Sleep for debugging purposes
        Thread.sleep(1000)
        Log.d(LOG_TAG, "Uploading file!")

        // Makes a notification when the work starts
        ImageUtils.makeStatusNotification("Uploading Files", applicationContext)

        val zipPath = inputData.getString(KEY_ZIP_PATH)

        ImageUtils.uploadFile(Uri.parse(zipPath))

        Log.d(LOG_TAG, "Success!")
        Result.success()
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: " + e.message, e)
        Result.retry()
    }
}