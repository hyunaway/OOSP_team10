package com.example.habittracker.widget

import android.content.Context
import com.example.habittracker.R
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.ui.avatar.AvatarGender
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

object WidgetResourceMapper {

    const val WATER_GOAL_ML = 2000
    const val STRETCH_GOAL_COUNT = 5

    val DEFAULT_PRIORITY_ORDER: List<HabitCategory> = listOf(
        HabitCategory.MEAL,
        HabitCategory.WATER,
        HabitCategory.DIGITAL,
        HabitCategory.STRETCH,
    )

    // ── 진입점: Context만으로 전체 상태를 빌드 ────────────────────────────────

    suspend fun buildWidgetState(context: Context): HabitWidgetState {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDependenciesEntryPoint::class.java,
        )

        val mealStatus = ep.getCurrentMealInterventionStatusUseCase()()
        val todayMealStatus = ep.getTodayMealStatusUseCase()().first()
        val waterStatus = ep.checkWaterInterventionNeededUseCase()()
        val digitalStatus = ep.getTodayDigitalStatusUseCase()().first()
        val stretchStatus = ep.getTodayStretchStatusUseCase()().first()
        val stretchInterventionStatus = ep.checkStretchInterventionNeededUseCase()()

        val prefManager = ep.userPreferenceManager()
        val priorityOrder = parsePriorityOrder(prefManager.categoryPriorityOrderFlow.first())
        val gender = AvatarGender.fromString(prefManager.avatarGenderFlow.first())
        val digitalThresholdMinutes = prefManager.digitalInterventionThresholdMinutesFlow
            .first()
            .coerceAtLeast(1)
        val waterGoalMl = waterStatus.effectiveGoalMl.coerceAtLeast(1)
        val waterRecommendedMl = waterStatus.recommendedAmountMl.coerceAtLeast(0)
        val stretchGoalCount = stretchInterventionStatus.personalizedGoalCount.coerceAtLeast(1)

