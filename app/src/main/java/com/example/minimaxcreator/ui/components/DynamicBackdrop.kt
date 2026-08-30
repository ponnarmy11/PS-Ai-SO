package com.example.minimaxcreator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

private class FloatingDot(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
fun DynamicBackdrop(modifier: Modifier = Modifier) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val dots = remember { mutableStateListOf<FloatingDot>() }

    LaunchedEffect(canvasSize) {
        dots.clear()
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
        val count = ((canvasSize.width * canvasSize.height) / 9000).coerceIn(72, 150)
        repeat(count) {
            dots += FloatingDot(
                x = (0..canvasSize.width).random().toFloat(),
                y = (0..canvasSize.height).random().toFloat(),
                vx = (Math.random() - 0.5).toFloat() * 0.14f,
                vy = (Math.random() - 0.5).toFloat() * 0.14f,
                radius = 0.8f + Math.random().toFloat() * 1.8f,
                alpha = 0.16f + Math.random().toFloat() * 0.38f
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                val width = canvasSize.width.toFloat()
                val height = canvasSize.height.toFloat()
                dots.forEach { dot ->
                    dot.x += dot.vx
                    dot.y += dot.vy
                    if (dot.x < -8f) dot.x = width + 8f
                    if (dot.x > width + 8f) dot.x = -8f
                    if (dot.y < -8f) dot.y = height + 8f
                    if (dot.y > height + 8f) dot.y = -8f
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDCEAFF),
                        Color(0xFFEDF5FF),
                        Color(0xFFF8FBFF)
                    )
                )
            )
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            dots.forEach { dot ->
                drawCircle(
                    color = Color(0xFF6C96FF).copy(alpha = dot.alpha),
                    radius = dot.radius,
                    center = Offset(dot.x, dot.y)
                )
            }
        }
    }
}
