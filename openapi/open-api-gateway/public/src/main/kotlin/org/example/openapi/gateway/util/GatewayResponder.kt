package org.example.openapi.gateway.util

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

object GatewayResponder {

    fun error(
        exchange: ServerWebExchange,
        status: HttpStatus,
        code: String,
        message: String,
    ): Mono<Void> {
        val requestId = exchange.attributes["request_id"] as? String ?: UUID.randomUUID().toString()
        val body = """{"code":"$code","message":"$message","request_id":"$requestId"}"""
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.response.statusCode = status
        exchange.response.headers.contentType = MediaType.APPLICATION_JSON
        val buffer = exchange.response.bufferFactory().wrap(bytes)
        return exchange.response.writeWith(Mono.just(buffer))
    }
}
