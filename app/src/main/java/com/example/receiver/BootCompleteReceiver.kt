package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.LogHistoryManager

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompleteReceiver", "Device reboot completed.")
            LogHistoryManager.addLog(
                sourcePackage = "Boot System",
                title = "System Boot Completed",
                body = "Maintained database parameters and active settings.",
                actionTaken = "Ready"
            )
        }
    }
}
