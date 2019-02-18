@file:JvmName("Constants")

package com.raywenderlich.android.photouploader

// Notification Channel constants
// Name of Notification Channel for verbose notifications of background work
@JvmField val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence = "Verbose WorkManager Notifications"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
@JvmField val NOTIFICATION_TITLE: CharSequence = "WorkRequest Starting"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
const val NOTIFICATION_ID = 1

// Image path constants
const val KEY_IMAGE_URI = "IMAGE_URI"
const val KEY_IMAGE_INDEX = "IMAGE_INDEX"
const val KEY_IMAGE_PATH_PREFIX = "IMAGE_PATH_"
//const val KEY_IMAGE_PATH = "IMAGE_PATH"
const val KEY_ZIP_PATH = "ZIP_PATH"

// Where to upload images
//  const val SERVER_UPLOAD_PATH = "http://10.0.2.2:3000/files" //local server URL
const val SERVER_UPLOAD_PATH = "https://simple-file-server.herokuapp.com/files" //shared service URL
