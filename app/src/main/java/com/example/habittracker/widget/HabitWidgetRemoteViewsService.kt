// 경로: com/example/habittracker/widget/HabitWidgetRemoteViewsService.kt
package com.example.habittracker.widget

import android.content.Intent
import android.widget.RemoteViewsService

/** StackView에 카드 데이터를 제공하는 바운드 서비스 */
class HabitWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        HabitWidgetRemoteViewsFactory(applicationContext, intent)
}
