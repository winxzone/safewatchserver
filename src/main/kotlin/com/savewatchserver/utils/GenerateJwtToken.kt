package com.savewatchserver.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object GenerateJwtToken {
    const val issuer = "com.savewatchserver"
    const val audience = "com.savewatchserver.audience"
    private const val secret = "secret_key"
    val algorithm = Algorithm.HMAC256(secret)
    private const val validityInMs = 36_000_00 * 24 // 24 часа

    // Функция генерации токена
    fun generateToken(userId: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)

    // Функция проверки токена
    fun validateToken(token: String): Boolean {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
                .verify(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
