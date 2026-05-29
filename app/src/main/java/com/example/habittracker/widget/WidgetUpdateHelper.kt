// 경로: com/example/habittracker/widget/WidgetUpdateHelper.kt
package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.habittracker.R
import com.example.habittracker.data.AppDatabase
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.usecase.digital.GetTodayDigitalStatusUseCase
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
import java.time.LocalDate

object WidgetUpdateHelper {

    private const val DIGITAL_OVERUSE_THRESHOLD_MINUTES = 120
    private const val WATER_GOAL_ML = 2000
    private const val STRETCH_GOAL_COUNT = 5

    suspend fun buildWidgetState(context: Context): HabitWidgetState {
        val appContext = context.applicationContext
        val ep = EntryPointAccessors.fromApplication(
            appContext,
            WidgetDependenciesEntryPoint::class.java,
        )

        val waterStatus = ep.checkWaterInterventionNeededUseCase()()
        val digitalStatus = ep.getTodayDigitalStatusUseCase()().first()
        val genderString = ep.userPreferenceManager().avatarGenderFlow.first()
        val stretchCount = AppDatabase.getInstance(appContext)
            .stretchDao()
            .getTodayStretchCount(LocalDate.now().toString())

        val displayWaterLevel = if (waterStatus.isNeedWater) {
            waterStatus.shortageLevel
        } else {
            WaterShortageLevel.NONE
        }
        val isDigitalOveruse = digitalStatus.totalUsageMinutes > DIGITAL_OVERUSE_THRESHOLD_MINUTES

        // TODO: 식사 부족 판정 — 다른 조원의 Meal 로직 병합 후 MealStatus.LACK 으로 교체
        val mealStatus = MealStatus.NORMAL
        // TODO: 스트레칭 부족 판정 — 다른 조원의 Stretch 로직 병합 후 StretchStatus.LACK 으로 교체
        val stretchStatus = StretchStatus.NORMAL

        val gender = when (AvatarGender.fromString(genderString)) {
            AvatarGender.MALE -> WidgetGender.MALE
            AvatarGender.FEMALE -> WidgetGender.FEMALE
        }

        val dominantCategory = WidgetResourceMapper.resolveDominantCategory(
            mealStatus = mealStatus,
            waterShortageLevel = displayWaterLevel,
            isDigitalOveruse = isDigitalOveruse,
            stretchStatus = stretchStatus,
        )

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
        )
    }

    // HabitStatusWidgetProvider 호환을 위해 유지
    suspend fun loadWidgetState(context: Context): HabitWidgetState = buildWidgetState(context)

    fun bindStateToRemoteViews(context: Context, views: RemoteViews, state: HabitWidgetState) {
        val avatarRes = WidgetResourceMapper.avatarResId(state.dominantCategory, state.gender)
        views.setImageViewResource(R.id.widget_avatar_image, avatarRes)
        views.setTextViewText(R.id.widget_speech_text, WidgetResourceMapper.speechBubbleText(state.dominantCategory))
        views.setTextViewText(R.id.widget_status_label, categoryLabel(state.dominantCategory))

        // 보조 상태 1: 물
        views.setViewVisibility(R.id.widget_sub_status_1_icon, View.GONE)
        views.setViewVisibility(R.id.widget_sub_status_1_text, View.VISIBLE)
        views.setTextViewText(R.id.widget_sub_status_1_text, "💧 ${state.waterTotalMl}ml")

        // 보조 상태 2: 디지털
        views.setViewVisibility(R.id.widget_sub_status_2_icon, View.GONE)
        views.setViewVisibility(R.id.widget_sub_status_2_text, View.VISIBLE)
        views.setTextViewText(R.id.widget_sub_status_2_text, "📱 ${state.digitalUsageMinutes}분")

        // 보조 상태 3: 스트레칭
        views.setViewVisibility(R.id.widget_sub_status_3_icon, View.GONE)
        views.setViewVisibility(R.id.widget_sub_status_3_text, View.VISIBLE)
        views.setTextViewText(R.id.widget_sub_status_3_text, "🧘 ${state.stretchCount}회")
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
        HabitStatusWidgetProvider.attachPendingIntents(context, views)
        manager.updateAppWidget(widgetId, views)
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
    fun userPreferenceManager(): UserPreferenceManager
}
