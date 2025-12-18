package com.example.imgcraft.sample.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/17/2025
 */
object ImageSaver {

    suspend fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        directoryName: String = "imageCraft"
    ): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveUsingMediaStore(context, bitmap, displayName, directoryName)
            } else {
                saveUsingLegacyFileApi(bitmap, displayName, directoryName)
            }
            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveUsingMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        directoryName: String
    ): Uri? {
        val relativePath = "${Environment.DIRECTORY_PICTURES}${File.separator}$directoryName"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            // Keep the file private to our app until we're done writing
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { stream ->
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw Exception("Failed to compress/save bitmap")
                }
            }
            // Release the "pending" status so other apps can see it
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        }
        return uri
    }

    private fun saveUsingLegacyFileApi(
        bitmap: Bitmap,
        displayName: String,
        directoryName: String
    ): Uri? {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val directory = File(root, directoryName)

        if (!directory.exists() && !directory.mkdirs()) {
            throw Exception("Could not create directory: ${directory.absolutePath}")
        }

        val file = File(directory, "$displayName.jpg")
        FileOutputStream(file).use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw Exception("Failed to compress/save bitmap")
            }
        }
        return Uri.fromFile(file)
    }
}