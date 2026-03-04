# Book Management API - 프로젝트 규칙

## 프로젝트 개요
Spring JDBC를 사용한 도서 관리 REST API입니다. Spring Data JPA는 사용하지 않으며, JdbcTemplate과 RowMapper를 활용합니다.

## 기술 스택
- **Framework**: Spring Boot 3.2.0
- **언어**: Java 17
- **빌드 도구**: Gradle (Groovy DSL)
- **데이터베이스**: H2 (In-memory)
- **ORM**: Spring JDBC (JPA 없음)
- **라이브러리**: Lombok

## 아키텍처
- **Controller**: HTTP 요청 처리
- **Service**: 비즈니스 로직 담당
- **Repository**: JDBC를 통한 데이터 접근 (JdbcTemplate)
- **Domain**: 엔티티 클래스
- **DTO**: 요청/응답 데이터 전달

## 코드 스타일
- Java 17 문법 사용
- Lombok 어노테이션으로 보일러플레이트 코드 최소화
- 생성자 주입을 통한 DI (필드 주입 금지)
- 메서드 체인이 필요한 경우를 제외하고 한 줄 원칙 준수

## JDBC 규칙
- JdbcTemplate 사용은 필수 (JPA 또는 Hibernate 금지)
- RowMapper를 이용한 결과 매핑
- SQL 쿼리는 직접 작성

## 데이터베이스 초기화
- `schema.sql`: 테이블 생성
- `data.sql`: 샘플 데이터 입력
- H2 콘솔: `http://localhost:8080/h2-console`

## API 엔드포인트
```
POST   /api/books          - 도서 등록
GET    /api/books          - 도서 전체 조회
GET    /api/books/{id}     - 특정 도서 조회
PUT    /api/books/{id}     - 도서 정보 수정
DELETE /api/books/{id}     - 도서 삭제
```

## 실행 방법
```bash
cd c:/work/book-management-api
gradle bootRun
```

## 금지사항
- Spring Data JPA 사용 금지
- @Autowired 필드 주입 금지
- Raw SQL 문자열 연결 (파라미터화된 쿼리만 사용)
- 예외처리 없이 null 반환 금지
