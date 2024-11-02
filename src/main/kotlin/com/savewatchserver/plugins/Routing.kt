package com.savewatchserver.plugins

import io.ktor.server.application.*
import com.savewatchserver.routes.userRoutes
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        userRoutes()
    }
}
