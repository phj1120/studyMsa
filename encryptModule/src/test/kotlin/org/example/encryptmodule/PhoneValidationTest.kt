package org.example.encryptmodule

import io.mockk.every
import io.mockk.mockk
import org.example.encryptmodule.bussiness.MemberService
import org.example.encryptmodule.domain.MemberRepository
import org.example.encryptmodule.domain.MemberRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 전화번호 검증 단위 테스트 (Spring 없음)
 *
 * 장점:
 * - 빠른 실행 (Spring 부팅 불필요)
 * - 격리된 테스트 (외부 의존성 없음)
 */
class PhoneValidationTest {

    @Test
    fun `전화번호 검증 실패시 예외가 발생해야 한다`() {
        // Given: FakeExternalApiClient로 검증 실패 설정
        val fakeApiClient = FakeExternalApiClient()
        fakeApiClient.shouldValidate = false

        val mockRepository = mockk<MemberRepository>()
        val memberService = MemberService(mockRepository, fakeApiClient)

        val request = MemberRequest(name = "홍길동", phoneNumber = "01012345678", age = 29)

        // When & Then: 예외 발생 확인
        val exception = assertThrows<IllegalArgumentException> {
            memberService.createMember(request)
        }

        assertEquals("Invalid phone number", exception.message)
    }

    @Test
    fun `전화번호 검증 성공시 정상 처리되어야 한다`() {
        // Given: FakeExternalApiClient로 검증 성공 설정
        val fakeApiClient = FakeExternalApiClient()
        fakeApiClient.shouldValidate = true  // 기본값이지만 명시

        val mockRepository = mockk<MemberRepository>()
        every { mockRepository.save(any()) } answers { firstArg() }

        val memberService = MemberService(mockRepository, fakeApiClient)

        val request = MemberRequest(name = "홍길동", phoneNumber = "01012345678", age = 29)

        // When: 정상 처리 (예외 발생 안 함)
        val result = memberService.createMember(request)

        // Then: 정상 처리됨
        assertEquals("홍길동", result.name)
        assertEquals("01012345678", result.phoneNumber)
    }

}
