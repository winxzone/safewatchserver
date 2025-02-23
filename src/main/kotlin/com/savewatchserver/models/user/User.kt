package com.savewatchserver.models.user

import com.savewatchserver.models.Child
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class User(
    @BsonId val id: String = ObjectId().toString(),
    val name: String,
    val email: String,
    val passwordHash: String,
    val children: MutableList<Child> = mutableListOf(),
)



