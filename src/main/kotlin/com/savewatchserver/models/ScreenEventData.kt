package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenEventData(
    val childDeviceId: String,
    val date: String, // Формат: YYYY-MM-DD
    val screenOnCount: Int,
    val screenOffCount: Int,
    val unlockCount: Int,
    val usedAtNight: Boolean
)
