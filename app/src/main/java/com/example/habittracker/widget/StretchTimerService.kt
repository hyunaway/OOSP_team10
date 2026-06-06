package com.example.habittracker.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.habittracker.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class StretchTimerService : Service() {

    private var countDownTimer: CountDownTimer? = null

    companion object {
        private const val NOTIFICATION_ID        = 8877
        private const val CHANNEL_ID             = "stretch_timer_channel"
        internal const val PREFS_STRETCH_TIMER   = "widget_stretch_timer"
        internal const val KEY_TIMER_STARTED_AT  = "stretch_started_at"
        internal const val KEY_TIMER_COMPLETED_AT = "stretch_completed_at"
        internal const val STRETCH_DURATION_MS   = 60_000L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(60),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(60))
        }

        val prefs = getSharedPreferences(PREFS_STRETCH_TIMER, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_TIMER_STARTED_AT, System.currentTimeMillis())
            .remove(KEY_TIMER_COMPLETED_AT)
            .apply()

        countDownTimer = object : CountDownTimer(STRETCH_DURATION_MS, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = ((millisUntilFinished + 999L) / 1_000L).toInt()
                updateNotification(remaining)
                sendBroadcast(
                    Intent(this@StretchTimerService, HabitStatusWidgetProvider::class.java).apply {
                        action = HabitStatusWidgetProvider.ACTION_STRETCH_TIMER_TICK
                    }
                )
            }

            override fun onFinish() {
                val prefs = getSharedPreferences(PREFS_STRETCH_TIMER, Context.MODE_PRIVATE)
                prefs.edit()
                    .remove(KEY_TIMER_STARTED_AT)
                    .putLong(KEY_TIMER_COMPLETED_AT, System.currentTimeMillis())
                    .apply()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ep = EntryPointAccessors.fromApplication(
                            applicationContext,
                            WidgetDependenciesEntryPoint::class.java,
                        )
                        val timeSlot = when (LocalTime.now().hour) {
                            in 0..11  -> "아침"
                            in 12..17 -> "점심"
                            in 18..21 -> "저녁"
                            else      -> "기타"
                        }
                        ep.stretchRepository().insertStretchRecord(
                            date = LocalDate.now().toString(),
                            timeSlot = timeSlot,
                            bodyParts = "[\"전신\"]",
                        )
                        ep.markUserActiveUseCase()("widget_stretch_timer")
                        WidgetUpdateHelper.updateAllWidgets(this@StretchTimerService)
                    } catch (_: Exception) {}
                    stopSelf()
                }
            }
        }.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "스트레칭 타이머",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "1분 스트레칭 진행 중" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(remaining: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.widget_dot_stretch)
            .setContentTitle("스트레칭 진행 중")
            .setContentText("남은 시간: %02d:%02d".format(remaining / 60, remaining % 60))
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun updateNotification(remaining: Int) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(remaining))
    }
}
