package com.savewatchserver.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class ChildDevice(
    @BsonId val id: String = ObjectId().toString(),
    val userId: String? = null,
    val childId: String? = null,
    val name: String,
    val deviceId: String? = null,
    val status: String = "unconfirmed",
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = null
)

