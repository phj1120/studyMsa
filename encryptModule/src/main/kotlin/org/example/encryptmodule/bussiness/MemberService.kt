package org.example.encryptmodule.bussiness

import org.example.encryptmodule.crypto.CryptoUtils
import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberRepository
import org.example.encryptmodule.domain.MemberRequest
import org.example.encryptmodule.domain.MemberResponse
import org.example.encryptmodule.external.ExternalApiClient
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val externalApiClient: ExternalApiClient
) {

    fun getMember(id: Long): MemberResponse {
        val member = memberRepository.findById(id).get() ?: throw IllegalArgumentException("Member not found")

        return MemberResponse.of(member)
    }

    fun getMembers(ids: List<Long>): List<MemberResponse> {
        val members = memberRepository.findMembersByIdIn(ids)

        return members.map {
            MemberResponse(
                name = CryptoUtils.getDecryptText(it.nameEncrypted, it.name)!!,
                phoneNumber = CryptoUtils.getDecryptText(it.phoneNumberEncrypted, it.phoneNumber)!!,
                age = it.age,
                id = it.id!!,
            )
        }
    }


    fun createMember(memberRequest: MemberRequest): Member {
        val validatePhoneNumber = externalApiClient.validatePhoneNumber(memberRequest.phoneNumber)
        if (!validatePhoneNumber) {
            throw IllegalArgumentException("Invalid phone number")
        }

        val member: Member = memberRequest.toMember()
        return memberRepository.save(member)
    }

    fun updateMember(id: Long, memberRequest: MemberRequest): Member {
        val member = memberRepository.findById(id).orElseThrow { NoSuchElementException("Member not found: $id") }
        member.name = memberRequest.name
        member.phoneNumber = memberRequest.phoneNumber
        member.age = memberRequest.age
        return memberRepository.save(member)
    }

    fun deleteMember(id: Long) {
        memberRepository.deleteById(id)
    }

}