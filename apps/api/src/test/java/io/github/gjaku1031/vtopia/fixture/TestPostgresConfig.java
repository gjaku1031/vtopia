package io.github.gjaku1031.vtopia.fixture;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 개발 DB와 분리된 PostgreSQL 테스트 접속 정보 공급
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestPostgresConfig {

    /**
     * Spring 컨텍스트 종료 시 함께 정리되는 PostgreSQL 생성
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18.3");
    }
}
