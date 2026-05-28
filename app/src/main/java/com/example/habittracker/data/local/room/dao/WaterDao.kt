// 경로: com/example/habittracker/data/local/room/dao/WaterDao.kt
package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habittracker.data.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
abstract class WaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: WaterLogEntity): Long

    @Update
    abstract suspend fun update(entity: WaterLogEntity): Int

    @Query("DELETE FROM water_logs WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    fun getTodayLogs(): Flow<List<WaterLogEntity>> = getLogsBetween(todayStart(), Long.MAX_VALUE)

    fun getTodayTotal(): Flow<Int?> = getTotalBetween(todayStart(), Long.MAX_VALUE)

    @Query("SELECT * FROM water_logs WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    abstract fun getLogsBetween(start: Long, end: Long): Flow<List<WaterLogEntity>>

    @Query("""
        SELECT CAST(strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM water_logs
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY hour
        ORDER BY hour
    """)
    abstract fun getHourlyDistribution(start: Long, end: Long): Flow<List<HourlyCount>>

    @Query("""
        SELECT CAST(strftime('%w', datetime(timestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) AS weekday,
               COUNT(*) AS count
        FROM water_logs
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY weekday
        ORDER BY weekday
    """)
    abstract fun getWeekdayPattern(start: Long, end: Long): Flow<List<WeekdayCount>>

    @Query("UPDATE water_logs SET amountMl = :amountMl WHERE id = :id")
    abstract suspend fun updateAmountById(id: Long, amountMl: Int): Int

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE timestamp BETWEEN :start AND :end")
    protected abstract fun getTotalBetween(start: Long, end: Long): Flow<Int?>

    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
