package com.example.minimaxcreator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimaxcreator.R
import kotlinx.coroutines.delay

@Composable
fun GeneratingAnimation(
    isVideo: Boolean,
    progressText: String? = null
) {
    Column {
        ThinkingCard()
        if (isVideo) {
            VideoGeneratingAnimation(progressText)
        } else {
            ShimmerCard()
        }
    }
}

@Composable
private fun ThinkingCard() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking-phase"
    )
    var textIndex by remember { mutableIntStateOf(0) }
    val thinkingTexts = listOf(
        "Understanding your prompt",
        "Preparing visual details",
        "Preparing generation"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(3400L)
            textIndex = (textIndex + 1) % thinkingTexts.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF3F8FF).copy(alpha = 0.82f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF0A84FF)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Thinking",
                    color = Color(0xFF1D1D1F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                AnimatedContent(
                    targetState = thinkingTexts[textIndex],
                    transitionSpec = {
                        fadeIn(animationSpec = tween(260, easing = EaseInOutCubic)) togetherWith
                            fadeOut(animationSpec = tween(180, easing = EaseInOutCubic))
                    },
                    label = "thinking-text"
                ) { text ->
                    Text(
                        text = text,
                        color = Color(0xFF6E6E73),
                        fontSize = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            repeat(3) { index ->
                val dotPhase = ((phase * 3f - index * 0.35f) % 1f + 1f) % 1f
                val scale = 0.72f + dotPhase * 0.72f
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(7.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.45f + scale * 0.45f
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF0A84FF))
                )
            }
        }
    }
}

@Composable
private fun ShimmerCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFECECEF).copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFFE0E0E0).copy(alpha = alpha))
        )
    }
}

@Composable
private fun VideoGeneratingAnimation(progressText: String?) {
    val images = listOf(R.drawable.gen_1, R.drawable.gen_2, R.drawable.gen_3)
    var currentIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentIndex = (currentIndex + 1) % images.size
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val currentPainter = painterResource(images[currentIndex])
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            androidx.compose.foundation.Image(
                painter = currentPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxSize()
            )
        }
        AnimatedContent(
            targetState = currentIndex,
            modifier = Modifier.align(Alignment.Center),
            transitionSpec = {
                fadeIn(animationSpec = tween(1200, easing = EaseInOutCubic)) togetherWith
                    fadeOut(animationSpec = tween(1200, easing = EaseInOutCubic))
            },
            label = "video-generating"
        ) { index ->
            androidx.compose.foundation.Image(
                painter = painterResource(images[index]),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = progressText ?: "AI is generating your video…",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
