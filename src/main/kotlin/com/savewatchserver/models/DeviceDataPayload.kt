package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class  DeviceDataPayload(
    val childDeviceId: String,
    val timestamp: String,
    val appUsage: List<AppUsageData>,
    val notifications: List<IncomingNotificationData>,
    val screenEvent: ScreenEventData
)
