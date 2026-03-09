package org.example.openapi.idp

import org.assertj.core.api.Assertions.assertThat
import org.example.openapi.auth.CreateCredentialResult
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
import org.springframework.util.LinkedMultiValueMap
import java.time.LocalDateTime
import java.util.Base64

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TokenIssuanceIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var clientId: String
    private lateinit var clientSecret: String
    private val tenantId = 1L

    @BeforeEach
    fun setUp() {
        val request = CreateCredentialRequest(
            tenantId = tenantId,
            keyName = "token-test-${System.currentTimeMillis()}",
            scopes = listOf("product:read", "order:read"),
            expiresAt = LocalDateTime.now().plusYears(1),
        )
        val response = restTemplate.postForEntity(
            "/internal/v1/credentials",
            request,
            CreateCredentialResult::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        clientId = response.body!!.credentialId
        clientSecret = response.body!!.clientSecret
    }

    @Test
    fun `TC01 유효한 Basic Auth와 client_credentials grant_type으로 JWT 반환`() {
        val response = requestToken(basicAuth(clientId, clientSecret))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body).contains("access_token")
        assertThat(body).contains("Bearer")

        // JWT claims 파싱: header.payload.signature
        val accessToken = extractAccessToken(body)
        val payload = decodeJwtPayload(accessToken)
        assertThat(payload).contains("\"iss\"")
        assertThat(payload).contains("\"sub\":\"$clientId\"")
        assertThat(payload).contains("product:read")
    }

    @Test
    fun `TC02 authorization_code grant_type은 400 반환`() {
        val response = requestToken(basicAuth(clientId, clientSecret), grantType = "authorization_code")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `TC03 Authorization 헤더 없으면 401 반환`() {
        val response = requestToken(authHeader = null)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `TC04 잘못된 Base64 인코딩은 401 반환`() {
        val response = requestToken(authHeader = "Basic !!invalid!!")

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `TC05 존재하지 않는 client_id는 401 반환`() {
        val response = requestToken(basicAuth("nonexistent_client_id_xyz", "any_secret"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `TC06 틀린 client_secret은 401 반환`() {
        val response = requestToken(basicAuth(clientId, "wrong_secret_value"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `TC07 삭제된 클라이언트로 토큰 요청하면 401 반환`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val deleteBody = mapOf("tenantId" to tenantId)
        restTemplate.exchange(
            "/internal/v1/credentials/$clientId",
            HttpMethod.DELETE,
            HttpEntity(deleteBody, headers),
            Void::class.java,
        )

        val response = requestToken(basicAuth(clientId, clientSecret))

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private fun requestToken(
        authHeader: String?,
        grantType: String = "client_credentials",
    ): org.springframework.http.ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            if (authHeader != null) set("Authorization", authHeader)
        }
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", grantType)
        }
        return restTemplate.postForEntity("/oauth/token", HttpEntity(body, headers), String::class.java)
    }

    private fun basicAuth(id: String, secret: String): String {
        val encoded = Base64.getEncoder().encodeToString("$id:$secret".toByteArray())
        return "Basic $encoded"
    }

    private fun extractAccessToken(jsonBody: String): String {
        val regex = """"access_token"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(jsonBody)!!.groupValues[1]
    }

    private fun decodeJwtPayload(jwt: String): String {
        val payloadBase64 = jwt.split(".")[1]
        return String(Base64.getUrlDecoder().decode(payloadBase64))
    }
}
