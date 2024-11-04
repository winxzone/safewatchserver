package com.savewatchserver.plugins

import com.auth0.jwt.JWT
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.savewatchserver.utils.GenerateJwtToken

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "com.savewatchserver"
            verifier(
                JWT
                    .require(GenerateJwtToken.algorithm)
                    .withIssuer(GenerateJwtToken.issuer)
                    .withAudience(GenerateJwtToken.audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
