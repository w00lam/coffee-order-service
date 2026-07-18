# 다중 인스턴스 system-test 증거 manifest

## 실행 식별

- 실행일: 2026-07-18 14:08~14:13 KST (`Asia/Seoul`)
- 정상 부하 원본 측정 구간: 2026-07-18 14:10:17.849~14:10:26.245 KST
- 장애 부하 원본 측정 구간: 2026-07-18 14:10:42.702~14:10:50.924 KST
- Git commit: `0260816020805ab1c2450b6379658dbb2139b908`
- 실행 전 작업 트리: `main`이 `origin/main`보다 12커밋 앞섰고, `.codex/settings.json`, `.codex/settings.local.json`, `docs/README.md`, `docs/Versions.md`만 미추적 상태였다. 네 파일은 수정·삭제·스테이징하지 않았다.
- 애플리케이션 인스턴스: Spring Boot 3개

## 실행 환경

- OS: Microsoft Windows `10.0.26200.8875`, locale `ko-KR`
- Java: Eclipse Temurin OpenJDK `21.0.11+10`
- Docker Desktop: `4.79.0 (230596)`
- Docker Engine: `29.5.3`, API `1.54`
- Docker Compose: `v5.1.4`
- PostgreSQL: `17.10` (`postgres:17.10`)
- Kafka: `3.8.0` (`apache/kafka-native:3.8.0`)
- k6: `1.6.1`

## system-test 설정

- 저장소의 `system-test/config.ps1` 기본 local port 설정과 격리된 Compose project를 사용했다.
- Outbox: poll `200ms`, batch `20`, lease `5s`, retry backoff `250ms`~`5s`
- Consumer retry: 3회, initial backoff `250ms`, multiplier 2, maximum `2s`
- 정상 부하 반복 수: charge 60, spend 80, same-token 30, conflicting-token 30, mixed-order 120
- 장애 부하: spend 120, mixed-order 120, 애플리케이션 장애 지연 3초, Kafka 의도 중단 5초
- k6 Threshold는 저장소에 있던 `functional_failures: count==0`, `unexpected_5xx: count==0`만 사용했다. 장애 실행은 기존 스크립트대로 기능 Threshold를 제외하고 `unexpected_5xx`만 판정했다.

## 실행 명령과 종료 결과

| 순서 | 명령 | 결과 |
|---:|---|---|
| 1 | `.\gradlew.bat test systemTestBootJar --no-daemon --console=plain` | exit 0, `BUILD SUCCESSFUL` |
| 2 | `powershell -ExecutionPolicy Bypass -File .\system-test\scripts\start-environment.ps1 -SkipBuild` | exit 0 |
| 3 | `docker compose -f .\system-test\compose.yml ps`와 각 인스턴스 `Invoke-WebRequest /menus` | PostgreSQL·Kafka healthy, 3개 모두 HTTP 200 |
| 4 | `powershell -ExecutionPolicy Bypass -File .\system-test\load\run-normal-load.ps1 -RunPrefix evidence-normal-20260718-1415` | exit 0, Threshold 2개 통과, 불변식 11개 통과 |
| 5 | `powershell -ExecutionPolicy Bypass -File .\system-test\fault\run-fault-injection.ps1 -RunPrefix evidence-fault-20260718-1418` | exit 0, Kafka health 복구, 불변식 11개 통과 |
| 6 | 두 prefix에 `verify-invariants.ps1` 재실행 | exit 0, PostgreSQL·Outbox·Consumer 집계 포함 |
| 7 | `powershell -ExecutionPolicy Bypass -File .\system-test\scripts\stop-environment.ps1` | exit 0, 이번 Compose project와 PID만 정리 |
| 8 | `.\gradlew.bat test --rerun-tasks --no-daemon --console=plain` | exit 0, 5 tasks executed, `BUILD SUCCESSFUL` |

## 스크린샷

각 PNG는 실제 command 종료 결과, k6 JSON, 장애 report와 PostgreSQL 불변식 출력을 일반 결과 화면으로 렌더링해 캡처했다. 터미널 외형을 합성하지 않았고 측정값을 보정하지 않았다. 별도 PowerShell window handle 직접 캡처는 실행 세션 제약으로 실패했으며, 이는 결과가 아니라 캡처 방식의 결함으로 분류했다.

| 파일 | 설명 | SHA-256 |
|---|---|---|
| `environment-ready.png` | Docker health와 Spring Boot 3개 `/menus` HTTP 200 | `64504c88e55e5f2b2e7706d0a7ee9fbb8b437697eec037a92bc090e9f4d1c4d9` |
| `normal-load-summary.png` | 요청 수, 처리량, 기능 성공률, 오류 분류, 평균·p95·p99와 Threshold | `6f760901b1e1a467ef925a7c7ca96a0de8e8cd4dcc73dc7db3c8c862008bc6b3` |
| `fault-recovery.png` | 애플리케이션 종료, Kafka 중단·복구 시각, 하네스 복구시간과 생존 인스턴스 응답 | `ad95b39ef98e7d2e5f378dfc53a82f6ef66e4d2800e8e123a85dd83c95abace0` |
| `invariants-passed.png` | 11개 불변식, Outbox 상태별 잔류와 Consumer 중복 업무 효과 | `e6ca176fe27d8f00602d234c7e564456c6ce63b1e30b518beb36a02a68fd8e9d` |

