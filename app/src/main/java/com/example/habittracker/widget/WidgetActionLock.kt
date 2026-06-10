package com.example.habittracker.widget

import android.content.Context

object WidgetActionLock {
    private const val PREFS_NAME = "widget_action_lock"
    private const val KEY_ACTION = "locked_action"
    private const val KEY_UNTIL  = "locked_until"
    private const val LOCK_DURATION_MS = 1_200L

    fun lock(context: Context, actionType: WidgetActionType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTION, actionType.name)
            .putLong(KEY_UNTIL, System.currentTimeMillis() + LOCK_DURATION_MS)
            .apply()
    }

    fun unlock(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_ACTION)
            .remove(KEY_UNTIL)
            .apply()
    }

    fun getLockedAction(context: Context): WidgetActionType? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (System.currentTimeMillis() > prefs.getLong(KEY_UNTIL, 0L)) return null
        val name = prefs.getString(KEY_ACTION, null) ?: return null
        return runCatching { WidgetActionType.valueOf(name) }.getOrNull()
    }
}
