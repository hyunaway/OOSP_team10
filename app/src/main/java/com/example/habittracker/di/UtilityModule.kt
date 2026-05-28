// 경로: com/example/habittracker/di/UtilityModule.kt
package com.example.habittracker.di

import android.content.Context
import com.example.habittracker.data.local.room.dao.NotificationActionLogDao
import com.example.habittracker.util.DeepLinkFactory
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.util.UsageStatsHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    @Provides
    @Singleton
    fun provideDeepLinkFactory(): DeepLinkFactory = DeepLinkFactory()

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        notificationActionLogDao: NotificationActionLogDao,
        deepLinkFactory: DeepLinkFactory,
    ): NotificationHelper = NotificationHelper(context, notificationActionLogDao, deepLinkFactory)

    @Provides
    @Singleton
    fun provideUsageStatsHelper(
        @ApplicationContext context: Context,
    ): UsageStatsHelper = UsageStatsHelper(context)

    // MessageToneSelector is auto-provided by Hilt via @Singleton @Inject constructor().
}
