package org.example.encryptmodule.v2

import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.v1.MemberService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MemberServiceV2Test(
    @Autowired private val memberService: MemberServiceV2
) {

    @Test
    fun getMemberTest() {
        val member1 = Member("P", "01012345678", 29)
        memberService.createMember(member1)

        val member = memberService.getMember(member1.id!!)

        println(member)
    }


    @Test
    fun getMembersTest() {
        val member1 = Member("P", "01012345678", 29)
        val member2 = Member("H", "01012345679", 30)
        val member3 = Member("J", "01012345670", 31)
        memberService.createMember(member1)
        memberService.createMember(member2)
        memberService.createMember(member3)

        val members = memberService.getMembers(listOf(member1.id!!, member2.id!!, member3.id!!))

        println(members)
    }

}