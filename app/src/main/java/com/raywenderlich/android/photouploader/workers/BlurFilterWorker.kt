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
import java.io.FileNotFoundException


class BlurFilterWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    private val LOG_TAG by lazy { BlurFilterWorker::class.java.simpleName }

    override fun doWork(): Result = try {
        // Sleep for debugging purposes
        Thread.sleep(1000)
        Log.d(LOG_TAG, "Applying filter to image!")

        // Makes a notification when the work starts
        ImageUtils.makeStatusNotification("Blurring image", applicationContext)

        val imageIndex = inputData.getInt(KEY_IMAGE_INDEX, 0)
        val imageUriString = inputData.getString(KEY_IMAGE_URI)

        val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, Uri.parse(imageUriString))

        val filteredBitmap = ImageUtils.applyBlurFilter(bitmap, applicationContext)
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
    } catch (fileNotFoundException: FileNotFoundException) {
        Log.e(LOG_TAG, "Failed to decode input stream", fileNotFoundException)
        Result.failure()
    }
}