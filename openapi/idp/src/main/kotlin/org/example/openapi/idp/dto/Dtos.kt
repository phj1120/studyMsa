package org.example.openapi.idp.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Long,
    val scope: String,
    val jti: String,
)

data class CreateCredentialRequest(
    @field:Positive val tenantId: Long,
    @field:NotBlank val keyName: String,
    @field:NotEmpty val scopes: List<String>,
    @field:Future val expiresAt: LocalDateTime,
)
