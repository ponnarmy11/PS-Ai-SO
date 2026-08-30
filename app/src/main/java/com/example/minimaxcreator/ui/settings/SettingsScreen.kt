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
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.settings.miniMaxApiKey, state.settings.qwenApiKey) {
        miniMaxKey = state.settings.miniMaxApiKey
        qwenKey = state.settings.qwenApiKey
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
            Text("设置", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(20.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("API Key 管理", style = MaterialTheme.typography.titleMedium)
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
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveKeys(miniMaxKey, qwenKey) },
                enabled = miniMaxKey.isNotBlank() || qwenKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存平台 Key")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("模型配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ModelDropdown(
                label = "默认图片生成模型",
                options = ModelCatalog.IMAGE_MODELS,
                selected = state.settings.defaultImageModel,
                onSelect = viewModel::setDefaultImageModel
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModelDropdown(
                label = "默认视频生成模型",
                options = ModelCatalog.VIDEO_MODELS,
                selected = state.settings.defaultVideoModel,
                onSelect = viewModel::setDefaultVideoModel
            )
            Spacer(modifier = Modifier.height(16.dp))
            SettingSwitchRow(
                title = "生成后自动保存到相册",
                checked = state.settings.autoSave,
                onCheckedChange = viewModel::setAutoSave
            )
            SettingSwitchRow(
                title = "使用系统动态颜色",
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
            Text("数据管理", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除历史记录")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showCacheDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除缓存")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16
        ) {
            Text("关于", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Studio 2.2.2")
            Text("仅将用户输入发送至 MiniMax 与 Qwen API，不收集其他数据。")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("确认清除历史记录？") },
            text = { Text("此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showHistoryDialog = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("取消") }
            }
        )
    }
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("确认清除缓存？") },
            text = { Text("会删除临时下载文件，不影响历史记录。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showCacheDialog = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) { Text("取消") }
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
                    contentDescription = if (visible) "隐藏" else "显示"
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
