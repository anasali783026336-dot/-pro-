package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FirewallRuleDao {
    @Query("SELECT * FROM firewall_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<FirewallRuleEntity>>

    @Query("SELECT * FROM firewall_rules")
    suspend fun getAllRulesDirect(): List<FirewallRuleEntity>

    @Query("SELECT * FROM firewall_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): FirewallRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: FirewallRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<FirewallRuleEntity>)

    @Query("DELETE FROM firewall_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("DELETE FROM firewall_rules")
    suspend fun clearAll()
}
