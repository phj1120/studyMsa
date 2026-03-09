package org.example.openapi.idp

import org.assertj.core.api.Assertions.assertThat
import org.example.openapi.auth.CreateCredentialResult
import org.example.openapi.auth.RotateCredentialResult
import org.example.openapi.db.repository.ClientSecretRepository
import org.example.openapi.idp.dto.CreateCredentialRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.LinkedMultiValueMap
import java.time.LocalDateTime
import java.util.Base64

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CredentialLifecycleIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var clientSecretRepository: ClientSecretRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var testClientId: String
    private lateinit var testClientSecret: String
    private lateinit var testKeyName: String
    private val tenantId = 1L

    @BeforeEach
    fun setUp() {
        testKeyName = "lifecycle-test-${System.currentTimeMillis()}"
        val request = CreateCredentialRequest(
            tenantId = tenantId,
            keyName = testKeyName,
            scopes = listOf("product:read", "order:read"),
            expiresAt = LocalDateTime.now().plusYears(1),
        )
        val response = restTemplate.postForEntity(
            "/internal/v1/credentials",
            request,
            CreateCredentialResult::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        testClientId = response.body!!.credentialId
        testClientSecret = response.body!!.clientSecret
    }

    @Test
    fun `TC08 Credential 생성 시 clientId와 secret 반환 - 201`() {
        val keyName = "tc08-${System.currentTimeMillis()}"
        val request = CreateCredentialRequest(
            tenantId = tenantId,
            keyName = keyName,
            scopes = listOf("order:write"),
            expiresAt = LocalDateTime.now().plusYears(1),
        )

        val response = restTemplate.postForEntity(
            "/internal/v1/credentials",
            request,
            CreateCredentialResult::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        with(response.body!!) {
            assertThat(credentialId).startsWith("prod_svc_")
            assertThat(clientSecret).isNotBlank()
            assertThat(this.keyName).isEqualTo(keyName)
            assertThat(scopes).containsExactly("order:write")
        }
    }

    @Test
    fun `TC09 동일 keyName 중복 생성 - 409`() {
        val request = CreateCredentialRequest(
            tenantId = tenantId,
            keyName = testKeyName,
            scopes = listOf("product:read"),
            expiresAt = LocalDateTime.now().plusYears(1),
        )

        val response = restTemplate.postForEntity(
            "/internal/v1/credentials",
            request,
            String::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `TC10 Credential 삭제 후 토큰 요청 - 401`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.exchange(
            "/internal/v1/credentials/$testClientId",
            HttpMethod.DELETE,
            HttpEntity(mapOf("tenantId" to tenantId), headers),
            Void::class.java,
        )

        val response = requestToken(basicAuth(testClientId, testClientSecret))

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `TC11 Rotation 후 새 secret으로 토큰 발급 성공 - 200`() {
        val newSecret = rotateCredential(testClientId)

        val response = requestToken(basicAuth(testClientId, newSecret))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `TC12 Rotation 후 구 secret은 유예기간 내 유효 - 200`() {
        rotateCredential(testClientId)

        // 구 secret으로 토큰 요청 → 유예기간(6h) 내 유효
        val response = requestToken(basicAuth(testClientId, testClientSecret))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `TC13 Rotation 후 유예기간 만료된 구 secret으로 토큰 요청 - 401`() {
        rotateCredential(testClientId)

        // 구 secret(version=1)의 expiresAt을 과거로 직접 조작
        transactionTemplate.execute {
            val oldSecret = clientSecretRepository.findAllByClientIdAndDeletedAtIsNull(testClientId)
                .minByOrNull { it.version }!!
            oldSecret.expiresAt = LocalDateTime.now().minusSeconds(1)
            clientSecretRepository.save(oldSecret)
        }

        val response = requestToken(basicAuth(testClientId, testClientSecret))

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private fun requestToken(authHeader: String): org.springframework.http.ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", authHeader)
        }
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
        }
        return restTemplate.postForEntity("/oauth/token", HttpEntity(body, headers), String::class.java)
    }

    private fun basicAuth(id: String, secret: String): String {
        val encoded = Base64.getEncoder().encodeToString("$id:$secret".toByteArray())
        return "Basic $encoded"
    }

    private fun rotateCredential(credentialId: String): String {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = mapOf("tenantId" to tenantId)
        val response = restTemplate.postForEntity(
            "/internal/v1/credentials/$credentialId/rotate",
            HttpEntity(body, headers),
            RotateCredentialResult::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        return response.body!!.newClientSecret
    }
}
