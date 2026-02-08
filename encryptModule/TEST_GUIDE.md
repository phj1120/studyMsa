# 테스트 코드 가이드

## 빠른 시작

### 실무 테스트 파일

#### CompareV1V2Test.kt ⭐
**목적:** V1과 V2가 동일한 결과를 반환하는지 검증

```kotlin
@SpringBootTest
@Transactional
class CompareV1V2Test {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun fakeExternalApiClient(): ExternalApiClient {
            return FakeExternalApiClient()
        }
    }

    @Test
    fun `V1과 V2의 getMember 결과가 동일해야 한다`() {
        // Given
        val request = MemberRequest(name = "홍길동", phoneNumber = "01012345678", age = 29)

        val savedV1 = memberServiceV1.createMember(request)
        val savedV2 = memberServiceV2.createMember(request)

        // When
        val resultV1 = memberServiceV1.getMember(savedV1.id!!)
        val resultV2 = memberServiceV2.getMember(savedV2.id!!)

        // Then
        assertEquals(resultV1.name, resultV2.name)
        assertEquals(resultV1.phoneNumber, resultV2.phoneNumber)
        assertEquals(resultV1.age, resultV2.age)
    }
}
```

**특징:**
- @SpringBootTest 사용 - 실제 JPA + DB 동작 검증
- @Transactional - 테스트 후 자동 롤백
- FakeExternalApiClient 사용 - 외부 API Mock 대체

**언제 실행?**
- JPA Converter/Listener 추가/수정 시
- V1 → V2 마이그레이션 검증 시
- DB 스키마 변경 시
- Merge 전 최종 검증

---

## 핵심 개념

### Fake 패턴

**FakeExternalApiClient.kt**
```kotlin
class FakeExternalApiClient : ExternalApiClient {
    var shouldValidate: Boolean = true

    override fun validatePhoneNumber(phoneNumber: String): Boolean {
        return shouldValidate
    }
}
```

**왜 Fake를 사용하는가?**

Mock 대신 Fake를 사용하는 이유:
- ✅ **간단함**: Mock 설정 코드 불필요
- ✅ **재사용**: 모든 테스트에서 공통 사용
- ✅ **유지보수**: 한 곳에서만 관리
- ✅ **가독성**: 테스트 코드가 깔끔함

**Mock vs Fake 비교:**

```kotlin
// ❌ Mock 사용 (매번 설정 필요)
val mockApiClient = mockk<ExternalApiClient>()
every { mockApiClient.validatePhoneNumber(any()) } returns true
every { mockApiClient.validatePhoneNumber("invalid") } returns false

// ✅ Fake 사용 (한 번만 구현)
val fakeApiClient = FakeExternalApiClient()
fakeApiClient.shouldValidate = false  // 필요시에만 변경
```

---

## 실행 방법

### Gradle 명령어

```bash
# 모든 테스트
./gradlew test

# CompareV1V2Test만
./gradlew test --tests CompareV1V2Test

# 특정 메서드만
./gradlew test --tests "CompareV1V2Test.V1과 V2의 getMember 결과가 동일해야 한다"

# 테스트 결과 보고서
open build/reports/tests/test/index.html
```

### IDE에서 실행

1. 테스트 클래스/메서드 옆 녹색 화살표 클릭
2. 우클릭 → Run 'CompareV1V2Test'
3. 단축키: Ctrl+Shift+F10 (Windows), Cmd+Shift+R (Mac)

---

## 베스트 프랙티스

### 1. AAA 패턴 (Arrange-Act-Assert)

```kotlin
@Test
fun `테스트 설명`() {
    // Given (Arrange): 테스트 준비
    val request = MemberRequest(...)

    // When (Act): 실제 동작
    val result = service.getMember(id)

    // Then (Assert): 결과 검증
    assertEquals(expected, result)
}
```

### 2. 명확한 테스트 이름

```kotlin
// ✅ 좋은 예: 한글로 명확하게
@Test
fun `V1과 V2의 결과가 동일해야 한다`()

// ✅ 좋은 예: 영문
@Test
fun `should return same result for v1 and v2`()

// ❌ 나쁜 예
@Test
fun test1()
```

### 3. 하나의 테스트는 하나의 관심사만

```kotlin
// ✅ 좋은 예
@Test
fun `멤버 생성 성공`()

@Test
fun `멤버 조회 성공`()

// ❌ 나쁜 예: 여러 관심사 혼재
@Test
fun `멤버 생성하고 조회하고 수정하고 삭제`()
```

### 4. Fake를 사용한 외부 의존성 처리

