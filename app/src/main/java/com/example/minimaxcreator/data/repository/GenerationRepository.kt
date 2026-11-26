package com.example.minimaxcreator.data.repository

import com.example.minimaxcreator.data.local.db.GenerationRecordDao
import com.example.minimaxcreator.data.local.db.GenerationRecordEntity
import com.example.minimaxcreator.data.remote.api.MiniMaxApiService
import com.example.minimaxcreator.data.remote.api.AgnesApiService
import com.example.minimaxcreator.data.remote.api.SeedanceApiService
import com.example.minimaxcreator.data.remote.api.SeedanceVideoRequest
import com.example.minimaxcreator.data.remote.api.AgnesVideoCreateRequest
import com.example.minimaxcreator.data.remote.api.QwenApiService
import com.example.minimaxcreator.data.remote.dto.ImageGenerationRequest
import com.example.minimaxcreator.data.remote.dto.ImageGenerationResponse
import com.example.minimaxcreator.data.remote.dto.QwenImageGenerationRequest
import com.example.minimaxcreator.data.remote.dto.QwenImageGenerationResponse
import com.example.minimaxcreator.data.remote.dto.QwenTaskCreateResponse
import com.example.minimaxcreator.data.remote.dto.QwenTaskQueryResponse
import com.example.minimaxcreator.data.remote.dto.QwenVideoGenerationRequest
import com.example.minimaxcreator.data.remote.dto.VideoGenerationV2Request
import com.example.minimaxcreator.data.remote.dto.VideoGenerationV2Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