## 로컬 원본 결과

다음 파일은 재현 가능한 로컬 원본이며 `build/` 아래에 있어 Git에 추가하지 않는다.

| 파일 | 바이트 | SHA-256 |
|---|---:|---|
| `build/system-test/results/evidence-normal-20260718-1415-k6.json` | 1,851,725 | `098c61d7950cacb7d51849129b1b30d59b61c5cb57b2769f5c4dde29d97591b0` |
| `build/system-test/results/evidence-normal-20260718-1415-summary.json` | 6,033 | `bd7f050ab749915a0c6118e484f95d85889ac857458add0296d62001e57e322e` |
| `build/system-test/results/evidence-normal-20260718-1415-invariants.txt` | 22,800 | `1bced487863a9f054e3660e77605ddbf1ca9952af39e348108b4f2a4d66ee537` |
| `build/system-test/results/evidence-fault-20260718-1418-k6.json` | 1,575,950 | `f9cf1e31755d17ecedb3f49e73fca8154495916e1eecca1bba256b4b9109ab1b` |
| `build/system-test/results/evidence-fault-20260718-1418-summary.json` | 4,859 | `86c3dacf5117ced62d66b27a2632c1775460626c2b060bc795f323e984306912` |
| `build/system-test/results/evidence-fault-20260718-1418-fault-report.json` | 817 | `4ae9865a1ca27f9b58fd21a9268015abb405a0c1dcde861fb0aa0962eac65a60` |
| `build/system-test/results/evidence-fault-20260718-1418-invariants.txt` | 22,499 | `dffbc9fdbed97f61b93944a3f68e937aff4e6a923d0826ade8afa91646b1ac5e` |

## 결과 요약

### 정상 부하

- HTTP 522건, 61.94 req/s
- 평균 82.50 ms, p95 357.28 ms, p99 580.30 ms
- 기능 check 320/320 성공, 예상하지 않은 5xx 0건
- k6 `http_req_failed` 36건(6.90%)은 시나리오가 정상 업무 결과로 허용한 409 6건과 422 30건이다.
- 주문·Outbox·Consumer eventId가 각각 172건이며 11개 불변식이 모두 통과했다.

### 장애 주입

- HTTP 444건, 53.94 req/s, 평균 55.80 ms, p95 252.53 ms, p99 406.28 ms
- 종료 port 연결 실패 38건, 예상하지 않은 서버 5xx 0건
- 애플리케이션 종료: 2026-07-18 14:10:45.678 KST
- Kafka 중단 관측: 2026-07-18 14:10:46.797 KST
- Kafka health 복구: 2026-07-18 14:10:55.823 KST
- stop 요청부터 health 복구까지 측정한 하네스 시간: 9.026초
- 생존 인스턴스 두 개는 장애 후 각각 94건과 70건의 애플리케이션 응답을 반환했다.
- 주문·Outbox·Consumer eventId가 각각 132건이며 11개 불변식이 모두 통과했다.

### 자동 정합성

- 정상/장애 실행 모두 11개 불변식 통과
- 장애 복구 후 Outbox: PENDING 0, PROCESSING 0, FAILED 0, PUBLISHED 132
- Consumer: 132 rows, 132 distinct eventId, 중복 업무 효과 0
- PostgreSQL 주문 132건과 Outbox event 132건, Consumer 반영 132건 일치

## 결과로 확정한 사항

- 두 실행의 정합성 불변식은 모두 통과했다.
- 예상하지 않은 서버 5xx는 발생하지 않았다.
- 복구 후 PENDING·PROCESSING·FAILED Outbox는 남지 않았다.
- 동일 `eventId`의 Consumer 중복 업무 효과는 없었다.
- 장애 후 Kafka health, Outbox drain과 Consumer projection은 최종 복구됐다.

## 검증하지 못했거나 확정하지 않은 사항

- 단일 로컬 실행이므로 목표 처리량, p95·p99 SLO와 허용 오류율을 확정하지 않는다.
- Load Balancer가 없으므로 애플리케이션 failover RTO를 측정하지 않았다.
- 9.026초는 local container stop/start와 health 확인을 포함한 하네스 값이며 Kafka 운영 RTO가 아니다.
- Outbox lease·batch와 PostgreSQL connection pool 운영 최적값을 검증하지 않았다.
- 실행 중 Outbox 적체 추이, Consumer lag, Retry Topic·DLT 잔류, publisher별 선점 분포는 수집하지 않았다.
- 별도 콘솔 창의 직접 화면 캡처는 환경 제약으로 실패했고, 원본 결과 기반의 일반 증거 화면 캡처로 대체했다.
