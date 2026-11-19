package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

data class ProcessedMedia(
    val isVideo: Boolean,
    val mediaBase64: String,
    val thumbnailBase64: String,
    val clipTooLargeForCloud: Boolean = false
)

object MediaHelper {
    private const val MAX_IMAGE_DIM = 820
    private const val MAX_THUMB_DIM = 520
    private const val MAX_VIDEO_BYTES = 460_000 // ~613k Base64 chars, safely under 650k Firestore rule bound
    private const val MAX_IMAGE_BASE64_CHARS = 600_000

    fun processPickedMedia(context: Context, uri: Uri): Result<ProcessedMedia> {
        return try {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val isVideo = mimeType.startsWith("video/")
            if (isVideo) {
                processVideo(context, uri)
            } else {
                processImage(context, uri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun processImage(context: Context, uri: Uri): Result<ProcessedMedia> {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(IllegalStateException("Could not read selected image"))

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
        var sampleSize = 1
        val maxSide = max(boundsOpts.outWidth, boundsOpts.outHeight)
        while (maxSide / sampleSize > MAX_IMAGE_DIM * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: return Result.failure(IllegalStateException("Unsupported image format"))

        val scaled = scaleBitmap(decoded, MAX_IMAGE_DIM)
        var quality = 76
        var encoded = encodeBitmapJpegBase64(scaled, quality)
        while (encoded.length > MAX_IMAGE_BASE64_CHARS && quality > 30) {
            quality -= 12
            encoded = encodeBitmapJpegBase64(scaled, quality)
        }

        if (scaled != decoded) decoded.recycle()
        scaled.recycle()

        if (encoded.length > MAX_IMAGE_BASE64_CHARS) {
            return Result.failure(IllegalArgumentException("Image is too large even after compression"))
        }

        return Result.success(
            ProcessedMedia(
                isVideo = false,
                mediaBase64 = encoded,
                thumbnailBase64 = ""
            )
        )
    }

    private fun processVideo(context: Context, uri: Uri): Result<ProcessedMedia> {
        val retriever = MediaMetadataRetriever()
        var thumbnailBase64 = ""
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame != null) {
                val scaledThumb = scaleBitmap(frame, MAX_THUMB_DIM)
                thumbnailBase64 = encodeBitmapJpegBase64(scaledThumb, 72)
                if (scaledThumb != frame) frame.recycle()
                scaledThumb.recycle()
            }
        } catch (_: Exception) {
            // Fallback empty thumbnail if metadata retriever cannot decode frame
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(16_384)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > MAX_VIDEO_BYTES) {
                    return@use null
                }
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        }

        return if (bytes != null) {
            val videoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Result.success(
                ProcessedMedia(
                    isVideo = true,
                    mediaBase64 = videoBase64,
                    thumbnailBase64 = thumbnailBase64,
                    clipTooLargeForCloud = false
                )
            )
        } else {
            Result.success(
                ProcessedMedia(
                    isVideo = true,
                    mediaBase64 = "",
                    thumbnailBase64 = thumbnailBase64,
                    clipTooLargeForCloud = true
                )
            )
        }
    }

    private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val scale = maxDim.toFloat() / max(width, height).toFloat()
        val newW = max(1, (width * scale).toInt())
        val newH = max(1, (height * scale).toInt())
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    private fun encodeBitmapJpegBase64(bitmap: Bitmap, quality: Int): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun writeBase64VideoToTempFile(context: Context, memoryId: String, base64: String): Uri? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val safeId = memoryId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(context.cacheDir, "crew_memory_$safeId.mp4")
            file.writeBytes(bytes)
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }
}
