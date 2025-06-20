package com.savewatchserver.controllers

import com.savewatchserver.collections.DeviceDataCollection
import com.savewatchserver.models.DeviceDataPayload
import com.savewatchserver.models.NotificationData
import com.savewatchserver.services.EmotionAnalyzerService

object DeviceDataController {

    suspend fun handleIncomingData(payload: DeviceDataPayload) {
        try {
            // 1. Сохраняем appUsage
            DeviceDataCollection.insertAppUsage(payload.appUsage)

            // 2. Сохраняем screenEvents
            DeviceDataCollection.insertScreenEvent(payload.screenEvent)

            // 3. Анализируем текст уведомлений, но не сохраняем сами тексты
            if (payload.notifications.isNotEmpty()) {
                val messages = payload.notifications.map { "${it.title} ${it.text}" }

                val emotionResults = EmotionAnalyzerService.analyzeIndividual(messages)

                // 4. Преобразуем в NotificationData для сохранения в БД
                val notificationDataList = payload.notifications.mapIndexed { index, notification ->
                    NotificationData(
                        childDeviceId = notification.childDeviceId,
                        packageName = notification.packageName,
                        timestamp = notification.timestamp,
                        emotion = emotionResults[index].emotion,
                        confidence = emotionResults[index].confidence
                    )
                }

                // 5. Сохраняем только результаты эмоций
                DeviceDataCollection.insertNotifications(notificationDataList)
            }

            println("Device data processed successfully.")
        } catch (e: Exception) {
            println("Error saving device data: ${e.localizedMessage}")
            throw e
        }
    }
}

