package com.example.minimaxcreator.data.repository

import com.example.minimaxcreator.data.local.db.GenerationRecordDao
import com.example.minimaxcreator.data.local.db.GenerationRecordEntity
import com.example.minimaxcreator.data.remote.api.MiniMaxApiService
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

@Singleton
class GenerationRepository @Inject constructor(
    private val miniMaxApi: MiniMaxApiService,
    private val qwenApi: QwenApiService,
    private val dao: GenerationRecordDao
) {
    suspend fun generateMiniMaxImage(request: ImageGenerationRequest): ImageGenerationResponse {
        val response = miniMaxApi.generateImage(request)
        val baseResp = response.baseResp
        if (baseResp != null && baseResp.statusCode != null && baseResp.statusCode != 0) {
            error(baseResp.statusMessage ?: "图片生成失败")
        }
        return response
    }

    suspend fun createMiniMaxVideo(request: VideoGenerationV2Request): VideoGenerationV2Response {
        return miniMaxApi.createVideo(request)
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
                    val url = task.output?.videoUrl ?: error("任务成功但未返回视频地址")
                    return listOf(url)
                }
                "FAILED" -> error(task.output?.message ?: "Qwen 视频生成失败")
                "CANCELED" -> error("Qwen 任务已取消")
                "UNKNOWN" -> error("Qwen 任务不存在或已过期")
                else -> {
                    onProgress(status, task.output?.message)
                    interval = (interval * 1.5).coerceAtMost(5_000.0).toLong()
                }
            }
        }
        error("Qwen 任务超时，请稍后重试")
    }

    suspend fun pollMiniMaxVideo(
        taskId: String,
        onProgress: suspend (status: String, message: String?) -> Unit
    ): List<String> {
        var interval = 2_000L
        val timeout = System.currentTimeMillis() + 10 * 60 * 1000L
        while (System.currentTimeMillis() < timeout) {
            delay(interval)
            val task = miniMaxApi.queryVideo(taskId).task ?: error("视频任务不存在")
            when (task.status) {
                "succeeded" -> return listOfNotNull(task.content?.url)
                "failed" -> error(task.error?.message ?: "视频生成失败")
                "cancelled" -> error("视频任务已取消")
                else -> {
                    onProgress(task.status ?: "running", task.error?.message)
                    interval = (interval * 1.5).coerceAtMost(5_000.0).toLong()
                }
            }
        }
        error("视频生成超时，请稍后重试")
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
            error(message ?: code ?: "请求失败")
        }
    }
}
