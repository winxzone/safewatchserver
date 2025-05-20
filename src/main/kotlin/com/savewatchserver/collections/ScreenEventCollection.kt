package com.savewatchserver.collections

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import org.bson.Document
import com.mongodb.client.model.InsertManyOptions
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.models.ScreenEvent
import com.savewatchserver.models.ScreenEventType
import org.litote.kmongo.and
import kotlin.collections.map

object ScreenEventCollection {

    private val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("screenEventsData")

    fun insertMany(events: List<ScreenEvent>) {
        if (events.isEmpty()) return

        val documents = events.map {
            Document()
                .append("childDeviceId", it.childDeviceId)
                .append("timestamp", it.timestamp)
                .append("event", it.event.name)
                .append("eventTime", it.timestamp)
        }

        collection.insertMany(documents, InsertManyOptions())
    }

    fun getForDay(childDeviceId: String, date: String): List<ScreenEvent> {
        val start = "${date}T00:00:00"
        val end = "${date}T23:59:59"

        val filter = and(
            eq("childDeviceId", childDeviceId),
            gte("timestamp", start),
            lte("timestamp", end)
        )

        return collection.find(filter).map {
            ScreenEvent(
                childDeviceId = it.getString("childDeviceId"),
                event = ScreenEventType.valueOf(it.getString("event")),
                timestamp = it.getString("timestamp")
            )
        }.toList()
    }


}