# 다중 인스턴스 시스템 검증 결과

## 실행 환경

- 2026-07-18, Windows 로컬 환경
- Java 21, Spring Boot 4.1.0, Docker Compose
- PostgreSQL 17.10 1개, Kafka 3.8.0 1개, Spring Boot 프로세스 3개
- HTTP ports: 18081, 18082, 18083 (실행 설정으로 변경 가능)
- 결과 파일: `build/system-test/results/` (대용량·실행별 산출물은 Git 미추적)
- Outbox 설정: poll 200 ms, batch 20, lease 5초, retry backoff 250 ms~5초
- Kafka Consumer retry: 3회, 250 ms 시작, 2배 증가, 최대 2초

## 기준선 실행

정상 부하를 `ChargeIterations=25`, `SpendIterations=30`, `IdempotentIterations=20`, `ConflictIterations=20`, `MixedIterations=30`으로 실행했다. 이 수치는 재현 가능한 로컬 기준선이며 성능 목표가 아니다.

- 총 HTTP 요청: 187
- 처리량: 23.02 req/s
- 평균 응답시간: 103.17 ms
- p95: 412.53 ms
- p99: 531.67 ms
- 예상하지 않은 5xx: 0건 (0%)
- 기능 실패: 0건
- 자동 정합성 검사: 11개 불변식 모두 통과
- 완료 주문과 Outbox event: 각각 62건
- Outbox 잔류: 0건, FAILED: 0건
- Consumer 반영: 62건, distinct eventId 62건

## 장애 주입

`fault/run-fault-injection.ps1`로 부하 중 애플리케이션 한 인스턴스를 종료하고 Kafka를 3초간 중지한 뒤 health 확인까지 복구했다.

- 총 HTTP 요청: 207
- 처리량: 25.33 req/s
- 평균 응답시간: 53.75 ms
- p95: 230.49 ms
- p99: 327.42 ms
- 종료된 인스턴스로 향한 전송 실패: 35건 (16.91%)
- 예상하지 않은 서버 5xx: 0건
- Kafka stop 시작부터 컨테이너 health 복구까지: 7.023초
- 완료 주문과 Consumer 반영: 각각 85건, distinct eventId 85건
- 자동 정합성 검사: 11개 불변식 모두 통과
- 복구 후 PENDING·PROCESSING·FAILED Outbox: 0건

전송 실패 35건은 별도 Load Balancer 없이 k6가 종료된 port까지 순환한 결과다. 살아 있는 두 인스턴스는 계속 처리했지만, 이 수치만으로 서비스 수준의 장애 오류율이나 failover 시간을 판정할 수는 없다.

## 결과로 확정한 기준

- 이 검증 묶음은 부하 측정만이 아니라 다중 인스턴스, 장애 복구와 정합성을 함께 다루므로 `system-test`로 분류한다.
- 정상 실행의 합격 조건은 기능 실패와 예상하지 않은 5xx가 없고, 11개 데이터 정합성 불변식이 모두 통과하는 것이다.
- 장애 실행의 합격 조건은 살아 있는 인스턴스가 요청을 계속 처리하고, 복구 후 주문·포인트·Outbox·Consumer projection 불변식이 모두 통과하는 것이다.
- 처리량과 응답시간은 현재 환경의 비교 기준선으로만 기록한다. 이번 단일 실행값을 장기 Threshold나 SLO로 채택하지 않는다.
- 장애 시 종료된 port로 직접 보낸 연결 실패는 서버 5xx와 분리한다. 서비스 가용성 목표는 Load Balancer와 재시도 정책을 포함한 환경에서 별도로 결정한다.
- 측정된 7.023초는 Kafka 컨테이너 stop/start와 health 확인을 포함한 하네스 복구시간이다. 운영 Kafka RTO로 사용하지 않는다.

## 아직 확정하지 않은 값

- 정상·피크·버스트 구간별 목표 처리량과 최소 성공률
- p95·p99 응답시간 Threshold와 허용 오류율
- 애플리케이션 failover 및 Kafka 복구 RTO
- Outbox lease 5초와 retry backoff 설정의 운영 적정성
- PostgreSQL connection pool 사용량과 허용 포화도
- Outbox 최대 적체량·최장 대기시간과 Consumer lag 목표
- Retry Topic·DLT 잔류 허용 기준

이 값들은 고정된 하드웨어 사양에서 단계별 부하를 각각 여러 번 반복하고, 중앙값과 변동 폭, 예상 사용자 규모와 요구 여유 용량을 함께 비교한 뒤 결정한다.

## 해석과 위험

이번 결과는 PostgreSQL·Kafka 단일 노드와 로컬 네트워크에서 얻은 기준선이다. Outbox 최종 상태와 projection 정합성은 확인했지만, Publisher별 선점 분포, 실행 중 Outbox 적체 변화, PostgreSQL connection 사용량, Consumer lag, Retry Topic·DLT 잔류는 아직 자동 측정하지 않는다. 애플리케이션 종료 시점도 Outbox 선점 직후로 결정적으로 고정하지 않았으므로 lease 회수 시간의 정량 증거로 사용할 수 없다.

다음 우선순위는 관측 지표를 추가한 뒤 낮음·중간·높음 부하를 반복 측정하고, Load Balancer를 포함한 장애 전환과 Outbox lease 회수를 별도 시나리오로 검증하는 것이다.
