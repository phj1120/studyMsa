package org.example.openapi.gateway.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import jakarta.annotation.PostConstruct

data class RouteActionMapping(
    val method: String,
    val pathPattern: String,
    val requiredAction: String,
)

/**
 * route-action-config.json에서 Route-Action 매핑을 로드.
 * Default Deny: 매핑 없는 경로는 null 반환 → 403.
 */
@Component
class RouteActionConfig {

    private val pathMatcher = AntPathMatcher()
    private lateinit var mappings: List<RouteActionMapping>

    @PostConstruct
    fun init() {
        val resource = ClassPathResource("route-action-config.json")
        mappings = jacksonObjectMapper().readValue(resource.inputStream)
    }

    fun findRequiredAction(method: String, path: String): String? {
        return mappings.firstOrNull { mapping ->
            mapping.method.equals(method, ignoreCase = true) &&
                pathMatcher.match(mapping.pathPattern, path)
        }?.requiredAction
    }
}
