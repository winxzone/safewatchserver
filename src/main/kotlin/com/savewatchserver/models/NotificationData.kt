package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val childDeviceId: String,
    val packageName: String,
    val timestamp: String,
    val emotion: String,
    val confidence: Double
)
