package com.savewatchserver.routes

import com.savewatchserver.controllers.ChildController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.childRouter() {
    authenticate("auth-jwt") {
        route("/child") {
            post("/add") {
                ChildController.addChild(call)
            }
            get("/{childId}/profile") {
                ChildController.getChildProfile(call)
            }
        }
    }
}
