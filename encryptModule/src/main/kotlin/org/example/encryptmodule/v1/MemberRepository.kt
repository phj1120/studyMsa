package org.example.encryptmodule.v1

import org.example.encryptmodule.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findMembersByIdIn(ids: List<Long>): List<Member>


}