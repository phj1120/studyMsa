package org.example.openapi.mock.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    private val products: MutableList<MutableMap<String, Any>> = mutableListOf(
        mutableMapOf("id" to 1, "name" to "노트북", "price" to 1_500_000, "stock" to 10),
        mutableMapOf("id" to 2, "name" to "마우스", "price" to 35_000,    "stock" to 50),
        mutableMapOf("id" to 3, "name" to "키보드", "price" to 89_000,    "stock" to 30),
    )

    @GetMapping
    fun list() = mapOf("items" to products, "totalCount" to products.size)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Int) =
        products.find { it["id"] == id } ?: throw NoSuchElementException("product not found: $id")

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody body: Map<String, Any>): Map<String, Any> {
        val next = mutableMapOf<String, Any>(
            "id" to (products.size + 1),
            "name" to (body["name"] ?: ""),
            "price" to (body["price"] ?: 0),
            "stock" to (body["stock"] ?: 0),
        )
        products.add(next)
        return next
    }

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Int, @RequestBody body: Map<String, Any>): Map<String, Any> {
        val product = products.find { it["id"] == id } ?: throw NoSuchElementException("product not found: $id")
        body.forEach { (k, v) -> if (k != "id") product[k] = v }
        return product
    }
}
