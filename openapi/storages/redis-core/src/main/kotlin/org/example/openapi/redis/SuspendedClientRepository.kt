package org.example.openapi.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

/**
 * JWT는 Stateless이므로 즉시 무효화 불가.
 * 계정 정지 시 Redis에 블랙리스트를 등록해 Gateway에서 O(1)로 차단.
 * Key: SUSPENDED:{client_id}
 * TTL: 해당 JWT의 남은 exp 시간
 */
@Repository
class SuspendedClientRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {

    companion object {
        private const val KEY_PREFIX = "SUSPENDED:"
    }

    fun suspend(clientId: String, ttl: Duration) {
        redisTemplate.opsForValue().set("$KEY_PREFIX$clientId", "1", ttl)
    }

    fun isSuspended(clientId: String): Boolean =
        redisTemplate.hasKey("$KEY_PREFIX$clientId") == true

    fun revoke(clientId: String) {
        redisTemplate.delete("$KEY_PREFIX$clientId")
    }
}
