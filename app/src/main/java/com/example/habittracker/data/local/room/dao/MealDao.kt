// 경로: com/example/habittracker/data/local/room/dao/MealDao.kt
package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
abstract class MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: MealLogEntity): Long

    @Update
    abstract suspend fun update(entity: MealLogEntity): Int

    @Query("DELETE FROM meal_logs WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM meal_logs WHERE mealDate = :mealDate")
    abstract suspend fun getLogsByMealDate(mealDate: String): List<MealLogEntity>

    @Query("SELECT * FROM meal_logs WHERE mealDate = :mealDate ORDER BY timestamp DESC")
    abstract fun observeLogsByMealDate(mealDate: String): Flow<List<MealLogEntity>>

    @Query("""
        SELECT * FROM meal_logs
        WHERE mealDate = :todayDate
           OR (
                mealDate = :previousDate
                AND (isLateNight = 1 OR type = 'LATE_NIGHT')
           )
        ORDER BY timestamp DESC
    """)
    abstract fun observeLogsForMealScreen(
        todayDate: String,
        previousDate: String,
    ): Flow<List<MealLogEntity>>

    @Query("SELECT COUNT(*) FROM meal_logs WHERE isLateNight = 1 AND timestamp BETWEEN :start AND :end")
    abstract fun getLateNightCount(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM meal_logs WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    abstract fun getLogsBetween(start: Long, end: Long): Flow<List<MealLogEntity>>

    @Query("UPDATE meal_logs SET type = :type WHERE id = :id")
    abstract suspend fun updateTypeById(id: Long, type: MealType): Int

    @Query("SELECT type, COUNT(*) AS count FROM meal_logs WHERE timestamp BETWEEN :start AND :end GROUP BY type")
    abstract fun getSkippedMealPattern(start: Long, end: Long): Flow<List<MealTypeCount>>

    @Query("""
        SELECT CAST(strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM meal_logs
        WHERE type = :type AND timestamp BETWEEN :start AND :end
        GROUP BY hour
        ORDER BY hour
    """)
    abstract fun getMealHourByType(type: String, start: Long, end: Long): Flow<List<HourlyCount>>

}
