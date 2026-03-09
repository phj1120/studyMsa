package org.example.openapi.idp.controller

import org.example.openapi.auth.CreateCredentialCommand
import org.example.openapi.auth.CreateCredentialResult
import org.example.openapi.auth.CredentialService
import org.example.openapi.auth.RotateCredentialCommand
import org.example.openapi.auth.RotateCredentialResult
import org.example.openapi.idp.dto.CreateCredentialRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

/**
 * Internal API. Tenant Admin BE에서만 호출. Public 노출 금지.
 */
@RestController
@RequestMapping("/internal/v1/credentials")
class CredentialController(private val credentialService: CredentialService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateCredentialRequest): CreateCredentialResult =
        credentialService.create(
            CreateCredentialCommand(
                tenantId = request.tenantId,
                keyName = request.keyName,
                scopes = request.scopes,
                expiresAt = request.expiresAt,
            ),
        )

    @PostMapping("/{credentialId}/rotate")
    fun rotate(@PathVariable credentialId: String, @RequestBody body: Map<String, Long>): RotateCredentialResult {
        val tenantId = body["tenantId"] ?: error("tenantId required")
        return credentialService.rotate(
            RotateCredentialCommand(credentialId = credentialId, tenantId = tenantId),
        )
    }

    @DeleteMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable credentialId: String, @RequestBody body: Map<String, Long>) {
        val tenantId = body["tenantId"] ?: error("tenantId required")
        credentialService.delete(
            org.example.openapi.auth.DeleteCredentialCommand(credentialId = credentialId, tenantId = tenantId),
        )
    }
}
