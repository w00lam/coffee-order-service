# 다중 인스턴스 성능 측정 및 최적화 결과 (2026-07-18)

## 결론

- 확인된 주 병목은 고부하에서 Outbox publisher가 batch 처리를 마친 뒤 매번 기다리는 `200ms` fixed delay였다.
- 단일 변수로 `OUTBOX_POLL_INTERVAL`을 `200ms`에서 `20ms`로 줄였다. 검증 후 애플리케이션과 system-test 기본값을 `20ms`로 맞췄으며, batch `20`, lease `5s`, Kafka/Hikari 설정과 부하 데이터는 바꾸지 않았다.
- 고부하 5회 중앙값 기준 Outbox publication p95는 `3318.17ms`에서 `2135.31ms`로 `35.65%` 감소했고 p99는 `35.54%` 감소했다.
- 30회 채택 실행(baseline 15회, optimized 15회)에서 기능 실패와 예상하지 않은 5xx는 0건이었다. 11개 정합성 불변식, Outbox 최종 잔류 0건, Consumer 중복 업무 효과 0건을 모두 유지했다.
- 이번 수치는 운영 SLO나 새로운 k6 Threshold가 아니다.

## 환경과 부하

- Windows 로컬, Java 21, PostgreSQL 17.10, Kafka 3.8.0, Spring Boot 애플리케이션 3개
- 격리 Compose project `coffee-order-perf-codex`, 포트 `25432`, `29092`, `28081~28083`
- baseline과 optimized는 각각 빈 측정 전용 DB에서 시작해 `low → medium → high` 순서로 5회씩 실행했다.
- low: charge 30, spend 40, same-token 15, conflict 15, mixed 60
- medium: charge 60, spend 80, same-token 30, conflict 30, mixed 120
- high: charge 240, spend 320, same-token 120, conflict 120, mixed 480
- k6 Threshold는 기존 `functional_failures: count==0`, `unexpected_5xx: count==0`만 유지했다.

## 발견한 병목과 근거

고부하 baseline 5회에서 매 실행 532개 Outbox event가 생성됐다.

| 지표 | baseline 5회 범위 | 중앙값 |
|---|---:|---:|
| Outbox PENDING 최대 | 300~420 | 341 |
| publication p50 | 1697.16~2037.12ms | 1845.74ms |
| publication p95 | 3195.88~3456.14ms | 3318.17ms |
| publication p99 | 3241.79~3501.39ms | 3359.19ms |
| unpublished 최대 age | 3019~3382ms | 3222ms |
| Kafka send 평균 | 13.47~15.32ms | 13.71ms |

publisher 3개는 실행별로 각각 165~192건을 선점해 한 인스턴스 독점이 없었다. batch 20건의 동기 Kafka 전송에는 관측 평균으로 약 270~306ms가 필요했고, 그 뒤 `200ms` fixed delay가 추가됐다. Hikari pending/timeout, Kafka producer 오류와 Consumer lag가 모두 0인 상태에서도 PENDING 적체와 약 3.3초 p95가 5회 반복됐으므로 Outbox poll delay를 주 병목으로 판정했다.

## 검토했지만 주 병목이 아니었던 항목

- PostgreSQL query/index: 고부하에서 hot account 차감 query의 누적 실행시간은 11.10~13.24초, 최대 단건은 510.84~687.16ms였다. 충전 query도 누적 6.37~7.18초였다. 이는 동일 계정에 의도적으로 집중된 row lock 직렬화 근거이며 index 누락 근거는 아니었다. 정합성 경계를 바꾸지 않는 최소 query/index 개선을 확인하지 못해 변경하지 않았다.
- connection pool: high 5회 모두 Hikari pending 최대 0, timeout 0이었다. active 표본은 1~10 범위였으므로 pool 크기 10을 조정할 근거가 없었다.
- Kafka producer: 오류 0건이고 request/send latency만으로 broker 병목을 확정할 근거가 없었다. 동기 전송 비용은 Outbox 처리시간에 포함되지만 이번 실험에서는 더 큰 고정 poll delay 한 변수만 변경했다.
- Consumer: 측정된 records lag 최대 0, 중복 업무 효과 0이었다. 단일 topic partition 때문에 한 인스턴스가 주로 처리했지만 현재 부하에서 lag 병목은 확인되지 않았다.
- JVM/GC: high GC pause 최대 4~5ms, live thread 최대 55였고 heap/GC 포화 증거가 없었다.
- 부하 발생기/로컬 Docker: system CPU 최대 표본이 약 71~84%여서 절대 처리 한계는 로컬 host 영향을 받는다. 따라서 측정값을 운영 용량이나 SLO로 일반화하지 않는다.

## 변경 전후 반복 측정

값은 5회 `중앙값 [최소~최대]`이다. 변화율은 중앙값 기준이며 latency는 감소가 개선이다.

| 부하 | 지표 | baseline | optimized | 변화율 |
|---|---|---:|---:|---:|
| low | 처리량(req/s) | 31.990 [31.683~32.029] | 31.838 [31.719~32.094] | -0.47% |
| low | p95 | 237.28 [181.39~329.13]ms | 242.14 [195.95~350.86]ms | +2.05% |
| low | p99 | 334.16 [305.89~492.96]ms | 332.41 [312.35~510.32]ms | -0.52% |
| medium | 처리량(req/s) | 62.868 [62.176~63.201] | 63.195 [62.541~63.229] | +0.52% |
| medium | p95 | 205.60 [168.92~282.40]ms | 218.27 [175.37~329.99]ms | +6.16% |
| medium | p99 | 414.78 [306.21~447.67]ms | 396.57 [342.78~700.87]ms | -4.39% |
| high | 처리량(req/s) | 227.539 [223.866~228.333] | 230.156 [223.561~235.624] | +1.15% |
| high | p95 | 89.67 [82.44~97.52]ms | 83.63 [76.36~96.70]ms | -6.73% |
| high | p99 | 243.31 [233.30~282.86]ms | 239.40 [230.62~255.36]ms | -1.61% |
| 전체 | 기능 실패율 | 0% | 0% | 동일 |
| 전체 | 예상하지 않은 5xx | 0건 | 0건 | 동일 |

