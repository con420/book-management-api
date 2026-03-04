# CouponBiz 프로젝트 코드 분석 보고서

## 📊 프로젝트 개요
- **이름**: CouponBiz (쿠폰 관리 API)
- **기술 스택**: Java 17, Spring Boot 3.2.0, MyBatis, MySQL, Gradle, Lombok
- **아키텍처**: Controller → Service → Mapper 구조
- **주요 기능**: 쿠폰 생성/발급/사용/만료 관리

---

## ✅ 장점

### 1. **계층적 구조 설계**
- Controller, Service, Mapper 계층이 명확하게 분리됨
- DTO를 통한 요청/응답 분리로 API 계약이 명확함
- 단일 책임 원칙(SRP) 준수

### 2. **예외 처리**
- `GlobalExceptionHandler`를 통한 중앙화된 예외 처리
- 비즈니스 로직에 따른 세분화된 예외 정의 (NotFound, AccessDenied, AlreadyUsed 등)
- 적절한 HTTP 상태 코드 반환

### 3. **도메인 모델링**
- 도메인 객체(`Coupon`, `CouponInfo`)에 비즈니스 로직이 포함됨 (도메인 주도 설계)
- `isUsable()`, `use()`, `expire()` 메서드로 상태 관리가 명확함
- 불변성 검증이 도메인 계층에서 이루어짐

### 4. **트랜잭션 관리**
- `@Transactional` 어노테이션으로 데이터 일관성 보장
- 쿠폰 발급, 사용, 만료 작업이 원자성(Atomicity)을 만족함

### 5. **스케줄러 활용**
- `@Scheduled` 어노테이션으로 만료된 쿠폰을 자동 처리
- 백그라운드 작업 분리

---

## ⚠️ 주요 문제점

### 1. **Race Condition / 동시성 문제** 🔴 (심각)
**위치**: `CouponServiceImpl.useCoupon()` (78-103줄)

```java
Coupon coupon = couponMapper.selectByCouponCode(couponCode); // 1단계
if (coupon.getStatus() == CouponStatus.USED) {               // 2단계: 검증
    throw new CouponAlreadyUsedException("...");
}
coupon.use();                                                 // 3단계: 상태 변경
couponMapper.updateStatus(coupon);                           // 4단계: DB 저장
```

**문제점**:
- 같은 쿠폰을 2명 이상이 동시에 사용하려 할 때, 검증(2단계) → 변경(3단계) 사이에 시간 차이 발생
- 두 개의 요청이 동시에 진행되면 중복 사용이 발생할 수 있음

**시나리오**:
```
Thread A: SELECT 쿠폰 → 상태=ISSUED (O)
Thread B: SELECT 쿠폰 → 상태=ISSUED (O)
Thread A: UPDATE 상태=USED
Thread B: UPDATE 상태=USED (중복 사용됨!)
```

**해결방안**:
- `SELECT ... FOR UPDATE` (pessimistic locking) 또는
- 데이터베이스 레벨에서 `UNIQUE` 제약 조건 활용 또는
- 낙관적 잠금(optimistic locking) 구현

---

### 2. **RATE 할인 타입 미구현** 🟡 (중간)
**위치**: `CouponInfo.getDiscountAmount()` (19-25줄)

```java
public int getDiscountAmount(int orderAmount) {
    if (discountType == DiscountType.FIXED) {
        return Math.min(orderAmount, discountValue);
    }
    // TODO: RATE 타입 구현
    return 0; // ← RATE 타입은 항상 0원 할인!
}
```

**문제점**:
- RATE(비율) 할인이 정의되어 있지만 구현이 안 됨
- RATE 타입 쿠폰 사용 시 할인금액이 항상 0원이 됨
- 데이터 불일치 가능성

