package org.example.encryptmodule.v2

import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberRequest
import org.example.encryptmodule.v1.MemberService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback

@SpringBootTest
class MemberServiceV2Test(
    @Autowired private val memberService: MemberServiceV2
) {

    @Test
    fun getMemberTest() {
        val memberRequest1 = MemberRequest(name = "P", phoneNumber = "01012345678", age = 29)
        val savedMember = memberService.createMember(memberRequest1)

        val member = memberService.getMember(savedMember.id!!)

        println(member)
    }


    @Test
    fun getMembersTest() {
        val memberRequest1 = MemberRequest(name = "P", phoneNumber = "01012345678", age = 29)
        val memberRequest2 = MemberRequest(name = "H", phoneNumber = "01012345679", age = 30)
        val memberRequest3 = MemberRequest(name = "J", phoneNumber = "01012345670", age = 31)

        val savedMember1 = memberService.createMember(memberRequest1)
        val savedMember2 = memberService.createMember(memberRequest2)
        val savedMember3 = memberService.createMember(memberRequest3)

        val ids = listOf(savedMember1.id!!, savedMember2.id!!, savedMember3.id!!)
        val members = memberService.getMembers(ids)

        println(members)
    }

}