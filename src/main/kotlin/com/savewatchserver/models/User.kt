package com.savewatchserver.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class User(
    @BsonId val id: String = ObjectId().toString(),
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: Role = Role.PARENT,
    val children: List<String>? = null
)

enum class Role {
    PARENT, CHILD
}
