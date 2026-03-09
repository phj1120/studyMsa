package org.example.openapi.db.repository

import org.example.openapi.db.entity.ClientSecret
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ClientSecretRepository : JpaRepository<ClientSecret, Long> {

    /**
     * Dual Activation 지원: deleted_at IS NULL AND (expires_at IS NULL OR expires_at > now)
     * Secret Rotation 유예 기간 동안 복수의 active secret이 반환될 수 있음.
     */
    @Query(
        """
        SELECT cs FROM ClientSecret cs
        WHERE cs.clientId = :clientId
          AND cs.deletedAt IS NULL
          AND (cs.expiresAt IS NULL OR cs.expiresAt > :now)
        ORDER BY cs.version DESC
        """,
    )
    fun findActiveSecrets(
        @Param("clientId") clientId: String,
        @Param("now") now: LocalDateTime,
    ): List<ClientSecret>

    fun findTopByClientIdOrderByVersionDesc(clientId: String): ClientSecret?

    fun findAllByClientIdAndDeletedAtIsNull(clientId: String): List<ClientSecret>
}
