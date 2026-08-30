package com.example.minimaxcreator.data.remote.api

import com.example.minimaxcreator.data.remote.dto.QwenImageGenerationRequest
import com.example.minimaxcreator.data.remote.dto.QwenImageGenerationResponse
import com.example.minimaxcreator.data.remote.dto.QwenTaskCreateResponse
import com.example.minimaxcreator.data.remote.dto.QwenTaskQueryResponse
import com.example.minimaxcreator.data.remote.dto.QwenVideoGenerationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface QwenApiService {
    @POST("services/aigc/multimodal-generation/generation")
    suspend fun generateImage(
        @Body request: QwenImageGenerationRequest
    ): QwenImageGenerationResponse

    @Headers("X-DashScope-Async: enable")
    @POST("services/aigc/video-generation/video-synthesis")
    suspend fun createVideoTask(
        @Body request: QwenVideoGenerationRequest
    ): QwenTaskCreateResponse

    @GET("tasks/{taskId}")
    suspend fun queryTask(
        @Path("taskId") taskId: String
    ): QwenTaskQueryResponse
}
