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
            val state = WidgetUpdateHelper.loadWidgetState(context)
            appWidgetIds.forEach { id ->
                WidgetUpdateHelper.updateWidget(context, appWidgetManager, id, state)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ADD_WATER_250 -> {
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getInstance(context).waterDao().insert(
                        WaterLogEntity(
                            timestamp = System.currentTimeMillis(),
                            amountMl = 250,
                            source = "widget",
                        )
                    )
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
            ACTION_OPEN_STRETCH -> {
                val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse("app://habittracker/stretch?trigger=widget")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setClass(context, MainActivity::class.java)
                }
                context.startActivity(deepLink)
            }
            ACTION_OPEN_APP -> {
                val open = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(open)
            }
            ACTION_REFRESH_WIDGET -> {
                CoroutineScope(Dispatchers.IO).launch {
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_ADD_WATER_250 = "com.example.habittracker.widget.ACTION_ADD_WATER_250"
        const val ACTION_OPEN_STRETCH = "com.example.habittracker.widget.ACTION_OPEN_STRETCH"
        const val ACTION_OPEN_APP = "com.example.habittracker.widget.ACTION_OPEN_APP"
        const val ACTION_REFRESH_WIDGET = "com.example.habittracker.widget.ACTION_REFRESH_WIDGET"

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