@Singleton
class GenerationRepository @Inject constructor(
    private val miniMaxApi: MiniMaxApiService,
    private val qwenApi: QwenApiService,
    private val agnesApi: AgnesApiService,
    private val seedanceApi: SeedanceApiService,
    private val dao: GenerationRecordDao
) {
    suspend fun generateMiniMaxImage(request: ImageGenerationRequest): ImageGenerationResponse {
        val response = miniMaxApi.generateImage(request)
        val baseResp = response.baseResp
        if (baseResp != null && baseResp.statusCode != null && baseResp.statusCode != 0) {
            error(baseResp.statusMessage ?: "Image generation failed")
        }
        return response
    }

    suspend fun createMiniMaxVideo(request: VideoGenerationV2Request): VideoGenerationV2Response {
        return miniMaxApi.createVideo(request)
    }


    suspend fun createAgnesVideoTask(apiKey: String, request: AgnesVideoCreateRequest): String {
        val response = agnesApi.createVideo("Bearer $apiKey", request)
        return (response.video_id ?: response.id ?: response.task_id)
            ?.takeIf { it.isNotBlank() }
            ?: error(response.error?.message ?: response.message ?: "Agnes did not return a video task ID")
    }

    suspend fun pollAgnesVideo(
        apiKey: String,
        videoId: String,
        onProgress: suspend (status: String, message: String?) -> Unit
    ): List<String> {
        var interval = 3_000L
        var consecutiveFailures = 0
        val timeout = System.currentTimeMillis() + 20 * 60 * 1000L
        while (System.currentTimeMillis() < timeout) {
            delay(interval)
            val task = try {
                agnesApi.queryVideo("Bearer $apiKey", videoId, "agnes-video-2.5-flash")
            } catch (e: HttpException) {
                val retryable = e.code() in setOf(408, 429, 500, 502, 503, 504, 520, 522, 524)
                if (!retryable || consecutiveFailures >= 5) throw e
                consecutiveFailures++
                val retryAfter = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                val waitMs = retryAfter?.times(1_000L)
                    ?: (3_000L * (1L shl (consecutiveFailures - 1))).coerceAtMost(30_000L)
                onProgress("retrying", "Agnes temporarily unavailable (HTTP ${e.code()}). Retrying in ${waitMs / 1_000}s…")
                delay(waitMs)
                continue
            } catch (e: IOException) {
                if (consecutiveFailures >= 5) throw e
                consecutiveFailures++
                val waitMs = (3_000L * (1L shl (consecutiveFailures - 1))).coerceAtMost(30_000L)
                onProgress("retrying", "Network interrupted. Retrying in ${waitMs / 1_000}s…")
                delay(waitMs)
                continue
            }
            consecutiveFailures = 0
            when (task.status?.lowercase()) {
                "completed", "succeeded", "success" ->
                    return listOfNotNull(task.url ?: task.video_url)
                        .ifEmpty { error("Agnes task completed but no video URL was returned") }
                "failed", "error" -> error(task.error?.message ?: task.message ?: "Agnes video generation failed")
                else -> {
                    onProgress(task.status ?: "processing", task.message)
                    interval = (interval * 1.25).coerceAtMost(8_000.0).toLong()
                }
            }
        }
        error("Agnes video generation timed out. Check History before retrying.")
    }

    suspend fun createSeedanceVideoTask(
        apiKey: String,
        mode: String,
        request: SeedanceVideoRequest
    ): String {
        val response = seedanceApi.createVideo("Bearer $apiKey", mode, request)
        return response.requestId?.takeIf { it.isNotBlank() }
            ?: error(response.error ?: "Seedance did not return a request ID")
    }

    suspend fun pollSeedanceVideo(
        apiKey: String,
        mode: String,
        requestId: String,
        onProgress: suspend (status: String, message: String?) -> Unit
    ): List<String> {
        val authorization = "Bearer $apiKey"
        var interval = 3_000L
        val timeout = System.currentTimeMillis() + 30 * 60 * 1000L
        while (System.currentTimeMillis() < timeout) {
            delay(interval)
            val status = seedanceApi.queryStatus(authorization, mode, requestId)
            when (status.status?.uppercase()) {
                "COMPLETED" -> {
                    val result = seedanceApi.getResult(authorization, mode, requestId)
                    return listOfNotNull(result.video?.url)
                        .ifEmpty { error(result.error ?: "Seedance completed but returned no video URL") }
                }
                "FAILED", "ERROR" -> error(status.error ?: status.logs?.lastOrNull()?.message ?: "Seedance video generation failed")
                else -> {
                    onProgress(status.status ?: "IN_PROGRESS", status.logs?.lastOrNull()?.message)
                    interval = (interval * 1.25).coerceAtMost(8_000.0).toLong()
                }
            }
        }
        error("Seedance video generation timed out. Check History before retrying.")
    }

    suspend fun generateQwenImage(request: QwenImageGenerationRequest): QwenImageGenerationResponse {
        val response = qwenApi.generateImage(request)
        checkResponseError(response.code, response.message)
        return response
    }

    suspend fun createQwenVideoTask(request: QwenVideoGenerationRequest): QwenTaskCreateResponse {
        val response = qwenApi.createVideoTask(request)
        checkResponseError(response.code, response.message)
        return response
    }

    suspend fun queryQwenTask(taskId: String): QwenTaskQueryResponse {
        val response = qwenApi.queryTask(taskId)
        checkResponseError(response.code, response.message)
        return response
    }

    suspend fun pollQwenTask(
        taskId: String,
        onProgress: suspend (status: String, message: String?) -> Unit
    ): List<String> {
        var interval = 2_000L
        val timeout = System.currentTimeMillis() + 15 * 60 * 1000L
        while (System.currentTimeMillis() < timeout) {
            delay(interval)
            val task = queryQwenTask(taskId)
            val status = task.output?.taskStatus ?: continue
            when (status) {
                "SUCCEEDED" -> {
                    val url = task.output?.videoUrl ?: error("Task succeeded but no video URL was returned")
                    return listOf(url)
                }
                "FAILED" -> error(task.output?.message ?: "Qwen video generation failed")
                "CANCELED" -> error("Qwen task was cancelled")
                "UNKNOWN" -> error("Qwen task does not exist or has expired")
                else -> {
                    onProgress(status, task.output?.message)
                    interval = (interval * 1.5).coerceAtMost(5_000.0).toLong()
                }
            }
        }
        error("Qwen task timed out. Please try again later.")
    }

    suspend fun pollMiniMaxVideo(
        taskId: String,
        onProgress: suspend (status: String, message: String?) -> Unit
    ): List<String> {
        var interval = 2_000L
        val timeout = System.currentTimeMillis() + 10 * 60 * 1000L
        while (System.currentTimeMillis() < timeout) {
            delay(interval)
            val task = miniMaxApi.queryVideo(taskId).task ?: error("Video task does not exist")
            when (task.status) {
                "succeeded" -> return listOfNotNull(task.content?.url)
                "failed" -> error(task.error?.message ?: "Video generation failed")
                "cancelled" -> error("Video task was cancelled")
                else -> {
                    onProgress(task.status ?: "running", task.error?.message)
                    interval = (interval * 1.5).coerceAtMost(5_000.0).toLong()
                }
            }
        }
        error("Video generation timed out. Please try again later.")
    }

    suspend fun upsertRecord(record: GenerationRecordEntity) {
        dao.upsert(record)
    }

    suspend fun deleteRecord(id: String) {
        dao.deleteById(id)
    }

    suspend fun deleteRecords(ids: List<String>) {
        dao.deleteByIds(ids)
    }

    suspend fun clearRecords() {
        dao.deleteAll()
    }

    private fun checkResponseError(code: String?, message: String?) {
        if (!code.isNullOrBlank() || !message.isNullOrBlank()) {
            error(message ?: code ?: "Request failed")
        }
    }
}
