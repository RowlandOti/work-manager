package com.raywenderlich.android.photouploader.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.raywenderlich.android.photouploader.DIRECTORY_OUTPUTS
import com.raywenderlich.android.photouploader.ImageUtils
import com.raywenderlich.android.photouploader.KEY_IMAGE_URI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.*
import java.util.*

abstract class BaseFilterWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    companion object {
        private val LOG_TAG by lazy { BaseFilterWorker::class.java.simpleName }
        const val ASSET_PREFIX = "file:///android_asset/"

        /**
         * Creates an input stream which can be used to read the given `resourceUri`.
         *
         * @param context  the application [Context].
         * @param resourceUri the [String] resourceUri.
         * @return the [InputStream] for the resourceUri.
         */
        @Throws(IOException::class)
        fun inputStreamFor(context: Context, resourceUri: String): InputStream? {
            // If the resourceUri is an Android asset URI, then use AssetManager to get a handle to
            if (resourceUri.startsWith(ASSET_PREFIX)) {
                val assetManager = context.resources.assets
                return assetManager.open(resourceUri.substring(ASSET_PREFIX.length))
            } else {
                // Not an Android asset Uri. Use a ContentResolver to get a handle to the input stream.
                val resolver = context.contentResolver
                return resolver.openInputStream(Uri.parse(resourceUri))
            }
        }
    }

    override val coroutineContext: CoroutineDispatcher get() = Dispatchers.IO

    override suspend fun doWork(): Result {
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        try {
            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(LOG_TAG, "Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }
            val context = applicationContext
            val inputStream = inputStreamFor(context, resourceUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val output = applyFilter(bitmap)
            // write bitmap to a file and set the output
            val outputUri = ImageUtils.writeBitmapToFile(applicationContext, output)
            return Result.success(workDataOf(KEY_IMAGE_URI to outputUri.toString()))
        } catch (fileNotFoundException: FileNotFoundException) {
            Log.e(LOG_TAG, "Failed to decode input stream", fileNotFoundException)
            throw RuntimeException("Failed to decode input stream", fileNotFoundException)
        } catch (throwable: Throwable) {
            Log.e(LOG_TAG, "Error applying filter", throwable)
            return Result.failure()
        }
    }

    abstract fun applyFilter(input: Bitmap): Bitmap
}