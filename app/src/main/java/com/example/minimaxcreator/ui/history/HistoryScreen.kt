package com.example.minimaxcreator.ui.history

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minimaxcreator.data.local.db.GenerationRecord
import com.example.minimaxcreator.data.local.db.GenerationStatus
import com.example.minimaxcreator.ui.components.GlassHeader
import com.example.minimaxcreator.ui.components.VideoPlayerCard
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onRegenerate: (GenerationRecord) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<GenerationRecord?>(null) }
    var deleteDialog by remember { mutableStateOf(false) }
    val filtered = state.records.filter { record ->
        state.query.isBlank() || record.prompt.contains(state.query, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.selectionMode) {
                    TextButton(onClick = viewModel::exitSelectionMode) {
                        Text("Cancel")
                    }
                    Text("${state.selectedIds.size} selected")
                    IconButton(
                        onClick = viewModel::deleteSelected,
                        enabled = state.selectedIds.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                    }
                } else {
                    Text("History", style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = viewModel::enterSelectionMode) {
                        Text("Select")
                    }
                }
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search prompts") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No creations yet. Start creating!",
                    color = Color(0xFF6E6E73)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                items(filtered, key = { it.id }) { record ->
                    HistoryRecordCard(
                        record = record,
                        selected = record.id in state.selectedIds,
                        selectionMode = state.selectionMode,
                        onClick = { viewModel.toggleSelection(record.id) },
                        onOpen = { detail = record },
                        onLongClick = { viewModel.enterSelectionMode(); viewModel.toggleSelection(record.id) },
                        onRegenerate = { onRegenerate(record) },
                        onDelete = {
                            viewModel.deleteRecord(record)
                        },
                        onShare = { viewModel.shareRecord(record) },
                        onSelect = { viewModel.toggleSelection(record.id) }
                    )
                }
            }
        }
    }

    detail?.let { record ->
        RecordDetailDialog(
            record = record,
            resultUrls = viewModel.resultUrls(record),
            attachments = viewModel.attachments(record),
            onDismiss = { detail = null },
            onRegenerate = {
                val current = record
                detail = null
                onRegenerate(current)
            },
            onShare = { viewModel.shareRecord(record) },
            onDelete = {
                viewModel.deleteRecord(record)
                detail = null
            }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("清除History") },
            text = { Text("Delete all history records?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    deleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HistoryRecordCard(
    record: GenerationRecord,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSelect: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val urls = remember(record.id, record.resultUrlsJson) {
        runCatching {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(record.resultUrlsJson)
        }.getOrDefault(emptyList())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { if (selectionMode) onSelect() else onOpen() },
                onLongClick = onLongClick
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = urls.firstOrNull()?.let {
                        ImageRequest.Builder(LocalContext.current).data(it).build()
                    },
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.prompt,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${record.platform.name} · ${record.model} · ${relativeTime(record.createdAt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectionMode) {
                    Icon(
                        imageVector = if (selected) Icons.Outlined.Check else Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                } else {
                    Row {
                        IconButton(onClick = { onShare() }) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Regenerate") },
                onClick = {
                    menuExpanded = false
                    onRegenerate()
                },
                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    menuExpanded = false
                    onShare()
                },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun RecordDetailDialog(
    record: GenerationRecord,
    resultUrls: List<String>,
    attachments: List<String>,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(record.prompt, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                if (record.type.name.endsWith("VIDEO")) {
                    resultUrls.firstOrNull()?.let { VideoPlayerCard(it) }
                } else {
                    resultUrls.forEach { url ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(url).build(),
                            contentDescription = "Generated result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (record.status == GenerationStatus.FAILED) {
                    Text(record.errorMessage ?: "Generation failed", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Button(onClick = onRegenerate) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Regenerate")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = onShare) { Text("Share") }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes} min ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} days ago"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
