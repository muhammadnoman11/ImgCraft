package com.github.muhammadnoman11.imgcraft.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlin.math.min

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/18/2025
 */
object BitmapUtils {


    /* ---------------------------------------------------------
    * Image Decoding
    * --------------------------------------------------------- */

    /**
     * Safely decode a bitmap from a URI using ImageDecoder.
     * Ensures mutable and software-allocated bitmap.
     */
    fun decodeUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Creates a downscaled preview bitmap to improve performance.
     */
    fun createPreviewBitmap(original: Bitmap, maxSize: Int = 1080): Bitmap {
        val scale = min(
            maxSize / original.width.toFloat(),
            maxSize / original.height.toFloat()
        ).coerceAtMost(1f)
        return Bitmap.createScaledBitmap(
            original,
            (original.width * scale).toInt(),
            (original.height * scale).toInt(),
            true
        )
    }


}