# PRD: OAuth 2.0 Open API Platform

> 버전: 1.0 | 작성일: 2026-03-05

---

## 목차

1. [개요](#1-개요)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [IdP 요구사항](#3-idp-요구사항)
4. [Open API Gateway 요구사항](#4-open-api-gateway-요구사항)
5. [Tenant Admin 요구사항](#5-tenant-admin-요구사항)
6. [API 명세](#6-api-명세)
7. [DB 스키마](#7-db-스키마)
8. [보안 정책](#8-보안-정책)
9. [모니터링 및 로깅](#9-모니터링-및-로깅)
10. [Phase 로드맵](#10-phase-로드맵)

---

## 1. 개요

### 1-1. 배경 및 문제 정의

B2B 외부 연동 시스템에서 다음 문제가 발생하고 있다.

| 구분 | 문제 | 리스크 |
|------|------|--------|
| **Critical** | 영구 Static Key를 이메일로 평문 전달 | 유출 시 즉시 차단 수단 없음 |
| **Critical** | 단일 키로 모든 API 접근 가능 (무제한 권한) | 최소 권한 원칙 위반, 오남용 위험 |
| **Critical** | 하나의 테넌트 계정에 단일 키만 사용 | 키 유출 시 해당 테넌트 전체 노출 |
| **Critical** | Secret 원본 DB 저장 / 암호화 없음 | DB 유출 시 즉각 피해 |
| **Safety** | API 호출 추적 로그 없음 | 사고 발생 시 원인 파악 불가 |
| **Safety** | 키 수명주기 관리 체계 없음 | 이상 징후 발생 시 즉각 대응 불가 |
| **Efficiency** | 키 발급/관리 수동 프로세스 | 운영 공수 낭비 |

### 1-2. 목표 (Goals)

**Phase 1 (이번 구현 범위):**
- OAuth 2.0 Client Credentials Grant 기반 인증 체계 도입
- BCrypt 기반 Secret 단방향 해시 저장 (원본 저장 금지)
- Scope 기반 최소 권한 원칙 적용
- 1:N 구조의 다중 키(ServiceKey) 지원
- Secret Rotation (무중단 키 교체)
- 전체 API 호출 Audit Log
- Self-Service 키 관리 Admin

**Non-Goal (Phase 1 제외):**
- Refresh Token (Client Credentials에서 불필요 — 만료 시 재발급)
- private_key_jwt 인증 방식 (Phase 2 고도화 옵션)
- IP Allow List (Phase 2)
- Open API SDK (Phase 2)
- V1 레거시 강제 종료 (점진적 마이그레이션)

### 1-3. 용어 정의

| 용어 | 설명 |
|------|------|
| **Tenant Account** | API 키를 생성/관리하는 마스터 계정 (`tenantId`로 식별) |
| **ServiceKey** | 실제 API를 호출하는 기술적 자격증명 (`client_id + client_secret`) |
| **IdP** | Identity Provider. OAuth 2.0 인증 서버 (토큰 발급) |
| **JWKS** | JSON Web Key Set. JWT 서명 검증용 공개키 집합 |
| **Scope** | API 접근 권한 단위. `{resource}:{action}` 형식 (예: `order:read`) |
| **Secret Rotation** | 무중단 키 교체. 신/구 키를 유예 기간 동안 동시 허용 (Dual Activation) |
| **Soft Delete** | `deleted_at` 컬럼으로 논리 삭제. 물리 삭제 없음 |
| **Default Deny** | 명시적으로 허용된 정책 외 모든 요청 차단 |

### 1-4. 인증 방식 선택 근거

OAuth 2.0 Client Credentials Grant 채택:

| 항목 | Client Secret (채택) | private_key_jwt | Refresh Token |
|------|---------------------|-----------------|---------------|
| 권한 주체 | 시스템/서비스 | 시스템/서비스 | 사용자 세션 |
| 구현 부담 | 낮음 | 높음 (키쌍 관리) | 중간 |
| 상태 관리 | Stateless | Stateless | Stateful |
| 보안성 | 중간 | 높음 | - |
| 적합 대상 | B2B 대량 연동 | 엔터프라이즈 티어 | 사용자 앱 |

B2B 시스템은 사람이 아닌 **서버가 24/7 자동 호출**하는 환경이므로 Client Credentials가 가장 표준적이고 운영이 단순하다.

---

## 2. 시스템 아키텍처

### 2-1. 전체 흐름

```
[외부 클라이언트 서버]
        │  HTTPS / Authorization: Bearer {JWT}
        ▼
┌─────────────────────────────────────────────────┐
│              Open API Gateway (Public)           │
│                                                  │
│  Pipeline:                                       │
│  ① JWT 서명 검증 (JWKS 캐시, TTL 15분)           │
│  ② 계정 정지(SUSPENDED) 확인 → Redis             │
│  ③ Scope 기반 인가 (Route-Action Mapping)        │
│  ④ Rate Limiting (Token Bucket, Redis)           │
│  ⑤ Audit Log (Fire-and-Forget)                  │
└────────────────┬────────────────────────────────┘
                 │  내부 라우팅
        ┌────────┴────────┐
        ▼                 ▼
┌──────────────┐   ┌──────────────────────────┐
│ Backend APIs │   │         IdP              │
│ (도메인 서비스)│   │  POST /oauth/token       │
└──────────────┘   │  GET  /oauth/.well-known/│
                   │        jwks.json         │
                   └──────────────────────────┘
                              ▲  Internal HTTP
                              │
              ┌───────────────┴─────────────────┐
              │        Tenant Admin (BE)         │
              │  - ServiceKey 생성/삭제/조회      │
              │  - Secret Rotation               │
              │  - IdP Internal API 프록시        │
              └─────────────────────────────────┘
                              ▲
                              │
              [운영 담당자 (Admin UI)]
```

### 2-2. 계정 구조 (1:N)

```
Tenant Account (tenantId: 1001)
    │
    ├── ServiceKey #1  client_id: prod_svc_abc  scope: [order:read]
    ├── ServiceKey #2  client_id: prod_svc_def  scope: [product:write]
    └── ServiceKey #3  client_id: prod_svc_ghi  scope: [claim:update]
```

설계 의도: 키 하나가 유출되어도 해당 scope 범위로만 피해가 국한된다.

### 2-3. 모듈 구조 (Gradle 멀티 모듈)

```
open-api-platform/
├── tenant-admin/               # [App] 테넌트 관리 어드민 (FE + BE)
├── idp/                        # [App] OAuth 2.0 Authorization Server
├── open-api-gateway/
│   └── public/                 # [App] Spring Cloud Gateway (WebFlux)
├── business/
│   ├── business-tenant/        # 테넌트 도메인 비즈니스 로직
│   └── business-auth/          # 인증/인가 비즈니스 로직
├── support/
│   ├── common/                 # 공통 예외(OpenApiException), 응답(ApiResponse)
│   ├── web-api/                # GlobalExceptionHandler, Web 공통 설정
│   ├── logger/                 # Logback JSON 로깅 설정
│   ├── jwt/                    # nimbus-jose-jwt 래퍼 (JWT 생성/검증)
│   └── security/               # Spring Security 공통 설정
└── storages/
    ├── db-core/                # JPA + MySQL
    └── redis-core/             # Redis(Lettuce) 클라이언트
```

### 2-4. 기술 스택

| 레이어 | 기술 | 버전 | 용도 |
|--------|------|------|------|
| Language | Java | 21 | |
| Framework | Spring Boot | 3.x | 전체 App |
| Gateway | Spring Cloud Gateway | 4.x | WebFlux 기반 API GW |
| Security | Spring Security OAuth2 RS | 6.x | JWT 자동 검증 |
| JWT | nimbus-jose-jwt | 9.37.x | RSA 서명/검증/JWKS 포맷 |
| DB | MySQL | 8.x | 메인 스토리지 (로컬: H2) |
| ORM | Spring Data JPA | 3.x | |
| Cache | Redis | 7.x | Rate Limit 카운터, 블랙리스트 |
| Build | Gradle | 8.x | 멀티 모듈 |

---

## 3. IdP 요구사항

### 3-1. 기능 요구사항 (Functional Requirements)

#### FR-IDP-001: Credential 생성 및 자격증명 발급

- `client_id` 생성: CSPRNG 20바이트 → Base64URL (no padding) → `{env}_{prefix}_{random}` 형식
- `client_secret` 생성: CSPRNG 32바이트 이상 → Base64URL
- `secret_hash`: BCrypt 단방향 해시 → `client_secrets` 테이블 저장
- **원본 `client_secret`은 생성 API 응답에서 단 1회만 반환** (이후 조회 불가)

#### FR-IDP-002: OAuth 2.0 Token 발급 (`POST /oauth/token`)

- `grant_type=client_credentials` 지원
- `Authorization: Basic base64(client_id:client_secret)` 파싱
- 검증 파이프라인:
    1. `oauth_clients.deleted_at IS NULL` 확인
    2. `client_secrets.secret_hash` BCrypt 대조
    3. `client_secrets.expires_at > NOW()` 확인
- 성공 시 RSA 서명 JWT 발급 (`exp`: 3600초)

**JWT Payload 구조:**
```json
{
  "iss": "https://idp.your-domain.com",
  "sub": "prod_svc_x8s7abc123",
  "exp": 1706845200,
  "iat": 1706841600,
  "jti": "a1b2-c3d4-e5f6-uuid4",
  "scope": "order:read product:write",
  "tid": 1001,
  "env": "PROD"
}
```

| 클레임 | 설명 |
|--------|------|
| `sub` | client_id (ServiceKey 식별자) |
| `tid` | Tenant Account ID (리소스 격리 기준) |
| `jti` | 고유 식별자 (블랙리스트, 재사용 방지) |
| `scope` | 공백 구분 권한 문자열 (RFC 6749 표준) |
| `env` | 환경 혼용 실수 방지 |

#### FR-IDP-003: JWKS 엔드포인트 (`GET /oauth/.well-known/jwks.json`)

- RSA 공개키를 JWKS 포맷으로 반환
- `kid` 포함 (키 롤오버 시 Gateway가 정확한 키로 검증)
- 라이브러리: `nimbus-jose-jwt`

#### FR-IDP-004: Secret Rotation (Dual Activation)

```
요청 수신
    │
    ▼
신규 Secret(v2) 생성 + BCrypt 해시 → client_secrets INSERT
    │
    ▼
구버전(v1) expires_at = NOW() + {grace_period(기본 6h)}
    │
    ▼
유예 기간 동안: v1, v2 모두 인증 허용
    │
    ▼
grace_period 경과 → v1 자동 만료 (expires_at < NOW())
```

#### FR-IDP-005: Credential 폐기 (`DELETE`)

- `oauth_clients.deleted_at = CURRENT_TIMESTAMP` (Soft Delete)
- 폐기 후 `POST /oauth/token` 시 즉시 `401` 반환

#### FR-IDP-006: 만료일 기반 권한 회수

- 토큰 발급 요청 시 `expires_at > NOW()` 실시간 검증
- 배치 없이 **요청 시점에 판단** (배치 간격의 보안 공백 제거)

### 3-2. 비기능 요구사항 (Non-Functional Requirements)

| 항목 | 요건 |
|------|------|
| 보안 | Secret 원본 DB/로그 저장 완전 금지 |
| 보안 | client_id/secret 생성에 CSPRNG 필수 (`SecureRandom`) |
| 성능 | `POST /oauth/token` P99 < 500ms (BCrypt 연산 포함) |
| 성능 | BCrypt 연산은 별도 스레드 풀 격리 권장 |
| 표준 | OAuth 2.0 에러 응답 RFC 6749 포맷 준수 |
| 접근 | IdP는 Internal 전용. Public 노출은 Gateway를 통해서만 |

---

## 4. Open API Gateway 요구사항

### 4-1. 인증/인가 파이프라인 순서

```
요청 수신
    │
    ├─① Authorization 헤더 확인 ──────── 없으면 → 401
    │
    ├─② JWT 서명/exp/iss 검증 ──────── 실패 시 → 401
    │    (JWKS 캐시, TTL 15분)
    │
    ├─③ SUSPENDED 계정 확인(Redis) ──── 정지됨 → 403
    │
    ├─④ Scope 기반 인가 ──────────────── 권한 없음 → 403
    │    (Route-Action Mapping)
    │
    ├─⑤ Rate Limiting(Token Bucket) ─── 초과 시 → 429
    │    (Redis, RL_KEY:{path}:{client_id})
    │
    └─⑥ 백엔드 라우팅 + Audit Log (비동기)
```

### 4-2. 기능 요구사항

#### FR-GW-001: JWT 인증 필터

- `Authorization: Bearer {token}` 헤더 없으면 `401`
- IdP JWKS 엔드포인트에서 공개키 조회 → 로컬 메모리 캐싱 (TTL 15분)
- 매 요청마다 IdP 호출하지 않고 **캐시된 키로 검증** (성능)
- `exp` (만료), `iss` (발급자) 표준 클레임 검증

#### FR-GW-002: Scope 기반 인가 (`AuthorizationEnforcementFilter`)

Route-Action 매핑 설정 (`route-action-config.json`):
```json
[
  { "method": "POST",  "pathPattern": "/api/v1/products",    "requiredAction": "product:write" },
  { "method": "PATCH", "pathPattern": "/api/v1/products/**", "requiredAction": "product:write" },
  { "method": "GET",   "pathPattern": "/api/v1/products",    "requiredAction": "product:read"  },
  { "method": "GET",   "pathPattern": "/api/v1/orders",      "requiredAction": "order:read"    },
  { "method": "POST",  "pathPattern": "/api/v1/orders/**",   "requiredAction": "order:write"   },
  { "method": "PUT",   "pathPattern": "/api/v1/claims/**",   "requiredAction": "claim:update"  }
]
```

동작:
1. **Identify**: 요청 Method + Path → Required Action 조회
2. **Extract**: JWT `scope` 클레임 추출 (`"order:read product:write"` → List)
3. **Evaluate**: Required Action 포함 여부 대조 (와일드카드 `order:*` 지원)
4. **Decision**: 불충분 시 백엔드로 보내지 않고 즉시 `403`

**Default Deny**: 매핑 정책 없는 경로 → 즉시 `403`

#### FR-GW-003: Rate Limiting (Token Bucket)

- 구현: Spring Cloud Gateway `RequestRateLimiter` + Redis Lua Script (원자성 보장)
- Key: `RL_KEY:{api_path}:{client_id}` (테넌트 × API 경로별 격리)
- **Fail-Open**: Redis 장애 시 트래픽 차단하지 않고 통과 (가용성 우선)
- 초과 시: `429 Too Many Requests` + `Retry-After` 헤더

Token Bucket 파라미터:
```yaml
redis-rate-limiter:
  replenishRate: 10      # 초당 토큰 리필 속도
  burstCapacity: 50      # 버킷 최대 용량 (순간 폭주 허용치)
  requestedTokens: 1     # 요청 1회당 토큰 소모량
```

Token Bucket 채택 이유: B2B 연동 클라이언트는 평소 조용하다가 배치 처리 시 순간 폭주(Burst)하는 트래픽 패턴을 가지므로, 평균 속도 제어와 순간 폭주 허용을 동시에 제공하는 Token Bucket이 적합.

#### FR-GW-004: Audit Logging

- 모든 요청에 `request_id` (UUID v4) 생성
- **Fire-and-Forget** 비동기 처리 (로그 기록이 응답 지연 유발 금지)
- 기록 필드: `request_id`, `client_id`, `tenant_id`, `method`, `path`, `required_action`, `result(ALLOW/DENY)`, `status_code`, `latency_ms`, `error_code`

#### FR-GW-005: 에러 응답 표준화

```json
{
  "code": "INSUFFICIENT_SCOPE",
  "message": "요청한 API에 대한 권한이 없습니다.",
  "request_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

- 내부 스택 트레이스, DB 쿼리, 서버 IP 절대 노출 금지

### 4-3. 비기능 요구사항

| 항목 | 요건 |
|------|------|
| 성능 | Gateway 오버헤드 P95 < 25ms |
| 성능 | JWKS 캐시 히트율 > 99% |
| 성능 | 매 요청 DB 조회 금지 |
| 확장성 | Stateless 구조 → Scale-out 가능 |
| 확장성 | Redis Shared Storage로 다중 인스턴스 Rate Limit 공유 |
| 보안 | Default Deny 정책 |
| 가용성 | Redis 장애 시 Fail-Open (Rate Limit 우회) |

---

## 5. Tenant Admin 요구사항

### 5-1. FE 기능 요구사항

#### FR-TA-FE-001: ServiceKey 생성 Form

입력 항목:
- `keyName`: 영문/숫자/하이픈만 허용, 최대 50자, `tenantId + keyName` 복합 중복 체크
- `scopeCodes`: 카테고리별 체크박스

| 카테고리 | 조회 | 등록/수정 | 삭제 |
|---------|------|---------|------|
| 상품 | `product:read` | `product:write` | `product:delete` |
| 주문/배송 | `order:read` | `order:write` | - |
| 클레임/CS | `claim:read` | `claim:update` | - |
| 통계 | `stats:read` | - | - |
| 마케팅 | `marketing:read` | `marketing:write` | - |
| 정산 | `settlement:read` | - | - |

- `expiresAt`: 날짜 선택, 최대 1년

생성 완료 후:
- `client_id`와 `client_secret`을 화면에 표시
- 보안 경고 문구: *"이 Secret 값은 지금 단 한 번만 표시되며, 창을 닫으면 다시는 확인할 수 없습니다. 반드시 안전한 곳에 복사해 두세요."*

#### FR-TA-FE-002: ServiceKey 목록 화면

- 컬럼: `keyName`, `client_id` (일부 마스킹), `scope` 배지, `status` 배지, `created_at`, `expires_at`
- 상태별 시각화: `Active` (초록), `Expiring` (주황), `Expired` (빨강), `Deleted` (회색)

#### FR-TA-FE-003: ServiceKey 상세 화면

- `client_secret`: `●●●●●●●●` 마스킹 처리 (원본 노출 불가)
- `[시크릿 교체]` 버튼 → 확인 모달 → Rotate API → 새 secret 1회 노출
- 구버전 키 `EXPIRING` 상태 + 유예 기간 남은 시간 표시
- `[키 삭제]` 버튼 → 확인 모달

### 5-2. BE 기능 요구사항

#### FR-TA-BE-001: ServiceKey 생성

- 프론트로부터 `keyName`, `scopeCodes`, `expiresAt` 수신
- IdP `POST /internal/v1/credentials` 호출
- **응답받은 `client_secret`은 DB/로그 저장 금지** → 즉시 FE로 패스스루

#### FR-TA-BE-002: ServiceKey 목록 조회

- `tenantId` 기준으로 소유한 ServiceKey 목록 반환

#### FR-TA-BE-003: Secret Rotation

- IdP `POST /internal/v1/credentials/{credentialId}/rotate` 호출
- 새 `client_secret` 패스스루

#### FR-TA-BE-004: ServiceKey 삭제

- IdP `DELETE /internal/v1/credentials/{credentialId}` 호출

#### FR-TA-BE-005: 소유권 검증 (공통 미들웨어)

- 모든 쓰기 API에서 세션의 `tenantId`와 리소스의 `tenantId` 일치 확인
- 불일치 시 `403` 반환 (IdP 호출 전에 차단)

### 5-3. 에러 처리 (FE/BE 통일 규격)

| 시나리오 | HTTP | `error_code` | FE 처리 |
|---------|------|--------------|---------|
| ServiceKey 없음 | 404 | `CREDENTIAL_NOT_FOUND` | 알림 후 목록으로 리다이렉트 |
| 이미 삭제/정지된 계정 | 409 | `CREDENTIAL_INVALID_STATUS` | 알림 + 교체 버튼 비활성화 |
| 너무 잦은 교체 시도 | 429 | `TOO_MANY_REQUESTS` | 알림 (잠시 후 재시도 안내) |
| 서버 오류 | 500 | `INTERNAL_SERVER_ERROR` | 일반 에러 모달 |

---

## 6. API 명세

### 6-1. IdP API

#### `POST /oauth/token`
```
Content-Type: application/x-www-form-urlencoded
Authorization: Basic base64(client_id:client_secret)

Request:
  grant_type=client_credentials

Response 200:
{
  "access_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImFwaS1rZXktdjEifQ...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "order:read product:write",
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}

Response 401:
{
  "error": "invalid_client",
  "error_description": "Client authentication failed"
}

Response 400:
{
  "error": "unsupported_grant_type",
  "error_description": "The authorization grant type is not supported"
}
```

#### `GET /oauth/.well-known/jwks.json`
```
Response 200:
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "api-key-v1",
      "alg": "RS256",
      "n": "sKg_p3...",
      "e": "AQAB"
    }
  ]
}
```

#### `POST /internal/v1/credentials`
```
Request:
{
  "tenantId": 1001,
  "keyName": "order-integration",
  "scopes": ["order:read", "product:write"],
  "expiresAt": "2027-03-05T00:00:00Z"
}

Response 201:
{
  "credentialId": "prod_svc_x8s7abc123",
  "clientSecret": "원본_시크릿_1회만_반환",
  "keyName": "order-integration",
  "scopes": ["order:read", "product:write"],
  "expiresAt": "2027-03-05T00:00:00Z",
  "createdAt": "2026-03-05T10:00:00Z"
}
```

#### `POST /internal/v1/credentials/{credentialId}/rotate`
```
Response 200:
{
  "credentialId": "prod_svc_x8s7abc123",
  "newClientSecret": "새로운_시크릿_1회만_반환",
  "previousSecretExpiresAt": "2026-03-05T16:00:00Z"
}
```

#### `DELETE /internal/v1/credentials/{credentialId}`
```
Response 204 (No Content)
```

### 6-2. Tenant Admin API

#### `POST /api/v1/tenants/{tenantId}/service-keys`
```
Request:
{
  "keyName": "order-integration",
  "scopeCodes": ["order:read", "product:write"],
  "expiresAt": "2027-03-05T00:00:00Z"
}

Response 201:
{
  "keyId": "prod_svc_x8s7abc123",
  "clientSecret": "원본_시크릿_1회만_반환",
  "keyName": "order-integration",
  "scopeCodes": ["order:read", "product:write"],
  "createdAt": "2026-03-05T10:00:00Z"
}
```

#### `GET /api/v1/tenants/{tenantId}/service-keys`
```
Response 200:
{
  "items": [
    {
      "keyId": "prod_svc_x8s7abc123",
      "keyName": "order-integration",
      "status": "ACTIVE",
      "scopeCodes": ["order:read", "product:write"],
      "expiresAt": "2027-03-05T00:00:00Z",
      "createdAt": "2026-03-05T10:00:00Z"
    }
  ],
  "totalCount": 1
}
```

#### `POST /api/v1/tenants/{tenantId}/service-keys/{keyId}/rotate`
```
Response 200:
{
  "keyId": "prod_svc_x8s7abc123",
  "newClientSecret": "새로운_시크릿_1회만_반환",
  "previousSecretExpiresAt": "2026-03-05T16:00:00Z",
  "message": "새 Secret이 활성화되었습니다. 기존 Secret은 설정된 유예 시간 후 만료됩니다."
}
```

#### `DELETE /api/v1/tenants/{tenantId}/service-keys/{keyId}`
```
Response 204 (No Content)
```

### 6-3. 에러 코드 전체 정의

| HTTP | `code` | 발생 위치 | 설명 |
|------|--------|---------|------|
| 400 | `INVALID_REQUEST` | 전체 | 요청 파라미터 오류 |
| 400 | `unsupported_grant_type` | IdP | OAuth 표준 |
| 401 | `UNAUTHORIZED` | GW | 토큰 없음/만료 |
| 401 | `invalid_client` | IdP | OAuth 표준, 자격증명 불일치 |
| 401 | `unauthorized_client` | IdP | 정지된 계정 |
| 403 | `FORBIDDEN` | GW, TA | 권한 부족 |
| 403 | `INSUFFICIENT_SCOPE` | GW | Scope 불충분 |
| 403 | `ACCOUNT_SUSPENDED` | GW | 정지 계정 |
| 404 | `CREDENTIAL_NOT_FOUND` | IdP, TA | 자격증명 없음 |
| 409 | `CREDENTIAL_INVALID_STATUS` | IdP, TA | 이미 삭제/정지 |
| 429 | `TOO_MANY_REQUESTS` | GW, TA | Rate Limit 초과 또는 잦은 Rotation |
| 500 | `INTERNAL_SERVER_ERROR` | 전체 | 서버 오류 |

---

## 7. DB 스키마

### 7-1. DDL

```sql
-- =============================================
-- oauth_clients: 클라이언트 기본 정보 및 생명주기
-- =============================================
CREATE TABLE oauth_clients (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    client_id   VARCHAR(100) NOT NULL COMMENT 'CSPRNG 기반 식별자 (예: prod_svc_x8s7abc)',
    tenant_id   BIGINT       NOT NULL COMMENT '테넌트 계정 식별자',
    key_name    VARCHAR(100) NOT NULL COMMENT '키 용도 이름 (영문)',
    scopes      JSON         NOT NULL COMMENT '허용 스코프 (예: ["order:read"])',
    expires_at  TIMESTAMP    NOT NULL COMMENT '클라이언트 만료일',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL     COMMENT 'Soft Delete. NULL이면 유효',
    PRIMARY KEY (id),
    UNIQUE  KEY uk_client_id (client_id),
    INDEX       idx_tenant_id (tenant_id),
    INDEX       idx_expires_at (expires_at),
    INDEX       idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- client_secrets: 자격증명 및 Rotation 관리
-- =============================================
CREATE TABLE client_secrets (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    client_id   VARCHAR(100) NOT NULL COMMENT 'oauth_clients.client_id 참조',
    version     INT          NOT NULL COMMENT '발급 차수. Rotation 추적용',
    secret_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시 (원본 저장 불가)',
    description VARCHAR(50)  NULL     COMMENT '관리용 메모 (예: Initial Key, Rotation 2026-03)',
    expires_at  TIMESTAMP    NULL     COMMENT '자연 만료일 또는 Rotation 유예 기간 종료 시점',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL     COMMENT 'Soft Delete',
    PRIMARY KEY (id),
    INDEX idx_client_id (client_id),
    INDEX idx_client_version (client_id, version),
    INDEX idx_expires_at (expires_at),
    CONSTRAINT fk_secrets_client_id
        FOREIGN KEY (client_id) REFERENCES oauth_clients(client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- system_scopes: 시스템 정의 Scope 메타데이터
-- =============================================
CREATE TABLE system_scopes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    scope_code  VARCHAR(50)  NOT NULL COMMENT '예: order:read',
    category    VARCHAR(50)  NOT NULL COMMENT '예: ORDER',
    description VARCHAR(200) NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scope_code (scope_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Data
INSERT INTO system_scopes (scope_code, category, description) VALUES
  ('product:read',    'PRODUCT',    '상품 조회'),
  ('product:write',   'PRODUCT',    '상품 등록/수정'),
  ('product:delete',  'PRODUCT',    '상품 삭제'),
  ('order:read',      'ORDER',      '주문 조회'),
  ('order:write',     'ORDER',      '주문 처리'),
  ('claim:read',      'CLAIM',      '클레임 조회'),
  ('claim:update',    'CLAIM',      '클레임 처리'),
  ('stats:read',      'STATS',      '통계 조회'),
  ('marketing:read',  'MARKETING',  '마케팅 조회'),
  ('marketing:write', 'MARKETING',  '마케팅 실행'),
  ('settlement:read', 'SETTLEMENT', '정산 조회');
```

### 7-2. 상태 도출 로직

`status` 컬럼 없이 조건으로 도출:

| 상태 | 도출 조건 |
|------|---------|
| `ACTIVE` | `deleted_at IS NULL AND expires_at > NOW()` |
| `EXPIRING` | Rotation 후 구버전 secret의 `expires_at`이 유예기간 내 |
| `EXPIRED` | `deleted_at IS NULL AND expires_at <= NOW()` |
| `DELETED` | `deleted_at IS NOT NULL` |

### 7-3. 핵심 쿼리 패턴

**토큰 발급 시 검증:**
```sql
SELECT oc.id, oc.scopes, oc.tenant_id, cs.secret_hash
FROM oauth_clients oc
JOIN client_secrets cs ON oc.client_id = cs.client_id
WHERE oc.client_id = ?
  AND oc.deleted_at IS NULL
  AND oc.expires_at > NOW()
  AND cs.deleted_at IS NULL
  AND cs.expires_at > NOW()
ORDER BY cs.version DESC;
-- 복수 행 반환 시 (Dual Activation 구간) 모두 BCrypt 대조
```

**Rotation 처리:**
```sql
-- Step 1: 구버전 유예 기간 설정
UPDATE client_secrets
SET expires_at = DATE_ADD(NOW(), INTERVAL 6 HOUR)
WHERE client_id = ? AND version = (SELECT MAX(version) FROM client_secrets WHERE client_id = ?);

-- Step 2: 신버전 INSERT
INSERT INTO client_secrets (client_id, version, secret_hash, description)
VALUES (?, ?, ?, 'Rotation 2026-03');
```

---

## 8. 보안 정책

### 8-1. Secret 생성 원칙

```java
// 올바른 방법: CSPRNG (SecureRandom)
SecureRandom secureRandom = new SecureRandom();
byte[] bytes = new byte[32];
secureRandom.nextBytes(bytes);
String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

// 금지: 예측 가능한 난수
// Math.random(), new Random() → 절대 사용 금지
```

BCrypt 해시:
```java
PasswordEncoder encoder = new BCryptPasswordEncoder(10); // strength >= 10
String secretHash = encoder.encode(rawSecret);
// 저장 후 rawSecret은 응답에만 포함, 메모리에서 제거
```

### 8-2. Dual Activation 상세 타임라인

```
T+0h: Rotate API 호출
       → v2 INSERT (expires_at: NULL)
       → v1 expires_at = T+6h

T+0h ~ T+6h: 이중 활성 구간
       → v1, v2 모두 BCrypt 대조 허용
       → 서버 코드를 새 secret으로 교체하는 유예 기간

T+6h: v1 자동 만료 (expires_at < NOW())
       → v2만 유효

T+6h 이후: (선택적 배치) v1 Soft Delete
```

### 8-3. Redis 블랙리스트 메커니즘

즉시 차단이 필요한 경우 (JWT는 Stateless라 즉시 무효화 불가):

```
계정 정지 처리:
  Redis SET SUSPENDED:{client_id} = "1"
  TTL = 해당 JWT의 남은 exp 시간

Gateway 검증:
  Redis GET SUSPENDED:{client_id}
  존재하면 → 403 즉시 반환 (JWT 유효해도 차단)
  O(1) 성능, DB 조회 없음
```

### 8-4. JWKS 키 롤오버 전략

키 교체 시 인증 장애 방지:

```
키 교체 중:
  JWKS에 구형 키(kid=v1)와 신규 키(kid=v2) 동시 노출
  (병행 기간: 최소 JWKS 캐시 TTL × 2 = 30분 권장)

Gateway 동작:
  JWT 헤더의 kid 확인 → 로컬 캐시에 없으면 → JWKS 즉시 재조회
  → 이후 신규 kid로 검증
```

### 8-5. 에러 응답 정보 최소화

```
금지:
  × "client_id 'prod_svc_abc'가 존재하지 않습니다" (존재 여부 힌트)
  × Stack trace 노출
  × 내부 DB 쿼리 메시지
  × 서버 IP, 내부 도메인

허용:
  ✓ "인증에 실패했습니다" (범용 메시지)
  ✓ error_code (클라이언트 파싱용)
  ✓ request_id (추적용, 내부 로그와 연결)
```

---

## 9. 모니터링 및 로깅

### 9-1. Audit Log 스키마

```json
{
  "timestamp": "2026-03-05T10:00:00.000Z",
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "client_id": "prod_svc_x8s7abc",
  "tenant_id": 1001,
  "method": "POST",
  "path": "/api/v1/products",
  "required_action": "product:write",
  "result": "ALLOW",
  "status_code": 201,
  "latency_ms": 45,
  "error_code": null
}
```

### 9-2. 주요 메트릭

**IdP:**

| 메트릭 | 타입 | 임계치 |
|--------|------|--------|
| `idp_token_issued_total` | Counter | - |
| `idp_auth_failure_total` | Counter | 급증 시 알람 (Brute-force 의심) |
| `idp_token_latency_p99` | Histogram | >500ms Warning / >1s Critical |
| `idp_bcrypt_duration` | Histogram | CPU 과부하 감지 |
| `idp_secret_rotated_total` | Counter | 급증 시 관리자 계정 탈취 의심 |

**Gateway:**

| 메트릭 | 타입 | 임계치 |
|--------|------|--------|
| `gw_auth_401_total` | Counter | 급증 시 토큰 탈취/SDK 오구현 의심 |
| `gw_authz_403_total` | Counter | >1% 시 권한 오설정 의심 |
| `gw_rate_limit_429_total` | Counter | 급증 시 루프 버그/DDoS 의심 |
| `gw_suspended_403_total` | Counter | >0 즉시 알람 |
| `gw_auth_latency_p99` | Histogram | >10ms |
| `gw_overhead_latency_p95` | Histogram | >25ms Scale-out 검토 |
| `gw_jwks_cache_hit_rate` | Gauge | <99% 위험 (캐시 TTL 확인) |

### 9-3. 로그 파이프라인

```
Application (Logback → JSON)
    │  /logs/api-access.json
    │  Rolling: 100MB/file, 최대 3일 보관
    ▼
[Log Aggregator] (Vector / Fluentd / Logstash)
    ▼
[Message Queue] (Kafka 등)
    ▼
[Object Storage] (S3 등) → [Query Engine] (Athena 등)
```

---

## 10. Phase 로드맵

### Phase 1: 보안 컴플라이언스 + 서비스 런칭 (현재)

**목표:** 법적 리스크 해소, 보안 표준 수립, Self-Service 운영

| 시스템 | 필수 작업 | 해결되는 문제 |
|--------|---------|-------------|
| **IdP** | Client Credentials 발급, JWKS, Secret Rotation, Soft Delete, `expires_at` 검증 | 인증 취약성, 좀비 계정 |
| **Gateway** | JWT 검증, Scope 인가, Rate Limiting, Audit Log, Redis 블랙리스트 | 과도한 권한, 블랙박스 운영 |
| **Tenant Admin** | ServiceKey CRUD (FE + BE), Secret 1회 노출 UI, Rotation UI | 수동 운영, 관리 부재 |

### Phase 2: 고도화 (이후)

| 시스템 | 작업 | 기대 효과 |
|--------|------|---------|
| Gateway | IP Allow List (클라이언트 서버 IP 화이트리스트) | 토큰 탈취 시 2차 방어선 |
| Gateway | 테넌트 등급별 Rate Limit 차등 적용 | SLA 차별화 |
| IdP | `private_key_jwt` 인증 옵션 | 엔터프라이즈 티어 보안 강화 |
| 전체 | Open API SDK (Java/Python) | 연동 시간 단축 |
| 전체 | API 문서 자동화 (OpenAPI 3.0 Spec) | 문서 불일치 해소 |

### Phase 3: 레거시 마이그레이션

```
현재:   기존 클라이언트 → V1 API (직접)
Phase1: 신규 클라이언트 → V2 API (Gateway 필수)
Phase2: 기존 클라이언트 전환 유도, V1 신규 발급 중단
Phase3: V1 엔드포인트 종료
```

---

## 참고 문서

- [RFC 6749: OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [RFC 7517: JSON Web Key (JWK)](https://tools.ietf.org/html/rfc7517)
- [RFC 7519: JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [Spring Cloud Gateway 공식 문서](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [nimbus-jose-jwt 라이브러리](https://connect2id.com/products/nimbus-jose-jwt)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
