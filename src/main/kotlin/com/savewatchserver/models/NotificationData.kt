package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val childDeviceId: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: String
)