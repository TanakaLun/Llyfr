package io.github.tanakalun.llyfr.data.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageUtils {

    fun getImagesDir(context: Context): File {
        val dir = File(context.filesDir, "note_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun copyImageToInternal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(getImagesDir(context), fileName)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageDimensions(path: String): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        return (options.outWidth.coerceAtLeast(100) to options.outHeight.coerceAtLeast(100))
    }

    fun deleteImage(context: Context, path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteImages(context: Context, paths: List<String>) {
        paths.forEach { deleteImage(context, it) }
    }
}
