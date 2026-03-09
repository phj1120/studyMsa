package org.example.openapi.gateway.filter

import com.nimbusds.jwt.JWTClaimsSet
import org.example.openapi.gateway.util.GatewayResponder
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * ④ Rate Limiting (Token Bucket, Redis Lua Script)
 * Key: RL_KEY:{path}:{client_id}
 * Fail-Open: Redis 장애 시 트래픽 차단하지 않고 통과 (가용성 우선)
 * 초과 시: 429 Too Many Requests + Retry-After 헤더
 */
@Component
class RateLimitFilter(
    private val redisRateLimiter: RedisRateLimiter,
) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getOrder(): Int = -150

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val claims = exchange.attributes["jwt_claims"] as? JWTClaimsSet
            ?: return chain.filter(exchange)
        val clientId = claims.subject ?: return chain.filter(exchange)

        val path = exchange.request.uri.path
        // RedisRateLimiter의 id 파라미터가 Redis 키 접두사로 사용됨
        val rateLimitId = "RL_KEY:$path:$clientId"

        return redisRateLimiter.isAllowed("global", rateLimitId)
            .flatMap { response ->
                if (response.isAllowed) {
                    chain.filter(exchange)
                } else {
                    log.warn("Rate limit exceeded: clientId={}, path={}", clientId, path)
                    exchange.response.headers.set("Retry-After", "1")
                    GatewayResponder.error(
                        exchange,
                        HttpStatus.TOO_MANY_REQUESTS,
                        "TOO_MANY_REQUESTS",
                        "요청 횟수를 초과했습니다.",
                    )
                }
            }
            .onErrorResume { ex ->
                // Fail-Open: Redis 장애 시 요청 통과
                log.warn("Rate limiter Redis error (fail-open): {}", ex.message)
                chain.filter(exchange)
            }
    }
}
