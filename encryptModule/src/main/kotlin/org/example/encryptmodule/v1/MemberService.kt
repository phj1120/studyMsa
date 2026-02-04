package org.example.encryptmodule.v1

import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberDto
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository,
) {

    // 암복호화 필드를 V1, V2 공통 영역에서 사용할 경우
    fun getMember(id: Long): MemberDto {
        val member = memberRepository.findById(id).get() ?: throw IllegalArgumentException("Member not found")

        return MemberDto.of(member)
    }

    // 암복호화 필드를 V1, V2 별도의 에서 사용할 경우
    fun getMembers(ids: List<Long>): List<MemberDto> {
        val members = memberRepository.findMembersByIdIn(ids)

        return members.map {
            MemberDto(
                name = it.name,
                phoneNumber = it.phoneNumber,
                age = it.age,
                id = it.id!!,
            )
        }
    }


    fun createMember(member: Member) {
        memberRepository.save(member)
    }

}