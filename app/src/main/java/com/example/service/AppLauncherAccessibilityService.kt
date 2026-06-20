package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLauncherAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return

        val sourcePackage = event.packageName?.toString() ?: return
        if (sourcePackage == packageName) return

        val context = applicationContext
        val isServiceActive = SettingsManager.isServiceActive(context)
        if (!isServiceActive) return

        serviceScope.launch {
            val db = AppDatabase.getDatabase(context)
            val isMonitored = db.monitoredAppDao().isAppMonitored(sourcePackage)

            if (isMonitored) {
                val targetPackage = SettingsManager.getTargetPackage(context)
                if (sourcePackage == targetPackage) {
                    LogHistoryManager.addLog(
                        sourcePackage = "$sourcePackage (A11y)",
                        title = "Infinite Loop Prevention",
                        body = "Source is identical to target.",
                        actionTaken = "Blocked"
                    )
                    return@launch
                }

                CoroutineScope(Dispatchers.Main).launch {
                    AutoLaunchHelper.launchTargetApp(context, targetPackage, "$sourcePackage (A11y)")
                }
            } else {
                // To keep logs from overwhelming, only log general state
                LogHistoryManager.addLog(
                    sourcePackage = "$sourcePackage (A11y)",
                    title = "Notification Event Intercepted",
                    body = "Accessibility parsed a notification status change.",
                    actionTaken = "Ignored"
                )
            }
        }
    }

    override fun onInterrupt() {
        Log.w("AppLauncherA11y", "Accessibility Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        LogHistoryManager.addLog(
            sourcePackage = "Accessibility Engine",
            title = "A11y Connected",
            body = "Accessibility event pipeline integrated successfully.",
            actionTaken = "Active"
        )
    }
}
