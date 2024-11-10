package com.savewatchserver.models.user

import com.savewatchserver.models.Child
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val children: List<Child> = emptyList()
)
