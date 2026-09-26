package com.slh.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Compresses a picked image (gallery / camera content URI) into a
 * smaller JPEG under the app's cache/files directory so local
 * photoUri values stay small and multi-device sync (when Storage is
 * added later) stays cheap.
 *
 * Returns the file:// path of the compressed image, or null on failure.
 */
object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_SIDE_PX = 1280
    private const val JPEG_QUALITY = 78

    fun compress(
        context: Context,
        sourceUri: Uri,
        filePrefix: String = "photo"
    ): String? {
        return try {
            val resolver = context.contentResolver
            val input = resolver.openInputStream(sourceUri)
                ?: return null

            val original = input.use {
                BitmapFactory.decodeStream(it)
            } ?: return null

            val scaled = scaleDown(original, MAX_SIDE_PX)
            if (scaled !== original) {
                original.recycle()
            }

            val dir = File(context.filesDir, "photos")
            if (!dir.exists()) dir.mkdirs()

            val outFile = File(
                dir,
                "${filePrefix}_${System.currentTimeMillis()}.jpg"
            )

            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
                fos.flush()
            }
            scaled.recycle()

            outFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "compress failed: ${e.message}")
            null
        }
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= maxSide) return src

        val scale = maxSide.toFloat() / longest.toFloat()
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
