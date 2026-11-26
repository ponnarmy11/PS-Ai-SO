package com.example.minimaxcreator.data.model

import com.example.minimaxcreator.data.local.db.PlatformType
import com.example.minimaxcreator.data.local.db.SubMode
import com.example.minimaxcreator.data.local.db.TopMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface ModelParams {
    val model: String
    val platform: PlatformType
    val topMode: TopMode
}

@Serializable
data class MiniMaxImageParams(
    override val model: String = ModelCatalog.MINIMAX_IMAGE_01,
    val aspectRatio: String = "1:1",
    val n: Int = 1,
    val seed: Long? = null,
    val promptOptimizer: Boolean = false,
    val aigcWatermark: Boolean = false,
    val width: Int? = null,
    val height: Int? = null
) : ModelParams {
    override val platform: PlatformType = PlatformType.MINIMAX
    override val topMode: TopMode = TopMode.IMAGE
}

@Serializable
data class QwenImageParams(
    override val model: String = ModelCatalog.QWEN_IMAGE,
    val size: String = "1024*1024",
    val n: Int = 1,
    val seed: Int? = null,
    val promptExtend: Boolean = true,
    val promptExtendMode: String = "direct",
    val enableThinking: Boolean = true,
    val watermark: Boolean = false,
    val negativePrompt: String = ""
) : ModelParams {
    override val platform: PlatformType = PlatformType.QWEN
    override val topMode: TopMode = TopMode.IMAGE
}

@Serializable
data class MiniMaxVideoParams(
    override val model: String = ModelCatalog.MINIMAX_H3,
    val resolution: String = "768P",
    val duration: Int = 5,
    val ratio: String = "16:9",
    val aigcWatermark: Boolean = false
) : ModelParams {
    override val platform: PlatformType = PlatformType.MINIMAX
    override val topMode: TopMode = TopMode.VIDEO
}

@Serializable
data class AgnesVideoParams(
    override val model: String = ModelCatalog.AGNES_VIDEO_FLASH,
    val seconds: Int = 5,
    val aspectRatio: String = "16:9",
    val seed: Int? = null
) : ModelParams {
    override val platform: PlatformType = PlatformType.AGNES
    override val topMode: TopMode = TopMode.VIDEO
}

@Serializable
data class SeedanceVideoParams(
    override val model: String = ModelCatalog.SEEDANCE_25,
    val resolution: String = "720p",
    val duration: Int = 5,
    val ratio: String = "16:9",
    val generateAudio: Boolean = true,
    val promptOptimizer: Boolean = true
) : ModelParams {
    override val platform: PlatformType = PlatformType.FAL
    override val topMode: TopMode = TopMode.VIDEO
}

@Serializable
data class FalVideoParams(
    override val model: String,
    val resolution: String = "1080p",
    val duration: Int = 5,
    val ratio: String = "16:9",
    val generateAudio: Boolean = true
) : ModelParams {
    override val platform: PlatformType = PlatformType.FAL
    override val topMode: TopMode = TopMode.VIDEO
}

@Serializable
data class QwenVideoParams(
    override val model: String = ModelCatalog.QWEN_VIDEO,
    val resolution: String = "480P",
    val ratio: String = "adaptive",
    val duration: Int = 5,
    val promptExtend: Boolean = true,
    val watermark: Boolean = false,
    val negativePrompt: String = "",
    val audioUrl: String = "",
    val audio: Boolean = true,
    val seed: Int? = null
) : ModelParams {
    override val platform: PlatformType = PlatformType.QWEN
    override val topMode: TopMode = TopMode.VIDEO
}

object ModelCatalog {
    const val MINIMAX_IMAGE_01 = "image-01"
    const val MINIMAX_IMAGE_01_LIVE = "image-01-live"
    const val QWEN_IMAGE = "qwen-image-3.0-pro"
    const val MINIMAX_H3 = "MiniMax-H3"
    const val QWEN_VIDEO = "wan3.0-video"
    const val AGNES_VIDEO_FLASH = "agnes-video-2.5-flash"
    const val SEEDANCE_25 = "Seedance 2.5"
    const val KLING_V4 = "kling-v4"
    const val WAN_30 = "wan-3"
    const val HAILUO_23 = "hailuo-23"

