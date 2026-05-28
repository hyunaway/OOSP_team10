// 경로: com/example/habittracker/data/local/room/dao/NotificationActionLogDao.kt
package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.NotificationActionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationActionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationActionLogEntity): Long

    @Query("UPDATE notification_action_logs SET clickedAt = :clickedAt, actionType = :actionType WHERE id = :id")
    suspend fun updateClick(id: Long, clickedAt: Long, actionType: String): Int

    @Query("UPDATE notification_action_logs SET dismissed = 1 WHERE id = :id")
    suspend fun updateDismiss(id: Long): Int

    @Query("""
        SELECT category,
               CAST(SUM(CASE WHEN clickedAt IS NOT NULL THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) AS rate
        FROM notification_action_logs
        WHERE shownAt BETWEEN :start AND :end
        GROUP BY category
    """)
    fun getClickRateByCategory(start: Long, end: Long): Flow<List<CategoryClickRate>>

    @Query("""
        SELECT CAST(strftime('%H', datetime(shownAt/1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
               CAST(SUM(CASE WHEN clickedAt IS NULL AND dismissed = 0 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) AS ignoreRate
        FROM notification_action_logs
        WHERE category = :category AND shownAt BETWEEN :start AND :end
        GROUP BY hour
        ORDER BY hour
    """)
    fun getHourlyIgnoreRate(category: String, start: Long, end: Long): Flow<List<HourlyIgnoreRate>>
}
