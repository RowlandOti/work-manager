package com.raywenderlich.android.photouploader.workers

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raywenderlich.android.photouploader.KEY_IMAGE_PATH_PREFIX
import com.raywenderlich.android.photouploader.ImageUtils
import com.raywenderlich.android.photouploader.KEY_IMAGE_INDEX
import com.raywenderlich.android.photouploader.KEY_IMAGE_URI

private val LOG_TAG by lazy { SepiatoneFilterWorker::class.java.simpleName }


class SepiatoneFilterWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    override fun doWork(): Result = try {
        // Sleep for debugging purposes
        Thread.sleep(1000)
        Log.d(LOG_TAG, "Applying SepiaTone filter to image!")

        // Makes a notification when the work starts
        ImageUtils.makeStatusNotification("Sepiatoning image", applicationContext)

        val imageIndex = inputData.getInt(KEY_IMAGE_INDEX, 0)
        val imageUriString = inputData.getString(KEY_IMAGE_URI)

        val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, Uri.parse(imageUriString))

        var filteredBitmap = ImageUtils.applySepiaFilter(bitmap)
        val filteredImageFile = ImageUtils.writeBitmapToFile(applicationContext, filteredBitmap)

        val outputData = Data.Builder()
                .putInt(KEY_IMAGE_INDEX, imageIndex)
                .putString(KEY_IMAGE_PATH_PREFIX + imageIndex, filteredImageFile.toString())
                .putString(KEY_IMAGE_URI, filteredImageFile.toURI().toString())
                .build()

        Log.d(LOG_TAG, "Success!")
        Result.success(outputData)
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: " + e.message, e)
        Result.failure()
    }
}