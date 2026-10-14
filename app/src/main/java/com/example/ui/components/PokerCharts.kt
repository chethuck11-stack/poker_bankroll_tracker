package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyHelper
import com.example.data.repository.GroupProfitStat
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.RedLoss
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class ChartPoint(
    val label: String,
    val valuePaise: Long,
    val dateEpochMs: Long = 0L
)

@Composable
fun BankrollGrowthChart(
    points: List<ChartPoint>,
    peakPaise: Long,
    modifier: Modifier = Modifier,
    lineColor: Color = EmeraldProfit
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No bankroll history yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    // Build at least 2 points for a curve if only 1 point exists
    val displayPoints = if (points.size == 1) {
        val single = points.first()
        listOf(
            single.copy(valuePaise = single.valuePaise / 2),
            single
        )
    } else {
        points
    }

    val values = displayPoints.map { it.valuePaise }
    val minValue = values.minOrNull() ?: 0L
    val maxValue = values.maxOrNull() ?: 0L
    val displayMin = (minValue - 10_000L).coerceAtLeast(0L)
    val displayMax = if (maxValue == displayMin) displayMin + 100_000L else maxValue
    val range = (displayMax - displayMin).coerceAtLeast(1L)

    val dateFormat = SimpleDateFormat("MM-dd", Locale.US)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val n = displayPoints.size
            val stepX = if (n > 1) width / (n - 1) else width

            val strokePath = Path()
            val fillPath = Path()
            val pointOffsets = mutableListOf<Offset>()

            displayPoints.forEachIndexed { i, pt ->
                val x = i * stepX
                val normalizedY = (pt.valuePaise - displayMin).toFloat() / range.toFloat()
                val y = height - (normalizedY * (height - 30.dp.toPx()) + 15.dp.toPx())
                val offset = Offset(x, y)
                pointOffsets.add(offset)

                if (i == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Area Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        lineColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Line
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw circular point rings
            pointOffsets.forEach { offset ->
                drawCircle(
                    color = Color(0xFF0F1418),
                    radius = 5.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.5.dp.toPx(),
                    center = offset
                )
            }
        }

        // X-Axis Date labels row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (displayPoints.first().dateEpochMs > 0) dateFormat.format(Date(displayPoints.first().dateEpochMs)) else "Start",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = if (displayPoints.last().dateEpochMs > 0) dateFormat.format(Date(displayPoints.last().dateEpochMs)) else "10-14",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun MonthlyProfitLossChart(
    monthlyStats: List<GroupProfitStat>,
    modifier: Modifier = Modifier
) {
    if (monthlyStats.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(Color(0xFF0F1418), RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No completed tournaments yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    val displayList = monthlyStats.takeLast(6)
    val maxAbs = displayList.maxOfOrNull { abs(it.netProfitPaise) }?.coerceAtLeast(10_000L) ?: 100_000L

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(Color(0xFF0F1418), RoundedCornerShape(12.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height
                val midY = height * 0.7f // baseline near bottom for positive profit
                val count = displayList.size
                val barGroupWidth = width / count
                val barWidth = (barGroupWidth * 0.35f).coerceIn(18.dp.toPx(), 44.dp.toPx())

                displayList.forEachIndexed { i, stat ->
                    val centerX = i * barGroupWidth + (barGroupWidth / 2f)
                    val isPositive = stat.netProfitPaise >= 0
                    val ratio = (abs(stat.netProfitPaise).toFloat() / maxAbs.toFloat()).coerceIn(0.12f, 0.95f)
                    val barHeight = ratio * (midY - 20.dp.toPx())

                    val barColor = if (isPositive) EmeraldProfit else RedLoss
                    val top = if (isPositive) midY - barHeight else midY

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - (barWidth / 2f), top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            // Month Labels and values row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                displayList.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(60.dp)
                    ) {
                        Text(
                            text = CurrencyHelper.formatInrCompact(stat.netProfitPaise, showSign = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stat.netProfitPaise >= 0) EmeraldProfit else RedLoss,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stat.key.take(3), // e.g. "Oct"
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
