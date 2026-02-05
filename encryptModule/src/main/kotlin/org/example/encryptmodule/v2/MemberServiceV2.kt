package org.example.encryptmodule.v2

import org.example.encryptmodule.crypto.CryptoUtils
import org.example.encryptmodule.crypto.decrypt
import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberRequest
import org.example.encryptmodule.domain.MemberResponse
import org.springframework.stereotype.Service

@Service
class MemberServiceV2(
    private val memberRepositoryV2: MemberRepositoryV2,
) {

    // 암복호화 필드를 V1, V2 공통 영역에서 사용할 경우
    fun getMember(id: Long): MemberResponse {
        val member = memberRepositoryV2.findById(id).get() ?: throw IllegalArgumentException("Member not found")

        return MemberResponse.ofV2(member)
    }

    // 암복호화 필드를 V1, V2 별도의 에서 사용할 경우
    fun getMembers(ids: List<Long>): List<MemberResponse> {
        val members = memberRepositoryV2.findMembersByIdIn(ids)

        return members.map {
            MemberResponse(
                name = CryptoUtils.getDecryptText(it.name, it.nameEncrypted),
                phoneNumber = CryptoUtils.getDecryptText(it.phoneNumber, it.phoneNumberEncrypted),
//                name = CryptoUtils.getDecryptTextFieldName(it, Member::nameEncrypted),
//                phoneNumber = CryptoUtils.getDecryptTextFieldName(it, Member::phoneNumberEncrypted),
//                name = CryptoUtils.getDecryptTextAnnotation(it, Member::nameEncrypted),
//                phoneNumber = CryptoUtils.getDecryptTextAnnotation(it, Member::phoneNumberEncrypted),
                age = it.age,
                id = it.id!!,
            )
        }
    }

    fun getMembersV3(ids: List<Long>): List<MemberResponse> {
        val members = memberRepositoryV2.findMembersByIdIn(ids)
        for (member in members) {
            member.decrypt()
        }

        return members.map {
            MemberResponse(
                name = it.name,
                phoneNumber = it.phoneNumber,
                age = it.age,
                id = it.id!!,
            )
        }
    }


    fun createMember(memberRequest: MemberRequest): Member {
        // TODO Annotation 으로 처리
        val member = memberRequest.toMemberV2()
        member.phoneNumberEncrypted = CryptoUtils.encrypt(memberRequest.phoneNumber)
        member.nameEncrypted = CryptoUtils.encrypt(memberRequest.name)

        return memberRepositoryV2.save(member)
    }
}