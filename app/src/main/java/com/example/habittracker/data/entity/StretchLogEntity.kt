// 경로: com/example/habittracker/data/entity/StretchLogEntity.kt
package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.habittracker.data.model.BodyPartType

@Entity(tableName = "stretch_logs")
data class StretchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val bodyPart: BodyPartType,
    val durationSeconds: Int,
)
