package com.savewatchserver.controllers

import com.savewatchserver.collections.DeviceDataCollection
import com.savewatchserver.models.DeviceDailySummary
import com.savewatchserver.models.NotificationData
import com.savewatchserver.services.BehaviorAnalyzer
import com.savewatchserver.services.EmotionAnalyzerService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

object SummaryController {

    fun getDailySummary(childDeviceId: String, date: String?): DeviceDailySummary {
        return try {
            val summaryDate = date ?: LocalDate.now().toString()
            val now = LocalDateTime.now()
            val lastUpdated = now.format(DateTimeFormatter.ISO_DATE_TIME)

            val usageList = DeviceDataCollection.getAppUsageForDay(childDeviceId, summaryDate)
            val screenEvent = DeviceDataCollection.getScreenEventForDay(childDeviceId, summaryDate)
            val notificationDataList = DeviceDataCollection.getNotificationDataForDay(childDeviceId, summaryDate)
            println("ChildDeviceId - $childDeviceId")
            println("UsageList size: ${usageList.size}, screenEvent: $screenEvent, notifications: ${notificationDataList.size}")

            if (usageList.isEmpty() && screenEvent == null && notificationDataList.isEmpty()) {
                throw NoDataForSummaryException("Нет данных для отчета на $summaryDate")
            }

            val totalScreenTimeMs = usageList.sumOf { it.totalTimeForeground }
            val totalScreenTimeMin = (totalScreenTimeMs / 60000).toInt()

            val unlocks = screenEvent?.unlockCount ?: 0
            val usedAtNight = screenEvent?.usedAtNight ?: false

            val topApp = usageList.maxByOrNull { it.totalTimeForeground }?.packageName ?: "unknown"

            val textEmotion = if (notificationDataList.isNotEmpty()) {
                analyzeNotificationEmotions(notificationDataList)
            } else {
                EmotionAnalyzerService.EmotionResponse("unknown", 0.0)
            }

            println("Text Emotion: ${textEmotion.emotion}, confidence: ${textEmotion.confidence}")

            val todayRecord = BehaviorAnalyzer.DailyRecord(
                screenTimeMin = totalScreenTimeMin,
                unlocks = unlocks,
                notifications = notificationDataList.size,
                usedAtNight = usedAtNight,
                topAppPackage = topApp
            )

            val behaviorEmotion = BehaviorAnalyzer.analyze(todayRecord)
            println("Behavior Emotion: ${behaviorEmotion.emotion}, confidence: ${behaviorEmotion.confidence}")

            val (finalEmotion, finalConfidence) = combineEmotionResults(textEmotion, behaviorEmotion)

            println("Combined Emotion: $finalEmotion, confidence: $finalConfidence")

            DeviceDailySummary(
                childDeviceId = childDeviceId,
                date = summaryDate,
                lastUpdated = lastUpdated,
                emotion = finalEmotion,
                emotionConfidence = finalConfidence,
                totalScreenTime = totalScreenTimeMs,
                topAppPackage = topApp,
                notificationsCount = notificationDataList.size,
                screenUnlockCount = unlocks,
                usedAtNight = usedAtNight
            )
        } catch (e: Exception) {
            println("Error in getDailySummary: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun analyzeNotificationEmotions(notifications: List<NotificationData>): EmotionAnalyzerService.EmotionResponse {
        if (notifications.isEmpty()) {
            return EmotionAnalyzerService.EmotionResponse("unknown", 0.0)
        }

        // Группируем по эмоциям и считаем средневзвешенную уверенность
        val emotionGroups = notifications
            .groupBy { it.emotion }
            .mapValues { (_, notifs) ->
                val avgConfidence = notifs.map { it.confidence }.average()
                val count = notifs.size
                avgConfidence * count // Вес = уверенность * количество
            }

        // Находим эмоцию с максимальным весом
        val dominantEmotion = emotionGroups.maxByOrNull { it.value }
            ?: return EmotionAnalyzerService.EmotionResponse("unknown", 0.0)

        // Нормализуем уверенность
        val normalizedConfidence = minOf(dominantEmotion.value / notifications.size, 1.0)

        return EmotionAnalyzerService.EmotionResponse(
            emotion = dominantEmotion.key,
            confidence = normalizedConfidence
        )
    }

    private fun combineEmotionResults(
        textEmotion: EmotionAnalyzerService.EmotionResponse,
        behaviorAnalysis: BehaviorAnalyzer.AnalysisResult
    ): Pair<String, Double> {

        // Если текстовая эмоция неизвестна, используем только поведенческий анализ
        if (textEmotion.emotion == "unknown") {
            return behaviorAnalysis.emotion to behaviorAnalysis.confidence
        }

        // Если эмоции совпадают - повышаем уверенность, но не превышаем максимум каждого метода
        if (textEmotion.emotion == behaviorAnalysis.emotion) {
            // Комбинируем уверенности, учитывая что поведенческий анализ ограничен 70%
            val combinedConfidence = minOf(
                (textEmotion.confidence * 0.6 + behaviorAnalysis.confidence * 0.4),
                maxOf(textEmotion.confidence, behaviorAnalysis.confidence) * 1.2,
                1.0
            )
            return textEmotion.emotion to combinedConfidence.roundTo(2)
        }

        // Если эмоции не совпадают - выбираем по уверенности с приоритетом
        return when {
            // Высокая уверенность в тексте (>= 0.8) - доверяем тексту
            textEmotion.confidence >= 0.8 -> {
                textEmotion.emotion to textEmotion.confidence
            }

            // Высокая уверенность в поведении (>= 0.6, так как макс 0.7) - доверяем поведению
            behaviorAnalysis.confidence >= 0.6 -> {
                behaviorAnalysis.emotion to behaviorAnalysis.confidence
            }

            // Средняя уверенность - взвешенное решение с приоритетом текста
            textEmotion.confidence >= 0.5 -> {
                textEmotion.emotion to (textEmotion.confidence * 0.8).roundTo(2)
            }

            // Низкая уверенность в тексте - выбираем по максимальной уверенности
            else -> {
                if (textEmotion.confidence >= behaviorAnalysis.confidence) {
                    textEmotion.emotion to textEmotion.confidence
                } else {
                    behaviorAnalysis.emotion to behaviorAnalysis.confidence
                }
            }
        }
    }

    // Вспомогательная функция для округления
    private fun Double.roundTo(decimals: Int): Double {
        val multiplier = 10.0.pow(decimals.toDouble())
        return (this * multiplier).roundToInt() / multiplier
    }
}

class NoDataForSummaryException(message: String) : Exception(message)
