package com.savewatchserver.controllers

import com.mongodb.client.MongoCollection
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.MongoDBConnection.database
import com.savewatchserver.collections.ChildDeviceCollection
import com.savewatchserver.collections.UserCollection
import com.savewatchserver.collections.UserCollection.deleteOldPhoto
import com.savewatchserver.models.Child
import com.savewatchserver.constants.ErrorMessage
import com.savewatchserver.models.ExpandedChildProfile
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.bson.Document
import org.bson.types.ObjectId

object ChildController {
    private val bucket = MongoDBConnection.childAvatarsBucket

    suspend fun getChildProfile(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val childId = call.parameters["childId"] ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_CHILD_ID)

        val userDoc = UserCollection.findById(userId)
            ?: return call.respond(HttpStatusCode.NotFound, ErrorMessage.USER_NOT_FOUND)

        val childrenDocs = userDoc.getList("children", Document::class.java)
        val childDoc = childrenDocs.find { it.getObjectId("_id").toHexString() == childId }
        if (childDoc == null) {
            return call.respond(HttpStatusCode.NotFound, ErrorMessage.CHILD_NOT_FOUND)
        }

        val childProfile = Child(
            id = childDoc.getObjectId("_id").toHexString(),
            name = childDoc.getString("name"),
            photoId = childDoc.getString("photoId")
        )

