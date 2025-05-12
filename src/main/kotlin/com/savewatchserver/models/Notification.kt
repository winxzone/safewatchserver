package com.savewatchserver.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class Notification(
    @BsonId
    val id: String = ObjectId().toString(),
    val userId: String? = null,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)