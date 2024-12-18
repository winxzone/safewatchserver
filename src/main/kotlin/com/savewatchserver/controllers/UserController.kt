package com.savewatchserver.controllers

import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.constants.ErrorMessage
import com.savewatchserver.models.Child
import com.savewatchserver.models.user.User
import com.savewatchserver.models.user.UserLogin
import com.savewatchserver.models.user.UserRegistration
import com.savewatchserver.models.user.UserProfile
import com.savewatchserver.utils.GenerateJwtToken
import com.savewatchserver.utils.PasswordUtils
import com.savewatchserver.validation.UserValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
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
                        photoId = it.getString("photoId")
                    )
                }?.toMutableList() ?: mutableListOf()
            )
            call.respond(user)
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.USER_NOT_FOUND)
        }
    }

    suspend fun registerUser(call: ApplicationCall) {
        val registrationData = call.receive<UserRegistration>()
        call.application.log.info("Registration attempt with email: ${registrationData.email}")

        if (!UserValidator.validateRegistrationData(call, registrationData)) return

        if (registrationData.password != registrationData.confirmPassword) {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage.PASSWORDS_DO_NOT_MATCH)
            return
        }

        val existingUser = database.getCollection("users").find(Document("email", registrationData.email)).firstOrNull()
        if (existingUser != null) {
            call.respond(HttpStatusCode.Conflict, ErrorMessage.EMAIL_ALREADY_REGISTERED)
            return
        }

        val hashedPassword = PasswordUtils.hashPassword(registrationData.password)

        val user = User(
            name = registrationData.name,
            email = registrationData.email,
            passwordHash = hashedPassword,
            children = mutableListOf()
        )

        val doc = Document()
            .append("_id", ObjectId())
            .append("name", user.name)
            .append("email", user.email)
            .append("passwordHash", user.passwordHash)
            .append("children", user.children)

        database.getCollection("users").insertOne(doc)

        val userId = doc.getObjectId("_id").toHexString()
        call.respond(HttpStatusCode.Created, mapOf("userId" to userId))
    }

    suspend fun loginUser(call: ApplicationCall) {
        val loginData = call.receive<UserLogin>()
        val userDoc = database.getCollection("users").find(Document("email", loginData.email)).firstOrNull()

        if (userDoc != null) {
            val hashedPassword = userDoc.getString("passwordHash")

            if (PasswordUtils.checkPassword(loginData.password, hashedPassword)) {
                // Если пароль правильный, генерируем JWT токен
                val userId = userDoc.getObjectId("_id").toHexString()
                val token = GenerateJwtToken.generateToken(userId)
                // Отправляем ответ с токеном
                call.respond(HttpStatusCode.OK, mapOf("token" to token, "message" to "Login successful"))
            } else {
                // Неверный пароль
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to ErrorMessage.INVALID_PASSWORD))
            }
        } else {
            // Пользователь не найден
            call.respond(HttpStatusCode.NotFound, mapOf("error" to ErrorMessage.USER_NOT_FOUND))
        }
    }

    suspend fun getUserProfile(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val userDoc = database.getCollection("users").find(Document("_id", ObjectId(userId))).firstOrNull()

        if (userDoc != null) {
            val userProfile = UserProfile(
                id = userDoc.getObjectId("_id").toHexString(),
                name = userDoc.getString("name"),
                email = userDoc.getString("email")
            )
            call.respond(HttpStatusCode.OK, userProfile)
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.USER_NOT_FOUND)
        }
    }
}
