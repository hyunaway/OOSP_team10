package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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
            speechBubbleMessage = waterStatus.message,
            stretchCount = stretchCount,
            avatarHealthScore = totalScore,
            avatarEmoji = emoji,
        )
    }

    fun buildRemoteViews(context: Context, state: HabitWidgetState): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_habit_status).apply {
            setTextViewText(R.id.avatarText, state.avatarEmoji)
            setTextViewText(R.id.waterText, "물 ${state.waterTotalMl}ml · ${state.waterStatusText}")
            setTextViewText(R.id.speechText, state.speechBubbleMessage)
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
            WaterShortageLevel.NONE -> "좋음"
            WaterShortageLevel.LIGHT -> "물 부족"
            WaterShortageLevel.MEDIUM -> "탈수 주의"
            WaterShortageLevel.SEVERE -> "탈수"
        }

    private const val WATER_GOAL_ML = 2000
    private const val STRETCH_GOAL_COUNT = 5
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependenciesEntryPoint {
    fun checkWaterInterventionNeededUseCase(): CheckWaterInterventionNeededUseCase
}
