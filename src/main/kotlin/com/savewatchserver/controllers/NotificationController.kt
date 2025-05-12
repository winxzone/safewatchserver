package com.savewatchserver.controllers

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.savewatchserver.collections.NotificationCollection
import com.savewatchserver.constants.ErrorMessage
import com.savewatchserver.models.Notification
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import org.bson.Document
import org.bson.types.ObjectId

object NotificationController {

    suspend fun sendNotification(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val notification = call.receive<Notification>()
        val document = Document()
            .append("userId", ObjectId(userId))
            .append("title", notification.title)
            .append("message", notification.message)
            .append("isRead", notification.isRead)
            .append("isDeleted", notification.isDeleted)
            .append("timestamp", notification.timestamp)

        NotificationCollection.collection.insertOne(document)
        call.respondText("Notification created successfully!")
    }

    suspend fun getNotifications(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Unauthorized")

        val filter = Filters.and(
            Filters.eq("userId", ObjectId(userId)),
            Filters.eq("isDeleted", false)
        )

        val documents = NotificationCollection.collection.find(filter).toList()

        val notifications = documents.map { document ->

            Notification(
                id = document.getObjectId("_id").toHexString(),
                userId = document.getObjectId("userId").toHexString(),
                title = document.getString("title"),
                message = document.getString("message"),
                isRead = document.getBoolean("isRead"),
                isDeleted = document.getBoolean("isDeleted"),
                timestamp = document.getLong("timestamp")
            )
        }
        call.respond(notifications)
    }

    // Удаление/скрытия уведомления для пользователя(не удаляется с БД)
    suspend fun deleteNotification(call: ApplicationCall) {
        val notificationId = call.parameters["notificationId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "Notification ID is missing")

        if (!ObjectId.isValid(notificationId)) {
            return call.respond(HttpStatusCode.BadRequest, "Invalid notification ID")
        }

        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Unauthorized")

        val filter = Filters.and(
            Filters.eq("_id", ObjectId(notificationId)),
            Filters.eq("userId", ObjectId(userId))
        )

        val update = Updates.set("isDeleted", true)

        val result = NotificationCollection.collection.updateOne(filter, update)
        if (result.modifiedCount > 0) {
            call.respondText("Notification marked as deleted successfully!")
        } else {
            call.respond(HttpStatusCode.NotFound, "Notification not found or already deleted")
        }
    }

}