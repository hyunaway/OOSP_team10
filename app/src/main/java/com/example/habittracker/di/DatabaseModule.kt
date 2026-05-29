// 경로: com/example/habittracker/di/DatabaseModule.kt
package com.example.habittracker.di

import android.content.Context
import com.example.habittracker.data.AppDatabase
import com.example.habittracker.data.local.room.dao.DigitalInterventionLogDao
import com.example.habittracker.data.local.room.dao.DigitalSessionDao
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.local.room.dao.NotificationActionLogDao
import com.example.habittracker.data.local.room.dao.StretchDao
import com.example.habittracker.data.local.room.dao.WaterDao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideWaterDao(db: AppDatabase): WaterDao = db.waterDao()

    @Provides
    @Singleton
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides
    @Singleton
    fun provideDigitalSessionDao(db: AppDatabase): DigitalSessionDao = db.digitalSessionDao()

    @Provides
    @Singleton
    fun provideDigitalInterventionLogDao(db: AppDatabase): DigitalInterventionLogDao =
        db.digitalInterventionLogDao()

    @Provides
    @Singleton
    fun provideStretchDao(db: AppDatabase): StretchDao = db.stretchDao()

    @Provides
    @Singleton
    fun provideNotificationActionLogDao(db: AppDatabase): NotificationActionLogDao =
        db.notificationActionLogDao()


}
