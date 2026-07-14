# 주문 이벤트 전달

## Status

Accepted

## Context

- 관련 요구사항: `FR-4 — 주문 내역 실시간 전송`, `NFR-3 — 데이터 일관성`, `NFR-4 — 신뢰성`, `NFR-5 — 전송 적시성`, `BR-5 — 주문 내역 전송 데이터`, `DEP-3 — 주문 내역`, `DEP-4 — 데이터 수집 플랫폼`, `AC-4 — 주문 내역 전송`, `AC-6 — 다중 인스턴스와 동시성`.
- 주문 트랜잭션과 외부 메시지 전달은 하나의 원자적 자원으로 묶을 수 없으므로, 주문 성공을 유지하면서 이벤트 유실과 중복 효과를 통제할 경계가 필요하다.
- 현재 범위에서는 주문당 이벤트 하나만 발행한다.

## Decision

- 주문 이벤트는 Kafka로 전달하고 전달 보장은 `at-least-once`로 한다.
- 주문 트랜잭션에서 이벤트 발행 의도를 Transactional Outbox로 함께 기록하고, 애플리케이션 내부 Publisher가 이를 조회하여 Kafka에 발행한다.
- 실제 Kafka 발행 실패는 이미 완료된 주문의 성공 여부를 바꾸지 않는다.
- 여러 Publisher는 PostgreSQL `FOR UPDATE SKIP LOCKED`로 서로 다른 Outbox 이벤트를 선점하여 동일 이벤트의 동시 처리를 제어한다.
- 선점과 `PROCESSING` 상태 기록은 짧은 DB 트랜잭션에서 수행하고 커밋하여 락을 해제한 뒤 Kafka를 호출한다. Publisher는 Kafka 네트워크 호출 중 DB 락을 유지하지 않는다.
- 일시적인 발행 장애는 지수 백오프로 최대 24시간 재시도하고, 영구적 장애는 실패 상태로 분류한다.
- Producer 측 최종 실패와 Consumer 측 DLT는 별도의 실패 경계로 관리한다.
- Consumer는 `(consumerName, eventId)` 처리 이력을 사용하여 중복 효과를 방지한다. 업무 반영과 처리 이력 기록은 같은 트랜잭션에서 수행하고, DB 반영 후 Offset을 확정한다.
- 이벤트 하나는 주문 하나를 나타내며 복수 주문 항목을 포함한다. 각 항목은 최소한 메뉴 ID와 수량을 표현한다.
- Kafka Key는 `orderId`로 설정하지만 현재 범위에서는 주문당 이벤트 하나만 발행한다.
- 사용자 단위 Kafka 이벤트 순서는 보장하지 않는다.
- Kafka 파티션 초기값은 6개로 한다.
- 정상 상황에서 주문 완료 후 Kafka 발행까지의 목표는 3초 이내다.

이 방향은 주문 성공을 외부 브로커 가용성과 분리하면서 발행 의도를 보존하고 다중 Publisher 처리량을 확보한다. 대신 중복 전달을 허용하므로 Consumer 멱등성, Outbox 상태 복구와 운영 모니터링이 필요하다.

## Options Considered

- Option: Kafka와 Transactional Outbox를 결합한 `at-least-once` 전달.
- Rationale: 로컬 주문 데이터와 발행 의도를 함께 확정하고 브로커 장애 시 나중에 재시도할 수 있다.
- Trade-offs: Outbox 저장·정리, Publisher 선점, 중복 소비 방지가 추가된다.
- Decision: Accepted.

- Option: PostgreSQL `FOR UPDATE SKIP LOCKED`로 짧게 선점하는 다중 Publisher.
- Rationale: 잠긴 이벤트를 기다리지 않고 다른 이벤트를 가져갈 수 있어 여러 Publisher가 DB를 공통 조정 지점으로 사용하면서 작업을 분담할 수 있다.
- Trade-offs: 정합성은 DB 선점으로 확보하고 다중 서버 처리량도 높일 수 있지만, 선점 임대 만료와 발행 성공 후 상태 기록 실패 복구가 필요해 구현 복잡도가 증가한다. Kafka 호출 전 트랜잭션을 커밋하므로 네트워크 지연이 DB 락 시간을 늘리지는 않는다.
- Decision: Accepted.

