package org.example.openapi.auth

import java.time.LocalDateTime

data class CreateCredentialCommand(
    val tenantId: Long,
    val keyName: String,
    val scopes: List<String>,
    val expiresAt: LocalDateTime,
)

data class RotateCredentialCommand(
    val credentialId: String,
    val tenantId: Long,
    val gracePeriodHours: Long = 6,
)

data class DeleteCredentialCommand(
    val credentialId: String,
    val tenantId: Long,
)
