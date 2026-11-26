package com.example.minimaxcreator.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class FalVideoRequest(
    val prompt: String,
    val resolution: String = "1080p",
    val duration: Int = 5,
    @SerialName("aspect_ratio") val aspectRatio: String = "16:9",
    val audio: Boolean = true,
    @SerialName("image_url") val imageUrl: String? = null
)

interface FalVideoApiService {
    @POST("v1/fal/{model}/{mode}")
    suspend fun createVideo(
        @Header("Authorization") authorization: String,
        @Path("model") model: String,
        @Path("mode") mode: String,
        @Body request: FalVideoRequest
    ): SeedanceQueueResponse

    @GET("v1/fal/{model}/{mode}/requests/{requestId}/status")
    suspend fun queryStatus(
        @Header("Authorization") authorization: String,
        @Path("model") model: String,
        @Path("mode") mode: String,
        @Path("requestId") requestId: String
    ): SeedanceQueueStatusResponse

    @GET("v1/fal/{model}/{mode}/requests/{requestId}/result")
    suspend fun getResult(
        @Header("Authorization") authorization: String,
        @Path("model") model: String,
        @Path("mode") mode: String,
        @Path("requestId") requestId: String
    ): SeedanceVideoResultResponse
}
