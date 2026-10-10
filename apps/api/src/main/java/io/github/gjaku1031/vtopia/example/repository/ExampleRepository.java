package io.github.gjaku1031.vtopia.example.repository;

import io.github.gjaku1031.vtopia.example.domain.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 예제 모델의 저장·단일 조회
 */
public interface ExampleRepository extends JpaRepository<ExampleEntity, Long> {
}
