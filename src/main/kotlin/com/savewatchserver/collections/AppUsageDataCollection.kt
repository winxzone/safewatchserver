package com.savewatchserver.collections

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.savewatchserver.MongoDBConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

object AppUsageDataCollection {

    private val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("appUsageData")

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