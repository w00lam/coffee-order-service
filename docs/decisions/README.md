# 의사결정 기록

이 디렉터리는 장기적인 영향을 주는 설계 결정을 개별 문서로 관리합니다.

## 의사결정 목록

| 문서 | 상태 | 요약 |
| --- | --- | --- |
| [주문 처리, 포인트 일관성 및 멱등성](order-processing-consistency-and-idempotency.md) | Accepted | 서버 권위의 복수 항목 주문, 포인트 원자 결제와 일회성 주문 토큰의 경계를 정의한다. |
| [주문 이벤트 전달](order-event-delivery.md) | Accepted | Kafka, Transactional Outbox, 다중 Publisher와 Consumer 멱등성을 통한 `at-least-once` 전달을 정의한다. |
| [인기 메뉴 집계와 조회](popular-menu-projection-and-query.md) | Accepted | 최근 168시간 판매 수량 집계와 PostgreSQL 기준 데이터·Redis 조회 캐시의 역할을 정의한다. |
| [실행 환경과 데이터 토폴로지](runtime-and-data-topology.md) | Accepted | PostgreSQL, Kafka, Redis와 다중 애플리케이션 인스턴스의 책임을 정의한다. |
| [검증 및 운영 기준](verification-and-operations.md) | Accepted | k6 부하 목표, 데이터 정확성, 장애 격리와 검증 증거 관리 기준을 정의한다. |

## 작성 규칙

- 파일명은 내용을 설명하는 kebab-case를 사용합니다. 예: `order-status-model.md`
- 각 결정은 `templates/decision.md`를 기준으로 별도 파일에 기록합니다.
- 이 인덱스에는 결정의 전체 내용을 복제하지 않고 문서 링크와 상태만 기록합니다.
