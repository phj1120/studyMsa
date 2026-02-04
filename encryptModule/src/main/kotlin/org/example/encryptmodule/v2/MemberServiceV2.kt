package org.example.encryptmodule.v2

import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberDto
import org.springframework.stereotype.Service

@Service
class MemberServiceV2(
    private val memberRepositoryV2: MemberRepositoryV2,
) {

    // 암복호화 필드를 V1, V2 공통 영역에서 사용할 경우
    fun getMember(id: Long): MemberDto {
        val member = memberRepositoryV2.findById(id).get() ?: throw IllegalArgumentException("Member not found")

        return MemberDto.ofV2(member)
    }

    // 암복호화 필드를 V1, V2 별도의 에서 사용할 경우
    fun getMembers(ids: List<Long>): List<MemberDto> {
        val members = memberRepositoryV2.findMembersByIdIn(ids)

        return members.map {
            MemberDto(
                name = CryptoUtils.getDecryptText("it.name", it.nameEncrypted),
                phoneNumber = CryptoUtils.getDecryptText("it.phoneNumber", it.phoneNumber),
                age = it.age,
                id = it.id!!,
            )
        }
    }


    fun createMember(member: Member) {
        // TODO Annotation 으로 처리
        member.phoneNumberEncrypted = CryptoUtils.encrypt(member.phoneNumber)
        member.nameEncrypted = CryptoUtils.encrypt(member.name)

        memberRepositoryV2.save(member)
    }

}