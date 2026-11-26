package com.example.minimaxcreator.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimaxcreator.data.local.db.AppSettings
import com.example.minimaxcreator.data.repository.GenerationRepository
import com.example.minimaxcreator.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val generationRepository: GenerationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { SettingsUiState(settings = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun saveApiKey(value: String) {
        viewModelScope.launch {
            settingsRepository.setApiKey(value)
            _events.emit("API Key saved")
        }
    }

    fun saveKeys(miniMaxKey: String, qwenKey: String) {
        viewModelScope.launch {
            settingsRepository.setMiniMaxApiKey(miniMaxKey)
            settingsRepository.setQwenApiKey(qwenKey)
            _events.emit("API Keys saved")
        }
    }

    fun saveKeys(miniMaxKey: String, qwenKey: String, agnesKey: String) {
        viewModelScope.launch {
            settingsRepository.setMiniMaxApiKey(miniMaxKey)
            settingsRepository.setQwenApiKey(qwenKey)
            settingsRepository.setAgnesApiKey(agnesKey)
            _events.emit("API Keys saved")
        }
    }

    fun saveFalApiKey(value: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { settingsRepository.setFalApiKey(value) }
            result.onSuccess {
                _events.emit("fal.ai API Key saved")
                onResult(true)
            }.onFailure {
                _events.emit("fal.ai API Key save failed")
                onResult(false)
            }
        }
    }

    fun saveAgnesApiKey(value: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { settingsRepository.setAgnesApiKey(value) }
            result.onSuccess {
                _events.emit("Agnes API Key saved")
                onResult(true)
            }.onFailure {
                _events.emit("Agnes API Key save failed")
                onResult(false)
            }
        }
    }

    fun saveMiniMaxApiKey(value: String) {
        viewModelScope.launch {
            settingsRepository.setMiniMaxApiKey(value)
            _events.emit("MiniMax API Key saved")
        }
    }

    fun saveQwenApiKey(value: String) {
        viewModelScope.launch {
            settingsRepository.setQwenApiKey(value)
            _events.emit("Qwen API Key saved")
        }
    }

    fun setDefaultImageModel(model: String) {
        viewModelScope.launch { settingsRepository.setDefaultImageModel(model) }
    }

    fun setDefaultVideoModel(model: String) {
        viewModelScope.launch { settingsRepository.setDefaultVideoModel(model) }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSave(enabled)
            _events.emit(if (enabled) "Auto-save enabled" else "Auto-save disabled")
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColors(enabled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            generationRepository.clearRecords()
            _events.emit("History cleared")
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            _events.emit("Cache cleared")
        }
    }
}
