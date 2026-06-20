package com.example.data

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "notification_launcher_settings"
    private const val KEY_TARGET_PACKAGE = "target_package_name"
    private const val KEY_TARGET_APP_NAME = "target_app_name"
    private const val KEY_IS_SERVICE_ACTIVE = "is_service_active"

    fun getTargetPackage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_PACKAGE, "") ?: ""
    }

    fun getTargetAppName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_APP_NAME, "") ?: ""
    }

    fun saveTargetApp(context: Context, packageName: String, appName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TARGET_PACKAGE, packageName)
            .putString(KEY_TARGET_APP_NAME, appName)
            .apply()
    }

    fun isServiceActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_SERVICE_ACTIVE, true)
    }

    fun setServiceActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_SERVICE_ACTIVE, active).apply()
    }
}
