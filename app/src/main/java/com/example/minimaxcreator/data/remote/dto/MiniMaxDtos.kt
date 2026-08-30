package com.example.minimaxcreator.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ImageSubjectReference(
    val type: String = "character",
    @SerialName("image_file") val imageFile: String
)

@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    @SerialName("subject_reference")
    val subjectReference: List<ImageSubjectReference>? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = "1:1",
    @SerialName("response_format") val responseFormat: String = "url",
    val seed: Long? = null,
    val n: Int = 1,
    @SerialName("prompt_optimizer") val promptOptimizer: Boolean = false,
    @SerialName("aigc_watermark") val aigcWatermark: Boolean = false,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class ImageGenerationData(
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("image_base64") val imageBase64: List<String> = emptyList()
)

@Serializable
data class ImageGenerationMetadata(
    @SerialName("success_count") val successCount: JsonElement? = null,
    @SerialName("failed_count") val failedCount: JsonElement? = null
)

@Serializable
data class BaseResp(
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("status_msg") val statusMessage: String? = null
)

@Serializable
data class ImageGenerationResponse(
    val id: String? = null,
    val data: ImageGenerationData? = null,
    val metadata: ImageGenerationMetadata? = null,
    @SerialName("base_resp") val baseResp: BaseResp? = null
)

@Serializable
data class VideoContentUrl(
    val url: String
)

@Serializable
data class VideoContentItem(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: VideoContentUrl? = null,
    val role: String? = null
)

@Serializable
data class VideoGenerationV2Request(
    val model: String = "MiniMax-H3",
    val content: List<VideoContentItem>,
    val resolution: String,
    val duration: Int,
    val ratio: String? = "adaptive",
    @SerialName("aigc_watermark") val aigcWatermark: Boolean = false
)

@Serializable
data class VideoGenerationV2Response(
    @SerialName("task_id") val taskId: String? = null
)

@Serializable
data class VideoTaskContent(
    val url: String? = null,
    val prompt: String? = null
)

@Serializable
data class VideoTaskError(
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class VideoTask(
    val id: String? = null,
    val model: String? = null,
    val status: String? = null,
    val content: VideoTaskContent? = null,
    val error: VideoTaskError? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
    val resolution: String? = null,
    val duration: Int? = null,
    val ratio: String? = null,
    @SerialName("task_type") val taskType: String? = null,
    val modality: String? = null
)

@Serializable
data class VideoTaskQueryResponse(
    val task: VideoTask? = null
)
