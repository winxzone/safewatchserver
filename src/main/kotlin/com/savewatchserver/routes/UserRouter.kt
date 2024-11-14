package com.savewatchserver.routes

import com.savewatchserver.controllers.UserController
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.userRouter() {
    route("/user") {
        // Получение пользователя по ID
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            UserController.getUserById(call, id)
        }

        // Регистрация нового пользователя
        post("/register") {
            UserController.registerUser(call)
        }

        // Авторизация пользователя
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
