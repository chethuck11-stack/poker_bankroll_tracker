package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldProfit

@Composable
fun SpadeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    spadeColor: Color = EmeraldProfit,
    backgroundColor: Color = Color(0xFF07271E),
    borderColor: Color = Color(0xFF0F5132)
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height

            // Vector Spade path
            val path = Path().apply {
                // Top sharp tip
                moveTo(w * 0.5f, 0f)
                // Left curve
                cubicTo(
                    w * 0.4f, h * 0.25f,
                    0f, h * 0.35f,
                    0f, h * 0.6f
                )
                cubicTo(
                    0f, h * 0.85f,
                    w * 0.3f, h * 0.88f,
                    w * 0.45f, h * 0.75f
                )
                // Stem left
                lineTo(w * 0.43f, h)
                // Stem base
                lineTo(w * 0.57f, h)
                // Stem right
                lineTo(w * 0.55f, h * 0.75f)
                // Right curve
                cubicTo(
                    w * 0.7f, h * 0.88f,
                    w, h * 0.85f,
                    w, h * 0.6f
                )
                cubicTo(
                    w, h * 0.35f,
                    w * 0.6f, h * 0.25f,
                    w * 0.5f, 0f
                )
                close()
            }

            drawPath(path = path, color = spadeColor)
        }
    }
}
