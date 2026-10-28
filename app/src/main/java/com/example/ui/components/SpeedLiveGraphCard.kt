package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkType
import com.example.service.SpeedTelemetry
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SpeedLiveGraphCard(
    telemetry: SpeedTelemetry,
    activeNetwork: NetworkType,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مراقبة سرعة الشبكة الحية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "الشبكة النشطة: ${activeNetwork.titleAr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Session Total Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "بيانات الجلسة",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = telemetry.formattedSessionTotal(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speeds Numbers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Download
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "سرعة التنزيل", fontSize = 10.sp, color = TextMuted)
                            Text(text = telemetry.formattedDownload(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    }
                }

                // Upload
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "سرعة الرفع", fontSize = 10.sp, color = TextMuted)
                            Text(text = telemetry.formattedUpload(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time Canvas Graph
            val downHistory = telemetry.recentDownHistory
            val upHistory = telemetry.recentUpHistory

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(104.dp)) {
                    val maxVal = maxOf(
                        downHistory.maxOrNull() ?: 1024L,
                        upHistory.maxOrNull() ?: 1024L,
                        10240L // Minimum scale 10 KB/s
                    ).toFloat()

                    val count = maxOf(downHistory.size, upHistory.size).coerceAtLeast(2)
                    val stepX = size.width / (count - 1).coerceAtLeast(1)

                    // Draw Download Path (Cyan)
                    if (downHistory.size > 1) {
                        val path = Path()
                        downHistory.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = size.height - ((value.toFloat() / maxVal) * size.height).coerceIn(2f, size.height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = CyberCyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }

                    // Draw Upload Path (Emerald)
                    if (upHistory.size > 1) {
                        val path = Path()
                        upHistory.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = size.height - ((value.toFloat() / maxVal) * size.height).coerceIn(2f, size.height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = CyberEmerald, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Technical note
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الرسم يوضح معدل النقل الفعلي في الثانية خلال آخر 30 ثانية وليس السرعة النظرية.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
