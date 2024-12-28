package com.savewatchserver.collections

import com.savewatchserver.MongoDBConnection
import com.mongodb.client.MongoCollection
import org.bson.Document

object NotificationCollection {
    private val database = MongoDBConnection.database
    val collection: MongoCollection<Document> = database.getCollection("notifications")
}