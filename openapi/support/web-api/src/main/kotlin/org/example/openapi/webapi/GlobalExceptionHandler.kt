package org.example.openapi.webapi

import org.example.openapi.common.exception.ErrorCode
import org.example.openapi.common.exception.OpenApiException
import org.example.openapi.common.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import jakarta.servlet.http.HttpServletRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(OpenApiException::class)
    fun handleOpenApiException(
        ex: OpenApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val requestId = request.getHeader("X-Request-Id")
        log.warn("OpenApiException: code={}, message={}", ex.errorCode.code, ex.message)
        return ResponseEntity
            .status(ex.errorCode.httpStatus)
            .body(ErrorResponse(code = ex.errorCode.code, message = ex.message, requestId = requestId))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val requestId = request.getHeader("X-Request-Id")
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: ErrorCode.INVALID_REQUEST.defaultMessage
        return ResponseEntity
            .status(400)
            .body(ErrorResponse(code = ErrorCode.INVALID_REQUEST.code, message = message, requestId = requestId))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val requestId = request.getHeader("X-Request-Id")
        log.error("Unexpected error", ex)
        return ResponseEntity
            .status(500)
            .body(
                ErrorResponse(
                    code = ErrorCode.INTERNAL_SERVER_ERROR.code,
                    message = ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage,
                    requestId = requestId,
                ),
            )
    }
}
