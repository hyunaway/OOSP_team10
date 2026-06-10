package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.AppDatabase
import com.example.habittracker.data.entity.WaterLogEntity
import com.example.habittracker.data.model.MealType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first

class HabitStatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = WidgetUpdateHelper.buildWidgetState(context)
                appWidgetIds.forEach { id ->
                    WidgetUpdateHelper.updateWidget(context, appWidgetManager, id, state)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdateHelper.updateAllWidgets(context)
            } catch (_: Exception) {}
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("HabitWidget", "onReceive: action = ${intent.action}")
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_NEXT_CARD -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (widgetId != -1) {
                    val count = intent.getIntExtra(EXTRA_CARD_COUNT, 4)
                    val current = getCardIndex(context, widgetId)
                    setCardIndex(context, widgetId, (current + 1) % count)
                    CoroutineScope(Dispatchers.IO).launch {
                        try { WidgetUpdateHelper.updateAllWidgets(context) } catch (_: Exception) {}
                    }
                }
            }
            ACTION_PREV_CARD -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (widgetId != -1) {
                    val count = intent.getIntExtra(EXTRA_CARD_COUNT, 4)
                    val current = getCardIndex(context, widgetId)
                    setCardIndex(context, widgetId, (current - 1 + count) % count)
                    CoroutineScope(Dispatchers.IO).launch {
                        try { WidgetUpdateHelper.updateAllWidgets(context) } catch (_: Exception) {}
                    }
                }
            }
            ACTION_ADD_WATER_250 -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ep = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            WidgetDependenciesEntryPoint::class.java,
                        )
                        val activeStartedAt = ep.userPreferenceManager().todayActiveStartedAtFlow.first()
                        if (activeStartedAt == null) {
                            return@launch
                        }
                        AppDatabase.getInstance(context).waterDao().insert(
                            WaterLogEntity(
                                timestamp = System.currentTimeMillis(),
                                amountMl = 250,
                                source = "widget",
                            )
                        )
                        ep.markUserActiveUseCase()("widget_water_log")
                        WidgetUpdateHelper.showActionAvatarThenUpdate(context, WidgetActionType.WATER)
                    } catch (_: Exception) {}
                }
            }
            ACTION_ADD_STRETCH_QUICK -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ep = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            WidgetDependenciesEntryPoint::class.java,
                        )
                        val activeStartedAt = ep.userPreferenceManager().todayActiveStartedAtFlow.first()
                        if (activeStartedAt == null) {
                            return@launch
                        }
                        val timeSlot = when (LocalTime.now().hour) {
                            in 0..11 -> "아침"
                            in 12..17 -> "점심"
                            in 18..21 -> "저녁"
                            else -> "기타"
                        }
                        ep.stretchRepository().insertStretchRecord(
                            date = LocalDate.now().toString(),
                            timeSlot = timeSlot,
                        )
                        ep.markUserActiveUseCase()("widget_stretch_log")
                        WidgetUpdateHelper.updateAllWidgets(context)
                    } catch (_: Exception) {}
                }
            }
            ACTION_QUICK_LOG_MEAL -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ep = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            WidgetDependenciesEntryPoint::class.java,
                        )
                        val activeStartedAt = ep.userPreferenceManager().todayActiveStartedAtFlow.first()
                        if (activeStartedAt == null) {
                            return@launch
                        }
                        val mealStatus = ep.getCurrentMealInterventionStatusUseCase()()
                        val mealType = mealStatus.actionableMealType
                            ?: when (LocalTime.now().hour) {
                                in 0..9   -> MealType.BREAKFAST
                                in 10..15 -> MealType.LUNCH
                                else      -> MealType.DINNER
                            }
                        val now = LocalTime.now()
                        val recordedTime = "${now.hour.toString().padStart(2, '0')}:" +
                            now.minute.toString().padStart(2, '0')
                        ep.mealRepository().addLog(
                            type = mealType,
                            timestamp = System.currentTimeMillis(),
                            isLateNight = false,
                            viaDeliveryApp = false,
                            source = "widget",
                            mealDate = LocalDate.now().toString(),
                            recordedTime = recordedTime,
                            inputMethod = "widget_quick",
                            triggerType = "widget",
                        )
                        ep.markUserActiveUseCase()("widget_meal_log")
                        WidgetUpdateHelper.showActionAvatarThenUpdate(context, WidgetActionType.MEAL)
                    } catch (_: Exception) {}
                }
            }
            ACTION_MEAL_QUICK_RECORD -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ep = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            WidgetDependenciesEntryPoint::class.java,
                        )
                        val activeStartedAt = ep.userPreferenceManager().todayActiveStartedAtFlow.first()
                        if (activeStartedAt == null) {
                            return@launch
                        }
                        val mealType = when (LocalTime.now().hour) {
                            in 0..9   -> MealType.BREAKFAST
                            in 10..15 -> MealType.LUNCH
                            else      -> MealType.DINNER
                        }
                        val todayMealStatus = ep.getTodayMealStatusUseCase()().first()
                        val alreadyLogged = when (mealType) {
                            MealType.BREAKFAST   -> todayMealStatus.breakfastLogged
                            MealType.LUNCH       -> todayMealStatus.lunchLogged
                            MealType.DINNER      -> todayMealStatus.dinnerLogged
                            MealType.LATE_NIGHT  -> false
                        }
                        if (!alreadyLogged) {
                            val now = LocalTime.now()
                            val recordedTime = "${now.hour.toString().padStart(2, '0')}:" +
                                now.minute.toString().padStart(2, '0')
                            ep.mealRepository().addLog(
                                type = mealType,
                                timestamp = System.currentTimeMillis(),
                                isLateNight = false,
                                viaDeliveryApp = false,
                                source = "widget",
                                mealDate = LocalDate.now().toString(),
                                recordedTime = recordedTime,
                                inputMethod = "widget_quick",
                                triggerType = "widget",
                            )
                            ep.markUserActiveUseCase()("widget_meal_log")
                            WidgetUpdateHelper.showActionAvatarThenUpdate(context, WidgetActionType.MEAL)
                        } else {
                            WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    } catch (_: Exception) {}
                }
            }
            ACTION_STRETCH_START_TIMER -> {
                val prefs = context.getSharedPreferences(
                    StretchTimerService.PREFS_STRETCH_TIMER, Context.MODE_PRIVATE)
                val startedAt = prefs.getLong(StretchTimerService.KEY_TIMER_STARTED_AT, 0L)
                val now = System.currentTimeMillis()
                val alreadyRunning = startedAt > 0L &&
                    (now - startedAt) < StretchTimerService.STRETCH_DURATION_MS
                if (!alreadyRunning) {
                    // 타이머 시작 시간을 먼저 기록해 getTimerState가 isRunning=true를 즉시 반환하도록 함
                    prefs.edit()
                        .putLong(StretchTimerService.KEY_TIMER_STARTED_AT, System.currentTimeMillis())
                        .apply()
                    val serviceIntent = Intent(context, StretchTimerService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try { WidgetUpdateHelper.updateAllWidgets(context) } catch (_: Exception) {}
                    }
                }
            }
            ACTION_STRETCH_TIMER_TICK -> {
                WidgetUpdateHelper.updateAllWidgetsSync(context)
            }
            ACTION_OPEN_MEAL_EDIT -> {
                launchDeepLink(context, Uri.parse("app://habittracker/meal?type=&source=widget"))
            }
            ACTION_OPEN_STRETCH -> {
                launchDeepLink(context, Uri.parse("app://habittracker/stretch?trigger=widget"))
            }
            ACTION_OPEN_APP -> {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            ACTION_REFRESH_WIDGET -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        WidgetUpdateHelper.updateAllWidgets(context)
                    } catch (_: Exception) {}
                }
            }
            ACTION_GO_TO_DOMINANT -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val state = WidgetUpdateHelper.buildWidgetState(context)
                        val uri = deepLinkUri(state.dominantCategory)
                        if (uri != null) {
                            launchDeepLink(context, uri)
                        } else {
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_NEXT_CARD           = "com.example.habittracker.widget.ACTION_NEXT_CARD"
        const val ACTION_PREV_CARD           = "com.example.habittracker.widget.ACTION_PREV_CARD"
        const val ACTION_QUICK_LOG_MEAL      = "com.example.habittracker.widget.ACTION_QUICK_LOG_MEAL"
        const val ACTION_MEAL_QUICK_RECORD   = "com.example.habittracker.widget.ACTION_MEAL_QUICK_RECORD"
        const val ACTION_OPEN_MEAL_EDIT      = "com.example.habittracker.widget.ACTION_OPEN_MEAL_EDIT"
        const val ACTION_ADD_WATER_250      = "com.example.habittracker.widget.ACTION_ADD_WATER_250"
        const val ACTION_ADD_STRETCH_QUICK   = "com.example.habittracker.widget.ACTION_ADD_STRETCH_QUICK"
        const val ACTION_OPEN_STRETCH        = "com.example.habittracker.widget.ACTION_OPEN_STRETCH"
        const val ACTION_STRETCH_START_TIMER = "com.example.habittracker.widget.ACTION_STRETCH_START_TIMER"
        const val ACTION_STRETCH_TIMER_TICK  = "com.example.habittracker.widget.ACTION_STRETCH_TIMER_TICK"

        private const val COMPLETED_DISPLAY_MS = 300_000L  // 5분간 완료 상태 유지

        fun getTimerState(context: Context): StretchTimerWidgetState {
            val prefs = context.getSharedPreferences(
                StretchTimerService.PREFS_STRETCH_TIMER, Context.MODE_PRIVATE)
            val startedAt   = prefs.getLong(StretchTimerService.KEY_TIMER_STARTED_AT, 0L)
            val completedAt = prefs.getLong(StretchTimerService.KEY_TIMER_COMPLETED_AT, 0L)
            val now = System.currentTimeMillis()
            if (startedAt > 0L) {
                val remaining = ((StretchTimerService.STRETCH_DURATION_MS - (now - startedAt)) / 1_000L)
                    .toInt().coerceAtLeast(0)
                if (remaining > 0)
                    return StretchTimerWidgetState(isRunning = true, remainingSeconds = remaining, isCompleted = false)
            }
            if (completedAt > 0L && now - completedAt < COMPLETED_DISPLAY_MS)
                return StretchTimerWidgetState(isRunning = false, remainingSeconds = 0, isCompleted = true)
            return StretchTimerWidgetState(isRunning = false, remainingSeconds = 60, isCompleted = false)
        }

        const val ACTION_OPEN_APP           = "com.example.habittracker.widget.ACTION_OPEN_APP"
        const val ACTION_REFRESH_WIDGET     = "com.example.habittracker.widget.ACTION_REFRESH_WIDGET"
        const val ACTION_GO_TO_DOMINANT     = "com.example.habittracker.widget.ACTION_GO_TO_DOMINANT"

        const val EXTRA_CARD_COUNT = "extra_card_count"

        private const val PREFS_FLIPPER = "widget_flipper"

        fun getCardIndex(context: Context, widgetId: Int): Int =
            context.getSharedPreferences(PREFS_FLIPPER, Context.MODE_PRIVATE)
                .getInt("idx_$widgetId", 0)

        fun setCardIndex(context: Context, widgetId: Int, index: Int) =
            context.getSharedPreferences(PREFS_FLIPPER, Context.MODE_PRIVATE)
                .edit().putInt("idx_$widgetId", index).apply()

        fun nextCardPendingIntent(context: Context, widgetId: Int, count: Int): PendingIntent {
            val intent = Intent(context, HabitStatusWidgetProvider::class.java).apply {
                action = ACTION_NEXT_CARD
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_CARD_COUNT, count)
            }
            return PendingIntent.getBroadcast(
                context, 8000 + widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun prevCardPendingIntent(context: Context, widgetId: Int, count: Int): PendingIntent {
            val intent = Intent(context, HabitStatusWidgetProvider::class.java).apply {
                action = ACTION_PREV_CARD
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_CARD_COUNT, count)
            }
            return PendingIntent.getBroadcast(
                context, 9000 + widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun refreshPendingIntent(context: Context): PendingIntent =
            broadcastPendingIntent(context, ACTION_REFRESH_WIDGET, 2006)

        fun attachPendingIntents(context: Context, views: RemoteViews) {
            views.setOnClickPendingIntent(
                R.id.addWaterButton,
                broadcastPendingIntent(context, ACTION_ADD_WATER_250, 2001),
            )
            views.setOnClickPendingIntent(
                R.id.stretchButton,
                broadcastPendingIntent(context, ACTION_OPEN_STRETCH, 2002),
            )
            views.setOnClickPendingIntent(
                R.id.openAppButton,
                broadcastPendingIntent(context, ACTION_OPEN_APP, 2003),
            )
            views.setOnClickPendingIntent(
                R.id.avatarText,
                broadcastPendingIntent(context, ACTION_REFRESH_WIDGET, 2004),
            )
            views.setOnClickPendingIntent(
                R.id.widget_action_button,
                broadcastPendingIntent(context, ACTION_GO_TO_DOMINANT, 2008),
            )
            views.setOnClickPendingIntent(
                R.id.widget_right_panel,
                broadcastPendingIntent(context, ACTION_GO_TO_DOMINANT, 2005),
            )
        }

        fun quickActionPendingIntent(
            context: Context,
            dominantCategory: HabitCategory,
        ): PendingIntent {
            val action = when (dominantCategory) {
                HabitCategory.WATER   -> ACTION_ADD_WATER_250
                HabitCategory.STRETCH -> ACTION_ADD_STRETCH_QUICK
                else                  -> ACTION_GO_TO_DOMINANT
            }
            return broadcastPendingIntent(context, action, 2007)
        }

        private fun deepLinkUri(category: HabitCategory): Uri? = when (category) {
            HabitCategory.MEAL    -> Uri.parse("app://habittracker/meal?type=&source=widget")
            HabitCategory.WATER   -> Uri.parse("app://habittracker/water?source=widget")
            HabitCategory.DIGITAL -> Uri.parse("app://habittracker/digital?app=&interventionId=-1&source=widget")
            HabitCategory.STRETCH -> Uri.parse("app://habittracker/stretch?trigger=widget")
            HabitCategory.GOOD    -> null
        }

        private fun launchDeepLink(context: Context, uri: Uri) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setClass(context, MainActivity::class.java)
                }
            )
        }

        private fun broadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, HabitStatusWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
