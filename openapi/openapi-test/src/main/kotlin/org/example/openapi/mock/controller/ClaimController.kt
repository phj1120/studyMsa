package org.example.openapi.mock.controller

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/claims")
class ClaimController {

    private val claims: MutableList<MutableMap<String, Any>> = mutableListOf(
        mutableMapOf("id" to 1, "orderId" to 1, "reason" to "상품 불량",  "status" to "PENDING"),
        mutableMapOf("id" to 2, "orderId" to 2, "reason" to "오배송",     "status" to "PENDING"),
        mutableMapOf("id" to 3, "orderId" to 3, "reason" to "단순 변심",  "status" to "APPROVED"),
    )

    @PutMapping("/{id}")
    fun update(@PathVariable id: Int, @RequestBody body: Map<String, Any>): Map<String, Any> {
        val claim = claims.find { it["id"] == id } ?: throw NoSuchElementException("claim not found: $id")
        body.forEach { (k, v) -> if (k != "id") claim[k] = v }
        return claim
    }
}
