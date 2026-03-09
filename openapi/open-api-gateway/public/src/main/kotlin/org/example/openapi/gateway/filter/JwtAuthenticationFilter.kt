package org.example.openapi.gateway.filter

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.example.openapi.gateway.util.GatewayResponder
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

/**
 * ① JWT 서명/exp/iss 검증
 * JWKSourceBuilder로 JWKS 캐시(TTL 15분) 적용.
 * 매 요청마다 IdP 호출하지 않음.
 */
@Component
class JwtAuthenticationFilter(
    @Value("\${openapi.gateway.jwks-uri}") jwksUri: String,
    @Value("\${openapi.gateway.expected-issuer}") private val expectedIssuer: String,
) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    private val jwtProcessor = DefaultJWTProcessor<SecurityContext>().apply {
        val jwkSource = JWKSourceBuilder
            .create<SecurityContext>(URI(jwksUri).toURL())
            .cache(15 * 60 * 1000L, 60 * 1000L)  // TTL 15분, refresh 1분
            .build()
        jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
    }

    override fun getOrder(): Int = -200

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val authorization = exchange.request.headers.getFirst("Authorization")

        if (authorization.isNullOrBlank() || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange)
        }

        val token = authorization.removePrefix("Bearer ")

        return try {
            val claims = jwtProcessor.process(token, null)
            validateIssuer(claims)
            exchange.attributes["jwt_claims"] = claims
            chain.filter(exchange)
        } catch (e: Exception) {
            log.debug("JWT validation failed: {}", e.message)
            unauthorized(exchange)
        }
    }

    private fun validateIssuer(claims: JWTClaimsSet) {
        if (claims.issuer != expectedIssuer) {
            throw IllegalArgumentException("Invalid issuer: ${claims.issuer}")
        }
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> =
        GatewayResponder.error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다.")
}
