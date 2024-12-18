package com.savewatchserver.controllers

import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.ChildDevice
import com.savewatchserver.models.Child
import com.savewatchserver.constants.ErrorMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.bson.Document
import org.bson.types.ObjectId

object ChildDeviceController {

    // Метод регистрации устройства ребенка
    suspend fun registerChildDevice(call: ApplicationCall) {
        val accountId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val childDeviceRequest = call.receive<ChildDevice>()

        val childDevice = ChildDevice(
            id = ObjectId().toString(),
            accountId = accountId,
            name = childDeviceRequest.name,
            status = childDeviceRequest.status,
            createdAt = System.currentTimeMillis(),
            deviceId = childDeviceRequest.deviceId
        )

        val deviceDoc = Document()
            .append("_id", ObjectId(childDevice.id))
            .append("accountId", ObjectId(childDevice.accountId))
            .append("name", childDevice.name)
            .append("status", childDevice.status)
            .append("createdAt", childDevice.createdAt)
            .append("deviceId", childDevice.deviceId)
        database.getCollection("childDevices").insertOne(deviceDoc)

        call.respond(HttpStatusCode.Created, mapOf("deviceId" to childDevice.id))
    }

    // Метод подтверждения устройства ребенка
    suspend fun confirmChildDevice(call: ApplicationCall) {
        val accountId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val deviceId = call.parameters["deviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_DEVICE_ID)

        val deviceDoc = database.getCollection("childDevices").find(
            Document("_id", ObjectId(deviceId)).append("accountId", ObjectId(accountId))
        ).firstOrNull()

        if (deviceDoc == null) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)
            return
        }

        // Проверяем, что устройство еще не связано с ребенком
        val existingChildId = deviceDoc.getString("childId")
        if (!existingChildId.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage.DEVICE_ALREADY_LINKED)
            return
        }

        // Создаем профиль ребенка
        val childId = ObjectId().toHexString()
        val child = Child(
            id = childId,
            name = deviceDoc.getString("name") ?: "Unnamed Child",
            photoId = "default_photo_id" // фото по умолчанию
        )

        val childDoc = Document()
            .append("_id", ObjectId(childId))
            .append("name", child.name)
            .append("photoId", child.photoId)

        // Обновляем пользователя, добавляя ребенка в список "children"
        val userUpdateResult = database.getCollection("users").updateOne(
            Document("_id", ObjectId(accountId)),
            Document("\$push", Document("children", childDoc))
        )

        if (userUpdateResult.matchedCount.toInt() == 0) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.USER_NOT_FOUND)
            return
        }

        // Обновляем устройство, связывая его с профилем ребенка
        val deviceUpdateResult = database.getCollection("childDevices").updateOne(
            Document("_id", ObjectId(deviceId)),
            Document("\$set", Document("status", "confirmed")
                .append("childId", childId)
                .append("confirmedAt", System.currentTimeMillis()))
        )

        if (deviceUpdateResult.matchedCount > 0) {
            call.respond(HttpStatusCode.OK, mapOf("childId" to childId))
        } else {
            call.respond(HttpStatusCode.InternalServerError, ErrorMessage.DEVICE_CONFIRMATION_FAILED)
        }
    }

    // Метод получения списка устройств ребенка
    suspend fun listChildDevices(call: ApplicationCall) {
        val accountId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val devices = database.getCollection("childDevices")
            .find(Document("accountId", ObjectId(accountId)))
            .toList()

        if (devices.isEmpty()) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.NO_CHILD_DEVICES_FOUND)
        } else {
            val childDevices = devices.map { doc ->
                val status = doc.getString("status")

                if (status == "confirmed") {
                    ChildDevice(
                        id = doc.getObjectId("_id").toHexString(),
                        accountId = doc.getObjectId("accountId").toHexString(),
                        name = doc.getString("name"),
                        status = status,
                        createdAt = doc.getLong("createdAt"),
                        childId = doc.getString("childId"),
                        confirmedAt = doc.getLong("confirmedAt"),
                        deviceId = doc.getString("deviceId")
                    )
                } else {
                    ChildDevice(
                        id = doc.getObjectId("_id").toHexString(),
                        accountId = doc.getObjectId("accountId").toHexString(),
                        name = doc.getString("name"),
                        status = status,
                        createdAt = doc.getLong("createdAt"),
                        deviceId = doc.getString("deviceId")
                    )
                }
            }

            call.respond(HttpStatusCode.OK, childDevices)
        }
    }

    // Метод для отмены запроса на подтверждение устройства
    suspend fun cancelChildDeviceRequest(call: ApplicationCall) {
        val accountId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val deviceId = call.parameters["deviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_DEVICE_ID)

        // Проверяем, существует ли устройство с этим ID и accountId
        val deviceDoc = database.getCollection("childDevices").find(
            Document("_id", ObjectId(deviceId)).append("accountId", ObjectId(accountId))
        ).firstOrNull()

        if (deviceDoc == null) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)
            return
        }

        // Проверяем статус устройства, если оно уже подтверждено, то нельзя отменить
        val status = deviceDoc.getString("status")
        if (status == "confirmed") {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage.CANNOT_CANCEL_CONFIRMED_DEVICE)
            return
        }

        // Обновляем устройство, меняем статус на "cancelled" и очищаем childId
        val updateResult = database.getCollection("childDevices").updateOne(
            Document("_id", ObjectId(deviceId)),
            Document("\$set", Document("status", "cancelled").append("childId", null))
        )

        if (updateResult.matchedCount > 0) {
            call.respond(HttpStatusCode.OK, "Device confirmation request cancelled successfully.")
        } else {
            call.respond(HttpStatusCode.InternalServerError, ErrorMessage.DEVICE_CANCELLATION_FAILED)
        }
    }

}
