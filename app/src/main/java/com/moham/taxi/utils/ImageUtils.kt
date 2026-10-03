package com.moham.taxi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    
    private const val TAG = "ImageUtils"
    private const val TICKETS_FOLDER = "tickets"
    private const val MAX_DIMENSION = 1024

    /**
     * Creates a temporary file in the cache directory to be used by the camera app.
     * Uses externalCacheDir when available so all OEM camera apps have permission to write to it.
     */
    fun createTempImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = context.externalCacheDir ?: context.cacheDir
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Compresses the image at the given URI and saves it as a WebP file in the internal storage's tickets directory.
     * Implements inJustDecodeBounds downsampling to prevent OutOfMemoryError on high-res camera photos.
     * Returns the relative path to the saved file (e.g., "tickets/filename.webp")
     */
    fun compressAndSaveTicketPhoto(context: Context, sourceUri: Uri): String? {
        android.util.Log.d(TAG, "compressAndSaveTicketPhoto called with uri: $sourceUri")
        return try {
            // Ensure tickets directory exists
            val ticketsDir = File(context.filesDir, TICKETS_FOLDER)
            if (!ticketsDir.exists()) {
                val created = ticketsDir.mkdirs()
                android.util.Log.d(TAG, "ticketsDir created: $created")
            }

            // Create output file
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "TICKET_${timeStamp}.webp"
            val outputFile = File(ticketsDir, fileName)

            val file = if (sourceUri.scheme == "file" && sourceUri.path != null) {
                File(sourceUri.path!!)
            } else null

            android.util.Log.d(TAG, "Direct file: $file (exists=${file?.exists()}, length=${file?.length()})")

            // Step 1: Decode image dimensions only (prevents allocating full bitmap in memory)
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (file != null && file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, boundsOptions)
                } ?: run {
                    android.util.Log.e(TAG, "Failed to open InputStream for URI: $sourceUri")
                    return null
                }
            }

            android.util.Log.d(TAG, "Decoded bounds: ${boundsOptions.outWidth} x ${boundsOptions.outHeight}")
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                android.util.Log.e(TAG, "Invalid image bounds: ${boundsOptions.outWidth} x ${boundsOptions.outHeight}")
                return null
            }

            // Step 2: Calculate inSampleSize to downsample during decoding
            val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, MAX_DIMENSION)
            android.util.Log.d(TAG, "Calculated inSampleSize: $sampleSize")

            // Step 3: Decode bitmap with downsampling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = if (file != null && file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                }
            } ?: run {
                android.util.Log.e(TAG, "Failed to decode sampled bitmap")
                return null
            }

            android.util.Log.d(TAG, "Decoded sampled bitmap: ${sampledBitmap.width} x ${sampledBitmap.height}")

            // Step 4: Scale bitmap accurately to MAX_DIMENSION if still larger
            val width = sampledBitmap.width
            val height = sampledBitmap.height
            val scaledBitmap = if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                val ratio = Math.min(MAX_DIMENSION.toFloat() / width, MAX_DIMENSION.toFloat() / height)
                val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
                val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true).also {
                    if (it != sampledBitmap) {
                        sampledBitmap.recycle()
                    }
                }
            } else {
                sampledBitmap
            }

            // Step 5: Correct EXIF orientation if needed
            val rotationDegrees = getRotationDegrees(context, sourceUri, file)
            android.util.Log.d(TAG, "Rotation degrees: $rotationDegrees")
            val finalBitmap = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true).also {
                    if (it != scaledBitmap) {
                        scaledBitmap.recycle()
                    }
                }
            } else {
                scaledBitmap
            }

            // Step 6: Compress to WebP
            FileOutputStream(outputFile).use { outputStream ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, outputStream)
                } else {
                    @Suppress("DEPRECATION")
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP, 75, outputStream)
                }
            }

            // Recycle final bitmap
            finalBitmap.recycle()

            android.util.Log.d(TAG, "Successfully saved WebP ticket: $outputFile (bytes=${outputFile.length()})")

            // Return relative path
            "$TICKETS_FOLDER/$fileName"
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in compressAndSaveTicketPhoto", e)
            null
        }
    }

    /**
     * Calculates the inSampleSize value for BitmapFactory.Options to downsample the image
     * to a size close to maxDim, dramatically reducing RAM usage during decoding.
     */
    fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val maxDimension = maxOf(rawWidth, rawHeight)
        val halfMax = maxDimension / 2
        while (halfMax / inSampleSize >= maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Decodes a downsampled Bitmap from a local file path to fit within targetSize.
     * Uses inJustDecodeBounds to read dimensions first, then decodes with inSampleSize,
     * drastically reducing RAM usage (e.g. ~64 KB instead of 4 MB for thumbnails).
     */
    fun decodeSampledBitmap(filePath: String, targetSize: Int = 144): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null
            }

            val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, targetSize)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(filePath, decodeOptions)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error decoding sampled bitmap: $filePath", e)
            null
        }
    }


    /**
     * Reads the EXIF orientation from the image URI and returns the required rotation degrees.
     */
    private fun getRotationDegrees(context: Context, sourceUri: Uri, file: File?): Float {
        return try {
            val exifInterface = if (file != null && file.exists()) {
                ExifInterface(file.absolutePath)
            } else if (sourceUri.scheme == "file" && sourceUri.path != null) {
                ExifInterface(sourceUri.path!!)
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    ExifInterface(stream)
                } ?: return 0f
            }

            when (exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Could not determine EXIF rotation: ${e.message}")
            0f
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
