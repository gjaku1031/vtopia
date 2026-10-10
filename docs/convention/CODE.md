# 코드 컨벤션

상태: 적용. 작성일: 2026-10-04. 개정일: 2026-10-07(Kotlin → Java 전환). ken-blog 컨벤션을 기준으로 적용.

Java 코드의 접근 범위·상속·JPA 엔티티·null 처리·표현식·API 사용에 적용하는 기준. 주석의 형식과 간격은 [Javadoc.md](Javadoc.md), 영속 모델의 설계 근거는 [ADR_persistence.md](../ADR/ADR_persistence.md) 참조.

## 언어와 빌드

- API 애플리케이션·테스트는 Java로 작성. Kotlin 소스·플러그인·`kotlin-*` 의존성을 추가하지 않음.
- Gradle 빌드 스크립트는 Groovy DSL(`build.gradle`, `settings.gradle`) 사용.
- JDK 25 언어 기능 사용 가능. 값 묶음은 `record`, 타입 분기는 패턴 매칭 `switch`·`instanceof` 우선.
- Lombok 등 바이트코드 생성 도구는 별도 결정 없이 도입하지 않음.

## 접근 범위

- 선언의 접근 범위는 실제 호출자에게 필요한 수준으로 제한.
- 클래스 내부에서만 사용하는 상태·도우미는 `private`, 같은 패키지에서만 쓰는 계약은 package-private 사용. 다른 패키지에서 호출해야 할 때만 `public`.
- 인스턴스 상태에 의존하지 않는 `private` 도우미는 `static`으로 선언.
- 상속 계약이 없는 일반 클래스는 `final` 선언 가능. 프레임워크 프록시가 필요한 클래스(`@Entity`, `@Configuration`, AOP 대상)에는 `final` 사용 금지.
- `protected`는 하위 클래스에 공개할 필요가 있는 멤버에만 사용. `final` 클래스에서 `protected` 사용 금지.
- 의존성·불변 상태 필드는 `private final`로 선언하고 생성자에서 주입. 외부 조회만 허용하면 getter만 공개하고 setter를 만들지 않음.

## JPA 엔티티와 프록시

### 상속 가능 범위

- `@Entity`·`@MappedSuperclass` 클래스와 프록시가 호출하는 `public`·`protected`·package-private 메서드에 `final` 사용 금지.
- 영속 필드에도 `final` 사용 금지. 필드는 `private`로 두고 매핑 어노테이션은 필드에 둠(필드 접근).
- 프록시 대상이 아닌 복합 키 `@Embeddable`과 DTO·enum은 `final` 또는 `record` 사용 가능. 엔티티에 `record` 사용 금지.