    val FAL_VIDEO_MODELS = listOf(KLING_V4, WAN_30, HAILUO_23)
    val IMAGE_MODELS = listOf(MINIMAX_IMAGE_01, MINIMAX_IMAGE_01_LIVE, QWEN_IMAGE)
    val VIDEO_MODELS = listOf(MINIMAX_H3, QWEN_VIDEO, SEEDANCE_25) + FAL_VIDEO_MODELS

    fun displayName(model: String): String = when (model) {
        KLING_V4 -> "Kling 4.0"
        WAN_30 -> "Wan 3.0"
        HAILUO_23 -> "Hailuo 2.3 Pro"
        else -> model
    }

    fun isQwenImage(model: String): Boolean = model == QWEN_IMAGE
    fun isQwen(model: String): Boolean = model == QWEN_IMAGE || model == QWEN_VIDEO
    fun isVideoEdit(model: String): Boolean = false

    fun defaultParams(model: String, topMode: TopMode): ModelParams {
        return when {
            topMode == TopMode.IMAGE && model == QWEN_IMAGE -> QwenImageParams()
            topMode == TopMode.VIDEO && model == QWEN_VIDEO -> QwenVideoParams()
            topMode == TopMode.VIDEO && model == AGNES_VIDEO_FLASH -> AgnesVideoParams()
            topMode == TopMode.VIDEO && model == SEEDANCE_25 -> SeedanceVideoParams()
            topMode == TopMode.VIDEO && model in FAL_VIDEO_MODELS -> FalVideoParams(model = model)
            topMode == TopMode.IMAGE -> MiniMaxImageParams(model = model)
            else -> MiniMaxVideoParams()
        }
    }

    fun encodeParams(json: Json, params: ModelParams): String {
        return when (params) {
            is MiniMaxImageParams -> json.encodeToString(MiniMaxImageParams.serializer(), params)
            is QwenImageParams -> json.encodeToString(QwenImageParams.serializer(), params)
            is MiniMaxVideoParams -> json.encodeToString(MiniMaxVideoParams.serializer(), params)
            is QwenVideoParams -> json.encodeToString(QwenVideoParams.serializer(), params)
            is AgnesVideoParams -> json.encodeToString(AgnesVideoParams.serializer(), params)
            is SeedanceVideoParams -> json.encodeToString(SeedanceVideoParams.serializer(), params)
            is FalVideoParams -> json.encodeToString(FalVideoParams.serializer(), params)
        }
    }

    fun decodeParams(
        json: Json,
        raw: String,
        model: String,
        topMode: TopMode
    ): ModelParams {
        if (raw.isBlank()) return defaultParams(model, topMode)
        val fallback = defaultParams(model, topMode)
        return runCatching {
            when (fallback) {
                is MiniMaxImageParams -> json.decodeFromString(MiniMaxImageParams.serializer(), raw)
                is QwenImageParams -> json.decodeFromString(QwenImageParams.serializer(), raw)
                is MiniMaxVideoParams -> json.decodeFromString(MiniMaxVideoParams.serializer(), raw)
                is QwenVideoParams -> json.decodeFromString(QwenVideoParams.serializer(), raw)
                is AgnesVideoParams -> json.decodeFromString(AgnesVideoParams.serializer(), raw)
                is SeedanceVideoParams -> json.decodeFromString(SeedanceVideoParams.serializer(), raw)
                is FalVideoParams -> json.decodeFromString(FalVideoParams.serializer(), raw)
            }
        }.getOrDefault(fallback)
    }

    fun platformFor(model: String): PlatformType {
        return when (model) { AGNES_VIDEO_FLASH -> PlatformType.AGNES; SEEDANCE_25, KLING_V4, WAN_30, HAILUO_23 -> PlatformType.FAL; QWEN_IMAGE, QWEN_VIDEO -> PlatformType.QWEN; else -> PlatformType.MINIMAX }
    }

    fun subModeFor(model: String, topMode: TopMode, hasImage: Boolean): SubMode {
        return when {
            isVideoEdit(model) -> SubMode.VIDEO_EDIT
            topMode == TopMode.IMAGE -> if (hasImage) SubMode.IMAGE_TO_IMAGE else SubMode.TEXT_TO_IMAGE
            else -> if (hasImage) SubMode.IMAGE_TO_VIDEO else SubMode.TEXT_TO_VIDEO
        }
    }
}
