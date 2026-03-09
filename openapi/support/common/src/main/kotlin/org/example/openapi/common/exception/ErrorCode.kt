package org.example.openapi.common.exception

enum class ErrorCode(
    val httpStatus: Int,
    val code: String,
    val defaultMessage: String,
) {
    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 파라미터가 올바르지 않습니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증에 실패했습니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),
    INSUFFICIENT_SCOPE(403, "INSUFFICIENT_SCOPE", "요청한 API에 대한 권한이 없습니다."),
    ACCOUNT_SUSPENDED(403, "ACCOUNT_SUSPENDED", "정지된 계정입니다."),
    CREDENTIAL_NOT_FOUND(404, "CREDENTIAL_NOT_FOUND", "자격증명을 찾을 수 없습니다."),
    CREDENTIAL_INVALID_STATUS(409, "CREDENTIAL_INVALID_STATUS", "이미 삭제되거나 정지된 자격증명입니다."),
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "요청 횟수를 초과했습니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
}
