package com.example.minimaxcreator.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class AgnesVideoCreateRequest(
    val model: String = "agnes-video-2.5-flash",
    val prompt: String,
    val mode: String = "text",
    val seconds: String = "5",
    val size: String = "720P",
    val aspect_ratio: String = "16:9",
    val seed: Int? = null,
    val n: Int = 1
)

@Serializable
data class AgnesVideoCreateResponse(
    val id: String? = null,
    val task_id: String? = null,
    val video_id: String? = null,
    val status: String? = null,
    val message: String? = null,
    val error: AgnesApiError? = null
)

@Serializable
data class AgnesApiError(val message: String? = null, val code: String? = null)

@Serializable
data class AgnesVideoStatusResponse(
    val status: String? = null,
    val url: String? = null,
    val video_url: String? = null,
    val progress: Int? = null,
    val message: String? = null,
    val error: AgnesApiError? = null
)

interface AgnesApiService {
    @POST("v1/videos")
    suspend fun createVideo(
        @Header("Authorization") authorization: String,
        @Body request: AgnesVideoCreateRequest
    ): AgnesVideoCreateResponse

    @GET("agnesapi")
    suspend fun queryVideo(
        @Header("Authorization") authorization: String,
        @Query("video_id") videoId: String,
        @Query("model_name") model: String
    ): AgnesVideoStatusResponse
}
