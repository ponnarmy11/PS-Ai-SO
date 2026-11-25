package com.example.minimaxcreator.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.minimaxcreator.data.local.db.ChatMessage
import com.example.minimaxcreator.data.local.db.GenerationStatus
import com.example.minimaxcreator.data.local.db.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageCard(
    message: ChatMessage,
    onRetry: (ChatMessage) -> Unit,
    onDownload: (String, Boolean) -> Unit,
    onShare: (String, Boolean) -> Unit,
    onOpenImage: (List<String>, Int) -> Unit,
    onOpenAttachment: (Uri) -> Unit
) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.86f else 0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.prompt.isNotBlank()) {
                    Text(
                        text = message.prompt,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (message.attachmentUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.attachmentUris.forEach { uri ->
                            val parsed = Uri.parse(uri)
                            AsyncImage(
                                model = parsed,
                                contentDescription = "Reference image",
                                modifier = Modifier
                            .width(136.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                                    .clickable { onOpenAttachment(parsed) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    when (message.status) {
                        GenerationStatus.PENDING -> {
                            GeneratingAnimation(
                                isVideo = message.type.name.endsWith("VIDEO"),
                                progressText = message.progressText
                            )
                        }
                        GenerationStatus.SUCCESS -> {
                            if (message.type.name.endsWith("VIDEO")) {
                                message.resultUrls.firstOrNull()?.let { url ->
                                    VideoPlayerCard(url = url)
                                }
                            } else {
                                ImageResultGrid(message.resultUrls, onOpenImage)
                            }
                            MessageActions(
                                isVideo = message.type.name.endsWith("VIDEO"),
                                url = message.resultUrls.firstOrNull(),
                                onDownload = onDownload,
                                onShare = onShare
                            )
                        }
                        GenerationStatus.FAILED -> {
                            Text(
                                text = message.errorMessage ?: "Generation failed",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onRetry(message) }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Retry")
                                }
                                Text("Retry", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.createdAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun ImageResultGrid(
    urls: List<String>,
    onOpenImage: (List<String>, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        urls.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { columnIndex, url ->
                    val index = rowIndex * 2 + columnIndex
                    AsyncImage(
                        model = url,
                        contentDescription = "Generated result $index",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onOpenImage(urls, index) },
                        contentScale = ContentScale.Crop
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MessageActions(
    isVideo: Boolean,
    url: String?,
    onDownload: (String, Boolean) -> Unit,
    onShare: (String, Boolean) -> Unit
) {
    if (url == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        IconButton(
            onClick = { onDownload(url, isVideo) },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Outlined.Download, contentDescription = "Download")
        }
        IconButton(
            onClick = { onShare(url, isVideo) },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Outlined.Share, contentDescription = "Share")
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerCard(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
            volume = 0f
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}
