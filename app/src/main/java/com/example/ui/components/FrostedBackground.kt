package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MeshBgBase

@Composable
fun FrostedBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshBgBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left radial gradient (hsla(210, 100%, 90%, 1))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCCE5FF), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(0f, 0f)
            )

            // Top-right radial gradient (hsla(220, 100%, 95%, 1))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE5EDFF), Color.Transparent),
                    center = Offset(width, 0f),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(width, 0f)
            )

            // Bottom-right radial gradient (hsla(200, 100%, 92%, 1))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD6F0FF), Color.Transparent),
                    center = Offset(width, height),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(width, height)
            )

            // Bottom-left radial gradient (hsla(215, 100%, 96%, 1))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEAF1FF), Color.Transparent),
                    center = Offset(0f, height),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(0f, height)
            )
        }
        content()
    }
}
