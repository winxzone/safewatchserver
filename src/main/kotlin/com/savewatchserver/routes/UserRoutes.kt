package com.savewatchserver.routes

import com.savewatchserver.controllers.UserController
import com.savewatchserver.models.Child
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.*

fun Route.userRoutes() {
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

        authenticate("auth-jwt") {
            post("/add-child") {
                UserController.addChildForUser(call)
            }
        }

    }
}
