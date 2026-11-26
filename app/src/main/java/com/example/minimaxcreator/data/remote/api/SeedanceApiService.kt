package com.example.minimaxcreator.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class SeedanceVideoRequest(
    val prompt: String,
    val resolution: String = "720p",
    val duration: String = "5",
    @SerialName("aspect_ratio") val aspectRatio: String = "16:9",
    @SerialName("generate_audio") val generateAudio: Boolean = true,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class SeedanceQueueResponse(
    @SerialName("request_id") val requestId: String? = null,
    val status: String? = null,
    @SerialName("status_url") val statusUrl: String? = null,
    @SerialName("response_url") val responseUrl: String? = null,
    val error: String? = null
)

@Serializable
data class SeedanceQueueStatusResponse(
    val status: String? = null,
    val error: String? = null,
    val logs: List<SeedanceLog>? = null
)

@Serializable
data class SeedanceLog(
    val message: String? = null,
    val level: String? = null
)

@Serializable
data class SeedanceFile(
    val url: String? = null,
    @SerialName("content_type") val contentType: String? = null
)

@Serializable
data class SeedanceVideoResultResponse(
    val video: SeedanceFile? = null,
    val seed: Long? = null,
    val error: String? = null
)

interface SeedanceApiService {
    @POST("v1/seedance/{mode}")
    suspend fun createVideo(
        @Header("Authorization") authorization: String,
        @Path("mode") mode: String,
        @Body request: SeedanceVideoRequest
    ): SeedanceQueueResponse

    @GET("v1/seedance/{mode}/requests/{requestId}/status")
    suspend fun queryStatus(
        @Header("Authorization") authorization: String,
        @Path("mode") mode: String,
        @Path("requestId") requestId: String
    ): SeedanceQueueStatusResponse

    @GET("v1/seedance/{mode}/requests/{requestId}/result")
    suspend fun getResult(
        @Header("Authorization") authorization: String,
        @Path("mode") mode: String,
        @Path("requestId") requestId: String
    ): SeedanceVideoResultResponse
}
