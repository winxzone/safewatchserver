package com.savewatchserver.routes

import com.savewatchserver.controllers.UserController
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/users") {
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
    }
}
