package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firewall_logs")
data class FirewallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val networkType: String, // WIFI, CELLULAR
    val actionTaken: String, // BLOCKED, ALERTED
    val timestamp: Long = System.currentTimeMillis()
)
