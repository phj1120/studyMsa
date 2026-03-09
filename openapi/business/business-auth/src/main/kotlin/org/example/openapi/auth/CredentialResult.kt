package org.example.openapi.auth

import java.time.LocalDateTime

data class CreateCredentialResult(
    val credentialId: String,
    /** 원본 secret. 단 1회만 반환. 이후 조회 불가 */
    val clientSecret: String,
    val keyName: String,
    val scopes: List<String>,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)

data class RotateCredentialResult(
    val credentialId: String,
    /** 신규 secret. 단 1회만 반환 */
    val newClientSecret: String,
    val previousSecretExpiresAt: LocalDateTime,
)

data class ServiceKeyInfo(
    val credentialId: String,
    val keyName: String,
    val status: ServiceKeyStatus,
    val scopes: List<String>,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)

enum class ServiceKeyStatus { ACTIVE, EXPIRING, EXPIRED, DELETED }
