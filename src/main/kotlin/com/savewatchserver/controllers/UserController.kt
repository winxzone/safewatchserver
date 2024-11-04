package com.savewatchserver.controllers

import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.Child
import com.savewatchserver.models.user.User
import com.savewatchserver.models.user.UserLogin
import com.savewatchserver.models.user.UserRegistration
import com.savewatchserver.utils.GenerateJwtToken
import com.savewatchserver.utils.PasswordUtils
import com.savewatchserver.validation.UserValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.bson.Document
import org.bson.types.ObjectId

object UserController {

    suspend fun getUserById(call: ApplicationCall, id: String) {
        val doc = database.getCollection("users").find(Document("_id", ObjectId(id))).first()
        if (doc != null) {
            val user = User(
                id = doc.getObjectId("_id").toHexString(),
                name = doc.getString("name"),
                email = doc.getString("email"),
                passwordHash = doc.getString("passwordHash"),
                children = doc.getList("children", Document::class.java)?.map {
                    Child(
                        id = it.getObjectId("_id").toHexString(),
                        name = it.getString("name"),
                        age = it.getInteger("age"),
                        photoId = it.getString("photoId")
                    )
                } ?: emptyList()
            )
            call.respond(user)
        } else {
            call.respond(HttpStatusCode.NotFound, "User not found")
        }
    }

    suspend fun registerUser(call: ApplicationCall) {
        val registrationData = call.receive<UserRegistration>()

        // Валидация данных пользователя
        if (!UserValidator.validateRegistrationData(call, registrationData)) return

        // Проверка на дублирование email
        val existingUser = database.getCollection("users").find(Document("email", registrationData.email)).firstOrNull()
        if (existingUser != null) {
            call.respond(HttpStatusCode.Conflict, "Email already registered")
            return
        }

        // Хешируем пароль
        val hashedPassword = PasswordUtils.hashPassword(registrationData.password)

        // Создаём пользователя
        val user = User(
            name = registrationData.name,
            email = registrationData.email,
            passwordHash = hashedPassword,
            children = emptyList()
        )

        // Сохраняем пользователя в базе данных
        val doc = Document()
            .append("_id", ObjectId())
            .append("name", user.name)
            .append("email", user.email)
            .append("passwordHash", user.passwordHash)
            .append("children", user.children)

        database.getCollection("users").insertOne(doc)

        val userId = doc.getObjectId("_id").toHexString()
        // Отправляем ответ с ID пользователя
        call.respond(HttpStatusCode.Created, mapOf("userId" to userId))
    }

    suspend fun loginUser(call: ApplicationCall) {
        val loginData = call.receive<UserLogin>()
        val userDoc = database.getCollection("users").find(Document("email", loginData.email)).firstOrNull()

        if (userDoc != null) {
            val hashedPassword = userDoc.getString("passwordHash")

            if (PasswordUtils.checkPassword(loginData.password, hashedPassword)) {
                val userId = userDoc.getObjectId("_id").toHexString()
                val token = GenerateJwtToken.generateToken(userId)
                call.respond(HttpStatusCode.OK, mapOf("token" to token))
                call.respond(HttpStatusCode.OK, "Login successful")
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid password")
            }
        } else {
            call.respond(HttpStatusCode.NotFound, "User not found")
        }
    }
}
