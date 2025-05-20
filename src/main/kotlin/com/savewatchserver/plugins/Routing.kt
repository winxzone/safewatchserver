package com.savewatchserver.plugins

import com.savewatchserver.routes.childDeviceRouter
import io.ktor.server.application.*
import com.savewatchserver.routes.userRouter
import com.savewatchserver.routes.childRouter
import com.savewatchserver.routes.deviceDataRouter
import com.savewatchserver.routes.deviceLinkRouter
import com.savewatchserver.routes.notificationRouter
import com.savewatchserver.routes.summaryRouter
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        userRouter()
        childDeviceRouter()
        childRouter()
        notificationRouter()
        deviceLinkRouter()
        deviceDataRouter()
        summaryRouter()
    }
}
