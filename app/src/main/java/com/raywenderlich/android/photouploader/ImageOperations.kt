package com.raywenderlich.android.photouploader

import android.net.Uri
import androidx.work.*
import com.raywenderlich.android.photouploader.workers.*

/**
 * Builds and holds WorkContinuation based on supplied filters.
 * src: https://github.com/googlesamples/android-architecture-components/blob/master/WorkManagerSample/app/src/main/java/com/example/background/ImageOperations.kt
 */
internal class ImageOperations private constructor(val continuation: WorkContinuation) {

    internal class Builder(private val mImageUri: Uri) {
        private var mApplyWaterColor: Boolean = false
        private var mApplyGrayScale: Boolean = false
        private var mApplySepiaTone: Boolean = false
        private var mApplyBlur: Boolean = false
        private var mApplySave: Boolean = false
        private var mApplyUpload: Boolean = false

        fun setApplyWaterColor(applyWaterColor: Boolean): Builder {
            mApplyWaterColor = applyWaterColor
            return this
        }

        fun setApplyGrayScale(applyGrayScale: Boolean): Builder {
            mApplyGrayScale = applyGrayScale
            return this
        }

        fun setApplySepiaTone(applySepiaTone: Boolean): Builder {
            mApplySepiaTone = applySepiaTone
            return this
        }

        fun setApplyBlur(applyBlur: Boolean): Builder {
            mApplyBlur = applyBlur
            return this
        }

        fun setApplySave(applySave: Boolean): Builder {
            mApplySave = applySave
            return this
        }

        fun setApplyUpload(applyUpload: Boolean): Builder {
            mApplyUpload = applyUpload
            return this
        }

        /**
         * Creates the [WorkContinuation] depending on the list of selected filters.
         *
         * @return the instance of [WorkContinuation].
         */
        fun build(): ImageOperations {
            var hasInputData = false
            var continuation = WorkManager.getInstance()
                    .beginUniqueWork(IMAGE_MANIPULATION_WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            OneTimeWorkRequest.from(CleanFilesWorker::class.java))

            if (mApplyWaterColor) {

                val waterColor = OneTimeWorkRequestBuilder<WaterColorFilterWorker>()
                        .setInputData(createInputData())
                        .build()
                continuation = continuation.then(waterColor)
                hasInputData = true
            }

            if (mApplyGrayScale) {
                val grayScaleBuilder = OneTimeWorkRequestBuilder<GrayScaleFilterWorker>()
                if (!hasInputData) {
                    grayScaleBuilder.setInputData(createInputData())
                    hasInputData = true
                }
                val grayScale = grayScaleBuilder.build()
                continuation = continuation.then(grayScale)
            }

            if (mApplySepiaTone) {
                val sepiaToneBuilder = OneTimeWorkRequestBuilder<SepiatoneFilterWorker>()
                if (!hasInputData) {
                    sepiaToneBuilder.setInputData(createInputData())
                    hasInputData = true
                }
                val sepiaTone = sepiaToneBuilder.build()
                continuation = continuation.then(sepiaTone)
            }

            if (mApplyBlur) {
                val blurBuilder = OneTimeWorkRequestBuilder<BlurFilterWorker>()
                if (!hasInputData) {
                    blurBuilder.setInputData(createInputData())
                    hasInputData = true
                }
                val blur = blurBuilder.build()
                continuation = continuation.then(blur)
            }

            if (mApplySave) {
                val save = OneTimeWorkRequestBuilder<SaveImageToGalleryWorker>()
                        .setInputData(createInputData())
                        .addTag(DIRECTORY_OUTPUTS)
                        .build()
                continuation = continuation.then(save)
            }

            if (mApplyUpload) {
                val upload = OneTimeWorkRequestBuilder<UploadWorker>()
                        .setInputData(createInputData())
                        .addTag(DIRECTORY_OUTPUTS)
                        .build()
                continuation = continuation.then(upload)
            }
            return ImageOperations(continuation)
        }

        private fun createInputData(): Data {
            return workDataOf(KEY_IMAGE_URI to mImageUri.toString())
        }
    }
}