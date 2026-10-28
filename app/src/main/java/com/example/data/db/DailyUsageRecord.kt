package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageRecord(
    @PrimaryKey val dateString: String, // Format: YYYY-MM-DD
    val wifiBytes: Long,
    val mobileBytes: Long,
    val totalBytes: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
