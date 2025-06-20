package com.savewatchserver.collections

import com.savewatchserver.MongoDBConnection
import com.mongodb.client.MongoCollection
import org.bson.Document


object NotificationCollection {

    internal val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("notifications")
}