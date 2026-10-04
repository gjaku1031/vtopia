package io.github.gjaku1031.vtopia.example.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * JPA 쓰기·jOOQ 읽기 구성을 검증하는 예제 모델
 *
 * 업무 도메인 추가 시 교체할 템플릿이며 HTTP 쓰기 API는 제공하지 않음
 * 인자 없는 생성자는 JPA 인스턴스 생성에만 사용
 */
@Entity
@Table(name = "examples")
open class ExampleEntity protected constructor() {
    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    /**
     * 이름
     */
    @Column(nullable = false, length = 100)
    open lateinit var name: String
        protected set

    /**
     * 공백이 아닌 100자 이하 이름으로 초기화
     */
    internal constructor(name: String) : this() {
        // 초기화 중 재정의 가능한 메서드를 호출하지 않고 검증 후 직접 대입
        require(name.isNotBlank() && name.length <= 100) { "이름은 1~100자여야 합니다." }
        this.name = name
    }

    /**
     * 공백이 아닌 100자 이하 이름으로 변경
     */
    internal open fun rename(name: String) {
        // 변경 조건 검증 후 영속 상태 갱신
        require(name.isNotBlank() && name.length <= 100) { "이름은 1~100자여야 합니다." }
        this.name = name
    }
}
