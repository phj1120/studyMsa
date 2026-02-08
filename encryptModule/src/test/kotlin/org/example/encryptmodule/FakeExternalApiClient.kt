package org.example.encryptmodule

import org.example.encryptmodule.external.ExternalApiClient

/**
 * 테스트용 Fake ExternalApiClient
 *
 * Mock을 매번 만드는 것보다 Fake를 한 번 만들어서 재사용하는 것이 더 깔끔합니다.
 */
class FakeExternalApiClient : ExternalApiClient {

    // 테스트에서 설정 가능
    var shouldValidate: Boolean = true

    override fun validatePhoneNumber(phoneNumber: String): Boolean {
        // 기본적으로 모든 전화번호를 유효하다고 처리
        // 필요 시 shouldValidate로 제어 가능
        return shouldValidate
    }
}