low/medium HTTP p95의 작은 상승은 각 5회 범위가 서로 겹치고, 같은 구간의 평균·p99·처리량이 함께 악화하지 않아 일관된 회귀로 판정하지 않았다.

## Outbox, pool, Consumer 변화

| high 지표 | baseline 중앙값 [범위] | optimized 중앙값 [범위] | 변화율 |
|---|---:|---:|---:|
| publication p50 | 1845.74 [1697.16~2037.12]ms | 1227.39 [1201.22~1438.71]ms | -33.50% |
| publication p95 | 3318.17 [3195.88~3456.14]ms | 2135.31 [2053.78~2180.23]ms | -35.65% |
| publication p99 | 3359.19 [3241.79~3501.39]ms | 2165.37 [2109.90~2218.30]ms | -35.54% |
| 최대 PENDING | 341 [300~420] | 274 [212~311] | -19.65% |
| unpublished 최대 age | 3222 [3019~3382]ms | 1332 [1290~1589]ms | -58.66% |
| 관측 drain window | 10588.94 [4350.90~10929.47]ms | 7564.23 [1000~7754.02]ms | -28.56% |
| Consumer event p95 | 3552.16 [3403.04~3680.83]ms | 3279.10 [3011.76~3334.34]ms | -7.69% |

- optimized high에서도 publisher 3개가 실행별 각 163~191건을 처리해 선점 분산을 유지했다.
- Hikari pending/timeout은 전후 모두 0이었다.
- Kafka producer 오류와 Consumer lag는 전후 모두 0이었다.
- process CPU 최대값의 high 중앙값은 약 5.82%→6.12%, system CPU는 76.46%→79.61%로 소폭 증가했다. 180ms 짧아진 poll에 따른 설명 가능한 비용이며 GC pause는 4ms→3ms, HTTP 주요 지표는 유지 또는 개선됐다.

## 정합성

채택한 30회 모두 다음 11개 불변식을 통과했다.

1. `point_charge_no_lost_update`
2. `all_balances_non_negative`
3. `point_order_equation`
4. `one_confirmed_order_per_token`
5. `one_outbox_event_per_successful_order`
6. `order_total_matches_items`
7. `outbox_payload_matches_order`
8. `consumer_event_applied_at_most_once`
9. `popular_projection_matches_orders`
10. `outbox_recovered_without_residue`
11. `no_failed_outbox`

각 실행의 최종 PENDING/PROCESSING/FAILED는 0, orders/outbox/consumer event 수는 일치했고 Consumer 중복 업무 효과는 0이었다. 빠른 poll 실험 중 기존 verifier가 Outbox drain만 기다리고 Consumer projection 완료 전에 검사하는 race가 한 번 드러나, 종료 조건을 두 단계 모두 drain될 때까지 기다리도록 보강한 뒤 optimized 15회를 새 prefix로 전량 재실행했다.

## 원본 결과와 SHA-256

- 기준선: `build/system-test/results/perf-measured-baseline-{low|medium|high}-{01..05}-*`
- 최적화: `build/system-test/results/perf-validated-optimized-{low|medium|high}-{01..05}-*`
- 실행별 관측 원본: 각 prefix의 `-observability/`
- 전체 390개 채택 원본 manifest: `build/system-test/results/performance-20260718-sha256.csv`
- manifest SHA-256: `a4e1c61350c5125efe52140758d37fce1b42929510dcb627e4121831e3439e04`
- HTTP 집계: `build/system-test/results/performance-comparison-http.json`
- Outbox 집계: `build/system-test/results/performance-comparison-outbox.json`

각 observability 디렉터리의 `sha256-manifest.json`에도 실행별 원본 파일 hash가 있다. 원본은 재현 가능한 로컬 산출물이라 Git에는 추가하지 않는다.

구조 재구성 후 `run-performance-suite.ps1`는 시작 시 `build/system-test/effective-config.json`을 확인해 baseline `200ms`, optimized `20ms`가 실제 실행 설정과 일치하는지 검증한다. low·medium·high 입력값은 `load/profiles.ps1`, 실행별 manifest는 `reporting/write-run-manifest.ps1`, phase별 HTTP index는 `reporting/summarize-performance.ps1`에서 관리한다.

## 환경 한계와 미확정 사항

- 단일 로컬 PostgreSQL, Kafka broker 1개와 topic partition 1개 결과다. AWS/RDS/MSK나 다중 partition 결과가 아니다.
- 로컬 system CPU가 높아 부하 발생기와 Docker host 한계를 분리 확정하지 못했다.
- hot-key PostgreSQL row lock contention은 확인됐지만 정합성을 유지하는 안전한 최소 최적화는 이번 범위에서 확정하지 못했다.
- Kafka 비동기 batch 전송, Consumer partition/concurrency, Hikari 크기, Outbox batch/lease는 한 번에 한 변수 원칙에 따라 변경하지 않았다.
- 결과는 운영 SLO, 최종 capacity, 새 Threshold의 근거로 사용하지 않는다.
