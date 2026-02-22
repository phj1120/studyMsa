package org.example.encryptmodule.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
@Converter(autoApply = false)
class CryptoConverter(
    @Value("\${crypto.secret-key}") secretKey: String
) : AttributeConverter<String, String> {

    private val keySpec: SecretKeySpec = SecretKeySpec(
        Base64.getDecoder().decode(secretKey).copyOf(KEY_SIZE), "AES"
    )

    override fun convertToDatabaseColumn(attribute: String?): String? =
        attribute?.let { encrypt(it) }

    override fun convertToEntityAttribute(dbData: String?): String? =
        dbData?.let { decrypt(it) }

    private fun encrypt(plainText: String): String {
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    private fun decrypt(cipherText: String): String {
        val decoded = Base64.getDecoder().decode(cipherText)
        val iv = decoded.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = decoded.copyOfRange(GCM_IV_LENGTH, decoded.size)
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 32       // AES-256
        private const val GCM_IV_LENGTH = 12  // 96-bit IV (GCM 권장)
        private const val GCM_TAG_BITS = 128  // 인증 태그 128-bit
    }
}