```kotlin
// ✅ 좋은 예: Fake 사용
@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun fakeExternalApiClient() = FakeExternalApiClient()
}

// ❌ 나쁜 예: 매번 Mock 설정
@Test
fun test1() {
    val mock = mockk<ExternalApiClient>()
    every { mock.validatePhoneNumber(any()) } returns true
    // ...
}
```

---

## 테스트 작성 가이드

### 새로운 테스트 추가하기

**1. CompareV1V2Test에 메서드 추가**

```kotlin
@Test
fun `새로운 API 엔드포인트 V1과 V2 동일성 검증`() {
    // Given
    val request = MemberRequest(...)
    val savedV1 = memberServiceV1.createMember(request)
    val savedV2 = memberServiceV2.createMember(request)

    // When
    val resultV1 = memberServiceV1.newMethod(savedV1.id!!)
    val resultV2 = memberServiceV2.newMethod(savedV2.id!!)

    // Then
    assertEquals(resultV1, resultV2)
}
```

**2. 외부 API 동작 변경이 필요한 경우**

```kotlin
@Test
fun `전화번호 검증 실패시 예외 발생`() {
    // Given
    val fakeClient = FakeExternalApiClient()
    fakeClient.shouldValidate = false  // 검증 실패하도록 설정

    val service = MemberServiceV2(repository, fakeClient)
    val request = MemberRequest(name = "홍길동", phoneNumber = "invalid", age = 29)

    // When & Then
    assertThrows<IllegalArgumentException> {
        service.createMember(request)
    }
}
```

---

## 트러블슈팅

### Q1. 테스트가 실패해요
**A:** 먼저 로그를 확인하세요
```bash
./gradlew test --tests CompareV1V2Test --info
```

### Q2. Spring 부팅이 너무 느려요
**A:** 현재는 통합 테스트만 있어서 불가피합니다. 빠른 피드백이 필요하면 단위 테스트(Mock 기반)를 추가하세요.

### Q3. FakeExternalApiClient를 수정하고 싶어요
**A:** `FakeExternalApiClient.kt` 파일을 직접 수정하면 됩니다. 모든 테스트에 적용됩니다.

### Q4. 외부 API 호출 동작을 테스트별로 다르게 하려면?
**A:** FakeExternalApiClient에 설정 메서드 추가:
```kotlin
class FakeExternalApiClient : ExternalApiClient {
    private val validPhoneNumbers = mutableSetOf<String>()

    fun addValidPhoneNumber(phoneNumber: String) {
        validPhoneNumbers.add(phoneNumber)
    }

    override fun validatePhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber in validPhoneNumbers
    }
}
```

---

## 파일 구조

```
encryptModule/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── org/example/encryptmodule/
│   │           ├── v1/
│   │           │   ├── MemberService.kt
│   │           │   └── MemberRepository.kt
│   │           ├── v2/
│   │           │   ├── MemberServiceV2.kt
│   │           │   └── MemberRepositoryV2.kt
│   │           └── external/
│   │               └── ExternalApiClient.kt
│   └── test/
│       └── kotlin/
│           └── org/example/encryptmodule/
│               ├── CompareV1V2Test.kt          ⭐ 실무 테스트
│               ├── FakeExternalApiClient.kt    ⭐ Fake 구현체
│               └── TEST_GUIDE.md              📖 이 파일
```

---

## 정리

### 핵심 원칙

1. **실제 동작 검증**: @SpringBootTest로 JPA 실제 동작 확인
2. **Fake 활용**: Mock 대신 Fake로 간단하고 재사용 가능하게
3. **명확한 테스트**: 이름만 보고도 의도 파악 가능
4. **자동화**: CI/CD에 통합

### 테스트 전략

| 목적 | 방법 | 파일 |
|------|------|------|
| V1/V2 동일성 검증 | @SpringBootTest + Fake | CompareV1V2Test.kt |
| 외부 API Mock | Fake 구현 | FakeExternalApiClient.kt |

### 주요 차이점

**이전 (복잡함):**
- CompareV1V2Test (통합 테스트)
- FastCompareV1V2Test (단위 테스트)
- MockExampleTest (학습용)
- FakeExampleTest (학습용)
- WireMockExampleTest (학습용)
- BulkEndpointTest (대량 테스트)

**현재 (단순함):**
- CompareV1V2Test (실무 테스트) ⭐
- FakeExternalApiClient (Fake 구현) ⭐

**2개 파일로 충분합니다!**

---

## CI/CD 통합 예제

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Run tests
        run: ./gradlew test

      - name: Publish test report
        if: always()
        uses: mikepenz/action-junit-report@v3
        with:
          report_paths: '**/build/test-results/test/TEST-*.xml'
```

---

Happy Testing! 🚀
