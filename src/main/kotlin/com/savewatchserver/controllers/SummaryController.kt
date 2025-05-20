package com.savewatchserver.controllers

import com.savewatchserver.collections.AppUsageCollection
import com.savewatchserver.collections.NotificationCollection
import com.savewatchserver.collections.ScreenEventCollection
import com.savewatchserver.models.DeviceDailySummary
import com.savewatchserver.models.ScreenEventType
import com.savewatchserver.services.BehaviorAnalyzer
import com.savewatchserver.services.EmotionAnalyzerService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

object SummaryController {

    suspend fun getDailySummary(childDeviceId: String, date: String?): DeviceDailySummary {
        return try {
            val summaryDate = date ?: LocalDate.now().toString()
            val now = LocalDateTime.now()
            val lastUpdated = now.format(DateTimeFormatter.ISO_DATE_TIME)

            val usageList = AppUsageCollection.getForDay(childDeviceId, summaryDate)
            val screenEvents = ScreenEventCollection.getForDay(childDeviceId, summaryDate)
            val notifications = NotificationCollection.getForDay(childDeviceId, summaryDate)

            if (usageList.isEmpty() && screenEvents.isEmpty() && notifications.isEmpty()) {
                throw NoDataForSummaryException("Нет данных для отчета на $summaryDate")
            }

            val totalScreenTimeMs = usageList.sumOf { it.totalTimeForeground }
            val totalScreenTimeMin = (totalScreenTimeMs / 60000).toInt()

            val unlocks = screenEvents.count { it.event == ScreenEventType.UNLOCKED }

            val usedAtNight = screenEvents.any {
                val time = LocalDateTime.parse(it.timestamp)
                val hour = time.hour
                hour < 6 || hour >= 22
            }

            val topApp = usageList.maxByOrNull { it.totalTimeForeground }?.packageName ?: "unknown"

            val messages = notifications.map { "${it.title} ${it.text}" }

            val textEmotion = EmotionAnalyzerService.analyze(messages)

            val behaviorEmotion = BehaviorAnalyzer.analyze(
                unlocks = unlocks,
                notifications = notifications.size,
                totalScreenTimeMin = totalScreenTimeMin,
                usedAtNight = usedAtNight,
                topAppPackage = topApp
            )

            val finalEmotion = if (textEmotion.emotion == behaviorEmotion.emotion) {
                textEmotion.emotion
            } else {
                if (textEmotion.confidence >= 0.8) textEmotion.emotion else behaviorEmotion.emotion
            }

            val finalConfidence = max(textEmotion.confidence, behaviorEmotion.confidence)

            DeviceDailySummary(
                childDeviceId = childDeviceId,
                date = summaryDate,
                lastUpdated = lastUpdated,
                emotion = finalEmotion,
                emotionConfidence = finalConfidence,
                totalScreenTime = totalScreenTimeMs,
                topAppPackage = topApp,
                notificationsCount = notifications.size,
                screenUnlockCount = unlocks,
                usedAtNight = usedAtNight
            )
        } catch (e: Exception) {
            println("Error in getDailySummary: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

class NoDataForSummaryException(message: String) : Exception(message)
