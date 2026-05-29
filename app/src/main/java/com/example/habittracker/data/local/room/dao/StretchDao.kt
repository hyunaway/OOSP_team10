package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.StretchingRecord

@Dao
abstract class StretchDao {

    @Query("SELECT COUNT(*) FROM stretching_records WHERE date = :date")
    abstract suspend fun getTodayStretchCount(date: String): Int

    @Query("SELECT * FROM stretching_records WHERE date = :date")
    abstract fun getTodayRecords(date: String): kotlinx.coroutines.flow.Flow<List<StretchingRecord>>

    @Query("DELETE FROM stretching_records WHERE date = :date AND time_slot = :timeSlot")
    abstract suspend fun deleteBySlot(date: String, timeSlot: String): Int

    @Query("SELECT * FROM stretching_records WHERE date BETWEEN :startDate AND :endDate")
    abstract fun getRecordsBetween(startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<StretchingRecord>>

    @Query("SELECT * FROM stretching_records WHERE date = :date AND time_slot = :timeSlot LIMIT 1")
    abstract suspend fun getRecordByTimeSlot(date: String, timeSlot: String): StretchingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(record: StretchingRecord): Long

    suspend fun insertStretchRecord(date: String, timeSlot: String, bodyParts: String) {
        val now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        insert(StretchingRecord(date = date, timeSlot = timeSlot, bodyParts = bodyParts, createdAt = now))
    }

    @Query("UPDATE stretching_records SET body_parts = :bodyParts WHERE id = :id")
    abstract suspend fun updateStretchRecord(id: Int, bodyParts: String): Int

    @Query("DELETE FROM stretching_records WHERE id = :id")
    abstract suspend fun deleteStretchRecord(id: Int): Int

    suspend fun isGoalAchieved(date: String): Boolean {
        return getTodayStretchCount(date) >= 4
    }

    suspend fun calculateStreak(today: String): Int {
        var streak = 0
        var currentDate = java.time.LocalDate.parse(today)
        while (isGoalAchieved(currentDate.toString())) {
            streak++
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }
}
