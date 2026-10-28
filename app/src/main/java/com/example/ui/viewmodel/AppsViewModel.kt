package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DataGuardianApplication
import com.example.data.model.AppUsageInfo
import com.example.data.model.FirewallPolicy
import com.example.service.DataGuardianVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppSortOption(val labelAr: String) {
    BY_USAGE("الأكثر استهلاكاً"),
    ALPHABETICAL("أبجدياً (الاسم)"),
    BY_WIFI("استهلاك Wi-Fi"),
    BY_MOBILE("استهلاك الشريحة")
}

enum class AppTimeRange(val labelAr: String) {
    TODAY("استهلاك اليوم"),
    THIS_MONTH("استهلاك الشهر")
}

data class AppsUiState(
    val apps: List<AppUsageInfo> = emptyList(),
    val filteredApps: List<AppUsageInfo> = emptyList(),
    val searchQuery: String = "",
    val sortOption: AppSortOption = AppSortOption.BY_USAGE,
    val timeRange: AppTimeRange = AppTimeRange.TODAY,
    val selectedApp: AppUsageInfo? = null,
    val isLoading: Boolean = true
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataGuardianApplication
    private val networkStatsRepo = app.networkStatsRepository
    private val firewallRepo = app.firewallRepository

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        // Observe rules in database
        viewModelScope.launch {
            firewallRepo.rulesMapFlow.collectLatest {
                loadApps()
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val startTime = if (_uiState.value.timeRange == AppTimeRange.TODAY) {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val rules = app.database.firewallRuleDao().getAllRulesDirect().associate {
                it.packageName to (try { FirewallPolicy.valueOf(it.policy) } catch (_: Exception) { FirewallPolicy.ALLOW_ALL })
            }

            val appList = networkStatsRepo.getAppUsageList(startTime, System.currentTimeMillis(), rules)
            _uiState.value = _uiState.value.copy(
                apps = appList,
                isLoading = false
            )
            applyFilters()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setSortOption(option: AppSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilters()
    }

    fun setTimeRange(range: AppTimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = range)
        loadApps()
    }

    fun selectApp(appInfo: AppUsageInfo?) {
        _uiState.value = _uiState.value.copy(selectedApp = appInfo)
    }

    fun updatePolicy(packageName: String, appName: String, policy: FirewallPolicy) {
        viewModelScope.launch(Dispatchers.IO) {
            firewallRepo.setRule(packageName, appName, policy)
            DataGuardianVpnService.reloadRules(getApplication())
            loadApps()
        }
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        var list = _uiState.value.apps

        if (query.isNotEmpty()) {
            list = list.filter {
                it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }

        list = when (_uiState.value.sortOption) {
            AppSortOption.BY_USAGE -> list.sortedByDescending { it.totalBytes }
            AppSortOption.ALPHABETICAL -> list.sortedBy { it.appName.lowercase() }
            AppSortOption.BY_WIFI -> list.sortedByDescending { it.wifiBytes }
            AppSortOption.BY_MOBILE -> list.sortedByDescending { it.mobileBytes }
        }

        _uiState.value = _uiState.value.copy(filteredApps = list)
    }
}