**해결방안**:
```java
public int getDiscountAmount(int orderAmount) {
    if (discountType == DiscountType.FIXED) {
        return Math.min(orderAmount, discountValue);
    } else if (discountType == DiscountType.RATE) {
        return (int) (orderAmount * (discountValue / 100.0));
    }
    return 0;
}
```

---

### 3. **약한 쿠폰 코드 생성** 🟡 (중간)
**위치**: `CouponCodeGenerator.generate()` (8-14줄)

```java
public static String generate(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
        int idx = (int) (Math.random() * CHARS.length()); // ← 문제!
        sb.append(CHARS.charAt(idx));
    }
    return sb.toString();
}
```

**문제점**:
- `Math.random()`은 암호학적으로 안전하지 않음
- 예측 가능한 난수 생성
- 쿠폰 코드 12자리 중복 확률 높음 (Birthday Paradox)
- UNIQUE 제약이 있으므로 충돌 시 삽입 실패 (사용자 경험 악화)

**예상 충돌 확률**:
- 36^12 ≈ 4.7 * 10^18 가능한 조합
- 하지만 약한 난수 생성으로 실제로는 훨씬 높음

**해결방안**:
```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();

public static String generate(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
        int idx = SECURE_RANDOM.nextInt(CHARS.length());
        sb.append(CHARS.charAt(idx));
    }
    return sb.toString();
}
```

---

### 4. **스케줄러 Cron 표현식 오류** 🔴 (심각)
**위치**: `CouponScheduler.processCouponExpiration()` (13줄)

```java
@Scheduled(cron = " 0 */1 * * * *") // ← 공백이 앞에 있음!
public void processCouponExpiration() {
    couponService.expireCoupons();
}
```

**문제점**:
- Cron 표현식이 `" 0 */1 * * * *"` (앞에 공백)
- 올바른 형식: `"0 */1 * * * *"` (공백 없음)
- 스케줄러가 정상적으로 작동하지 않을 가능성

**현재 의도**: 1분마다 실행 (`*/1`)
- 초: 0초
- 분: 매 1분마다
- 시: 모든 시간
- 일: 모든 날짜
- 월: 모든 월
- 요일: 모든 요일

**해결**:
```java
@Scheduled(cron = "0 */1 * * * *")
```

---

### 5. **만료된 쿠폰 검증 누락** 🟡 (중간)
**위치**: `CouponServiceImpl.useCoupon()` (78-102줄)

```java
@Transactional
public CouponLog useCoupon(String couponCode, Long userId, int orderAmount) {
    Coupon coupon = couponMapper.selectByCouponCode(couponCode);
    if (coupon == null) {
        throw new CouponNotFoundException("...");
    }
    if (!coupon.getUserId().equals(userId)) {
        throw new CouponAccessDeniedException("...");
    }
    if (coupon.getStatus() == CouponStatus.USED) {
        throw new CouponAlreadyUsedException("...");
    }
    // ← 만료된 쿠폰 검증이 없음! (Coupon.use() 메서드에만 있음)
    coupon.use(); // 여기서 만료 검증
    // ...
}
```

**문제점**:
- 만료 검증이 `Coupon.use()` 메서드 내부에서만 이루어짐
- 서비스 계층에서는 알 수 없음
- 엣지 케이스 발생 가능 (예: 매우 만료된 쿠폰은 DB에서 이미 CANCEL 상태일 수 있음)

**개선안**:
```java
public CouponLog useCoupon(String couponCode, Long userId, int orderAmount) {
    Coupon coupon = couponMapper.selectByCouponCode(couponCode);
    // ...
    // 명시적인 만료 검증
    if (coupon.getCouponInfo().getExpiredAt().isBefore(LocalDateTime.now())) {
        throw new CouponExpiredException("만료된 쿠폰입니다.");
    }
    coupon.use();
    // ...
}
```

---

### 6. **SQL Join의 비효율성** 🟡 (중간)
**위치**: `CouponMapper.xml` (33-37줄, 다른 쿼리들)

