package com.example.minimaxcreator.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QwenImageContent(
    val text: String? = null,
    val image: String? = null
)

@Serializable
data class QwenImageMessage(
    val role: String = "user",
    val content: List<QwenImageContent>
)

@Serializable
data class QwenImageInput(
    val messages: List<QwenImageMessage>
)

@Serializable
data class QwenImageParameters(
    val size: String = "1024*1024",
    val n: Int = 1,
    val seed: Int? = null,
    @SerialName("prompt_extend") val promptExtend: Boolean = true,
    @SerialName("prompt_extend_mode") val promptExtendMode: String = "direct",
    @SerialName("enable_thinking") val enableThinking: Boolean = true,
    val watermark: Boolean = false,
    @SerialName("negative_prompt") val negativePrompt: String = ""
)

@Serializable
data class QwenImageGenerationRequest(
    val model: String,
    val input: QwenImageInput,
    val parameters: QwenImageParameters
)

@Serializable
data class QwenImageOutput(
    val choices: List<QwenChoice>? = null
)

@Serializable
data class QwenImageGenerationResponse(
    val output: QwenImageOutput? = null,
    @SerialName("request_id") val requestId: String? = null,
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class QwenMediaInput(
    val type: String,
    val url: String
)

@Serializable
data class QwenVideoInput(
    val prompt: String? = null,
    @SerialName("negative_prompt") val negativePrompt: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val media: List<QwenMediaInput>? = null
)

@Serializable
data class QwenVideoParameters(
    val resolution: String? = null,
    val ratio: String? = null,
    val duration: Int? = null,
    val audio: Boolean? = null,
    val seed: Int? = null,
    @SerialName("prompt_extend") val promptExtend: Boolean? = null,
    val watermark: Boolean? = null
)

@Serializable
data class QwenVideoGenerationRequest(
    val model: String,
    val input: QwenVideoInput,
    val parameters: QwenVideoParameters
)

@Serializable
data class QwenTaskCreateOutput(
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("task_status") val taskStatus: String? = null
)

@Serializable
data class QwenTaskCreateResponse(
    val output: QwenTaskCreateOutput? = null,
    @SerialName("request_id") val requestId: String? = null,
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class QwenResult(
    val url: String? = null,
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class QwenChoiceContent(
    val image: String? = null,
    val type: String? = null
)

@Serializable
data class QwenChoiceMessage(
    val content: List<QwenChoiceContent>? = null
)

@Serializable
data class QwenChoice(
    val message: QwenChoiceMessage? = null
)

@Serializable
data class QwenTaskOutput(
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("task_status") val taskStatus: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    val results: List<QwenResult>? = null,
    val choices: List<QwenChoice>? = null,
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class QwenTaskQueryResponse(
    val output: QwenTaskOutput? = null,
    @SerialName("request_id") val requestId: String? = null,
    val code: String? = null,
    val message: String? = null
)
