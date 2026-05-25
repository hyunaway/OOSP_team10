// 경로: com/example/habittracker/util/NotificationHelper.kt
package com.example.habittracker.util

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.HabitTrackerApplication
import com.example.habittracker.R
import com.example.habittracker.data.entity.NotificationActionLogEntity
import com.example.habittracker.data.local.room.dao.NotificationActionLogDao
import java.util.concurrent.atomic.AtomicInteger

class NotificationHelper(
    private val context: Context,
    private val notificationActionLogDao: NotificationActionLogDao,
    private val deepLinkFactory: DeepLinkFactory,
) {

    suspend fun sendWaterReminder(message: String) {
        val id = nextId()
        val contentIntent = deepLinkFactory.waterIntent(context)
        logShown("water", id)
        send(
            channelId = HabitTrackerApplication.CHANNEL_WATER,
            notificationId = id,
            title = "💧 수분 보충 시간",
            body = message,
            contentIntent = contentIntent,
        )
    }

    suspend fun sendMealReminder(message: String, mealType: String) {
        val id = nextId()
        val contentIntent = deepLinkFactory.mealIntent(context, type = mealType)
        logShown("meal", id)
        send(
            channelId = HabitTrackerApplication.CHANNEL_MEAL,
            notificationId = id,
            title = "🍽 식사 알림",
            body = message,
            contentIntent = contentIntent,
        )
    }

    suspend fun sendDigitalIntervention(
        message: String,
        appPackage: String,
        interventionId: Long,
    ) {
        val id = nextId()
        val contentIntent = deepLinkFactory.digitalIntent(
            context,
            appPackage = appPackage,
            interventionId = interventionId,
        )
        logShown("digital", id)
        send(
            channelId = HabitTrackerApplication.CHANNEL_DIGITAL,
            notificationId = id,
            title = "📱 스마트폰 사용 알림",
            body = message,
            contentIntent = contentIntent,
        )
    }

    suspend fun sendDigitalDailySummary(
        totalMinutes: Int,
        appBreakdown: Map<String, Int>,
    ) {
        val id = nextId()
        val topApp = appBreakdown.maxByOrNull { it.value }
        val body = buildString {
            append("오늘 총 ${totalMinutes}분 사용")
            if (topApp != null) append(" • 최다: ${topApp.key} ${topApp.value}분")
        }
        val contentIntent = deepLinkFactory.digitalIntent(context, appPackage = "")
        logShown("digital_summary", id)
        send(
            channelId = HabitTrackerApplication.CHANNEL_DIGITAL,
            notificationId = id,
            title = "📊 오늘의 디지털 사용 요약",
            body = body,
            contentIntent = contentIntent,
        )
    }

    suspend fun sendStretchReminder(message: String, trigger: String = "normal") {
        val id = nextId()
        val contentIntent = deepLinkFactory.stretchIntent(context, trigger = trigger)
        logShown("stretch", id)
        send(
            channelId = HabitTrackerApplication.CHANNEL_STRETCH,
            notificationId = id,
            title = "🧘 스트레칭 시간",
            body = message,
            contentIntent = contentIntent,
        )
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun send(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        contentIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private suspend fun logShown(category: String, notificationId: Int) {
        notificationActionLogDao.insert(
            NotificationActionLogEntity(
                category = category,
                notificationId = notificationId,
                shownAt = System.currentTimeMillis(),
                clickedAt = null,
                actionType = null,
                dismissed = false,
            )
        )
    }

    private val counter = AtomicInteger(2000)
    private fun nextId(): Int = counter.getAndIncrement()
}
