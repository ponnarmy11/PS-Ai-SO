package com.example.minimaxcreator.data.remote.api

import com.example.minimaxcreator.data.remote.dto.ImageGenerationRequest
import com.example.minimaxcreator.data.remote.dto.ImageGenerationResponse
import com.example.minimaxcreator.data.remote.dto.VideoGenerationV2Request
import com.example.minimaxcreator.data.remote.dto.VideoGenerationV2Response
import com.example.minimaxcreator.data.remote.dto.VideoTaskQueryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MiniMaxApiService {
    @POST("v1/image_generation")
    suspend fun generateImage(@Body request: ImageGenerationRequest): ImageGenerationResponse

    @POST("v2/video_generation")
    suspend fun createVideo(@Body request: VideoGenerationV2Request): VideoGenerationV2Response

    @GET("v2/query/video_generation/{taskId}")
    suspend fun queryVideo(@Path("taskId") taskId: String): VideoTaskQueryResponse
}
