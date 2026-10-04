package io.github.gjaku1031.vtopia.example.repository

import io.github.gjaku1031.vtopia.jooq.tables.references.EXAMPLES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 생성된 jOOQ 타입을 사용하는 예제 조회
 */
@Repository
internal class ExampleQueries(
    /**
     * JPA와 DataSource·Spring 트랜잭션을 공유하는 SQL 실행기
     */
    private val dsl: DSLContext,
) {
    /**
     * ID에 해당하는 이름 조회, 행이 없으면 null 반환
     *
     * 같은 트랜잭션의 JPA 변경을 읽을 때는 호출자가 먼저 flush 수행
     */
    fun findName(id: Long): String? = dsl
        .select(EXAMPLES.NAME)
        .from(EXAMPLES)
        .where(EXAMPLES.ID.eq(id))
        .fetchOne(EXAMPLES.NAME)
}
