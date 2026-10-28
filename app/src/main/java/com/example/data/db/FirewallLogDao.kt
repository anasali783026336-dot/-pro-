package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FirewallLogDao {
    @Query("SELECT * FROM firewall_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<FirewallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FirewallLogEntity)

    @Query("DELETE FROM firewall_logs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM firewall_logs")
    fun getLogsCount(): Flow<Int>
}
