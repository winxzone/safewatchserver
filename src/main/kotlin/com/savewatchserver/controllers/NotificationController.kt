package com.savewatchserver.controllers

import com.mongodb.client.model.Filters
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
            .append("timestamp", notification.timestamp)

        NotificationCollection.collection.insertOne(document)
        call.respondText("Notification created successfully!")
    }

    suspend fun getNotifications(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Unauthorized")

        val documents = NotificationCollection.collection.find(Filters.eq("userId", userId)).toList()
        val notifications = documents.map { document ->
            Notification(
                id = document.getObjectId("_id").toHexString(),
                userId = document.getString("userId"),
                title = document.getString("title"),
                message = document.getString("message"),
                isRead = document.getBoolean("isRead"),
                timestamp = document.getLong("timestamp")
            )
        }
        call.respond(notifications)
    }


    suspend fun deleteNotification(call: ApplicationCall) {
        val notificationId = call.parameters["notificationId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "Notification ID is missing")

        if (!ObjectId.isValid(notificationId)) {
            return call.respond(HttpStatusCode.BadRequest, "Invalid notification ID")
        }

        val result = NotificationCollection.collection.deleteOne(Filters.eq("_id", ObjectId(notificationId)))
        if (result.deletedCount > 0) {
            call.respondText("Notification deleted successfully!")
        } else {
            call.respond(HttpStatusCode.NotFound, "Notification not found")
        }
    }
}