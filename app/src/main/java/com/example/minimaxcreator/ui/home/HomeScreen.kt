package com.example.minimaxcreator.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.minimaxcreator.data.local.db.ChatMessage
import com.example.minimaxcreator.data.local.db.GenerationType
import com.example.minimaxcreator.data.local.db.TopMode
import com.example.minimaxcreator.data.model.ModelCatalog
import com.example.minimaxcreator.ui.components.GlassCard
import com.example.minimaxcreator.ui.components.GlassHeader
import com.example.minimaxcreator.ui.components.MessageCard
import com.example.minimaxcreator.ui.components.ParameterBottomSheet
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var greeting by remember { mutableStateOf(timeGreeting()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showParameters by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var fullscreenUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var fullscreenIndex by remember { mutableStateOf(0) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        uris.forEach(viewModel::addAttachment)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let(viewModel::addAttachment)
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Unable to use camera", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            greeting = timeGreeting()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = modeTitle(state.mode),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { showParameters = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Generation settings")
                    }
                }
            }
            ModeSelector(
                mode = state.topMode,
                onModeChange = viewModel::setTopMode
            )
            AnimatedContent(
                targetState = state.topMode,
                transitionSpec = {
                    (
                        slideInHorizontally(
                            animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f),
                            initialOffsetX = { width -> if (targetState == TopMode.VIDEO) width else -width }
                        ) + fadeIn(animationSpec = tween(300))
                    ) togetherWith (
                        slideOutHorizontally(
                            animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f),
                            targetOffsetX = { width -> if (initialState == TopMode.VIDEO) width else -width }
                        ) + fadeOut(animationSpec = tween(300))
                    )
                },
                label = "mode-switch"
            ) { animatedTopMode ->
                val animatedMode = if (animatedTopMode == TopMode.VIDEO) {
                    if (state.mode.isVideo()) state.mode else GenerationType.TEXT_TO_VIDEO
                } else {
                    if (!state.mode.isVideo()) state.mode else GenerationType.TEXT_TO_IMAGE
                }
                ModeIndicatorRow(
                    mode = animatedMode,
                    selectedModel = state.selectedModel,
                    onModelChange = viewModel::setModel
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (state.messages.isEmpty()) {
                    EmptyState(state.topMode)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 4.dp,
                            vertical = 8.dp
                        )
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(360, easing = EaseOutCubic)
                                    ) + fadeIn(animationSpec = tween(360, easing = EaseOutCubic)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MessageCard(
                                        message = message,
                                        onRetry = viewModel::retry,
                                        onDownload = viewModel::saveRemote,
                                        onShare = viewModel::shareRemote,
                                        onOpenImage = { urls, index ->
                                            fullscreenUrls = urls
                                            fullscreenIndex = index
                                        },
                                        onOpenAttachment = { uri ->
                                            Toast.makeText(context, uri.toString(), Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AttachmentPreview(
                attachments = state.attachments,
                onRemove = viewModel::removeAttachment
            )
            InputBar(
                mode = state.mode,
                attachEnabled = true,
                prompt = state.prompt,
                isGenerating = state.isGenerating,
                onPromptChange = viewModel::updatePrompt,
                onSend = viewModel::generate,
                onAttach = { showAttachmentPicker = true },
                onOpenParameters = { showParameters = true }
            )
        }
    }

    ParameterBottomSheet(
        visible = showParameters,
        currentParams = state.selectedParams,
        hasImageAttached = state.attachments.isNotEmpty(),
        onDismiss = { showParameters = false },
        onConfirm = viewModel::setParams
    )

    if (showAttachmentPicker) {
        ModalBottomSheet(onDismissRequest = { showAttachmentPicker = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add reference images", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentPicker = false
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentPicker = false
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take photo")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (fullscreenUrls.isNotEmpty()) {
        FullscreenImageDialog(
            urls = fullscreenUrls,
            initialIndex = fullscreenIndex,
            onDismiss = { fullscreenUrls = emptyList() }
        )
    }
}

@Composable
private fun ModeSelector(
    mode: TopMode,
    onModeChange: (TopMode) -> Unit
) {
    val modes = listOf(
        TopMode.IMAGE to "Image generation",
        TopMode.VIDEO to "Video generation"
    )
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentPadding = 8
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (itemMode, label) ->
                SegmentedButton(
                    selected = mode == itemMode,
                    onClick = { onModeChange(itemMode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    label = { Text(label, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
private fun ModeIndicatorRow(
    mode: GenerationType,
    selectedModel: String,
    onModelChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentPadding = 8
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {},
                label = { Text(modeTitle(mode), fontSize = 12.sp) }
            )
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(selectedModel, fontSize = 12.sp)
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select model")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    val models = if (mode.isVideo()) ModelCatalog.VIDEO_MODELS else ModelCatalog.IMAGE_MODELS
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                expanded = false
                                onModelChange(model)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(mode: TopMode) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (
                fadeIn(animationSpec = tween(280, easing = EaseOutCubic)) +
                    slideInVertically(
                        initialOffsetY = { height -> height / 6 },
                        animationSpec = tween(280, easing = EaseOutCubic)
                    )
            ) togetherWith (
                fadeOut(animationSpec = tween(200, easing = EaseOutCubic)) +
                    slideOutVertically(
                        targetOffsetY = { height -> -height / 8 },
                        animationSpec = tween(200, easing = EaseOutCubic)
                    )
            )
        },
        label = "empty-state"
    ) { animatedMode ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(
                        if (animatedMode == TopMode.VIDEO) {
                            com.example.minimaxcreator.R.drawable.gen_2
                        } else {
                            com.example.minimaxcreator.R.drawable.gen_1
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .width(180.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (animatedMode == TopMode.VIDEO) "Create an amazing video" else "Describe the image you want",
                    color = Color(0xFFDCE6F3)
                )
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    attachments: List<Uri>,
    onRemove: (Uri) -> Unit
) {
    if (attachments.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments, key = { it.toString() }) { uri ->
            Box {
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected reference image",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemove(uri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove image",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    mode: GenerationType,
    attachEnabled: Boolean,
    prompt: String,
    isGenerating: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onOpenParameters: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentPadding = 8
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (mode.topMode() == TopMode.IMAGE || mode.topMode() == TopMode.VIDEO) {
                IconButton(
                    onClick = onAttach,
                    modifier = Modifier.alpha(if (attachEnabled) 1f else 0.38f)
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Add image")
                }
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = { Text(if (mode.requiresAttachment()) "Upload an image and describe the changes…" else "Describe the image you want…") },
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 5,
                shape = RoundedCornerShape(18.dp)
            )
            IconButton(onClick = onOpenParameters) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
            FilledIconButton(
                onClick = onSend,
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Outlined.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun FullscreenImageDialog(
    urls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(urls) { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(url).build(),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

private fun modeTitle(mode: GenerationType): String {
    return when (mode) {
        GenerationType.TEXT_TO_IMAGE -> "Text to Image"
        GenerationType.IMAGE_TO_IMAGE -> "Image to Image"
        GenerationType.TEXT_TO_VIDEO -> "Text to Video"
        GenerationType.IMAGE_TO_VIDEO -> "Image to Video"
        GenerationType.VIDEO_EDIT -> "Video Editing"
    }
}

private fun GenerationType.topMode(): TopMode {
    return if (isVideo()) TopMode.VIDEO else TopMode.IMAGE
}

private fun timeGreeting(): String {
    return when (LocalTime.now().hour) {
        in 5..10 -> "Good morning"
        in 11..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
