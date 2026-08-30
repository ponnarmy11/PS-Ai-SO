package com.example.minimaxcreator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .background(
                color = Color(0xD9FFFFFF),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color(0x66FFFFFF),
                shape = shape
            )
            .padding(contentPadding.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .background(
                color = Color(0xE6FFFFFF),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color(0x66FFFFFF),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}
