package com.example.minimaxcreator.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimaxcreator.data.local.db.ChatMessage
import com.example.minimaxcreator.data.local.db.GenerationRecord
import com.example.minimaxcreator.data.local.db.GenerationStatus
import com.example.minimaxcreator.data.local.db.GenerationType
import com.example.minimaxcreator.data.local.db.MessageRole
import com.example.minimaxcreator.data.local.db.PlatformType
import com.example.minimaxcreator.data.local.db.SubMode
import com.example.minimaxcreator.data.local.db.TopMode
import com.example.minimaxcreator.data.local.db.toEntity
import com.example.minimaxcreator.data.model.MiniMaxImageParams
import com.example.minimaxcreator.data.model.MiniMaxVideoParams
import com.example.minimaxcreator.data.model.ModelCatalog
import com.example.minimaxcreator.data.model.ModelParams
import com.example.minimaxcreator.data.model.QwenImageParams
import com.example.minimaxcreator.data.model.QwenVideoParams
import com.example.minimaxcreator.data.remote.dto.ImageGenerationRequest
import com.example.minimaxcreator.data.remote.dto.ImageSubjectReference
import com.example.minimaxcreator.data.remote.dto.QwenImageContent
import com.example.minimaxcreator.data.remote.dto.QwenImageGenerationRequest
import com.example.minimaxcreator.data.remote.dto.QwenImageInput
import com.example.minimaxcreator.data.remote.dto.QwenImageMessage
import com.example.minimaxcreator.data.remote.dto.QwenImageParameters
import com.example.minimaxcreator.data.remote.dto.QwenMediaInput
import com.example.minimaxcreator.data.remote.dto.QwenVideoGenerationRequest
import com.example.minimaxcreator.data.remote.dto.QwenVideoInput
import com.example.minimaxcreator.data.remote.dto.QwenVideoParameters
import com.example.minimaxcreator.data.remote.dto.VideoContentItem
import com.example.minimaxcreator.data.remote.dto.VideoContentUrl
import com.example.minimaxcreator.data.remote.dto.VideoGenerationV2Request
import com.example.minimaxcreator.data.repository.GenerationRepository
import com.example.minimaxcreator.data.repository.ImageDataReader
import com.example.minimaxcreator.data.repository.MediaSaver
import com.example.minimaxcreator.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GenerationRepository,
    private val settingsRepository: SettingsRepository,
    private val imageDataReader: ImageDataReader,
    private val mediaSaver: MediaSaver,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { current ->
                    val available = availableModels(current.topMode)
                    val defaultModel = if (current.topMode == TopMode.IMAGE) {
                        settings.defaultImageModel.takeIf { it in available } ?: ModelCatalog.MINIMAX_IMAGE_01
                    } else {
                        settings.defaultVideoModel.takeIf { it in available } ?: ModelCatalog.MINIMAX_H3
                    }
                    val model = current.selectedModel.takeIf { it in available } ?: defaultModel
                    val params = if (current.selectedParams.model == model) {
                        current.selectedParams
                    } else {
                        decodeParams(settings.modelParams[model], model, current.topMode)
                    }
                    current.copy(
                        miniMaxApiKey = settings.miniMaxApiKey,
                        qwenApiKey = settings.qwenApiKey,
                        defaultImageModel = settings.defaultImageModel,
                        defaultVideoModel = settings.defaultVideoModel,
                        modelParams = settings.modelParams,
                        selectedModel = model,
                        selectedParams = params,
                        autoSave = settings.autoSave,
                        mode = resolveMode(current.topMode, model, current.attachments.isNotEmpty())
                    )
                }
            }
        }
    }

    fun setTopMode(topMode: TopMode) {
        _uiState.update { current ->
            val available = availableModels(topMode)
            val defaultModel = if (topMode == TopMode.IMAGE) {
                current.defaultImageModel.takeIf { it in available } ?: ModelCatalog.MINIMAX_IMAGE_01
            } else {
                current.defaultVideoModel.takeIf { it in available } ?: ModelCatalog.MINIMAX_H3
            }
            val model = current.selectedModel.takeIf { it in available } ?: defaultModel
            val params = decodeParams(current.modelParams[model], model, topMode)
            current.copy(
                topMode = topMode,
                selectedModel = model,
                selectedParams = params,
                mode = resolveMode(topMode, model, current.attachments.isNotEmpty())
            )
        }
    }

    fun setModel(model: String) {
        val current = _uiState.value
        val params = decodeParams(current.modelParams[model], model, current.topMode)
        _uiState.update {
            it.copy(
                selectedModel = model,
                selectedParams = params,
                mode = resolveMode(current.topMode, model, it.attachments.isNotEmpty())
            )
        }
    }

    fun updatePrompt(value: String) {
        _uiState.update { it.copy(prompt = value) }
    }

    fun addAttachment(uri: Uri) {
        _uiState.update {
            val max = when (val params = it.selectedParams) {
                is QwenImageParams -> 3
                is QwenVideoParams -> 1
                is MiniMaxImageParams -> 1
                is MiniMaxVideoParams -> 1
            }
            if (it.attachments.size >= max) it
            else it.copy(
                attachments = it.attachments + uri,
                mode = resolveMode(it.topMode, it.selectedModel, true)
            )
        }
    }

    fun removeAttachment(uri: Uri) {
        _uiState.update {
            val attachments = it.attachments.filterNot { item -> item == uri }
            it.copy(
                attachments = attachments,
                mode = resolveMode(it.topMode, it.selectedModel, attachments.isNotEmpty())
            )
        }
    }

    fun clearAttachments() {
        _uiState.update {
            it.copy(
                attachments = emptyList(),
                mode = resolveMode(it.topMode, it.selectedModel, false)
            )
        }
    }

    fun setParams(params: ModelParams) {
        val raw = ModelCatalog.encodeParams(json, params)
        _uiState.update {
            it.copy(
                selectedParams = params,
                modelParams = it.modelParams + (params.model to raw)
            )
        }
        viewModelScope.launch { settingsRepository.setModelParams(params.model, raw) }
    }

    fun generate() {
        if (_uiState.value.isGenerating) return
        viewModelScope.launch { launchGeneration(null) }
    }

    fun retry(message: ChatMessage) {
        if (_uiState.value.isGenerating) return
        val params = decodeParams(message.paramsJson, message.model, message.topMode)
        _uiState.update {
            it.copy(
                topMode = message.topMode,
                selectedModel = message.model,
                selectedParams = params,
                prompt = message.prompt,
                attachments = message.attachmentUris.map(Uri::parse),
                mode = message.type
            )
        }
        viewModelScope.launch { launchGeneration(message) }
    }

    fun prepareRegeneration(record: GenerationRecord) {
        val topMode = record.topMode
        val params = decodeParams(record.paramsJson, record.model, topMode)
        val attachments = runCatching {
            json.decodeFromString<List<String>>(record.attachmentsJson)
        }.getOrDefault(emptyList())
        _uiState.update {
            it.copy(
                topMode = topMode,
                selectedModel = record.model,
                selectedParams = params,
                prompt = record.prompt,
                attachments = attachments.map(Uri::parse),
                mode = generationTypeFor(record)
            )
        }
    }

    fun saveRemote(url: String, isVideo: Boolean) {
        viewModelScope.launch {
            runCatching { mediaSaver.saveRemote(url, isVideo) }
                .onSuccess { _events.emit("Saved to Gallery") }
                .onFailure { _events.emit("Save failed: ${it.message ?: "Unknown error"}") }
        }
    }

    fun shareRemote(url: String, isVideo: Boolean) {
        viewModelScope.launch {
            runCatching { mediaSaver.shareRemote(url, isVideo) }
                .onFailure { _events.emit("Share failed: ${it.message ?: "Unknown error"}") }
        }
    }

    private suspend fun launchGeneration(seed: ChatMessage?) {
        val state = _uiState.value
        val params = state.selectedParams
        val prompt = state.prompt
        val attachments = state.attachments
        val platformKey = when (params.platform) {
            PlatformType.MINIMAX -> state.miniMaxApiKey
            PlatformType.QWEN -> state.qwenApiKey
        }
        if (platformKey.isBlank()) {
            _events.emit(
                if (params.platform == PlatformType.MINIMAX) "Please configure your MiniMax API Key first"
                else "Please configure your Qwen (DashScope) API Key first"
            )
            return
        }
        if (prompt.isBlank()) {
            _events.emit("Enter a prompt to create")
            return
        }
        try {
            validateParams(params, attachments)
        } catch (t: Throwable) {
            _events.emit(friendlyError(t))
            return
        }
        val mode = resolveMode(state.topMode, state.selectedModel, attachments.isNotEmpty())

        val userId = UUID.randomUUID().toString()
        val aiId = seed?.id ?: UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            id = userId,
            role = MessageRole.USER,
            type = mode,
            prompt = prompt,
            attachmentUris = attachments.map(Uri::toString),
            status = GenerationStatus.SUCCESS,
            completedAt = System.currentTimeMillis(),
            platform = params.platform,
            model = params.model,
            topMode = params.topMode,
            subMode = ModelCatalog.subModeFor(params.model, params.topMode, attachments.isNotEmpty())
        )
        val aiMessage = ChatMessage(
            id = aiId,
            role = MessageRole.AI,
            type = mode,
            prompt = prompt,
            attachmentUris = attachments.map(Uri::toString),
            paramsJson = ModelCatalog.encodeParams(json, params),
            status = GenerationStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            platform = params.platform,
            model = params.model,
            topMode = params.topMode,
            subMode = ModelCatalog.subModeFor(params.model, params.topMode, attachments.isNotEmpty())
        )
        val newMessages = if (seed == null) {
            state.messages + listOf(userMessage, aiMessage)
        } else {
            state.messages.filterNot { it.id == seed.id } + listOf(userMessage, aiMessage)
        }
        _uiState.update {
            it.copy(prompt = "", attachments = emptyList(), messages = newMessages, isGenerating = true)
        }
        repository.upsertRecord(aiMessage.toRecord().toEntity())
        try {
            val dataUris = attachments.map { imageDataReader.read(it, maxImageBytes(params)) }
            when (params) {
                is MiniMaxImageParams -> generateMiniMaxImage(aiMessage, params, dataUris)
                is QwenImageParams -> generateQwenImage(aiMessage, params, dataUris)
                is MiniMaxVideoParams -> generateMiniMaxVideo(aiMessage, params, dataUris)
                is QwenVideoParams -> generateQwenVideo(aiMessage, params, dataUris)
            }
        } catch (t: Throwable) {
            val failed = aiMessage.copy(
                status = GenerationStatus.FAILED,
                errorMessage = friendlyError(t),
                completedAt = System.currentTimeMillis()
            )
            updateMessage(failed)
            repository.upsertRecord(failed.toRecord().toEntity())
            _events.emit(failed.errorMessage ?: "Generation failed")
        } finally {
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    private suspend fun generateMiniMaxImage(
        message: ChatMessage,
        params: MiniMaxImageParams,
        dataUris: List<String>
    ) {
        val request = ImageGenerationRequest(
            model = params.model,
            prompt = message.prompt.take(1500),
            subjectReference = if (message.type == GenerationType.IMAGE_TO_IMAGE) {
                dataUris.map { ImageSubjectReference(imageFile = it) }
            } else null,
            aspectRatio = if (params.width != null && params.height != null) null else params.aspectRatio,
            width = params.width,
            height = params.height,
            seed = params.seed,
            n = params.n,
            promptOptimizer = params.promptOptimizer,
            aigcWatermark = params.aigcWatermark
        )
        val response = repository.generateMiniMaxImage(request)
        val urls = response.data?.imageUrls.orEmpty()
        if (urls.isEmpty()) error(response.baseResp?.statusMessage ?: "Image generation returned no result")
        completeWithUrls(message, urls, false)
    }

    private suspend fun generateQwenImage(
        message: ChatMessage,
        params: QwenImageParams,
        dataUris: List<String>
    ) {
        val content = buildList {
            dataUris.forEach { add(QwenImageContent(image = it)) }
            add(QwenImageContent(text = message.prompt.take(4500)))
        }
        val request = QwenImageGenerationRequest(
            model = params.model,
            input = QwenImageInput(messages = listOf(QwenImageMessage(content = content))),
            parameters = QwenImageParameters(
                size = params.size,
                n = params.n,
                seed = params.seed,
                promptExtend = params.promptExtend,
                promptExtendMode = if (dataUris.isNotEmpty()) "direct" else params.promptExtendMode,
                enableThinking = params.enableThinking,
                watermark = params.watermark,
                negativePrompt = params.negativePrompt
            )
        )
        val response = repository.generateQwenImage(request)
        val urls = response.output?.choices.orEmpty()
            .flatMap { it.message?.content.orEmpty() }
            .mapNotNull { it.image }
        if (urls.isEmpty()) error(response.message ?: "Qwen Image generation returned no result")
        completeWithUrls(message, urls, false)
    }

    private suspend fun generateMiniMaxVideo(
        message: ChatMessage,
        params: MiniMaxVideoParams,
        dataUris: List<String>
    ) {
        val content = buildList {
            add(VideoContentItem(type = "text", text = message.prompt.take(1500)))
            if (message.type == GenerationType.IMAGE_TO_VIDEO && dataUris.isNotEmpty()) {
                add(VideoContentItem(
                    type = "image_url",
                    imageUrl = VideoContentUrl(dataUris.first()),
                    role = "first_frame"
                ))
            }
        }
        val request = VideoGenerationV2Request(
            content = content,
            resolution = params.resolution,
            duration = params.duration,
            ratio = if (message.type == GenerationType.IMAGE_TO_VIDEO) "adaptive" else params.ratio,
            aigcWatermark = params.aigcWatermark
        )
        val response = repository.createMiniMaxVideo(request)
        val taskId = response.taskId ?: error("Failed to create video task")
        val queued = message.copy(taskId = taskId, progressText = "Queued…")
        updateMessage(queued)
        repository.upsertRecord(queued.toRecord().toEntity())
        val urls = repository.pollMiniMaxVideo(taskId) { status, taskMessage ->
            updateVideoProgress(message, taskId, status, taskMessage)
        }
        completeWithUrls(message.copy(taskId = taskId), urls, true)
    }

    private suspend fun generateQwenVideo(
        message: ChatMessage,
        params: QwenVideoParams,
        dataUris: List<String>
    ) {
        val isImageVideo = message.type == GenerationType.IMAGE_TO_VIDEO
        val input = QwenVideoInput(
            prompt = message.prompt.take(20000),
            negativePrompt = params.negativePrompt.ifBlank { null },
            audioUrl = params.audioUrl.ifBlank { null },
            media = if (isImageVideo && dataUris.isNotEmpty()) {
                listOf(QwenMediaInput(type = "first_frame", url = dataUris.first()))
            } else null
        )
        val request = QwenVideoGenerationRequest(
            model = params.model,
            input = input,
            parameters = QwenVideoParameters(
                resolution = params.resolution,
                ratio = if (isImageVideo) "adaptive" else params.ratio,
                duration = params.duration,
                audio = params.audio,
                seed = params.seed,
                promptExtend = params.promptExtend,
                watermark = params.watermark
            )
        )
        val response = repository.createQwenVideoTask(request)
        val taskId = response.output?.taskId ?: error("Qwen Failed to create video task")
        val queued = message.copy(taskId = taskId, progressText = "Queued…")
        updateMessage(queued)
        repository.upsertRecord(queued.toRecord().toEntity())
        val urls = repository.pollQwenTask(taskId) { status, taskMessage ->
            updateVideoProgress(message, taskId, status, taskMessage)
        }
        completeWithUrls(message.copy(taskId = taskId), urls, true)
    }

    private suspend fun updateVideoProgress(
        message: ChatMessage,
        taskId: String,
        status: String,
        taskMessage: String?
    ) {
        val updated = message.copy(
            taskId = taskId,
            progressText = when (status.lowercase()) {
                "queued", "pending" -> "Queued…"
                "running" -> "Processing…"
                else -> "Processing…"
            },
            errorMessage = taskMessage
        )
        updateMessage(updated)
        repository.upsertRecord(updated.toRecord().toEntity())
    }

    private suspend fun completeWithUrls(
        message: ChatMessage,
        urls: List<String>,
        isVideo: Boolean
    ) {
        if (_uiState.value.autoSave) urls.forEach { mediaSaver.saveRemote(it, isVideo) }
        val completed = message.copy(
            resultUrls = urls,
            thumbnailUrl = urls.firstOrNull(),
            status = GenerationStatus.SUCCESS,
            completedAt = System.currentTimeMillis()
        )
        updateMessage(completed)
        repository.upsertRecord(completed.toRecord().toEntity())
        _events.emit("Generation completed")
    }

    private fun updateMessage(updated: ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { message ->
                if (message.id == updated.id) updated else message
            })
        }
    }

    private fun validateParams(params: ModelParams, attachments: List<Uri>) {
        when (params) {
            is MiniMaxImageParams -> {
                if (attachments.size > 1) error("MiniMax image-to-image supports up to 1 reference image")
                validateMiniMaxImageParams(params)
            }
            is QwenImageParams -> {
                if (attachments.size > 3) error("Qwen image-to-image supports up to 3 reference images")
                if (params.n !in 1..6) error("Qwen image count must be between 1 and 6")
            }
            is MiniMaxVideoParams -> {
                if (attachments.size > 1) error("MiniMax image-to-video supports only 1 first-frame image")
                require(params.duration in 4..15) { "MiniMax video duration must be between 4 and 15 seconds" }
            }
            is QwenVideoParams -> {
                if (attachments.size > 1) error("Qwen image-to-video supports only 1 first-frame image")
                require(params.duration in 2..30) { "Qwen video duration must be between 2 and 30 seconds" }
            }
        }
    }

    private fun validateMiniMaxImageParams(params: MiniMaxImageParams) {
        if (params.n !in 1..9) error("Image count must be between 1 and 9")
        val width = params.width
        val height = params.height
        if (width != null || height != null) {
            require(width != null && height != null) { "Enter both custom width and height" }
            require(width in 512..2048 && height in 512..2048) { "Custom width and height must be between 512 and 2048" }
            require(width % 8 == 0 && height % 8 == 0) { "Custom width and height must be multiples of 8" }
            require(params.model == "image-01") { "Custom dimensions are only available for image-01" }
        }
    }

    private fun resolveMode(topMode: TopMode, model: String, hasImage: Boolean): GenerationType {
        return when {
            ModelCatalog.isVideoEdit(model) -> GenerationType.VIDEO_EDIT
            topMode == TopMode.IMAGE -> if (hasImage) GenerationType.IMAGE_TO_IMAGE else GenerationType.TEXT_TO_IMAGE
            else -> if (hasImage) GenerationType.IMAGE_TO_VIDEO else GenerationType.TEXT_TO_VIDEO
        }
    }

    private fun generationTypeFor(record: GenerationRecord): GenerationType {
        return when (record.subMode) {
            SubMode.VIDEO_EDIT -> GenerationType.VIDEO_EDIT
            SubMode.IMAGE_TO_IMAGE -> GenerationType.IMAGE_TO_IMAGE
            SubMode.IMAGE_TO_VIDEO -> GenerationType.IMAGE_TO_VIDEO
            SubMode.TEXT_TO_VIDEO -> GenerationType.TEXT_TO_VIDEO
            SubMode.TEXT_TO_IMAGE -> GenerationType.TEXT_TO_IMAGE
        }
    }

    private fun decodeParams(raw: String?, model: String, topMode: TopMode): ModelParams {
        return ModelCatalog.decodeParams(json, raw ?: "", model, topMode)
    }

    private fun availableModels(topMode: TopMode): List<String> {
        return if (topMode == TopMode.IMAGE) ModelCatalog.IMAGE_MODELS else ModelCatalog.VIDEO_MODELS
    }

    private fun maxImageBytes(params: ModelParams): Long {
        return when (params) {
            is QwenVideoParams -> 20L * 1024L * 1024L
            is QwenImageParams -> 10L * 1024L * 1024L
            is MiniMaxVideoParams -> 30L * 1024L * 1024L
            is MiniMaxImageParams -> 10L * 1024L * 1024L
        }
    }

    private fun ChatMessage.toRecord(): GenerationRecord {
        return GenerationRecord(
            id = id,
            type = type,
            prompt = prompt,
            paramsJson = paramsJson,
            resultUrlsJson = json.encodeToString(ListSerializer(String.serializer()), resultUrls),
            status = status,
            errorMessage = errorMessage,
            createdAt = createdAt,
            completedAt = completedAt,
            thumbnailUrl = thumbnailUrl,
            attachmentsJson = json.encodeToString(ListSerializer(String.serializer()), attachmentUris),
            taskId = taskId,
            platform = platform,
            model = model,
            topMode = topMode,
            subMode = subMode
        )
    }

    private fun friendlyError(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401, 2049 -> "Invalid API Key. Check Settings."
                402, 1008 -> "Insufficient account balance. Check your account."
                429, 1002 -> "Too many requests. Please try again later."
                500, 503 -> "Server is busy. Please try again later."
                else -> "Request failed (${error.code()})"
            }
            is UnknownHostException -> "Network connection failed. Check your internet connection."
            is SocketTimeoutException -> "Network connection timed out. Please try again later."
            else -> error.message ?: "Generation failed"
        }
    }
}
