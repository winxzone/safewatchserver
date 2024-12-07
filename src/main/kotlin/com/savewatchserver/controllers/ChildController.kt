package com.savewatchserver.controllers

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.models.Child
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

    // Функция для обновления имени ребенка
    suspend fun updateChildName(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Missing userId in token")

        val childId = call.parameters["childId"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing childId parameter")

        val newChildName = try {
            call.receive<Map<String, String>>()["newName"] ?: throw IllegalArgumentException("Missing 'newName' field")
        } catch (e: Exception) {
            return call.respond(HttpStatusCode.BadRequest, "Invalid data format: ${e.message}")
        }

        if (newChildName.isBlank()) {
            return call.respond(HttpStatusCode.BadRequest, "Name cannot be blank")
        }

        val usersCollection: MongoCollection<Document> = database.getCollection("users")

        val filter = Filters.eq("_id", ObjectId(userId))
        val update = Updates.set("children.$[child].name", newChildName)
        val updateOptions = UpdateOptions().arrayFilters(
            listOf(Filters.eq("child._id", ObjectId(childId)))
        )

        val updateResult = usersCollection.updateOne(filter, update, updateOptions)

        if (updateResult.modifiedCount > 0) {
            call.respond(HttpStatusCode.OK, "Child's name updated successfully")
        } else {
            call.respond(HttpStatusCode.NotFound, "Child not found or user does not exist")
        }
    }

    // Получение всех детей пользователя
    suspend fun getAllChildren(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, "Missing userId in token")

        val userDoc = database.getCollection("users").find(Document("_id", ObjectId(userId))).firstOrNull()

        if (userDoc != null) {
            val childrenDocs = userDoc.getList("children", Document::class.java)

            if (childrenDocs.isNotEmpty()) {
                val children = childrenDocs.map { doc ->
                    Child(
                        id = doc.getObjectId("_id").toHexString(),
                        name = doc.getString("name"),
                        photoId = doc.getString("photoId")
                    )
                }
                call.respond(HttpStatusCode.OK, children)
            } else {
                call.respond(HttpStatusCode.OK, emptyList<Child>()) // Возвращаем пустой список
            }
        } else {
            call.respond(HttpStatusCode.NotFound, "User not found")
        }
    }
}
