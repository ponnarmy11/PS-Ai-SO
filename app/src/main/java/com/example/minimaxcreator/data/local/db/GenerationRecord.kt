package com.example.minimaxcreator.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class GenerationType {
    TEXT_TO_IMAGE,
    IMAGE_TO_IMAGE,
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO,
    VIDEO_EDIT
}

@Serializable
enum class GenerationStatus {
    PENDING,
    SUCCESS,
    FAILED
}

@Serializable
enum class PlatformType {
    MINIMAX,
    QWEN
}

@Serializable
enum class TopMode {
    IMAGE,
    VIDEO
}

@Serializable
enum class SubMode {
    TEXT_TO_IMAGE,
    IMAGE_TO_IMAGE,
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO,
    VIDEO_EDIT
}

@Serializable
data class ImageParams(
    val model: String = "image-01",
    val aspectRatio: String = "1:1",
    val n: Int = 1,
    val seed: Long? = null,
    val promptOptimizer: Boolean = false,
    val aigcWatermark: Boolean = false,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class VideoParams(
    val resolution: String = "768P",
    val duration: Int = 5,
    val ratio: String = "16:9",
    val aigcWatermark: Boolean = false
)

@Serializable
data class AppSettings(
    val apiKey: String = "",
    val miniMaxApiKey: String = "",
    val qwenApiKey: String = "",
    val agnesApiKey: String = "",
    val defaultImageModel: String = "image-01",
    val defaultVideoModel: String = "MiniMax-H3",
    val modelParams: Map<String, String> = emptyMap(),
    val autoSave: Boolean = false,
    val dynamicColors: Boolean = false,
    val imageParams: ImageParams = ImageParams(),
    val videoParams: VideoParams = VideoParams()
)

enum class MessageRole {
    USER,
    AI
}

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val type: GenerationType,
    val prompt: String,
    val attachmentUris: List<String> = emptyList(),
    val paramsJson: String = "",
    val resultUrls: List<String> = emptyList(),
    val status: GenerationStatus,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val thumbnailUrl: String? = null,
    val taskId: String? = null,
    val progressText: String? = null,
    val platform: PlatformType = PlatformType.MINIMAX,
    val model: String = "image-01",
    val topMode: TopMode = TopMode.IMAGE,
    val subMode: SubMode = SubMode.TEXT_TO_IMAGE
)

data class GenerationRecord(
    val id: String,
    val type: GenerationType,
    val prompt: String,
    val paramsJson: String,
    val resultUrlsJson: String,
    val status: GenerationStatus,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val thumbnailUrl: String?,
    val attachmentsJson: String,
    val taskId: String?,
    val platform: PlatformType = PlatformType.MINIMAX,
    val model: String = "image-01",
    val topMode: TopMode = TopMode.IMAGE,
    val subMode: SubMode = SubMode.TEXT_TO_IMAGE
)

@Entity(tableName = "generation_records")
data class GenerationRecordEntity(
    @PrimaryKey val id: String,
    val type: String,
    val prompt: String,
    val paramsJson: String,
    val resultUrlsJson: String,
    val status: String,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val thumbnailUrl: String?,
    val attachmentsJson: String,
    val taskId: String?,
    val platform: String = "MINIMAX",
    val model: String = "image-01",
    val topMode: String = "IMAGE",
    val subMode: String = "TEXT_TO_IMAGE"
)

fun GenerationRecordEntity.toModel(): GenerationRecord {
    return GenerationRecord(
        id = id,
        type = runCatching { GenerationType.valueOf(type) }.getOrDefault(GenerationType.TEXT_TO_IMAGE),
        prompt = prompt,
        paramsJson = paramsJson,
        resultUrlsJson = resultUrlsJson,
        status = runCatching { GenerationStatus.valueOf(status) }.getOrDefault(GenerationStatus.FAILED),
        errorMessage = errorMessage,
        createdAt = createdAt,
        completedAt = completedAt,
        thumbnailUrl = thumbnailUrl,
        attachmentsJson = attachmentsJson,
        taskId = taskId,
        platform = runCatching { PlatformType.valueOf(platform) }.getOrDefault(PlatformType.MINIMAX),
        model = model.ifBlank { "image-01" },
        topMode = runCatching { TopMode.valueOf(topMode) }.getOrDefault(TopMode.IMAGE),
        subMode = runCatching { SubMode.valueOf(subMode) }.getOrDefault(subModeFrom(type))
    )
}

fun GenerationRecord.toEntity(): GenerationRecordEntity {
    return GenerationRecordEntity(
        id = id,
        type = type.name,
        prompt = prompt,
        paramsJson = paramsJson,
        resultUrlsJson = resultUrlsJson,
        status = status.name,
        errorMessage = errorMessage,
        createdAt = createdAt,
        completedAt = completedAt,
        thumbnailUrl = thumbnailUrl,
        attachmentsJson = attachmentsJson,
        taskId = taskId,
        platform = platform.name,
        model = model,
        topMode = topMode.name,
        subMode = subMode.name
    )
}

private fun subModeFrom(type: String): SubMode {
    return runCatching {
        when (GenerationType.valueOf(type)) {
            GenerationType.TEXT_TO_IMAGE -> SubMode.TEXT_TO_IMAGE
            GenerationType.IMAGE_TO_IMAGE -> SubMode.IMAGE_TO_IMAGE
            GenerationType.TEXT_TO_VIDEO -> SubMode.TEXT_TO_VIDEO
            GenerationType.IMAGE_TO_VIDEO -> SubMode.IMAGE_TO_VIDEO
            GenerationType.VIDEO_EDIT -> SubMode.VIDEO_EDIT
        }
    }.getOrDefault(SubMode.TEXT_TO_IMAGE)
}
