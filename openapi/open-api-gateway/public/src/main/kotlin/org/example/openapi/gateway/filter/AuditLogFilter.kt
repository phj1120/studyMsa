package org.example.openapi.gateway.filter

import com.nimbusds.jwt.JWTClaimsSet
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * ⑤ Audit Log (Fire-and-Forget)
 * 모든 요청에 request_id(UUID v4)를 생성하고 처리 결과를 비동기 로깅.
 * 로그 기록이 응답 지연을 유발하지 않음.
 */
@Component
class AuditLogFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger("AUDIT")

    override fun getOrder(): Int = -100

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val requestId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        exchange.attributes["request_id"] = requestId
        // 다운스트림에서 추적 가능하도록 헤더에 추가
        val mutated = exchange.mutate()
            .request { it.header("X-Request-Id", requestId) }
            .build()

        return chain.filter(mutated).doFinally {
            // Fire-and-Forget: 응답 완료 후 로깅 (응답 지연 없음)
            val latencyMs = System.currentTimeMillis() - startTime
            val statusCode = mutated.response.statusCode?.value() ?: 0
            val claims = mutated.attributes["jwt_claims"] as? JWTClaimsSet
            val clientId = claims?.subject ?: "unknown"
            val tenantId = claims?.getLongClaim("tid") ?: 0L
            val requiredAction = mutated.attributes["required_action"] as? String ?: ""
            val result = if (statusCode in 200..299) "ALLOW" else "DENY"
            val method = exchange.request.method.name()
            val path = exchange.request.uri.path

            log.info(
                "request_id={} client_id={} tenant_id={} method={} path={} required_action={} result={} status_code={} latency_ms={}",
                requestId, clientId, tenantId, method, path, requiredAction, result, statusCode, latencyMs,
            )
        }
    }
}
