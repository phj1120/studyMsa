package org.example.openapi.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    private val rsaKeyStore: RsaKeyStore,
    @Value("\${openapi.jwt.issuer}") private val issuer: String,
    @Value("\${openapi.jwt.key-id}") private val keyId: String,
    @Value("\${openapi.jwt.expiration-seconds:3600}") private val expirationSeconds: Long,
) {

    fun generate(
        clientId: String,
        tenantId: Long,
        scopes: List<String>,
        env: String,
    ): TokenResult {
        val now = Instant.now()
        val exp = now.plusSeconds(expirationSeconds)
        val jti = UUID.randomUUID().toString()

        val claimsSet = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(clientId)
            .expirationTime(Date.from(exp))
            .issueTime(Date.from(now))
            .jwtID(jti)
            .claim("scope", scopes.joinToString(" "))
            .claim("tid", tenantId)
            .claim("env", env)
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(keyId)
            .build()

        val jwt = SignedJWT(header, claimsSet)
        jwt.sign(RSASSASigner(rsaKeyStore.getPrivateKey()))

        return TokenResult(
            accessToken = jwt.serialize(),
            expiresIn = expirationSeconds,
            scope = scopes.joinToString(" "),
            jti = jti,
        )
    }

    data class TokenResult(
        val accessToken: String,
        val expiresIn: Long,
        val scope: String,
        val jti: String,
    )
}
