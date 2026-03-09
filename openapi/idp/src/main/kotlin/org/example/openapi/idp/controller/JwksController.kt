package org.example.openapi.idp.controller

import org.example.openapi.jwt.RsaKeyStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JwksController(private val rsaKeyStore: RsaKeyStore) {

    /**
     * GET /oauth/.well-known/jwks.json
     * RSA 공개키를 JWKS 포맷으로 반환.
     * Gateway가 TTL 15분 캐시로 사용.
     */
    @GetMapping("/oauth/.well-known/jwks.json")
    fun jwks(): Map<String, Any> = rsaKeyStore.getJwksJson()
}
