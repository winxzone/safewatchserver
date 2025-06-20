package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class ExpandedChildProfile(
    val id: String,
    val name: String,
    val photoId: String?,
    val summary: DeviceDailySummary? = null
)
