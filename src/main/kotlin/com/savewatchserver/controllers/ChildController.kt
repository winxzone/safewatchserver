package com.savewatchserver.controllers

import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.Child
import com.savewatchserver.models.user.UserProfile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.bson.Document
import org.bson.types.ObjectId

object ChildController {

    suspend fun getChildProfile(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Missing userId in token")

        val childId = call.parameters["childId"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing childId parameter")

        val userDoc = database.getCollection("users").find(Document("_id", ObjectId(userId))).firstOrNull()

        if (userDoc != null) {
            val childDoc = userDoc.getList("children", Document::class.java)?.find { it.getObjectId("_id").toHexString() == childId }

            if (childDoc != null) {
                val childProfile = Child(
                    id = childDoc.getObjectId("_id").toHexString(),
                    name = childDoc.getString("name"),
                    photoId = childDoc.getString("photoId")
                )
                call.respond(HttpStatusCode.OK, childProfile)
            } else {
                call.respond(HttpStatusCode.NotFound, "Child not found")
            }
        } else {
            call.respond(HttpStatusCode.NotFound, "User not found")
        }
    }

    suspend fun addChild(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
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
