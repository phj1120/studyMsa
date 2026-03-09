package org.example.openapi.auth

import org.example.openapi.common.exception.ErrorCode
import org.example.openapi.common.exception.OpenApiException
import org.example.openapi.db.entity.ClientSecret
import org.example.openapi.db.entity.OauthClient
import org.example.openapi.db.repository.ClientSecretRepository
import org.example.openapi.db.repository.OauthClientRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

@Service
class CredentialService(
    private val oauthClientRepository: OauthClientRepository,
    private val clientSecretRepository: ClientSecretRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()
    private val encoder = BCryptPasswordEncoder(10)

    @Transactional
    fun create(command: CreateCredentialCommand): CreateCredentialResult {
        if (oauthClientRepository.existsByTenantIdAndKeyNameAndDeletedAtIsNull(command.tenantId, command.keyName)) {
            throw OpenApiException(ErrorCode.CREDENTIAL_INVALID_STATUS, "동일한 keyName이 이미 존재합니다.")
        }

        val clientId = generateClientId()
        val rawSecret = generateSecret()
        val secretHash = encoder.encode(rawSecret)

        val client = oauthClientRepository.save(
            OauthClient(
                clientId = clientId,
                tenantId = command.tenantId,
                keyName = command.keyName,
                scopes = command.scopes,
                expiresAt = command.expiresAt,
            ),
        )

        clientSecretRepository.save(
            ClientSecret(
                clientId = clientId,
                version = 1,
                secretHash = secretHash,
                description = "Initial Key",
            ),
        )

        log.info("Credential created: clientId={}, tenantId={}", clientId, command.tenantId)

        return CreateCredentialResult(
            credentialId = client.clientId,
            clientSecret = rawSecret,
            keyName = client.keyName,
            scopes = client.scopes,
            expiresAt = client.expiresAt,
            createdAt = client.createdAt,
        )
    }

    @Transactional
    fun rotate(command: RotateCredentialCommand): RotateCredentialResult {
        val client = findActiveClient(command.credentialId, command.tenantId)

        // Step 1: 현재 최신 버전 secret에 유예 기간 설정
        val currentSecret = clientSecretRepository.findTopByClientIdOrderByVersionDesc(command.credentialId)
            ?: throw OpenApiException(ErrorCode.CREDENTIAL_NOT_FOUND)

        val gracePeriodEnd = LocalDateTime.now().plusHours(command.gracePeriodHours)
        currentSecret.expiresAt = gracePeriodEnd
        clientSecretRepository.save(currentSecret)

        // Step 2: 신규 secret 생성 및 INSERT
        val rawSecret = generateSecret()
        val secretHash = encoder.encode(rawSecret)
        val newVersion = currentSecret.version + 1

        clientSecretRepository.save(
            ClientSecret(
                clientId = command.credentialId,
                version = newVersion,
                secretHash = secretHash,
                description = "Rotation ${LocalDateTime.now().toLocalDate()}",
            ),
        )

        log.info("Credential rotated: clientId={}, newVersion={}", command.credentialId, newVersion)

        return RotateCredentialResult(
            credentialId = client.clientId,
            newClientSecret = rawSecret,
            previousSecretExpiresAt = gracePeriodEnd,
        )
    }

    @Transactional
    fun delete(command: DeleteCredentialCommand) {
        val client = findActiveClient(command.credentialId, command.tenantId)
        client.deletedAt = LocalDateTime.now()
        oauthClientRepository.save(client)
        log.info("Credential deleted: clientId={}", command.credentialId)
    }

    @Transactional(readOnly = true)
    fun listByTenant(tenantId: Long): List<ServiceKeyInfo> {
        return oauthClientRepository.findAllByTenantIdAndDeletedAtIsNull(tenantId).map { client ->
            val status = resolveStatus(client)
            ServiceKeyInfo(
                credentialId = client.clientId,
                keyName = client.keyName,
                status = status,
                scopes = client.scopes,
                expiresAt = client.expiresAt,
                createdAt = client.createdAt,
            )
        }
    }

    /**
     * 토큰 발급 요청 시 인증 검증.
     * Dual Activation: 유효한 모든 secret에 대해 BCrypt 대조.
     */
    @Transactional(readOnly = true)
    fun authenticate(clientId: String, rawSecret: String): OauthClient {
        val now = LocalDateTime.now()

        val client = oauthClientRepository.findByClientIdAndDeletedAtIsNullAndExpiresAtAfter(clientId, now)
            ?: throw OpenApiException(ErrorCode.UNAUTHORIZED)

        val activeSecrets = clientSecretRepository.findActiveSecrets(clientId, now)
        if (activeSecrets.isEmpty()) {
            throw OpenApiException(ErrorCode.UNAUTHORIZED)
        }

        val matched = activeSecrets.any { encoder.matches(rawSecret, it.secretHash) }
        if (!matched) {
            throw OpenApiException(ErrorCode.UNAUTHORIZED)
        }

        return client
    }

    private fun findActiveClient(credentialId: String, tenantId: Long): OauthClient {
        val client = oauthClientRepository.findByClientIdAndDeletedAtIsNull(credentialId)
            ?: throw OpenApiException(ErrorCode.CREDENTIAL_NOT_FOUND)

        if (client.tenantId != tenantId) {
            throw OpenApiException(ErrorCode.FORBIDDEN)
        }
        if (client.deletedAt != null) {
            throw OpenApiException(ErrorCode.CREDENTIAL_INVALID_STATUS)
        }
        return client
    }

    private fun resolveStatus(client: OauthClient): ServiceKeyStatus {
        if (client.deletedAt != null) return ServiceKeyStatus.DELETED
        val now = LocalDateTime.now()
        return if (client.expiresAt.isAfter(now)) ServiceKeyStatus.ACTIVE else ServiceKeyStatus.EXPIRED
    }

    /** CSPRNG 기반 client_id 생성: prod_svc_{Base64URL 20bytes} */
    private fun generateClientId(): String {
        val bytes = ByteArray(20)
        secureRandom.nextBytes(bytes)
        val random = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "prod_svc_$random"
    }

    /** CSPRNG 기반 client_secret 생성: Base64URL 32bytes */
    private fun generateSecret(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
