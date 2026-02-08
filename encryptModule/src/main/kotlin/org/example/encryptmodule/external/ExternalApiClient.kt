package org.example.encryptmodule.external

import org.springframework.stereotype.Component

/**
 * 외부 API를 호출하는 클라이언트 (HTTP Mock 예제용)
 */
interface ExternalApiClient {
    fun validatePhoneNumber(phoneNumber: String): Boolean
}

@Component
class RestTemplateExternalApiClient() : ExternalApiClient {
    override fun validatePhoneNumber(phoneNumber: String): Boolean {
        if ("exception phone number" == phoneNumber) {
            throw IllegalArgumentException("exception phone number")
        }
        return true
    }
}
