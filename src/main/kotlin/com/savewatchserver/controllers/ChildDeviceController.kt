package com.savewatchserver.controllers

import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.ChildDevice
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
        val parentId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Parent authentication required")

        val childDeviceRequest = call.receive<ChildDevice>()

        // Создаем объект устройства ребенка и добавляем parentId
        val childDevice = ChildDevice(
            name = childDeviceRequest.name,
            parentId = parentId,
            status = "unconfirmed"
        )

        // Сохраняем устройство в базе данных
        val deviceDoc = Document("_id", ObjectId())
            .append("parentId", ObjectId(childDevice.parentId))
            .append("name", childDevice.name)
            .append("status", childDevice.status)

        database.getCollection("childDevices").insertOne(deviceDoc)

        call.respond(HttpStatusCode.Created, mapOf("deviceId" to deviceDoc["_id"].toString()))
    }


    // Метод подтверждения устройства ребенка
    suspend fun confirmChildDevice(call: ApplicationCall) {
        val parentId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Parent authentication required")

        val deviceId = call.parameters["deviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "Device ID is required")

        // Подтверждение устройства, обновляя статус на "confirmed"
        val updateResult = database.getCollection("childDevices").updateOne(
            Document("_id", ObjectId(deviceId)).append("parentId", ObjectId(parentId)),
            Document("\$set", Document("status", "confirmed"))
        )

        if (updateResult.matchedCount > 0) {
            call.respond(HttpStatusCode.OK, "Device confirmed successfully")
        } else {
            call.respond(HttpStatusCode.NotFound, "Device not found or already confirmed")
        }
    }

    // Метод получения списка устройств ребенка
    suspend fun listChildDevices(call: ApplicationCall) {
        val principal = call.principal<JWTPrincipal>()
        val parentId = principal?.payload?.getClaim("userId")?.asString()

        if (parentId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Parent authentication required")
            return
        }

        // Извлекаем устройства для родителя с учетом parentId
        val devices = database.getCollection("childDevices")
            .find(Document("parentId", ObjectId(parentId)))
            .toList()

        if (devices.isEmpty()) {
            call.respond(HttpStatusCode.NotFound, "No child devices found for this parent")
        } else {
            val childDevices = devices.map { doc ->
                ChildDevice(
                    id = doc.getObjectId("_id").toHexString(),
                    parentId = doc.getObjectId("parentId").toHexString(),
                    name = doc.getString("name"),
                    status = doc.getString("status")
                )
            }
            call.respond(HttpStatusCode.OK, childDevices)
        }
    }


}