상속 가능 여부는 [Hibernate 프록시 요구 조건](https://docs.hibernate.org/orm/7.4/userguide/html_single/#entity-pojo-final)에 근거.

### 생성자와 상태 변경

- JPA 인스턴스 생성용 인자 없는 생성자는 `protected` 유지. 경고 제거 목적으로 `private`로 변경하지 않음.
- 업무용 생성자와 팩터리는 호출 범위에 맞게 별도 제공. 빈 생성자로 만든 불완전한 인스턴스를 일반 호출자에게 노출하지 않음.
- 외부 조회가 필요한 엔티티 값은 `public` getter로 공개. `public` setter를 만들지 않음.
- 상태 변경은 검증·수정 시각 등의 계약을 가진 도메인 메서드로 수행.
- 생성자에서 재정의 가능한 메서드를 호출하지 않음. 생성자·도메인 메서드가 공유하는 검증은 `private static` 도우미로 분리.

```java
/**
 * 첨부 파일의 식별 정보
 */
@Entity
@Table(name = "attachments")
public class AttachmentEntity {

    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JPA 인스턴스 생성 전용 생성자
     */
    protected AttachmentEntity() {
    }

    /**
     * ID 조회, 저장 전에는 null
     */
    public Long getId() {
        return id;
    }
}
```

### 프록시의 값 사용

- 프록시 엔티티의 필드를 다른 인스턴스에서 직접 읽지 않음. 다른 엔티티의 값은 getter로 조회.
- `equals`·`hashCode`를 구현하면 `Hibernate.getClass` 등 프록시를 고려한 비교 사용.

## null 처리와 JDBC 조회

- null이 허용되는 반환값·인자에는 JSpecify `@Nullable`(`org.jspecify.annotations.Nullable`) 표시. 표시가 없으면 non-null 계약.
- `Optional`은 반환 타입에만 사용. 필드·인자에 사용하지 않음.
- 타입·앞선 조건으로 non-null이 보장된 값에 불필요한 null 검사를 추가하지 않음. 반드시 존재해야 하는 값은 `Objects.requireNonNull`로 계약 위반을 드러냄.
- `queryForObject`는 정확히 한 행을 요구. 0행이면 `null` 대신 `EmptyResultDataAccessException` 발생. `RowMapper`의 null 반환과 행 누락을 구분.
- 0행을 정상적인 부재로 처리해야 하면 해당 호출에서 `EmptyResultDataAccessException`만 처리하거나 행 개수를 명시적으로 다루는 조회 사용.
- 반드시 존재해야 하는 상태 행의 누락은 시스템 오류로 전파. DB 연결 장애·잘못된 SQL·중복 행까지 `Exception`으로 묶어 `false`나 빈 값으로 숨기지 않음.

## 람다와 지역 변수

- 지역 변수 `var`는 오른쪽 식에서 타입이 분명할 때만 사용.
- 한 줄로 끝나는 람다에는 블록 `{}`를 쓰지 않음. 메서드 참조가 더 읽기 쉬우면 메서드 참조 사용.
- 패턴 변수(`instanceof Type name`, `case Type name`)는 해당 분기 안에서만 사용되도록 범위 유지.

## 문자열과 라이브러리 API

- 여러 줄 SQL·JSON 등 긴 리터럴은 텍스트 블록(`"""`) 사용.
- 사용 중인 라이브러리의 deprecated API는 공식 대체 API와 동작을 확인한 뒤 교체. 경고를 없애기 위해 의존성을 임의로 올리거나 검증 강도를 낮추지 않음.
- Jackson 3 문자열 노드는 `isString`, `stringValue()`, 명시적으로 변환이 필요한 곳은 `asString()` 사용. 이전 `isTextual`, `textValue()`, `asText()` 별칭은 새 코드에서 사용하지 않음.
- JSON 입력 검증은 문자열 타입 확인 후 값을 읽는 계약 유지. `stringValue()`를 강제 문자열 변환인 `asString()`으로 바꾸어 숫자·불리언을 허용하지 않음.

## 경고 처리와 검증

- IDE 경고를 모두 같은 종류로 취급하지 않음. 컴파일 진단, 동작 결함 가능성, 스타일 제안을 진단 코드와 실제 사용처로 구분.
- 도달 불가능한 분기·효과 없는 방어 코드·폐기 예정 API는 원인을 확인해 정리.
- 경고의 선언과 사용처를 확인하여 접근 범위·상속·프레임워크 설정 중 원인을 먼저 수정. IDE 제안을 그대로 적용하기 전에 프레임워크 계약과 동작 보존 확인.
- 경고를 일괄 `@SuppressWarnings` 처리하거나 IDE 검사를 끄는 방식으로 해결하지 않음.
- 빌드 설정 변경 후 Gradle 프로젝트를 다시 불러와 IDE 정보 갱신. 컴파일 경고 0건과 에디터 진단 0건은 구분하여 보고. 에디터 검사를 실행하지 않았다면 실행한 것으로 보고하지 않음.
- 엔티티 변경 시 앱·`jpaModel` 컴파일 결과에서 클래스와 프록시 대상 메서드의 `final` 여부 확인.
- 접근 제어만 변경한 경우 생성 DDL의 동일성 확인. 테이블·열·FK·인덱스 변경이 섞이지 않도록 검증.
- 프록시를 통한 실제 지연 조회·도메인 메서드 호출·변경 감지·재조회는 격리 DB 통합 검사로 확인. 컴파일 성공만으로 영속 동작을 검증했다고 보고하지 않음.
