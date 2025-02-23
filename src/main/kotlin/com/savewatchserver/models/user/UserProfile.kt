package com.savewatchserver.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
)
