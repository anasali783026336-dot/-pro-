package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.AlertRepository
import com.example.data.repository.FirewallRepository
import com.example.data.repository.NetworkStatsRepository
import com.example.data.repository.QuotaRepository
import com.example.service.NotificationHelper
import com.example.worker.QuotaAlertWorker

class DataGuardianApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val networkStatsRepository by lazy {
        NetworkStatsRepository(this, database.dailyUsageDao())
    }
    val firewallRepository by lazy {
        FirewallRepository(database.firewallRuleDao(), database.firewallLogDao())
    }
    val quotaRepository by lazy {
        QuotaRepository(this)
    }
    val alertRepository by lazy {
        AlertRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.initChannels(this)
            QuotaAlertWorker.schedule(this)
        } catch (_: Exception) {}
    }
}
