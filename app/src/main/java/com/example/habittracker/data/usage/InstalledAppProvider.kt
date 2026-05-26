package com.example.habittracker.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
)

@Singleton
class InstalledAppProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun getLaunchableApps(): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryLaunchableActivities(intent)
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val appName = resolveInfo.loadLabel(packageManager)?.toString().orEmpty()
                val packageName = activityInfo.packageName
                if (appName.isBlank() || packageName == context.packageName) return@mapNotNull null
                InstalledAppInfo(
                    appName = appName,
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun PackageManager.queryLaunchableActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
    }
}
