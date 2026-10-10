package io.github.gjaku1031.vtopia.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA 쓰기·jOOQ 읽기 구성을 검증하는 예제 모델
 *
 * 업무 도메인 추가 시 교체할 템플릿이며 HTTP 쓰기 API는 제공하지 않음
 */
@Entity
@Table(name = "examples")
public class ExampleEntity {

    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * JPA 인스턴스 생성 전용 생성자
     */
    protected ExampleEntity() {
    }

    /**
     * 공백이 아닌 100자 이하 이름으로 초기화
     *
     * @throws IllegalArgumentException 이름이 비었거나 100자를 넘을 때
     */
    public ExampleEntity(String name) {
        // 초기화 중 재정의 가능한 메서드를 호출하지 않고 검증 후 직접 대입
        requireValidName(name);
        this.name = name;
    }

    /**
     * ID 조회, 저장 전에는 null
     */
    public Long getId() {
        return id;
    }

    /**
     * 이름 조회
     */
    public String getName() {
        return name;
    }

    /**
     * 공백이 아닌 100자 이하 이름으로 변경
     *
     * @throws IllegalArgumentException 이름이 비었거나 100자를 넘을 때
     */
    public void rename(String name) {
        // 변경 조건 검증 후 영속 상태 갱신
        requireValidName(name);
        this.name = name;
    }

    /**
     * 이름 길이·공백 조건 검증
     *
     * @throws IllegalArgumentException 이름이 null·공백이거나 100자를 넘을 때
     */
    private static void requireValidName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("이름은 1~100자여야 합니다.");
        }
    }
}
