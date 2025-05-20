package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageData(
    val childDeviceId: String,
    val packageName: String,
    val totalTimeForeground: Long,
    val lastTimeUsed: String,
    val timestamp: String
)
