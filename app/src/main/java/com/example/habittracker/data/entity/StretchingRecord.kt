package com.example.habittracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stretching_records",
    indices = [Index(value = ["date"])]
)
data class StretchingRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    @ColumnInfo(name = "time_slot") val timeSlot: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
