package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDailySummary(
    val childDeviceId: String,
    val date: String,
    val lastUpdated: String,
    val emotion: String,
    val emotionConfidence: Double,
    val totalScreenTime: Long,
    val topAppPackage: String,
    val notificationsCount: Int,
    val screenUnlockCount: Int,
    val usedAtNight: Boolean
)