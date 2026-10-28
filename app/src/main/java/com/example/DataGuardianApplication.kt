package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.FirewallRepository
import com.example.data.repository.NetworkStatsRepository
import com.example.data.repository.QuotaRepository

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

    override fun onCreate() {
        super.onCreate()
    }
}
