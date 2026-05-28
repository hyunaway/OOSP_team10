// 경로: com/example/habittracker/di/RepositoryModule.kt
package com.example.habittracker.di

import com.example.habittracker.data.repository.DigitalRepositoryImpl
import com.example.habittracker.data.repository.MealRepositoryImpl
import com.example.habittracker.data.repository.StretchRepositoryImpl
import com.example.habittracker.data.repository.WaterRepositoryImpl
import com.example.habittracker.domain.repository.DigitalRepository
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.repository.WaterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWaterRepository(impl: WaterRepositoryImpl): WaterRepository

    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    @Singleton
    abstract fun bindDigitalRepository(impl: DigitalRepositoryImpl): DigitalRepository

    @Binds
    @Singleton
    abstract fun bindStretchRepository(impl: StretchRepositoryImpl): StretchRepository
}
