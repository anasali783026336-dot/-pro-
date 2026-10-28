package com.example.data.repository

import com.example.data.db.FirewallLogDao
import com.example.data.db.FirewallLogEntity
import com.example.data.db.FirewallRuleDao
import com.example.data.db.FirewallRuleEntity
import com.example.data.model.FirewallPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirewallRepository(
    private val firewallRuleDao: FirewallRuleDao,
    private val firewallLogDao: FirewallLogDao
) {
    val allRulesFlow: Flow<List<FirewallRuleEntity>> = firewallRuleDao.getAllRules()

    val rulesMapFlow: Flow<Map<String, FirewallPolicy>> = firewallRuleDao.getAllRules().map { list ->
        list.associate { it.packageName to (try { FirewallPolicy.valueOf(it.policy) } catch (_: Exception) { FirewallPolicy.ALLOW_ALL }) }
    }

    val recentLogsFlow: Flow<List<FirewallLogEntity>> = firewallLogDao.getRecentLogs()

    suspend fun getRuleForPackage(packageName: String): FirewallPolicy {
        val entity = firewallRuleDao.getRuleForPackage(packageName) ?: return FirewallPolicy.ALLOW_ALL
        return try {
            FirewallPolicy.valueOf(entity.policy)
        } catch (_: Exception) {
            FirewallPolicy.ALLOW_ALL
        }
    }

    suspend fun setRule(packageName: String, appName: String, policy: FirewallPolicy) {
        firewallRuleDao.insertOrUpdate(
            FirewallRuleEntity(
                packageName = packageName,
                appName = appName,
                policy = policy.name,
                isApplied = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetAllRules() {
        firewallRuleDao.clearAll()
    }

    suspend fun logAttempt(packageName: String, appName: String, networkType: String, action: String = "حظر محاولة الاتصال") {
        firewallLogDao.insertLog(
            FirewallLogEntity(
                packageName = packageName,
                appName = appName,
                networkType = networkType,
                actionTaken = action,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearLogs() {
        firewallLogDao.clearLogs()
    }
}
