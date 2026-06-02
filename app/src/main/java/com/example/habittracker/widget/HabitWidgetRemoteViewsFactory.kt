// 경로: com/example/habittracker/widget/HabitWidgetRemoteViewsFactory.kt
package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.habittracker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * StackView 카드 데이터를 제공하는 팩토리.
 *
 * onDataSetChanged()는 백그라운드 스레드에서 호출되므로 runBlocking으로
 * buildWidgetState 를 동기 실행해 cardItems를 갱신한다.
 */
class HabitWidgetRemoteViewsFactory(
    private val context: Context,
    intent: Intent,
) : RemoteViewsService.RemoteViewsFactory {

    @Suppress("unused")
    private val widgetId: Int =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    private var cardItems: List<WidgetHabitState> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val state = runBlocking(Dispatchers.IO) {
            try {
                WidgetUpdateHelper.buildWidgetState(context)
            } catch (_: Exception) {
                null
            }
        }
        cardItems = state?.categoryStates ?: emptyList()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = cardItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = cardItems.getOrNull(position)
            ?: return RemoteViews(context.packageName, R.layout.widget_stack_item)

        return RemoteViews(context.packageName, R.layout.widget_stack_item).apply {
            setTextViewText(R.id.widget_stack_category_name, item.title)
            setTextViewText(R.id.widget_stack_message, item.message)
            setTextViewText(R.id.widget_stack_description, item.description)
            setTextViewText(R.id.widget_stack_quick_action, quickActionText(item.category))

            // 카드 전체 클릭 → 해당 카테고리 화면으로 이동
            val navFillIn = Intent().apply {
                putExtra(HabitStatusWidgetProvider.EXTRA_CARD_CATEGORY, item.category.name)
                putExtra(HabitStatusWidgetProvider.EXTRA_CARD_ACTION, ACTION_NAVIGATE)
            }
            setOnClickFillInIntent(R.id.widget_stack_card_body, navFillIn)

            // 빠른 기록 버튼 → 카테고리별 동작
            val actionFillIn = Intent().apply {
                putExtra(HabitStatusWidgetProvider.EXTRA_CARD_CATEGORY, item.category.name)
                putExtra(HabitStatusWidgetProvider.EXTRA_CARD_ACTION, quickActionType(item.category))
            }
            setOnClickFillInIntent(R.id.widget_stack_quick_action, actionFillIn)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    private fun quickActionText(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.WATER -> "💧 +1잔 (250ml)"
        WidgetHabitCategory.STRETCH -> "🧘 완료"
        WidgetHabitCategory.MEAL -> "🍽 식사 기록하기"
        WidgetHabitCategory.DIGITAL -> "📱 사용 기록 보기"
        WidgetHabitCategory.GOOD -> "기록하러 가기"
    }

    private fun quickActionType(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.WATER -> ACTION_ADD_WATER
        WidgetHabitCategory.STRETCH -> ACTION_ADD_STRETCH
        else -> ACTION_NAVIGATE
    }

    companion object {
        const val ACTION_NAVIGATE = "NAVIGATE"
        const val ACTION_ADD_WATER = "ADD_WATER"
        const val ACTION_ADD_STRETCH = "ADD_STRETCH"
    }
}
