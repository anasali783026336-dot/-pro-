package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DataGuardianApplication
import com.example.data.model.DataUsageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class HistoryPreset(val labelAr: String) {
    TODAY("اليوم"),
    YESTERDAY("أمس"),
    TWO_DAYS_AGO("أول أمس"),
    THIS_WEEK("هذا الأسبوع"),
    LAST_WEEK("الأسبوع السابق"),
    THIS_MONTH("هذا الشهر"),
    LAST_MONTH("الشهر السابق")
}

data class HistoryUiState(
    val selectedPreset: HistoryPreset = HistoryPreset.TODAY,
    val selectedPeriodStats: DataUsageStats = DataUsageStats(),
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedCalendarDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val monthUsageMap: Map<Int, DataUsageStats> = emptyMap(),
    val isLoading: Boolean = false
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataGuardianApplication
    private val networkStatsRepo = app.networkStatsRepository

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadPreset(HistoryPreset.TODAY)
        loadCalendarMonth(
            _uiState.value.calendarYear,
            _uiState.value.calendarMonth,
            _uiState.value.selectedCalendarDay
        )
    }

    fun selectPreset(preset: HistoryPreset) {
        _uiState.value = _uiState.value.copy(selectedPreset = preset)
        loadPreset(preset)
    }

    private fun loadPreset(preset: HistoryPreset) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val stats = when (preset) {
                HistoryPreset.TODAY -> networkStatsRepo.getTodayUsage()
                HistoryPreset.YESTERDAY -> networkStatsRepo.getYesterdayUsage()
                HistoryPreset.TWO_DAYS_AGO -> networkStatsRepo.getTwoDaysAgoUsage()
                HistoryPreset.THIS_WEEK -> networkStatsRepo.getLast7DaysUsage()
                HistoryPreset.LAST_WEEK -> {
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -13)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    val start = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, 6)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    val end = cal.timeInMillis
                    networkStatsRepo.getNetworkUsage(start, end)
                }
                HistoryPreset.THIS_MONTH -> networkStatsRepo.getCurrentMonthUsage(1)
                HistoryPreset.LAST_MONTH -> networkStatsRepo.getLastMonthUsage()
            }

            _uiState.value = _uiState.value.copy(
                selectedPeriodStats = stats,
                isLoading = false
            )
        }
    }

    fun selectCalendarDay(day: Int) {
        _uiState.value = _uiState.value.copy(selectedCalendarDay = day)
        viewModelScope.launch(Dispatchers.IO) {
            val stats = networkStatsRepo.getUsageForDate(
                _uiState.value.calendarYear,
                _uiState.value.calendarMonth,
                day
            )
            // Update day in map as well
            val updatedMap = _uiState.value.monthUsageMap.toMutableMap()
            updatedMap[day] = stats
            _uiState.value = _uiState.value.copy(monthUsageMap = updatedMap)
        }
    }

    fun previousMonth() {
        var year = _uiState.value.calendarYear
        var month = _uiState.value.calendarMonth - 1
        if (month < 0) {
            month = 11
            year--
        }
        loadCalendarMonth(year, month, 1)
    }

    fun nextMonth() {
        var year = _uiState.value.calendarYear
        var month = _uiState.value.calendarMonth + 1
        if (month > 11) {
            month = 0
            year++
        }
        loadCalendarMonth(year, month, 1)
    }

    private fun loadCalendarMonth(year: Int, month: Int, selectedDay: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                calendarYear = year,
                calendarMonth = month,
                selectedCalendarDay = selectedDay,
                isLoading = true
            )
            val map = networkStatsRepo.getMonthDailyBreakdown(year, month)
            _uiState.value = _uiState.value.copy(
                monthUsageMap = map,
                isLoading = false
            )
        }
    }
}
