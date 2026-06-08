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
        val waterGoalMl = waterStatus.recommendedAmountMl.coerceAtLeast(1)
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
        )

        val isMealRisk = mealStatus.isActionable
        val isWaterRisk = waterStatus.shortageLevel != WaterShortageLevel.NONE
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
            waterShortageLevel = waterStatus.shortageLevel,
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

        return HabitWidgetState(
            dominantCategory = dominantCategory,
            avatarResId = avatarResId(dominantCategory, gender),
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

    // ── 아바타 리소스 매핑 ────────────────────────────────────────────────────

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
        waterShortageLevel: WaterShortageLevel,
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
            statusLabel = if (isMealRisk) "식사 기록 필요" else "식사 OK",
            description = if (isMealRisk) "식사를 놓친 것 같아요. 기록을 남겨 건강한 습관을 유지해요."
                          else "오늘 식사 잘 챙겼어!",
            actionLabel = "🍽 식사 기록하기",
            riskLevel = if (isMealRisk) RiskLevel.WARNING else RiskLevel.NORMAL,
            isActionable = mealWidgetData.todayRecordCount < mealWidgetData.targetMealCount,
            mealData = mealWidgetData,
        ),
        HabitCardState(
            category = HabitCategory.WATER,
            iconResId = R.drawable.widget_dot_water,
            statusLabel = if (isWaterRisk) "물을 마셔야 해요" else "물 섭취가 좋아요",
            description = "${waterTotalMl}ml / ${waterGoalMl}ml",
            actionLabel = "💧 +1잔 (250ml)",
            riskLevel = when (waterShortageLevel) {
                WaterShortageLevel.SEVERE -> RiskLevel.DANGER
                WaterShortageLevel.MEDIUM,
                WaterShortageLevel.LIGHT -> RiskLevel.WARNING
                WaterShortageLevel.NONE -> RiskLevel.NORMAL
            },
            isActionable = isWaterRisk,
            waterData = WaterWidgetData(currentMl = waterTotalMl, goalMl = waterGoalMl),
        ),
        HabitCardState(
            category = HabitCategory.DIGITAL,
            iconResId = R.drawable.widget_dot_digital,
            statusLabel = when {
                digitalUsageMinutes > digitalThresholdMinutes * 3 / 2 -> "과사용"
                digitalUsageMinutes > digitalThresholdMinutes          -> "주의"
                else                                                   -> "양호"
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
            statusLabel = if (isStretchRisk) "스트레칭 부족" else "스트레칭 OK",
            description = "${stretchCount}회 / ${stretchGoalCount}회",
            actionLabel = "🧘 완료",
            riskLevel = if (isStretchRisk) RiskLevel.WARNING else RiskLevel.NORMAL,
            isActionable = isStretchRisk,
            stretchData = stretchWidgetData,
        ),
    )
}
