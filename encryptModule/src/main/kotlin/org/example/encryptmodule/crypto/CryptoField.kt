package org.example.encryptmodule.crypto

/**
 * 암호화 필드에 붙여서 평문 필드를 명시
 *
 * 사용 예:
 * @field:CryptoField("name")
 * var nameEncrypted: String? = null
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CryptoField(
    val plainField: String
)
