package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val time: String,
    val sourcePackage: String,
    val title: String,
    val body: String,
    val actionTaken: String
)

object LogHistoryManager {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun addLog(sourcePackage: String, title: String, body: String, actionTaken: String) {
        val entry = LogEntry(
            time = timeFormat.format(Date()),
            sourcePackage = sourcePackage,
            title = title,
            body = body,
            actionTaken = actionTaken
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 20) {
            current.removeLast()
        }
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
