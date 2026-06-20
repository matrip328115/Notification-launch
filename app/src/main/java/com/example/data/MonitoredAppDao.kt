package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    fun getAllMonitoredAppsFlow(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps")
    suspend fun getAllMonitoredAppsList(): List<MonitoredApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun updateEnabledStatus(packageName: String, isEnabled: Boolean)

    @Delete
    suspend fun delete(app: MonitoredApp)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM monitored_apps WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isAppMonitored(packageName: String): Boolean
}
