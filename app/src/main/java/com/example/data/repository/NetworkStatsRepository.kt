package com.example.data.repository

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import com.example.data.db.DailyUsageDao
import com.example.data.db.DailyUsageRecord
import com.example.data.model.AppUsageInfo
import com.example.data.model.DataUsageStats
import com.example.data.model.FirewallPolicy
import com.example.data.model.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NetworkStatsRepository(
    private val context: Context,
    private val dailyUsageDao: DailyUsageDao
) {
    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val packageManager: PackageManager = context.packageManager

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Checks if the user has granted the PACKAGE_USAGE_STATS permission.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Identifies current active network type (Wi-Fi, Cellular, None).
     */
    fun getActiveNetworkType(): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.NONE
        }
    }

    /**
     * Gets network usage for a specific time range (startTime to endTime in millis).
     */
    suspend fun getNetworkUsage(startTime: Long, endTime: Long): DataUsageStats = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext DataUsageStats(0L, 0L)
        }

        var wifiBytes = 0L
        var mobileBytes = 0L

        // 1. Query Wi-Fi
        try {
            val bucket = networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            wifiBytes = bucket.rxBytes + bucket.txBytes
        } catch (_: Exception) {
            // Fallback: sum from querySummary if device-level query is restricted
            wifiBytes = sumFromQuerySummary(ConnectivityManager.TYPE_WIFI, startTime, endTime)
        }

        // 2. Query Mobile
        try {
            val bucket = networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            mobileBytes = bucket.rxBytes + bucket.txBytes
        } catch (_: Exception) {
            // Fallback: sum from querySummary
            mobileBytes = sumFromQuerySummary(ConnectivityManager.TYPE_MOBILE, startTime, endTime)
        }

        DataUsageStats(wifiBytes = wifiBytes, mobileBytes = mobileBytes)
    }

    private fun sumFromQuerySummary(networkType: Int, startTime: Long, endTime: Long): Long {
        var total = 0L
        try {
            val stats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                total += bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) {}
        return total
    }

    /**
     * Convenience method to get usage for Today.
     */
    suspend fun getTodayUsage(): DataUsageStats {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val stats = getNetworkUsage(startTime, endTime)

        // Save daily snapshot to Room
        saveDailyUsageToDb(dateFormat.format(Date(startTime)), stats)
        return stats
    }

    /**
     * Usage for Yesterday.
     */
    suspend fun getYesterdayUsage(): DataUsageStats {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return getNetworkUsage(start, end)
    }

    /**
     * Usage for 2 days ago.
     */
    suspend fun getTwoDaysAgoUsage(): DataUsageStats {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return getNetworkUsage(start, end)
    }

    /**
     * Usage for the last 7 days.
     */
    suspend fun getLast7DaysUsage(): DataUsageStats {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return getNetworkUsage(cal.timeInMillis, System.currentTimeMillis())
    }

    /**
     * Usage for current billing cycle (this month).
     */
    suspend fun getCurrentMonthUsage(cycleStartDay: Int = 1): DataUsageStats {
        val cal = Calendar.getInstance().apply {
            val todayDay = get(Calendar.DAY_OF_MONTH)
            if (todayDay < cycleStartDay) {
                // cycle started last month
                add(Calendar.MONTH, -1)
            }
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val validDay = cycleStartDay.coerceAtMost(maxDay)
            set(Calendar.DAY_OF_MONTH, validDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return getNetworkUsage(cal.timeInMillis, System.currentTimeMillis())
    }

    /**
     * Usage for previous month.
     */
    suspend fun getLastMonthUsage(): DataUsageStats {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return getNetworkUsage(start, end)
    }

    /**
     * Usage for a specific date (year, month 0-indexed, day).
     */
    suspend fun getUsageForDate(year: Int, month: Int, day: Int): DataUsageStats {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        val stats = getNetworkUsage(start, end)
        saveDailyUsageToDb(dateFormat.format(Date(start)), stats)
        return stats
    }

    /**
     * Queries daily usage records for all days in a specific month to populate the interactive calendar.
     */
    suspend fun getMonthDailyBreakdown(year: Int, month: Int): Map<Int, DataUsageStats> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, DataUsageStats>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val now = System.currentTimeMillis()

        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            if (start > now) break // Don't query future days

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            val dateStr = dateFormat.format(Date(start))
            // Check Room cache first
            val cached = dailyUsageDao.getRecordForDate(dateStr)
            if (cached != null && start + 86400000L < now) {
                // Finished past day, cached record is final
                result[day] = DataUsageStats(cached.wifiBytes, cached.mobileBytes)
            } else {
                val stats = getNetworkUsage(start, end)
                result[day] = stats
                saveDailyUsageToDb(dateStr, stats)
            }
        }
        result
    }

    private suspend fun saveDailyUsageToDb(dateStr: String, stats: DataUsageStats) {
        try {
            dailyUsageDao.insertOrUpdate(
                DailyUsageRecord(
                    dateString = dateStr,
                    wifiBytes = stats.wifiBytes,
                    mobileBytes = stats.mobileBytes,
                    totalBytes = stats.totalBytes
                )
            )
        } catch (_: Exception) {}
    }

    /**
     * Gets network usage broken down per application (UID).
     */
    suspend fun getAppUsageList(
        startTime: Long,
        endTime: Long,
        currentRules: Map<String, FirewallPolicy>
    ): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext emptyList()
        }

        // Map UID -> (wifiBytes, mobileBytes, backgroundBytes)
        val uidWifi = mutableMapOf<Int, Long>()
        val uidMobile = mutableMapOf<Int, Long>()
        val uidBackground = mutableMapOf<Int, Long>()

        // 1. Scan Wi-Fi per UID
        try {
            val stats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                uidWifi[uid] = (uidWifi[uid] ?: 0L) + bytes
                if (bucket.state == NetworkStats.Bucket.STATE_DEFAULT) {
                    uidBackground[uid] = (uidBackground[uid] ?: 0L) + bytes
                }
            }
            stats.close()
        } catch (_: Exception) {}

        // 2. Scan Mobile per UID
        try {
            val stats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                uidMobile[uid] = (uidMobile[uid] ?: 0L) + bytes
                if (bucket.state == NetworkStats.Bucket.STATE_DEFAULT) {
                    uidBackground[uid] = (uidBackground[uid] ?: 0L) + bytes
                }
            }
            stats.close()
        } catch (_: Exception) {}

        // 3. Match with installed applications
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val result = mutableListOf<AppUsageInfo>()
        for (app in installedApps) {
            val uid = app.uid
            val wifi = uidWifi[uid] ?: 0L
            val mobile = uidMobile[uid] ?: 0L
            val bg = uidBackground[uid] ?: 0L
            val total = wifi + mobile

            val appName = try {
                packageManager.getApplicationLabel(app).toString()
            } catch (_: Exception) {
                app.packageName
            }

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val policy = currentRules[app.packageName] ?: FirewallPolicy.ALLOW_ALL

            result.add(
                AppUsageInfo(
                    packageName = app.packageName,
                    appName = appName,
                    uid = uid,
                    wifiBytes = wifi,
                    mobileBytes = mobile,
                    totalBytes = total,
                    backgroundBytes = bg,
                    policy = policy,
                    isSystemApp = isSystem
                )
            )
        }

        // Sort descending by total usage
        result.sortedByDescending { it.totalBytes }
    }
}
