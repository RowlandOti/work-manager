/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.photouploader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min


object ImageUtils {

    private const val LOG_TAG = "ImageUtils"


    //grayscale multipliers
    private const val GRAYSCALE_RED = 0.3
    private const val GRAYSCALE_GREEN = 0.59
    private const val GRAYSCALE_BLUE = 0.11

    private const val MAX_COLOR = 255

    private const val SEPIA_TONE_RED = 110
    private const val SEPIA_TONE_GREEN = 65
    private const val SEPIA_TONE_BLUE = 20

    private const val COMPRESS_BUFFER_CHUNK = 1024

    private val okHttpClient by lazy { OkHttpClient() }

    private const val MULTIPART_NAME = "file"

    /**
     * Sepia filter.
     * From: https://github.com/yaa110/Effects-Pro/blob/master/src/org/appsroid/fxpro/bitmap/BitmapProcessing.java
     * Applies Sepiatone to the given Bitmap image
     * @param bitmap Image to blur
     * @return Blurred bitmap image
     */
    fun applySepiaFilter(bitmap: Bitmap): Bitmap {
        // image size
        val width = bitmap.width
        val height = bitmap.height

        // create output bitmap
        val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)

        // color information
        var alpha: Int
        var red: Int
        var green: Int
        var blue: Int
        var currentPixel: Int

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {

                // get pixel color
                currentPixel = bitmap.getPixel(x, y)

                // get color on each channel
                alpha = Color.alpha(currentPixel)
                red = Color.red(currentPixel)
                green = Color.green(currentPixel)
                blue = Color.blue(currentPixel)

                // apply grayscale sample
                red = (GRAYSCALE_RED * red + GRAYSCALE_GREEN * green + GRAYSCALE_BLUE * blue).toInt()
                green = red
                blue = green

                // apply intensity level for sepid-toning on each channel
                red += SEPIA_TONE_RED
                green += SEPIA_TONE_GREEN
                blue += SEPIA_TONE_BLUE

                //if you overflow any color, set it to MAX (255)
                red = min(red, MAX_COLOR)
                green = min(green, MAX_COLOR)
                blue = min(blue, MAX_COLOR)

                outputBitmap.setPixel(x, y, Color.argb(alpha, red, green, blue))
            }
        }

        bitmap.recycle()

        return outputBitmap
    }


    /**
     * Sepia filter.
     * From:
     * Blurs the given Bitmap image
     * @param bitmap Image to blur
     * @param applicationContext Application context
     * @return Blurred bitmap image
     */
    fun applyBlurFilter(bitmap: Bitmap, applicationContext: Context): Bitmap {

        lateinit var rsContext: RenderScript
        try {
            // Create the output bitmap
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)

            // Blur the image
            rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG)
            val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
            val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)
            val theIntrinsic = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
            theIntrinsic.apply {
                setRadius(10f)
                theIntrinsic.setInput(inAlloc)
                theIntrinsic.forEach(outAlloc)
            }
            outAlloc.copyTo(output)

            return output
        } finally {
            rsContext.finish()
        }
    }

    /**
     * Writes a given [Bitmap] to the [Context.getFilesDir] directory.
     *
     * @param applicationContext the application [Context].
     * @param bitmap the [Bitmap] which needs to be written to the files directory.
     * @return a [Uri] to the output [Bitmap].
     */
    @Throws(FileNotFoundException::class)
    fun writeBitmapToFile(applicationContext: Context, bitmap: Bitmap): Uri {
        // Bitmaps are being written to a temporary directory. This is so they can serve as inputs
        // for workers downstream, via Worker chaining.
        val name = String.format("filter-output-%s.png", UUID.randomUUID().toString())
        val outputDir = File(applicationContext.filesDir, DIRECTORY_OUTPUTS)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, name)
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, out)
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (ignore: IOException) {
                }
            }
        }
        return Uri.fromFile(outputFile)
    }

    /*fun writeBitmapToFile(applicationContext: Context, bitmap: Bitmap): File {
        val randomId = UUID.randomUUID().toString()
        val name = "$randomId.png"

        val outputDirectory = getOutputDirectory(applicationContext)
        val outputFile = File(outputDirectory, name)

        try {
            FileOutputStream(outputFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 0 *//* ignored for PNG *//*, outputStream)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return outputFile
    }*/

    private fun getOutputDirectory(applicationContext: Context): File {
        return File(applicationContext.filesDir, DIRECTORY_OUTPUTS).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun cleanFiles(applicationContext: Context) {
        val outputDirectory = getOutputDirectory(applicationContext)

        outputDirectory.listFiles()?.forEach { it.delete() }
    }

    fun createZipFile(applicationContext: Context, files: Array<String>): Uri {
        val randomId = UUID.randomUUID().toString()
        val name = "$randomId.zip"

        val outputDirectory = getOutputDirectory(applicationContext)
        val outputFile = File(outputDirectory, name)

        val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
        compressFiles(zipOutputStream, files)

        return Uri.fromFile(outputFile)
    }

    private fun compressFiles(zipOutputStream: ZipOutputStream, files: Array<String>) {
        zipOutputStream.use { out ->
            files.forEach { file ->
                FileInputStream(file).use { fileInput ->
                    BufferedInputStream(fileInput).use { origin ->
                        val entry = ZipEntry(file.substring(file.lastIndexOf("/")))
                        out.putNextEntry(entry)
                        origin.copyTo(out, COMPRESS_BUFFER_CHUNK)
                    }
                }
            }
        }
    }

    fun uploadFile(fileUri: Uri) {
        val file = File(fileUri.path)

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(MULTIPART_NAME, file.name, RequestBody.create(null, file))
                .build()

        val request = Request.Builder()
                .url(SERVER_UPLOAD_PATH)
                .post(requestBody)
                .build()

        val response = okHttpClient.newCall(request).execute()

        Log.d(LOG_TAG, "onResponse - Status: ${response?.code()} Body: ${response?.body()?.string()}")
    }

    /**
     * Create a Notification that is shown as a heads-up notification if possible.
     *
     * For this codelab, this is used to show a notification so that you know when different steps
     * of the background work chain are starting
     *
     * @param message Message shown on the notification
     * @param context Context needed to create Toast
     */
    fun makeStatusNotification(message: String, context: Context) {

        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
            val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description

            // Add the channel
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

            notificationManager?.createNotificationChannel(channel)
        }

        // Create the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(LongArray(0))

        // Show the notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}