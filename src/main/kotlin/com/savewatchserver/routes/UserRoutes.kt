package com.savewatchserver.routes

import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.Role
import com.savewatchserver.models.User
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.Document
import org.bson.types.ObjectId

fun Route.userRoutes() {
    route("/users") {
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            val doc = database.getCollection("users").find(Document("_id", ObjectId(id))).first()

            if (doc != null) {
                val user = User(
                    id = doc.getObjectId("_id").toHexString(),
                    name = doc.getString("name"),
                    email = doc.getString("email"),
                    passwordHash = doc.getString("passwordHash"),
                    role = Role.valueOf(doc.getString("role")),
                    children = doc.getList("children", String::class.java)?.toList()
                )
                call.respond(user)
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }

        // Добавление нового пользователя
        post {
            val user = call.receive<User>()
            val doc = Document()
                .append("_id", ObjectId())
                .append("name", user.name)
                .append("email", user.email)
                .append("passwordHash", user.passwordHash)
                .append("role", user.role.name)
                .append("children", user.children ?: emptyList<String>())

            database.getCollection("users").insertOne(doc)
            call.respond(HttpStatusCode.Created, user)
        }
    }
}
