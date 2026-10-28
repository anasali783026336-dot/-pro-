package com.example.service

import android.net.TrafficStats
import com.example.data.model.SpeedInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkSpeedTracker {
    private val _speedFlow = MutableStateFlow(SpeedInfo())
    val speedFlow: StateFlow<SpeedInfo> = _speedFlow.asStateFlow()

    private var trackerJob: Job? = null
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

    fun start(scope: CoroutineScope) {
        stop()
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
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

                    _speedFlow.value = SpeedInfo(downSpeed, upSpeed)
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
        _speedFlow.value = SpeedInfo()
    }
}
