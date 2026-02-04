package org.example.encryptmodule.v2

import org.example.encryptmodule.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepositoryV2 : JpaRepository<Member, Long> {
    fun findMembersByIdIn(ids: List<Long>): List<Member>


}