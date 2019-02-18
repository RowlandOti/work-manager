package com.raywenderlich.android.photouploader.workers

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raywenderlich.android.photouploader.ImageUtils
import com.raywenderlich.android.photouploader.KEY_IMAGE_PATH_PREFIX
import com.raywenderlich.android.photouploader.KEY_ZIP_PATH


private val LOG_TAG by lazy { CompressWorker::class.java.simpleName }


class CompressWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result = try {
        // Sleep for debugging purposes
        Thread.sleep(1000)
        Log.d(LOG_TAG, "Compressing files!")

        // Makes a notification when the work starts
        ImageUtils.makeStatusNotification("Compressing Files", applicationContext)

        val imagePaths = inputData.keyValueMap
                .filter { it.key.startsWith(KEY_IMAGE_PATH_PREFIX) }
                .map { it.value as String }

        val zipFile = ImageUtils.createZipFile(applicationContext, imagePaths.toTypedArray())

        val outputData = Data.Builder()
                .putString(KEY_ZIP_PATH, zipFile.path)
                .build()

        Log.d(LOG_TAG, "Success!")
        Result.success(outputData)
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: " + e.message, e)
        Result.failure()
    }
}