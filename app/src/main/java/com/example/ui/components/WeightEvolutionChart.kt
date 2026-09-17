package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PesajeUiModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun WeightEvolutionChart(
    pesajes: List<PesajeUiModel>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Evolución de Peso",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${pesajes.size} ${if (pesajes.size == 1) "pesaje registrado" else "pesajes registrados"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (pesajes.size >= 2) {
                    val first = pesajes.first().peso
                    val last = pesajes.last().peso
                    val diff = last - first
                    val sign = if (diff >= 0) "+" else ""
                    Surface(
                        color = if (diff >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$sign${String.format("%.1f", diff)} kg",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (diff >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pesajes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin registros de peso para graficar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (pesajes.size == 1) {
                // Solo un punto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pesajes.first().peso} kg",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Pesaje inicial del ${pesajes.first().fecha}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Agregue más pesajes para visualizar la curva de crecimiento",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                val lineColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        val width = size.width
                        val height = size.height

                        // Margins for labels
                        val paddingLeft = 60f
                        val paddingRight = 40f
                        val paddingTop = 30f
                        val paddingBottom = 40f

                        val chartWidth = width - paddingLeft - paddingRight
                        val chartHeight = height - paddingTop - paddingBottom

                        val minVal = pesajes.minOf { it.peso }
                        val maxVal = pesajes.maxOf { it.peso }

                        // Give some headroom
                        val range = if (maxVal == minVal) 10.0 else (maxVal - minVal)
                        val yMin = max(0.0, minVal - range * 0.15)
                        val yMax = maxVal + range * 0.15

                        // Horizontal gridlines (3 lines)
                        val steps = 3
                        for (i in 0..steps) {
                            val ratio = i.toFloat() / steps
                            val y = paddingTop + chartHeight * (1f - ratio)
                            val valAtStep = yMin + ratio * (yMax - yMin)

                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(width - paddingRight, y),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Y-axis label text
                            drawContext.canvas.nativeCanvas.drawText(
                                "${valAtStep.toInt()}k",
                                paddingLeft - 10f,
                                y + 10f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    isAntiAlias = true
                                }
                            )
                        }

                        // Calculate points
                        val points = pesajes.mapIndexed { index, item ->
                            val x = paddingLeft + (index.toFloat() / (pesajes.size - 1)) * chartWidth
                            val yRatio = ((item.peso - yMin) / (yMax - yMin)).toFloat().coerceIn(0f, 1f)
                            val y = paddingTop + chartHeight * (1f - yRatio)
                            Offset(x, y)
                        }

                        // Build curve path
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val cur = points[i]
                                val cX = (prev.x + cur.x) / 2f
                                cubicTo(cX, prev.y, cX, cur.y, cur.x, cur.y)
                            }
                        }

                        // Build filled area
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, height - paddingBottom)
                            lineTo(points.first().x, height - paddingBottom)
                            close()
                        }

                        // Draw gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.35f),
                                    lineColor.copy(alpha = 0.02f)
                                ),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // Draw line stroke
                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw points and values
                        points.forEachIndexed { i, pt ->
                            val peso = pesajes[i].peso
                            // Outer glow/shadow
                            drawCircle(
                                color = lineColor.copy(alpha = 0.2f),
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                            // Solid center
                            drawCircle(
                                color = lineColor,
                                radius = 4.5f.dp.toPx(),
                                center = pt
                            )
                            // White dot
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = pt
                            )

                            // Draw weight value above point if there are <= 6 points, or for first & last
                            if (pesajes.size <= 5 || i == 0 || i == pesajes.size - 1 || i == pesajes.size / 2) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    "${peso.toInt()}",
                                    pt.x,
                                    pt.y - 16f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.DKGRAY
                                        textSize = 26f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        isAntiAlias = true
                                    }
                                )
                            }
                        }

                        // Draw first and last dates
                        val firstDate = pesajes.first().fecha.substring(max(0, pesajes.first().fecha.length - 5))
                        val lastDate = pesajes.last().fecha.substring(max(0, pesajes.last().fecha.length - 5))

                        drawContext.canvas.nativeCanvas.drawText(
                            firstDate,
                            points.first().x,
                            height - 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )

                        if (pesajes.size > 1) {
                            drawContext.canvas.nativeCanvas.drawText(
                                lastDate,
                                points.last().x,
                                height - 10f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
