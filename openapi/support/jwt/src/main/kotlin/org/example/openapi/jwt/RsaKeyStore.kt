package org.example.openapi.jwt

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * application.yml에 설정된 RSA 키 쌍을 로드.
 *
 * 키 생성 명령:
 *   openssl genrsa 2048 | tee private.pem | openssl pkcs8 -topk8 -nocrypt -outform DER | base64 | tr -d '\n'
 *   openssl rsa -in private.pem -pubout -outform DER | base64 | tr -d '\n'
 */
@Component
class RsaKeyStore(
    @Value("\${openapi.jwt.key-id}") keyId: String,
    @Value("\${openapi.jwt.private-key-base64}") privateKeyBase64: String,
    @Value("\${openapi.jwt.public-key-base64}") publicKeyBase64: String,
) {

    private val rsaKey: RSAKey = run {
        val keyFactory = KeyFactory.getInstance("RSA")
        val decoder = Base64.getDecoder()

        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(decoder.decode(privateKeyBase64)),
        ) as RSAPrivateKey

        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(decoder.decode(publicKeyBase64)),
        ) as RSAPublicKey

        RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(keyId)
            .keyUse(KeyUse.SIGNATURE)
            .build()
    }

    fun getPrivateKey(): RSAKey = rsaKey

    fun getPublicKey(): RSAKey = rsaKey.toPublicJWK()

    fun getJwkSet(): JWKSet = JWKSet(rsaKey.toPublicJWK())

    fun getJwksJson(): Map<String, Any> = getJwkSet().toJSONObject()
}
