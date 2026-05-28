// 경로: com/example/habittracker/data/entity/NotificationActionLogEntity.kt
package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_action_logs")
data class NotificationActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val notificationId: Int,
    val shownAt: Long,
    val clickedAt: Long?,
    val actionType: String?,
    val dismissed: Boolean = false,
)
