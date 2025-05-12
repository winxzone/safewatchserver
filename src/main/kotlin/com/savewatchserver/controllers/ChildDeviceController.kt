package com.savewatchserver.controllers

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.collections.ChildDeviceCollection
import com.savewatchserver.collections.UserCollection
import com.savewatchserver.models.ChildDevice
import com.savewatchserver.constants.ErrorMessage
import com.savewatchserver.models.Child
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.bson.Document
import io.ktor.utils.io.*
import org.bson.types.ObjectId


object ChildDeviceController {

    suspend fun registerChildDevice(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val childDeviceRequest = call.receive<ChildDevice>()

        val existingDevice = ChildDeviceCollection.findByAccountId(userId).firstOrNull {
            it.getString("deviceId") == childDeviceRequest.deviceId
        }

        if (existingDevice != null) {
            call.respond(HttpStatusCode.BadRequest, "Device with this ID already registered.")
            return
        }

        val childDevice = ChildDevice(
            id = ObjectId().toString(),
            userId = userId,
            name = childDeviceRequest.name,
            status = childDeviceRequest.status,
            createdAt = System.currentTimeMillis(),
            deviceId = childDeviceRequest.deviceId,
            childId = null
        )

        ChildDeviceCollection.insertChildDevice(childDevice)

        call.respond(HttpStatusCode.Created, childDevice)
    }

    suspend fun confirmChildDevice(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val childDeviceId = call.parameters["childDeviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_DEVICE_ID)

        val deviceDoc = ChildDeviceCollection.findByIdAndUserId(childDeviceId, userId)

        if (deviceDoc == null) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)
            return
        }

        if (deviceDoc.getString("status") == "confirmed") {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage.DEVICE_ALREADY_CONFIRMED)
            return
        }

        ChildDeviceCollection.updateDeviceStatus(childDeviceId, "confirmed")

        call.respond(HttpStatusCode.OK, mapOf("message" to "Device confirmed. Please link it to a child profile later."))
    }

    suspend fun linkDeviceToChild(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val multipart = call.receiveMultipart()
        var childDeviceId: String? = null
        var name: String? = null
        var photoId: ObjectId? = null

        multipart.forEachPart { part ->
            when {
                part is PartData.FormItem && part.name == "childDeviceId" -> childDeviceId = part.value
                part is PartData.FormItem && part.name == "name" -> name = part.value
                part is PartData.FileItem && part.name == "photo" -> {
                    val fileName = part.originalFileName ?: "photo_${System.currentTimeMillis()}.jpg"
                    val fileBytes = part.provider().toByteArray()
                    if (fileBytes.isNotEmpty()) {
                        val uploadOptions = GridFSUploadOptions().metadata(
                            Document("fileName", fileName)
                                .append("uploadedAt", System.currentTimeMillis())
                                .append("userId", ObjectId(userId))
                        )
                        photoId = MongoDBConnection.childAvatarsBucket.uploadFromStream(fileName, fileBytes.inputStream(), uploadOptions)
                        println("Photo uploaded to GridFS with ID: $photoId")
                    }
                }
            }
            part.dispose()
        }

        if (childDeviceId.isNullOrEmpty() || name.isNullOrEmpty()) {
            println("Missing childDeviceId or name: childDeviceId=$childDeviceId, name=$name")
            return call.respond(HttpStatusCode.BadRequest, "Missing childDeviceId or name")
        }

        println("Processing: childDeviceId=$childDeviceId, name=$name, userId=$userId")

        val deviceDoc = ChildDeviceCollection.findByIdAndUserId(childDeviceId!!, userId)
        if (deviceDoc == null) {
            println("Device not found: $childDeviceId for user: $userId")
            return call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)
        }

        val currentChildId = deviceDoc["childId"]?.toString()
        if (!currentChildId.isNullOrEmpty()) {
            println("Device already linked: $childDeviceId to child: $currentChildId")
            return call.respond(HttpStatusCode.BadRequest, ErrorMessage.DEVICE_ALREADY_LINKED)
        }

        val childId = ObjectId()
        val child = Child(id = childId.toHexString(), name = name!!, photoId = photoId?.toHexString())
        println("Child created: $childId with name: $name")

        val updateResult = UserCollection.addChildToUser(userId, childId, child)
        if (!updateResult) {
            println("User not found: $userId")
            return call.respond(HttpStatusCode.NotFound, "User not found")
        }

        ChildDeviceCollection.updateDeviceStatus(childDeviceId!!, "confirmed", childId.toHexString())
        println("Device $childDeviceId confirmed and linked to child $childId")

        call.respond(HttpStatusCode.OK, mapOf("childId" to childId.toHexString()))
    }

    suspend fun listChildDevices(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val devices = ChildDeviceCollection.findByAccountId(userId)

        if (devices.isEmpty()) {
            return call.respond(HttpStatusCode.NotFound, ErrorMessage.NO_CHILD_DEVICES_FOUND)
        }

        val childDevices = devices.map { doc ->
            ChildDevice(
                id = doc.getObjectId("_id").toHexString(),
                userId = doc.getObjectId("userId").toHexString(),
                name = doc.getString("name") ?: "Unknown",
                status = doc.getString("status") ?: "unknown",
                createdAt = doc.getLong("createdAt"),
                deviceId = doc.getString("deviceId") ?: "N/A",
                childId = doc.getString("childId"),
                confirmedAt = doc.getLong("confirmedAt")
            )

        }

        call.respond(HttpStatusCode.OK, childDevices)
    }

    suspend fun cancelChildDeviceRequest(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val deviceId = call.parameters["deviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_DEVICE_ID)

        val deviceDoc = ChildDeviceCollection.findByIdAndUserId(deviceId, userId)

        if (deviceDoc == null) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)
            return
        }

        val status = deviceDoc.getString("status")
        if (status == "confirmed") {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage.CANNOT_CANCEL_CONFIRMED_DEVICE)
            return
        }
        if (status == "cancelled") {
            return call.respond(HttpStatusCode.BadRequest, ErrorMessage.DEVICE_ALREADY_CANCELLED)
        }

        // Удаляем устройство из базы и проверяем результат
        val deleted = ChildDeviceCollection.deleteDevice(deviceId, userId)
        if (deleted) {
            call.respond(HttpStatusCode.OK, "Device confirmation request cancelled and removed successfully.")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Failed to delete device from database")
        }
    }

    suspend fun deleteDeviceAndChildProfile(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val deviceId = call.parameters["deviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_DEVICE_ID)

        val deviceDoc = ChildDeviceCollection.findByIdAndUserId(deviceId, userId)
            ?: return call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)

        val childId = deviceDoc.getString("childId")

        if (!childId.isNullOrEmpty()) {
            val removed = UserCollection.removeChildFromUser(userId, childId)
            if (removed) {
                println("Профиль ребёнка $childId удалён из массива children пользователя $userId")
            } else {
                println("Не удалось удалить ребёнка $childId из профиля пользователя.")
            }
        }

        val deviceDeleted = ChildDeviceCollection.deleteDevice(deviceId, userId)
        if (deviceDeleted) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Устройство и профиль ребёнка удалены"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Ошибка удаления устройства"))
        }
    }
}
