package org.example.openapi.admin.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.example.openapi.auth.ServiceKeyInfo
import org.example.openapi.auth.ServiceKeyStatus
import java.time.LocalDateTime

data class CreateServiceKeyRequest(
    @field:NotBlank val keyName: String,
    @field:NotEmpty val scopeCodes: List<String>,
    @field:Future val expiresAt: LocalDateTime,
)

data class CreateServiceKeyResponse(
    val keyId: String,
    /** 원본 secret. 단 1회만 노출 */
    val clientSecret: String,
    val keyName: String,
    val scopeCodes: List<String>,
    val createdAt: LocalDateTime,
)

data class RotateServiceKeyResponse(
    val keyId: String,
    val newClientSecret: String,
    val previousSecretExpiresAt: LocalDateTime,
    val message: String = "새 Secret이 활성화되었습니다. 기존 Secret은 설정된 유예 시간 후 만료됩니다.",
)

data class ServiceKeyListResponse(
    val items: List<ServiceKeyItemDto>,
    val totalCount: Int,
)

data class ServiceKeyItemDto(
    val keyId: String,
    val keyName: String,
    val status: ServiceKeyStatus,
    val scopeCodes: List<String>,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)

fun ServiceKeyInfo.toDto() = ServiceKeyItemDto(
    keyId = credentialId,
    keyName = keyName,
    status = status,
    scopeCodes = scopes,
    expiresAt = expiresAt,
    createdAt = createdAt,
)
