package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firewall_rules")
data class FirewallRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val policy: String, // ALLOW_ALL, BLOCK_ALL, WIFI_ONLY, CELLULAR_ONLY
    val isApplied: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
