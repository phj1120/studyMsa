package org.example.openapi.gateway.filter

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtAuthenticationFilterTest {

    private lateinit var testRsaKey: RSAKey
    private lateinit var httpServer: HttpServer
    private lateinit var filter: JwtAuthenticationFilter

    private val expectedIssuer = "http://test-issuer"

    @BeforeAll
    fun startJwksServer() {
        testRsaKey = RSAKeyGenerator(2048)
            .keyID("test-key")
            .keyUse(KeyUse.SIGNATURE)
            .generate()

        val jwksJson = JWKSet(testRsaKey.toPublicJWK()).toString()

        httpServer = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/jwks") { ex ->
                val body = jwksJson.toByteArray()
                ex.sendResponseHeaders(200, body.size.toLong())
                ex.responseBody.use { it.write(body) }
            }
            start()
        }

        val jwksUrl = "http://localhost:${httpServer.address.port}/jwks"
        filter = JwtAuthenticationFilter(jwksUrl, expectedIssuer)
    }

    @AfterAll
    fun stopJwksServer() {
        httpServer.stop(0)
    }

    @Test
    fun `TC14 Authorization 헤더 없음 - 401`() {
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(401)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC15 Basic Auth 헤더(Bearer 아님) - 401`() {
        val credentials = java.util.Base64.getEncoder().encodeToString("user:pass".toByteArray())
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test", "Authorization" to "Basic $credentials")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(401)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC16 유효한 Bearer JWT - chain 통과 및 jwt_claims attribute 저장`() {
        val token = buildJwt()
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test", "Authorization" to "Bearer $token")
        val chain = mockk<GatewayFilterChain> { every { filter(any()) } returns Mono.empty() }

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode).isNull()
        assertThat(exchange.attributes["jwt_claims"]).isNotNull
        verify { chain.filter(exchange) }
    }

    @Test
    fun `TC17 만료된 JWT - 401`() {
        // Nimbus DefaultJWTClaimsVerifier has 60s clock skew tolerance; use -120s to be safe
        val token = buildJwt(expOffsetSeconds = -120)
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test", "Authorization" to "Bearer $token")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(401)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC18 다른 RSA 키로 서명한 JWT (서명 불일치) - 401`() {
        val otherKey = RSAKeyGenerator(2048).keyID("other-key").keyUse(KeyUse.SIGNATURE).generate()
        val token = buildJwt(rsaKey = otherKey)
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test", "Authorization" to "Bearer $token")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(401)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC19 iss가 다른 JWT - 401`() {
        val token = buildJwt(issuer = "http://malicious-issuer")
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test", "Authorization" to "Bearer $token")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(401)
        verify(exactly = 0) { chain.filter(any()) }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private fun buildJwt(
        rsaKey: RSAKey = testRsaKey,
        issuer: String = expectedIssuer,
        expOffsetSeconds: Long = 3600,
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("test-client")
            .expirationTime(Date(System.currentTimeMillis() + expOffsetSeconds * 1000))
            .build()
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(),
            claims,
        )
        jwt.sign(RSASSASigner(rsaKey))
        return jwt.serialize()
    }

    private fun makeExchange(
        method: HttpMethod,
        path: String,
        vararg headers: Pair<String, String>,
    ): MockServerWebExchange {
        val requestBuilder = MockServerHttpRequest.method(method, path)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        return MockServerWebExchange.from(requestBuilder.build())
    }
}
