package com.savewatchserver.services

import kotlin.math.pow
import kotlin.math.roundToInt

object BehaviorAnalyzer {

    data class DailyRecord(
        val screenTimeMin: Int,
        val unlocks: Int,
        val notifications: Int,
        val usedAtNight: Boolean,
        val topAppPackage: String
    )

    data class AnalysisResult(
        val behaviorScore: Double,
        val confidence: Double,
        val scientificBasis: String,
        val emotion: String,
        val topConcern: String
    )

    private fun fixedThresholds(): Map<String, Double> {
        return mapOf(
            "screen_time" to 120.0,      // 2 часа
            "unlocks" to 30.0,
            "notifications" to 10.0
        )
    }

    private fun isAppProblematic(packageName: String): Double {
        return when {
            packageName.contains("tiktok", true) -> 1.0
            packageName.contains("instagram", true) -> 1.0
            packageName.contains("facebook", true) -> 0.9
            packageName.contains("youtube", true) -> 0.8
            packageName.contains("whatsapp", true) -> 0.7
            packageName.contains("discord", true) -> 0.6
            packageName.contains("edu", true) || packageName.contains("learn", true) -> 0.2
            else -> 0.3
        }
    }

    fun analyze(currentRecord: DailyRecord): AnalysisResult {
        val thresholds = fixedThresholds()

        val features = mapOf(
            "screen_time" to (currentRecord.screenTimeMin / thresholds["screen_time"]!!).coerceIn(0.0, 1.0),
            "night_usage" to if (currentRecord.usedAtNight) 1.0 else 0.0,
            "unlocks" to (currentRecord.unlocks / thresholds["unlocks"]!!).coerceIn(0.0, 1.0),
            "notifications" to (currentRecord.notifications / thresholds["notifications"]!!).coerceIn(0.0, 1.0),
            "app_type" to isAppProblematic(currentRecord.topAppPackage)
        )

        val weights = mapOf(
            "screen_time" to 0.35,
            "night_usage" to 0.2,
            "unlocks" to 0.15,
            "notifications" to 0.15,
            "app_type" to 0.15
        )

        val score = weights.entries.sumOf { (key, weight) -> features[key]!! * weight }.coerceIn(0.0, 1.0)
        // 75% максимум
        val confidence = (score * 0.75).roundTo(2)

        val emotion = when {
            score >= 0.85 -> "anxiety"
            score >= 0.7 -> "stress"
            score >= 0.5 -> "sadness"
            score >= 0.3 -> "neutral"
            else -> "joy"
        }

        return AnalysisResult(
            behaviorScore = score.roundTo(2),
            confidence = confidence,
            scientificBasis = generateScientificBasis(features),
            emotion = emotion,
            topConcern = detectTopConcern(features)
        )
    }

    private fun generateScientificBasis(features: Map<String, Double>): String {
        val reasons = mutableListOf<String>()
        if (features["screen_time"]!! > 0.7) reasons.add("высокое экранное время")
        if (features["night_usage"] == 1.0) reasons.add("использование ночью")
        if (features["app_type"]!! > 0.8) reasons.add("преобладание соцсетей")
        if (features["notifications"]!! > 0.7) reasons.add("много уведомлений")

        return if (reasons.isNotEmpty()) reasons.joinToString(", ")
        else "стабильное цифровое поведение"
    }

    private fun detectTopConcern(features: Map<String, Double>): String {
        return listOf(
            "screen_time" to features["screen_time"]!!,
            "night_usage" to features["night_usage"]!!,
            "unlocks" to features["unlocks"]!!,
            "notifications" to features["notifications"]!!,
            "app_type" to features["app_type"]!!
        ).maxByOrNull { it.second }?.first ?: "none"
    }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = 10.0.pow(decimals)
        return (this * factor).roundToInt() / factor
    }
}
