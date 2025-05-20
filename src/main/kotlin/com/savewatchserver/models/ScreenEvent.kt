package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenEvent(
    val childDeviceId: String,
    val event: ScreenEventType,
    val timestamp: String
)

@Serializable
enum class ScreenEventType {
    SCREEN_ON,
    SCREEN_OFF,
    UNLOCKED
}