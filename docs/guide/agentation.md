# Agentation UI 주석 도구

기준일: 2026-10-04. `agentation` 3.1.2, `agentation-mcp` 1.3.2 기준.

개발 중인 화면의 요소를 브라우저에서 클릭해 주석을 남기면 요소 경로·주변 텍스트·스타일과 함께 에이전트(Claude Code)에 전달하는 도구다. 개발 서버에서만 동작하며 운영 빌드에는 포함되지 않는다. 공식 문서: <https://www.agentation.com>

## 구성

```text
브라우저 (Next.js 개발 서버 페이지)
  └ <Agentation> 툴바
      │ 주석 추가·수정·삭제마다 HTTP 동기화, SSE로 상태 수신
      ▼
agentation-mcp HTTP 서버 (localhost:4747, ~/.agentation/store.db)
      ▲
      │ MCP stdio
Claude Code
```

- 툴바는 주석을 브라우저 로컬 저장소에 먼저 저장하고 `endpoint`로 즉시 동기화한다. 서버에 닿지 않으면 로컬에만 남고, 서버에 연결되면 동기화된다.
- `agentation-mcp server`는 MCP stdio 서버와 HTTP 서버(4747)를 함께 띄운다. 4747이 이미 사용 중이면 HTTP 서버는 생략하고 기존 서버를 사용한다.
- 주석은 SQLite(`~/.agentation/store.db`)에 저장되어 서버를 재시작해도 유지된다.

## 프로젝트 설정

이미 저장소에 반영된 설정이다.

| 파일 | 내용 |
| --- | --- |
| `apps/web/package.json` | `agentation`을 devDependency로 고정 버전 설치 |
| `apps/web/src/app/layout.tsx` | `NODE_ENV === "development"`일 때만 `<Agentation endpoint="http://localhost:4747" />` 렌더링 |
| `apps/web/next.config.ts` | `allowedDevOrigins: ["127.0.0.1"]`로 VS Code 포트 포워딩 주소의 개발 자원 요청 허용 |

`agentation`은 번들에 `"use client"`를 포함하므로 Server Component인 루트 레이아웃에서 별도 래퍼 없이 사용한다.

## 개발자별 설정

MCP 등록은 `~/.claude.json`의 프로젝트 local 범위에 저장되어 저장소에 포함되지 않는다. 각자 한 번 실행한다.

```bash
claude mcp add agentation -- npx -y agentation-mcp server
npx agentation-mcp doctor
```

`doctor`는 Node.js 버전, Claude Code 설정, 저장소, 4747 서버 상태를 점검한다. 등록 후 새로 연 Claude Code 세션부터 MCP 도구가 로드된다.

Claude Code를 띄우지 않고 브라우저 동기화만 먼저 확인하려면 서버를 직접 실행한다.

```bash
npx -y agentation-mcp server
```

## 원격 VS Code에서 사용

원격 서버에서 개발 서버를 띄우고 로컬 브라우저로 접속할 때는 두 포트를 모두 포워딩한다. VS Code 하단 **Ports** 탭에서 확인·추가한다.

| 포트 | 용도 | 확인 |
| --- | --- | --- |
| 3000 | Next.js 개발 서버 | 페이지 우측 하단에 툴바 표시 |
| 4747 | Agentation HTTP 서버 | 로컬 브라우저에서 `http://localhost:4747/health`가 `{"status":"ok"}` 응답 |

- 툴바는 로컬 브라우저에서 `http://localhost:4747`로 직접 요청한다. 4747의 Forwarded Address가 다른 포트로 바뀌면 동기화되지 않는다.
- 백그라운드로 실행한 서버는 VS Code가 자동 감지하지 못할 수 있으므로 **Add Port**로 수동 추가한다.

## 주석 처리 흐름

주석 상태는 `pending` → `acknowledged` → `resolved` 또는 `dismissed`로 바뀐다. 에이전트는 MCP 도구로 주석을 읽고 처리 결과를 스레드 답글로 남긴다.

| MCP 도구 | 역할 |
| --- | --- |
| `agentation_list_sessions` | 주석이 있는 페이지(세션) 목록 |
| `agentation_get_session` | 세션의 전체 주석 |
| `agentation_get_pending` / `agentation_get_all_pending` | 세션별·전체 미처리 주석 |
| `agentation_acknowledge` | 확인 표시 |
| `agentation_resolve` | 처리 완료 표시와 요약 답글 |
| `agentation_dismiss` | 반영하지 않는 사유와 함께 종료 |
| `agentation_reply` | 상태 변경 없이 답글 |
| `agentation_watch_annotations` | 새 주석이 들어올 때까지 대기 후 묶어서 반환 |

`agentation_watch_annotations`를 반복 호출하면 주석을 남기는 즉시 에이전트가 처리하는 핸즈프리 방식으로 사용할 수 있다.

MCP 도구가 로드되지 않은 세션에서는 같은 동작을 HTTP API로 수행할 수 있다.

```bash
curl -s localhost:4747/pending                                  # 미처리 주석
curl -s -X PATCH localhost:4747/annotations/<id> \
  -H 'Content-Type: application/json' \
  -d '{"status":"resolved","resolvedBy":"agent"}'               # 처리 완료
curl -s -X POST localhost:4747/annotations/<id>/thread \
  -H 'Content-Type: application/json' \
  -d '{"role":"agent","content":"Resolved: 요약"}'              # 답글
curl -s -X DELETE localhost:4747/annotations/<id>               # 삭제
```

## 웹훅 대신 MCP를 쓰는 이유

툴바의 `webhookUrl`도 주석 추가·수정·삭제마다 자동 전송(Auto-Send 기본 활성)하지만 다음 이유로 MCP `endpoint` 방식을 사용한다.

- 웹훅은 지정 URL로 POST만 보낸다. 에이전트에 전달하려면 별도 수신 서버가 필요하고, 처리 결과를 주석에 되돌리는 경로가 없다.
- MCP는 에이전트가 주석을 직접 조회하고 상태·답글을 갱신한다. 브라우저는 SSE로 변경을 받아 표시한다.
- 두 방식 모두 브라우저가 직접 요청하므로 원격 환경의 포트 포워딩 조건은 같다.

Slack·이슈 트래커 등 외부 연동이 필요하면 MCP 서버의 환경변수 `AGENTATION_WEBHOOK_URL`(여러 개는 `AGENTATION_WEBHOOKS`)로 서버 측 웹훅을 추가한다. 서버 측 웹훅은 실패 시 재시도한다.

## 문제 해결

| 증상 | 원인 | 조치 |
| --- | --- | --- |
| 페이지는 열리지만 툴바가 없음 | `localhost` 외 주소(예: `127.0.0.1`)에서 접속해 Next.js가 개발 자원 요청을 차단, 하이드레이션 실패. 개발 서버 로그에 `Blocked cross-origin request` 출력 | 접속 호스트를 `allowedDevOrigins`에 추가하고 개발 서버 재시작 |
| 주석을 남겨도 서버에 없음 (`/sessions`가 `[]`) | 로컬 브라우저에서 4747에 닿지 않음 | 4747 포워딩 후 `/health` 확인, 페이지 새로고침으로 로컬 주석 동기화 |
| 새 세션에 MCP 도구가 없음 | MCP 미등록 또는 등록 전 시작한 세션 | `claude mcp list` 확인, 세션 재시작 |
| `doctor`에서 서버 미기동 | Claude Code 세션이나 수동 서버가 실행되지 않음 | `npx -y agentation-mcp server` 실행 |
