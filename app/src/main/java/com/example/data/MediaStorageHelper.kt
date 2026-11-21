package com.example.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object MediaStorageHelper {
    private const val MEDIA_DIR = "instincta_media"

    suspend fun persistPickedMedia(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                ?: if (mime.startsWith("video/")) "mp4" else "jpg"
            val dir = File(context.filesDir, MEDIA_DIR)
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "${UUID.randomUUID()}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun isVideoFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".webm")
    }
}
