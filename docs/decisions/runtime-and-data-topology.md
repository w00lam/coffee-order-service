# 실행 환경과 데이터 토폴로지

## Status

Accepted

## Context

- 관련 요구사항: `NFR-1 — 확장성`, `NFR-2 — 동시성`, `NFR-3 — 데이터 일관성`, `NFR-4 — 신뢰성`, `NFR-7 — 구현 재량 및 제약`, `DEP-1 — 메뉴 정보`, `DEP-2 — 사용자 식별값 및 포인트 정보`, `DEP-3 — 주문 내역`, `DEP-4 — 데이터 수집 플랫폼`, `DEP-5 — 인증된 사용자 정보`, `AC-6 — 다중 인스턴스와 동시성`.
- 서비스는 다수 서버와 다수 인스턴스 환경에서 주문, 포인트, 이벤트와 인기 메뉴 조회를 일관되게 처리해야 한다.
- 이 문서는 구성요소의 책임을 정의하며 실제 배포 크기나 상세 네트워크 구성은 확정하지 않는다.
- PostgreSQL, Kafka와 Redis는 단순 사용 여부가 아니라 각 기술이 해결하는 문제와 검증 가능한 효과를 제시해야 한다.

## Decision

- PostgreSQL을 주문, 주문 항목, 포인트, 이벤트 발행 의도와 인기 메뉴 집계의 영속 저장소로 사용한다.
- Kafka를 주문 이벤트 전달 브로커로 사용한다.
- Redis를 인기 메뉴 조회 캐시와 캐시 갱신 조정에 사용한다.
- 주문·포인트·인기 메뉴 집계의 기준 데이터는 PostgreSQL에 두고 Redis를 기준 데이터로 사용하지 않는다.
- PostgreSQL의 원자적 트랜잭션으로 주문·포인트·Outbox 기록의 일관성을 보장하고, `FOR UPDATE SKIP LOCKED`로 다중 Outbox Publisher가 잠긴 작업을 기다리지 않고 서로 다른 이벤트를 병렬 선점하도록 구현한다.
- 미발행 Outbox에는 Partial Index를 적용하고, Consumer 처리 이력에는 `(consumer_name, event_id)` Unique Constraint와 `ON CONFLICT DO NOTHING`을 적용한다.
- PostgreSQL 고유 기능의 효과는 일반적인 CRUD 사용과 구분하여 동시 Publisher 경합 테스트와 실행 계획 등 재현 가능한 증거로 제시한다.
- 애플리케이션은 여러 서버의 여러 인스턴스로 실행할 수 있게 구성한다.
- 애플리케이션 인스턴스는 로컬 상태에 의존하지 않고 공유 데이터 저장소와 메시징 인프라를 사용한다.
- 성능 검증의 업무 배경은 다수 매장을 가진 프랜차이즈 서비스로 둔다.
- Redis 고가용성 필수 범위는 Primary/Replica와 Sentinel의 장애 감지·자동 Failover 및 클라이언트 재연결 흐름을 설계·학습하고, 로컬에서는 Redis 중단 시 PostgreSQL 집계 대체 조회가 동작하는지 실제 검증하는 데까지로 한다.
- Sentinel이 Replica를 새 Primary로 승격하고 애플리케이션이 재연결하는 실제 Failover 검증은 필수 범위에서 제외하고 시간 여유가 있을 때 선택적으로 수행한다.

PostgreSQL, Kafka, Redis는 사용자가 학습한 기술을 활용하면서 각각 원자적 영속성, 비동기 전달, 빠른 조회와 분산 조정이라는 서로 다른 책임을 맡는다. 구성요소가 늘어 운영 복잡성과 장애 지점이 증가하지만, 기준 데이터와 파생 데이터를 구분하고 다중 인스턴스 확장을 검증할 수 있다.

## Options Considered

- Option: PostgreSQL을 기준 데이터 저장소, Kafka를 이벤트 전달, Redis를 파생 조회 캐시로 사용한다.
- Rationale: 트랜잭션 데이터, 비동기 전달과 고빈도 조회의 책임을 분리하면서 각 기술의 학습 목표를 실제 요구사항과 연결한다.
- Trade-offs: 세 구성요소의 로컬 실행, 장애 처리, 관측과 운영 비용이 추가된다.
- Decision: Accepted.

- Option: 여러 애플리케이션 인스턴스가 공유 인프라를 사용한다.
- Rationale: `NFR-1 — 확장성`, `NFR-4 — 신뢰성`, `AC-6 — 다중 인스턴스와 동시성`의 다중 서버 환경을 직접 검증할 수 있다.
- Trade-offs: 로컬 메모리 상태에 의존할 수 없고 경쟁 조건과 장애 전환 테스트가 필요하다.
- Decision: Accepted.

