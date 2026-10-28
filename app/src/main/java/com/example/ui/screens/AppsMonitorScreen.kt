package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppUsageInfo
import com.example.data.model.DataUsageStats
import com.example.data.model.FirewallPolicy
import com.example.ui.components.AppUsageCard
import com.example.ui.components.FirewallPolicyChip
import com.example.ui.theme.CyberBackground
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
import com.example.ui.viewmodel.AppSortOption
import com.example.ui.viewmodel.AppTimeRange
import com.example.ui.viewmodel.AppsViewModel

@Composable
fun AppsMonitorScreen(
    viewModel: AppsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "مراقبة التطبيقات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "تفاصيل استهلاك البيانات لكل تطبيق مثبت على الجهاز",
                style = MaterialTheme.typography.bodySmall,
                color = CyberCyan
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم التطبيق أو اسم الحزمة...", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = CyberCyan
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "مسح", tint = TextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        // Time Range (Today vs This Month) and Sort chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.timeRange == AppTimeRange.TODAY) CyberCyan.copy(alpha = 0.2f) else CyberSurface)
                        .border(1.dp, if (state.timeRange == AppTimeRange.TODAY) CyberCyan else CyberBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.setTimeRange(AppTimeRange.TODAY) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "استهلاك اليوم",
                        fontSize = 12.sp,
                        fontWeight = if (state.timeRange == AppTimeRange.TODAY) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.timeRange == AppTimeRange.TODAY) CyberCyan else TextSecondary
                    )
                }

                // Month button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.timeRange == AppTimeRange.THIS_MONTH) CyberCyan.copy(alpha = 0.2f) else CyberSurface)
                        .border(1.dp, if (state.timeRange == AppTimeRange.THIS_MONTH) CyberCyan else CyberBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.setTimeRange(AppTimeRange.THIS_MONTH) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "استهلاك الشهر",
                        fontSize = 12.sp,
                        fontWeight = if (state.timeRange == AppTimeRange.THIS_MONTH) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.timeRange == AppTimeRange.THIS_MONTH) CyberCyan else TextSecondary
                    )
                }
            }
        }

        // Sort Options Horizontal Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(AppSortOption.entries) { option ->
                    val isSelected = option == state.sortOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberEmerald.copy(alpha = 0.2f) else CyberSurface)
                            .border(1.dp, if (isSelected) CyberEmerald else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setSortOption(option) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.labelAr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyberEmerald else TextSecondary
                        )
                    }
                }
            }
        }

        // Apps Count Label
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "عدد التطبيقات: ${state.filteredApps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "انقر على أي تطبيق لعرض التفاصيل وتحديد سياسة الجدار",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberCyan)
                }
            }
        } else if (state.filteredApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد تطبيقات مطابقة لمعايير البحث",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(state.filteredApps, key = { it.packageName }) { appInfo ->
                AppUsageCard(
                    appInfo = appInfo,
                    onAppClick = { viewModel.selectApp(appInfo) },
                    onPolicyChange = { policy ->
                        viewModel.updatePolicy(appInfo.packageName, appInfo.appName, policy)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // App Detail Dialog
    state.selectedApp?.let { appInfo ->
        AppDetailDialog(
            appInfo = appInfo,
            onDismiss = { viewModel.selectApp(null) },
            onPolicyChange = { policy ->
                viewModel.updatePolicy(appInfo.packageName, appInfo.appName, policy)
                viewModel.selectApp(appInfo.copy(policy = policy))
            }
        )
    }
}

@Composable
fun AppDetailDialog(
    appInfo: AppUsageInfo,
    onDismiss: () -> Unit,
    onPolicyChange: (FirewallPolicy) -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(appInfo.packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(appInfo.packageName)
            drawable.toBitmap(width = 120, height = 120).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorderGlow, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }

                // App Icon
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = appInfo.appName,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Total consumption box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceVariant)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "إجمالي الاستهلاك المسجل", fontSize = 12.sp, color = TextMuted)
                            Text(
                                text = DataUsageStats.formatBytesShort(appInfo.totalBytes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "استهلاك Wi-Fi", fontSize = 12.sp, color = TextMuted)
                            Text(
                                text = DataUsageStats.formatBytesShort(appInfo.wifiBytes),
                                fontSize = 12.sp,
                                color = WifiBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "استهلاك الشريحة", fontSize = 12.sp, color = TextMuted)
                            Text(
                                text = DataUsageStats.formatBytesShort(appInfo.mobileBytes),
                                fontSize = 12.sp,
                                color = MobileOrange
                            )
                        }

                        if (appInfo.backgroundBytes > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "نشاط الخلفية (Background)", fontSize = 12.sp, color = TextMuted)
                                Text(
                                    text = DataUsageStats.formatBytesShort(appInfo.backgroundBytes),
                                    fontSize = 12.sp,
                                    color = CyberEmerald
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Firewall Policy Selector
                Text(
                    text = "سياسة جدار الحماية لهذا التطبيق",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FirewallPolicyChip(
                            title = "سماح",
                            isSelected = appInfo.policy == FirewallPolicy.ALLOW_ALL,
                            selectedColor = CyberEmerald,
                            onClick = { onPolicyChange(FirewallPolicy.ALLOW_ALL) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FirewallPolicyChip(
                            title = "حظر",
                            isSelected = appInfo.policy == FirewallPolicy.BLOCK_ALL,
                            selectedColor = CyberCrimson,
                            onClick = { onPolicyChange(FirewallPolicy.BLOCK_ALL) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FirewallPolicyChip(
                            title = "Wi-Fi فقط",
                            isSelected = appInfo.policy == FirewallPolicy.WIFI_ONLY,
                            selectedColor = WifiBlue,
                            onClick = { onPolicyChange(FirewallPolicy.WIFI_ONLY) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FirewallPolicyChip(
                            title = "شريحة فقط",
                            isSelected = appInfo.policy == FirewallPolicy.CELLULAR_ONLY,
                            selectedColor = MobileOrange,
                            onClick = { onPolicyChange(FirewallPolicy.CELLULAR_ONLY) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Open App System Info
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", appInfo.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "فتح إعدادات التطبيق في النظام", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}
