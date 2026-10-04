package io.github.gjaku1031.vtopia

import io.github.gjaku1031.vtopia.example.domain.ExampleEntity
import io.github.gjaku1031.vtopia.example.repository.ExampleQueries
import io.github.gjaku1031.vtopia.example.repository.ExampleRepository
import io.github.gjaku1031.vtopia.fixture.TestPostgresConfig
import io.github.gjaku1031.vtopia.jooq.tables.references.EXAMPLES
import jakarta.persistence.EntityManager
import org.hibernate.Hibernate
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * PostgreSQL에서 JPA·jOOQ 공유 트랜잭션과 지연 프록시 계약 검증
 */
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Import(TestPostgresConfig::class)
internal class PersistenceIntegrationTest(
    /**
     * 예제 모델 저장소
     */
    @Autowired private val repository: ExampleRepository,

    /**
     * 생성된 jOOQ 타입 기반 조회기
     */
    @Autowired private val queries: ExampleQueries,

    /**
     * jOOQ 쓰기의 롤백 참여 검증용 실행기
     */
    @Autowired private val dsl: DSLContext,

    /**
     * 영속성 컨텍스트와 지연 프록시 조회기
     */
    @Autowired private val entityManager: EntityManager,

    /**
     * JPA·jOOQ 공통 트랜잭션 관리자
     */
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    /**
     * JPA 저장 후 생성된 jOOQ 타입으로 같은 트랜잭션의 행 조회
     */
    @Test
    fun readsJpaWriteThroughJooq() {
        TransactionTemplate(transactionManager).executeWithoutResult { transaction ->
            // JPA 변경을 DB에 먼저 반영하여 jOOQ 조회에 노출
            val entity = repository.saveAndFlush(ExampleEntity("첫 이름"))
            assertEquals("첫 이름", queries.findName(requireNotNull(entity.id)))
            transaction.setRollbackOnly()
        }
    }

    /**
     * 지연 프록시의 getter·도메인 변경과 커밋 후 재조회 검증
     */
    @Test
    fun updatesThroughLazyProxy() {
        val id = requireNotNull(repository.saveAndFlush(ExampleEntity("변경 전")).id)
        try {
            TransactionTemplate(transactionManager).executeWithoutResult {
                // getter 조회 전에는 프록시가 초기화되지 않는지 확인
                val reference = entityManager.getReference(ExampleEntity::class.java, id)
                assertFalse(Hibernate.isInitialized(reference))
                assertEquals("변경 전", reference.name)
                assertTrue(Hibernate.isInitialized(reference))
                entityManager.clear()

                // 새 프록시에서 도메인 메서드가 초기화와 변경 감지를 수행하는지 확인
                val mutableReference = entityManager.getReference(ExampleEntity::class.java, id)
                assertFalse(Hibernate.isInitialized(mutableReference))
                mutableReference.rename("변경 후")
            }
            assertEquals("변경 후", queries.findName(id))
        } finally {
            // 커밋 검증에 사용한 행만 정리
            repository.deleteById(id)
        }
    }

    /**
     * JPA와 jOOQ의 쓰기가 하나의 Spring 트랜잭션에서 함께 롤백됨을 검증
     */
    @Test
    fun rollsBackJpaAndJooqTogether() {
        val ids = requireNotNull(TransactionTemplate(transactionManager).execute { transaction ->
            // 서로 다른 영속 기술로 작성한 두 행의 식별자 보관
            val jpaId = requireNotNull(repository.saveAndFlush(ExampleEntity("JPA 롤백")).id)
            val jooqId = requireNotNull(dsl.insertInto(EXAMPLES)
                .set(EXAMPLES.NAME, "jOOQ 롤백")
                .returning(EXAMPLES.ID)
                .fetchOne()?.id)
            assertEquals("JPA 롤백", queries.findName(jpaId))
            assertEquals("jOOQ 롤백", queries.findName(jooqId))
            transaction.setRollbackOnly()
            jpaId to jooqId
        })
        // 트랜잭션 종료 후 두 행 모두 존재하지 않아야 함
        assertNull(queries.findName(ids.first))
        assertNull(queries.findName(ids.second))
    }
}
