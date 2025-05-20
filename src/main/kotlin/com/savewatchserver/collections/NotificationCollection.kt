package com.savewatchserver.collections

import com.savewatchserver.MongoDBConnection
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.InsertManyOptions
import com.savewatchserver.models.NotificationData
import org.bson.Document
import kotlin.collections.map

object NotificationCollection {

    internal val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("notificationsDeviceData")

    fun insertMany(notifications: List<NotificationData>) {
        if (notifications.isEmpty()) return

        val documents = notifications.map {
            Document()
                .append("childDeviceId", it.childDeviceId)
                .append("timestamp", it.timestamp)
                .append("packageName", it.packageName)
                .append("title", it.title)
                .append("text", it.text)
                .append("notificationTime", it.timestamp)
        }

        collection.insertMany(documents, InsertManyOptions())
    }

    fun getForDay(childDeviceId: String, date: String): List<NotificationData> {
        val start = "${date}T00:00:00"
        val end = "${date}T23:59:59"

        val filter = and(
            eq("childDeviceId", childDeviceId),
            gte("timestamp", start),
            lte("timestamp", end)
        )

        return collection.find(filter).map {
            NotificationData(
                childDeviceId = it.getString("childDeviceId"),
                packageName = it.getString("packageName"),
                title = it.getString("title"),
                text = it.getString("text"),
                timestamp = it.getString("timestamp")
            )
        }.toList()
    }

}