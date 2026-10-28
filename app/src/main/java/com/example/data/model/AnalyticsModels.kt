package com.example.data.model

import java.util.Locale

data class ComparisonResult(
    val currentBytes: Long,
    val previousBytes: Long
) {
    val diffBytes: Long = currentBytes - previousBytes
    val percentChange: Double = if (previousBytes > 0) {
        ((diffBytes.toDouble() / previousBytes.toDouble()) * 100.0)
    } else 0.0

    val isIncrease: Boolean = diffBytes > 0

    fun formattedPercent(): String {
        val sign = if (isIncrease) "+" else ""
        return "$sign${String.format(Locale.US, "%.1f", percentChange)}%"
    }
}

data class SpikeAppInfo(
    val appName: String,
    val packageName: String,
    val todayBytes: Long,
    val averageBytes: Long
)

data class DataSavingSuggestion(
    val titleAr: String,
    val descAr: String,
    val priorityAr: String, // "عالية", "متوسطة", "منخفضة"
    val isWarning: Boolean = false
)

data class SmartAnalyticsReport(
    val todayVsYesterday: ComparisonResult = ComparisonResult(0L, 0L),
    val thisWeekVsLastWeek: ComparisonResult = ComparisonResult(0L, 0L),
    val thisMonthVsLastMonth: ComparisonResult = ComparisonResult(0L, 0L),
    val dailyAverageBytes: Long = 0L,
    val forecastedMonthTotalBytes: Long = 0L,
    val estimatedDaysRemaining: Int = 0,
    val spikingApps: List<SpikeAppInfo> = emptyList(),
    val suggestions: List<DataSavingSuggestion> = emptyList(),
    val hasSufficientData: Boolean = false
)
