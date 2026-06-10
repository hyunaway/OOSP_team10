package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.time.LocalTime
import kotlinx.coroutines.delay
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.usecase.activity.MarkUserActiveUseCase
import com.example.habittracker.domain.usecase.digital.GetTodayDigitalStatusUseCase
import com.example.habittracker.domain.usecase.meal.GetCurrentMealInterventionStatusUseCase
import com.example.habittracker.domain.usecase.meal.GetTodayMealStatusUseCase
import com.example.habittracker.domain.usecase.stretch.CheckStretchInterventionNeededUseCase
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    suspend fun buildWidgetState(context: Context): HabitWidgetState =
        WidgetResourceMapper.buildWidgetState(context)

    suspend fun loadWidgetState(context: Context): HabitWidgetState =
        buildWidgetState(context)

    fun bindStateToRemoteViews(context: Context, state: HabitWidgetState, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_status)

        // 아바타 이미지 + 클릭 → 새로고침
        views.setImageViewResource(R.id.widget_avatar_image, state.avatarResId)
        views.setOnClickPendingIntent(
            R.id.widget_avatar_image,
            HabitStatusWidgetProvider.refreshPendingIntent(context),
        )

        // 카테고리 도트 — 정상: 카테고리 아이콘, 위험: 불꽃 후광 아이콘
        fun dotResId(category: HabitCategory): Int {
            val isRisk = state.categoryCards
                .firstOrNull { it.category == category }
                ?.riskLevel != RiskLevel.NORMAL
            return if (isRisk) {
                when (category) {
                    HabitCategory.MEAL    -> R.drawable.widget_dot_meal_risk
                    HabitCategory.WATER   -> R.drawable.widget_dot_water_risk
                    HabitCategory.DIGITAL -> R.drawable.widget_dot_digital_risk
                    HabitCategory.STRETCH -> R.drawable.widget_dot_stretch_risk
                    HabitCategory.GOOD    -> R.drawable.widget_dot_inactive
                }
            } else {
                when (category) {
                    HabitCategory.MEAL    -> R.drawable.widget_dot_meal
                    HabitCategory.WATER   -> R.drawable.widget_dot_water
                    HabitCategory.DIGITAL -> R.drawable.widget_dot_digital
                    HabitCategory.STRETCH -> R.drawable.widget_dot_stretch
                    HabitCategory.GOOD    -> R.drawable.widget_dot_inactive
                }
            }
        }
        views.setImageViewResource(R.id.widget_status_icon_meal,    dotResId(HabitCategory.MEAL))
        views.setImageViewResource(R.id.widget_status_icon_water,   dotResId(HabitCategory.WATER))
        views.setImageViewResource(R.id.widget_status_icon_digital, dotResId(HabitCategory.DIGITAL))
        views.setImageViewResource(R.id.widget_status_icon_stretch, dotResId(HabitCategory.STRETCH))

        // ViewFlipper: 현재 카드가 항상 index 0이 되도록 순서를 회전해서 추가
        // setDisplayedChild()가 동적 children에서 불안정하므로 rotation 방식 사용
        val cardIndex = HabitStatusWidgetProvider.getCardIndex(context, widgetId)
        val cards = state.categoryCards
        val rotated = cards.drop(cardIndex) + cards.take(cardIndex)
        views.removeAllViews(R.id.widget_view_flipper)
        rotated.forEach { card ->
            views.addView(R.id.widget_view_flipper, buildCardView(context, card, widgetId))
        }
        views.setInt(R.id.widget_view_flipper, "setDisplayedChild", 0)

        // 이전/다음 버튼
        val count = state.categoryCards.size
        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            HabitStatusWidgetProvider.prevCardPendingIntent(context, widgetId, count),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            HabitStatusWidgetProvider.nextCardPendingIntent(context, widgetId, count),
        )

        // 레거시 hidden 뷰 바인딩
        val dominantCard = state.categoryCards.firstOrNull { it.category == state.dominantCategory }
        views.setTextViewText(R.id.widget_card_category_name, dominantCard?.statusLabel ?: "")
        views.setTextViewText(R.id.widget_card_message,       dominantCard?.description ?: "")
        views.setTextViewText(R.id.widget_card_description,   "")
        views.setTextViewText(R.id.widget_card_quick_action,  dominantCard?.actionLabel ?: "")
        views.setTextViewText(R.id.widget_speech_text,        dominantCard?.description ?: "")
        views.setTextViewText(R.id.widget_status_label,       dominantCard?.statusLabel ?: "")
        views.setTextViewText(R.id.avatarText,                "")
        views.setTextViewText(R.id.waterText,                 "")
        views.setTextViewText(R.id.stretchText,               "")
        views.setTextViewText(R.id.abnormalStatusText,        "")
        views.setTextViewText(R.id.speechText,                "")

        return views
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

    suspend fun showActionAvatarThenUpdate(context: Context, actionType: WidgetActionType) {
        WidgetActionLock.lock(context, actionType)
        updateAllWidgets(context)
        delay(1_000)
        WidgetActionLock.unlock(context)
        updateAllWidgets(context)
    }

    fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        state: HabitWidgetState,
    ) {
        val views = bindStateToRemoteViews(context, state, widgetId)
        HabitStatusWidgetProvider.attachPendingIntents(context, views)
        manager.updateAppWidget(widgetId, views)
    }

    // ── 카드 RemoteViews 빌드 ──────────────────────────────────────────────────

    private fun buildCardView(context: Context, item: HabitCardState, widgetId: Int): RemoteViews {
        val base = widgetId * 100 + item.category.ordinal * 10
        if (item.category == HabitCategory.MEAL)    return buildMealCardView(context, item, widgetId, base)
        if (item.category == HabitCategory.WATER)   return buildWaterCardView(context, item, widgetId, base)
        if (item.category == HabitCategory.DIGITAL) return buildDigitalCardView(context, item, widgetId, base)
        if (item.category == HabitCategory.STRETCH) return buildStretchCardView(context, item, widgetId, base)
        return RemoteViews(context.packageName, R.layout.widget_habit_card_item).apply {
            setImageViewResource(R.id.widget_card_icon, item.iconResId)
            setTextViewText(R.id.widget_card_name,   item.category.displayName())
            setTextViewText(R.id.widget_card_status, item.statusLabel)
            setTextViewText(R.id.widget_card_desc,   item.description)
            
            if (widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                setOnClickPendingIntent(
                    R.id.widget_card_name,
                    navigatePendingIntent(context, item.category, base + 1),
                )
            }
            
            setViewVisibility(R.id.tv_last_meal_time, View.GONE)
            setViewVisibility(R.id.tv_meal_count,     View.GONE)
            setViewVisibility(R.id.pb_meal_progress,  View.GONE)
            
            if (item.isActionable) {
                setViewVisibility(R.id.widget_card_btn, View.VISIBLE)
                setTextViewText(R.id.widget_card_btn, item.actionLabel)
                setFloat(R.id.widget_card_btn, "setAlpha", 1.0f)
                val btnAction = when (item.category) {
                    HabitCategory.WATER   -> HabitStatusWidgetProvider.ACTION_ADD_WATER_250
                    HabitCategory.STRETCH -> HabitStatusWidgetProvider.ACTION_ADD_STRETCH_QUICK
                    else                  -> null
                }
                if (btnAction != null && widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setOnClickPendingIntent(
                        R.id.widget_card_btn,
                        broadcastPI(context, btnAction, base + 2),
                    )
                }
            } else {
                setViewVisibility(R.id.widget_card_btn, View.GONE)
            }
        }
    }

    private fun buildMealCardView(context: Context, item: HabitCardState, widgetId: Int, base: Int): RemoteViews {
        val mealData = item.mealData
        val breakfastLogged = mealData?.breakfastLogged ?: false
        val lunchLogged     = mealData?.lunchLogged     ?: false
        val dinnerLogged    = mealData?.dinnerLogged    ?: false

        val currentMealLogged = when (LocalTime.now().hour) {
            in 0..9   -> breakfastLogged
            in 10..15 -> lunchLogged
            else      -> dinnerLogged
        }
        val allDone = mealData?.let { it.todayRecordCount >= it.targetMealCount } ?: false
        val buttonDisabled = currentMealLogged || allDone

        return RemoteViews(context.packageName, R.layout.widget_meal_card_item).apply {
            setTextViewText(R.id.widget_card_name,   "식사")
            setTextViewText(R.id.widget_card_status, item.statusLabel)

            // 끼니별 원 상태
            setImageViewResource(
                R.id.widget_meal_circle_breakfast,
                if (breakfastLogged) R.drawable.widget_meal_circle_on else R.drawable.widget_meal_circle_off,
            )
            setImageViewResource(
                R.id.widget_meal_circle_lunch,
                if (lunchLogged) R.drawable.widget_meal_circle_on else R.drawable.widget_meal_circle_off,
            )
            setImageViewResource(
                R.id.widget_meal_circle_dinner,
                if (dinnerLogged) R.drawable.widget_meal_circle_on else R.drawable.widget_meal_circle_off,
            )

            if (widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                setOnClickPendingIntent(
                    R.id.widget_card_name,
                    navigatePendingIntent(context, HabitCategory.MEAL, base + 1),
                )
                val mealLocked = WidgetActionLock.getLockedAction(context) == WidgetActionType.MEAL
                when {
                    mealLocked -> {
                        setTextViewText(R.id.widget_btn_eat, "기록 완료!")
                        setFloat(R.id.widget_btn_eat, "setAlpha", 0.5f)
                    }
                    buttonDisabled -> {
                        setTextViewText(R.id.widget_btn_eat, "체크완료")
                        setFloat(R.id.widget_btn_eat, "setAlpha", 0.5f)
                    }
                    else -> {
                        setTextViewText(R.id.widget_btn_eat, "먹었어요")
                        setFloat(R.id.widget_btn_eat, "setAlpha", 1.0f)
                        setOnClickPendingIntent(
                            R.id.widget_btn_eat,
                            broadcastPI(context, HabitStatusWidgetProvider.ACTION_MEAL_QUICK_RECORD, base + 2),
                        )
                    }
                }
                setOnClickPendingIntent(
                    R.id.widget_btn_edit_meal,
                    navigatePendingIntent(context, HabitCategory.MEAL, base + 3),
                )
            } else {
                setTextViewText(R.id.widget_btn_eat, "먹었어요")
                setFloat(R.id.widget_btn_eat, "setAlpha", 0.5f)
                setFloat(R.id.widget_btn_edit_meal, "setAlpha", 0.5f)
            }
        }
    }

    private fun buildWaterCardView(context: Context, item: HabitCardState, widgetId: Int, base: Int): RemoteViews {
        val waterData = item.waterData
        return RemoteViews(context.packageName, R.layout.widget_water_card_item).apply {
            setTextViewText(R.id.widget_card_name, "물 섭취")
            if (waterData != null) {
                val currentCups = waterData.currentMl / 250
                val goalCups    = waterData.goalMl / 250
                setTextViewText(R.id.widget_water_amount, "${currentCups}잔 / ${goalCups}잔")
                setProgressBar(R.id.widget_water_progress, waterData.goalMl, waterData.currentMl, false)
            } else {
                setTextViewText(R.id.widget_water_amount, "0잔 / 8잔")
                setProgressBar(R.id.widget_water_progress, 100, 0, false)
            }
            if (widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                setOnClickPendingIntent(
                    R.id.widget_card_name,
                    navigatePendingIntent(context, HabitCategory.WATER, base + 1),
                )
                val waterLocked = WidgetActionLock.getLockedAction(context) == WidgetActionType.WATER
                setTextViewText(R.id.widget_btn_water_log, if (waterLocked) "기록 완료!" else "물 한잔")
                setFloat(R.id.widget_btn_water_log, "setAlpha", if (waterLocked) 0.5f else 1.0f)
                if (!waterLocked) {
                    setOnClickPendingIntent(
                        R.id.widget_btn_water_log,
                        broadcastPI(context, HabitStatusWidgetProvider.ACTION_ADD_WATER_250, base + 2),
                    )
                }
                setOnClickPendingIntent(
                    R.id.widget_btn_water_edit,
                    navigatePendingIntent(context, HabitCategory.WATER, base + 3),
                )
            }
        }
    }

    private fun buildDigitalCardView(context: Context, item: HabitCardState, widgetId: Int, base: Int): RemoteViews {
        val digitalData = item.digitalData
        return RemoteViews(context.packageName, R.layout.widget_digital_card_item).apply {
            setTextViewText(R.id.widget_card_name, "디지털")

            val (badgeText, badgeColor) = when (item.riskLevel) {
                RiskLevel.NORMAL  -> Pair("양호",  0xFF5BAE6E.toInt())
                RiskLevel.WARNING -> Pair("주의",  0xFFF07B3F.toInt())
                RiskLevel.DANGER  -> Pair("과사용", 0xFF8B75D7.toInt())
            }
            setTextViewText(R.id.widget_digital_status_badge, badgeText)
            setTextColor(R.id.widget_digital_status_badge, badgeColor)

            val usageSummary = if (digitalData != null) {
                when (item.riskLevel) {
                    RiskLevel.DANGER -> {
                        val excess = (digitalData.usageMinutes - digitalData.goalMinutes).coerceAtLeast(0)
                        "목표보다 ${excess}분 초과"
                    }
                    else -> {
                        val h = digitalData.usageMinutes / 60
                        val m = digitalData.usageMinutes % 60
                        if (h > 0) "오늘 사용 ${h}시간 ${m}분" else "오늘 사용 ${m}분"
                    }
                }
            } else {
                item.description
            }
            setTextViewText(R.id.widget_digital_usage, usageSummary)

            if (widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                setOnClickPendingIntent(
                    R.id.widget_card_name,
                    navigatePendingIntent(context, HabitCategory.DIGITAL, base + 1),
                )
                setOnClickPendingIntent(
                    R.id.widget_btn_digital_manage,
                    navigatePendingIntent(context, HabitCategory.DIGITAL, base + 2),
                )
            }
        }
    }

    private fun buildStretchCardView(context: Context, item: HabitCardState, widgetId: Int, base: Int): RemoteViews {
        val stretchData = item.stretchData
        val timerState  = stretchData?.timerState
            ?: StretchTimerWidgetState(isRunning = false, remainingSeconds = 60, isCompleted = false)

        return RemoteViews(context.packageName, R.layout.widget_stretch_card_item).apply {
            setTextViewText(R.id.widget_card_name, "스트레칭")

            val statusMsg = when {
                timerState.isRunning   -> "몸을 천천히 풀어주세요"
                timerState.isCompleted -> "몸이 한결 가벼워졌어요"
                item.riskLevel == RiskLevel.WARNING || item.riskLevel == RiskLevel.DANGER
                                       -> "몸이 굳어가고 있어요"
                else                   -> "몸이 가볍게 풀렸어요"
            }
            setTextViewText(R.id.widget_stretch_status, statusMsg)

            val secondaryText = when {
                timerState.isRunning   -> "1분 스트레칭 진행 중"
                timerState.isCompleted -> "방금 스트레칭함"
                else -> {
                    val lastAt = stretchData?.lastStretchAtMillis
                    if (lastAt != null && lastAt > 0L) {
                        val minutesAgo = ((System.currentTimeMillis() - lastAt) / 60_000L).toInt()
                        val hoursAgo = minutesAgo / 60
                        when {
                            hoursAgo >= 1  -> "마지막 스트레칭 ${hoursAgo}시간 전"
                            minutesAgo > 0 -> "마지막 스트레칭 ${minutesAgo}분 전"
                            else           -> "방금 스트레칭함"
                        }
                    } else "스트레칭을 아직 안 했어요"
                }
            }
            setTextViewText(R.id.widget_stretch_secondary, secondaryText)

            val btnText = when {
                timerState.isRunning -> {
                    val m = timerState.remainingSeconds / 60
                    val s = timerState.remainingSeconds % 60
                    "%02d:%02d".format(m, s)
                }
                timerState.isCompleted -> "완료됨"
                else -> "스트레칭"
            }
            setTextViewText(R.id.widget_btn_stretch, btnText)

            val btnAlpha = when {
                timerState.isCompleted -> 0.6f
                timerState.isRunning   -> 0.85f
                else                   -> 1.0f
            }
            setFloat(R.id.widget_btn_stretch, "setAlpha", btnAlpha)

            if (widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                // 타이머 진행 중에는 카드 클릭으로 앱 진입 차단
                if (!timerState.isRunning) {
                    setOnClickPendingIntent(
                        R.id.widget_card_name,
                        navigatePendingIntent(context, HabitCategory.STRETCH, base + 1),
                    )
                }

                // 버튼 클릭: 기본 상태일 때만 타이머 시작
                if (!timerState.isRunning && !timerState.isCompleted) {
                    setOnClickPendingIntent(
                        R.id.widget_btn_stretch,
                        broadcastPI(context, HabitStatusWidgetProvider.ACTION_STRETCH_START_TIMER, base + 2),
                    )
                }
            }
        }
    }

    private fun navigatePendingIntent(context: Context, category: HabitCategory, requestCode: Int): PendingIntent {
        val uri = when (category) {
            HabitCategory.MEAL    -> Uri.parse("app://habittracker/meal?type=&source=widget")
            HabitCategory.WATER   -> Uri.parse("app://habittracker/water?source=widget")
            HabitCategory.DIGITAL -> Uri.parse("app://habittracker/digital?app=&interventionId=-1&source=widget")
            HabitCategory.STRETCH -> Uri.parse("app://habittracker/stretch?trigger=widget")
            HabitCategory.GOOD    -> null
        }
        val intent = if (uri != null) {
            Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setClass(context, MainActivity::class.java)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun broadcastPI(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, HabitStatusWidgetProvider::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private fun HabitCategory.displayName(): String = when (this) {
    HabitCategory.MEAL    -> "식사"
    HabitCategory.WATER   -> "물"
    HabitCategory.DIGITAL -> "디지털"
    HabitCategory.STRETCH -> "스트레칭"
    HabitCategory.GOOD    -> "양호"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependenciesEntryPoint {
    fun checkWaterInterventionNeededUseCase(): CheckWaterInterventionNeededUseCase
    fun getTodayDigitalStatusUseCase(): GetTodayDigitalStatusUseCase
    fun getTodayMealStatusUseCase(): GetTodayMealStatusUseCase
    fun getCurrentMealInterventionStatusUseCase(): GetCurrentMealInterventionStatusUseCase
    fun checkStretchInterventionNeededUseCase(): CheckStretchInterventionNeededUseCase
    fun getTodayStretchStatusUseCase(): GetTodayStretchStatusUseCase
    fun mealRepository(): MealRepository
    fun stretchRepository(): StretchRepository
    fun userPreferenceManager(): UserPreferenceManager
    fun markUserActiveUseCase(): MarkUserActiveUseCase
}