        val todayMealRecordCount = listOf(
            todayMealStatus.breakfastLogged,
            todayMealStatus.lunchLogged,
            todayMealStatus.dinnerLogged,
        ).count { it }
        val mealWidgetData = MealWidgetData(
            lastMealTime = todayMealStatus.lastMealAt?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
            },
            todayRecordCount = todayMealRecordCount,
            breakfastLogged = todayMealStatus.breakfastLogged,
            lunchLogged = todayMealStatus.lunchLogged,
            dinnerLogged = todayMealStatus.dinnerLogged,
        )

        val isMealRisk = mealStatus.isActionable
        val isWaterRisk = waterStatus.isNeedWater
        val isDigitalRisk = digitalStatus.totalUsageMinutes > digitalThresholdMinutes
        val isStretchRisk = stretchInterventionStatus.isNeedStretch

        val dominantCategory = resolveDominantCategory(
            isMealRisk = isMealRisk,
            isWaterRisk = isWaterRisk,
            isDigitalRisk = isDigitalRisk,
            isStretchRisk = isStretchRisk,
            priorityOrder = priorityOrder,
        )

        val stretchTimerState = HabitStatusWidgetProvider.getTimerState(context)
        val stretchWidgetData = StretchWidgetData(
            lastStretchAtMillis = stretchStatus.lastStretchAt,
            totalCount          = stretchInterventionStatus.todayCount,
            personalizedGoalCount = stretchGoalCount,
            timerState          = stretchTimerState,
        )

        val categoryCards = buildCategoryCards(
            isMealRisk = isMealRisk,
            isWaterRisk = isWaterRisk,
            waterTotalMl = waterStatus.currentAmountMl,
            waterGoalMl = waterGoalMl,
            waterRecommendedMl = waterRecommendedMl,
            waterShortageLevel = waterStatus.shortageLevel,
            waterMessage = waterStatus.message,
            isDigitalRisk = isDigitalRisk,
            digitalUsageMinutes = digitalStatus.totalUsageMinutes,
            digitalThresholdMinutes = digitalThresholdMinutes,
            isStretchRisk = isStretchRisk,
            stretchCount = stretchInterventionStatus.todayCount,
            stretchGoalCount = stretchGoalCount,
            mealWidgetData = mealWidgetData,
            stretchWidgetData = stretchWidgetData,
        ).sortedBy { card ->
            priorityOrder.indexOf(card.category).takeIf { it >= 0 } ?: Int.MAX_VALUE
        }

        val actionLock = WidgetActionLock.getLockedAction(context)
        val finalAvatarResId = when {
            actionLock != null          -> actionAvatarResId(gender, actionLock)
            stretchTimerState.isRunning -> actionAvatarResId(gender, WidgetActionType.STRETCH)
            else                        -> avatarResId(dominantCategory, gender)
        }

        return HabitWidgetState(
            dominantCategory = dominantCategory,
            avatarResId = finalAvatarResId,
            categoryCards = categoryCards,
        )
    }

    // ── dominantCategory 결정 ─────────────────────────────────────────────────

    fun resolveDominantCategory(
        isMealRisk: Boolean,
        isWaterRisk: Boolean,
        isDigitalRisk: Boolean,
        isStretchRisk: Boolean,
        priorityOrder: List<HabitCategory> = DEFAULT_PRIORITY_ORDER,
    ): HabitCategory {
        val riskMap = mapOf(
            HabitCategory.MEAL to isMealRisk,
            HabitCategory.WATER to isWaterRisk,
            HabitCategory.DIGITAL to isDigitalRisk,
            HabitCategory.STRETCH to isStretchRisk,
        )
        return priorityOrder.firstOrNull { riskMap[it] == true } ?: HabitCategory.GOOD
    }

    // ── 우선순위 문자열 파싱 (DataStore: "MEAL,WATER,DIGITAL,STRETCH") ─────────

    fun parsePriorityOrder(raw: String): List<HabitCategory> {
        val parsed = raw.split(",")
            .mapNotNull { name -> runCatching { HabitCategory.valueOf(name.trim()) }.getOrNull() }
            .filter { it != HabitCategory.GOOD }
        return parsed.ifEmpty { DEFAULT_PRIORITY_ORDER }
    }

    // ── 액션 아바타 리소스 매핑 ───────────────────────────────────────────────

    fun actionAvatarResId(gender: AvatarGender, actionType: WidgetActionType): Int = when (gender) {
        AvatarGender.MALE -> when (actionType) {
            WidgetActionType.MEAL    -> R.drawable.widget_avatar_male_meal_done
            WidgetActionType.WATER   -> R.drawable.widget_avatar_male_water_done
            WidgetActionType.STRETCH -> R.drawable.widget_avatar_male_stretch_done
        }
        AvatarGender.FEMALE -> when (actionType) {
            WidgetActionType.MEAL    -> R.drawable.widget_avatar_female_meal_done
            WidgetActionType.WATER   -> R.drawable.widget_avatar_female_water_done
            WidgetActionType.STRETCH -> R.drawable.widget_avatar_female_stretch_done
        }
    }

    // ── 일반 아바타 리소스 매핑 ───────────────────────────────────────────────

    fun avatarResId(category: HabitCategory, gender: AvatarGender): Int = when (gender) {
        AvatarGender.MALE -> when (category) {
            HabitCategory.GOOD    -> R.drawable.widget_avatar_male_good
            HabitCategory.MEAL    -> R.drawable.widget_avatar_male_meal_lack
            HabitCategory.WATER   -> R.drawable.widget_avatar_male_water_lack
            HabitCategory.DIGITAL -> R.drawable.widget_avatar_male_digital_overuse
            HabitCategory.STRETCH -> R.drawable.widget_avatar_male_stretch_lack
        }
        AvatarGender.FEMALE -> when (category) {
            HabitCategory.GOOD    -> R.drawable.widget_avatar_female_good
            HabitCategory.MEAL    -> R.drawable.widget_avatar_female_meal_lack
            HabitCategory.WATER   -> R.drawable.widget_avatar_female_water_lack
            HabitCategory.DIGITAL -> R.drawable.widget_avatar_female_digital_overuse
            HabitCategory.STRETCH -> R.drawable.widget_avatar_female_stretch_lack
        }
    }

    // ── 카테고리 카드 빌드 ────────────────────────────────────────────────────

    private fun buildCategoryCards(
        isMealRisk: Boolean,
        isWaterRisk: Boolean,
        waterTotalMl: Int,
        waterGoalMl: Int,
        waterRecommendedMl: Int,
        waterShortageLevel: WaterShortageLevel,
        waterMessage: String,
        isDigitalRisk: Boolean,
        digitalUsageMinutes: Int,
        digitalThresholdMinutes: Int,
        isStretchRisk: Boolean,
        stretchCount: Int,
        stretchGoalCount: Int,
        mealWidgetData: MealWidgetData,
        stretchWidgetData: StretchWidgetData,
    ): List<HabitCardState> = listOf(
        HabitCardState(
            category = HabitCategory.MEAL,
            iconResId = R.drawable.widget_dot_meal,
            statusLabel = if (isMealRisk) "식사를 챙겨볼까요?" else "식사 흐름 좋아요",
            description = if (isMealRisk) "가볍게 기록해요"
                          else "오늘 흐름 좋아요",
            actionLabel = "🍽 식사 기록하기",
            riskLevel = if (isMealRisk) RiskLevel.WARNING else RiskLevel.NORMAL,
            isActionable = mealWidgetData.todayRecordCount < mealWidgetData.targetMealCount,
            mealData = mealWidgetData,
        ),
        HabitCardState(
            category = HabitCategory.WATER,
            iconResId = R.drawable.widget_dot_water,
            statusLabel = waterStatusLabel(
                isWaterRisk = isWaterRisk,
                waterTotalMl = waterTotalMl,
                waterGoalMl = waterGoalMl,
                waterMessage = waterMessage,
            ),
            description = waterDescription(
                isWaterRisk = isWaterRisk,
                waterMessage = waterMessage,
                waterTotalMl = waterTotalMl,
                waterGoalMl = waterGoalMl,
                waterRecommendedMl = waterRecommendedMl,
            ),
            actionLabel = "💧 +1잔 (250ml)",
            riskLevel = if (isWaterRisk) {
                when (waterShortageLevel) {
                    WaterShortageLevel.SEVERE -> RiskLevel.DANGER
                    WaterShortageLevel.MEDIUM,
                    WaterShortageLevel.LIGHT -> RiskLevel.WARNING
                    WaterShortageLevel.NONE -> RiskLevel.NORMAL
                }
            } else {
                RiskLevel.NORMAL
            },
            isActionable = isWaterRisk,
            waterData = WaterWidgetData(
                currentMl = waterTotalMl,
                goalMl = waterGoalMl,
                recommendedMl = waterRecommendedMl,
            ),
        ),
        HabitCardState(
            category = HabitCategory.DIGITAL,
            iconResId = R.drawable.widget_dot_digital,
            statusLabel = when {
                digitalUsageMinutes > digitalThresholdMinutes * 3 / 2 -> "사용 시간이 늘었어요"
                digitalUsageMinutes > digitalThresholdMinutes          -> "잠깐 쉬어볼까요?"
                else                                                   -> "사용 습관 좋아요"
            },
            description = "${digitalUsageMinutes}분 사용",
            actionLabel = "📱 사용 기록 보기",
            riskLevel = when {
                digitalUsageMinutes > digitalThresholdMinutes * 3 / 2 -> RiskLevel.DANGER
                digitalUsageMinutes > digitalThresholdMinutes          -> RiskLevel.WARNING
                else                                                   -> RiskLevel.NORMAL
            },
            isActionable = false,
            digitalData = DigitalWidgetData(
                usageMinutes = digitalUsageMinutes,
                goalMinutes  = digitalThresholdMinutes,
            ),
        ),
        HabitCardState(
            category = HabitCategory.STRETCH,
            iconResId = R.drawable.widget_dot_stretch,
            statusLabel = if (isStretchRisk) "몸을 풀어볼까요?" else "몸이 가벼워요",
            description = "${stretchCount}회 / ${stretchGoalCount}회",
            actionLabel = "🧘 완료",
            riskLevel = if (isStretchRisk) RiskLevel.WARNING else RiskLevel.NORMAL,
            isActionable = isStretchRisk,
            stretchData = stretchWidgetData,
        ),
    )

    private fun waterStatusLabel(
        isWaterRisk: Boolean,
        waterTotalMl: Int,
        waterGoalMl: Int,
        waterMessage: String,
    ): String = when {
        waterTotalMl >= waterGoalMl -> "오늘 물 목표 달성"
        waterMessage.isRecentWaterHoldMessage() -> "방금 물을 마셨어요"
        isWaterRisk -> "물이 조금 부족해요"
        else -> "수분 섭취 좋아요"
    }

    private fun waterDescription(
        isWaterRisk: Boolean,
        waterMessage: String,
        waterTotalMl: Int,
        waterGoalMl: Int,
        waterRecommendedMl: Int,
    ): String = when {
        waterMessage.isRecentWaterHoldMessage() -> "잠시 후 다시 확인해요"
        isWaterRisk -> "한 잔만 더 마셔볼까요?"
        else -> "${waterTotalMl}/${waterGoalMl}ml · 권장 ${waterRecommendedMl}ml"
    }

    private fun String.isRecentWaterHoldMessage(): Boolean =
        contains("방금 물을 마셨어요")
}
