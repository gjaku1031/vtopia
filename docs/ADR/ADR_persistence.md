# JPA 중심 영속성과 jOOQ 코드 생성

기준일: 2026-10-04. 상태: 채택.

## 결정

ken-blog의 JPA 중심 구조를 유지하고 DB dialect·드라이버·테스트 컨테이너를 PostgreSQL로 변경한다. Spring Boot 4.1.1, JDK 25, Gradle 9.3.0, jOOQ 3.21.8은 참조 프로젝트와 맞춘다. 언어는 2026-10-07부터 Kotlin 대신 Java를 사용하며 빌드 스크립트는 Groovy DSL로 작성한다.

JPA는 저장·단일 조회·변경 잠금, jOOQ는 복잡한 읽기를 담당한다. 영속 모델의 원본은 JPA 엔티티다. jOOQ 전용 SQL을 수동 관리하거나 빌드 중 실제 DB에 접속하지 않는다.

```text
domain Java
  → compileJpaModelJava
  → generateJpaSchema
  → build/generated/jooq/schema.sql
  → jooqCodegen
  → compileJava
```

`jpaModel` 소스셋은 `domain` 아래 원본과 `BusinessException`만 먼저 컴파일한다. 서비스·컨트롤러·jOOQ 생성 타입을 도메인에서 참조하지 않는다. 앱과 같은 Boot BOM의 Hibernate가 PostgreSQL dialect와 snake_case 이름 정책으로 DDL을 생성한다. JDBC 메타데이터 접근·DB schema action은 비활성화한다. 전용 클래스 로더와 Hibernate registry는 성공·실패 모두 정리한다.

jOOQ `DDLDatabase`와 `JavaGenerator`가 DDL을 읽어 Java 타입을 생성한다. 명시적인 JOIN을 사용하며 암시적 관계 경로·DAO·POJO 생성은 끈다. `build/` 산출물은 Git에서 제외한다. 빌드 전용 `jpaModel` 클래스는 API JAR에 중복 포함하지 않는다.

## 엔티티·트랜잭션 계약

엔티티 클래스·프록시 대상 메서드·영속 필드에 `final`을 쓰지 않는다. JPA 생성자는 `protected`, 외부 조회 값은 setter 없이 `public` getter로 공개하고 상태 변경은 도메인 메서드로 수행한다. 상세 규칙은 [CODE.md](../convention/CODE.md)를 따른다.

JPA와 jOOQ는 Boot가 구성한 하나의 DataSource와 Spring 트랜잭션을 공유한다. 같은 트랜잭션에서 아직 flush하지 않은 JPA 변경은 jOOQ 조회에 보이지 않으므로 호출자가 먼저 flush한다. 저장소 내부에서 별도 커넥션이나 트랜잭션을 임의 생성하지 않는다.

기본 설정의 `ddl-auto=validate`는 스키마를 변경하지 않는다. 로컬 개발만 `update`, 격리 테스트만 `create-drop`을 사용한다. 코드 생성용 DDL은 운영 마이그레이션 파일이 아니다. 운영 배포 전 버전별 마이그레이션 절차를 별도로 결정한다.

## 검증과 비용

교체 가능한 `ExampleEntity`로 실제 PostgreSQL 통합 테스트를 수행한다. JPA 저장 후 jOOQ 조회, 지연 프록시의 getter·도메인 메서드와 커밋 후 변경 감지, JPA·jOOQ 쓰기의 공동 롤백을 검증한다. Testcontainers는 개발 DB와 별개 인스턴스를 사용한다.

도메인 선행 컴파일과 DDL 생성 태스크를 유지해야 한다. Boot 버전을 변경하면 빌드용 BOM도 함께 변경한다. 엔티티 경로·명명 전략·dialect 변경 시 생성 설정과 DB 통합 테스트를 함께 갱신한다. 최소 한 개의 엔티티가 필요하므로 예제 삭제는 실제 업무 모델 추가와 함께 수행한다.
