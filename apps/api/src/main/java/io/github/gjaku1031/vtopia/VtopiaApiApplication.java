package io.github.gjaku1031.vtopia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vtopia API의 컴포넌트·자동 설정 진입점
 */
@SpringBootApplication
public class VtopiaApiApplication {

    /**
     * 전달받은 실행 인자로 Spring 애플리케이션 기동
     */
    public static void main(String[] args) {
        SpringApplication.run(VtopiaApiApplication.class, args);
    }
}
