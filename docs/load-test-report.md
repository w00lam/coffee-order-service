# 다중 인스턴스 부하·장애 검증 결과

## 실행 환경

- 2026-07-18, Windows 로컬 환경
- Java 21, Spring Boot 4.1.0, Docker Compose
- PostgreSQL 17.10 1개, Kafka 3.8.0 1개, Spring Boot 프로세스 3개
- HTTP ports: 18081, 18082, 18083 (실행 설정으로 변경 가능)
- 결과 파일: `build/load-test/results/` (대용량·실행별 산출물은 Git 미추적)

## 기준선 실행

정상 부하를 `ChargeIterations=25`, `SpendIterations=30`, `IdempotentIterations=20`, `ConflictIterations=20`, `MixedIterations=30`으로 실행했다.

- 총 HTTP 요청: 187
- 처리량: 22.97 req/s
- 평균 응답시간: 93.32 ms
- p95: 320.2 ms
- p99: 이번 k6 summary에는 p99가 출력되지 않아 미측정
- 예상하지 않은 5xx: 0건 (0%)
- 자동 정합성 검사: 11개 불변식 모두 통과
- Outbox 잔류: 0건, FAILED: 0건
- 동일 eventId 중복 반영: 0건

## 장애 주입

`run-fault-injection.ps1`는 부하 중 첫 번째 애플리케이션 프로세스를 종료하고 Kafka를 설정된 시간 동안 중지한 뒤 복구한다. 장애 시각과 복구 시각은 실행별 JSON 보고서에 기록한다. 실제 장애 실행은 환경별 Docker 상태와 실행 시간이 달라질 수 있으므로, 보고서에 생성된 `*-fault-report.json`과 k6 summary를 근거로 갱신한다.

이번 실행(`fault-20260718042625-3164`)에서는 18081 인스턴스를 종료하고 Kafka를 복구했다. k6는 242 HTTP 요청, 29.04 req/s, 평균 96.04 ms, p95 379.23 ms를 기록했고 예상하지 않은 5xx는 0건이었다. 애플리케이션 종료로 인한 연결 실패와 업무 오류 10건은 장애 주입 시나리오의 예상 결과로 분류했다. Kafka 복구까지 측정된 시간은 7.018초였고, 11개 정합성 불변식은 모두 통과했다.

## 해석과 위험

이번 결과는 로컬 재현 기준선이며 성능 합격 목표가 아니다. PostgreSQL·Kafka 단일 노드, 로컬 네트워크, 제한된 부하량으로 인해 운영 환경의 병목을 대표하지 않는다. 다음 단계는 장애 실행을 반복해 복구시간 분포와 Outbox backoff/consumer lag를 수집하고, PostgreSQL connection 사용량을 별도 관측하는 것이다.
