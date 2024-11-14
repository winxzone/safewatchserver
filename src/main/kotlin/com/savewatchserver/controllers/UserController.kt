package com.savewatchserver.controllers

import com.savewatchserver.MongoDBConnection.database
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
import io.ktor.server.routing.RoutingCall
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
                } ?: emptyList()
            )
            call.respond(user)
        } else {
            call.respond(HttpStatusCode.NotFound, "Пользователь не найден")
        }
    }

    suspend fun registerUser(call: ApplicationCall) {
        val registrationData = call.receive<UserRegistration>()
        call.application.log.info("Registration attempt with email: ${registrationData.email}")

        // Валидация данных пользователя
        if (!UserValidator.validateRegistrationData(call, registrationData)) return

        // Проверка на совпадение паролей
        if (registrationData.password != registrationData.confirmPassword) {
            call.respond(HttpStatusCode.BadRequest, "Пароли не совпадают")
            return
        }

        // Проверка на дублирование email
        val existingUser = database.getCollection("users").find(Document("email", registrationData.email)).firstOrNull()
        if (existingUser != null) {
            call.respond(HttpStatusCode.Conflict, "Почта уже зарегистрирована")
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
                // Если пароль правильный, генерируем JWT токен
                val userId = userDoc.getObjectId("_id").toHexString()
                val token = GenerateJwtToken.generateToken(userId)
                // Отправляем ответ с токеном
                call.respond(HttpStatusCode.OK, mapOf("token" to token, "message" to "Login successful"))
            } else {
                // Неверный пароль
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid password"))
            }
        } else {
            // Пользователь не найден
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
        }
    }

    suspend fun getUserProfile(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Missing userId in token")

        val userDoc = database.getCollection("users").find(Document("_id", ObjectId(userId))).firstOrNull()

        if (userDoc != null) {
            val userProfile = UserProfile(
                id = userDoc.getObjectId("_id").toHexString(),
                name = userDoc.getString("name"),
                email = userDoc.getString("email")
            )
            call.respond(HttpStatusCode.OK, userProfile)
        } else {
            call.respond(HttpStatusCode.NotFound, "User not found")
        }
    }

    suspend fun addChildForUser(call: ApplicationCall) {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Missing userId in token")

        // Получаем данные ребенка из запроса
        val child = call.receive<Child>()

        // Создаем документ для ребенка
        val childDoc = Document()
            .append("_id", ObjectId())
            .append("name", child.name)
            .append("photoId", child.photoId)

        // Добавляем ребенка в коллекцию "users"
        val updateResult = database.getCollection("users").updateOne(
            Document("_id", ObjectId(userId)),
            Document("\$push", Document("children", childDoc))
        )

        if (updateResult.matchedCount > 0) {
            call.respond(HttpStatusCode.Created, "Child added successfully")
        } else {
            call.respond(HttpStatusCode.NotFound, "User not found")
        }
    }
}
