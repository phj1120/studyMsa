package org.example.encryptmodule.crypto

import org.example.encryptmodule.domain.Member
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

class CryptoUtils {
    companion object {
        fun encrypt(plainText: String): String {
            return "${plainText}-encrypt"
        }

        fun decrypt(encryptText: String): String {
            return encryptText.removeSuffix("encrypted")
        }

        fun getDecryptText(plainText: String, encryptText: String?): String {
            if (encryptText == null) {
                return plainText
            }
            val decryptText = decrypt(encryptText)
            if(decryptText == plainText) {
                return plainText
            }
            return decryptText
        }

        fun <T : Any, F> getDecryptTextFieldName(target: T, encryptField: KProperty1<T, F>): String {
            val encryptedValue = encryptField.call(target)
            if (encryptedValue is String && encryptedValue.isNotEmpty()) {
                return this.decrypt(encryptedValue)
            }

            val plainFieldName = encryptField.name.removeSuffix("Encrypted")
            val plainField = target::class.members.find { it.name == plainFieldName }

            return plainField?.call(target) as? String ?: throw IllegalArgumentException("plainField is null")
        }

        fun <T : Any, F> getDecryptTextAnnotation(target: T, encryptField: KProperty1<T, F>): String {
            val encryptedValue = encryptField.call(target)
            if (encryptedValue is String && encryptedValue.isNotEmpty()) {
                return this.decrypt(encryptedValue)
            }

            val annotation = encryptField.findAnnotation<CryptoField>()
                ?: throw IllegalArgumentException("@CryptoField annotation not found on ${encryptField.name}")

            val plainFieldName = annotation.plainField
            val plainField = target::class.members.find { it.name == plainFieldName }

            return plainField?.call(target) as? String
                ?: throw IllegalArgumentException("Can't find or read plainField: $plainFieldName")
        }
    }
}

// 엔티티가 순수해질 수 있게 확장함수로 모으자!
fun Member.decrypt(): Member {
    // 데이터 유형
    // 암호화 O, 평문O <- 복호화한데이터가 평문필드랑 다를 경우 복호화 된 데이터를 내릴 경우, 복호화 된 데이터로 업데이트가 될 수 있고, 평문을 내리면 이슈 없음.
    // 암호화 O, 평문X <- 이렇게 될 경우 평문 필드가 업데이트가 될 수 있음.
    // 암호화 X, 평문O <- 암호화 데이터 없으면 평문 데이터 내리니까 이슈 X
    // 암호화 X, 평문X <- 암호화 데이터 없으면 평문 데이터 내리니까 이슈 X
    // RO DB 로 접근해서 읽었다가 없데이트 해서 뻑나면 조회 안되니까.
    // 운영 마인드로 방어적으로 가면 진짜 그냥 DTO 로 안전빵으로가는게 나을 듯.
    this.phoneNumber = CryptoUtils.getDecryptText(this.phoneNumber, this.phoneNumberEncrypted!!)
    this.name = CryptoUtils.getDecryptText(this.name, this.nameEncrypted!!)

    return this
}