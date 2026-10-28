package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppUsageCard
import com.example.ui.components.FirewallQuickStatusCard
import com.example.ui.components.QuotaDonutCard
import com.example.ui.components.SpeedAndStatusCard
import com.example.ui.components.SummaryStatCard
import com.example.ui.components.UsageBarChartCard
import com.example.ui.components.UsagePermissionWarningCard
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TotalPurple
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onToggleFirewall: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header with App Title and Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "حارس البيانات PRO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "راقب استهلاكك، تحكّم في اتصالك، واحمِ باقتك",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyan
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = CyberCyan
                    )
                }
            }
        }

        // Permission warning if not granted
        if (!state.hasPermission) {
            item {
                UsagePermissionWarningCard()
            }
        }

        // Live Speed and Connection Status with Real-time Graph
        item {
            com.example.ui.components.SpeedLiveGraphCard(
                telemetry = state.speedTelemetry,
                activeNetwork = state.activeNetwork
            )
        }

        // Firewall Status Quick Card
        item {
            FirewallQuickStatusCard(
                isRunning = state.isFirewallRunning,
                blockedCount = state.blockedAppsCount,
                onToggle = onToggleFirewall
            )
        }

        // Quota Donut Gauge
        item {
            QuotaDonutCard(
                usedBytes = state.monthUsage.totalBytes,
                quotaBytes = state.quotaConfig.quotaBytes,
                cycleRenewalDay = state.quotaConfig.renewalDayOfMonth
            )
        }

        // Today's Usage Stat Card
        item {
            SummaryStatCard(
                title = "استهلاك اليوم",
                stats = state.todayUsage,
                icon = Icons.Default.Today,
                accentColor = CyberCyan
            )
        }

        // Yesterday and Current Month Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SummaryStatCard(
                        title = "استهلاك أمس",
                        stats = state.yesterdayUsage,
                        icon = Icons.Default.History,
                        accentColor = MobileOrange
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SummaryStatCard(
                        title = "الشهر الحالي",
                        stats = state.monthUsage,
                        icon = Icons.Default.CalendarMonth,
                        accentColor = TotalPurple
                    )
                }
            }
        }

        // 7-Days Chart
        if (state.last7DaysDailyItems.isNotEmpty()) {
            item {
                UsageBarChartCard(items = state.last7DaysDailyItems)
            }
        }

        // Top 5 apps header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أكثر 5 تطبيقات استهلاكاً اليوم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                OutlinedButton(
                    onClick = onNavigateToApps,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
                ) {
                    Text(text = "عرض الكل", fontSize = 12.sp)
                }
            }
        }

        // Top 5 apps list
        if (state.top5Apps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.isLoading) "جاري جمع إحصائيات النظام..." else "لا توجد بيانات استهلاك مسجلة لليوم حتى الآن",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(state.top5Apps) { appInfo ->
                AppUsageCard(
                    appInfo = appInfo,
                    onAppClick = onNavigateToApps,
                    onPolicyChange = {}
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
