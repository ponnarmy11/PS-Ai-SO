package com.example.minimaxcreator.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class MediaSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun saveRemote(url: String, isVideo: Boolean): String = withContext(Dispatchers.IO) {
        val mime = if (isVideo) "video/mp4" else "image/jpeg"
        val extension = if (isVideo) "mp4" else "jpg"
        val fileName = "Studio_${System.currentTimeMillis()}.$extension"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isVideo) "${Environment.DIRECTORY_MOVIES}/Studio"
                    else "${Environment.DIRECTORY_PICTURES}/Studio"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(contentUri, values)
                ?: error("无法创建相册文件")
            val output = context.contentResolver.openOutputStream(uri)
            try {
                download(url, output ?: error("无法打开相册文件"))
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri.toString()
            } catch (t: Throwable) {
                context.contentResolver.delete(uri, null, null)
                throw t
            }
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                ),
                "Studio"
            )
            directory.mkdirs()
            val file = File(directory, fileName)
            FileOutputStream(file).use { download(url, it) }
            file.absolutePath
        }
    }

    suspend fun shareRemote(url: String, isVideo: Boolean) = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "shared").apply { mkdirs() }
        val ext = if (isVideo) "mp4" else "jpg"
        val target = File(file, "Studio_${System.currentTimeMillis()}.$ext")
        FileOutputStream(target).use { download(url, it) }
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            target
        )
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (isVideo) "video/mp4" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享作品").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun download(url: String, output: OutputStream) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.inputStream.use { input ->
            input.copyTo(output)
        }
    }
}
