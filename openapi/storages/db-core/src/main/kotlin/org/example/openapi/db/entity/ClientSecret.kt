package org.example.openapi.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "client_secrets",
    indexes = [
        Index(name = "idx_client_id", columnList = "client_id"),
        Index(name = "idx_client_version", columnList = "client_id, version"),
        Index(name = "idx_secret_expires_at", columnList = "expires_at"),
    ],
)
class ClientSecret(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** oauth_clients.client_id 참조 */
    @Column(name = "client_id", nullable = false, length = 100)
    val clientId: String,

    /** Rotation 추적용 발급 차수 */
    @Column(name = "version", nullable = false)
    val version: Int,

    /** BCrypt 해시. 원본 저장 불가 */
    @Column(name = "secret_hash", nullable = false, length = 255)
    val secretHash: String,

    @Column(name = "description", length = 50)
    val description: String? = null,

    /** 자연 만료일 또는 Rotation 유예 기간 종료 시점. NULL이면 영구 */
    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
)
