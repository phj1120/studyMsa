package org.example.openapi.idp.controller

import org.example.openapi.auth.CredentialService
import org.example.openapi.common.exception.ErrorCode
import org.example.openapi.common.exception.OpenApiException
import org.example.openapi.idp.dto.TokenResponse
import org.example.openapi.jwt.JwtProvider
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
class TokenController(
    private val credentialService: CredentialService,
    private val jwtProvider: JwtProvider,
) {

    /**
     * POST /oauth/token
     * Content-Type: application/x-www-form-urlencoded
     * Authorization: Basic base64(client_id:client_secret)
     * grant_type=client_credentials
     */
    @PostMapping("/oauth/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun token(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam("grant_type") grantType: String,
    ): TokenResponse {
        if (grantType != "client_credentials") {
            throw OpenApiException(ErrorCode.INVALID_REQUEST, "unsupported_grant_type")
        }

        val (clientId, clientSecret) = parseBasicAuth(authorization)
        val client = credentialService.authenticate(clientId, clientSecret)

        val tokenResult = jwtProvider.generate(
            clientId = client.clientId,
            tenantId = client.tenantId,
            scopes = client.scopes,
            env = "PROD",
        )

        return TokenResponse(
            accessToken = tokenResult.accessToken,
            tokenType = "Bearer",
            expiresIn = tokenResult.expiresIn,
            scope = tokenResult.scope,
            jti = tokenResult.jti,
        )
    }

    private fun parseBasicAuth(authorization: String): Pair<String, String> {
        if (!authorization.startsWith("Basic ")) {
            throw OpenApiException(ErrorCode.UNAUTHORIZED)
        }
        return try {
            val decoded = String(Base64.getDecoder().decode(authorization.removePrefix("Basic ")))
            val idx = decoded.indexOf(':')
            if (idx < 0) throw IllegalArgumentException()
            decoded.substring(0, idx) to decoded.substring(idx + 1)
        } catch (e: Exception) {
            throw OpenApiException(ErrorCode.UNAUTHORIZED)
        }
    }
}