        call.respond(HttpStatusCode.OK, childProfile)

    }

    suspend fun getExpandedChildProfile(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized)

        val childId = call.parameters["childId"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing childId")

        val date = call.request.queryParameters["date"]

        val userDoc = UserCollection.findById(userId)
            ?: return call.respond(HttpStatusCode.NotFound, "User not found")

        val childrenDocs = userDoc.getList("children", Document::class.java)
        val childDoc = childrenDocs.find { it.getObjectId("_id").toHexString() == childId }
            ?: return call.respond(HttpStatusCode.NotFound, "Child not found")

        val name = childDoc.getString("name")
        val photoId = childDoc.getString("photoId")
        val id = childDoc.getObjectId("_id").toHexString()

        // Получаем childDeviceId по childId
        val deviceDoc = ChildDeviceCollection.findByChildId(childId)
        val childDeviceId = deviceDoc?.getObjectId("_id")?.toHexString()

        val summary = try {
            if (childDeviceId != null)
                SummaryController.getDailySummary(childDeviceId, date)
            else null
        } catch (_: NoDataForSummaryException) {
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val response = ExpandedChildProfile(
            id = id,
            name = name,
            photoId = photoId,
            summary = summary
        )

        call.respond(HttpStatusCode.OK, response)
    }


    suspend fun updateChildName(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val childId = call.parameters["childId"] ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_CHILD_ID)

        val newChildName = try {
            call.receive<Map<String, String>>()["newName"] ?: throw IllegalArgumentException("Missing 'newName' field")
        } catch (e: Exception) {
            return call.respond(HttpStatusCode.BadRequest, "${ErrorMessage.INVALID_DATA_FORMAT}: ${e.message}")
        }

        if (newChildName.isBlank()) {
            return call.respond(HttpStatusCode.BadRequest, ErrorMessage.NAME_CANNOT_BE_BLANK)
        }

        val usersCollection: MongoCollection<Document> = database.getCollection("users")

        val filter = Filters.eq("_id", ObjectId(userId))
        val update = Updates.set("children.$[child].name", newChildName)
        val updateOptions = UpdateOptions().arrayFilters(
            listOf(Filters.eq("child._id", ObjectId(childId)))
        )

        val updateResult = usersCollection.updateOne(filter, update, updateOptions)

        if (updateResult.modifiedCount > 0) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Child's name updated successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.CHILD_NOT_FOUND)
        }
    }

    suspend fun getAllChildren(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val userDoc = UserCollection.findById(userId)

        if (userDoc == null) {
            call.respond(HttpStatusCode.NotFound, ErrorMessage.USER_NOT_FOUND)
            return
        }

        val childrenDocs = userDoc.getList("children", Document::class.java)
        val children = childrenDocs.map { doc ->
            Child(
                id = doc.getObjectId("_id").toHexString(),
                name = doc.getString("name"),
                photoId = doc.getString("photoId"),
            )
        }

        call.respond(HttpStatusCode.OK, children)
    }

    suspend fun uploadChildPhoto(call: ApplicationCall) {
        val childId = call.parameters["childId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "Child ID is missing")

        val validChildId = try {
            ObjectId(childId)
        } catch (_: Exception) {
            return call.respond(HttpStatusCode.BadRequest, "Invalid Child ID format")
        }

        // Получаем старый photoId перед обновлением
        val oldPhotoId = UserCollection.findChildPhotoId(validChildId)
        println("Old photo ID before update: $oldPhotoId")

        val multipartData = call.receiveMultipart()
        var uploadedFileId: ObjectId? = null

        multipartData.forEachPart { part ->
            try {
                if (part is PartData.FileItem && part.name == "photo") {
                    val fileBytes = part.provider().readRemaining().readByteArray()
                    if (fileBytes.isEmpty()) {
                        part.dispose()
                        return@forEachPart
                    }

                    val fileName = part.originalFileName ?: "unknown.png"
                    val contentType = part.contentType?.toString() ?: "application/octet-stream"

                    val uploadOptions = GridFSUploadOptions().metadata(
                        Document("childId", validChildId.toHexString())
                            .append("fileName", fileName)
                            .append("contentType", contentType)
                            .append("uploadedAt", System.currentTimeMillis())
                    )

                    println("Uploading file: $fileName (${fileBytes.size} bytes)")

                    uploadedFileId = bucket.uploadFromStream(fileName, fileBytes.inputStream(), uploadOptions)

                    println("Uploaded file to GridFS with ID: $uploadedFileId")

                    part.dispose()
                    return@forEachPart
                }
            } catch (e: Exception) {
                part.dispose()
                println("Exception during file upload: ${e.message}")
                e.printStackTrace()
                return@forEachPart call.respond(HttpStatusCode.InternalServerError, "File upload failed: ${e.message}")
            }

            part.dispose()
        }

        if (uploadedFileId == null) {
            return call.respond(HttpStatusCode.BadRequest, "No valid file provided")
        }

        try {
            val updateSuccess = UserCollection.updateChildPhoto(validChildId, uploadedFileId.toString())

            if (!updateSuccess) {
                return call.respond(HttpStatusCode.InternalServerError, "Failed to update child photo")
            }

            // Удаляем старое фото, если оно было
            if (!oldPhotoId.isNullOrBlank()) {
                deleteOldPhoto(oldPhotoId)
            } else {
                println("No old photo ID found, skipping deletion")
            }

            println("New photo uploaded with fileId: $uploadedFileId")
            call.respond(HttpStatusCode.OK, mapOf("fileId" to uploadedFileId.toString()))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error updating child photo: ${e.message}")
        }
    }

    suspend fun downloadChildPhoto(call: ApplicationCall) {
        val childId = call.parameters["childId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "Child ID is missing")

        try {
            val usersCollection: MongoCollection<Document> = database.getCollection("users")

            val user = usersCollection.find(Filters.elemMatch("children", Filters.eq("_id", ObjectId(childId)))).firstOrNull()
                ?: return call.respond(HttpStatusCode.NotFound, "Child not found")

            val photoId = user.getList("children", Document::class.java)
                ?.find { it.getObjectId("_id") == ObjectId(childId) }
                ?.getString("photoId")
                ?: return call.respond(HttpStatusCode.NotFound, "Profile photo not found for this child")

            val objectId = try {
                ObjectId(photoId)
            } catch (_: IllegalArgumentException) {
                return call.respond(HttpStatusCode.BadRequest, "Invalid photoId format in database")
            }

            val fileStream = MongoDBConnection.childAvatarsBucket.openDownloadStream(objectId)

            call.respondOutputStream(contentType = ContentType.Image.PNG) {
                fileStream.copyTo(this)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error retrieving profile photo: ${e.message}")
        }
    }
}
