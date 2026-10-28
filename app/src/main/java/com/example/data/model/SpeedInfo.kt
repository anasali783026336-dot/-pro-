package com.example.data.model

data class SpeedInfo(
    val downBytesPerSec: Long = 0L,
    val upBytesPerSec: Long = 0L
) {
    fun formattedDownload(): String {
        return "${DataUsageStats.formatBytesShort(downBytesPerSec)}/s"
    }

    fun formattedUpload(): String {
        return "${DataUsageStats.formatBytesShort(upBytesPerSec)}/s"
    }
}
