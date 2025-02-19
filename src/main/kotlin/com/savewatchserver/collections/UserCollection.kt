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

    fun findChildrenByAccountId(accountId: String): List<Child> {
        val userDoc = collection.find(Document("_id", ObjectId(accountId))).firstOrNull()
        return userDoc?.getList("children", Document::class.java)?.map {
            Child(
                id = it.getObjectId("_id").toHexString(),
                name = it.getString("name"),
                photoId = it.getString("photoId")
            )
        } ?: emptyList()
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

    fun addChildToUser(userId: String, childDoc: Document): Boolean {
        val existingChild = collection.find(Filters.eq("_id", ObjectId(userId)))
            .projection(Projections.fields(Projections.include("children")))
            .firstOrNull()
            ?.getList("children", Document::class.java)
            ?.find { it.getString("name") == childDoc.getString("name") }

        // Если ребенок уже есть, обновляем его фото и удаляем старое
        if (existingChild != null) {
            val oldPhotoId = existingChild.getString("photoId")
            val newPhotoId = childDoc.getString("photoId")

            val updateResult = collection.updateOne(
                Filters.and(Filters.eq("_id", ObjectId(userId)), Filters.eq("children._id", existingChild.getObjectId("_id"))),
                Updates.set("children.$[elem].photoId", newPhotoId),
                UpdateOptions().arrayFilters(listOf(Filters.eq("elem._id", existingChild.getObjectId("_id"))))
            )

            if (updateResult.matchedCount > 0) {
                if (!oldPhotoId.isNullOrBlank()) {
                    deleteOldPhoto(oldPhotoId)
                }
                return true
            }
            return false
        }

        // Если ребенка нет, просто добавляем
        val updateResult = collection.updateOne(
            Filters.eq("_id", ObjectId(userId)),
            Updates.push("children", childDoc)
        )
        return updateResult.matchedCount > 0
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
