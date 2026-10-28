package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DataGuardianApplication
import com.example.data.model.AppUsageInfo
import com.example.data.model.DataUsageStats
import com.example.data.model.NetworkType
import com.example.data.model.SpeedInfo
import com.example.data.repository.QuotaConfig
import com.example.service.DataGuardianVpnService
import com.example.service.NetworkSpeedTracker
import com.example.ui.components.DayUsageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val todayUsage: DataUsageStats = DataUsageStats(),
    val yesterdayUsage: DataUsageStats = DataUsageStats(),
    val last7DaysUsage: DataUsageStats = DataUsageStats(),
    val monthUsage: DataUsageStats = DataUsageStats(),
    val top5Apps: List<AppUsageInfo> = emptyList(),
    val last7DaysDailyItems: List<DayUsageItem> = emptyList(),
    val activeNetwork: NetworkType = NetworkType.NONE,
    val speedInfo: SpeedInfo = SpeedInfo(),
    val quotaConfig: QuotaConfig = QuotaConfig(),
    val isFirewallRunning: Boolean = false,
    val blockedAppsCount: Int = 0,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataGuardianApplication
    private val networkStatsRepo = app.networkStatsRepository
    private val firewallRepo = app.firewallRepository
    private val quotaRepo = app.quotaRepository
    private val speedTracker = NetworkSpeedTracker()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        speedTracker.start(viewModelScope)

        // Observe speed
        viewModelScope.launch {
            speedTracker.speedFlow.collectLatest { speed ->
                _uiState.value = _uiState.value.copy(speedInfo = speed)
            }
        }

        // Observe Quota
        viewModelScope.launch {
            quotaRepo.quotaConfig.collectLatest { config ->
                _uiState.value = _uiState.value.copy(quotaConfig = config)
            }
        }

        // Observe Firewall Service Running state
        viewModelScope.launch {
            DataGuardianVpnService.isFirewallRunning.collectLatest { running ->
                _uiState.value = _uiState.value.copy(isFirewallRunning = running)
            }
        }

        // Observe Blocked count
        viewModelScope.launch {
            DataGuardianVpnService.blockedAppsCount.collectLatest { count ->
                _uiState.value = _uiState.value.copy(blockedAppsCount = count)
            }
        }

        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasPerm = networkStatsRepo.hasUsageStatsPermission()
            val activeNet = networkStatsRepo.getActiveNetworkType()

            _uiState.value = _uiState.value.copy(
                hasPermission = hasPerm,
                activeNetwork = activeNet,
                isLoading = true
            )

            if (!hasPerm) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val today = networkStatsRepo.getTodayUsage()
            val yesterday = networkStatsRepo.getYesterdayUsage()
            val last7Days = networkStatsRepo.getLast7DaysUsage()
            val month = networkStatsRepo.getCurrentMonthUsage(_uiState.value.quotaConfig.renewalDayOfMonth)

            // Top 5 apps today
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val rules = app.database.firewallRuleDao().getAllRulesDirect().associate {
                it.packageName to (try { com.example.data.model.FirewallPolicy.valueOf(it.policy) } catch (_: Exception) { com.example.data.model.FirewallPolicy.ALLOW_ALL })
            }

            val apps = networkStatsRepo.getAppUsageList(todayStart, System.currentTimeMillis(), rules)
            val top5 = apps.take(5)

            // 7 Days Chart breakdown
            val chartItems = mutableListOf<DayUsageItem>()
            val cal = Calendar.getInstance()
            val dayNamesAr = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

            for (i in 6 downTo 0) {
                val dayCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = dayCal.timeInMillis
                dayCal.set(Calendar.HOUR_OF_DAY, 23)
                dayCal.set(Calendar.MINUTE, 59)
                dayCal.set(Calendar.SECOND, 59)
                val end = dayCal.timeInMillis

                val dayStats = networkStatsRepo.getNetworkUsage(start, end)
                val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
                val dayLabel = dayNamesAr[(dayOfWeek - 1).coerceIn(0, 6)]
                chartItems.add(DayUsageItem(dayLabel, dayStats.wifiBytes, dayStats.mobileBytes))
            }

            _uiState.value = _uiState.value.copy(
                todayUsage = today,
                yesterdayUsage = yesterday,
                last7DaysUsage = last7Days,
                monthUsage = month,
                top5Apps = top5,
                last7DaysDailyItems = chartItems,
                isLoading = false
            )
        }
    }

    override fun onCleared() {
        speedTracker.stop()
        super.onCleared()
    }
}
