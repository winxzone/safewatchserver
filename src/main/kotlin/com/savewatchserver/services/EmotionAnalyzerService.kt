package com.savewatchserver.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*


object EmotionAnalyzerService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }


    @Serializable
    data class EmotionRequest(val messages: List<String>)

    @Serializable
    data class EmotionResponse(val emotion: String, val confidence: Double)

    suspend fun analyze(messages: List<String>): EmotionResponse {

        if (messages.isEmpty()) {
            return EmotionResponse(emotion = "unknown", confidence = 0.0)
        }

        return try {

            client.post("http://localhost:8000/analyze") {
                contentType(ContentType.Application.Json)
                setBody(EmotionRequest(messages))
            }.body()
        } catch (e: Exception) {
            println("EmotionAnalyzerService error: ${e.message}")
            EmotionResponse(emotion = "unknown", confidence = 0.0)
        }
    }
}