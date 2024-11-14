package com.savewatchserver.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class ChildDevice(
    @BsonId val id: String = ObjectId().toString(),
    val parentId: String? = null,
    val name: String,
    val status: String = "unconfirmed" // Статус устройства: "unconfirmed" или "confirmed"
)

