package com.example.minimaxcreator.ui.home

import android.net.Uri
import com.example.minimaxcreator.data.local.db.ChatMessage
import com.example.minimaxcreator.data.local.db.GenerationType
import com.example.minimaxcreator.data.local.db.TopMode
import com.example.minimaxcreator.data.model.MiniMaxImageParams
import com.example.minimaxcreator.data.model.ModelCatalog
import com.example.minimaxcreator.data.model.ModelParams

data class HomeUiState(
    val topMode: TopMode = TopMode.IMAGE,
    val mode: GenerationType = GenerationType.TEXT_TO_IMAGE,
    val selectedModel: String = ModelCatalog.MINIMAX_IMAGE_01,
    val selectedParams: ModelParams = MiniMaxImageParams(),
    val modelParams: Map<String, String> = emptyMap(),
    val prompt: String = "",
    val attachments: List<Uri> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val miniMaxApiKey: String = "",
    val qwenApiKey: String = "",
    val agnesApiKey: String = "",
    val falApiKey: String = "",
    val defaultImageModel: String = ModelCatalog.MINIMAX_IMAGE_01,
    val defaultVideoModel: String = ModelCatalog.MINIMAX_H3,
    val autoSave: Boolean = false,
    val isGenerating: Boolean = false
)

fun GenerationType.requiresAttachment(): Boolean {
    return this == GenerationType.IMAGE_TO_IMAGE || this == GenerationType.IMAGE_TO_VIDEO
}

fun GenerationType.isVideo(): Boolean {
    return this == GenerationType.TEXT_TO_VIDEO ||
        this == GenerationType.IMAGE_TO_VIDEO ||
        this == GenerationType.VIDEO_EDIT
}
