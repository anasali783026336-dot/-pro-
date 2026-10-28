package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AlertConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alert_prefs", Context.MODE_PRIVATE)

    private val _alertConfig = MutableStateFlow(loadConfig())
    val alertConfig: StateFlow<AlertConfig> = _alertConfig.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun loadConfig(): AlertConfig {
        return AlertConfig(
            alert50Enabled = prefs.getBoolean(KEY_50, true),
            alert75Enabled = prefs.getBoolean(KEY_75, true),
            alert90Enabled = prefs.getBoolean(KEY_90, true),
            alert100Enabled = prefs.getBoolean(KEY_100, true),
            dailyLimitMb = prefs.getInt(KEY_DAILY_LIMIT, 2048),
            highAppUsageMb = prefs.getInt(KEY_HIGH_APP, 512),
            firewallStoppedAlertEnabled = prefs.getBoolean(KEY_FIREWALL_STOPPED, true)
        )
    }

    fun updateConfig(config: AlertConfig) {
        prefs.edit()
            .putBoolean(KEY_50, config.alert50Enabled)
            .putBoolean(KEY_75, config.alert75Enabled)
            .putBoolean(KEY_90, config.alert90Enabled)
            .putBoolean(KEY_100, config.alert100Enabled)
            .putInt(KEY_DAILY_LIMIT, config.dailyLimitMb)
            .putInt(KEY_HIGH_APP, config.highAppUsageMb)
            .putBoolean(KEY_FIREWALL_STOPPED, config.firewallStoppedAlertEnabled)
            .apply()
        _alertConfig.value = config
    }

    fun shouldFireThresholdAlert(thresholdPercent: Int): Boolean {
        val lastThreshold = prefs.getInt(KEY_LAST_THRESHOLD, 0)
        return if (thresholdPercent > lastThreshold) {
            prefs.edit().putInt(KEY_LAST_THRESHOLD, thresholdPercent).apply()
            true
        } else false
    }

    fun resetThresholdIfCycleRenewed() {
        prefs.edit().putInt(KEY_LAST_THRESHOLD, 0).apply()
    }

    fun shouldFireDailyAlert(): Boolean {
        val today = dateFormat.format(Date())
        val lastDate = prefs.getString(KEY_LAST_DAILY_DATE, "")
        return if (today != lastDate) {
            prefs.edit().putString(KEY_LAST_DAILY_DATE, today).apply()
            true
        } else false
    }

    companion object {
        private const val KEY_50 = "alert_50"
        private const val KEY_75 = "alert_75"
        private const val KEY_90 = "alert_90"
        private const val KEY_100 = "alert_100"
        private const val KEY_DAILY_LIMIT = "alert_daily_limit"
        private const val KEY_HIGH_APP = "alert_high_app"
        private const val KEY_FIREWALL_STOPPED = "alert_firewall_stopped"
        private const val KEY_LAST_THRESHOLD = "alert_last_threshold"
        private const val KEY_LAST_DAILY_DATE = "alert_last_daily_date"
    }
}
