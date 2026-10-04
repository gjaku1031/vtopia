# Vtopia

Next.js + Spring Boot(Kotlin) 모노레포. API의 코드·주석·영속성 구조는 [ken-blog](https://github.com/gjaku1031/ken-blog)를 기준으로 구성한다.

## 구성

```text
apps/
  api/                  Spring Boot · Kotlin · JPA · jOOQ
  web/                  Next.js App Router · React · TypeScript
docs/
  convention/           코드·주석 작성 규칙
  ADR/                  영속성 설계 결정
compose.yaml            로컬 PostgreSQL
```

| 구성 | 버전 |
| --- | --- |
| Node.js / npm | 24.21.0 / 11.19.0 |
| Next.js / React | 16.3.8 / 19.3.0 |
| TypeScript | 5.9.3 |
| JDK / Gradle Wrapper | 25 / 9.3.0 |
| Spring Boot / Kotlin | 4.1.1 / 2.3.21 |
| jOOQ / PostgreSQL | 3.21.8 / 18.3 |

JDK 25, Node.js 24, Docker Engine 및 Compose v2가 필요하다. Gradle도 JDK 25로 실행해야 한다. Windows에서는 WSL을 사용한다.

## 로컬 실행

저장소 루트에서 실행한다.

```bash
cp .env.example .env
cp apps/web/.env.example apps/web/.env.local
npm ci
npm run db:up
```

두 터미널에서 각각 실행한다.

```bash
npm run dev:api
```

```bash
npm run dev:web
```

- 웹: <http://localhost:3000>
- API 상태: <http://localhost:8080/actuator/health>
- 웹을 통한 API 상태: <http://localhost:3000/api/health>

`dev:api`는 `local` 프로필을 지정한다. 이 프로필은 `apps/api`를 기준으로 루트 `.env`를 읽고 개발 DB에 `ddl-auto=update`를 적용한다. `.env` 없이도 예제 접속 정보로 실행할 수 있다. 다른 작업 디렉터리에서 API를 실행할 때는 환경변수를 직접 전달한다.

루트 `.env`는 Docker Compose와 API 접속 정보에 사용한다. `POSTGRES_*`와 `DB_*`는 자동 동기화되지 않으므로 DB 이름·계정·포트를 바꾸면 양쪽을 함께 수정한다. PostgreSQL 계정 초기값은 빈 볼륨에만 적용된다. API 포트 변경 시 `apps/web/.env.local`의 `API_ORIGIN`도 맞춘다.

Next.js는 `/api/*`를 Spring에 전달한다. `/api/health`만 Actuator health로 연결한다. `API_ORIGIN`은 서버 설정이며 `NEXT_PUBLIC_*`로 노출하지 않는다. rewrite 대상은 빌드 시 반영되므로 운영 주소 변경 후 다시 빌드한다.

```bash
npm run db:down
```

DB를 중지해도 이름 있는 볼륨의 데이터는 보존된다.

## 검사·빌드

```bash
npm run lint
npm run typecheck
npm run test:api
npm run build
```

`test:api`는 Docker에서 별도 PostgreSQL Testcontainer를 만들고 종료 시 정리한다. 개발 DB는 사용하지 않는다. JPA 저장→jOOQ 조회, 지연 프록시 변경 감지, JPA·jOOQ 공동 롤백을 검증한다.

`build:api`는 실행 JAR을 생성하며 테스트는 별도다. API 테스트와 빌드를 함께 실행하려면 `apps/api`에서 `./gradlew clean build`를 실행한다. 웹은 위 명령으로 lint·타입 검사·프로덕션 빌드를 수행한다.

2026-10-04 기준 `npm audit --omit=dev`는 취약점 0건이다. 전체 감사에는 ESLint의 간접 의존성 `braces` 관련 high 5건이 표시된다. [공식 보안 공지](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm)에 수정 버전이 없어 Next.js 린트 설정을 유지하고 추후 업데이트 대상으로 남긴다. ESLint 9는 현재 Next.js 설정의 React 플러그인 호환 범위에 맞췄다.

```bash
npm run jooq:generate
```

jOOQ 타입은 JPA 엔티티에서 생성한다. 코드 생성과 `bootJar`에는 실행 중인 DB가 필요하지 않다. `apps/api/build/`의 DDL·생성 코드는 Git에 넣지 않는다.

API 산출물은 `apps/api/build/libs/vtopia-api.jar`, 웹 산출물은 `apps/web/.next/`에 생성된다. 웹은 `npm run start --workspace=@vtopia/web`로 실행할 수 있다.

## API 개발 기준

- 기본 패키지: `io.github.gjaku1031.vtopia`
- 기능별로 `domain`, `repository`, `service`, `controller`, `dto`를 배치한다. 사용하지 않는 레이어는 미리 만들지 않는다.
- JPA는 저장·단일 조회·변경 잠금, jOOQ는 목록·검색·집계 등 복잡한 읽기를 담당한다.
- 서비스가 Spring `@Transactional` 경계를 소유한다. 같은 트랜잭션에서 JPA 변경을 jOOQ로 읽기 전에 flush한다.
- `example` 패키지는 영속성 검증과 새 도메인 작성용 템플릿이다. 실제 업무 기능은 아니며 HTTP API로 노출하지 않는다. 첫 업무 엔티티를 추가한 뒤 예제와 통합 테스트를 함께 교체한다. 코드 생성은 최소 한 개의 엔티티를 요구한다.
- 공통 오류는 `BusinessException`과 RFC 9457 `ProblemDetail`로 처리한다. 인증·인가와 업무 API는 기능 요구사항에 맞춰 추가한다.

공통 설정은 `ddl-auto=validate`이며 DB 접속 정보는 환경변수로 전달한다. 운영 DB 스키마 변경 절차는 아직 구성하지 않았다. 운영 배포 전 별도 마이그레이션 절차를 마련하고, 개발용 `local` 프로필을 운영에 사용하지 않는다.

## 컨벤션

작업 전 [AGENTS.md](AGENTS.md), [코드 컨벤션](docs/convention/CODE.md), [주석 컨벤션](docs/convention/KDoc.md)을 확인한다. 엔티티의 명시적 `open`, `protected constructor()`, `protected set`, 한국어 여러 줄 KDoc 규칙을 적용한다.

프런트엔드는 Server Component를 기본으로 사용하고 브라우저 상태·이벤트가 필요한 경계만 Client Component로 분리한다. `@/*`는 `apps/web/src/*`를 가리킨다. `package.json`은 npm workspace와 실행 명령, `tsconfig.json`은 strict 타입 검사와 경로 별칭을 관리한다. JSON에는 주석을 삽입하지 않는다.

설계 근거는 [영속성 ADR](docs/ADR/ADR_persistence.md)에 기록한다. 프레임워크 사용 기준은 [Next.js App Router 문서](https://nextjs.org/docs/app)와 [Spring Boot SQL 문서](https://docs.spring.io/spring-boot/reference/data/sql.html)를 참고한다.
