package io.github.gjaku1031.vtopia;

import static io.github.gjaku1031.vtopia.jooq.Tables.EXAMPLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gjaku1031.vtopia.example.domain.ExampleEntity;
import io.github.gjaku1031.vtopia.example.repository.ExampleQueries;
import io.github.gjaku1031.vtopia.example.repository.ExampleRepository;
import io.github.gjaku1031.vtopia.fixture.TestPostgresConfig;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import org.hibernate.Hibernate;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL에서 JPA·jOOQ 공유 트랜잭션과 지연 프록시 계약 검증
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TestPostgresConfig.class)
class PersistenceIntegrationTest {

    /**
     * 예제 모델 저장소
     */
    @Autowired
    private ExampleRepository repository;

    /**
     * 생성된 jOOQ 타입 기반 조회기
     */
    @Autowired
    private ExampleQueries queries;

    /**
     * jOOQ 쓰기의 롤백 참여 검증용 실행기
     */
    @Autowired
    private DSLContext dsl;

    /**
     * 영속성 컨텍스트와 지연 프록시 조회기
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * JPA·jOOQ 공통 트랜잭션 관리자
     */
    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * JPA 저장 후 생성된 jOOQ 타입으로 같은 트랜잭션의 행 조회
     */
    @Test
    void readsJpaWriteThroughJooq() {
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            // JPA 변경을 DB에 먼저 반영하여 jOOQ 조회에 노출
            ExampleEntity entity = repository.saveAndFlush(new ExampleEntity("첫 이름"));
            assertEquals("첫 이름", queries.findName(Objects.requireNonNull(entity.getId())));
            transaction.setRollbackOnly();
        });
    }

    /**
     * 지연 프록시의 getter·도메인 변경과 커밋 후 재조회 검증
     */
    @Test
    void updatesThroughLazyProxy() {
        long id = Objects.requireNonNull(repository.saveAndFlush(new ExampleEntity("변경 전")).getId());
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
                // getter 조회 전에는 프록시가 초기화되지 않는지 확인
                ExampleEntity reference = entityManager.getReference(ExampleEntity.class, id);
                assertFalse(Hibernate.isInitialized(reference));
                assertEquals("변경 전", reference.getName());
                assertTrue(Hibernate.isInitialized(reference));
                entityManager.clear();

                // 새 프록시에서 도메인 메서드가 초기화와 변경 감지를 수행하는지 확인
                ExampleEntity mutableReference = entityManager.getReference(ExampleEntity.class, id);
                assertFalse(Hibernate.isInitialized(mutableReference));
                mutableReference.rename("변경 후");
            });
            assertEquals("변경 후", queries.findName(id));
        } finally {
            // 커밋 검증에 사용한 행만 정리
            repository.deleteById(id);
        }
    }

    /**
     * JPA와 jOOQ의 쓰기가 하나의 Spring 트랜잭션에서 함께 롤백됨을 검증
     */
    @Test
    void rollsBackJpaAndJooqTogether() {
        WrittenIds ids = Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(transaction -> {
            // 서로 다른 영속 기술로 작성한 두 행의 식별자 보관
            long jpaId = Objects.requireNonNull(repository.saveAndFlush(new ExampleEntity("JPA 롤백")).getId());
            long jooqId = Objects.requireNonNull(dsl.insertInto(EXAMPLES)
                .set(EXAMPLES.NAME, "jOOQ 롤백")
                .returning(EXAMPLES.ID)
                .fetchOne()).getId();
            assertEquals("JPA 롤백", queries.findName(jpaId));
            assertEquals("jOOQ 롤백", queries.findName(jooqId));
            transaction.setRollbackOnly();
            return new WrittenIds(jpaId, jooqId);
        }));
        // 트랜잭션 종료 후 두 행 모두 존재하지 않아야 함
        assertNull(queries.findName(ids.jpaId()));
        assertNull(queries.findName(ids.jooqId()));
    }

    /**
     * 롤백 검증용으로 JPA·jOOQ가 각각 작성한 행의 ID
     *
     * @param jpaId JPA로 저장한 행 ID
     * @param jooqId jOOQ로 저장한 행 ID
     */
    private record WrittenIds(long jpaId, long jooqId) {
    }
}
