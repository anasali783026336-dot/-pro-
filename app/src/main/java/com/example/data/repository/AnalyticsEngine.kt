package com.example.data.repository

import com.example.data.model.ComparisonResult
import com.example.data.model.DataSavingSuggestion
import com.example.data.model.DataUsageStats
import com.example.data.model.SmartAnalyticsReport
import com.example.data.model.SpikeAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class AnalyticsEngine(
    private val networkStatsRepo: NetworkStatsRepository,
    private val quotaRepo: QuotaRepository
) {

    suspend fun generateReport(): SmartAnalyticsReport = withContext(Dispatchers.IO) {
        if (!networkStatsRepo.hasUsageStatsPermission()) {
            return@withContext SmartAnalyticsReport(hasSufficientData = false)
        }

        val quotaConfig = quotaRepo.quotaConfig.value
        val todayStats = networkStatsRepo.getTodayUsage()
        val yesterdayStats = networkStatsRepo.getYesterdayUsage()

        // 1. Today vs Yesterday Comparison
        val todayVsYesterday = ComparisonResult(todayStats.totalBytes, yesterdayStats.totalBytes)

        // 2. This Week (last 7 days) vs Previous Week
        val thisWeekStats = networkStatsRepo.getLast7DaysUsage()
        val prevWeekCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -13)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val prevWeekStart = prevWeekCal.timeInMillis
        prevWeekCal.add(Calendar.DAY_OF_YEAR, 6)
        prevWeekCal.set(Calendar.HOUR_OF_DAY, 23)
        prevWeekCal.set(Calendar.MINUTE, 59)
        prevWeekCal.set(Calendar.SECOND, 59)
        val prevWeekEnd = prevWeekCal.timeInMillis
        val prevWeekStats = networkStatsRepo.getNetworkUsage(prevWeekStart, prevWeekEnd)

        val thisWeekVsLastWeek = ComparisonResult(thisWeekStats.totalBytes, prevWeekStats.totalBytes)

        // 3. This Month vs Previous Month
        val thisMonthStats = networkStatsRepo.getCurrentMonthUsage(quotaConfig.renewalDayOfMonth)
        val lastMonthStats = networkStatsRepo.getLastMonthUsage()
        val thisMonthVsLastMonth = ComparisonResult(thisMonthStats.totalBytes, lastMonthStats.totalBytes)

        // 4. Daily Average & End-of-month Forecast
        val cal = Calendar.getInstance()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Days elapsed in current cycle
        val daysElapsed = dayOfMonth.coerceAtLeast(1)
        val dailyAverage = if (daysElapsed > 0) thisMonthStats.totalBytes / daysElapsed else 0L

        val forecastedTotal = dailyAverage * totalDaysInMonth
        val remainingQuota = (quotaConfig.quotaBytes - thisMonthStats.totalBytes).coerceAtLeast(0L)
        val estimatedDaysRemaining = if (dailyAverage > 0) {
            (remainingQuota / dailyAverage).toInt().coerceAtMost(totalDaysInMonth)
        } else totalDaysInMonth

        // 5. Detect Spiking Apps Today
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val appsToday = networkStatsRepo.getAppUsageList(todayStart, System.currentTimeMillis(), emptyMap())
        val spikingList = mutableListOf<SpikeAppInfo>()

        for (app in appsToday.take(10)) {
            // If app consumed > 100 MB today
            if (app.totalBytes >= 100L * 1024L * 1024L) {
                spikingList.add(
                    SpikeAppInfo(
                        appName = app.appName,
                        packageName = app.packageName,
                        todayBytes = app.totalBytes,
                        averageBytes = app.totalBytes / 2 // Approximation based on active session
                    )
                )
            }
        }

        // 6. Generate Contextual Suggestions
        val suggestions = mutableListOf<DataSavingSuggestion>()

        if (forecastedTotal > quotaConfig.quotaBytes && quotaConfig.quotaBytes > 0) {
            val exceedGb = (forecastedTotal - quotaConfig.quotaBytes) / (1024L * 1024L * 1024L)
            suggestions.add(
                DataSavingSuggestion(
                    titleAr = "خطر تجاوز الباقة الشهرية",
                    descAr = "بناءً على متوسط استهلاكك اليومي (${DataUsageStats.formatBytesShort(dailyAverage)})، يُتوقع تجاوز الباقة بنحو $exceedGb جيجابايت قبل نهاية الشهر.",
                    priorityAr = "عالية",
                    isWarning = true
                )
            )
        }

        if (todayStats.mobileBytes > todayStats.wifiBytes && todayStats.mobileBytes > 200L * 1024L * 1024L) {
            suggestions.add(
                DataSavingSuggestion(
                    titleAr = "اعتماد مكثف على بيانات الشريحة",
                    descAr = "أكثر من 50% من استهلاك اليوم تم عبر شبكة الهاتف. احرص على تفعيل Wi-Fi عند تنزيل التحديثات والفيديوهات الكبيرة.",
                    priorityAr = "متوسطة",
                    isWarning = false
                )
            )
        }

        val topBgApp = appsToday.maxByOrNull { it.backgroundBytes }
        if (topBgApp != null && topBgApp.backgroundBytes > 50L * 1024L * 1024L) {
            suggestions.add(
                DataSavingSuggestion(
                    titleAr = "استهلاك في الخلفية: ${topBgApp.appName}",
                    descAr = "استهلك هذا التطبيق ${DataUsageStats.formatBytesShort(topBgApp.backgroundBytes)} في الخلفية دون فتح الشاشة. ننصح بحظره أو تقييده عبر جدار الحماية.",
                    priorityAr = "عالية",
                    isWarning = true
                )
            )
        }

        if (suggestions.isEmpty()) {
            suggestions.add(
                DataSavingSuggestion(
                    titleAr = "استهلاكك ممتاز ومستقر",
                    descAr = "معدل استهلاكك اليومي يقع ضمن الحدود الآمنة لباقة الإنترنت الشهرية دون هدر ملحوظ.",
                    priorityAr = "منخفضة",
                    isWarning = false
                )
            )
        }

        SmartAnalyticsReport(
            todayVsYesterday = todayVsYesterday,
            thisWeekVsLastWeek = thisWeekVsLastWeek,
            thisMonthVsLastMonth = thisMonthVsLastMonth,
            dailyAverageBytes = dailyAverage,
            forecastedMonthTotalBytes = forecastedTotal,
            estimatedDaysRemaining = estimatedDaysRemaining,
            spikingApps = spikingList,
            suggestions = suggestions,
            hasSufficientData = thisMonthStats.totalBytes > 0
        )
    }
}
