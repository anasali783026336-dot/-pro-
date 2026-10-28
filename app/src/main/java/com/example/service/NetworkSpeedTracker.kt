package com.example.service

import android.net.TrafficStats
import com.example.data.model.DataUsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SpeedTelemetry(
    val downBytesPerSec: Long = 0L,
    val upBytesPerSec: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L,
    val recentDownHistory: List<Long> = emptyList(),
    val recentUpHistory: List<Long> = emptyList()
) {
    fun formattedDownload(): String = "${DataUsageStats.formatBytesShort(downBytesPerSec)}/s"
    fun formattedUpload(): String = "${DataUsageStats.formatBytesShort(upBytesPerSec)}/s"
    fun formattedSessionTotal(): String = DataUsageStats.formatBytesShort(sessionRxBytes + sessionTxBytes)
}

class NetworkSpeedTracker {
    private val _telemetryFlow = MutableStateFlow(SpeedTelemetry())
    val telemetryFlow: StateFlow<SpeedTelemetry> = _telemetryFlow.asStateFlow()

    private var trackerJob: Job? = null
    private var startRxBytes = 0L
    private var startTxBytes = 0L
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

    private val downHistory = ArrayDeque<Long>(30)
    private val upHistory = ArrayDeque<Long>(30)

    fun start(scope: CoroutineScope) {
        stop()
        val initRx = TrafficStats.getTotalRxBytes()
        val initTx = TrafficStats.getTotalTxBytes()

        startRxBytes = if (initRx != TrafficStats.UNSUPPORTED.toLong()) initRx else 0L
        startTxBytes = if (initTx != TrafficStats.UNSUPPORTED.toLong()) initTx else 0L

        lastRxBytes = startRxBytes
        lastTxBytes = startTxBytes
        lastTimestamp = System.currentTimeMillis()

        trackerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentTimestamp = System.currentTimeMillis()

                val elapsedSec = (currentTimestamp - lastTimestamp).coerceAtLeast(1) / 1000.0
                if (lastRxBytes != TrafficStats.UNSUPPORTED.toLong() && lastTxBytes != TrafficStats.UNSUPPORTED.toLong()) {
                    val deltaRx = (currentRx - lastRxBytes).coerceAtLeast(0)
                    val deltaTx = (currentTx - lastTxBytes).coerceAtLeast(0)

                    val downSpeed = (deltaRx / elapsedSec).toLong()
                    val upSpeed = (deltaTx / elapsedSec).toLong()

                    val sessionRx = (currentRx - startRxBytes).coerceAtLeast(0)
                    val sessionTx = (currentTx - startTxBytes).coerceAtLeast(0)

                    synchronized(downHistory) {
                        if (downHistory.size >= 30) downHistory.removeFirst()
                        downHistory.addLast(downSpeed)

                        if (upHistory.size >= 30) upHistory.removeFirst()
                        upHistory.addLast(upSpeed)
                    }

                    _telemetryFlow.value = SpeedTelemetry(
                        downBytesPerSec = downSpeed,
                        upBytesPerSec = upSpeed,
                        sessionRxBytes = sessionRx,
                        sessionTxBytes = sessionTx,
                        recentDownHistory = downHistory.toList(),
                        recentUpHistory = upHistory.toList()
                    )
                }

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTimestamp = currentTimestamp
            }
        }
    }

    fun stop() {
        trackerJob?.cancel()
        trackerJob = null
        _telemetryFlow.value = SpeedTelemetry()
    }
}
