package org.example.openapi.db.repository

import org.example.openapi.db.entity.SystemScope
import org.springframework.data.jpa.repository.JpaRepository

interface SystemScopeRepository : JpaRepository<SystemScope, Long> {
    fun findByScopeCodeIn(scopeCodes: List<String>): List<SystemScope>
    fun existsByScopeCode(scopeCode: String): Boolean
}
