package com.savewatchserver.services

object BehaviorAnalyzer {

    //todo: добавить веса и улучшить критерии оценивания

    data class Result(
        val emotion: String,
        val confidence: Double
    )

    fun analyze(
        unlocks: Int,
        notifications: Int,
        totalScreenTimeMin: Int,
        usedAtNight: Boolean,
        topAppPackage: String
    ): Result {
        var score = 0.0

        // Эвристики
        if (unlocks > 50) score += 1.5
        if (notifications > 100) score += 1.0
        if (totalScreenTimeMin > 180) score += 2.0
        if (usedAtNight) score += 2.5

        if (topAppPackage.contains("tiktok") || topAppPackage.contains("youtube")) {
            score += 1.5
        }

        // Эмоции по баллам
        val (emotion, confidence) = when {
            score >= 6.0 -> "стресс" to 0.9
            score >= 4.0 -> "тревожность" to 0.75
            score >= 2.0 -> "раздражение" to 0.6
            else -> "спокойствие" to 0.4
        }

        return Result(emotion, confidence)
    }
}