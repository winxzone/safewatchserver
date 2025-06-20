package com.savewatchserver.routes

import com.savewatchserver.controllers.NoDataForSummaryException
import com.savewatchserver.controllers.SummaryController
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.summaryRouter() {
    authenticate("auth-jwt") {
        route("/device-summary") {
            get("/{childDeviceId}/daily") {
                val childDeviceId = call.parameters["childDeviceId"]
                    ?: return@get call.respondText(
                        "Missing childDeviceId",
                        status = HttpStatusCode.BadRequest
                    )
                val date = call.request.queryParameters["date"]

                try {
                    val summary = SummaryController.getDailySummary(childDeviceId, date)
                    call.respond(summary)
                } catch (_: NoDataForSummaryException) {
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to get daily summary", e)
                    call.respond(HttpStatusCode.InternalServerError, "Internal server error")
                }
            }
        }
    }
}