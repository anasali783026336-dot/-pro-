package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.DataGuardianApplication
import com.example.data.model.DataUsageStats
import com.example.service.NotificationHelper
import java.util.Calendar
import java.util.concurrent.TimeUnit

class QuotaAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DataGuardianApplication ?: return Result.success()
        val networkStatsRepo = app.networkStatsRepository
        val quotaRepo = app.quotaRepository
        val alertRepo = app.alertRepository

        if (!networkStatsRepo.hasUsageStatsPermission()) {
            return Result.success()
        }

        val quotaConfig = quotaRepo.quotaConfig.value
        val alertConfig = alertRepo.alertConfig.value

        // 1. Check Month Quota
        val monthUsage = networkStatsRepo.getCurrentMonthUsage(quotaConfig.renewalDayOfMonth)
        val quotaBytes = quotaConfig.quotaBytes

        if (quotaBytes > 0) {
            val percent = ((monthUsage.totalBytes.toDouble() / quotaBytes.toDouble()) * 100.0).toInt()

            val usedStr = DataUsageStats.formatBytesShort(monthUsage.totalBytes)
            val totalStr = DataUsageStats.formatBytesShort(quotaBytes)

            when {
                percent >= 100 && alertConfig.alert100Enabled -> {
                    if (alertRepo.shouldFireThresholdAlert(100)) {
                        NotificationHelper.sendQuotaAlert(applicationContext, 100, usedStr, totalStr)
                    }
                }
                percent >= 90 && alertConfig.alert90Enabled -> {
                    if (alertRepo.shouldFireThresholdAlert(90)) {
                        NotificationHelper.sendQuotaAlert(applicationContext, 90, usedStr, totalStr)
                    }
                }
                percent >= 75 && alertConfig.alert75Enabled -> {
                    if (alertRepo.shouldFireThresholdAlert(75)) {
                        NotificationHelper.sendQuotaAlert(applicationContext, 75, usedStr, totalStr)
                    }
                }
                percent >= 50 && alertConfig.alert50Enabled -> {
                    if (alertRepo.shouldFireThresholdAlert(50)) {
                        NotificationHelper.sendQuotaAlert(applicationContext, 50, usedStr, totalStr)
                    }
                }
            }
        }

        // 2. Check Daily Limit
        if (alertConfig.dailyLimitMb > 0) {
            val todayUsage = networkStatsRepo.getTodayUsage()
            val limitBytes = alertConfig.dailyLimitMb.toLong() * 1024L * 1024L
            if (todayUsage.totalBytes >= limitBytes) {
                if (alertRepo.shouldFireDailyAlert()) {
                    NotificationHelper.sendDailyLimitAlert(
                        applicationContext,
                        DataUsageStats.formatBytesShort(todayUsage.totalBytes),
                        "${alertConfig.dailyLimitMb} MB"
                    )
                }
            }
        }

        // 3. Check High App Usage Today
        if (alertConfig.highAppUsageMb > 0) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val appLimitBytes = alertConfig.highAppUsageMb.toLong() * 1024L * 1024L
            val apps = networkStatsRepo.getAppUsageList(todayStart, System.currentTimeMillis(), emptyMap())
            val topApp = apps.firstOrNull()

            if (topApp != null && topApp.totalBytes >= appLimitBytes) {
                // High app threshold triggered
                NotificationHelper.sendHighAppUsageAlert(
                    applicationContext,
                    topApp.appName,
                    DataUsageStats.formatBytesShort(topApp.totalBytes)
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "data_guardian_quota_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<QuotaAlertWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
