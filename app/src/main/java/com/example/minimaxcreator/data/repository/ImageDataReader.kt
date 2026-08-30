package com.example.minimaxcreator.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ImageDataReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun read(uri: Uri, maxBytes: Long = MAX_BYTES): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取所选图片")
        require(bytes.size <= maxBytes) { "图片超过 ${maxBytes / 1024 / 1024}MB 限制" }
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    companion object {
        const val MAX_BYTES = 30L * 1024L * 1024L
    }
}
