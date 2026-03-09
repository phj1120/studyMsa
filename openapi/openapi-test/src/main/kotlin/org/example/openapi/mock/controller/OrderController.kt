package org.example.openapi.mock.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    private val orders: MutableList<MutableMap<String, Any>> = mutableListOf(
        mutableMapOf("id" to 1, "productId" to 1, "quantity" to 2, "status" to "CONFIRMED", "totalPrice" to 3_000_000),
        mutableMapOf("id" to 2, "productId" to 2, "quantity" to 1, "status" to "PENDING",   "totalPrice" to 35_000),
        mutableMapOf("id" to 3, "productId" to 3, "quantity" to 3, "status" to "SHIPPED",   "totalPrice" to 267_000),
    )

    @GetMapping
    fun list() = mapOf("items" to orders, "totalCount" to orders.size)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Int) =
        orders.find { it["id"] == id } ?: throw NoSuchElementException("order not found: $id")

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun process(@PathVariable id: Int, @RequestBody body: Map<String, Any>): Map<String, Any> {
        val order = orders.find { it["id"] == id } ?: throw NoSuchElementException("order not found: $id")
        val action = body["action"]?.toString() ?: ""
        order["status"] = when (action) {
            "confirm" -> "CONFIRMED"
            "ship"    -> "SHIPPED"
            "cancel"  -> "CANCELLED"
            else      -> order["status"] ?: "UNKNOWN"
        }
        return order
    }
}
