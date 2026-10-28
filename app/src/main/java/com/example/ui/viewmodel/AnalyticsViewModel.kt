package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DataGuardianApplication
import com.example.data.model.SmartAnalyticsReport
import com.example.data.repository.AnalyticsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val report: SmartAnalyticsReport = SmartAnalyticsReport(),
    val isLoading: Boolean = true
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataGuardianApplication
    private val analyticsEngine = AnalyticsEngine(app.networkStatsRepository, app.quotaRepository)

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        refreshAnalytics()
    }

    fun refreshAnalytics() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val report = analyticsEngine.generateReport()
            _uiState.value = AnalyticsUiState(report = report, isLoading = false)
        }
    }
}
