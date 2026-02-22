package org.example.encryptmodule

import org.example.encryptmodule.bussiness.MemberService
import org.example.encryptmodule.domain.MemberRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * MemberService 서비스 로직 단위 테스트
 *
 * - Spring 없이 FakeMemberRepository + FakeExternalApiClient 사용 → 빠른 실행
 * - @EntityListeners / @Convert 는 동작하지 않으므로 암복호화 검증 불포함
 *   → 암복호화는 MemberRepositoryDataJpaTest(@DataJpaTest) 에서 검증
 */
class MemberServiceTest {

    private val fakeMemberRepository = FakeMemberRepository()
    private val fakeExternalApiClient = FakeExternalApiClient()
    private val memberService = MemberService(fakeMemberRepository, fakeExternalApiClient)

    // ── R (Read) ───────────────────────────────────────────────

    @Test
    fun getMemberTest() {
        val saved = memberService.createMember(MemberRequest(name = "P", phoneNumber = "01012345678", age = 29))

        val found = memberService.getMember(saved.id!!)

        assertEquals("P", found.name)
        assertEquals("01012345678", found.phoneNumber)
        println(found)
    }

    @Test
    fun getMembersTest() {
        val s1 = memberService.createMember(MemberRequest(name = "P", phoneNumber = "01012345678", age = 29))
        val s2 = memberService.createMember(MemberRequest(name = "H", phoneNumber = "01012345679", age = 30))
        val s3 = memberService.createMember(MemberRequest(name = "J", phoneNumber = "01012345670", age = 31))

        val members = memberService.getMembers(listOf(s1.id!!, s2.id!!, s3.id!!))

        assertEquals(3, members.size)
        println(members)
    }

    // ── C (Create) ────────────────────────────────────────────

    @Test
    fun createMemberTest() {
        val request = MemberRequest(name = "CreateUser", phoneNumber = "01011111111", age = 25)

        val saved = memberService.createMember(request)

        // FakeMemberRepository.save() 에서 암호화 필드 세팅 확인
        assertNotNull(saved.id)
        assertNotNull(saved.nameEncrypted)
        assertNotNull(saved.phoneNumberEncrypted)

        val found = memberService.getMember(saved.id!!)
        assertEquals(request.name, found.name)
        assertEquals(request.phoneNumber, found.phoneNumber)
    }

    @Test
    fun `전화번호 검증 실패 시 createMember가 예외를 던진다`() {
        fakeExternalApiClient.shouldValidate = false

        assertThrows<IllegalArgumentException> {
            memberService.createMember(MemberRequest(name = "X", phoneNumber = "00000000000", age = 20))
        }
    }

    // ── U (Update) ────────────────────────────────────────────

    @Test
    fun updateMemberTest() {
        val saved = memberService.createMember(MemberRequest(name = "Before", phoneNumber = "01022222222", age = 25))

        val updateRequest = MemberRequest(name = "After", phoneNumber = "01033333333", age = 26)
        val updated = memberService.updateMember(saved.id!!, updateRequest)

        // 업데이트 후 암호화 필드 재세팅 확인
        assertNotNull(updated.nameEncrypted)
        assertNotNull(updated.phoneNumberEncrypted)

        val found = memberService.getMember(updated.id!!)
        assertEquals(updateRequest.name, found.name)
        assertEquals(updateRequest.phoneNumber, found.phoneNumber)
        assertEquals(updateRequest.age, found.age)
    }

    @Test
    fun `존재하지 않는 ID로 updateMember 호출 시 예외를 던진다`() {
        assertThrows<NoSuchElementException> {
            memberService.updateMember(9999L, MemberRequest(name = "X", phoneNumber = "01000000000", age = 20))
        }
    }

    // ── D (Delete) ────────────────────────────────────────────

    @Test
    fun deleteMemberTest() {
        val saved = memberService.createMember(MemberRequest(name = "ToDelete", phoneNumber = "01044444444", age = 25))

        memberService.deleteMember(saved.id!!)

        assertFalse(fakeMemberRepository.existsById(saved.id!!))
        assertThrows<NoSuchElementException> {
            memberService.getMember(saved.id!!)
        }
    }
}
