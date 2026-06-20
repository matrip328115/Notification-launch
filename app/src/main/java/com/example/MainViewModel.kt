package com.example

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.MonitoredApp
import com.example.data.SettingsManager
import com.example.service.AutoLaunchHelper
import com.example.service.LogEntry
import com.example.service.LogHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisplayAppInfo(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val isMonitored: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getDatabase(context)
    private val repository = AppRepository(db.monitoredAppDao())

    private val _installedApps = MutableStateFlow<List<DisplayAppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isNotificationListenerGranted = MutableStateFlow<Boolean>(false)
    val isNotificationListenerGranted: StateFlow<Boolean> = _isNotificationListenerGranted.asStateFlow()

    private val _isAccessibilityGranted = MutableStateFlow<Boolean>(false)
    val isAccessibilityGranted: StateFlow<Boolean> = _isAccessibilityGranted.asStateFlow()

    private val _isSystemAlertGranted = MutableStateFlow<Boolean>(false)
    val isSystemAlertGranted: StateFlow<Boolean> = _isSystemAlertGranted.asStateFlow()

    private val _isServiceActive = MutableStateFlow<Boolean>(SettingsManager.isServiceActive(context))
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _targetPackage = MutableStateFlow<String>(SettingsManager.getTargetPackage(context))
    val targetPackage: StateFlow<String> = _targetPackage.asStateFlow()

    private val _targetAppName = MutableStateFlow<String>(SettingsManager.getTargetAppName(context))
    val targetAppName: StateFlow<String> = _targetAppName.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = LogHistoryManager.logs

    val displayAppsFlow: StateFlow<List<DisplayAppInfo>> = combine(
        _installedApps,
        repository.allMonitoredAppsFlow,
        _searchQuery
    ) { installed: List<DisplayAppInfo>, monitored: List<com.example.data.MonitoredApp>, query: String ->
        val monitoredPackages = monitored.map { it.packageName }.toSet()
        installed.map { app ->
            app.copy(isMonitored = monitoredPackages.contains(app.packageName))
        }.filter {
            query.isBlank() || it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }.sortedBy { it.appName }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadInstalledApps()
        checkPermissions()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filteredList = mutableListOf<DisplayAppInfo>()

            for (app in apps) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    val appName = app.loadLabel(pm).toString()
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    filteredList.add(
                        DisplayAppInfo(
                            packageName = app.packageName,
                            appName = appName,
                            isSystem = isSystem,
                            isMonitored = false
                        )
                    )
                }
            }

            _installedApps.value = filteredList
        }
    }

    fun checkPermissions() {
        _isNotificationListenerGranted.value = isNotificationListenerServiceEnabled(context)
        _isAccessibilityGranted.value = isAccessibilityServiceEnabled(context)
        _isSystemAlertGranted.value = Settings.canDrawOverlays(context)
        _isServiceActive.value = SettingsManager.isServiceActive(context)
        _targetPackage.value = SettingsManager.getTargetPackage(context)
        _targetAppName.value = SettingsManager.getTargetAppName(context)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleServiceActive() {
        val next = !_isServiceActive.value
        SettingsManager.setServiceActive(context, next)
        _isServiceActive.value = next
        LogHistoryManager.addLog(
            sourcePackage = "User",
            title = if (next) "Service Started" else "Service Paused",
            body = if (next) "Status changed: ${if (next) "ACTIVE" else "PAUSED"}" else "Active trigger intercept suspended.",
            actionTaken = "ConfigUpdated"
        )
    }

    fun setTargetApplication(packageName: String, appName: String) {
        SettingsManager.saveTargetApp(context, packageName, appName)
        _targetPackage.value = packageName
        _targetAppName.value = appName
        LogHistoryManager.addLog(
            sourcePackage = "System Settings",
            title = "Target App Saved",
            body = "Successfully targeted launch of $appName ($packageName).",
            actionTaken = "Updated Parameters"
        )
    }

    fun toggleMonitoredApp(packageName: String, appName: String, isMonitored: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isMonitored) {
                repository.insertOrUpdate(MonitoredApp(packageName, appName))
                LogHistoryManager.addLog(
                    sourcePackage = "DB Manager",
                    title = "Database Registered",
                    body = "Added app: $appName",
                    actionTaken = "Monitoring Active"
                )
            } else {
                repository.deleteByPackage(packageName)
                LogHistoryManager.addLog(
                    sourcePackage = "DB Manager",
                    title = "Database Deregistered",
                    body = "Removed app: $appName",
                    actionTaken = "Monitoring Inactive"
                )
            }
        }
    }

    fun simulateNotificationMatch(sourcePackage: String) {
        val target = _targetPackage.value
        val sourceAppName = _installedApps.value.find { it.packageName == sourcePackage }?.appName ?: sourcePackage
        LogHistoryManager.addLog(
            sourcePackage = sourcePackage,
            title = "Manual Simulator Received",
            body = "Bypassing hardware intercept. Emulating notification trigger from $sourceAppName.",
            actionTaken = "Processing Match"
        )
        if (target.isBlank()) {
            LogHistoryManager.addLog(
                sourcePackage = sourcePackage,
                title = "Simulator Cancelled",
                body = "Trigger simulation failed: Select a target app first.",
                actionTaken = "Aborted"
            )
            return
        }
        viewModelScope.launch {
            AutoLaunchHelper.launchTargetApp(context, target, sourcePackage)
        }
    }

    private fun isNotificationListenerServiceEnabled(context: Context): Boolean {
        val cn = android.content.ComponentName(context, com.example.service.NotificationTriggerService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val cn = android.content.ComponentName(context, com.example.service.AppLauncherAccessibilityService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
