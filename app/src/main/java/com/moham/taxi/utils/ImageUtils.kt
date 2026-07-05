package com.moham.taxi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    
    private const val TICKETS_FOLDER = "tickets"

    /**
     * Creates a temporary file in the cache directory to be used by the camera app.
     */
    fun createTempImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = context.cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Compresses the image at the given URI and saves it as a WebP file in the internal storage's tickets directory.
     * Returns the relative path to the saved file (e.g., "tickets/filename.webp")
     */
    fun compressAndSaveTicketPhoto(context: Context, sourceUri: Uri): String? {
        return try {
            // Ensure tickets directory exists
            val ticketsDir = File(context.filesDir, TICKETS_FOLDER)
            if (!ticketsDir.exists()) {
                ticketsDir.mkdirs()
            }

            // Create output file
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "TICKET_${timeStamp}.webp"
            val outputFile = File(ticketsDir, fileName)

            // Read original bitmap
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Calculate new dimensions (max 1024x1024 to save space but keep readability)
            val maxDim = 1024
            var width = originalBitmap.width
            var height = originalBitmap.height
            if (width > maxDim || height > maxDim) {
                val ratio = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
                width = (width * ratio).toInt()
                height = (height * ratio).toInt()
            }
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            // Compress to WebP
            val outputStream = FileOutputStream(outputFile)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, outputStream)
            } else {
                @Suppress("DEPRECATION")
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 75, outputStream)
            }
            outputStream.close()

            // Recycle bitmaps
            if (originalBitmap != scaledBitmap) {
                originalBitmap.recycle()
            }
            scaledBitmap.recycle()

            // Return relative path
            "$TICKETS_FOLDER/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Gets the absolute File object for a given relative path.
     */
    fun getTicketPhotoFile(context: Context, relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file else null
    }

    /**
     * Deletes a ticket photo file.
     */
    fun deleteTicketPhoto(context: Context, relativePath: String?): Boolean {
        if (relativePath.isNullOrBlank()) return false
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
