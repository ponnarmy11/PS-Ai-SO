package com.example.minimaxcreator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minimaxcreator.ui.components.GlassCard
import com.example.minimaxcreator.data.model.ModelCatalog
import com.example.minimaxcreator.ui.components.GlassHeader

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var miniMaxKey by remember { mutableStateOf(state.settings.miniMaxApiKey) }
    var qwenKey by remember { mutableStateOf(state.settings.qwenApiKey) }
    var falKey by remember { mutableStateOf(state.settings.falApiKey) }
    var falSaveStatus by remember { mutableStateOf<String?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.settings.miniMaxApiKey, state.settings.qwenApiKey, state.settings.falApiKey) {
        miniMaxKey = state.settings.miniMaxApiKey
        qwenKey = state.settings.qwenApiKey
        falKey = state.settings.falApiKey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        GlassHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 8.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(20.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("API Key Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            KeyField(
                value = miniMaxKey,
                onValueChange = { miniMaxKey = it },
                label = "MiniMax API Key"
            )
            KeyField(
                value = qwenKey,
                onValueChange = { qwenKey = it },
                label = "Qwen (DashScope) API Key"
            )
            KeyField(
                value = falKey,
                onValueChange = { falKey = it; falSaveStatus = null },
                label = "fal.ai API Key"
            )
            Button(
                onClick = {
                    falSaveStatus = "Saving fal.ai API key..."
                    viewModel.saveFalApiKey(falKey) { saved ->
                        falSaveStatus = if (saved) "fal.ai API key saved securely." else "Could not save fal.ai API key. Please try again."
                    }
                },
                enabled = falKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save fal.ai API Key")
            }
            falSaveStatus?.let { status ->
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveKeys(miniMaxKey, qwenKey) },
                enabled = miniMaxKey.isNotBlank() || qwenKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save API Keys")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("Model Configuration", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ModelDropdown(
                label = "Default Image Model",
                options = ModelCatalog.IMAGE_MODELS,
                selected = state.settings.defaultImageModel,
                onSelect = viewModel::setDefaultImageModel
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModelDropdown(
                label = "Default Video Model",
                options = ModelCatalog.VIDEO_MODELS,
                selected = state.settings.defaultVideoModel,
                onSelect = viewModel::setDefaultVideoModel
            )
            Spacer(modifier = Modifier.height(16.dp))
            SettingSwitchRow(
                title = "Automatically save generated media to Gallery",
                checked = state.settings.autoSave,
                onCheckedChange = viewModel::setAutoSave
            )
            SettingSwitchRow(
                title = "Use system dynamic colors",
                checked = state.settings.dynamicColors,
                onCheckedChange = viewModel::setDynamicColors
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear History")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showCacheDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Cache")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Studio 2.2.2")
            Text("Only your prompts and selected media are sent to the selected AI provider.")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Clear all history?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showHistoryDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("Clear cache?") },
            text = { Text("Temporary downloaded files will be deleted. History will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        visualTransformation = if (visible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide" else "Show"
                )
            }
        }
    )
}

@Composable
private fun ModelDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(selected)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
