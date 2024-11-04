package com.savewatchserver.models

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Child(
    val id: String = ObjectId().toString(),
    val name: String,
    val age: Int,
    val photoId: String? // ID фото, которое будет загружаться
)
