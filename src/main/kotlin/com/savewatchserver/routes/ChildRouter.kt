package com.savewatchserver.routes

import com.savewatchserver.controllers.ChildController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.childRouter() {
    authenticate("auth-jwt") {
        route("/child") {
            get("/{childId}/profile") {
                ChildController.getChildProfile(call)
            }
            put("/{childId}/profile/change-name"){
                ChildController.updateChildName(call)
            }
            put("/{childId}/profile/photo/upload"){
                ChildController.uploadChildPhoto(call)
            }
            get("/{childId}/profile/photo"){
                ChildController.downloadChildPhoto(call)
            }
            get("all/photo/{childId}"){
                ChildController.downloadChildPhoto(call)
            }
            get("/all") {
                ChildController.getAllChildren(call)
            }
            get("/{childId}/expanded-profile"){
                ChildController.getExpandedChildProfile(call)
            }
        }
    }
}
