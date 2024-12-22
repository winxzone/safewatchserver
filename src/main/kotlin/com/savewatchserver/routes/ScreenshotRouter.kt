package com.savewatchserver.routes

import com.savewatchserver.controllers.ScreenshotController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.screenshotRouter() {
    authenticate("auth-jwt"){
        route("/screenshot") {
            post("/upload/{childDeviceId}") {
                ScreenshotController.uploadScreenshot(call)
            }
            get("/download/{fileId}") {
                ScreenshotController.downloadScreenshot(call)
            }
        }
    }
}
