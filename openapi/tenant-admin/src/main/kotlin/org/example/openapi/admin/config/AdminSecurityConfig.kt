package org.example.openapi.admin.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class AdminSecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // Phase 1: 세션 기반 인증은 별도 구현. 현재는 permitAll로 열어둠
                auth.requestMatchers("/api/v1/**").permitAll()
                auth.anyRequest().denyAll()
            }
        return http.build()
    }
}
