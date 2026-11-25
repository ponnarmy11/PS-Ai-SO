package com.example.minimaxcreator.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimaxcreator.data.local.db.GenerationRecord
import com.example.minimaxcreator.data.local.db.GenerationRecordDao
import com.example.minimaxcreator.data.local.db.GenerationStatus
import com.example.minimaxcreator.data.local.db.toModel
import com.example.minimaxcreator.data.repository.GenerationRepository
import com.example.minimaxcreator.data.repository.MediaSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: GenerationRepository,
    private val dao: GenerationRecordDao,
    private val mediaSaver: MediaSaver,
    private val json: Json
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            dao.observeAll().collect { entities ->
                _uiState.update {
                    it.copy(
                        records = entities.map { record -> record.toModel() },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun toggleSelection(id: String) {
        _uiState.update {
            val selected = it.selectedIds.toMutableSet().apply {
                if (!add(id)) remove(id)
            }
            it.copy(selectedIds = selected)
        }
    }

    fun deleteRecord(record: GenerationRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record.id)
            _events.emit("Deleted")
        }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.deleteRecords(selected.toList())
            _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
            _events.emit("Deleted ${selected.size} records")
        }
    }

    fun shareRecord(record: GenerationRecord) {
        val url = resultUrls(record).firstOrNull() ?: return
        viewModelScope.launch {
            runCatching { mediaSaver.shareRemote(url, record.type.name.endsWith("VIDEO")) }
                .onFailure { _events.emit("Share failed: ${it.message ?: "Unknown error"}") }
        }
    }

    fun resultUrls(record: GenerationRecord): List<String> {
        return runCatching { json.decodeFromString<List<String>>(record.resultUrlsJson) }
            .getOrDefault(emptyList())
    }

    fun attachments(record: GenerationRecord): List<String> {
        return runCatching { json.decodeFromString<List<String>>(record.attachmentsJson) }
            .getOrDefault(emptyList())
    }

    fun successRecords(): List<GenerationRecord> {
        return _uiState.value.records.filter { it.status == GenerationStatus.SUCCESS }
    }
}
