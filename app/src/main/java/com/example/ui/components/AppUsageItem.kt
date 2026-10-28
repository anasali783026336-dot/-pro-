package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppUsageInfo
import com.example.data.model.DataUsageStats
import com.example.data.model.FirewallPolicy
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WifiBlue

@Composable
fun AppUsageCard(
    appInfo: AppUsageInfo,
    onAppClick: () -> Unit,
    onPolicyChange: (FirewallPolicy) -> Unit,
    showFirewallControls: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon = remember(appInfo.packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(appInfo.packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .clickable { onAppClick() },
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = appInfo.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appInfo.appName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Name & Package Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appInfo.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Total Usage Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DataUsageStats.formatBytesShort(appInfo.totalBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (appInfo.totalBytes > 0) CyberCyan else TextMuted
                    )
                    if (appInfo.backgroundBytes > 0) {
                        Text(
                            text = "خلفية: ${DataUsageStats.formatBytesShort(appInfo.backgroundBytes)}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wi-Fi and Mobile breakdown row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = WifiBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DataUsageStats.formatBytesShort(appInfo.wifiBytes),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = MobileOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DataUsageStats.formatBytesShort(appInfo.mobileBytes),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Firewall Policy Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (appInfo.policy) {
                                FirewallPolicy.ALLOW_ALL -> CyberEmerald.copy(alpha = 0.15f)
                                FirewallPolicy.BLOCK_ALL -> CyberCrimson.copy(alpha = 0.15f)
                                FirewallPolicy.WIFI_ONLY -> WifiBlue.copy(alpha = 0.15f)
                                FirewallPolicy.CELLULAR_ONLY -> MobileOrange.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = appInfo.policy.titleAr,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (appInfo.policy) {
                            FirewallPolicy.ALLOW_ALL -> CyberEmerald
                            FirewallPolicy.BLOCK_ALL -> CyberCrimson
                            FirewallPolicy.WIFI_ONLY -> WifiBlue
                            FirewallPolicy.CELLULAR_ONLY -> MobileOrange
                        }
                    )
                }
            }

            // Quick policy switches if expanded in firewall tab
            if (showFirewallControls) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FirewallPolicyChip(
                        title = "سماح",
                        isSelected = appInfo.policy == FirewallPolicy.ALLOW_ALL,
                        selectedColor = CyberEmerald,
                        onClick = { onPolicyChange(FirewallPolicy.ALLOW_ALL) }
                    )
                    FirewallPolicyChip(
                        title = "حظر",
                        isSelected = appInfo.policy == FirewallPolicy.BLOCK_ALL,
                        selectedColor = CyberCrimson,
                        onClick = { onPolicyChange(FirewallPolicy.BLOCK_ALL) }
                    )
                    FirewallPolicyChip(
                        title = "Wi-Fi فقط",
                        isSelected = appInfo.policy == FirewallPolicy.WIFI_ONLY,
                        selectedColor = WifiBlue,
                        onClick = { onPolicyChange(FirewallPolicy.WIFI_ONLY) }
                    )
                    FirewallPolicyChip(
                        title = "شريحة فقط",
                        isSelected = appInfo.policy == FirewallPolicy.CELLULAR_ONLY,
                        selectedColor = MobileOrange,
                        onClick = { onPolicyChange(FirewallPolicy.CELLULAR_ONLY) }
                    )
                }
            }
        }
    }
}

@Composable
fun FirewallPolicyChip(
    title: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else CyberSurfaceVariant)
            .border(
                1.dp,
                if (isSelected) selectedColor else CyberBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedColor else TextSecondary
        )
    }
}
