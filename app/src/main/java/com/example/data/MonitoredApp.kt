package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
