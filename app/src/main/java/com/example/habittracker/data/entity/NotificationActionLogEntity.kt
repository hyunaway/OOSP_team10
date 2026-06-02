package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_action_logs",
    indices = [Index(value = ["shownAt"])]
)
data class NotificationActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val notificationId: Int,
    val shownAt: Long,
    val clickedAt: Long?,
    val actionType: String?,
    val dismissed: Boolean = false,
)
