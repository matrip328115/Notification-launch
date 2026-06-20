package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationTriggerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val sourcePackage = sbn.packageName
        if (sourcePackage == packageName) return // Ignore self

        val context = applicationContext
        val isServiceActive = SettingsManager.isServiceActive(context)
        if (!isServiceActive) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString() ?: "No Title"
        val text = extras?.getCharSequence("android.text")?.toString() ?: "No Message Content"

        // Handle possible ongoing / silent notifications filter if needed, but we intercept based on database selection
        serviceScope.launch {
            val db = AppDatabase.getDatabase(context)
            val isMonitored = db.monitoredAppDao().isAppMonitored(sourcePackage)
            
            if (isMonitored) {
                val targetPackage = SettingsManager.getTargetPackage(context)
                
                if (sourcePackage == targetPackage) {
                    LogHistoryManager.addLog(
                        sourcePackage = sourcePackage,
                        title = "Infinite Loop Prevention",
                        body = "Source and target packages are the same ($sourcePackage). Launch aborted.",
                        actionTaken = "Blocked"
                    )
                    return@launch
                }

                // Launch target application
                CoroutineScope(Dispatchers.Main).launch {
                    AutoLaunchHelper.launchTargetApp(context, targetPackage, sourcePackage)
                }
            } else {
                LogHistoryManager.addLog(
                    sourcePackage = sourcePackage,
                    title = title,
                    body = text,
                    actionTaken = "Ignored"
                )
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        LogHistoryManager.addLog(
            sourcePackage = "System Listener",
            title = "Service Bound",
            body = "Notification interceptor is online and ready.",
            actionTaken = "Monitoring"
        )
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        LogHistoryManager.addLog(
            sourcePackage = "System Listener",
            title = "Service Unbound",
            body = "Notification interceptor lost active connection.",
            actionTaken = "Stopped"
        )
    }
}
