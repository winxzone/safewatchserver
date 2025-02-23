package com.savewatchserver.routes

import com.savewatchserver.controllers.UserController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.userRouter() {
    route("/user") {
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            UserController.getUserById(call, id)
        }
        post("/register") {
            UserController.registerUser(call)
        }
        post("/login") {
            UserController.loginUser(call)
        }
        authenticate("auth-jwt") {
            get("/profile") {
                UserController.getUserProfile(call)
            }
        }
    }
}