```xml
<select id="selectByCouponCode" resultMap="CouponResultMap">
    SELECT *
    FROM coupon c, coupon_info ci
    WHERE c.coupon_info_id = ci.coupon_info_id
    AND c.coupon_code = #{couponCode}
</select>
```

**문제점**:
- 암시적 JOIN (Implicit Join) 사용
- 쿼리 최적화 어려움
- 인덱스 활용이 덜 명시적

**개선안**:
```xml
<select id="selectByCouponCode" resultMap="CouponResultMap">
    SELECT *
    FROM coupon c
    INNER JOIN coupon_info ci ON c.coupon_info_id = ci.coupon_info_id
    WHERE c.coupon_code = #{couponCode}
</select>
```

---

### 7. **NULL 체크 불일관성** 🟡 (중간)
**위치**: `CouponServiceImpl` (48줄, 69줄, 79줄)

```java
// 48줄
CouponInfo info = couponInfoMapper.selectById(couponInfoId);
if (info == null) {
    throw new CouponNotFoundException("...");
}

// 69줄
Coupon coupon = couponMapper.selectByCouponCode(couponCode);
if (coupon == null) {
    throw new CouponNotFoundException("...");
}

// 79줄
Coupon coupon = couponMapper.selectByCouponCode(couponCode);
if (coupon == null) {
    throw new CouponNotFoundException("...");
}
```

**문제점**:
- NULL 체크가 매번 반복됨 (DRY 원칙 위반)
- 코드 중복

**개선안**:
```java
// Mapper에서 직접 예외 처리
public Optional<Coupon> selectByCouponCode(String couponCode);

// Service에서
Coupon coupon = couponMapper.selectByCouponCode(couponCode)
    .orElseThrow(() -> new CouponNotFoundException("..."));
```

---

### 8. **트랜잭션 처리 부재 - 대량 만료 처리** 🟡 (중간)
**위치**: `CouponServiceImpl.expireCoupons()` (114-130줄)

```java
@Transactional
public void expireCoupons() {
    List<Coupon> expiredCoupons = couponMapper.selectExpiredCoupons(LocalDateTime.now());

    for (Coupon coupon : expiredCoupons) {
        coupon.expire();
        couponMapper.updateStatus(coupon); // ← 개별 업데이트

        CouponLog log = CouponLog.builder()...build();
        couponLogMapper.insert(log); // ← 개별 삽입
    }
    // 한 쿠폰 만료 실패 시 전체 롤백됨 (의도적이긴 하지만 성능 문제)
}
```

**문제점**:
- 대량의 UPDATE/INSERT가 개별적으로 실행됨
- DB 왕복이 많음
- 성능 저하 (1000개 쿠폰 = 2000번의 쿠폰 업데이트/삽입)
- TODO 주석: "쿠폰 만료 실패 시 알람처리" 미구현

**개선안** (배치 업데이트):
```java
@Transactional
public void expireCoupons() {
    List<Coupon> expiredCoupons = couponMapper.selectExpiredCoupons(LocalDateTime.now());
    if (!expiredCoupons.isEmpty()) {
        couponMapper.updateStatusBatch(expiredCoupons, CouponStatus.CANCEL);
        couponLogMapper.insertBatch(expiredCoupons.stream()
            .map(c -> CouponLog.builder()...build())
            .collect(Collectors.toList()));
    }
}
```

---

### 9. **Lombok @Setter 사용** 🟡 (중간)
**위치**: `Coupon.java`, `CouponInfo.java` 등

```java
@Getter
@Setter // ← 문제!
@NoArgsConstructor
@AllArgsConstructor
public class Coupon { ... }
```

**문제점**:
- 모든 필드에 Setter가 생성됨
- 도메인 객체의 상태가 언제든 변경될 수 있음
- 의도하지 않은 수정 가능 (예: `coupon.setCouponId(999)`)
- 도메인 객체의 불변성 위반

