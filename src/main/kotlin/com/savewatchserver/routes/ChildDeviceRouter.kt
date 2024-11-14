package com.savewatchserver.routes

import com.savewatchserver.controllers.ChildDeviceController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.childDeviceRouter() {
    route("/child-device") {
        authenticate("auth-jwt") {
            post("/register") {
                ChildDeviceController.registerChildDevice(call)
            }
            post("/confirm/{deviceId}") {
                ChildDeviceController.confirmChildDevice(call)
            }
            get("/list") {
                ChildDeviceController.listChildDevices(call)
            }
        }
    }
}