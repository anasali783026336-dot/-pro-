package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage ORDER BY dateString DESC")
    fun getAllRecords(): Flow<List<DailyUsageRecord>>

    @Query("SELECT * FROM daily_usage WHERE dateString = :date LIMIT 1")
    suspend fun getRecordForDate(date: String): DailyUsageRecord?

    @Query("SELECT * FROM daily_usage WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<DailyUsageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyUsageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DailyUsageRecord>)

    @Query("DELETE FROM daily_usage")
    suspend fun clearAll()
}
