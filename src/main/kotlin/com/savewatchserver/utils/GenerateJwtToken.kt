package com.savewatchserver.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object GenerateJwtToken {
    const val issuer = "com.savewatchserver"
    const val audience = "com.savewatchserver.audience"
    private const val secret = "secret_key"
    val algorithm = Algorithm.HMAC256(secret)

    private const val validityInMs = 36_000_000L * 24/// 30 дней

    // Функция генерации токена
    fun generateToken(userId: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)

}
