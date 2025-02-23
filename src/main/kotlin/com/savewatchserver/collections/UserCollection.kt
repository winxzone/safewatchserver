package com.savewatchserver.collections

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.Child
import com.savewatchserver.models.user.User
import org.bson.Document
import org.bson.types.ObjectId

object UserCollection {
    private val collection = database.getCollection("users")
    private val bucket = MongoDBConnection.childAvatarsBucket

    fun findById(id: String): Document? {
        return collection.find(Document("_id", ObjectId(id))).firstOrNull()
    }

    fun findByEmail(email: String): Document? {
        return collection.find(Document("email", email)).firstOrNull()
    }

    fun insertUser(user: User): String {
        val doc = Document()
            .append("_id", ObjectId())
            .append("name", user.name)
            .append("email", user.email)
            .append("passwordHash", user.passwordHash)
            .append("children", user.children)

        collection.insertOne(doc)
        return doc.getObjectId("_id").toHexString()
    }

    fun findChildPhotoId(childId: ObjectId): String? {
        val userDoc = collection.find(Filters.eq("children._id", childId))
            .projection(Projections.fields(Projections.include("children")))
            .firstOrNull()

        return userDoc?.getList("children", Document::class.java)
            ?.firstOrNull { it.getObjectId("_id") == childId }
            ?.getString("photoId")
    }


    fun updateChildPhoto(childId: ObjectId, newPhotoId: String): Boolean {
        val updateResult = collection.updateOne(
            Filters.eq("children._id", childId),
            Updates.set("children.$[elem].photoId", newPhotoId),
            UpdateOptions().arrayFilters(listOf(Filters.eq("elem._id", childId)))
        )
        return updateResult.matchedCount > 0
    }

    fun addChildToUser(userId: String, childId: ObjectId, child: Child): Boolean {
        val childDoc = Document("_id", childId)
            .append("name", child.name)
            .append("photoId", child.photoId)
        val updateResult = collection.updateOne(
            Filters.eq("_id", ObjectId(userId)),
            Updates.push("children", childDoc)
        )
        val success = updateResult.matchedCount > 0
        if (success) {
            println("Added child $childId to user $userId")
        } else {
            println("Failed to add child $childId to user $userId - user not found")
        }
        return success
    }

    fun deleteOldPhoto(photoId: String) {
        try {
            val photoObjectId = ObjectId(photoId)
            println("Attempting to delete old photo: $photoObjectId")
            bucket.delete(photoObjectId)
            println("Successfully deleted old photo: $photoObjectId")
        } catch (e: Exception) {
            println("Error deleting old photo: ${e.message}")
        }
    }

}
