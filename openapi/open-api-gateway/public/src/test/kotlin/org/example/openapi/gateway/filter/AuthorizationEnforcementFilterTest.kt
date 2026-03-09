package org.example.openapi.gateway.filter

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.example.openapi.gateway.config.RouteActionConfig
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationEnforcementFilterTest {

    private lateinit var filter: AuthorizationEnforcementFilter

    @BeforeAll
    fun setUp() {
        val routeActionConfig = RouteActionConfig()
        routeActionConfig.init()
        filter = AuthorizationEnforcementFilter(routeActionConfig)
    }

    @Test
    fun `TC23 jwt_claims attribute 없음 - 403 FORBIDDEN`() {
        val exchange = makeExchange(HttpMethod.GET, "/api/v1/products")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(403)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC24 매핑 없는 경로 - 403 FORBIDDEN (Default Deny)`() {
        val exchange = makeExchangeWithScopes(HttpMethod.DELETE, "/api/v1/products/1", "product:delete")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(403)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC25 scope 없는 JWT로 GET products - 403 INSUFFICIENT_SCOPE`() {
        val exchange = makeExchangeWithScopes(HttpMethod.GET, "/api/v1/products")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(403)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `TC26 product_read scope로 GET products - 통과`() {
        val exchange = makeExchangeWithScopes(HttpMethod.GET, "/api/v1/products", "product:read")
        val chain = mockk<GatewayFilterChain> { every { filter(any()) } returns Mono.empty() }

        filter.filter(exchange, chain).block()

        verify { chain.filter(exchange) }
    }

    @Test
    fun `TC27 order_와일드카드로 GET orders - 통과`() {
        val exchange = makeExchangeWithScopes(HttpMethod.GET, "/api/v1/orders", "order:*")
        val chain = mockk<GatewayFilterChain> { every { filter(any()) } returns Mono.empty() }

        filter.filter(exchange, chain).block()

        verify { chain.filter(exchange) }
    }

    @Test
    fun `TC28 order_와일드카드로 POST orders 상세 - 통과`() {
        val exchange = makeExchangeWithScopes(HttpMethod.POST, "/api/v1/orders/1", "order:*")
        val chain = mockk<GatewayFilterChain> { every { filter(any()) } returns Mono.empty() }

        filter.filter(exchange, chain).block()

        verify { chain.filter(exchange) }
    }

    @Test
    fun `TC29 product_read scope로 POST products (write 필요) - 403 INSUFFICIENT_SCOPE`() {
        val exchange = makeExchangeWithScopes(HttpMethod.POST, "/api/v1/products", "product:read")
        val chain = mockk<GatewayFilterChain>()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.statusCode?.value()).isEqualTo(403)
        verify(exactly = 0) { chain.filter(any()) }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private fun makeExchange(method: HttpMethod, path: String): MockServerWebExchange {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build())
    }

    private fun makeExchangeWithScopes(
        method: HttpMethod,
        path: String,
        vararg scopes: String,
    ): MockServerWebExchange {
        val exchange = makeExchange(method, path)
        val claims = JWTClaimsSet.Builder()
            .subject("test-client")
            .claim("scope", scopes.joinToString(" "))
            .build()
        exchange.attributes["jwt_claims"] = claims
        return exchange
    }
}
