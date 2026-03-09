package org.example.openapi.db.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "oauth_clients",
    indexes = [
        Index(name = "idx_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_expires_at", columnList = "expires_at"),
        Index(name = "idx_deleted_at", columnList = "deleted_at"),
    ],
)
class OauthClient(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "client_id", unique = true, nullable = false, length = 100)
    val clientId: String,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(name = "key_name", nullable = false, length = 100)
    val keyName: String,

    @Column(name = "scopes", columnDefinition = "JSON", nullable = false)
    @Convert(converter = ScopeListConverter::class)
    val scopes: List<String>,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
)

@Converter
class ScopeListConverter : AttributeConverter<List<String>, String> {
    private val mapper = ObjectMapper()
    private val typeRef = object : TypeReference<List<String>>() {}

    override fun convertToDatabaseColumn(attribute: List<String>): String =
        mapper.writeValueAsString(attribute)

    override fun convertToEntityAttribute(dbData: String): List<String> =
        mapper.readValue(dbData, typeRef)
}