**개선안**:
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    // @Setter 제거
    // use(), expire() 메서드로만 상태 변경
}
```

---

### 10. **Admin Controller 미검토** 🟡 (중간)
**위치**: `CouponAdminController.java` (미읽음)

보안 검증이 있는지 확인 필요:
- 인증/인가 체크
- 요청 유효성 검증
- 권한 검사

---

### 11. **로깅 부재** 🟡 (중간)

**문제점**:
- 디버깅 어려움
- 감시/모니터링 불가능
- 쿠폰 사용 히스토리 추적 어려움

**개선안**:
```java
log.info("쿠폰 발급: couponInfoId={}, userId={}, couponCode={}",
         couponInfoId, userId, couponCode);
log.warn("중복 사용 시도: couponCode={}, userId={}", couponCode, userId);
```

---

### 12. **입력 유효성 검증 부재** 🟡 (중간)

**위치**: Controller의 요청 DTO들

**문제점**:
- `CouponInfoCreateRequest`에서 `discountValue` 음수 체크 없음
- `orderAmount` 음수 체크 없음
- 조회 요청의 `userId` 유효성 검증 없음

**개선안**:
```java
@Data
public class CouponInfoCreateRequest {
    @NotBlank(message = "할인 타입은 필수입니다")
    private String discountType;

    @Positive(message = "할인값은 양수여야 합니다")
    private int discountValue;

    @FutureOrPresent(message = "만료일은 미래여야 합니다")
    private LocalDateTime expiredAt;
}
```

---

## 📈 개선 우선순위

| 순위 | 문제 | 심각도 | 영향범위 | 해결시간 |
|------|------|--------|---------|---------|
| 1 | Race Condition | 🔴 높음 | 데이터 무결성 | 2시간 |
| 2 | Cron 표현식 오류 | 🔴 높음 | 스케줄러 작동 | 5분 |
| 3 | RATE 할인 미구현 | 🟡 중간 | 기능 | 30분 |
| 4 | 약한 난수 생성 | 🟡 중간 | 보안 | 15분 |
| 5 | 만료 검증 로직 | 🟡 중간 | 로직 정합성 | 20분 |
| 6 | SQL JOIN 형식 | 🟡 중간 | 성능/가독성 | 30분 |
| 7 | 배치 처리 부재 | 🟡 중간 | 성능 | 1시간 |
| 8 | @Setter 제거 | 🟡 중간 | 설계 | 30분 |
| 9 | 입력 검증 | 🟡 중간 | 보안/안정성 | 45분 |
| 10 | 로깅 부재 | 🟡 중간 | 운영성 | 1시간 |

---

## 🎯 권장사항

### 즉시 해결 (Critical)
1. ✅ Race Condition - 낙관적 또는 비관적 잠금 도입
2. ✅ Cron 표현식 수정 - 공백 제거

### 단기 개선 (High)
3. RATE 할인 로직 구현
4. SecureRandom 적용
5. 입력 유효성 검증 추가 (@Validated, Bean Validation)
6. @Setter 제거 및 불변성 강화

### 중기 개선 (Medium)
7. SQL JOIN 명시적 형식 변경
8. 배치 처리 쿼리 작성
9. 로깅 프레임워크 도입 (SLF4J, Logback)
10. 통합 테스트 작성

---

## 📝 결론

**전체 평가**: ⭐⭐⭐ (3/5)

### 좋은 점
- 아키텍처가 명확하고 계층이 잘 분리됨
- 예외 처리가 체계적
- 도메인 주도 설계 시도

### 개선 필요한 점
- **동시성 안전성** 부족 (가장 중요!)
- 완성되지 않은 기능들
- 보안 및 검증 미흡
- 성능 최적화 필요

### 다음 단계
1. Race Condition 해결이 최우선
2. 테스트 코드 작성으로 안정성 확보
3. 모니터링/로깅 추가
4. 성능 테스트 및 최적화

