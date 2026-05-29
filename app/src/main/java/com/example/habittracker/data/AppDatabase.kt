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
import com.example.habittracker.data.entity.StretchingRecord
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
        StretchingRecord::class,
        NotificationActionLogEntity::class,
    ],
    version = 4,
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
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stretching_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` TEXT NOT NULL, 
                        `time_slot` TEXT NOT NULL, 
                        `body_parts` TEXT NOT NULL, 
                        `created_at` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO stretching_records (date, time_slot, body_parts, created_at)
                    SELECT 
                        COALESCE(NULLIF(mealDate, ''), date(timestamp/1000, 'unixepoch', 'localtime')) AS date,
                        CASE 
                            WHEN stretchSlot = 'AM' THEN '아침'
                            WHEN stretchSlot = 'PM' THEN '점심'
                            WHEN stretchSlot = 'EVE' THEN '저녁'
                            ELSE '기타'
                        END AS time_slot,
                        CASE 
                            WHEN bodyPart = 'NECK' THEN '["목"]'
                            WHEN bodyPart = 'SHOULDER' THEN '["어깨"]'
                            WHEN bodyPart = 'BACK' THEN '["허리"]'
                            ELSE '["전신"]'
                        END AS body_parts,
                        datetime(timestamp/1000, 'unixepoch', 'localtime') AS created_at
                    FROM stretch_logs
                """.trimIndent())

                db.execSQL("DROP TABLE IF EXISTS stretch_logs")
            }
        }
    }
}
