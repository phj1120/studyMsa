package org.example.openapi.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RateLimiterConfig {

    /**
     * Token Bucket 파라미터 (PRD FR-GW-003):
     * - replenishRate: 10  (초당 토큰 리필)
     * - burstCapacity: 50  (버킷 최대 용량)
     * - requestedTokens: 1 (요청당 소모 토큰)
     */
    @Bean
    fun redisRateLimiter(): RedisRateLimiter = RedisRateLimiter(10, 50, 1)
}
