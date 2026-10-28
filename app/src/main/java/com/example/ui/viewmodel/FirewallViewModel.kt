package com.example.ui.viewmodel

import android.app.Application
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DataGuardianApplication
import com.example.data.db.FirewallLogEntity
import com.example.data.db.FirewallRuleEntity
import com.example.data.model.AppUsageInfo
import com.example.data.model.FirewallPolicy
import com.example.service.DataGuardianVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FirewallUiState(
    val isRunning: Boolean = false,
    val blockedAppsCount: Int = 0,
    val rules: List<FirewallRuleEntity> = emptyList(),
    val logs: List<FirewallLogEntity> = emptyList(),
    val appList: List<AppUsageInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataGuardianApplication
    private val firewallRepo = app.firewallRepository
    private val networkStatsRepo = app.networkStatsRepository

    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            DataGuardianVpnService.isFirewallRunning.collectLatest { running ->
                _uiState.value = _uiState.value.copy(isRunning = running)
            }
        }

        viewModelScope.launch {
            DataGuardianVpnService.blockedAppsCount.collectLatest { count ->
                _uiState.value = _uiState.value.copy(blockedAppsCount = count)
            }
        }

        viewModelScope.launch {
            firewallRepo.allRulesFlow.collectLatest { ruleList ->
                _uiState.value = _uiState.value.copy(rules = ruleList)
            }
        }

        viewModelScope.launch {
            firewallRepo.recentLogsFlow.collectLatest { logList ->
                _uiState.value = _uiState.value.copy(logs = logList)
            }
        }

        loadAppList()
    }

    fun loadAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rulesMap = app.database.firewallRuleDao().getAllRulesDirect().associate {
                it.packageName to (try { FirewallPolicy.valueOf(it.policy) } catch (_: Exception) { FirewallPolicy.ALLOW_ALL })
            }
            val startOfToday = System.currentTimeMillis() - 86400000L
            val apps = networkStatsRepo.getAppUsageList(startOfToday, System.currentTimeMillis(), rulesMap)
            _uiState.value = _uiState.value.copy(
                appList = apps,
                isLoading = false
            )
        }
    }

    fun setAppPolicy(packageName: String, appName: String, policy: FirewallPolicy) {
        viewModelScope.launch(Dispatchers.IO) {
            firewallRepo.setRule(packageName, appName, policy)
            if (_uiState.value.isRunning) {
                DataGuardianVpnService.reloadRules(getApplication())
            }
            loadAppList()
        }
    }

    fun startFirewall() {
        DataGuardianVpnService.startService(getApplication())
    }

    fun stopFirewall() {
        DataGuardianVpnService.stopService(getApplication())
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            firewallRepo.clearLogs()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
