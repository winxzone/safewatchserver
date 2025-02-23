package com.savewatchserver.routes

import com.savewatchserver.controllers.ChildDeviceController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post


fun Route.deviceLinkRouter() {
    authenticate("auth-jwt") {
        post("/child-device/link") {
            ChildDeviceController.linkDeviceToChild(call)
        }
    }
}