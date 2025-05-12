package com.savewatchserver.validation

import com.savewatchserver.MongoDBConnection
import com.savewatchserver.models.user.UserRegistration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.bson.Document

object UserValidator {

    // Проверка корректности имени
    private fun validateName(name: String): Boolean {
        return name.isNotBlank() && name.length in 2..50
    }

    // Проверка корректности email
    private fun validateEmail(email: String): Boolean {
        return email.matches(
            Regex(
                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
            )
        )
    }

    // Проверка корректности пароля
    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    // Проверка совпадения пароля и подтверждения
    private fun validatePasswordConfirmation(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    // Проверка на существование email в базе данных
    private fun isEmailUnique(email: String): Boolean {
        val userCollection = MongoDBConnection.database.getCollection("users")
        val existingUser = userCollection.find(Document("email", email)).first()
        return existingUser == null
    }

    // Комплексная проверка всех полей регистрации
    suspend fun validateRegistrationData(call: ApplicationCall, registrationData: UserRegistration): Boolean {
        if (!validateName(registrationData.name)) {
            call.respond(HttpStatusCode.BadRequest, "Invalid name: must be between 2 and 50 characters.")
            return false
        }

        if (!validateEmail(registrationData.email)) {
            call.respond(HttpStatusCode.BadRequest, "Invalid email format.")
            return false
        }

        if (!validatePassword(registrationData.password)) {
            call.respond(HttpStatusCode.BadRequest, "Password must be at least 6 characters long.")
            return false
        }

        if (!validatePasswordConfirmation(registrationData.password, registrationData.confirmPassword)) {
            call.respond(HttpStatusCode.BadRequest, "Passwords do not match.")
            return false
        }

        if (!isEmailUnique(registrationData.email)) {
            call.respond(HttpStatusCode.Conflict, "Email is already in use.")
            return false
        }

        return true
    }
}