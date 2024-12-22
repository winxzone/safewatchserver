package com.savewatchserver.controllers

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.savewatchserver.MongoDBConnection
import com.savewatchserver.constants.ErrorMessage
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.toByteArray
import org.bson.Document
import org.bson.types.ObjectId
import java.io.ByteArrayInputStream
import java.io.InputStream

object ScreenshotController {

    private val bucket = MongoDBConnection.gridFSBucket

    suspend fun uploadScreenshot(call: ApplicationCall) {
        val childDeviceId = call.parameters["childDeviceId"]
            ?: return call.respond(HttpStatusCode.BadRequest, ErrorMessage.MISSING_DEVICE_ID)

        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: return call.respond(HttpStatusCode.Unauthorized, ErrorMessage.UNAUTHORIZED)

        val deviceDoc = MongoDBConnection.database.getCollection("childDevices").find(
            Document("_id", ObjectId(childDeviceId)).append("accountId", ObjectId(userId))
        ).firstOrNull()

        if (deviceDoc == null) {
            return call.respond(HttpStatusCode.NotFound, ErrorMessage.DEVICE_NOT_FOUND)
        }

        val multipart = call.receiveMultipart()
        var uploadedFileId: ObjectId? = null

        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                val fileName = part.originalFileName ?: "unknown.png"
                val contentType = part.contentType?.toString() ?: "application/octet-stream"

                val uploadOptions = GridFSUploadOptions().metadata(
                    Document("childDeviceId", childDeviceId)
                        .append("fileName", fileName)
                        .append("contentType", contentType)
                        .append("uploadedAt", System.currentTimeMillis())
                )

                // Загружаем файл в GridFS
                val inputStream: InputStream = ByteArrayInputStream(part.provider().toByteArray())
                uploadedFileId = bucket.uploadFromStream(fileName, inputStream, uploadOptions)

                part.dispose()
                return@forEachPart
            }
            part.dispose()
        }

        if (uploadedFileId != null) {
            call.respond(HttpStatusCode.Created, mapOf("fileId" to uploadedFileId.toString()))
        } else {
            call.respond(HttpStatusCode.BadRequest, "No file found in the request")
        }
    }

    suspend fun downloadScreenshot(call: ApplicationCall) {
        val fileId = call.parameters["fileId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "File ID is required")

        val objectId = try {
            ObjectId(fileId)
        } catch (e: IllegalArgumentException) {
            return call.respond(HttpStatusCode.BadRequest, "Invalid fileId format")
        }

        try {
            val fileStream: InputStream = bucket.openDownloadStream(objectId)
            call.respondOutputStream(contentType = ContentType.Image.PNG) {
                fileStream.copyTo(this)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.NotFound, "File not found")
        }
    }
}
