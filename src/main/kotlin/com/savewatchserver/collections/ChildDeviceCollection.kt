package com.savewatchserver.collections

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.models.ChildDevice
import org.bson.Document
import org.bson.types.ObjectId

object ChildDeviceCollection {
    private val collection: MongoCollection<Document> =
        MongoDBConnection.database.getCollection("childDevices")

    fun insertChildDevice(childDevice: ChildDevice) {
        val deviceDoc = Document()
            .append("_id", ObjectId(childDevice.id))
            .append("userId", ObjectId(childDevice.userId))
            .append("name", childDevice.name)
            .append("status", childDevice.status)
            .append("createdAt", childDevice.createdAt)
            .append("deviceId", childDevice.deviceId)
            .append("childId", childDevice.childId)
        collection.insertOne(deviceDoc)
    }

    fun findByAccountId(userId: String): List<Document> {
        return collection.find(eq("userId", ObjectId(userId))).toList()
    }

    fun findByChildId(childId: String): Document? {
        return collection.find(eq("childId", childId)).firstOrNull()
    }

    fun findByIdAndUserId(childDeviceId: String, userId: String): Document? {
        return collection.find(
            Filters.and(
                eq("_id", ObjectId(childDeviceId)),
                eq("userId", ObjectId(userId))
            )
        ).firstOrNull()
    }

    fun updateDeviceStatus(childDeviceId: String, status: String, childId: String? = null) {
        val updates = Updates.combine(
            Updates.set("status", status),
            Updates.set("childId", childId),
            Updates.set("confirmedAt", System.currentTimeMillis())
        )
        collection.updateOne(eq("_id", ObjectId(childDeviceId)), updates)
        println("Updated device $childDeviceId to status $status with childId $childId")
    }

    fun deleteDevice(childDeviceId: String, userId: String): Boolean {
        val result = collection.deleteOne(
            Filters.and(
                eq("_id", ObjectId(childDeviceId)),
                eq("userId", ObjectId(userId))
            )
        )
        return result.deletedCount > 0
    }
}