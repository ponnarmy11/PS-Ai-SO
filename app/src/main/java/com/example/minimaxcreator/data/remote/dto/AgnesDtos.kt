package com.example.minimaxcreator.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AgnesVideoCreateRequest(
    val model: String = "agnes-video-2.5-flash",
    val prompt: String,
    val mode: String = "text",
    val seconds: String = "5",
    val size: String = "720P",
    val aspect_ratio: String = "16:9",
    val n: Int = 1,
    val first_frame: String? = null
)

@Serializable
data class AgnesVideoCreateResponse(
    val id: String? = null,
    val task_id: String? = null,
    val video_id: String? = null,
    val status: String? = null,
    val message: String? = null
)

@Serializable
data class AgnesVideoStatusResponse(
    val status: String? = null,
    val url: String? = null,
    val message: String? = null,
    val error: String? = null
)
