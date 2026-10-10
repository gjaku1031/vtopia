package io.github.gjaku1031.vtopia.example.repository;

import static io.github.gjaku1031.vtopia.jooq.Tables.EXAMPLES;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/**
 * 생성된 jOOQ 타입을 사용하는 예제 조회
 */
@Repository
public class ExampleQueries {

    /**
     * JPA와 DataSource·Spring 트랜잭션을 공유하는 SQL 실행기
     */
    private final DSLContext dsl;

    /**
     * SQL 실행기 주입
     */
    public ExampleQueries(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * ID에 해당하는 이름 조회, 행이 없으면 null 반환
     *
     * 같은 트랜잭션의 JPA 변경을 읽을 때는 호출자가 먼저 flush 수행
     */
    public @Nullable String findName(long id) {
        return dsl
            .select(EXAMPLES.NAME)
            .from(EXAMPLES)
            .where(EXAMPLES.ID.eq(id))
            .fetchOne(EXAMPLES.NAME);
    }
}
