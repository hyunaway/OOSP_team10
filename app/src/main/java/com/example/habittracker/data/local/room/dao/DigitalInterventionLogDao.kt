// 경로: com/example/habittracker/data/local/room/dao/DigitalInterventionLogDao.kt
package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.DigitalInterventionLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
abstract class DigitalInterventionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: DigitalInterventionLogEntity): Long

    @Query("UPDATE digital_interventions SET reacted = :reacted, actionType = :actionType WHERE id = :id")
    abstract suspend fun updateReaction(id: Long, reacted: Boolean, actionType: String): Int

    fun getTodayInterventions(): Flow<List<DigitalInterventionLogEntity>> =
        getInterventionsBetween(todayStart(), Long.MAX_VALUE)

    @Query("""
        SELECT messageTone,
               CAST(SUM(CASE WHEN reacted = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) AS rate
        FROM digital_interventions
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY messageTone
    """)
    abstract fun getToneReactionRate(start: Long, end: Long): Flow<List<ToneReactionRate>>

    @Query("SELECT * FROM digital_interventions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    protected abstract fun getInterventionsBetween(start: Long, end: Long): Flow<List<DigitalInterventionLogEntity>>

    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
