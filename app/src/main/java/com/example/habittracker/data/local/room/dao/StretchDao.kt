// 경로: com/example/habittracker/data/local/room/dao/StretchDao.kt
package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habittracker.data.entity.StretchLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
abstract class StretchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: StretchLogEntity): Long

    @Update
    abstract suspend fun update(entity: StretchLogEntity): Int

    @Query("DELETE FROM stretch_logs WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    fun getTodayLogs(): Flow<List<StretchLogEntity>> = getLogsBetween(todayStart(), Long.MAX_VALUE)

    @Query("SELECT * FROM stretch_logs WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    abstract fun getLogsBetween(start: Long, end: Long): Flow<List<StretchLogEntity>>

    @Query("""
        SELECT * FROM stretch_logs
        WHERE timestamp BETWEEN :start AND :end AND bodyPart = :bodyPart
        ORDER BY timestamp DESC
    """)
    abstract fun getLogsBetweenByBodyPart(
        start: Long,
        end: Long,
        bodyPart: String,
    ): Flow<List<StretchLogEntity>>

    @Query("UPDATE stretch_logs SET bodyPart = :bodyPart WHERE id = :id")
    abstract suspend fun updateBodyPartById(id: Long, bodyPart: String): Int

    @Query("SELECT bodyPart, COUNT(*) AS count FROM stretch_logs WHERE timestamp BETWEEN :start AND :end GROUP BY bodyPart")
    abstract fun getBodyPartDistribution(start: Long, end: Long): Flow<List<BodyPartCount>>

    @Query("""
        SELECT CAST(strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM stretch_logs
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY hour
        ORDER BY hour
    """)
    abstract fun getHourlyDistribution(start: Long, end: Long): Flow<List<HourlyCount>>

    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
