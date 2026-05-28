package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.habittracker.R
import com.example.habittracker.data.AppDatabase
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

object WidgetUpdateHelper {

    suspend fun loadWidgetState(context: Context): HabitWidgetState {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        val waterStatus = EntryPointAccessors.fromApplication(
            appContext,
            WidgetDependenciesEntryPoint::class.java,
        ).checkWaterInterventionNeededUseCase()()

        val waterTotalMl = waterStatus.currentAmountMl
        val stretchCount = db.stretchDao().getTodayLogs().first().size
        val displayWaterLevel = if (waterStatus.isNeedWater) {
            waterStatus.shortageLevel
        } else {
            WaterShortageLevel.NONE
        }
        val abnormalStatusText = waterAbnormalStatusText(displayWaterLevel)
        val widgetMessage = waterWidgetMessage(displayWaterLevel)

        val waterScore = minOf(waterTotalMl / WATER_GOAL_ML.toFloat(), 1f) * 50f
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
            waterTotalMl = waterTotalMl,
            isNeedWater = waterStatus.isNeedWater,
            waterShortageLevel = displayWaterLevel,
            waterStatusText = waterStatusText(displayWaterLevel),
            speechBubbleMessage = widgetMessage,
            abnormalStatusText = abnormalStatusText,
            abnormalStatusType = if (displayWaterLevel == WaterShortageLevel.NONE) {
                WidgetAbnormalStatusType.NONE
            } else {
                WidgetAbnormalStatusType.WATER
            },
            abnormalStatusColor = waterStatusColor(displayWaterLevel),
            widgetMessage = widgetMessage,
            stretchCount = stretchCount,
            avatarHealthScore = totalScore,
            avatarEmoji = emoji,
        )
    }

    fun buildRemoteViews(context: Context, state: HabitWidgetState): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_habit_status).apply {
            setTextViewText(R.id.avatarText, state.avatarEmoji)
            setTextViewText(R.id.waterText, "물 ${state.waterTotalMl}ml")
            if (state.abnormalStatusType == WidgetAbnormalStatusType.NONE) {
                setViewVisibility(R.id.abnormalStatusText, View.GONE)
                setViewVisibility(R.id.speechText, View.GONE)
                setTextViewText(R.id.abnormalStatusText, "")
                setTextViewText(R.id.speechText, "")
            } else {
                setViewVisibility(R.id.abnormalStatusText, View.VISIBLE)
                setTextViewText(R.id.abnormalStatusText, "상태 이상: ${state.abnormalStatusText}")
                setTextColor(R.id.abnormalStatusText, state.abnormalStatusColor)
                setViewVisibility(R.id.speechText, View.VISIBLE)
                setTextViewText(R.id.speechText, state.widgetMessage)
                setTextColor(R.id.speechText, state.abnormalStatusColor)
            }
            setTextViewText(R.id.stretchText, "스트레칭: ${state.stretchCount}회")
        }
    }

    suspend fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, HabitStatusWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        val state = loadWidgetState(context)
        ids.forEach { id -> updateWidget(context, manager, id, state) }
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

    private fun waterWidgetMessage(level: WaterShortageLevel): String =
        when (level) {
            WaterShortageLevel.NONE -> ""
            WaterShortageLevel.LIGHT -> "물 한 잔이면 좋아요."
            WaterShortageLevel.MEDIUM -> "목이 마를 수 있어요."
            WaterShortageLevel.SEVERE -> "물 보충이 필요해요."
        }

    private fun waterStatusColor(level: WaterShortageLevel): Int =
        when (level) {
            WaterShortageLevel.NONE -> Color.TRANSPARENT
            WaterShortageLevel.LIGHT -> Color.rgb(30, 136, 229)
            WaterShortageLevel.MEDIUM -> Color.rgb(21, 101, 192)
            WaterShortageLevel.SEVERE -> Color.rgb(13, 71, 161)
        }

    private const val WATER_GOAL_ML = 2000
    private const val STRETCH_GOAL_COUNT = 5
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependenciesEntryPoint {
    fun checkWaterInterventionNeededUseCase(): CheckWaterInterventionNeededUseCase
}
