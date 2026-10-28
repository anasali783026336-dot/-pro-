package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.FirewallRuleEntity
import com.example.data.model.AlertConfig
import com.example.data.model.DataUsageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataExportManager(
    private val database: AppDatabase,
    private val quotaRepo: QuotaRepository,
    private val alertRepo: AlertRepository
) {

    /**
     * Exports daily usage records to a clean CSV file in app cache.
     */
    suspend fun exportUsageToCsv(context: Context): File = withContext(Dispatchers.IO) {
        val records = database.dailyUsageDao().getAllRecords().first()
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "data_guardian_history_${System.currentTimeMillis()}.csv")

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write("التاريخ,استهلاك_واي_فاي_بايت,استهلاك_الشريحة_بايت,الإجمالي_بايت,استهلاك_واي_فاي_منسق,استهلاك_الشريحة_منسق,الإجمالي_منسق\n")
            for (r in records) {
                val wifiFmt = DataUsageStats.formatBytesShort(r.wifiBytes)
                val mobFmt = DataUsageStats.formatBytesShort(r.mobileBytes)
                val totFmt = DataUsageStats.formatBytesShort(r.totalBytes)
                writer.write("${r.dateString},${r.wifiBytes},${r.mobileBytes},${r.totalBytes},\"$wifiFmt\",\"$mobFmt\",\"$totFmt\"\n")
            }
        }
        file
    }

    /**
     * Exports settings, quota, alerts, and firewall rules to a verified JSON string.
     */
    suspend fun exportSettingsAndRulesJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Data Guardian PRO")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Quota
        val quota = quotaRepo.quotaConfig.value
        val quotaObj = JSONObject().apply {
            put("quotaBytes", quota.quotaBytes)
            put("renewalDayOfMonth", quota.renewalDayOfMonth)
            put("warningPercent", quota.warningPercent)
        }
        root.put("quota", quotaObj)

        // Alerts
        val alerts = alertRepo.alertConfig.value
        val alertObj = JSONObject().apply {
            put("alert50Enabled", alerts.alert50Enabled)
            put("alert75Enabled", alerts.alert75Enabled)
            put("alert90Enabled", alerts.alert90Enabled)
            put("alert100Enabled", alerts.alert100Enabled)
            put("dailyLimitMb", alerts.dailyLimitMb)
            put("highAppUsageMb", alerts.highAppUsageMb)
            put("firewallStoppedAlertEnabled", alerts.firewallStoppedAlertEnabled)
        }
        root.put("alerts", alertObj)

        // Rules
        val rules = database.firewallRuleDao().getAllRulesDirect()
        val rulesArr = JSONArray()
        for (rule in rules) {
            val ruleObj = JSONObject().apply {
                put("packageName", rule.packageName)
                put("appName", rule.appName)
                put("policy", rule.policy)
            }
            rulesArr.put(ruleObj)
        }
        root.put("rules", rulesArr)

        root.toString(2)
    }

    /**
     * Validates and imports JSON backup, restoring settings and rules safely.
     */
    suspend fun importSettingsAndRulesFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "Data Guardian PRO") {
                return@withContext Result.failure(IllegalArgumentException("ملف النسخ الاحتياطي غير صالح أو غير مخصص لحارس البيانات PRO"))
            }

            // Restore Quota
            if (root.has("quota")) {
                val q = root.getJSONObject("quota")
                quotaRepo.updateConfig(
                    quotaBytes = q.optLong("quotaBytes", 20L * 1024L * 1024L * 1024L),
                    renewalDay = q.optInt("renewalDayOfMonth", 1),
                    warningPercent = q.optInt("warningPercent", 85)
                )
            }

            // Restore Alerts
            if (root.has("alerts")) {
                val a = root.getJSONObject("alerts")
                alertRepo.updateConfig(
                    AlertConfig(
                        alert50Enabled = a.optBoolean("alert50Enabled", true),
                        alert75Enabled = a.optBoolean("alert75Enabled", true),
                        alert90Enabled = a.optBoolean("alert90Enabled", true),
                        alert100Enabled = a.optBoolean("alert100Enabled", true),
                        dailyLimitMb = a.optInt("dailyLimitMb", 2048),
                        highAppUsageMb = a.optInt("highAppUsageMb", 512),
                        firewallStoppedAlertEnabled = a.optBoolean("firewallStoppedAlertEnabled", true)
                    )
                )
            }

            // Restore Rules
            var rulesRestored = 0
            if (root.has("rules")) {
                val arr = root.getJSONArray("rules")
                val ruleEntities = mutableListOf<FirewallRuleEntity>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val pkg = item.optString("packageName", "")
                    val app = item.optString("appName", pkg)
                    val pol = item.optString("policy", "ALLOW_ALL")
                    if (pkg.isNotEmpty()) {
                        ruleEntities.add(
                            FirewallRuleEntity(
                                packageName = pkg,
                                appName = app,
                                policy = pol,
                                isApplied = true,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        rulesRestored++
                    }
                }
                if (ruleEntities.isNotEmpty()) {
                    database.firewallRuleDao().insertAll(ruleEntities)
                }
            }

            Result.success(rulesRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears local history database.
     */
    suspend fun clearHistory(): Unit = withContext(Dispatchers.IO) {
        database.dailyUsageDao().clearAll()
    }
}
