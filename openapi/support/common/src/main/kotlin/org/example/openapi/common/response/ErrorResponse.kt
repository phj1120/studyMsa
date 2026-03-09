package org.example.openapi.common.response

data class ErrorResponse(
    val code: String,
    val message: String,
    val requestId: String? = null,
)
