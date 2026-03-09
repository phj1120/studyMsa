package org.example.openapi.gateway.filter

import com.nimbusds.jwt.JWTClaimsSet
import org.example.openapi.gateway.util.GatewayResponder
import org.example.openapi.redis.SuspendedClientRepository
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * ② SUSPENDED 계정 확인 (Redis O(1))
 * JWT가 유효해도 Redis 블랙리스트에 등록된 경우 즉시 403 반환.
 */
@Component
class SuspendedAccountFilter(
    private val suspendedClientRepository: SuspendedClientRepository,
) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getOrder(): Int = -190

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val claims = exchange.attributes["jwt_claims"] as? JWTClaimsSet ?: return chain.filter(exchange)
        val clientId = claims.subject ?: return chain.filter(exchange)

        return Mono.fromCallable { suspendedClientRepository.isSuspended(clientId) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { suspended ->
                if (suspended) {
                    log.warn("Suspended account attempted access: clientId={}", clientId)
                    GatewayResponder.error(exchange, HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "정지된 계정입니다.")
                } else {
                    chain.filter(exchange)
                }
            }
    }
}
