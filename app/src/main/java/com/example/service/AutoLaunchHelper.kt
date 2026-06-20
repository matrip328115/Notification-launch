package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log

object AutoLaunchHelper {
    private const val TAG = "AutoLaunchHelper"

    fun launchTargetApp(context: Context, targetPackage: String, triggerSource: String) {
        if (targetPackage.isBlank()) {
            LogHistoryManager.addLog(
                sourcePackage = triggerSource,
                title = "Launch Aborted",
                body = "Triggered, but no target app has been configured.",
                actionTaken = "Skipped"
            )
            return
        }

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched application: $targetPackage successfully.")
                LogHistoryManager.addLog(
                    sourcePackage = triggerSource,
                    title = "Target App Triggered",
                    body = "Dispatched launch request for target: $targetPackage",
                    actionTaken = "Launched Successfully"
                )
            } else {
                Log.e(TAG, "No launch intent found for target: $targetPackage")
                LogHistoryManager.addLog(
                    sourcePackage = triggerSource,
                    title = "Launch Failed",
                    body = "Could not locate a launch Intent for $targetPackage",
                    actionTaken = "Error"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching target: $targetPackage", e)
            LogHistoryManager.addLog(
                sourcePackage = triggerSource,
                title = "Launch Exception",
                body = "Exception: ${e.message}",
                actionTaken = "Failure"
            )
        }
    }
}
