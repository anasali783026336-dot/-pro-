package com.example.data.model

import java.util.Locale

data class DataUsageStats(
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes

    companion object {
        fun formatBytes(bytes: Long): Pair<String, String> {
            val absBytes = if (bytes < 0) 0L else bytes
            return when {
                absBytes >= 1_073_741_824L -> {
                    val gb = absBytes.toDouble() / 1_073_741_824.0
                    Pair(String.format(Locale.US, "%.2f", gb), "جيجابايت (GB)")
                }
                absBytes >= 1_048_576L -> {
                    val mb = absBytes.toDouble() / 1_048_576.0
                    Pair(String.format(Locale.US, "%.1f", mb), "ميجابايت (MB)")
                }
                absBytes >= 1024L -> {
                    val kb = absBytes.toDouble() / 1024.0
                    Pair(String.format(Locale.US, "%.1f", kb), "كيلوبايت (KB)")
                }
                else -> {
                    Pair("$absBytes", "بايت")
                }
            }
        }

        fun formatBytesShort(bytes: Long): String {
            val (value, unit) = formatBytes(bytes)
            val shortUnit = when {
                unit.contains("GB") -> "GB"
                unit.contains("MB") -> "MB"
                unit.contains("KB") -> "KB"
                else -> "B"
            }
            return "$value $shortUnit"
        }
    }
}
