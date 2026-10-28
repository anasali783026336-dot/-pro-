package com.example.data.model

data class AlertConfig(
    val alert50Enabled: Boolean = true,
    val alert75Enabled: Boolean = true,
    val alert90Enabled: Boolean = true,
    val alert100Enabled: Boolean = true,
    val dailyLimitMb: Int = 2048, // 2 GB daily limit, 0 = disabled
    val highAppUsageMb: Int = 512, // 512 MB single app limit, 0 = disabled
    val firewallStoppedAlertEnabled: Boolean = true
)
