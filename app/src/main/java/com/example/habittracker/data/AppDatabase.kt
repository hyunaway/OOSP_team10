// 경로: com/example/habittracker/data/AppDatabase.kt
package com.example.habittracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.habittracker.data.entity.DigitalInterventionLogEntity
import com.example.habittracker.data.entity.DigitalSessionEntity
import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.entity.NotificationActionLogEntity
import com.example.habittracker.data.entity.StretchLogEntity
import com.example.habittracker.data.entity.WaterLogEntity
import com.example.habittracker.data.local.room.dao.DigitalInterventionLogDao
import com.example.habittracker.data.local.room.dao.DigitalSessionDao
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.local.room.dao.NotificationActionLogDao
import com.example.habittracker.data.local.room.dao.StretchDao
import com.example.habittracker.data.local.room.dao.WaterDao

@Database(
    entities = [
        WaterLogEntity::class,
        MealLogEntity::class,
        DigitalSessionEntity::class,
        DigitalInterventionLogEntity::class,
        StretchLogEntity::class,
        NotificationActionLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun waterDao(): WaterDao
    abstract fun mealDao(): MealDao
    abstract fun digitalSessionDao(): DigitalSessionDao
    abstract fun digitalInterventionLogDao(): DigitalInterventionLogDao
    abstract fun stretchDao(): StretchDao
    abstract fun notificationActionLogDao(): NotificationActionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
