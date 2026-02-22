package org.example.encryptmodule

import org.example.encryptmodule.crypto.CryptoConverter
import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

/**
 * JPA 레이어 + 암복호화 통합 테스트
 *
 * - 실제 JPA를 사용하므로 @EntityListeners / @Convert 가 정상 동작
 * - JdbcTemplate 로 DB 원시값을 직접 조회해 암호화 여부 검증
 * - @DataJpaTest 기본 @Transactional → 각 테스트 후 롤백 (DB 오염 없음)
 *
 * FakeMemberRepository 와 충돌하지 않는 이유:
 *   각 테스트 클래스는 독립된 Spring 컨텍스트를 가지므로 서로 영향 없음.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CryptoConverter::class)
class MemberRepositoryDataJpaTest(
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val testEntityManager: TestEntityManager,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) {

    // ── C (Create) ────────────────────────────────────────────

    @Test
    fun createTest() {
        val saved = memberRepository.saveAndFlush(Member(name = "P", phoneNumber = "01011111111", age = 25))

        // CryptoConverter: DB 원시값이 암호문인지 확인 (평문과 달라야 함)
        val rawNameEncrypted = rawNameEncrypted(saved.id!!)
        assertNotNull(rawNameEncrypted)
        assertNotEquals("P", rawNameEncrypted)

        // CryptoConverter: 복호화 확인 (flush + clear 후 DB에서 재조회)
        testEntityManager.clear()
        val found = memberRepository.findById(saved.id!!).get()

        assertNotNull(found.nameEncrypted)
        assertEquals("P", found.nameEncrypted)   // @Convert 가 복호화한 값
        assertEquals("P", found.name)
        assertEquals("01011111111", found.phoneNumber)
    }

    // ── U (Update) ────────────────────────────────────────────

    @Test
    fun updateTest() {
        val saved = memberRepository.saveAndFlush(Member(name = "Before", phoneNumber = "01022222222", age = 25))
        val rawBefore = rawNameEncrypted(saved.id!!)

        saved.name = "After"
        saved.phoneNumber = "01033333333"
        memberRepository.saveAndFlush(saved)

        // CryptoConverter: 업데이트된 암호문이 이전과 다른지 확인
        // (GCM 은 랜덤 IV → 같은 평문도 매번 다른 암호문)
        val rawAfter = rawNameEncrypted(saved.id!!)
        assertNotEquals(rawBefore, rawAfter)
        assertNotEquals("After", rawAfter)

        // CryptoConverter: 복호화 확인
        testEntityManager.clear()
        val found = memberRepository.findById(saved.id!!).get()

        assertEquals("After", found.nameEncrypted)
        assertEquals("After", found.name)
        assertEquals("01033333333", found.phoneNumber)
    }

    // ── D (Delete) ────────────────────────────────────────────

    @Test
    fun deleteTest() {
        val saved = memberRepository.saveAndFlush(Member(name = "ToDelete", phoneNumber = "01044444444", age = 25))

        memberRepository.deleteById(saved.id!!)
        testEntityManager.flush()

        assertFalse(memberRepository.existsById(saved.id!!))
    }

    // ── Helper ────────────────────────────────────────────────

    private fun rawNameEncrypted(id: Long): String? =
        jdbcTemplate.queryForObject(
            "SELECT name_encrypted FROM member WHERE id = ?",
            String::class.java,
            id,
        )
}
