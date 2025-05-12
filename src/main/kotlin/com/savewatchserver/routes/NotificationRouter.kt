package com.savewatchserver.routes

import com.savewatchserver.controllers.NotificationController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.delete
import io.ktor.server.routing.route

fun Route.notificationRouter() {
    authenticate("auth-jwt"){
        route("/notification") {
            post("create") {
                NotificationController.sendNotification(call)
            }
            get("list") {
                NotificationController.getNotifications(call)
            }
            delete("delete/{notificationId}") {
                NotificationController.deleteNotification(call)
            }

        }
    }
}