package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "digital_sessions",
    indices = [Index(value = ["startTime"])]
)
data class DigitalSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
)
