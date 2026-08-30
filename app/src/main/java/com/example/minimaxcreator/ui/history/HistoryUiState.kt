package com.example.minimaxcreator.ui.history

import com.example.minimaxcreator.data.local.db.GenerationRecord

data class HistoryUiState(
    val records: List<GenerationRecord> = emptyList(),
    val query: String = "",
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)
