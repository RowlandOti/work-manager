package com.raywenderlich.android.photouploader.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raywenderlich.android.photouploader.KEY_IMAGE_URI
import java.text.SimpleDateFormat
import java.util.*

/**
 * Saves an output image to the [MediaStore].
 */
class SaveImageToGalleryWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "SvImageToGalleryWrkr"
        private const val TITLE = "Filtered Image"
        private val DATE_FORMATTER = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault())
    }

    override fun doWork(): Result {
        val resolver = applicationContext.contentResolver
        try {
            val resourceUri = inputData.getString(KEY_IMAGE_URI)
            val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)))
            val imageUrl = MediaStore.Images.Media.insertImage(resolver, bitmap, TITLE, DATE_FORMATTER.format(Date()))
            if (TextUtils.isEmpty(imageUrl)) {
                Log.e(TAG, "Writing to MediaStore failed")
                return Result.failure()
            }
            // Set the result of the worker by calling setOutputData().
            val output = Data.Builder()
                    .putString(KEY_IMAGE_URI, imageUrl)
                    .build()
            return Result.success(output)
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to save image to Gallery", exception)
            return Result.failure()
        }
    }
}