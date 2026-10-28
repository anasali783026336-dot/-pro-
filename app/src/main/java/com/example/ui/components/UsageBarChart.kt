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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DataUsageStats
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WifiBlue

data class DayUsageItem(
    val dayLabelAr: String,
    val wifiBytes: Long,
    val mobileBytes: Long
)

@Composable
fun UsageBarChartCard(
    items: List<DayUsageItem>,
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
            // Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "استهلاك آخر 7 أيام",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Wifi legend
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WifiBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "واي فاي",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Mobile legend
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MobileOrange)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "شريحة",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxBytes = items.maxOfOrNull { it.wifiBytes + it.mobileBytes }?.coerceAtLeast(1024L * 1024L) ?: (1024L * 1024L)

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(125.dp)) {
                    val count = items.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.45f).coerceAtMost(24.dp.toPx())
                    val maxHeight = size.height

                    // Grid guideline lines
                    for (i in 1..3) {
                        val y = maxHeight * (i / 4f)
                        drawLine(
                            color = CyberBorder,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    items.forEachIndexed { index, item ->
                        val centerX = (index * slotWidth) + (slotWidth / 2f)
                        val total = item.wifiBytes + item.mobileBytes
                        val barHeight = ((total.toFloat() / maxBytes.toFloat()) * maxHeight).coerceIn(4f, maxHeight)

                        val mobileFraction = if (total > 0) item.mobileBytes.toFloat() / total.toFloat() else 0f
                        val mobileHeight = barHeight * mobileFraction
                        val wifiHeight = barHeight - mobileHeight

                        val startX = centerX - (barWidth / 2f)
                        val bottomY = maxHeight

                        // Draw Wifi (bottom portion)
                        if (wifiHeight > 0) {
                            drawRoundRect(
                                color = WifiBlue,
                                topLeft = Offset(startX, bottomY - wifiHeight),
                                size = Size(barWidth, wifiHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // Draw Mobile (stacked top portion)
                        if (mobileHeight > 0) {
                            drawRoundRect(
                                color = MobileOrange,
                                topLeft = Offset(startX, bottomY - barHeight),
                                size = Size(barWidth, mobileHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    Text(
                        text = item.dayLabelAr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
