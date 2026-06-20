package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val monitoredAppDao: MonitoredAppDao) {
    val allMonitoredAppsFlow: Flow<List<MonitoredApp>> = monitoredAppDao.getAllMonitoredAppsFlow()

    suspend fun getAllMonitoredAppsList(): List<MonitoredApp> {
        return monitoredAppDao.getAllMonitoredAppsList()
    }

    suspend fun insertOrUpdate(app: MonitoredApp) {
        monitoredAppDao.insertOrUpdate(app)
    }

    suspend fun updateEnabledStatus(packageName: String, isEnabled: Boolean) {
        monitoredAppDao.updateEnabledStatus(packageName, isEnabled)
    }

    suspend fun delete(app: MonitoredApp) {
        monitoredAppDao.delete(app)
    }

    suspend fun deleteByPackage(packageName: String) {
        monitoredAppDao.deleteByPackage(packageName)
    }

    suspend fun isAppMonitored(packageName: String): Boolean {
        return monitoredAppDao.isAppMonitored(packageName)
    }
}
