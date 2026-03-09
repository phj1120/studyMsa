package org.example.openapi.admin.service

import org.example.openapi.admin.dto.CreateServiceKeyRequest
import org.example.openapi.admin.dto.CreateServiceKeyResponse
import org.example.openapi.admin.dto.RotateServiceKeyResponse
import org.example.openapi.admin.dto.ServiceKeyListResponse
import org.example.openapi.admin.dto.toDto
import org.example.openapi.auth.CreateCredentialCommand
import org.example.openapi.auth.CredentialService
import org.example.openapi.auth.DeleteCredentialCommand
import org.example.openapi.auth.RotateCredentialCommand
import org.springframework.stereotype.Service

@Service
class ServiceKeyService(private val credentialService: CredentialService) {

    fun create(tenantId: Long, request: CreateServiceKeyRequest): CreateServiceKeyResponse {
        val result = credentialService.create(
            CreateCredentialCommand(
                tenantId = tenantId,
                keyName = request.keyName,
                scopes = request.scopeCodes,
                expiresAt = request.expiresAt,
            ),
        )
        // clientSecret은 DB/로그 저장 없이 즉시 FE로 패스스루
        return CreateServiceKeyResponse(
            keyId = result.credentialId,
            clientSecret = result.clientSecret,
            keyName = result.keyName,
            scopeCodes = result.scopes,
            createdAt = result.createdAt,
        )
    }

    fun list(tenantId: Long): ServiceKeyListResponse {
        val items = credentialService.listByTenant(tenantId).map { it.toDto() }
        return ServiceKeyListResponse(items = items, totalCount = items.size)
    }

    fun rotate(tenantId: Long, keyId: String): RotateServiceKeyResponse {
        val result = credentialService.rotate(
            RotateCredentialCommand(credentialId = keyId, tenantId = tenantId),
        )
        return RotateServiceKeyResponse(
            keyId = result.credentialId,
            newClientSecret = result.newClientSecret,
            previousSecretExpiresAt = result.previousSecretExpiresAt,
        )
    }

    fun delete(tenantId: Long, keyId: String) {
        credentialService.delete(DeleteCredentialCommand(credentialId = keyId, tenantId = tenantId))
    }
}
