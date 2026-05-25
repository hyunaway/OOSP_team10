// 경로: com/example/habittracker/data/entity/DigitalInterventionLogEntity.kt
package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digital_interventions")
data class DigitalInterventionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val triggerDurationMinutes: Int,
    val timestamp: Long,
    val reacted: Boolean = false,
    val messageTone: String,
    val actionType: String = "",
)
