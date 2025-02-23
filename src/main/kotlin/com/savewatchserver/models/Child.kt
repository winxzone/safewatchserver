package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class Child(
    val id: String?,
    val name: String,
    val photoId: String? = null
)
