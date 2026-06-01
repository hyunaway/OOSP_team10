// 경로: com/example/habittracker/widget/WidgetUpdateHelper.kt
package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.habittracker.R
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.usecase.activity.MarkUserActiveUseCase
import com.example.habittracker.domain.usecase.digital.GetTodayDigitalStatusUseCase
import com.example.habittracker.domain.usecase.meal.GetTodayMealStatusUseCase
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import com.example.habittracker.ui.avatar.AvatarGender
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    private const val DIGITAL_OVERUSE_THRESHOLD_MINUTES = 120
    private val WATER_GOAL_ML get() = WidgetResourceMapper.WATER_GOAL_ML
    private val STRETCH_GOAL_COUNT get() = WidgetResourceMapper.STRETCH_GOAL_COUNT

    suspend fun buildWidgetState(context: Context): HabitWidgetState {
        val appContext = context.applicationContext
        val ep = EntryPointAccessors.fromApplication(
            appContext,
            WidgetDependenciesEntryPoint::class.java,
        )

        val waterStatus = ep.checkWaterInterventionNeededUseCase()()
        val digitalStatus = ep.getTodayDigitalStatusUseCase()().first()
        val mealTodayStatus = ep.getTodayMealStatusUseCase()().first()
        val stretchTodayStatus = ep.getTodayStretchStatusUseCase()().first()
        val prefManager = ep.userPreferenceManager()
        val genderString = prefManager.avatarGenderFlow.first()
        val priorityOrderRaw = prefManager.categoryPriorityOrderFlow.first()

        // 앱(AvatarStateResolver)과 동일한 기준: 일일 목표의 50% 미만이면 물 부족
        val displayWaterLevel = if (waterStatus.currentAmountMl < WATER_GOAL_ML / 2) {
            WaterShortageLevel.LIGHT
        } else {
            WaterShortageLevel.NONE
        }
        val isDigitalOveruse = digitalStatus.totalUsageMinutes > DIGITAL_OVERUSE_THRESHOLD_MINUTES

        val loggedMealCount = listOf(
            mealTodayStatus.breakfastLogged,
            mealTodayStatus.lunchLogged,
            mealTodayStatus.dinnerLogged,
        ).count { it }
        val mealStatus = if (loggedMealCount < 2) MealStatus.LACK else MealStatus.NORMAL

        val stretchCount = stretchTodayStatus.totalCount
        val stretchStatus = if (stretchCount < STRETCH_GOAL_COUNT) StretchStatus.LACK else StretchStatus.NORMAL

        val gender = when (AvatarGender.fromString(genderString)) {
            AvatarGender.MALE -> WidgetGender.MALE
            AvatarGender.FEMALE -> WidgetGender.FEMALE
        }

        val priorityOrder = WidgetResourceMapper.parsePriorityOrder(priorityOrderRaw)
        val dominantCategory = WidgetResourceMapper.resolveDominantCategory(
            mealStatus = mealStatus,
            waterShortageLevel = displayWaterLevel,
            isDigitalOveruse = isDigitalOveruse,
            stretchStatus = stretchStatus,
            priorityOrder = priorityOrder,
        )

        // 카드 순서를 사용자 우선순위 설정에 맞게 정렬
        val categoryStates = WidgetResourceMapper.buildCategoryStates(
            mealStatus = mealStatus,
            waterShortageLevel = displayWaterLevel,
            waterTotalMl = waterStatus.currentAmountMl,
            isDigitalOveruse = isDigitalOveruse,
            digitalUsageMinutes = digitalStatus.totalUsageMinutes,
            stretchStatus = stretchStatus,
            stretchCount = stretchCount,
        ).sortedBy { s -> priorityOrder.indexOf(s.category).let { if (it < 0) Int.MAX_VALUE else it } }

        val speechBubble = WidgetResourceMapper.speechBubbleText(dominantCategory)
        val waterScore = minOf(waterStatus.currentAmountMl / WATER_GOAL_ML.toFloat(), 1f) * 50f
        val stretchScore = minOf(stretchCount / STRETCH_GOAL_COUNT.toFloat(), 1f) * 50f
        val totalScore = (waterScore + stretchScore).toInt()

        val emoji = when {
            displayWaterLevel == WaterShortageLevel.SEVERE -> ":("
            displayWaterLevel == WaterShortageLevel.MEDIUM -> ":|"
            totalScore >= 80 -> ":)"
            totalScore >= 50 -> ":|"
            else -> ":("
        }

        return HabitWidgetState(
            waterTotalMl = waterStatus.currentAmountMl,
            isNeedWater = waterStatus.isNeedWater,
            waterShortageLevel = displayWaterLevel,
            waterStatusText = waterStatusText(displayWaterLevel),
            speechBubbleMessage = speechBubble,
            abnormalStatusText = waterAbnormalStatusText(displayWaterLevel),
            abnormalStatusType = if (displayWaterLevel == WaterShortageLevel.NONE) {
                WidgetAbnormalStatusType.NONE
            } else {
                WidgetAbnormalStatusType.WATER
            },
            abnormalStatusColor = waterStatusColor(displayWaterLevel),
            widgetMessage = speechBubble,
            stretchCount = stretchCount,
            avatarHealthScore = totalScore,
            avatarEmoji = emoji,
            dominantCategory = dominantCategory,
            gender = gender,
            mealStatus = mealStatus,
            stretchStatus = stretchStatus,
            digitalUsageMinutes = digitalStatus.totalUsageMinutes,
            categoryStates = categoryStates,
        )
    }

    // HabitStatusWidgetProvider 호환을 위해 유지
    suspend fun loadWidgetState(context: Context): HabitWidgetState = buildWidgetState(context)

    fun bindStateToRemoteViews(context: Context, views: RemoteViews, state: HabitWidgetState) {
        // ── 왼쪽: 대표 아바타 ────────────────────────────────────────────────────
        val avatarRes = WidgetResourceMapper.avatarResId(state.dominantCategory, state.gender)
        views.setImageViewResource(R.id.widget_avatar_image, avatarRes)

        // ── 왼쪽: 복합 위험 상태 도트 아이콘 ─────────────────────────────────────
        val isMealRisk = state.categoryStates.firstOrNull { it.category == WidgetHabitCategory.MEAL }?.isRisk
            ?: (state.mealStatus == MealStatus.LACK)
        val isWaterRisk = state.categoryStates.firstOrNull { it.category == WidgetHabitCategory.WATER }?.isRisk
            ?: (state.waterShortageLevel != WaterShortageLevel.NONE)
        val isDigitalRisk = state.categoryStates.firstOrNull { it.category == WidgetHabitCategory.DIGITAL }?.isRisk
            ?: (state.digitalUsageMinutes > DIGITAL_OVERUSE_THRESHOLD_MINUTES)
        val isStretchRisk = state.categoryStates.firstOrNull { it.category == WidgetHabitCategory.STRETCH }?.isRisk
            ?: (state.stretchStatus == StretchStatus.LACK)

        views.setImageViewResource(R.id.widget_status_icon_meal,
            if (isMealRisk) R.drawable.widget_dot_meal else R.drawable.widget_dot_inactive)
        views.setImageViewResource(R.id.widget_status_icon_water,
            if (isWaterRisk) R.drawable.widget_dot_water else R.drawable.widget_dot_inactive)
        views.setImageViewResource(R.id.widget_status_icon_digital,
            if (isDigitalRisk) R.drawable.widget_dot_digital else R.drawable.widget_dot_inactive)
        views.setImageViewResource(R.id.widget_status_icon_stretch,
            if (isStretchRisk) R.drawable.widget_dot_stretch else R.drawable.widget_dot_inactive)

        // ── 오른쪽: 단일 카테고리 카드 ───────────────────────────────────────────
        val cardState = state.categoryStates.firstOrNull { it.category == state.dominantCategory }
        val categoryName = cardState?.title ?: categoryLabel(state.dominantCategory)
        val message = cardState?.message ?: WidgetResourceMapper.speechBubbleText(state.dominantCategory)
        val description = cardState?.description ?: ""
        val actionText = quickActionButtonText(state.dominantCategory)

        views.setTextViewText(R.id.widget_card_category_name, categoryName)
        views.setTextViewText(R.id.widget_card_message, message)
        views.setTextViewText(R.id.widget_card_description, description)
        views.setTextViewText(R.id.widget_card_quick_action, actionText)

        // 빠른 기록 버튼 PendingIntent (카테고리별 동적 설정)
        views.setOnClickPendingIntent(
            R.id.widget_card_quick_action,
            HabitStatusWidgetProvider.quickActionPendingIntent(context, state.dominantCategory),
        )

        // ── 레거시 호환 뷰 설정 (hidden, 기존 코드 호환) ─────────────────────────
        views.setTextViewText(R.id.widget_speech_text, message)
        views.setTextViewText(R.id.widget_status_label, categoryLabel(state.dominantCategory))
    }

    fun buildRemoteViews(context: Context, state: HabitWidgetState): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_habit_status).apply {
            bindStateToRemoteViews(context, this, state)
            // 레거시 뷰 바인딩 (HabitStatusWidgetProvider 호환)
            setTextViewText(R.id.avatarText, state.avatarEmoji)
            setTextViewText(R.id.waterText, "물 ${state.waterTotalMl}ml")
            setTextViewText(R.id.stretchText, "스트레칭: ${state.stretchCount}회")
            if (state.abnormalStatusType == WidgetAbnormalStatusType.NONE) {
                setViewVisibility(R.id.abnormalStatusText, View.GONE)
                setTextViewText(R.id.abnormalStatusText, "")
                setViewVisibility(R.id.speechText, View.GONE)
                setTextViewText(R.id.speechText, "")
            } else {
                setViewVisibility(R.id.abnormalStatusText, View.VISIBLE)
                setTextViewText(R.id.abnormalStatusText, "상태 이상: ${state.abnormalStatusText}")
                setTextColor(R.id.abnormalStatusText, state.abnormalStatusColor)
                setViewVisibility(R.id.speechText, View.VISIBLE)
                setTextViewText(R.id.speechText, state.widgetMessage)
                setTextColor(R.id.speechText, state.abnormalStatusColor)
            }
        }
    }

    suspend fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, HabitStatusWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return
        val state = buildWidgetState(context)
        ids.forEach { id -> updateWidget(context, manager, id, state) }
    }

    fun updateAllWidgetsSync(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateAllWidgets(context)
        }
    }

    fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        state: HabitWidgetState,
    ) {
        val views = buildRemoteViews(context, state)

        // StackView 어댑터 설정 — widgetId 별로 고유 URI를 사용해 캐싱 충돌 방지
        val serviceIntent = android.content.Intent(context, HabitWidgetRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = android.net.Uri.parse(toUri(android.content.Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_stack_view, serviceIntent)
        views.setPendingIntentTemplate(
            R.id.widget_stack_view,
            HabitStatusWidgetProvider.cardClickTemplateIntent(context),
        )

        HabitStatusWidgetProvider.attachPendingIntents(context, views)
        manager.updateAppWidget(widgetId, views)
        // StackView 데이터 갱신 요청
        manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_stack_view)
    }

    private fun quickActionButtonText(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.WATER -> "💧 +1잔 (250ml)"
        WidgetHabitCategory.STRETCH -> "🧘 완료"
        WidgetHabitCategory.MEAL -> "🍽 식사 기록하기"
        WidgetHabitCategory.DIGITAL -> "📱 사용 기록 보기"
        WidgetHabitCategory.GOOD -> "기록하러 가기"
    }

    private fun categoryLabel(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.MEAL -> "식사 부족"
        WidgetHabitCategory.WATER -> "물 부족"
        WidgetHabitCategory.DIGITAL -> "디지털 과사용"
        WidgetHabitCategory.STRETCH -> "스트레칭 부족"
        WidgetHabitCategory.GOOD -> "양호"
    }

    private fun waterStatusText(level: WaterShortageLevel): String =
        when (level) {
            WaterShortageLevel.NONE -> "정상"
            WaterShortageLevel.LIGHT -> "물 부족"
            WaterShortageLevel.MEDIUM -> "탈수 주의"
            WaterShortageLevel.SEVERE -> "탈수"
        }

    private fun waterAbnormalStatusText(level: WaterShortageLevel): String =
        when (level) {
            WaterShortageLevel.NONE -> ""
            WaterShortageLevel.LIGHT -> "물 부족"
            WaterShortageLevel.MEDIUM -> "탈수 주의"
            WaterShortageLevel.SEVERE -> "탈수"
        }

    private fun waterStatusColor(level: WaterShortageLevel): Int =
        when (level) {
            WaterShortageLevel.NONE -> Color.TRANSPARENT
            WaterShortageLevel.LIGHT -> Color.rgb(30, 136, 229)
            WaterShortageLevel.MEDIUM -> Color.rgb(21, 101, 192)
            WaterShortageLevel.SEVERE -> Color.rgb(13, 71, 161)
        }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependenciesEntryPoint {
    fun checkWaterInterventionNeededUseCase(): CheckWaterInterventionNeededUseCase
    fun getTodayDigitalStatusUseCase(): GetTodayDigitalStatusUseCase
    fun getTodayMealStatusUseCase(): GetTodayMealStatusUseCase
    fun getTodayStretchStatusUseCase(): GetTodayStretchStatusUseCase
    fun stretchRepository(): StretchRepository
    fun userPreferenceManager(): UserPreferenceManager
    fun markUserActiveUseCase(): MarkUserActiveUseCase
}
