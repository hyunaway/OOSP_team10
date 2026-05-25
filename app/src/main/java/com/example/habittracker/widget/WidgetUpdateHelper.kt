// 경로: com/example/habittracker/widget/WidgetUpdateHelper.kt
package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.habittracker.R
import com.example.habittracker.data.AppDatabase
import kotlinx.coroutines.flow.first

object WidgetUpdateHelper {

    suspend fun loadWidgetState(context: Context): HabitWidgetState {
        val db = AppDatabase.getInstance(context)
        val waterTotalMl = db.waterDao().getTodayTotal().first() ?: 0
        val stretchCount = db.stretchDao().getTodayLogs().first().size

        val waterScore = minOf(waterTotalMl / 2000f, 1f) * 50f
        val stretchScore = minOf(stretchCount / 5f, 1f) * 50f
        val totalScore = (waterScore + stretchScore).toInt()

        val emoji = when {
            totalScore >= 80 -> "😊"
            totalScore >= 50 -> "😐"
            else -> "😢"
        }

        return HabitWidgetState(
            waterTotalMl = waterTotalMl,
            stretchCount = stretchCount,
            avatarHealthScore = totalScore,
            avatarEmoji = emoji,
        )
    }

    fun buildRemoteViews(context: Context, state: HabitWidgetState): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_habit_status).apply {
            setTextViewText(R.id.avatarText, state.avatarEmoji)
            setTextViewText(R.id.waterText, "💧 물: ${state.waterTotalMl}ml")
            setTextViewText(R.id.stretchText, "🧘 스트레칭: ${state.stretchCount}회")
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
}
