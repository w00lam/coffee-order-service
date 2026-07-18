# 다중 인스턴스 시스템 검증

이 디렉터리는 로컬 Docker PostgreSQL·Kafka와 Spring Boot 애플리케이션 3개를 같은 데이터 토폴로지로 실행한다. 애플리케이션 프로세스는 `systemTestBootJar`로 만들며 `X-System-Test-User` 헤더를 인증 사용자로 사용한다. 운영 코드와 API 계약은 변경하지 않는다.

검증 성격에 따라 파일을 나눈다.

- `load`: 동시 요청과 기준 성능 측정
- `fault`: 애플리케이션·Kafka 장애 주입과 복구
- `invariants`: PostgreSQL 원본·Outbox·인기 메뉴 projection 정합성 검사
- `observability`: 실행 중 애플리케이션·PostgreSQL 원본 수집
- `reporting`: 실행별 manifest와 반복 실행 index 생성
- `scripts`: 공통 환경 시작·종료

## 실행

PowerShell 실행 정책이 제한된 환경에서는 아래처럼 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\system-test\scripts\start-environment.ps1
powershell -ExecutionPolicy Bypass -File .\system-test\load\run-normal-load.ps1
powershell -ExecutionPolicy Bypass -File .\system-test\fault\run-fault-injection.ps1
powershell -ExecutionPolicy Bypass -File .\system-test\scripts\stop-environment.ps1
```

`APP_PORTS`, `POSTGRES_PORT`, `KAFKA_PORT`, `OUTBOX_*`, `ORDER_EVENTS_TOPIC`, `POPULAR_MENU_GROUP` 환경변수로 실행별 값을 바꿀 수 있다. 기존 사용자 컨테이너를 건드리지 않도록 Compose project 이름과 포트를 별도로 지정한다.

Actuator와 Prometheus endpoint는 공통 설정이 아니라 `observability` profile에 있으며, system-test 시작 스크립트가 `system-test,observability` profile을 명시적으로 활성화한다.

성능 반복 측정은 baseline과 optimized를 서로 다른 빈 DB 환경에서 실행한다. 시작 스크립트는 실제 적용 설정을 `build/system-test/effective-config.json`에 기록하고, 반복 실행 스크립트는 phase와 poll interval이 일치하지 않으면 실행을 중단한다. 기존 `functional_failures`, `unexpected_5xx` Threshold만 사용하며 새 SLO를 만들지 않는다.

```powershell
$env:OUTBOX_POLL_INTERVAL = '200ms'
powershell -ExecutionPolicy Bypass -File .\system-test\scripts\start-environment.ps1
powershell -ExecutionPolicy Bypass -File .\system-test\load\run-performance-suite.ps1 -Phase baseline -Profile all -Repetitions 5
powershell -ExecutionPolicy Bypass -File .\system-test\scripts\stop-environment.ps1

$env:OUTBOX_POLL_INTERVAL = '20ms'
powershell -ExecutionPolicy Bypass -File .\system-test\scripts\start-environment.ps1
powershell -ExecutionPolicy Bypass -File .\system-test\load\run-performance-suite.ps1 -Phase optimized -Profile all -Repetitions 5 -RunSet validated
powershell -ExecutionPolicy Bypass -File .\system-test\scripts\stop-environment.ps1
Remove-Item Env:OUTBOX_POLL_INTERVAL
```

각 실행은 `build/system-test/results/<run-prefix>-observability`에 Prometheus 원본, PostgreSQL query 통계와 slow query 로그, Hikari/JVM/HTTP/Kafka metric, Outbox 시계열과 event 지연, container 통계 및 SHA-256 manifest를 남긴다. `verify-invariants.ps1`는 Outbox와 Consumer projection이 모두 drain된 뒤 11개 불변식을 검증한다.

`load/run-performance-suite.ps1`는 단일 실행 진입점인 `load/run-normal-load.ps1`만 반복 호출한다. 부하 프로필은 `load/profiles.ps1`, 실행별 hash는 `reporting/write-run-manifest.ps1`, phase별 HTTP index는 `reporting/summarize-performance.ps1`가 담당한다.

정상 부하와 장애 주입 스크립트는 실행마다 고유 prefix를 생성하고, 완료 후 `verify-invariants.ps1`가 PostgreSQL 원본·Outbox·인기 메뉴 projection을 함께 검사한다. k6 원본 JSON·summary, 불변식 원본 출력과 장애 시각 기록은 `build/system-test/results`에 생성되며 Git에는 추적하지 않는다.

## 시나리오

- `load/run-normal-load.ps1`: 포인트 충전/차감, 동일 토큰 멱등성, 토큰 충돌, 여러 인스턴스 주문을 동시에 실행한다.
- `fault/run-fault-injection.ps1`: 부하 중 애플리케이션 한 인스턴스를 종료하고 Kafka를 중지·복구한 뒤 Outbox drain과 projection 정합성을 검사한다.
- `invariants/verify-invariants.ps1`: 잔액 방정식, 토큰·Outbox 1:1, 주문 금액, eventId 중복 방지, projection 일치, FAILED/잔류 Outbox를 자동 검사한다.

성능 수치는 합격 기준이 아니라 동일한 로컬 환경에서 비교할 기준선이다. 결과 보고서에는 처리량, p95/p99, 오류율과 장애 복구 시각을 함께 기록한다.
