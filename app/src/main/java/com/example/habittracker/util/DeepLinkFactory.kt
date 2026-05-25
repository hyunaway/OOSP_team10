// 경로: com/example/habittracker/util/DeepLinkFactory.kt
package com.example.habittracker.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.habittracker.MainActivity

class DeepLinkFactory {

    fun waterIntent(
        context: Context,
        source: String = "notification",
    ): PendingIntent = buildPendingIntent(
        context,
        uri = "app://habittracker/water?source=$source",
        requestCode = REQUEST_WATER,
    )

    fun mealIntent(
        context: Context,
        type: String,
        source: String = "notification",
    ): PendingIntent = buildPendingIntent(
        context,
        uri = "app://habittracker/meal?type=$type&source=$source",
        requestCode = REQUEST_MEAL,
    )

    fun digitalIntent(
        context: Context,
        appPackage: String,
        interventionId: Long = -1L,
        source: String = "notification",
    ): PendingIntent = buildPendingIntent(
        context,
        uri = "app://habittracker/digital?app=$appPackage&interventionId=$interventionId&source=$source",
        requestCode = REQUEST_DIGITAL,
    )

    fun stretchIntent(
        context: Context,
        trigger: String = "normal",
    ): PendingIntent = buildPendingIntent(
        context,
        uri = "app://habittracker/stretch?trigger=$trigger",
        requestCode = REQUEST_STRETCH,
    )

    private fun buildPendingIntent(
        context: Context,
        uri: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(uri),
            context,
            MainActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val REQUEST_WATER = 1001
        private const val REQUEST_MEAL = 1002
        private const val REQUEST_DIGITAL = 1003
        private const val REQUEST_STRETCH = 1004
    }
}
