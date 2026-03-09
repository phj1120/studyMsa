# 모듈 구성

## 포트 목록

| 포트 | 모듈 | 역할 |
|------|------|------|
| 8080 | `openapi/open-api-gateway` | 외부 API 요청 진입점. JWT 검증, Scope 인가, Rate Limit, Audit Log |
| 8081 | `openapi/idp` | OAuth 2.0 토큰 발급 서버. JWKS 공개, Credential CRUD |
| 8082 | `openapi/tenant-admin` | 테넌트 ServiceKey 관리 (생성/삭제/Rotation) |
| 8083 | `email-send-producer` | Kafka 이메일 이벤트 발행 |
| 8084 | `email-send-consumer` | Kafka 이메일 이벤트 소비 및 발송 처리 |
| 8085 | `encryptModule` | 필드 암호화/복호화 모듈 |
| 9001 | `product` (backend) | 상품 도메인 서비스 (Gateway 라우팅 대상) |
| 9002 | `orders` (backend) | 주문 도메인 서비스 (Gateway 라우팅 대상) |
| 9003 | `claims` (backend) | 클레임 도메인 서비스 (Gateway 라우팅 대상) |

## 인프라

| 포트 | 서비스 |
|------|--------|
| 3306 | MySQL |
| 6379 | Redis |
| 9092 | Kafka |

---

## 모듈 요약

### openapi/open-api-gateway (8080)
Spring Cloud Gateway (WebFlux) 기반 API 게이트웨이.

**필터 파이프라인 (순서대로):**
1. `JwtAuthenticationFilter` — Bearer 토큰 서명/exp/iss 검증. JWKS 캐시 TTL 15분
2. `SuspendedAccountFilter` — Redis 블랙리스트로 정지 계정 차단 (O(1))
3. `AuthorizationEnforcementFilter` — `route-action-config.json` 기반 Scope 인가. Default Deny
4. `RateLimitFilter` — Token Bucket Rate Limiting (Redis). Fail-Open
5. `AuditLogFilter` — 전체 요청 Audit Log (Fire-and-Forget)

---

### openapi/idp (8081)
OAuth 2.0 Authorization Server. Internal 전용 (Gateway를 통해서만 외부 노출).

**엔드포인트:**
- `POST /oauth/token` — Client Credentials 토큰 발급
- `GET /oauth/.well-known/jwks.json` — RSA 공개키 JWKS 반환
- `POST /internal/v1/credentials` — ServiceKey 생성
- `POST /internal/v1/credentials/{id}/rotate` — Secret Rotation (Dual Activation, 유예 6h)
- `DELETE /internal/v1/credentials/{id}` — Soft Delete

**RSA 키:** `application.yml`의 `private-key-base64` / `public-key-base64`로 고정 관리

---

### openapi/tenant-admin (8082)
ServiceKey 자기 관리 어드민 BE.

**엔드포인트:**
- `POST /api/v1/tenants/{tenantId}/service-keys`
- `GET /api/v1/tenants/{tenantId}/service-keys`
- `POST /api/v1/tenants/{tenantId}/service-keys/{keyId}/rotate`
- `DELETE /api/v1/tenants/{tenantId}/service-keys/{keyId}`

DB는 idp와 공유 (`openapi_db`). DDL은 idp가 관리, tenant-admin은 `ddl-auto: none`.

---

### email-send-producer (8083)
Kafka Producer. 이메일 발송 이벤트를 Kafka 토픽에 발행.

---

### email-send-consumer (8084)
Kafka Consumer. 이메일 이벤트 소비 후 실제 발송 처리. `ack-mode: manual`.

---

### encryptModule (8085)
JPA EntityListener 기반 필드 암호화/복호화 모듈.
`crypto.secret-key`로 AES 키 설정.
