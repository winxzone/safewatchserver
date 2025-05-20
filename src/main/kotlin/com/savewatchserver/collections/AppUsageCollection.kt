package com.savewatchserver.collections

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import org.bson.Document
import com.mongodb.client.model.InsertManyOptions
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.models.AppUsageData
import kotlin.collections.map

object AppUsageCollection {

    private val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("appUsageData")

    fun insertMany(usageList: List<AppUsageData>) {
        if (usageList.isEmpty()) return

        val documents = usageList.map {
            Document()
                .append("childDeviceId", it.childDeviceId)
                .append("timestamp", it.timestamp)
                .append("packageName", it.packageName)
                .append("totalTimeForeground", it.totalTimeForeground)
                .append("lastTimeUsed", it.lastTimeUsed)
        }

        collection.insertMany(documents, InsertManyOptions())
    }

    fun getForDay(childDeviceId: String, date: String): List<AppUsageData> {
        val start = "${date}T00:00:00"
        val end = "${date}T23:59:59"

        val filter = and(
            eq("childDeviceId", childDeviceId),
            gte("timestamp", start),
            lte("timestamp", end)
        )

        return collection.find(filter).map {
            AppUsageData(
                childDeviceId = it.getString("childDeviceId"),
                packageName = it.getString("packageName"),
                totalTimeForeground = it.getLong("totalTimeForeground"),
                lastTimeUsed = it.getString("lastTimeUsed"),
                timestamp = it.getString("timestamp")
            )
        }.toList()
    }


}