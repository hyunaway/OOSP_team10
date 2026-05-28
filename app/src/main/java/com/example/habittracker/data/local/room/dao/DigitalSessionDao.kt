// 경로: com/example/habittracker/data/local/room/dao/DigitalSessionDao.kt
package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habittracker.data.entity.DigitalSessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
abstract class DigitalSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: DigitalSessionEntity): Long

    @Update
    abstract suspend fun update(entity: DigitalSessionEntity): Int

    @Query("DELETE FROM digital_sessions WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    fun getTodaySessionsByApp(): Flow<List<AppDurationSum>> =
        getSessionsByAppBetween(todayStart(), Long.MAX_VALUE)

    @Query("SELECT * FROM digital_sessions WHERE startTime BETWEEN :start AND :end ORDER BY startTime DESC")
    abstract fun getSessionsBetween(start: Long, end: Long): Flow<List<DigitalSessionEntity>>

    @Query("""
        SELECT * FROM digital_sessions
        WHERE startTime BETWEEN :start AND :end AND appPackage = :appPackage
        ORDER BY startTime DESC
    """)
    abstract fun getSessionsBetweenByApp(
        start: Long,
        end: Long,
        appPackage: String,
    ): Flow<List<DigitalSessionEntity>>

    @Query("""
        SELECT appPackage, AVG(durationMinutes) AS avgDuration
        FROM digital_sessions
        WHERE startTime BETWEEN :start AND :end
        GROUP BY appPackage
    """)
    abstract fun getAvgSessionByApp(start: Long, end: Long): Flow<List<AppAvgDuration>>

    @Query("""
        SELECT appPackage, SUM(durationMinutes) AS total
        FROM digital_sessions
        WHERE startTime BETWEEN :start AND :end
        GROUP BY appPackage
    """)
    protected abstract fun getSessionsByAppBetween(start: Long, end: Long): Flow<List<AppDurationSum>>

    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
