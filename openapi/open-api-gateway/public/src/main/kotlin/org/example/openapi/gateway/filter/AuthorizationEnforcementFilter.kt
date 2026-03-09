package org.example.openapi.gateway.filter

import com.nimbusds.jwt.JWTClaimsSet
import org.example.openapi.gateway.config.RouteActionConfig
import org.example.openapi.gateway.util.GatewayResponder
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * ③ Scope 기반 인가 (AuthorizationEnforcementFilter)
 * Route-Action Mapping 기반 Default Deny 정책.
 * 매핑 없는 경로 → 즉시 403.
 */
@Component
class AuthorizationEnforcementFilter(
    private val routeActionConfig: RouteActionConfig,
) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getOrder(): Int = -180

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val claims = exchange.attributes["jwt_claims"] as? JWTClaimsSet ?: return forbidden(exchange)

        val method = exchange.request.method.name()
        val path = exchange.request.uri.path

        val requiredAction = routeActionConfig.findRequiredAction(method, path)
            ?: run {
                log.warn("No route-action mapping found: method={}, path={}", method, path)
                return forbidden(exchange)
            }

        val scopeString = claims.getStringClaim("scope") ?: ""
        val grantedScopes = scopeString.split(" ").filter { it.isNotBlank() }

        if (!isScopeGranted(requiredAction, grantedScopes)) {
            log.warn("Insufficient scope: required={}, granted={}, clientId={}", requiredAction, grantedScopes, claims.subject)
            return GatewayResponder.error(exchange, HttpStatus.FORBIDDEN, "INSUFFICIENT_SCOPE", "요청한 API에 대한 권한이 없습니다.")
        }

        exchange.attributes["required_action"] = requiredAction
        return chain.filter(exchange)
    }

    /**
     * 와일드카드 지원: order:* → order:read, order:write 모두 허용
     */
    private fun isScopeGranted(required: String, granted: List<String>): Boolean {
        return granted.any { scope ->
            scope == required || (scope.endsWith(":*") && required.startsWith(scope.dropLast(2) + ":"))
        }
    }

    private fun forbidden(exchange: ServerWebExchange): Mono<Void> =
        GatewayResponder.error(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.")
}
