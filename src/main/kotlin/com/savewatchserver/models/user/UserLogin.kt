package com.savewatchserver.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserLogin(
    val email: String,
    val password: String
)
