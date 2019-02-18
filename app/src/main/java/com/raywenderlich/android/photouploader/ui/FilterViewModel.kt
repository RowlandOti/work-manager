package com.raywenderlich.android.photouploader.ui

import android.arch.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.raywenderlich.android.photouploader.DIRECTORY_OUTPUTS
import com.raywenderlich.android.photouploader.IMAGE_MANIPULATION_WORK_NAME
import com.raywenderlich.android.photouploader.ImageOperations

/**
 * A [ViewModel] for [FilterActivity].
 *
 * Keeps track of pending image filter operations.
 */
class FilterViewModel : ViewModel() {
    private val mWorkManager: WorkManager = WorkManager.getInstance()

    internal val outputStatus: LiveData<List<WorkInfo>>
        get() = mWorkManager.getWorkInfosByTagLiveData(DIRECTORY_OUTPUTS)

    internal fun apply(imageOperations: ImageOperations) {
        imageOperations.continuation.enqueue()
    }

    internal fun cancel() {
        mWorkManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }
}