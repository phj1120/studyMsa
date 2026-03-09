package org.example.openapi.admin.controller

import org.example.openapi.admin.dto.CreateServiceKeyRequest
import org.example.openapi.admin.dto.CreateServiceKeyResponse
import org.example.openapi.admin.dto.RotateServiceKeyResponse
import org.example.openapi.admin.dto.ServiceKeyListResponse
import org.example.openapi.admin.service.ServiceKeyService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/service-keys")
class ServiceKeyController(private val serviceKeyService: ServiceKeyService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable tenantId: Long,
        @Valid @RequestBody request: CreateServiceKeyRequest,
    ): CreateServiceKeyResponse = serviceKeyService.create(tenantId, request)

    @GetMapping
    fun list(@PathVariable tenantId: Long): ServiceKeyListResponse =
        serviceKeyService.list(tenantId)

    @PostMapping("/{keyId}/rotate")
    fun rotate(
        @PathVariable tenantId: Long,
        @PathVariable keyId: String,
    ): RotateServiceKeyResponse = serviceKeyService.rotate(tenantId, keyId)

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable keyId: String,
    ) = serviceKeyService.delete(tenantId, keyId)
}
