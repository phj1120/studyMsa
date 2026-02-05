package org.example.encryptmodule

import org.example.encryptmodule.v1.MemberRepository
import org.example.encryptmodule.v1.MemberService
import org.example.encryptmodule.v2.MemberServiceV2
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue

@SpringBootTest
class MemberServiceTest(
    @Autowired val memberRepository: MemberRepository,
    @Autowired val memberService: MemberService,
    @Autowired val memberServiceV2: MemberServiceV2
) {
    @Test
    fun getMember() {
        val ids: List<Long> = memberRepository.findAll().map { it.id!! }
        val membersV1 = memberService.getMembers(ids)
        val membersV2 = memberServiceV2.getMembers(ids)
        val membersV3 = memberServiceV2.getMembersV3(ids)



        for ((idx, id) in ids.withIndex()) {
            println("$id: ${membersV1[idx]} | ${membersV2[idx]}")
            assertTrue { membersV1[idx] == membersV2[idx] }
            assertTrue { membersV1[idx] == membersV3[idx] }
        }
    }

}