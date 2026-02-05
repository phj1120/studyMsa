package org.example.encryptmodule.domain

import org.example.encryptmodule.crypto.CryptoUtils

data class MemberResponse (
    val name: String,
    val phoneNumber: String,
    val age: Int,
    var id: Long,
) {
    companion object {
        fun of(member: Member): MemberResponse {
            return MemberResponse(
                id = member.id!!,
                name = member.name,
                phoneNumber = member.phoneNumber,
                age = member.age
            )
        }

        fun ofV2(member: Member): MemberResponse {
            return MemberResponse(
                id = member.id!!,
                name = CryptoUtils.getDecryptText(member.name, member.nameEncrypted),
                phoneNumber = CryptoUtils.getDecryptText(member.phoneNumber, member.phoneNumber),
                age = member.age
            )
        }
    }
}