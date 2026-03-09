package org.example.openapi.idp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class IdpSecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // JWKS 엔드포인트: 공개
                auth.requestMatchers("/oauth/.well-known/jwks.json").permitAll()
                // 토큰 발급: 공개 (client credential로 인증)
                auth.requestMatchers("/oauth/token").permitAll()
                // Internal API: 실제 운영에서는 네트워크 레벨 격리 또는 mTLS로 보호
                auth.requestMatchers("/internal/**").permitAll()
                auth.anyRequest().denyAll()
            }
        return http.build()
    }
}
