package org.example.encryptmodule

import org.example.encryptmodule.domain.MemberRequest
import org.example.encryptmodule.external.ExternalApiClient
import org.example.encryptmodule.v1.MemberService
import org.example.encryptmodule.v2.MemberServiceV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional

/**
 * V1과 V2가 동일한 결과를 반환하는지 검증하는 테스트
 *
 * 목적: 암호화 방식 변경 후에도 비즈니스 로직이 동일하게 동작하는지 확인
 */
@SpringBootTest
@Transactional
class CompareV1V2Test {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun fakeExternalApiClient(): ExternalApiClient {
            return FakeExternalApiClient()
        }
    }

    @Autowired
    private lateinit var memberServiceV1: MemberService

    @Autowired
    private lateinit var memberServiceV2: MemberServiceV2

    // shouldValidate를 변경해야 하는 테스트에서만 사용
    @Autowired
    private lateinit var externalApiClient: ExternalApiClient

    @Test
    fun `V1과 V2의 getMember 결과가 동일해야 한다`() {
        // 이 테스트는 externalApiClient를 사용하지 않음
        // Given: 동일한 데이터로 각각 저장
        val request = MemberRequest(name = "홍길동", phoneNumber = "01012345678", age = 29)

        val savedV1 = memberServiceV1.createMember(request)
        val savedV2 = memberServiceV2.createMember(request)

        // When: 각각 조회
        val resultV1 = memberServiceV1.getMember(savedV1.id!!)
        val resultV2 = memberServiceV2.getMember(savedV2.id!!)

        // Then: 결과가 동일해야 함
        println("V1: ${resultV1.name}, ${resultV1.phoneNumber}, ${resultV1.age}")
        println("V2: ${resultV2.name}, ${resultV2.phoneNumber}, ${resultV2.age}")

        assertEquals(resultV1.name, resultV2.name, "이름이 동일해야 함")
        assertEquals(resultV1.phoneNumber, resultV2.phoneNumber, "전화번호가 동일해야 함")
        assertEquals(resultV1.age, resultV2.age, "나이가 동일해야 함")
    }

    @Test
    fun `전화번호 검증 실패시 예외가 발생해야 한다`() {
        // Given: FakeExternalApiClient의 shouldValidate를 false로 설정
        val fakeClient = externalApiClient as FakeExternalApiClient
        fakeClient.shouldValidate = false

        val request = MemberRequest(name = "홍길동", phoneNumber = "01012345678", age = 29)

        // When & Then: V1 전화번호 검증 실패로 예외 발생
        try {
            memberServiceV1.createMember(request)
            throw AssertionError("예외가 발생해야 함")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid phone number", e.message)
        }

        // When & Then: V2 전화번호 검증 실패로 예외 발생
        try {
            memberServiceV2.createMember(request)
            throw AssertionError("예외가 발생해야 함")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid phone number", e.message)
        }

        // 다시 true로 복원 (다른 테스트에 영향 없도록)
        fakeClient.shouldValidate = true
    }
}
