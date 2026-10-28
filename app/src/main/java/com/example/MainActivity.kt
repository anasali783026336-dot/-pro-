package com.example

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.DataGuardianVpnService
import com.example.ui.screens.AppsMonitorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FirewallScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartAnalyticsScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.DataGuardianTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AppsViewModel
import com.example.ui.viewmodel.FirewallViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.MainViewModel

data class NavItem(
    val titleAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DataGuardianTheme(darkTheme = true) {
                // Ensure complete native Arabic RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppContent()
                }
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val app = context.applicationContext as DataGuardianApplication

    val mainViewModel: MainViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val appsViewModel: AppsViewModel = viewModel()
    val firewallViewModel: FirewallViewModel = viewModel()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Back button pops back to Dashboard if in another tab
    if (selectedTabIndex != 0) {
        BackHandler {
            selectedTabIndex = 0
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            DataGuardianVpnService.startService(context)
        }
    }

    val handleToggleFirewall: (Boolean) -> Unit = { enable ->
        if (enable) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnLauncher.launch(prepareIntent)
            } else {
                DataGuardianVpnService.startService(context)
            }
        } else {
            DataGuardianVpnService.stopService(context)
        }
    }

    val navItems = listOf(
        NavItem("الرئيسية", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavItem("السجل", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem("التحليل", Icons.Filled.Insights, Icons.Outlined.Insights),
        NavItem("التطبيقات", Icons.Filled.Apps, Icons.Outlined.Apps),
        NavItem("جدار الحماية", Icons.Filled.Security, Icons.Outlined.Security),
        NavItem("الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                contentColor = TextPrimary
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTabIndex = index
                            if (index == 0) mainViewModel.refreshData()
                            if (index == 2) analyticsViewModel.refreshAnalytics()
                            if (index == 3) appsViewModel.loadApps()
                            if (index == 4) firewallViewModel.loadAppList()
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.titleAr
                            )
                        },
                        label = {
                            Text(text = item.titleAr, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTabIndex, label = "screen_crossfade") { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        viewModel = mainViewModel,
                        onNavigateToApps = { selectedTabIndex = 3 },
                        onNavigateToFirewall = { selectedTabIndex = 4 },
                        onToggleFirewall = handleToggleFirewall
                    )
                    1 -> HistoryScreen(
                        viewModel = historyViewModel
                    )
                    2 -> SmartAnalyticsScreen(
                        viewModel = analyticsViewModel
                    )
                    3 -> AppsMonitorScreen(
                        viewModel = appsViewModel
                    )
                    4 -> FirewallScreen(
                        viewModel = firewallViewModel
                    )
                    5 -> SettingsScreen(
                        viewModel = mainViewModel,
                        onSaveQuota = { bytes, day, warning ->
                            app.quotaRepository.updateConfig(bytes, day, warning)
                            mainViewModel.refreshData()
                        }
                    )
                }
            }
        }
    }
}
