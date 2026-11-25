package com.example.minimaxcreator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.minimaxcreator.data.model.AgnesVideoParams
import com.example.minimaxcreator.data.model.MiniMaxImageParams
import com.example.minimaxcreator.data.model.MiniMaxVideoParams
import com.example.minimaxcreator.data.model.ModelCatalog
import com.example.minimaxcreator.data.model.ModelParams
import com.example.minimaxcreator.data.model.QwenImageParams
import com.example.minimaxcreator.data.model.QwenVideoParams
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterBottomSheet(
    visible: Boolean,
    currentParams: ModelParams,
    hasImageAttached: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ModelParams) -> Unit
) {
    if (!visible) return
    var params by remember(currentParams) { mutableStateOf(currentParams) }
    var seedText by remember { mutableStateOf(seedTextFor(params)) }
    val current = params

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Generation Settings", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            when (current) {
                is MiniMaxImageParams -> MiniMaxImagePanel(
                    params = current,
                    seedText = seedText,
                    onSeedChange = { seedText = it },
                    onParamsChange = { params = it }
                )
                is QwenImageParams -> QwenImagePanel(
                    params = current,
                    seedText = seedText,
                    onSeedChange = { seedText = it },
                    onParamsChange = { params = it }
                )
                is MiniMaxVideoParams -> MiniMaxVideoPanel(current) { params = it }
                is QwenVideoParams -> QwenVideoPanel(current) { params = it }
                is AgnesVideoParams -> AgnesVideoPanel(current) { params = it }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    params = defaultParams(params)
                    seedText = ""
                }) {
                    Text("Restore Defaults")
                }
                Button(onClick = {
                    onConfirm(copyTextFields(params, seedText))
                    onDismiss()
                }) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MiniMaxImagePanel(
    params: MiniMaxImageParams,
    seedText: String,
    onSeedChange: (String) -> Unit,
    onParamsChange: (MiniMaxImageParams) -> Unit
) {
    SimpleChips(
        title = "Model",
        options = listOf(ModelCatalog.MINIMAX_IMAGE_01, ModelCatalog.MINIMAX_IMAGE_01_LIVE),
        selected = params.model,
        onSelect = { onParamsChange(params.copy(model = it)) }
    )
    SimpleChips(
        title = "Aspect Ratio",
        options = listOf("1:1", "16:9", "4:3", "3:2", "2:3", "3:4", "9:16", "21:9"),
        selected = params.aspectRatio,
        onSelect = { onParamsChange(params.copy(aspectRatio = it, width = null, height = null)) }
    )
    SliderText("Number of images: ${params.n}", params.n.toFloat(), 1f, 9f, 7) {
        onParamsChange(params.copy(n = it.roundToInt().coerceIn(1, 9)))
    }
    OutlinedTextField(
        value = seedText,
        onValueChange = { onSeedChange(it.filter(Char::isDigit)) },
        label = { Text("Seed (leave blank for random)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    SwitchRow("Prompt Optimization", params.promptOptimizer) {
        onParamsChange(params.copy(promptOptimizer = it))
    }
    SwitchRow("AIGC Watermark", params.aigcWatermark) {
        onParamsChange(params.copy(aigcWatermark = it))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = params.width?.toString() ?: "",
            onValueChange = { value -> onParamsChange(params.copy(width = value.toIntOrNull())) },
            label = { Text("Width") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = params.height?.toString() ?: "",
            onValueChange = { value -> onParamsChange(params.copy(height = value.toIntOrNull())) },
            label = { Text("Height") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QwenImagePanel(
    params: QwenImageParams,
    seedText: String,
    onSeedChange: (String) -> Unit,
    onParamsChange: (QwenImageParams) -> Unit
) {
    SimpleChips(
        title = "Size",
        options = listOf("1024*1024", "1280*720", "720*1280", "1536*2688", "2688*1536"),
        selected = params.size,
        onSelect = { onParamsChange(params.copy(size = it)) }
    )
    SliderText("Number of images: ${params.n}", params.n.toFloat(), 1f, 6f, 4) {
        onParamsChange(params.copy(n = it.roundToInt().coerceIn(1, 6)))
    }
    OutlinedTextField(
        value = seedText,
        onValueChange = { onSeedChange(it.filter(Char::isDigit)) },
        label = { Text("Seed") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = params.negativePrompt,
        onValueChange = { onParamsChange(params.copy(negativePrompt = it)) },
        label = { Text("Negative Prompt") },
        modifier = Modifier.fillMaxWidth()
    )
    SimpleChips("Prompt Rewrite Mode", listOf("direct", "agent"), params.promptExtendMode) {
        onParamsChange(params.copy(promptExtendMode = it))
    }
    SwitchRow("Smart Prompt Rewrite", params.promptExtend) {
        onParamsChange(params.copy(promptExtend = it))
    }
    SwitchRow("Thinking Mode", params.enableThinking) {
        onParamsChange(params.copy(enableThinking = it))
    }
    SwitchRow("Watermark", params.watermark) {
        onParamsChange(params.copy(watermark = it))
    }
}

@Composable
private fun MiniMaxVideoPanel(
    params: MiniMaxVideoParams,
    onParamsChange: (MiniMaxVideoParams) -> Unit
) {
    SimpleChips("Resolution", listOf("768P", "2K"), params.resolution) {
        onParamsChange(params.copy(resolution = it))
    }
    SliderText("Duration: ${params.duration} sec", params.duration.toFloat(), 4f, 15f, 10) {
        onParamsChange(params.copy(duration = it.roundToInt().coerceIn(4, 15)))
    }
    SimpleChips("Aspect Ratio", listOf("21:9", "16:9", "4:3", "1:1", "3:4", "9:16"), params.ratio) {
        onParamsChange(params.copy(ratio = it))
    }
    SwitchRow("AIGC Watermark", params.aigcWatermark) {
        onParamsChange(params.copy(aigcWatermark = it))
    }
}

@Composable
private fun QwenVideoPanel(
    params: QwenVideoParams,
    onParamsChange: (QwenVideoParams) -> Unit
) {
    SimpleChips("Resolution", listOf("480P", "720P", "1080P"), params.resolution) {
        onParamsChange(params.copy(resolution = it))
    }
    SimpleChips("Aspect Ratio", listOf("adaptive", "16:9", "9:16", "1:1", "4:3", "3:4"), params.ratio) {
        onParamsChange(params.copy(ratio = it))
    }
    SliderText("Duration: ${params.duration} sec", params.duration.toFloat(), 2f, 30f, 27) {
        onParamsChange(params.copy(duration = it.roundToInt().coerceIn(2, 30)))
    }
    OutlinedTextField(
        value = params.audioUrl,
        onValueChange = { onParamsChange(params.copy(audioUrl = it)) },
        label = { Text("Audio URL (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = params.negativePrompt,
        onValueChange = { onParamsChange(params.copy(negativePrompt = it)) },
        label = { Text("Negative Prompt") },
        modifier = Modifier.fillMaxWidth()
    )
    SwitchRow("Generate Audio", params.audio) {
        onParamsChange(params.copy(audio = it))
    }
    SwitchRow("Smart Prompt Rewrite", params.promptExtend) {
        onParamsChange(params.copy(promptExtend = it))
    }
    SwitchRow("Watermark", params.watermark) {
        onParamsChange(params.copy(watermark = it))
    }
}

@Composable
private fun AgnesVideoPanel(
    params: AgnesVideoParams,
    onParamsChange: (AgnesVideoParams) -> Unit
) {
    Text("Agnes Video 2.5 Flash · 720P")
    SimpleChips("Duration", listOf("4", "5", "6", "8", "10", "12"), params.duration.toString()) {
        onParamsChange(params.copy(duration = it.toInt()))
    }
    SimpleChips("Aspect Ratio", listOf("16:9", "9:16", "1:1", "4:3", "3:4", "21:9"), params.ratio) {
        onParamsChange(params.copy(ratio = it))
    }
}

@Composable
private fun SimpleChips(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) },
                leadingIcon = if (selected == option) {
                    { IconCheck() }
                } else null
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SliderText(
    text: String,
    value: Float,
    min: Float,
    max: Float,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Text(text)
    Slider(value = value, onValueChange = onValueChange, valueRange = min..max, steps = steps)
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IconCheck() {
    androidx.compose.material3.Icon(
        imageVector = Icons.Outlined.Check,
        contentDescription = null
    )
}

private fun defaultParams(params: ModelParams): ModelParams {
    return when (params) {
        is MiniMaxImageParams -> MiniMaxImageParams(model = params.model)
        is QwenImageParams -> QwenImageParams(model = params.model)
        is MiniMaxVideoParams -> MiniMaxVideoParams(model = params.model)
        is QwenVideoParams -> QwenVideoParams(model = params.model)
        is AgnesVideoParams -> AgnesVideoParams(model = params.model)
    }
}

private fun copyTextFields(params: ModelParams, seedText: String): ModelParams {
    return when (params) {
        is MiniMaxImageParams -> params.copy(seed = seedText.toLongOrNull())
        is QwenImageParams -> params.copy(seed = seedText.toIntOrNull())
        is QwenVideoParams -> params.copy(seed = seedText.toIntOrNull())
        is MiniMaxVideoParams -> params
        is AgnesVideoParams -> params
    }
}

private fun seedTextFor(params: ModelParams): String {
    return when (params) {
        is MiniMaxImageParams -> params.seed?.toString() ?: ""
        is QwenImageParams -> params.seed?.toString() ?: ""
        is QwenVideoParams -> params.seed?.toString() ?: ""
        is MiniMaxVideoParams -> ""
        is AgnesVideoParams -> ""
    }
}