- Option: PostgreSQL `FOR UPDATE SKIP LOCKED`, Partial Index와 Unique Constraint·`ON CONFLICT`를 Outbox와 Consumer 멱등 처리에 적용한다.
- Rationale: PostgreSQL을 단순 영속 저장소로만 사용하지 않고, 행 잠금과 잠긴 행 건너뛰기로 다중 Publisher 선점을 조정하고, 처리 대상만 포함하는 인덱스로 조회 범위를 줄이며, 데이터베이스 제약으로 Consumer 중복 효과를 원자적으로 방지할 수 있다.
- Trade-offs: 선점 상태와 임대 만료 복구가 필요하고, 조회 조건과 Partial Index 조건을 일치시켜야 하며, 처리 이력 보관 정책이 추가된다.
- Decision: Accepted.

- Option: Sentinel 구조는 설계·학습하고 Redis 장애 시 PostgreSQL 대체 조회는 로컬에서 검증하되 실제 Sentinel Failover는 선택 범위로 둔다.
- Rationale: Redis 장애 중 인기 메뉴 정확성과 가용성은 실제로 검증하면서, Sentinel 다중 노드 구성과 클라이언트 재연결 검증이 핵심 주문·정합성 구현 범위를 과도하게 확장하지 않도록 한다.
- Trade-offs: 자동 Primary 승격과 클라이언트 재연결의 실제 동작은 필수 검증 증거에 포함되지 않으며 선택 검증을 수행하기 전까지 설계 수준의 근거만 남는다.
- Decision: Accepted.

## Proposed

- 개발과 주된 검증은 Docker 기반 다중 컨테이너로 수행한다.
- 최종 검증에서는 AWS에 ALB, 복수 애플리케이션 인스턴스와 RDS를 일부 배포한다.
- MSK와 ElastiCache는 아키텍처를 설계하되 실제 배포 범위는 비용과 과제 범위를 고려해 제한한다.
- 로컬 다중 컨테이너 검증과 AWS 일부 배포 결과를 함께 제시한다.

위 AWS 배포 범위는 잠정 방향이며 Accepted로 승격하지 않는다.

## Rejected Alternatives

- 명시적으로 배제되어 Rejected로 확정할 인프라 대안은 없다.

## Downstream Impact

- Domain: 특정 인프라 제품이 도메인 책임에 침투하지 않도록 주문, 포인트, 이벤트와 인기 조회의 경계를 유지해야 한다.
- Specification: 외부 데이터 수집 플랫폼과 인증 정보 제공 계약을 제외하면 인프라 배치는 API 계약에 노출하지 않는다.
- Issue: 로컬 인프라 구성, 다중 인스턴스 실행, PostgreSQL 기능 검증, Redis 고가용성 학습과 선택된 AWS 검증 범위를 별도 작업으로 추적해야 한다.
- Implementation: 애플리케이션 인스턴스는 로컬 영속 상태에 의존하지 않아야 하며 PostgreSQL과 Redis의 기준 데이터 역할을 혼동하지 않아야 한다. Outbox 작업 선점은 Partial Index와 `FOR UPDATE SKIP LOCKED`의 짧은 트랜잭션 경계를 따르고 Consumer 멱등 처리는 Unique Constraint와 `ON CONFLICT`를 사용해야 한다.
- Test: 다중 인스턴스 동시성, 다중 Publisher 선점, Partial Index 실행 계획, Consumer 중복 효과 방지, Kafka와 Redis 장애, 재시작 후 복구와 데이터 기준점 유지 여부를 검증해야 한다.
- Pull Request / Review: 새로운 인프라 의존성, 단일 인스턴스 가정, Redis를 기준 데이터로 사용하는 변경과 환경별 구성 차이를 확인해야 한다.

## Assumptions

- 추가로 채택한 미검증 가정은 없다.

## Open Decisions

- PostgreSQL, Kafka와 Redis의 구체적인 버전.
- Kafka Broker 수, 복제 수와 장애 허용 수준.
- PostgreSQL 고가용성과 읽기 복제본을 학습·설계 범위에 둘지 실제 검증 환경에 포함할지 여부.
- 선택 범위인 실제 Sentinel Failover 검증을 수행할 경우의 Sentinel 수, Replica 수, Quorum, Failover 제한시간과 클라이언트 재연결 설정.
- 실제 AWS 배포 구성요소와 비용 한도.
- 애플리케이션, 데이터베이스와 부하 발생기의 크기.
- 환경별 네트워크, 보안 경계와 Secret 관리 방식.
- 로컬 Docker 환경과 AWS 환경이 동일하게 검증해야 할 범위.
- 외부 인증 제공자와 토큰 검증 구성.
