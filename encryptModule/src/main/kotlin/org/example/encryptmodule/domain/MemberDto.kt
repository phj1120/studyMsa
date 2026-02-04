package org.example.encryptmodule.domain

import org.example.encryptmodule.v2.CryptoUtils

data class MemberDto (
    val name: String,
    val phoneNumber: String,
    val age: Int,
    var id: Long,
) {
    companion object {
        fun of(member: Member): MemberDto {
            return MemberDto(
                id = member.id!!,
                name = member.name,
                phoneNumber = member.phoneNumber,
                age = member.age
            )
        }

        fun ofV2(member: Member): MemberDto {
            return MemberDto(
                id = member.id!!,
                name = CryptoUtils.getDecryptText("it.name", member.nameEncrypted),
                phoneNumber = CryptoUtils.getDecryptText("it.phoneNumber", member.phoneNumber),
                age = member.age
            )
        }
    }
}