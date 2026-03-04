# Book Management API

Spring JDBC를 사용한 간단한 도서 관리 REST API 프로젝트입니다.

## 프로젝트 구조

```
book-management-api/
├── build.gradle                 # Gradle 설정 파일
├── CLAUDE.md                    # Claude Code 프로젝트 규칙
├── README.md                    # 이 파일
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/example/bookmanagement/
    │   │   ├── BookManagementApplication.java  # Spring Boot 메인 클래스
    │   │   ├── controller/
    │   │   │   └── BookController.java        # HTTP 요청 처리
    │   │   ├── service/
    │   │   │   └── BookService.java           # 비즈니스 로직
    │   │   ├── repository/
    │   │   │   └── BookRepository.java        # JDBC 데이터 접근 (JdbcTemplate)
    │   │   ├── domain/
    │   │   │   └── Book.java                  # 도메인 모델
    │   │   └── dto/
    │   │       ├── BookRequest.java
    │   │       └── BookResponse.java
    │   └── resources/
    │       ├── application.yml                # Spring 설정
    │       ├── schema.sql                     # DB 테이블 생성
    │       └── data.sql                       # 샘플 데이터
    └── test/
```

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring JDBC (JdbcTemplate)**
- **H2 Database**
- **Gradle**
- **Lombok**

## 시작하기

### 1. 프로젝트 빌드
```bash
cd c:/work/book-management-api
gradle build
```

### 2. 애플리케이션 실행
```bash
gradle bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다.

### 3. H2 콘솔 접속 (선택사항)
```
http://localhost:8080/h2-console
```

## API 사용 예시

### 1. 도서 등록 (POST)
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot Guide",
    "author": "John Doe",
    "isbn": "978-1234567890",
    "price": 39.99
  }'
```

### 2. 도서 전체 조회 (GET)
```bash
curl http://localhost:8080/api/books
```

### 3. 특정 도서 조회 (GET)
```bash
curl http://localhost:8080/api/books/1
```

### 4. 도서 수정 (PUT)
```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "author": "Jane Doe",
    "isbn": "978-0987654321",
    "price": 49.99
  }'
```

### 5. 도서 삭제 (DELETE)
```bash
curl -X DELETE http://localhost:8080/api/books/1
```

## 데이터베이스 스키마

### books 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | 도서 ID (PK, AUTO_INCREMENT) |
| title | VARCHAR(255) | 도서 제목 |
| author | VARCHAR(255) | 저자 |
| isbn | VARCHAR(20) | ISBN (UNIQUE) |
| price | DECIMAL(10, 2) | 가격 |
| created_at | TIMESTAMP | 생성일시 |

## 샘플 데이터

애플리케이션 시작 시 다음 4권의 샘플 도서가 자동으로 로드됩니다:
- Spring in Action
- Clean Code
- Effective Java
- Design Patterns
