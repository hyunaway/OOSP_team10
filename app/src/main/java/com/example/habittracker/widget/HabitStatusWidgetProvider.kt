// 경로: com/example/habittracker/widget/HabitStatusWidgetProvider.kt
package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.AppDatabase
import com.example.habittracker.data.entity.WaterLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            } catch (_: Exception) {
                // 업데이트 실패 시 기존 위젯 내용 유지
            }
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
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ADD_WATER_250 -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppDatabase.getInstance(context).waterDao().insert(
                            WaterLogEntity(
                                timestamp = System.currentTimeMillis(),
                                amountMl = 250,
                                source = "widget",
                            )
                        )
                        WidgetUpdateHelper.updateAllWidgets(context)
                    } catch (_: Exception) {}
                }
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
        const val ACTION_ADD_WATER_250 = "com.example.habittracker.widget.ACTION_ADD_WATER_250"
        const val ACTION_OPEN_STRETCH = "com.example.habittracker.widget.ACTION_OPEN_STRETCH"
        const val ACTION_OPEN_APP = "com.example.habittracker.widget.ACTION_OPEN_APP"
        const val ACTION_REFRESH_WIDGET = "com.example.habittracker.widget.ACTION_REFRESH_WIDGET"
        const val ACTION_GO_TO_DOMINANT = "com.example.habittracker.widget.ACTION_GO_TO_DOMINANT"

        fun attachPendingIntents(context: Context, views: RemoteViews) {
            // 기록하러 가기 → 대표 상태 화면으로 이동
            views.setOnClickPendingIntent(
                R.id.widget_action_button,
                broadcastPendingIntent(context, ACTION_GO_TO_DOMINANT, 2005),
            )
            // 아바타 클릭 → 위젯 새로고침
            views.setOnClickPendingIntent(
                R.id.widget_avatar_image,
                broadcastPendingIntent(context, ACTION_REFRESH_WIDGET, 2006),
            )
            // 레거시 hidden 뷰용 — 기존 브로드캐스트 유지
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
        }

        private fun deepLinkUri(category: WidgetHabitCategory): Uri? = when (category) {
            WidgetHabitCategory.MEAL -> Uri.parse("app://habittracker/meal?source=widget")
            WidgetHabitCategory.WATER -> Uri.parse("app://habittracker/water?source=widget")
            WidgetHabitCategory.DIGITAL -> Uri.parse("app://habittracker/digital?source=widget")
            WidgetHabitCategory.STRETCH -> Uri.parse("app://habittracker/stretch?source=widget")
            WidgetHabitCategory.GOOD -> null
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
