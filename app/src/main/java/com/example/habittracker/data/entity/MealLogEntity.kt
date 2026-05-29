// 경로: com/example/habittracker/data/entity/MealLogEntity.kt
package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.habittracker.data.model.MealType

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: MealType,
    val isLateNight: Boolean,
    val viaDeliveryApp: Boolean,
    val source: String,
    val mealDate: String = "",
    val recordedTime: String = "",
    val inputMethod: String = "",
    val triggerType: String = "",
)
