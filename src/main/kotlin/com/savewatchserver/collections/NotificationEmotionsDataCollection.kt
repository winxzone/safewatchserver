package com.savewatchserver.collections

import com.mongodb.client.MongoCollection
import com.savewatchserver.MongoDBConnection
import org.bson.Document
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NotificationEmotionsDataCollection {

    private val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("notificationEmotionsData")

    suspend fun deleteManyByDeviceId(childDeviceId: String): Long =
        withContext(Dispatchers.IO) {
            try {
                val result = collection.deleteMany(
                    Filters.eq("childDeviceId", childDeviceId)
                )
                result.deletedCount
            } catch (e: Exception) {
                println("Помилка: ${e.message}")
                0
            }
        }
}