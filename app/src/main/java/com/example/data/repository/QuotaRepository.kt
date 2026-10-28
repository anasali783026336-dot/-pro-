package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuotaConfig(
    val quotaBytes: Long = 20L * 1024L * 1024L * 1024L, // Default 20 GB
    val renewalDayOfMonth: Int = 1,
    val warningPercent: Int = 85
)

class QuotaRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("quota_prefs", Context.MODE_PRIVATE)

    private val _quotaConfig = MutableStateFlow(loadConfig())
    val quotaConfig: StateFlow<QuotaConfig> = _quotaConfig.asStateFlow()

    private fun loadConfig(): QuotaConfig {
        val bytes = prefs.getLong(KEY_QUOTA_BYTES, 20L * 1024L * 1024L * 1024L)
        val day = prefs.getInt(KEY_RENEWAL_DAY, 1)
        val warning = prefs.getInt(KEY_WARNING_PERCENT, 85)
        return QuotaConfig(bytes, day, warning)
    }

    fun updateConfig(quotaBytes: Long, renewalDay: Int, warningPercent: Int) {
        prefs.edit()
            .putLong(KEY_QUOTA_BYTES, quotaBytes)
            .putInt(KEY_RENEWAL_DAY, renewalDay)
            .putInt(KEY_WARNING_PERCENT, warningPercent)
            .apply()
        _quotaConfig.value = QuotaConfig(quotaBytes, renewalDay, warningPercent)
    }

    companion object {
        private const val KEY_QUOTA_BYTES = "key_quota_bytes"
        private const val KEY_RENEWAL_DAY = "key_renewal_day"
        private const val KEY_WARNING_PERCENT = "key_warning_percent"
    }
}
