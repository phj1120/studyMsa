package org.example.openapi.db.repository

import org.example.openapi.db.entity.OauthClient
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface OauthClientRepository : JpaRepository<OauthClient, Long> {

    fun findByClientIdAndDeletedAtIsNull(clientId: String): OauthClient?

    fun findAllByTenantIdAndDeletedAtIsNull(tenantId: Long): List<OauthClient>

    fun existsByTenantIdAndKeyNameAndDeletedAtIsNull(tenantId: Long, keyName: String): Boolean

    fun findByClientIdAndDeletedAtIsNullAndExpiresAtAfter(
        clientId: String,
        now: LocalDateTime,
    ): OauthClient?
}
