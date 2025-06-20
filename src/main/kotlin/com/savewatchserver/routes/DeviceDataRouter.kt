package com.savewatchserver.routes

import com.savewatchserver.controllers.DeviceDataController
import com.savewatchserver.models.DeviceDataPayload
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.deviceDataRouter() {
    authenticate("auth-jwt") {
        route("/device-data") {

            post {
                try {
                    val payload = call.receive<DeviceDataPayload>()
                    DeviceDataController.handleIncomingData(payload)
                    call.respond(HttpStatusCode.Created, "Device data saved successfully.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, "Error saving device data: ${e.localizedMessage}")
                }
            }

        }
    }
}
