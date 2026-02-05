package org.example.encryptmodule.domain

data class MemberRequest(
    val name: String,
    val phoneNumber: String,
    val age: Int,
) {
    fun toMember(): Member {
        return Member(
            name = this.name,
            phoneNumber = this.phoneNumber,
            age = this.age
        )
    }

    fun toMemberV2(): Member {
        return Member(
            name = this.name,
            phoneNumber = this.phoneNumber,
            age = this.age
        )
    }
}