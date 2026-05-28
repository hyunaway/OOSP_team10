// 경로: com/example/habittracker/di/UseCaseModule.kt
package com.example.habittracker.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 모든 UseCase는 @Singleton + @Inject constructor로 선언되어 있으므로
 * Hilt가 자동으로 바인딩을 생성한다. 명시적 @Provides/@Binds가 필요 없다.
 *
 * 자동 제공 확인 목록:
 *  - GetDashboardStateUseCase (WaterRepository, MealRepository, StretchRepository, DigitalRepository 주입)
 *  - UpdatePersonalizationParamsUseCase (Analyze*PatternUseCase × 4, UserPreferenceManager 주입)
 *  - GetWeeklyReportUseCase / GetMonthlyReportUseCase (Repository + Analyze*UseCase 주입)
 *  - 도메인별 Water / Meal / Digital / Stretch UseCases
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule
