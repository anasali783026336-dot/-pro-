package com.example.data.model

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val totalBytes: Long = wifiBytes + mobileBytes,
    val backgroundBytes: Long = 0L,
    val policy: FirewallPolicy = FirewallPolicy.ALLOW_ALL,
    val isSystemApp: Boolean = false
)
