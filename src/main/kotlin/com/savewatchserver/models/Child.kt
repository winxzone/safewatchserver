package com.savewatchserver.models

import kotlinx.serialization.Serializable

@Serializable
data class Child(
    val id: String? = null,
    val name: String,
    val photoId: String? = null
)
