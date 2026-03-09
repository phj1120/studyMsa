package org.example.openapi.gateway.filter

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.example.openapi.redis.SuspendedClientRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono

class SuspendedAccountFilterTest {

    private lateinit var suspendedClientRepository: SuspendedClientRepository
    private lateinit var filter: SuspendedAccountFilter

    @BeforeEach
    fun setUp() {
        suspendedClientRepository = mockk()
        filter = SuspendedAccountFilter(suspendedClientRepository)
    }

    @Test
    fun `TC20 jwt_claims attribute 없음 - chain 통과`() {
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test")
        // jwt_claims를 주입하지 않음
        val chain = mockk<GatewayFilterChain> { every { filter(any()) } returns Mono.empty() }

        filter.filter(exchange, chain).block()

        verify { chain.filter(exchange) }
        verify(exactly = 0) { suspendedClientRepository.isSuspended(any()) }
    }

    @Test
    fun `TC21 정지된 계정 - 403 ACCOUNT_SUSPENDED`() {
        val exchange = makeExchangeWithClaims("suspended-client")
        every { suspendedClientRepository.isSuspended("suspended-client") } returns true
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(403)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC22 정상 계정 - chain 통과`() {
        val exchange = makeExchangeWithClaims("active-client")
        every { suspendedClientRepository.isSuspended("active-client") } returns false
        val chain = mockk<GatewayFilterChain> { every { filter(any()) } returns Mono.empty() }

        filter.filter(exchange, chain).block()

        verify { chain.filter(exchange) }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private fun makeExchange(method: HttpMethod, path: String): MockServerWebExchange {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build())
    }

    private fun makeExchangeWithClaims(clientId: String): MockServerWebExchange {
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/test")
        val claims = JWTClaimsSet.Builder()
            .subject(clientId)
            .build()
        exchange.attributes["jwt_claims"] = claims
        return exchange
    }
}
