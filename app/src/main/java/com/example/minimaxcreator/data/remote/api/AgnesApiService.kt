package com.example.minimaxcreator.data.remote.api

import com.example.minimaxcreator.data.remote.dto.AgnesVideoCreateRequest
import com.example.minimaxcreator.data.remote.dto.AgnesVideoCreateResponse
import com.example.minimaxcreator.data.remote.dto.AgnesVideoStatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AgnesApiService {
    @POST("v1/videos")
    suspend fun createVideo(@Body request: AgnesVideoCreateRequest): AgnesVideoCreateResponse

    @GET("agnesapi")
    suspend fun queryVideo(
        @Query("video_id") videoId: String,
        @Query("model_name") modelName: String
    ): AgnesVideoStatusResponse
}