| Publisher 선점 전략 | 정합성 | 성능 | 구현 복잡도 | 다중 서버 적합성 | 결정 |
| --- | --- | --- | --- | --- | --- |
| `FOR UPDATE SKIP LOCKED` | 동일 행의 동시 선점을 방지 | 잠긴 행을 건너뛰어 병렬 처리에 유리 | 보통 | 높음 | Accepted |
| 후보 조회 후 조건부 원자적 UPDATE | 한 Publisher만 상태 변경에 성공 | 같은 후보 경합 시 실패 갱신 증가 | 보통 | 높음 | 검토됨, Rejected 아님 |
| Redis `SET NX PX` 또는 Redisson 락 | Redis 락과 DB 상태가 분리됨 | 전체 Publisher 직렬화 가능성 | 높음 | 높음 | 검토됨, Rejected 아님 |
| 애플리케이션 로컬 락 | 다른 인스턴스의 처리를 막지 못함 | 인스턴스 내부에서는 좋음 | 낮음 | 낮음 | 검토됨, Rejected 아님 |

- Option: `orderId`를 Kafka Key로 사용하고 주문당 이벤트 하나만 발행한다.
- Rationale: 현재 이벤트 단위와 식별 기준을 주문에 맞추며 향후 같은 주문의 추가 이벤트가 생길 경우에도 주문 단위 파티셔닝을 유지할 수 있다.
- Trade-offs: 사용자 전체 주문의 순서는 제공하지 않으며, 현재 주문당 이벤트가 하나라 순서 보장의 실질적 효과는 제한적이다.
- Decision: Accepted.

## Proposed

- 최종 발행 실패를 운영자에게 알리고 수동 재처리할 수 있는 운영 절차를 둔다.
- 이벤트에 스키마 버전과 발생 시각 같은 운영 메타데이터를 포함한다.
- 장기간 실패 이벤트를 조회하고 재발행할 수 있는 운영 기능을 둔다.

## Rejected Alternatives

- Alternative: 사용자 식별값을 Kafka Key로 사용하여 사용자 단위 이벤트 순서를 보장한다.
- Reason: 현재 범위에서는 사용자 단위 Kafka 이벤트 순서를 보장하지 않고 `orderId`를 Key로 사용하기로 명시적으로 확정했다.

## Downstream Impact

- Domain: 주문 완료와 이벤트 발행 상태를 분리하고, 이벤트 식별자와 주문 이벤트 항목의 의미를 정의해야 한다.
- Specification: 주문 이벤트의 필수 필드, 이벤트 ID, 주문 ID, 복수 항목, 총금액과 전달 의미를 명세해야 한다.
- Issue: Outbox 기록, 다중 Publisher, 재시도, Consumer 멱등성, 실패 격리와 운영 복구를 별도 추적 작업으로 분해해야 한다.
- Implementation: `FOR UPDATE SKIP LOCKED`로 선점과 상태 기록을 짧게 완료하고 트랜잭션 커밋 후 Kafka를 호출해야 한다. Consumer 반영과 처리 이력은 원자적으로 저장하되 선점 SQL이나 클래스 구조는 이 문서에서 정하지 않는다.
- Test: 주문 커밋 후 발행, Kafka 장애 후 재시도, 다중 Publisher 경쟁, 중복 이벤트, Consumer 재시작, 발행 성공 후 로컬 상태 갱신 실패를 검증해야 한다.
- Pull Request / Review: 주문 성공과 이벤트 전달의 결합 여부, 장시간 DB 잠금, 중복 효과와 복구 누락을 중점 검토해야 한다.

## Assumptions

- 추가로 채택한 미검증 가정은 없다.

## Open Decisions

- `FOR UPDATE SKIP LOCKED`를 사용하는 선점 SQL의 구체적인 조회 조건, 정렬, 배치 크기와 상태 전이.
- 선점 임대시간과 임대 연장 여부.
- Kafka 발행은 성공했지만 Outbox 발행 완료 상태 갱신에 실패한 경우의 복구 방식.
- 이벤트 직렬화 형식과 스키마 버전 관리 방식.
- Consumer 재시도 횟수, 간격과 Retry Topic 사용 여부.
- DLT 보관기간, 재처리 조건과 재처리 주체.
- Outbox 데이터 보관·정리 기간.
- 데이터 수집 플랫폼의 실제 인터페이스와 장애 처리 계약.
- Kafka Broker 복제 수, Producer ACK와 Topic 운영 설정.
