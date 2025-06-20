package com.savewatchserver.collections

import com.mongodb.client.MongoCollection
import com.savewatchserver.MongoDBConnection
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.InsertManyOptions
import com.savewatchserver.models.AppUsageData
import com.savewatchserver.models.NotificationData
import com.savewatchserver.models.ScreenEventData
import org.bson.Document

object DeviceDataCollection {

    private val appUsageCollection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("appUsageData")

    private val notificationEmotionsCollection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("notificationEmotionsData")

    private val screenEventsCollection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("screenEventsData")


    fun insertAppUsage(list: List<AppUsageData>) {
        if (list.isEmpty()) return

        val documents = list.map {
            Document()
                .append("childDeviceId", it.childDeviceId)
                .append("timestamp", it.timestamp)
                .append("packageName", it.packageName)
                .append("totalTimeForeground", it.totalTimeForeground)
                .append("lastTimeUsed", it.lastTimeUsed)
        }

        appUsageCollection.insertMany(documents, InsertManyOptions())
    }


    fun insertNotifications(notifications: List<NotificationData>) {
        if (notifications.isEmpty()) return

        val documents = notifications.map {
            Document()
                .append("childDeviceId", it.childDeviceId)
                .append("packageName", it.packageName)
                .append("timestamp", it.timestamp)
                .append("emotion", it.emotion)
                .append("confidence", it.confidence)
        }

        notificationEmotionsCollection.insertMany(documents, InsertManyOptions())
    }

    fun insertScreenEvent(event: ScreenEventData?) {
        if (event == null) return

        val doc = Document()
            .append("childDeviceId", event.childDeviceId)
            .append("date", event.date)
            .append("screenOnCount", event.screenOnCount)
            .append("screenOffCount", event.screenOffCount)
            .append("unlockCount", event.unlockCount)
            .append("usedAtNight", event.usedAtNight)

        screenEventsCollection.insertOne(doc)
    }


    fun getAppUsageForDay(childDeviceId: String, date: String): List<AppUsageData> {
        val start = "${date}T00:00:00Z"
        val end = "${date}T23:59:59Z"

        val filter = and(
            eq("childDeviceId", childDeviceId),
            gte("timestamp", start),
            lte("timestamp", end)
        )

        return appUsageCollection.find(filter).map {
            AppUsageData(
                childDeviceId = it.getString("childDeviceId") ?: "unknown",
                packageName = it.getString("packageName") ?: "unknown",
                totalTimeForeground = it.getLong("totalTimeForeground") ?: 0L,
                lastTimeUsed = it.getString("lastTimeUsed") ?: "",
                timestamp = it.getString("timestamp") ?: ""
            )
        }.toList()
    }

    fun getNotificationDataForDay(childDeviceId: String, date: String): List<NotificationData> {
        val start = "${date}T00:00:00Z"
        val end = "${date}T23:59:59Z"

        val filter = and(
            eq("childDeviceId", childDeviceId),
            gte("timestamp", start),
            lte("timestamp", end)
        )

        return notificationEmotionsCollection.find(filter).map {
            NotificationData(
                childDeviceId = it.getString("childDeviceId"),
                packageName = it.getString("packageName"),
                timestamp = it.getString("timestamp"),
                emotion = it.getString("emotion"),
                confidence = it.getDouble("confidence") ?: 0.0
            )
        }.toList()
    }

    fun getScreenEventForDay(childDeviceId: String, date: String): ScreenEventData? {
        val doc = screenEventsCollection.find(
            and(
                eq("childDeviceId", childDeviceId),
                eq("date", date)
            )
        ).firstOrNull()

        return doc?.let {
            ScreenEventData(
                childDeviceId = it.getString("childDeviceId"),
                date = it.getString("date"),
                screenOnCount = it.getInteger("screenOnCount") ?: 0,
                screenOffCount = it.getInteger("screenOffCount") ?: 0,
                unlockCount = it.getInteger("unlockCount") ?: 0,
                usedAtNight = it.getBoolean("usedAtNight") ?: false
            )
        }
    }
}
